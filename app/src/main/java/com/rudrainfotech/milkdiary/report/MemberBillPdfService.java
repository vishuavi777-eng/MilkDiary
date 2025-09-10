package com.rudrainfotech.milkdiary.report;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.rudrainfotech.milkdiary.entity.*;
import com.rudrainfotech.milkdiary.i18n.I18n;
import com.rudrainfotech.milkdiary.service.*;

import java.awt.*;
import java.awt.font.*;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.BreakIterator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.*;

/**
 * Member bill + cap PDFs with i18n and Marathi digits, using embedded Noto Sans Devanagari.
 */
public class MemberBillPdfService {

    /* =========================
       Models / Small DTOs
       ========================= */
    private static class CapSummary {
        BigDecimal litre = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal avgFat; // can be null
        BigDecimal avgSnf;
    }
    private static class DayRow {
        java.time.LocalDate date;
        java.math.BigDecimal amQty = java.math.BigDecimal.ZERO;
        java.math.BigDecimal amAmt = java.math.BigDecimal.ZERO;
        java.math.BigDecimal pmQty = java.math.BigDecimal.ZERO;
        java.math.BigDecimal pmAmt = java.math.BigDecimal.ZERO;
    }
    private static class MemberCapData {
        java.util.List<DayRow> rows = new java.util.ArrayList<>();
        java.math.BigDecimal amTotalQty = java.math.BigDecimal.ZERO;
        java.math.BigDecimal pmTotalQty = java.math.BigDecimal.ZERO;

        java.math.BigDecimal totalQty = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalAmt = java.math.BigDecimal.ZERO;

        java.math.BigDecimal fatQty = java.math.BigDecimal.ZERO; // fat% * qty
        java.math.BigDecimal snfQty = java.math.BigDecimal.ZERO; // snf% * qty

        java.math.BigDecimal buffaloQty = java.math.BigDecimal.ZERO;
        java.math.BigDecimal cowQty = java.math.BigDecimal.ZERO;

        java.math.BigDecimal avgFat; // set later
        java.math.BigDecimal avgSnf; // set later
        java.math.BigDecimal gross;  // = totalAmt (2)
        java.math.BigDecimal roundOff;
        java.math.BigDecimal net;
    }
    private static class BillDayRow {
        java.time.LocalDate date;
        BigDecimal qty, rate, amount, fat, snf;
        BillDayRow(java.time.LocalDate d){ this.date=d; }
    }

    /* =========================
       Services / Fonts
       ========================= */
    private final BillingService billingSvc = new BillingService();
    @SuppressWarnings("unused")
    private final DailyEntryService dailySvc = new DailyEntryService();

    private static BaseFont DEV_REG;
    private static BaseFont DEV_BOLD;

    // AWT fonts for Java2D shaping
    private static java.awt.Font AWT_DEV_REG;
    private static java.awt.Font AWT_DEV_BOLD;

