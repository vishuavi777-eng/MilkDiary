package com.rudrainfotech.milkdiary.ui;

import com.rudrainfotech.milkdiary.entity.Outlet;
import com.rudrainfotech.milkdiary.i18n.I18n;
import com.rudrainfotech.milkdiary.service.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;

public class OutletSummaryView extends BorderPane {

    private final OutletService outletSvc = new OutletService();
    private final OutletSummaryService svc = new OutletSummaryService();
    private final BillingService billingSvc = new BillingService();
    private final Outlet outlet;

    private final ComboBox<Integer> month = new ComboBox<>();
    private final Spinner<Integer> year = new Spinner<>();
    private final Spinner<Integer> capSpinner = new Spinner<>();
    private final CheckBox overrideCk = new CheckBox("Override lock (admin)");

    private final Label vOutlet = new Label("-");
    private final Label vPeriod = new Label("-");
    private final Label vTot = new Label("-");
    private final Label vCow = new Label("-");
    private final Label vBuf = new Label("-");
    private final Label vAM = new Label("-");
    private final Label vPM = new Label("-");
    private final Label vFat = new Label("-");
    private final Label vSnf = new Label("-");
    private final Label vGross = new Label("-");
    private final Label vAdj = new Label("-");
    private final Label vRO = new Label("-");
    private final Label vNet = new Label("-");
    private final Label vBills = new Label("-");
    private final Label vLocked = new Label("-");
    private final Label vMembers = new Label("-");
    private final Label  lockInfo = new Label();

