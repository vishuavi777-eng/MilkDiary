package com.rudrainfotech.milkdiary.ui;

import com.rudrainfotech.milkdiary.entity.*;
import com.rudrainfotech.milkdiary.i18n.I18n;
import com.rudrainfotech.milkdiary.report.MemberBillPdfService;
import com.rudrainfotech.milkdiary.service.AppSettingsService;
import com.rudrainfotech.milkdiary.service.BillingService;
import com.rudrainfotech.milkdiary.service.OutletService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class MonthlyBillingView extends BorderPane implements StandardActions {
    private final OutletService outletSvc = new OutletService();
    private final BillingService svc = new BillingService();
    private final Outlet outlet;

    private final ComboBox<Integer> month = new ComboBox<>();
    private final Spinner<Integer> year = new Spinner<>();
    private final ComboBox<String> speciesFilter = new ComboBox<>();
    private final TableView<MonthlyBill> table = new TableView<>();
    private final ObservableList<MonthlyBill> rows = FXCollections.observableArrayList();

    public MonthlyBillingView() {
        this.outlet = outletSvc.getActiveOutlet();

        // period pickers
        for (int m=1; m<=12; m++) month.getItems().add(m);
        int nowY = LocalDate.now().getYear(); int nowM = LocalDate.now().getMonthValue();
        month.getSelectionModel().select(Integer.valueOf(nowM));
        year.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(nowY-3, nowY+3, nowY));

        Button load = new Button(I18n.t("button.load"));
        Button generate = new Button(I18n.t("button.generateAll"));
        Button recalc = new Button(I18n.t("button.recalcSelected"));
        Button lock = new Button(I18n.t("button.lock"));
        Button unlock = new Button(I18n.t("button.unlock"));
        Button adj = new Button(I18n.t("button.billAdjustments"));
        Button refresh = new Button(I18n.t("button.refresh"));
        Button pdf = new Button(I18n.t("button.billPDFSingle"));
        Button capAll = new Button(I18n.t("button.billPDFAll"));

        // Species filter (All/Cow/Buffalo)
        speciesFilter.getItems().addAll("All", "Cow", "Buffalo");
        speciesFilter.getSelectionModel().select("All");
        speciesFilter.valueProperty().addListener((obs, o, n) -> applyFilter());

        load.setOnAction(e -> reload());
        refresh.setOnAction(e -> reload());
        generate.setOnAction(e -> { svc.generateForAllMembers(outlet, year.getValue(), month.getValue(), true); reload(); });
        recalc.setOnAction(e -> {
            MonthlyBill b = table.getSelectionModel().getSelectedItem();
            if (b==null) { info("Select a bill first."); return; }
            svc.upsertBill(outlet, b.getMember(), b.getYear(), b.getMonth());
            reload();
        });

        lock.setOnAction(e -> {
            if (!requireAdminPin()) { error("Invalid PIN."); return; }
            MonthlyBill b = selected();
            if (b!=null){ svc.lockBill(b,true); reload(); }
        });

        unlock.setOnAction(e -> {
            if (!requireAdminPin()) { error("Invalid PIN."); return; }
            MonthlyBill b = selected();
            if (b!=null){ svc.lockBill(b,false); reload(); }
        });

        adj.setOnAction(e -> onAdjust());
        pdf.setOnAction(e -> onPdf());
        capAll.setOnAction(e -> onCapAll());

        HBox hBox1 = new HBox(10, new Label("Month:"), month, new Label("Year:"), year,
                new Label("Species:"), speciesFilter,
                load, refresh, generate, recalc);

        HBox hBox2 = new HBox(10,  lock, unlock, adj, pdf, capAll);

        VBox top = new VBox(10,
            hBox1,
            hBox2
        );
        top.setPadding(new Insets(10));
        setTop(top);

        // table columns
        TableColumn<MonthlyBill, String> cCode = new TableColumn<>("Code");
        cCode.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getMember().getCode()));
        cCode.setPrefWidth(100);

        TableColumn<MonthlyBill, String> cName = new TableColumn<>("Name");
        cName.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getMember().getName()));
        cName.setPrefWidth(200);

        TableColumn<MonthlyBill, String> cLit = new TableColumn<>("Litre");
        cLit.setCellValueFactory(d -> new SimpleObjectProperty<>(
                d.getValue().getTotalLitre()==null? "": d.getValue().getTotalLitre().toPlainString()));
        cLit.setPrefWidth(90);

        TableColumn<MonthlyBill, String> cFat = new TableColumn<>("Avg Fat");
        cFat.setCellValueFactory(d -> new SimpleObjectProperty<>(
                d.getValue().getAvgFat()==null? "": d.getValue().getAvgFat().toPlainString()));
        cFat.setPrefWidth(80);

        TableColumn<MonthlyBill, String> cSnf = new TableColumn<>("Avg SNF");
        cSnf.setCellValueFactory(d -> new SimpleObjectProperty<>(
                d.getValue().getAvgSnf()==null? "": d.getValue().getAvgSnf().toPlainString()));
        cSnf.setPrefWidth(80);

        TableColumn<MonthlyBill, Species> cSpecies = new TableColumn<>("Species");
        cSpecies.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getMember().getSpecies()));
        cSpecies.setPrefWidth(100);

        TableColumn<MonthlyBill, ?> cGross = new TableColumn<>("Gross");
        cGross.setCellValueFactory(new PropertyValueFactory<>("grossAmount"));
        cGross.setPrefWidth(90);

        TableColumn<MonthlyBill, ?> cAdj = new TableColumn<>("Adjustments");
        cAdj.setCellValueFactory(new PropertyValueFactory<>("adjustmentsTotal"));
        cAdj.setPrefWidth(100);

        TableColumn<MonthlyBill, ?> cRO = new TableColumn<>("RoundOff");
        cRO.setCellValueFactory(new PropertyValueFactory<>("roundOff"));
        cRO.setPrefWidth(90);

        TableColumn<MonthlyBill, ?> cNet = new TableColumn<>("Net");
        cNet.setCellValueFactory(new PropertyValueFactory<>("netAmount"));
        cNet.setPrefWidth(100);

        TableColumn<MonthlyBill, Boolean> cLocked = new TableColumn<>("Locked");
        cLocked.setCellValueFactory(new PropertyValueFactory<>("locked"));
        cLocked.setPrefWidth(70);

        table.getColumns().addAll(cCode, cName, cLit, cFat, cSnf, cGross, cAdj, cRO, cNet, cLocked, cSpecies);
        table.setItems(rows);
        table.setPlaceholder(new Label("Click Load or Generate All"));
        setCenter(table);

        reload();
    }

    private boolean requireAdminPin() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Admin PIN");
        d.setHeaderText("Enter Admin PIN");
        d.setContentText("PIN:");
        var res = d.showAndWait();
        return res.isPresent() && new AppSettingsService().verifyAdminPin(res.get());
    }

    private void reload() {
        if (outlet==null) { rows.clear(); return; }
        List<MonthlyBill> list = svc.listByOutletAndPeriod(outlet, year.getValue(), month.getValue());
        rows.setAll(list);
        applyFilter();
    }

    // Filter table by species selection
    private void applyFilter() {
        String f = speciesFilter.getValue();
        if (f == null || "All".equals(f)) {
            table.setItems(rows);
            return;
        }
        Species wanted = "Cow".equals(f) ? Species.COW : Species.BUFFALO;
        table.setItems(rows.filtered(b -> b.getMember()!=null && b.getMember().getSpecies()==wanted));
    }

    private MonthlyBill selected() {
        MonthlyBill b = table.getSelectionModel().getSelectedItem();
        if (b==null) info("Select a bill first.");
        return b;
    }

    private void onAdjust() {
        MonthlyBill b = selected();
        if (b==null) return;
        if (b.isLocked()) { error("Bill is locked."); return; }
        AdjustmentDialog.show(b, (type, amount, remark) -> {
            new BillingService().addOrUpdateAdjustment(b, type, amount, remark);
            reload();
        });
    }

    private void onCapAll() {
        Outlet outlet = new OutletService().getActiveOutlet();
        if (outlet == null) { error("Set an active outlet first."); return; }

        // Simple cap chooser
        ChoiceDialog<Integer> dlg = new ChoiceDialog<>(1, java.util.List.of(1,2,3));
        dlg.setTitle("Cap PDF (All Members)");
        dlg.setHeaderText("Select Cap");
        dlg.setContentText("Cap (1=days 1–10, 2=11–20, 3=21–EOM):");
        var res = dlg.showAndWait();
        if (res.isEmpty()) return;
        int capNo = res.get();

        // Fill-missing-days option
        Alert opt = new Alert(Alert.AlertType.CONFIRMATION);
        opt.setTitle("Fill Missing Days?");
        opt.setHeaderText("Include blank rows for dates without entries?");
        opt.setContentText("OK = Yes, Cancel = No");
        boolean fillMissingDays = opt.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;

        Species sp = switch (speciesFilter.getValue()) {
            case "Cow" -> Species.COW;
            case "Buffalo" -> Species.BUFFALO;
            default -> null; // All
        };

        FileChooser fc = new FileChooser();
        fc.setTitle("Save Cap PDF (All Members)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName(String.format("Cap-%04d-%02d-%d-AllMembers.pdf", year.getValue(), month.getValue(), capNo));
        java.io.File f = fc.showSaveDialog(getScene().getWindow());
        if (f == null) return;

        try {
            new MemberBillPdfService().generateCapAllMembers(outlet, year.getValue(), month.getValue(),
                    capNo, f, sp);
            info("Saved:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            error("PDF failed: " + rootMsg(ex));
        }
    }


    private void onPdf() {
        MonthlyBill b = selected();
        if (b == null) return;
        // choose cap (you can replace this with a nicer dialog/choices)
        int capNo = 1; // 1..10

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save Cap Bill PDF");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName(String.format("Bill-%s-%04d-%02d-AllCap.pdf",
                b.getMember().getCode(), b.getYear(), b.getMonth()));
        java.io.File f = fc.showSaveDialog(getScene().getWindow());
        if (f == null) return;

        try {
            // fillMissingDays=true → every date appears; false → only dates with entries
            new MemberBillPdfService().generate(b.getOutlet(), b.getMember(), b.getYear(), b.getMonth(), f, true);
            info("Saved:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            error("PDF failed: " + rootMsg(ex));
        }
    }

    private static void info(String m){ var a=new Alert(Alert.AlertType.INFORMATION,m,ButtonType.OK); a.setHeaderText(null); a.showAndWait();}
    private static void error(String m){ var a=new Alert(Alert.AlertType.ERROR,m,ButtonType.OK); a.setHeaderText("Error"); a.showAndWait();}
    private static String rootMsg(Throwable t){ Throwable x=t; while(x.getCause()!=null) x=x.getCause(); return x.getMessage(); }


    @Override public void actionSave() { /* no-op for now */ }
    @Override public void actionRefresh() { reload(); }
    @Override public void actionRecompute() {
        var b = table.getSelectionModel().getSelectedItem();
        if (b != null) { svc.upsertBill(outlet, b.getMember(), b.getYear(), b.getMonth()); reload(); }
    }
}