    private static synchronized BaseFont loadDevanagari(String resourcePath) {
        try (java.io.InputStream is = MemberBillPdfService.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new IllegalStateException("Font resource not found: " + resourcePath);
            java.nio.file.Path tmp = java.nio.file.Files.createTempFile("font-", ".ttf");
            java.nio.file.Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            tmp.toFile().deleteOnExit();
            return BaseFont.createFont(
                    tmp.toString(),
                    BaseFont.IDENTITY_H,   // REQUIRED for Devanagari glyphs
                    BaseFont.EMBEDDED
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to load font: " + resourcePath, e);
        }
    }
    private static BaseFont devanagariBaseFont() {
        if (DEV_REG == null) DEV_REG = loadDevanagari("/fonts/NotoSansDevanagari-Regular.ttf");
        return DEV_REG;
    }
    private static BaseFont devanagariBaseFontBold() {
        if (DEV_BOLD == null) DEV_BOLD = loadDevanagari("/fonts/NotoSansDevanagari-Bold.ttf");
        return DEV_BOLD;
    }

    private static synchronized java.awt.Font loadAwtFont(String resourcePath, int style, float sizePt) {
        try (java.io.InputStream is = MemberBillPdfService.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new IllegalStateException("Font resource not found: " + resourcePath);
            java.awt.Font base = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is);
            return base.deriveFont(style, sizePt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load AWT font: " + resourcePath, e);
        }
    }
    private static java.awt.Font awtDev(boolean bold, float sizePt) {
        if (bold) {
            if (AWT_DEV_BOLD == null || AWT_DEV_BOLD.getSize2D() != sizePt) {
                AWT_DEV_BOLD = loadAwtFont("/fonts/NotoSansDevanagari-Bold.ttf", java.awt.Font.BOLD, sizePt);
            }
            return AWT_DEV_BOLD.deriveFont(sizePt);
        } else {
            if (AWT_DEV_REG == null || AWT_DEV_REG.getSize2D() != sizePt) {
                AWT_DEV_REG = loadAwtFont("/fonts/NotoSansDevanagari-Regular.ttf", java.awt.Font.PLAIN, sizePt);
            }
            return AWT_DEV_REG.deriveFont(sizePt);
        }
    }

    // add near your helpers
    private static float minH(Font f) { return Math.max(12f, f.getSize() + 4f); }


    /* =========================
       Locale / digits helpers
       ========================= */
    private static boolean isMr() {
        return java.util.Locale.getDefault().getLanguage().equalsIgnoreCase("mr");
    }
    private static String toMrDigits(String s) {
        if (s == null) return "";
        return s.replace('0','०').replace('1','१').replace('2','२')
                .replace('3','३').replace('4','४').replace('5','५')
                .replace('6','६').replace('7','७').replace('8','८').replace('9','९');
    }
    private static String toLocalDigits(String s) {
        return isMr() ? toMrDigits(s) : (s == null ? "" : s);
    }
    private static String fmtDate(java.time.LocalDate d) {
        return toLocalDigits(d.toString());
    }
    private static String capTitle(int start, int end) {
        // bill.cap.days = "Days {0}–{1}" / "दिवस {0}–{1}"
        return I18n.t("bill.cap.days")
                .replace("{0}", toLocalDigits(Integer.toString(start)))
                .replace("{1}", toLocalDigits(Integer.toString(end)));
    }

    /* =========================
       Number helpers
       ========================= */
    private static BigDecimal nz(BigDecimal x){ return x==null?BigDecimal.ZERO:x; }
    private static String nvl(String s){ return s==null?"":s; }
    private static BigDecimal sc3(BigDecimal x){ return nz(x).setScale(3, RoundingMode.HALF_UP); }
    private static BigDecimal sc2(BigDecimal x){ return nz(x).setScale(2, RoundingMode.HALF_UP); }
    private static String fmt3(BigDecimal x){ return toLocalDigits(x==null?"": sc3(x).toPlainString()); }
    private static String fmt2(BigDecimal x){ return toLocalDigits(x==null?"": sc2(x).toPlainString()); }
    private static String nv2(BigDecimal x){ return toLocalDigits(x==null? "": x.setScale(2, RoundingMode.HALF_UP).toPlainString()); }

    /* =========================
       Public APIs
       ========================= */

    // Convenience overload: reads fillMissing & gap from settings
    public void generateCapAllMembers(Outlet outlet,
                                      int year,
                                      int month,
                                      int capNo,
                                      java.io.File file,
                                      Species speciesFilter) throws Exception {
        var s = new AppSettingsService();
        boolean fill = s.getBool(AppSettingsService.PDF_FILL_MISSING, true);
        float gap = (float) s.getDec(AppSettingsService.PDF_GAP_PT, new java.math.BigDecimal("8")).doubleValue();
        generateCapAllMembers(outlet, year, month, capNo, file, speciesFilter, fill, gap);
    }

    /**
     * One PDF for a single cap (1..10 / 11..20 / 21..EOM) for ALL members (3 blocks per page).
     */
    public void generateCapAllMembers(Outlet outlet,
                                      int year,
                                      int month,
                                      int capNo,
                                      java.io.File file,
                                      Species speciesFilter,
                                      boolean fillMissingDays,
                                      float gapPt) throws Exception {
        if (capNo < 1 || capNo > 3) throw new IllegalArgumentException("capNo must be 1..3");

        final Long outletId = outlet.getId();
        Outlet o = Tx.tx(s -> s.find(Outlet.class, outletId));

        YearMonth ym = YearMonth.of(year, month);
        int d1 = (capNo == 1) ? 1 : (capNo == 2 ? 11 : 21);
        int d2 = (capNo == 1) ? 10 : (capNo == 2 ? 20 : ym.lengthOfMonth());
        java.time.LocalDate start = ym.atDay(d1);
        java.time.LocalDate end   = ym.atDay(d2);

        String where = "where m.outlet.id = :oid";
        if (speciesFilter != null) where += " and m.species = :sp";

        String hql = ("""
                select m.id, m.code, m.name, m.species
                from Member m
                %s
                order by m.code
            """).formatted(where);

        List<Object[]> members = Tx.tx(s -> {
            var q = s.createQuery(hql, Object[].class).setParameter("oid", outletId);
            if (speciesFilter != null) q.setParameter("sp", speciesFilter);
            return q.getResultList();
        });

        Document doc = new Document(PageSize.A4, 18, 18, 18, 20);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
            PdfWriter writer = PdfWriter.getInstance(doc, fos);
            doc.open();

            // --- REPLACE font selection in generateCapAllMembers(...) ---
            final boolean mr = isMr();
            Font headF, baseF, smallF, boldF;
            if (mr) {
                var bf  = devanagariBaseFont();
                var bfb = devanagariBaseFontBold();
                headF  = new Font(bfb, 9);
                baseF  = new Font(bf,  8);
                smallF = new Font(bf,  7);
                boldF  = new Font(bfb, 8);
            } else {
                headF  = new Font(Font.HELVETICA, 9, Font.BOLD);
                baseF  = new Font(Font.HELVETICA, 8);
                smallF = new Font(Font.HELVETICA, 7);
                boldF  = new Font(Font.HELVETICA, 8, Font.BOLD);
            }

            int perPage = 3, onPage = 0;

            for (Object[] row : members) {
                Long memberId = (Long) row[0];
                String mCode  = (String) row[1];
                String mName  = (String) row[2];
                Species mSpecies = (Species) row[3];

                List<DailyMilkEntry> entries = Tx.tx(s ->
                        s.createQuery("""
                            from DailyMilkEntry e
                            where e.outlet.id=:oid and e.member.id=:mid and e.date between :sd and :ed
                            order by e.date asc, e.session asc
                        """, DailyMilkEntry.class)
                                .setParameter("oid", outletId)
                                .setParameter("mid", memberId)
                                .setParameter("sd", start)
                                .setParameter("ed", end)
                                .getResultList()
                );

                MemberCapData data = buildMemberCapData(entries, ym, d1, d2, fillMissingDays);
                if (data.totalQty.signum() == 0) continue;

                // Localized species name
                String speciesKey = (mSpecies == Species.BUFFALO) ? "species.buffalo" : "species.cow";
                String speciesLabel = I18n.t(speciesKey);

                PdfPTable block = makeMemberBlock(
                        o, mCode, mName, speciesLabel, start, end, data, headF, baseF, smallF, boldF, writer);

                block.setSpacingAfter(gapPt);
                doc.add(block);

                if (++onPage == perPage) {
                    doc.newPage();
                    onPage = 0;
                }
            }

            doc.close();
            writer.close();
        }
    }

