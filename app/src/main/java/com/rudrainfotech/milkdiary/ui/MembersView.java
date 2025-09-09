package com.rudrainfotech.milkdiary.ui;

import com.rudrainfotech.milkdiary.entity.Member;
import com.rudrainfotech.milkdiary.entity.Outlet;
import com.rudrainfotech.milkdiary.entity.Species;
import com.rudrainfotech.milkdiary.i18n.I18n;
import com.rudrainfotech.milkdiary.service.MemberService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.util.List;

public class MembersView extends BorderPane {
    private final Outlet outlet;
    private final MemberService svc = new MemberService();

    private final TableView<Member> table = new TableView<>();
    private final ObservableList<Member> rows = FXCollections.observableArrayList();
    private final TextField search = new TextField();
    private final ComboBox<String> speciesFilter = new ComboBox<>();

    public MembersView(Outlet outlet) {
        this.outlet = outlet;

        Label title = new Label("Members — Outlet: " + outlet.getName());
        Button add = new Button(I18n.t("button.add"));
        Button edit = new Button(I18n.t("button.edit"));
        Button del = new Button(I18n.t("button.delete"));
        Button refresh = new Button(I18n.t("button.refresh"));

        // Search
        search.setPromptText("Search code/name...");
        search.textProperty().addListener((obs, o, n) -> applyFilter(n));

        // Species filter
        speciesFilter.getItems().addAll("All", "Cow", "Buffalo");
        speciesFilter.getSelectionModel().select("All");
        speciesFilter.valueProperty().addListener((obs, o, n) -> applyFilter(search.getText()));

        add.setOnAction(e -> onAdd());
        edit.setOnAction(e -> onEdit());
        del.setOnAction(e -> onDelete());
        refresh.setOnAction(e -> reload());

        HBox top = new HBox(10,
                title, new Separator(),
                add, edit, del, refresh,
                new Separator(),
                new Label("Species:"), speciesFilter,
                search
        );
        top.setPadding(new Insets(10));
        setTop(top);

        TableColumn<Member, String> cCode = new TableColumn<>("Code");
        cCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        cCode.setPrefWidth(120);

        TableColumn<Member, String> cName = new TableColumn<>("Name");
        cName.setCellValueFactory(new PropertyValueFactory<>("name"));
        cName.setPrefWidth(220);

        TableColumn<Member, String> cSpecies = new TableColumn<>("Species");
        cSpecies.setCellValueFactory(new PropertyValueFactory<>("species")); // shows COW/BUFFALO
        cSpecies.setPrefWidth(120);

        TableColumn<Member, String> cPhone = new TableColumn<>("Phone");
        cPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        cPhone.setPrefWidth(140);

        TableColumn<Member, Boolean> cActive = new TableColumn<>("Active");
        cActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        cActive.setPrefWidth(80);

        table.getColumns().addAll(cCode, cName, cSpecies, cPhone, cActive);
        table.setItems(rows);
        table.setPlaceholder(new Label("No members yet. Click Add."));
        setCenter(table);

        reload();
    }

    private void reload() {
        List<Member> all = svc.listByOutlet(outlet);
        rows.setAll(all);
        applyFilter(search.getText());
    }

    private void applyFilter(String q) {
        String needle = (q == null) ? "" : q.toLowerCase().trim();
        String f = speciesFilter.getValue();

        var filtered = rows.filtered(m -> {
            // species filter
            boolean speciesOk = true;
            if (f != null && !"All".equals(f)) {
                Species sp = m.getSpecies();
                if ("Cow".equals(f)) speciesOk = (sp == Species.COW);
                else if ("Buffalo".equals(f)) speciesOk = (sp == Species.BUFFALO);
            }

            if (!speciesOk) return false;

            // text search
            if (needle.isEmpty()) return true;
            return (m.getCode() != null && m.getCode().toLowerCase().contains(needle)) ||
                    (m.getName() != null && m.getName().toLowerCase().contains(needle)) ||
                    (m.getPhone() != null && m.getPhone().toLowerCase().contains(needle));
        });

        table.setItems(filtered);
    }

    private void onAdd() {
        MemberDialog dlg = new MemberDialog(outlet, null);
        Member res = dlg.showAndWait().orElse(null);
        if (res == null) return;
        try {
            svc.save(outlet, res);
            reload();
        } catch (Exception ex) {
            showError("Could not save member.\n" + rootMsg(ex));
        }
    }

    private void onEdit() {
        Member sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showWarn("Pick a member to edit."); return; }
        Member copy = new Member();
        copy.setOutlet(sel.getOutlet());
        copy.setCode(sel.getCode());
        copy.setName(sel.getName());
        copy.setPhone(sel.getPhone());
        copy.setAddress(sel.getAddress());
        copy.setActive(sel.isActive());
        copy.setSpecies(sel.getSpecies());
        copy.setId(sel.getId()); // keep id for merge

        MemberDialog dlg = new MemberDialog(outlet, copy);
        Member res = dlg.showAndWait().orElse(null);
        if (res == null) return;
        try {
            svc.save(outlet, res);
            reload();
        } catch (Exception ex) {
            showError("Could not update member.\n" + rootMsg(ex));
        }
    }

    private void onDelete() {
        Member sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showWarn("Pick a member to delete."); return; }
        var confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete member " + sel.getCode() + "?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm delete");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                svc.delete(sel);
                reload();
            } catch (Exception ex) {
                showError("Could not delete member.\n" + rootMsg(ex));
            }
        }
    }

    private static void showWarn(String msg) {
        var a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
    private static void showError(String msg) {
        var a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Error"); a.showAndWait();
    }
    private static String rootMsg(Throwable t) {
        Throwable x = t; while (x.getCause() != null) x = x.getCause(); return x.getMessage();
    }
}