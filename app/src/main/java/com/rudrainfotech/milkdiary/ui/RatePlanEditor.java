package com.rudrainfotech.milkdiary.ui;

import com.rudrainfotech.milkdiary.entity.*;
import com.rudrainfotech.milkdiary.service.RatePlanService;
import com.rudrainfotech.milkdiary.service.RatePlanValidator;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RatePlanEditor extends BorderPane {
    private final RatePlanService svc = new RatePlanService();
    private final RatePlan plan;
    private final boolean isNew;

    private final TextField name = new TextField();
    private final DatePicker start = new DatePicker(LocalDate.now());
    private final DatePicker end = new DatePicker();

    private final TabPane tabs = new TabPane();

    public RatePlanEditor(RatePlan plan, boolean isNew) {
        this.plan = plan; this.isNew = isNew;

        // header
        GridPane header = new GridPane();
        header.setHgap(10); header.setVgap(10); header.setPadding(new Insets(10));
        header.addRow(0, new Label("Name*:"), name);
        header.addRow(1, new Label("Start*:"), start);
        header.addRow(2, new Label("End:"), end);

        if (!isNew) {
            name.setText(plan.getName());
            start.setValue(plan.getStartDate());
            end.setValue(plan.getEndDate());
        }

        setTop(header);

        // tabs per species
        tabs.getTabs().add(makeSpeciesTab(Species.COW));
        tabs.getTabs().add(makeSpeciesTab(Species.BUFFALO));
        setCenter(tabs);

        // footer
        Button save = new Button("Save");
        Button cancel = new Button("Close");
        HBox footer = new HBox(10, save, cancel);
        footer.setPadding(new Insets(10));
        setBottom(footer);

        save.setOnAction(e -> onSave());
        cancel.setOnAction(e -> ((javafx.stage.Stage)getScene().getWindow()).close());
    }

    private Tab makeSpeciesTab(Species sp) {
        TableView<RateItem> table = new TableView<>();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        var cType = new TableColumn<RateItem, String>("Type");
        cType.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getType().name()));
        cType.setMinWidth(90);

        // slab columns
        var bdConv = bigDecConverter();
        var cMinFat = decCol("MinFat", "minFat", bdConv);
        var cMaxFat = decCol("MaxFat", "maxFat", bdConv);
        var cMinSnf = decCol("MinSNF", "minSnf", bdConv);
        var cMaxSnf = decCol("MaxSNF", "maxSnf", bdConv);
        var cRate   = decCol("Rate/L", "ratePerLitre", bdConv);

        // formula columns
        var cBase   = decCol("Base", "base", bdConv);
        var cPerFat = decCol("PerFat", "perFat", bdConv);
        var cPerSnf = decCol("PerSNF", "perSnf", bdConv);

        var cOrder = new TableColumn<RateItem, Integer>("#");
        cOrder.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getSortOrder()));
        cOrder.setMinWidth(40);

        table.getColumns().addAll(cOrder, cType, cMinFat, cMaxFat, cMinSnf, cMaxSnf, cRate, cBase, cPerFat, cPerSnf);

        // rows for this species
        if (plan.getItems() == null) {
            plan.setItems(new java.util.ArrayList<>()); // add setter if missing
        }

        table.setItems(javafx.collections.FXCollections.observableArrayList(
            plan.getItems().stream()
                .filter(it -> it.getSpecies()==sp)
                .sorted(Comparator.comparing(it -> it.getSortOrder()==null?0:it.getSortOrder()))
                .collect(Collectors.toList())
        ));

        // toolbar
        Button addSlab = new Button("Add Slab");
        Button addFormula = new Button("Add Formula");
        Button del = new Button("Delete");
        Button up = new Button("Up");
        Button dn = new Button("Down");
        Button overlap = new Button("Check Overlaps");

        addSlab.setOnAction(e -> {
            RateItem it = new RateItem();
            it.setPlan(plan); it.setSpecies(sp); it.setType(RateType.SLAB);
            it.setSortOrder(table.getItems().size());
            table.getItems().add(it); plan.getItems().add(it);
        });
        addFormula.setOnAction(e -> {
            RateItem it = new RateItem();
            it.setPlan(plan); it.setSpecies(sp); it.setType(RateType.FORMULA);
            it.setSortOrder(table.getItems().size());
            table.getItems().add(it); plan.getItems().add(it);
        });
        del.setOnAction(e -> {
            var sel = table.getSelectionModel().getSelectedItem();
            if (sel==null) return;
            table.getItems().remove(sel);
            plan.getItems().remove(sel);
            renumber(table.getItems());
        });
        up.setOnAction(e -> moveRow(table, -1));
        dn.setOnAction(e -> moveRow(table, +1));
        overlap.setOnAction(e -> {
            var errs = RatePlanValidator.findOverlaps(clonePlanForCheck(sp), sp);
            if (errs.isEmpty()) info("No slab overlaps found for " + sp);
            else error(String.join("\n", errs));
        });

        HBox bar = new HBox(10, addSlab, addFormula, del, up, dn, overlap);
        bar.setPadding(new Insets(10));

        VBox box = new VBox(bar, table);
        Tab tab = new Tab(sp.name(), box);
        tab.setClosable(false);
        return tab;
    }

    private RatePlan clonePlanForCheck(Species sp) {
        RatePlan tmp = new RatePlan();
        tmp.setOutlet(plan.getOutlet());
        tmp.setName(plan.getName()); tmp.setStartDate(plan.getStartDate()); tmp.setEndDate(plan.getEndDate());
        // only species rows
        tmp.getItems().addAll(plan.getItems().stream().filter(it -> it.getSpecies()==sp).toList());
        return tmp;
    }

    private void moveRow(TableView<RateItem> table, int dir) {
        int idx = table.getSelectionModel().getSelectedIndex();
        if (idx<0) return;
        int j = idx + dir;
        if (j<0 || j>=table.getItems().size()) return;
        var list = table.getItems();
        var a = list.get(idx); var b = list.get(j);
        int sa = a.getSortOrder()==null?idx:a.getSortOrder();
        int sb = b.getSortOrder()==null?j:b.getSortOrder();
        a.setSortOrder(sb); b.setSortOrder(sa);
        list.set(idx, b); list.set(j, a);
        renumber(list);
        table.getSelectionModel().select(j);
    }

    private void renumber(List<RateItem> list) {
        for (int i=0;i<list.size();i++) list.get(i).setSortOrder(i);
    }

    private void onSave() {
        if (name.getText().isBlank()) { error("Name is required"); return; }
        plan.setName(name.getText().trim());
        plan.setStartDate(start.getValue());
        plan.setEndDate(end.getValue());

        try {
            svc.save(plan);
            info("Saved.");
            ((javafx.stage.Stage) getScene().getWindow()).close();
        } catch (Exception ex) {
            error(rootMsg(ex));
        }
    }

    private static TableColumn<RateItem, BigDecimal> decCol(String title, String prop, StringConverter<BigDecimal> conv) {
        TableColumn<RateItem, BigDecimal> c = new TableColumn<>(title);
        c.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(prop));
        c.setCellFactory(col -> {
            TextFieldTableCell<RateItem, BigDecimal> cell = new TextFieldTableCell<>(conv);
            return cell;
        });
        c.setOnEditCommit(ev -> {
            RateItem row = ev.getRowValue();
            try {
                var val = ev.getNewValue();
                switch (prop) {
                    case "minFat" -> row.setMinFat(val);
                    case "maxFat" -> row.setMaxFat(val);
                    case "minSnf" -> row.setMinSnf(val);
                    case "maxSnf" -> row.setMaxSnf(val);
                    case "ratePerLitre" -> row.setRatePerLitre(val);
                    case "base" -> row.setBase(val);
                    case "perFat" -> row.setPerFat(val);
                    case "perSnf" -> row.setPerSnf(val);
                }
            } catch (Exception ex) { /* ignore, converter ensures validity */ }
        });
        c.setMinWidth(80);
        return c;
    }

    private static StringConverter<BigDecimal> bigDecConverter() {
        return new StringConverter<>() {
            @Override public String toString(BigDecimal o) { return o==null? "" : o.stripTrailingZeros().toPlainString(); }
            @Override public BigDecimal fromString(String s) {
                if (s==null) return null;
                String t = s.trim(); if (t.isEmpty()) return null;
                return new BigDecimal(t);
            }
        };
    }

    private static void info(String m){ var a=new Alert(Alert.AlertType.INFORMATION,m,ButtonType.OK); a.setHeaderText(null); a.showAndWait();}
    private static void error(String m){ var a=new Alert(Alert.AlertType.ERROR,m,ButtonType.OK); a.setHeaderText("Error"); a.showAndWait();}
    private static String rootMsg(Throwable t){ Throwable x=t; while(x.getCause()!=null) x=x.getCause(); return x.getMessage(); }
}
