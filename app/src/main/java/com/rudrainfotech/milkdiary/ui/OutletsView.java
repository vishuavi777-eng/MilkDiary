package com.rudrainfotech.milkdiary.ui;

import com.rudrainfotech.milkdiary.entity.Outlet;
import com.rudrainfotech.milkdiary.i18n.I18n;
import com.rudrainfotech.milkdiary.service.AppSettingsService;
import com.rudrainfotech.milkdiary.service.OutletService;
import java.math.BigDecimal;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class OutletsView extends BorderPane {
    private final OutletService svc = new OutletService();
    private final AppSettingsService settings = new AppSettingsService();

    private final TableView<Outlet> table = new TableView<>();
    private final ObservableList<Outlet> rows = FXCollections.observableArrayList();
    private final Label activeLbl = new Label();

    public OutletsView() {
        Button add = new Button(I18n.t("button.add"));
        Button edit = new Button(I18n.t("button.edit"));
        Button del = new Button(I18n.t("button.delete"));
        Button setActive = new Button(I18n.t("button.setActive"));
        Button refresh = new Button(I18n.t("button.refresh"));

        add.setOnAction(e -> onAdd());
        edit.setOnAction(e -> onEdit());
        del.setOnAction(e -> onDelete());
        setActive.setOnAction(e -> onSetActive());
        refresh.setOnAction(e -> reload());

        HBox top = new HBox(10, new Label("Outlets"), new Separator(), add, edit, del, setActive, refresh, new Separator(), new Label("Active:"), activeLbl);
        top.setPadding(new Insets(10));
        setTop(top);

        TableColumn<Outlet, String> cName = new TableColumn<>("Name");
        cName.setCellValueFactory(new PropertyValueFactory<>("name"));
        cName.setPrefWidth(200);

        TableColumn<Outlet, String> cOwner = new TableColumn<>("Owner");
        cOwner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        cOwner.setPrefWidth(160);

        TableColumn<Outlet, String> cPhone = new TableColumn<>("Phone");
        cPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        cPhone.setPrefWidth(140);

        TableColumn<Outlet, String> cGstin = new TableColumn<>("GSTIN");
        cGstin.setCellValueFactory(new PropertyValueFactory<>("gstin"));
        cGstin.setPrefWidth(140);

        TableColumn<Outlet, BigDecimal> cCowSav = new TableColumn<>("Cow Saving/L");
        cCowSav.setCellValueFactory(new PropertyValueFactory<>("cowSavingPerLitre"));
        cCowSav.setPrefWidth(120);

        TableColumn<Outlet, BigDecimal> cBuffSav = new TableColumn<>("Buffalo Saving/L");
        cBuffSav.setCellValueFactory(new PropertyValueFactory<>("buffaloSavingPerLitre"));
        cBuffSav.setPrefWidth(140);

        table.getColumns().addAll(cName, cOwner, cPhone, cGstin, cCowSav, cBuffSav);
        table.setItems(rows);
        table.setPlaceholder(new Label("No outlets yet. Click Add."));
        setCenter(table);

        reload();
    }

    private void reload() {
        rows.setAll(svc.listAll());
        Long activeId = settings.getActiveOutletId();
        String label = "(none)";
        if (activeId != null) {
            for (Outlet o : rows) if (o.getId().equals(activeId)) { label = o.getName(); break; }
        }
        activeLbl.setText(label);
    }

    private void onAdd() {
        OutletDialog dlg = new OutletDialog(null);
        Outlet res = dlg.showAndWait().orElse(null);
        if (res == null) return;
        svc.save(res);
        // if no active outlet, set this one
        if (settings.getActiveOutletId() == null) settings.setActiveOutletId(res.getId());
        reload();
    }

    private void onEdit() {
        Outlet sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("Pick an outlet to edit."); return; }
        // make a copy
        Outlet copy = new Outlet();
        copy.setId(sel.getId());
        copy.setName(sel.getName());
        copy.setOwner(sel.getOwner());
        copy.setPhone(sel.getPhone());
        copy.setAddress(sel.getAddress());
        copy.setGstin(sel.getGstin());
        copy.setCowSavingPerLitre(sel.getCowSavingPerLitre());
        copy.setBuffaloSavingPerLitre(sel.getBuffaloSavingPerLitre());

        OutletDialog dlg = new OutletDialog(copy);
        Outlet res = dlg.showAndWait().orElse(null);
        if (res == null) return;
        svc.save(res);
        reload();
    }

    private void onDelete() {
        Outlet sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("Pick an outlet to delete."); return; }
        Long activeId = settings.getActiveOutletId();
        if (activeId != null && activeId.equals(sel.getId())) {
            warn("This outlet is the active one. Set another outlet active before deleting.");
            return;
        }
        var confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete outlet \""+sel.getName()+"\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm delete");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try { svc.delete(sel); reload(); }
            catch (Exception ex) { error("Could not delete.\n"+rootMsg(ex)); }
        }
    }

    private void onSetActive() {
        Outlet sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("Pick an outlet to set active."); return; }
        settings.setActiveOutletId(sel.getId());
        reload();
    }

    private static void warn(String msg){ var a=new Alert(Alert.AlertType.WARNING,msg,ButtonType.OK); a.setHeaderText(null); a.showAndWait();}
    private static void error(String msg){ var a=new Alert(Alert.AlertType.ERROR,msg,ButtonType.OK); a.setHeaderText("Error"); a.showAndWait();}
    private static String rootMsg(Throwable t){ Throwable x=t; while(x.getCause()!=null) x=x.getCause(); return x.getMessage(); }
}