    public OutletSummaryView() {
        this.outlet = outletSvc.getActiveOutlet();

        // Controls
        for (int m=1; m<=12; m++) month.getItems().add(m);
        int nowY = LocalDate.now().getYear(); int nowM = LocalDate.now().getMonthValue();
        month.getSelectionModel().select(Integer.valueOf(nowM));
        year.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(nowY-3, nowY+3, nowY));
        capSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 3, 1, 1));
        capSpinner.setEditable(true);

        Button refresh = new Button(I18n.t("button.refresh"));
        Button genAll = new Button(I18n.t("button.generateBills"));
        Button lockBtn = new Button(I18n.t("button.lockCap"));
        Button unlockBtn = new Button(I18n.t("button.unlockCap"));
        Button export = new Button(I18n.t("button.exportCSV"));

        refresh.setOnAction(e -> reload());
        genAll.setOnAction(e -> {
            if (outlet == null) { warn("Set an active outlet first."); return; }

            int y = year.getValue();
            int m = month.getValue();
            int capNo = capSpinner.getValue(); // you added this spinner earlier

            boolean locked = new BillLockService().isCapLocked(outlet.getId(), y, m, capNo);
            if (locked && !overrideCk.isSelected()) {
                warn("This cap is LOCKED. Enable 'Override lock' and enter Admin PIN to proceed.");
                return;
            }
            if (locked && overrideCk.isSelected() && !requireAdminPin()) return;


            // Recompute (full month math; your service already does upsert per member)
            billingSvc.generateForAllMembers(outlet, y, m, false);

            // Auto-lock this cap if setting enabled
            var settings = new AppSettingsService();
            if (settings.getBool(AppSettingsService.LOCK_ON_GENERATE, false)) {
                new BillLockService().lockCap(
                        new AppSettingsService().ensureActiveOutletExists(),
                        y, m, capNo,
                        settings.getString(AppSettingsService.PRINTED_BY, System.getProperty("user.name","user"))
                );
            }

            // Audit
            new AuditService().log("generate_bills", outlet, null, y, m, capNo, "outlet summary view");

            info("Bills generated/recalculated for all members.");
            refreshLockInfo();
            reload();
        });

        overrideCk.setSelected(false);
        overrideCk.selectedProperty().addListener((obs, was, now) -> {
            if (now) {
                TextInputDialog d = new TextInputDialog();
                d.setTitle("Admin PIN");
                d.setHeaderText("Enter Admin PIN to override lock");
                d.setContentText("PIN:");
                d.getEditor().setText("");
                d.getEditor().setPromptText("PIN");
                var res = d.showAndWait();
                boolean ok = res.isPresent() && new AppSettingsService().verifyAdminPin(res.get());
                if (!ok) {
                    overrideCk.setSelected(false);
                    showError("Invalid PIN.");
                }
            }
        });

        lockBtn.setOnAction(e -> {
            if (outlet==null) { warn("Set an active outlet first."); return; }
            if (!requireAdminPin()) return;
            int y = year.getValue(), m = month.getValue(), cap = capSpinner.getValue();
            String by = new AppSettingsService().getString(AppSettingsService.PRINTED_BY, System.getProperty("user.name","user"));
            new BillLockService().lockCap(outlet, y, m, cap, by);
            new AuditService().log("lock_cap", outlet, null, y, m, cap, "via OutletSummary");
            info("Locked cap " + cap);
            refreshLockInfo();
        });

        unlockBtn.setOnAction(e -> {
            if (outlet==null) { warn("Set an active outlet first."); return; }
            if (!requireAdminPin()) return;
            int y = year.getValue(), m = month.getValue(), cap = capSpinner.getValue();
            String by = new AppSettingsService().getString(AppSettingsService.PRINTED_BY, System.getProperty("user.name","user"));
            new BillLockService().unlockCap(outlet, y, m, cap, by);
            new AuditService().log("unlock_cap", outlet, null, y, m, cap, "via OutletSummary");
            info("Unlocked cap " + cap);
            refreshLockInfo();
        });

        export.setOnAction(e -> onExport());

        HBox hBox1 = new HBox(
                10,
                new Label("Month:"), month,
                new Label("Year:"), year,
                new Label("Cap:"), capSpinner,
                lockBtn, lockInfo, unlockBtn
        );

        HBox hBox2 = new HBox(
                10,
                refresh, genAll, export, overrideCk
        );
        VBox top = new VBox(10,
            hBox1,
            hBox2
        );
        top.setPadding(new Insets(10));
        setTop(top);

        // Summary grid
        GridPane gp = new GridPane();
        gp.setPadding(new Insets(16));
        gp.setHgap(18); gp.setVgap(10);

        int r=0;
        gp.addRow(r++, new Label("Outlet:"), vOutlet);
        gp.addRow(r++, new Label("Period:"), vPeriod);
        gp.add(new Separator(), 0, r++, 2, 1);

        gp.addRow(r++, new Label("Total Litre"), vTot);
        gp.addRow(r++, new Label("Cow Litre"), vCow);
        gp.addRow(r++, new Label("Buffalo Litre"), vBuf);
        gp.addRow(r++, new Label("AM Litre"), vAM);
        gp.addRow(r++, new Label("PM Litre"), vPM);
        gp.addRow(r++, new Label("Avg Fat"), vFat);
        gp.addRow(r++, new Label("Avg SNF"), vSnf);
        gp.add(new Separator(), 0, r++, 2, 1);

        gp.addRow(r++, new Label("Gross Amount"), vGross);
        gp.addRow(r++, new Label("Adjustments"), vAdj);
        gp.addRow(r++, new Label("Round Off"), vRO);
        gp.addRow(r++, new Label("Net Amount"), vNet);
        gp.add(new Separator(), 0, r++, 2, 1);

        gp.addRow(r++, new Label("Bills Count"), vBills);
        gp.addRow(r++, new Label("Locked Bills"), vLocked);
        gp.addRow(r++, new Label("Members With Entries"), vMembers);

        setCenter(gp);

        reload();
    }

    private boolean requireAdminPin() {
        var svc = new AppSettingsService();
        // If no PIN set, allow only if you explicitly want to
        String stored = svc.getString(AppSettingsService.ADMIN_PIN_HASH, null);
        if (stored == null || stored.isBlank()) {
            error("Admin PIN is not set. Please set it in Settings first.");
            return false;
        }
        Dialog<String> dlg = new Dialog<>();
        dlg.setTitle("Admin PIN");
        dlg.setHeaderText("Enter Admin PIN");
        PasswordField pf = new PasswordField();
        pf.setPromptText("PIN");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setContent(pf);
        dlg.setResultConverter(bt -> bt == ButtonType.OK ? pf.getText() : null);
        String pin = dlg.showAndWait().orElse(null);
        if (pin == null) return false;
        if (!svc.verifyAdminPin(pin)) { error("Incorrect PIN."); return false; }
        return true;
    }

    void refreshLockInfo() {
        Outlet o = new AppSettingsService().ensureActiveOutletExists();
        int y = year.getValue(), m = month.getValue(), c = capSpinner.getValue();
        boolean locked = new BillLockService().isCapLocked(o.getId(), y, m, c);
        lockInfo.setText(locked ? "LOCKED" : "Unlocked");
    }

    private void reload() {
        if (outlet == null) {
            setLabels("-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-");
            return;
        }
        int y = year.getValue();
        int m = month.getValue();
        OutletSummary S = svc.compute(outlet, y, m);

        vOutlet.setText(outlet.getName());
        vPeriod.setText(String.format("%04d-%02d", y, m));

        vTot.setText(fmt3(S.totalLitre));
        vCow.setText(fmt3(S.cowLitre));
        vBuf.setText(fmt3(S.buffaloLitre));
        vAM.setText(fmt3(S.amLitre));
        vPM.setText(fmt3(S.pmLitre));
        vFat.setText(S.avgFat==null? "": S.avgFat.toPlainString());
        vSnf.setText(S.avgSnf==null? "": S.avgSnf.toPlainString());
        vGross.setText(fmt2(S.grossTotal));
        vAdj.setText(fmt2(S.adjustmentsTotal));
        vRO.setText(fmt2(S.roundOffTotal));
        vNet.setText(fmt2(S.netTotal));
        vBills.setText(String.valueOf(S.billsCount));
        vLocked.setText(String.valueOf(S.lockedBillsCount));
        vMembers.setText(String.valueOf(S.membersWithEntries));
    }

    private void onExport() {
        if (outlet == null) { warn("Set an active outlet first."); return; }
        int y = year.getValue(); int m = month.getValue();
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Outlet Summary");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName(String.format("OutletSummary-%s-%04d-%02d.csv", safe(outlet.getName()), y, m));
        File f = fc.showSaveDialog(getScene().getWindow());
        if (f == null) return;
        try {
            svc.exportCsv(f, svc.compute(outlet, y, m), y, m, outlet);
            info("Exported:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            error("Export failed: " + rootMsg(ex));
        }
    }

    private static String fmt3(java.math.BigDecimal x){ return x==null? "": x.setScale(3, java.math.RoundingMode.HALF_UP).toPlainString(); }
    private static String fmt2(java.math.BigDecimal x){ return x==null? "": x.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(); }
    private static void info(String m){ var a=new Alert(Alert.AlertType.INFORMATION,m,ButtonType.OK); a.setHeaderText(null); a.showAndWait();}
    private static void warn(String m){ var a=new Alert(Alert.AlertType.WARNING,m,ButtonType.OK); a.setHeaderText("Warning"); a.showAndWait();}
    private static void error(String m){ var a=new Alert(Alert.AlertType.ERROR,m,ButtonType.OK); a.setHeaderText("Error"); a.showAndWait();}
    private static String rootMsg(Throwable t){ Throwable x=t; while(x.getCause()!=null) x=x.getCause(); return x.getMessage(); }
    private static String safe(String name){ return name==null? "Outlet" : name.replaceAll("[^\\w\\-]+","_"); }
    private static void showInfo(String m){ var a=new Alert(Alert.AlertType.INFORMATION,m,ButtonType.OK); a.setHeaderText(null); a.showAndWait();}
    private static void showError(String m){ var a=new Alert(Alert.AlertType.ERROR,m,ButtonType.OK); a.setHeaderText("Error"); a.showAndWait();}

    private void setLabels(String... all){
        Label[] arr = { vOutlet, vPeriod, vTot, vCow, vBuf, vAM, vPM, vFat, vSnf, vGross, vAdj, vRO, vNet, vBills, vLocked, vMembers };
        for (int i=0;i<arr.length && i<all.length;i++) arr[i].setText(all[i]);
    }

}