    // Keep old signature for convenience; fillMissingDays=true
    public void generate(Outlet outlet, Member member, int year, int month, java.io.File file) throws Exception {
        generate(outlet, member, year, month, file, true);
    }

    /**
     * Full-month member bill (3 caps stacked vertically).
     */
    public void generate(Outlet outlet, Member member, int year, int month, java.io.File file, boolean fillMissingDays) throws Exception {
        final Long outletId = outlet.getId();
        final Long memberId = member.getId();

        Outlet o = Tx.tx(s -> s.find(Outlet.class, outletId));
        Member m = Tx.tx(s -> s.find(Member.class, memberId));

        MonthlyBill bill = billingSvc.upsertBill(o, m, year, month);

        YearMonth ym = YearMonth.of(year, month);
        java.time.LocalDate start = ym.atDay(1);
        java.time.LocalDate end   = ym.atEndOfMonth();

        List<DailyMilkEntry> entries = Tx.tx(s ->
                s.createQuery("""
                    from DailyMilkEntry e
                    where e.outlet.id = :oid and e.member.id = :mid and e.date between :sd and :ed
                    order by e.date asc
                    """, DailyMilkEntry.class)
                        .setParameter("oid", outletId)
                        .setParameter("mid", memberId)
                        .setParameter("sd", start)
                        .setParameter("ed", end)
                        .getResultList()
        );

        Map<java.time.LocalDate, BillDayRow> byDate = new LinkedHashMap<>();
        for (DailyMilkEntry e : entries) {
            BillDayRow r = byDate.computeIfAbsent(e.getDate(), BillDayRow::new);
            BigDecimal q = nz(e.getQtyLitre());
            BigDecimal amt = nz(e.getAmount());
            r.qty = nz(r.qty).add(q);
            r.amount = nz(r.amount).add(amt);
            if (e.getFatPct() != null) r.fat = nz(r.fat).add(e.getFatPct().multiply(q));
            if (e.getSnfPct() != null) r.snf = nz(r.snf).add(e.getSnfPct().multiply(q));
        }
        for (BillDayRow r : byDate.values()) {
            if (r.qty != null && r.qty.signum() > 0) {
                r.rate = r.amount.divide(r.qty, 3, RoundingMode.HALF_UP);
                r.fat = r.fat == null ? null : r.fat.divide(r.qty, 2, RoundingMode.HALF_UP);
                r.snf = r.snf == null ? null : r.snf.divide(r.qty, 2, RoundingMode.HALF_UP);
            } else {
                r.rate = null;
            }
            r.qty = sc3(r.qty);
            r.amount = sc2(r.amount);
        }

        List<BillDayRow> cap1 = buildCap(byDate, ym,  1, 10, fillMissingDays);
        List<BillDayRow> cap2 = buildCap(byDate, ym, 11, 20, fillMissingDays);
        List<BillDayRow> cap3 = buildCap(byDate, ym, 21, ym.lengthOfMonth(), fillMissingDays);

        CapSummary s1 = summarize(cap1);
        CapSummary s2 = summarize(cap2);
        CapSummary s3 = summarize(cap3);

        Document doc = new Document(PageSize.A4, 20, 20, 20, 24);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
            PdfWriter writer = PdfWriter.getInstance(doc, fos);
            doc.open();

            // Devanagari fonts
//            var bf  = devanagariBaseFont();
//            var bfb = devanagariBaseFontBold();
//            Font h1    = new Font(bfb, 12);
//            Font h2    = new Font(bfb, 10);
//            Font base  = new Font(bf,   9);
//            Font small = new Font(bf,   8);
//            Font bold  = new Font(bfb,  9);

            final boolean mr = isMr();
            Font h1, h2, base, small, bold;
            if (mr) {
                var bf  = devanagariBaseFont();
                var bfb = devanagariBaseFontBold();
                h1    = new Font(bfb, 12);
                h2    = new Font(bfb, 10);
                base  = new Font(bf,   9);
                small = new Font(bf,   8);
                bold  = new Font(bfb,  9);
            } else {
                // Use built-in Helvetica for Latin; no extra TTF needed
                h1    = new Font(Font.HELVETICA, 12, Font.BOLD);
                h2    = new Font(Font.HELVETICA, 10, Font.BOLD);
                base  = new Font(Font.HELVETICA,  9);
                small = new Font(Font.HELVETICA,  8);
                bold  = new Font(Font.HELVETICA,  9, Font.BOLD);
            }

            // Header
            Paragraph title = new Paragraph(o.getName() == null ? I18n.t("app.title") : o.getName(), h1);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            if (o.getAddress() != null && !o.getAddress().isBlank()) {
                Paragraph addr = new Paragraph(o.getAddress(), small);
                addr.setAlignment(Element.ALIGN_CENTER);
                doc.add(addr);
            }
            doc.add(Chunk.NEWLINE);

            PdfPTable info = new PdfPTable(new float[]{28, 36, 18, 18});
            info.setWidthPercentage(100);
            addKV(info, I18n.t("bill.header.member"), m.getCode() + " - " + nvl(m.getName()), base);
            addKV(info, I18n.t("bill.header.period"), toLocalDigits(String.format("%04d-%02d", year, month)), base);
            addKV(info, I18n.t("bill.header.billNo"), nvl(bill.getBillNo()), base);
            addKV(info, I18n.t("bill.header.date"), fmtDate(java.time.LocalDate.now()), base);
            doc.add(info);
            doc.add(Chunk.NEWLINE);

            // Three caps vertically
            doc.add(capTable(capTitle(1, 10),   cap1, s1, h2, base, small, bold));
            doc.add(Chunk.NEWLINE);
            doc.add(capTable(capTitle(11, 20),  cap2, s2, h2, base, small, bold));
            doc.add(Chunk.NEWLINE);
            doc.add(capTable(capTitle(21, ym.lengthOfMonth()), cap3, s3, h2, base, small, bold));
            doc.add(Chunk.NEWLINE);

            // Totals
            PdfPTable totals = new PdfPTable(new float[]{60, 40});
            totals.setWidthPercentage(60);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            addKV(totals, I18n.t("bill.footer.gross"),       fmt2(nz(bill.getGrossAmount())), bold);
            addKV(totals, I18n.t("bill.footer.saving"),      fmt2(nz(bill.getSavingsAmount())), base);
            addKV(totals, I18n.t("bill.footer.adjustments"), fmt2(nz(bill.getAdjustmentsTotal())), base);
            addKV(totals, I18n.t("bill.footer.roundOff"),    fmt2(nz(bill.getRoundOff())), base);

            PdfPCell netK = new PdfPCell(new Phrase(I18n.t("bill.footer.net"), bold));
            netK.setBorder(Rectangle.TOP);
            PdfPCell netV = new PdfPCell(new Phrase(fmt2(nz(bill.getNetAmount())), bold));
            netV.setHorizontalAlignment(Element.ALIGN_RIGHT);
            netV.setBorder(Rectangle.TOP);
            totals.addCell(netK); totals.addCell(netV);
            doc.add(totals);

            doc.close();
            writer.close();
        }
    }

    /* =========================
       Internals
       ========================= */

    private MemberCapData buildMemberCapData(List<DailyMilkEntry> entries,
                                             YearMonth ym,
                                             int d1, int d2,
                                             boolean fillMissingDays) {

        Map<java.time.LocalDate, DayRow> map = new LinkedHashMap<>();
        if (fillMissingDays) {
            for (int d = d1; d <= d2; d++) {
                java.time.LocalDate dt = ym.atDay(d);
                DayRow dr = new DayRow(); dr.date = dt;
                map.put(dt, dr);
            }
        }

        for (DailyMilkEntry e : entries) {
            // Ensure latest rate/amount before tallying
            dailySvc.computeRateAndAmount(e.getOutlet(), e);

            java.time.LocalDate dt = e.getDate();
            DayRow dr = map.computeIfAbsent(dt, k -> { DayRow x=new DayRow(); x.date=k; return x; });

            BigDecimal q = nz(e.getQtyLitre());
            BigDecimal a = nz(e.getAmount());

            if (e.getSession() == SessionType.AM) {
                dr.amQty = dr.amQty.add(q);
                dr.amAmt = dr.amAmt.add(a);
            } else {
                dr.pmQty = dr.pmQty.add(q);
                dr.pmAmt = dr.pmAmt.add(a);
            }
        }

        MemberCapData data = new MemberCapData();
        data.rows = map.values().stream()
                .sorted(Comparator.comparing(r -> r.date))
                .toList();

        for (DailyMilkEntry e : entries) {
            BigDecimal q = nz(e.getQtyLitre());
            data.totalQty = data.totalQty.add(q);
            data.totalAmt = data.totalAmt.add(nz(e.getAmount()));
            if (e.getFatPct()!=null) data.fatQty = data.fatQty.add(e.getFatPct().multiply(q));
            if (e.getSnfPct()!=null) data.snfQty = data.snfQty.add(e.getSnfPct().multiply(q));
            if (e.getSpecies()==Species.BUFFALO) data.buffaloQty = data.buffaloQty.add(q);
            if (e.getSpecies()==Species.COW)     data.cowQty = data.cowQty.add(q);
            if (e.getSession()==SessionType.AM)  data.amTotalQty = data.amTotalQty.add(q);
            else                                 data.pmTotalQty = data.pmTotalQty.add(q);
        }

        if (data.totalQty.signum()>0) {
            data.avgFat = data.fatQty.divide(data.totalQty, 2, RoundingMode.HALF_UP);
            data.avgSnf = data.snfQty.divide(data.totalQty, 2, RoundingMode.HALF_UP);
        }

        data.gross = sc2(data.totalAmt);
        BigDecimal preRound = data.gross;
        BigDecimal rounded  = preRound.setScale(0, RoundingMode.HALF_UP);
        data.roundOff = rounded.subtract(preRound).setScale(2, RoundingMode.HALF_UP);
        data.net = preRound.add(data.roundOff);

        return data;
    }

    private PdfPTable makeMemberBlock(Outlet o,
                                      String memberCode,
                                      String memberName,
                                      String speciesLabel, // already localized
                                      java.time.LocalDate start,
                                      java.time.LocalDate end,
                                      MemberCapData data,
                                      Font headF,
                                      Font baseF,
                                      Font smallF,
                                      Font boldF,
                                      PdfWriter writer) {

        String printedBy = new AppSettingsService()
                .getString(AppSettingsService.PRINTED_BY, System.getProperty("user.name","user"));

        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);

        Color hbg = new Color(240,240,240);

        // Row 1
        PdfPTable hdrRow1 = new PdfPTable(new float[]{55, 45});
        hdrRow1.setWidthPercentage(100);
        hdrRow1.addCell(slabInline(I18n.t("bill.header.outlet"),
                (o.getName()==null ? I18n.t("app.title") : o.getName()), headF, boldF, hbg));
        hdrRow1.addCell(slabInline(I18n.t("bill.header.member"),
                memberCode + " - " + (memberName==null?"":memberName), headF, boldF, hbg));
        outer.addCell(hdrRow1);

        // Row 2
        PdfPTable hdrRow2 = new PdfPTable(new float[]{14, 20, 66});
        hdrRow2.setWidthPercentage(100);
        hdrRow2.addCell(slabInline(I18n.t("bill.header.type"),      speciesLabel, baseF, baseF, hbg));
        hdrRow2.addCell(slabInline(I18n.t("bill.header.printedBy"), printedBy,    baseF, baseF, hbg));
        hdrRow2.addCell(slabInline(I18n.t("bill.header.span"),
                fmtDate(start) + " – " + fmtDate(end), baseF, baseF, hbg));
        outer.addCell(hdrRow2);

        // Table
        PdfPTable t = new PdfPTable(new float[]{16, 12, 12, 12, 12, 14, 22});
        t.setWidthPercentage(100);
        header(t, I18n.t("bill.col.date"),  boldF);
        header(t, I18n.t("bill.col.amQty"), boldF);
        header(t, I18n.t("bill.col.amRate"),boldF);
        header(t, I18n.t("bill.col.pmQty"), boldF);
        header(t, I18n.t("bill.col.pmRate"),boldF);
        header(t, I18n.t("bill.col.qty"),   boldF);
        header(t, I18n.t("bill.col.amount"),boldF);

        for (DayRow r : data.rows) {
            BigDecimal dayQty = r.amQty.add(r.pmQty);
            BigDecimal dayAmt = r.amAmt.add(r.pmAmt);
            BigDecimal amRate = r.amQty.signum()>0 ? r.amAmt.divide(r.amQty, 3, RoundingMode.HALF_UP) : null;
            BigDecimal pmRate = r.pmQty.signum()>0 ? r.pmAmt.divide(r.pmQty, 3, RoundingMode.HALF_UP) : null;

            t.addCell(cell(smallF, fmtDate(r.date)));
            t.addCell(cellRight(smallF, r.amQty.signum()==0? "" : fmt3(r.amQty)));
            t.addCell(cellRight(smallF, amRate==null? "" : fmt3(amRate)));
            t.addCell(cellRight(smallF, r.pmQty.signum()==0? "" : fmt3(r.pmQty)));
            t.addCell(cellRight(smallF, pmRate==null? "" : fmt3(pmRate)));
            t.addCell(cellRight(smallF, dayQty.signum()==0? "" : fmt3(dayQty)));
            t.addCell(cellRight(smallF, dayAmt.signum()==0? "" : fmt2(dayAmt)));
        }

        // Totals
        PdfPCell bDate = new PdfPCell(new Phrase(""));
        bDate.setBorder(Rectangle.TOP);
        t.addCell(bDate);

        PdfPCell amTot = new PdfPCell(new Phrase(fmt3(data.amTotalQty), boldF));
        amTot.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amTot.setBorder(Rectangle.TOP);
        t.addCell(amTot);

        PdfPCell bAmRate = new PdfPCell(new Phrase(""));
        bAmRate.setBorder(Rectangle.TOP);
        t.addCell(bAmRate);

        PdfPCell pmTot = new PdfPCell(new Phrase(fmt3(data.pmTotalQty), boldF));
        pmTot.setHorizontalAlignment(Element.ALIGN_RIGHT);
        pmTot.setBorder(Rectangle.TOP);
        t.addCell(pmTot);

        PdfPCell bPmRate = new PdfPCell(new Phrase(""));
        bPmRate.setBorder(Rectangle.TOP);
        t.addCell(bPmRate);

        PdfPCell totQty = new PdfPCell(new Phrase(fmt3(data.totalQty), boldF));
        totQty.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totQty.setBorder(Rectangle.TOP);
        t.addCell(totQty);

        PdfPCell totAmt = new PdfPCell(new Phrase(fmt2(data.totalAmt), boldF));
        totAmt.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totAmt.setBorder(Rectangle.TOP);
        t.addCell(totAmt);

        outer.addCell(t);

        // Footer summary
        PdfPTable summary = new PdfPTable(new float[]{16, 16, 16, 16, 16, 20});
        summary.setWidthPercentage(100);
        Color bg = new Color(245,245,245);
        summary.addCell(slabInline(I18n.t("bill.footer.litre"),    fmt3(data.totalQty), baseF, boldF, bg));
        summary.addCell(slabInline(I18n.t("bill.footer.avgFat"),   data.avgFat==null? "" : data.avgFat.setScale(2, RoundingMode.HALF_UP).toPlainString(), baseF, boldF, bg));
        summary.addCell(slabInline(I18n.t("bill.footer.avgSnf"),   data.avgSnf==null? "" : data.avgSnf.setScale(2, RoundingMode.HALF_UP).toPlainString(), baseF, boldF, bg));
        summary.addCell(slabInline(I18n.t("bill.footer.gross"),    fmt2(data.gross), baseF, boldF, bg));
        summary.addCell(slabInline(I18n.t("bill.footer.roundOff"), fmt2(data.roundOff), baseF, boldF, bg));
        summary.addCell(slabInline(I18n.t("bill.footer.net"),      fmt2(data.net), baseF, boldF, bg));

        outer.addCell(summary);
        return outer;
    }

    private List<BillDayRow> buildCap(
            Map<java.time.LocalDate, BillDayRow> byDate,
            YearMonth ym,
            int startDayInclusive,
            int endDayInclusive,
            boolean fillMissingDays) {

        int last = Math.min(endDayInclusive, ym.lengthOfMonth());

        if (!fillMissingDays) {
            return byDate.values().stream()
                    .filter(r -> {
                        int d = r.date.getDayOfMonth();
                        return d >= startDayInclusive && d <= last;
                    })
                    .toList();
        }

        List<BillDayRow> out = new ArrayList<>();
        for (int d = startDayInclusive; d <= last; d++) {
            java.time.LocalDate dt = ym.atDay(d);
            BillDayRow r = byDate.get(dt);
            if (r == null) {
                r = new BillDayRow(dt);
                r.qty = sc3(BigDecimal.ZERO);
                r.amount = sc2(BigDecimal.ZERO);
                r.rate = null;
                r.fat = null;
                r.snf = null;
            }
            out.add(r);
        }
        return out;
    }

    private CapSummary summarize(List<BillDayRow> rows) {
        CapSummary s = new CapSummary();
        BigDecimal fatQty = BigDecimal.ZERO, snfQty = BigDecimal.ZERO;
        for (BillDayRow r : rows) {
            s.litre = s.litre.add(nz(r.qty));
            s.amount = s.amount.add(nz(r.amount));
            if (r.fat != null && r.qty != null) fatQty = fatQty.add(r.fat.multiply(r.qty));
            if (r.snf != null && r.qty != null) snfQty = snfQty.add(r.snf.multiply(r.qty));
        }
        if (s.litre.signum() > 0) {
            s.avgFat = fatQty.divide(s.litre, 2, RoundingMode.HALF_UP);
            s.avgSnf = snfQty.divide(s.litre, 2, RoundingMode.HALF_UP);
        }
        s.litre = sc3(s.litre);
        s.amount = sc2(s.amount);
        return s;
    }

    private PdfPTable capTable(String title,
                               List<BillDayRow> rows,
                               CapSummary sum,
                               Font capTitle,
                               Font base,
                               Font small,
                               Font headerBold) {
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);

        PdfPCell cap = new PdfPCell(new Phrase(title, capTitle));
        cap.setHorizontalAlignment(Element.ALIGN_LEFT);
        cap.setBackgroundColor(new Color(240,240,240));
        cap.setPadding(3);
        outer.addCell(cap);

        PdfPTable t = new PdfPTable(new float[]{22, 20, 24, 34}); // Date, Qty, Rate, Amount
        t.setWidthPercentage(100);
        header(t, I18n.t("bill.col.date"),  headerBold);
        header(t, I18n.t("bill.col.qty"),   headerBold);
        header(t, I18n.t("bill.col.rate"),  headerBold);
        header(t, I18n.t("bill.col.amount"),headerBold);

        for (BillDayRow r : rows) {
            t.addCell(cell(small, fmtDate(r.date)));
            t.addCell(cellRight(small, fmt3(r.qty)));
            t.addCell(cellRight(small, r.rate==null? "" : fmt3(r.rate)));
            t.addCell(cellRight(small, fmt2(r.amount)));
        }
        outer.addCell(t);

        // Summary line (single row)
        PdfPTable s = new PdfPTable(new float[]{32, 68});
        s.setWidthPercentage(100);
        PdfPCell k = new PdfPCell(new Phrase(I18n.t("bill.header.period"), base));
        k.setBackgroundColor(new Color(245,245,245));
        k.setPadding(3);
        PdfPCell v = new PdfPCell(new Phrase(
                I18n.t("bill.footer.litre") + ": " + fmt3(sum.litre) +
                        "   " + I18n.t("bill.footer.avgFat") + ": " + nv2(sum.avgFat) +
                        "   " + I18n.t("bill.footer.avgSnf") + ": " + nv2(sum.avgSnf) +
                        "   " + I18n.t("bill.col.amount") + ": " + fmt2(sum.amount), base));
        v.setPadding(3);
        s.addCell(k); s.addCell(v);

        outer.addCell(s);
        return outer;
    }

    /* =========================
       PDF cell helpers
       ========================= */
    private static void addKV(PdfPTable t, String k, String v, Font f) {
        if (isMr()) {
            // KEY (left)
            PdfPCell ck = new PdfPCell(new Phrase(" ", f));   // placeholder
            ck.setBorder(Rectangle.NO_BORDER);
            ck.setPadding(0);
            ck.setMinimumHeight(minH(f));
            ck.setCellEvent(new ShapedTextCellEvent(k == null ? "" : k, f.getSize(), isBold(f), Element.ALIGN_LEFT, 2f));

            // VALUE (right)
            PdfPCell cv = new PdfPCell(new Phrase(" ", f));   // placeholder
            cv.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cv.setBorder(Rectangle.NO_BORDER);
            cv.setPadding(0);
            cv.setMinimumHeight(minH(f));
            cv.setCellEvent(new ShapedTextCellEvent(toLocalDigits(v), f.getSize(), isBold(f), Element.ALIGN_RIGHT, 2f));

            t.addCell(ck); t.addCell(cv);
        } else {
            PdfPCell ck = new PdfPCell(new Phrase(k, f));
            ck.setBorder(Rectangle.NO_BORDER);

            PdfPCell cv = new PdfPCell(new Phrase(v, f));
            cv.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cv.setBorder(Rectangle.NO_BORDER);

            t.addCell(ck); t.addCell(cv);
        }
    }
    private static void header(PdfPTable t, String s, Font headerFont){
        if (isMr()) {
            PdfPCell c = new PdfPCell(new Phrase(" ", headerFont)); // placeholder
            c.setBackgroundColor(new Color(235,235,235));
            c.setPadding(3);
            c.setMinimumHeight(minH(headerFont));
            c.setCellEvent(new ShapedTextCellEvent(s, headerFont.getSize(), true, Element.ALIGN_LEFT, 3f));
            t.addCell(c);
        } else {
            PdfPCell c = new PdfPCell(new Phrase(s, headerFont));
            c.setBackgroundColor(new Color(235,235,235));
            c.setPadding(3);
            t.addCell(c);
        }
    }
    private static PdfPCell cell(Font f, String s){
        if (isMr()) {
            PdfPCell c = new PdfPCell(new Phrase(" ", f)); // placeholder
            c.setPadding(2);
            c.setUseAscender(true); c.setUseDescender(true);
            c.setMinimumHeight(minH(f));
            c.setCellEvent(new ShapedTextCellEvent(s==null?"":s, f.getSize(), isBold(f), Element.ALIGN_LEFT, 2f));
            return c;
        } else {
            PdfPCell c = new PdfPCell(new Phrase(s==null?"":s, f));
            c.setPadding(2);
            return c;
        }
    }
    private static PdfPCell cellRight(Font f, String s){
        if (isMr()) {
            PdfPCell c = new PdfPCell(new Phrase(" ", f)); // placeholder
            c.setPadding(2);
            c.setUseAscender(true); c.setUseDescender(true);
            c.setHorizontalAlignment(Element.ALIGN_RIGHT);
            c.setMinimumHeight(minH(f));
            c.setCellEvent(new ShapedTextCellEvent(s==null?"":s, f.getSize(), isBold(f), Element.ALIGN_RIGHT, 2f));
            return c;
        } else {
            PdfPCell c = cell(f, s);
            c.setHorizontalAlignment(Element.ALIGN_RIGHT);
            return c;
        }
    }
    private static boolean isBold(Font f){
        return (f.getStyle() & Font.BOLD) != 0 || f.getBaseFont() == DEV_BOLD;
    }
    // A compact 2-col key|value cell with background and right-aligned value.
    private static PdfPCell slab(String k, String v, Font keyFont, Font valFont, Color bg) {
        PdfPTable t = new PdfPTable(new float[]{50, 50});
        t.setWidthPercentage(100);

        PdfPCell ck, cv;
        if (isMr()) {
            ck = new PdfPCell(new Phrase(" ", keyFont)); // placeholder
            ck.setBorder(Rectangle.NO_BORDER);
            ck.setBackgroundColor(bg);
            ck.setPadding(2);
            ck.setMinimumHeight(minH(keyFont));
            ck.setCellEvent(new ShapedTextCellEvent(k == null ? "" : k, keyFont.getSize(), isBold(keyFont), Element.ALIGN_LEFT, 2f));

            cv = new PdfPCell(new Phrase(" ", valFont)); // placeholder
            cv.setBorder(Rectangle.NO_BORDER);
            cv.setBackgroundColor(bg);
            cv.setPadding(2);
            cv.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cv.setMinimumHeight(minH(valFont));
            cv.setCellEvent(new ShapedTextCellEvent(toLocalDigits(v == null ? "" : v), valFont.getSize(), isBold(valFont), Element.ALIGN_RIGHT, 2f));
        } else {
            ck = new PdfPCell(new Phrase(k == null ? "" : k, keyFont));
            ck.setBorder(Rectangle.NO_BORDER);
            ck.setBackgroundColor(bg);
            ck.setPadding(2);

            cv = new PdfPCell(new Phrase(v == null ? "" : v, valFont));
            cv.setBorder(Rectangle.NO_BORDER);
            cv.setBackgroundColor(bg);
            cv.setPadding(2);
            cv.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }

        t.addCell(ck);
        t.addCell(cv);

        PdfPCell outer = new PdfPCell(t);
        outer.setPadding(0);
        outer.setBackgroundColor(bg);
        outer.setBorder(Rectangle.NO_BORDER);
        return outer;
    }

    // One-line inline "key : value" slab
    private static PdfPCell slabInline(String k, String v, Font keyFont, Font valFont, Color bg) {
        String key = (k == null) ? "" : k;
        String val = (v == null) ? "" : v;

        // Outer wrapper to match your existing slab() return shape
        PdfPCell outer;

        if (isMr()) {
            // In Marathi path: use shaped text to ensure proper shaping for Devanagari.
            // Compose a single string: "key : value" with localized digits on value.
            String text = key + " : " + toLocalDigits(val);

            // Choose a font for shaping (pick value font if present, else key font)
            Font shapeFont = (valFont != null) ? valFont : keyFont;

            PdfPCell c = new PdfPCell(new Phrase(" ", shapeFont)); // placeholder
            c.setBorder(Rectangle.NO_BORDER);
            c.setBackgroundColor(bg);
            c.setPadding(2f);
            c.setMinimumHeight(minH(shapeFont));

            // Draw the shaped string left-aligned with a little inner padding
            c.setCellEvent(new ShapedTextCellEvent(
                    text,
                    shapeFont.getSize(),
                    isBold(shapeFont),
                    Element.ALIGN_LEFT,
                    2f
            ));

            outer = new PdfPCell(c);
        } else {
            // Non-Marathi: we can use a normal Phrase with mixed fonts
            Phrase ph = new Phrase();
            ph.add(new Chunk(key, keyFont));
            ph.add(new Chunk(" : ", keyFont)); // separator styled like key
            ph.add(new Chunk(val, valFont));

            PdfPCell c = new PdfPCell(ph);
            c.setBorder(Rectangle.NO_BORDER);
            c.setBackgroundColor(bg);
            c.setPadding(2f);

            // Ensure consistent row height similar to your two-cell version
            float mh = Math.max(minH(keyFont), minH(valFont));
            c.setMinimumHeight(mh);

            outer = new PdfPCell(c);
        }

        outer.setPadding(0);
        outer.setBackgroundColor(bg);
        outer.setBorder(Rectangle.NO_BORDER);
        return outer;
    }


    /* =========================
       Java2D shaping for Marathi-only cells
       ========================= */
    private static class ShapedTextCellEvent implements PdfPCellEvent {
        private final String text;
        private final float fontPt;
        private final boolean bold;
        private final int hAlign;
        private final float paddingPt;

        ShapedTextCellEvent(String text, float fontPt, boolean bold, int hAlign, float paddingPt) {
            this.text = text == null ? "" : text;
            this.fontPt = fontPt <= 0 ? 8f : fontPt;
            this.bold = bold;
            this.hAlign = hAlign;
            this.paddingPt = paddingPt;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle rect, PdfContentByte[] canvases) {
            if (!isMr()) return;                         // only used for Marathi
            if (text.isEmpty()) return;                  // IMPORTANT: avoid AttributedString on empty text

            final float w = Math.max(1f, rect.getWidth());
            final float h = Math.max(1f, rect.getHeight());

            PdfContentByte cb = canvases[PdfPTable.TEXTCANVAS];
            PdfTemplate tp = cb.createTemplate(w, h);
            Graphics2D g2 = new PdfGraphics2D(tp, w, h);
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Latin fallback + Devanagari font
                java.awt.Font latin = new java.awt.Font("SansSerif", bold ? java.awt.Font.BOLD : java.awt.Font.PLAIN, Math.round(fontPt));
                java.awt.Font dev   = awtDev(bold, fontPt);

                final String s = text;
                AttributedString as = new AttributedString(s);
                // Use *ranged* addAttribute so 0-length never throws
                as.addAttribute(TextAttribute.LANGUAGE, new java.util.Locale("mr","IN"), 0, s.length());
                as.addAttribute(TextAttribute.FONT, latin, 0, s.length());

                // Apply Devanagari font only on Devanagari runs
                int i = 0;
                while (i < s.length()) {
                    int cp = s.codePointAt(i);
                    int cc = Character.charCount(cp);
                    var blk = Character.UnicodeBlock.of(cp);
                    boolean isDev = blk == Character.UnicodeBlock.DEVANAGARI
                            || blk == Character.UnicodeBlock.DEVANAGARI_EXTENDED
                            || blk == Character.UnicodeBlock.DEVANAGARI_EXTENDED_A
                            || cp == 0x200C || cp == 0x200D; // ZWNJ/ZWJ
                    int j = i + cc;
                    if (isDev) {
                        while (j < s.length()) {
                            int cp2 = s.codePointAt(j);
                            int cc2 = Character.charCount(cp2);
                            var blk2 = Character.UnicodeBlock.of(cp2);
                            boolean isDev2 = blk2 == Character.UnicodeBlock.DEVANAGARI
                                    || blk2 == Character.UnicodeBlock.DEVANAGARI_EXTENDED
                                    || blk2 == Character.UnicodeBlock.DEVANAGARI_EXTENDED_A
                                    || cp2 == 0x200C || cp2 == 0x200D;
                            if (!isDev2) break;
                            j += cc2;
                        }
                        as.addAttribute(TextAttribute.FONT, dev, i, j);
                    }
                    i = j;
                }

                AttributedCharacterIterator it = as.getIterator();
                LineBreakMeasurer lbm = new LineBreakMeasurer(
                        it, BreakIterator.getLineInstance(new java.util.Locale("mr","IN")), g2.getFontRenderContext());

                final float availW = Math.max(1f, w - 2*paddingPt);
                final float x0 = paddingPt;
                float y = paddingPt;

                final int end = it.getEndIndex();
                while (lbm.getPosition() < end) {
                    TextLayout layout = lbm.nextLayout(availW);
                    if (layout == null) break;

                    y += layout.getAscent();
                    float dx = switch (hAlign) {
                        case Element.ALIGN_CENTER -> (availW - (float)layout.getAdvance()) / 2f;
                        case Element.ALIGN_RIGHT  -> (availW - (float)layout.getAdvance());
                        default -> 0f;
                    };
                    layout.draw(g2, x0 + dx, y);

                    y += layout.getDescent() + layout.getLeading();
                    if (y > h - paddingPt) break;
                }
            } finally {
                g2.dispose();
            }

            try {
                Image img = Image.getInstance(tp);
                img.setAbsolutePosition(rect.getLeft(), rect.getBottom());
                canvases[PdfPTable.TEXTCANVAS].addImage(img);
            } catch (Exception ignore) {}
        }
    }
}