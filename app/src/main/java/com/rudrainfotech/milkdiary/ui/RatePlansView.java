package com.rudrainfotech.milkdiary.ui;

import com.rudrainfotech.milkdiary.entity.Outlet;
import com.rudrainfotech.milkdiary.entity.RatePlan;
import com.rudrainfotech.milkdiary.i18n.I18n;
import com.rudrainfotech.milkdiary.service.OutletService;
import com.rudrainfotech.milkdiary.service.RatePlanService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

public class RatePlansView extends BorderPane {
    private final OutletService outletSvc = new OutletService();
    private final RatePlanService svc = new RatePlanService();

    private final Outlet outlet;
    private final TableView<RatePlan> table = new TableView<>();
    private final ObservableList<RatePlan> rows = FXCollections.observableArrayList();

    public RatePlansView() {
        this.outlet = outletSvc.getActiveOutlet();
        Label title = new Label("Rate Plans — Outlet: " + (outlet!=null?outlet.getName():"(none)"));
        Button add = new Button(I18n.t("button.add"));
        Button edit = new Button(I18n.t("button.edit"));
        Button clone = new Button(I18n.t("button.clone"));
        Button del = new Button(I18n.t("button.delete"));
        Button calc = new Button(I18n.t("button.calculator"));
        Button refresh = new Button(I18n.t("button.refresh"));

        add.setOnAction(e -> onAdd());
        edit.setOnAction(e -> onEdit());
        clone.setOnAction(e -> onClone());
        del.setOnAction(e -> onDelete());
        calc.setOnAction(e -> onCalc());
        refresh.setOnAction(e -> reload());

        HBox top = new HBox(10, title, new Separator(), add, edit, clone, del, calc, refresh);
        top.setPadding(new Insets(10));
        setTop(top);

        TableColumn<RatePlan, String> cName = new TableColumn<>("Name");
        cName.setCellValueFactory(new PropertyValueFactory<>("name"));
        cName.setPrefWidth(200);

        TableColumn<RatePlan, LocalDate> cStart = new TableColumn<>("Start");
        cStart.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        cStart.setPrefWidth(120);

        TableColumn<RatePlan, LocalDate> cEnd = new TableColumn<>("End");
        cEnd.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        cEnd.setPrefWidth(120);

        table.getColumns().addAll(cName, cStart, cEnd);
        table.setItems(rows);
        table.setPlaceholder(new Label("No plans. Click Add."));
        setCenter(table);

        reload();
    }

    private void reload() {
        if (outlet==null) { rows.clear(); return; }
        List<RatePlan> list = svc.listByOutlet(outlet);
        rows.setAll(list);
    }

    private void onAdd() {
        RatePlan plan = new RatePlan();
        plan.setOutlet(outlet);
        Stage s = openEditor(plan, true);
        s.setOnHidden(ev -> reload());
    }

    private void onEdit() {
        RatePlan sel = table.getSelectionModel().getSelectedItem();
        if (sel==null) { warn("Pick a plan to edit."); return; }

        // Load an initialized plan with items before passing to the editor
        RatePlan full = svc.findWithItems(sel.getId());

        Stage s = openEditor(full, false);
        s.setOnHidden(ev -> reload());
    }

    private void onClone() {
        RatePlan sel = table.getSelectionModel().getSelectedItem();
        if (sel==null) { warn("Pick a plan to clone."); return; }
        TextInputDialog td = new TextInputDialog(sel.getName()+" (Copy)");
        td.setHeaderText("Clone Plan"); td.setContentText("New plan name:");
        var name = td.showAndWait().orElse(null);
        if (name==null || name.isBlank()) return;

        var dateDlg = new Dialog<LocalDate[]>();
        dateDlg.setTitle("Clone Dates");
        DatePicker start = new DatePicker(LocalDate.now());
        DatePicker end = new DatePicker();
        var ok = new ButtonType("Clone", ButtonBar.ButtonData.OK_DONE);
        dateDlg.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        var gp = new javafx.scene.layout.GridPane(); gp.setHgap(10); gp.setVgap(10); gp.setPadding(new Insets(10));
        gp.addRow(0, new Label("Start:"), start); gp.addRow(1, new Label("End:"), end);
        dateDlg.getDialogPane().setContent(gp);
        dateDlg.setResultConverter(btn -> btn==ok ? new LocalDate[]{start.getValue(), end.getValue()} : null);
        LocalDate[] res = dateDlg.showAndWait().orElse(null);
        if (res==null) return;

        try {
            svc.clonePlan(sel, name, res[0], res[1]);
            reload();
        } catch (Exception ex) { error(rootMsg(ex)); }
    }

    private void onDelete() {
        RatePlan sel = table.getSelectionModel().getSelectedItem();
        if (sel==null) { warn("Pick a plan to delete."); return; }
        var confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete plan \""+sel.getName()+"\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm delete");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try { svc.delete(sel); reload(); }
            catch (Exception ex) { error(rootMsg(ex)); }
        }
    }

    private void onCalc() {
        Stage s = new Stage();
        s.initModality(Modality.NONE);
        s.setTitle("Rate Calculator");
        s.setScene(new Scene(new RateCalculatorDialog(outlet), 420, 280));
        s.show();
    }

    private Stage openEditor(RatePlan plan, boolean isNew) {
        Stage s = new Stage();
        s.initModality(Modality.NONE);
        s.setTitle(isNew ? "New Rate Plan" : "Edit Rate Plan");
        s.setScene(new Scene(new RatePlanEditor(plan, isNew), 980, 620));
        s.show();
        return s;
    }

    private static void warn(String m){ var a=new Alert(Alert.AlertType.WARNING,m,ButtonType.OK); a.setHeaderText(null); a.showAndWait();}
    private static void error(String m){ var a=new Alert(Alert.AlertType.ERROR,m,ButtonType.OK); a.setHeaderText("Error"); a.showAndWait();}
    private static String rootMsg(Throwable t){ Throwable x=t; while(x.getCause()!=null) x=x.getCause(); return x.getMessage(); }
}
