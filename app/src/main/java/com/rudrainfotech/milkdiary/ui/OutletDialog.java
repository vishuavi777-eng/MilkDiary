package com.rudrainfotech.milkdiary.ui;

import com.rudrainfotech.milkdiary.entity.Outlet;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import java.math.BigDecimal;

public class OutletDialog extends Dialog<Outlet> {
    private final TextField name = new TextField();
    private final TextField owner = new TextField();
    private final TextField phone = new TextField();
    private final TextField address = new TextField();
    private final TextField gstin = new TextField();
    private final TextField cowSaving = new TextField();
    private final TextField buffaloSaving = new TextField();

    public OutletDialog(Outlet existing) {
        setTitle(existing == null ? "Add Outlet" : "Edit Outlet");
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new Insets(12));
        gp.addRow(0, new Label("Name*:"), name);
        gp.addRow(1, new Label("Owner:"), owner);
        gp.addRow(2, new Label("Phone:"), phone);
        gp.addRow(3, new Label("Address:"), address);
        gp.addRow(4, new Label("GSTIN:"), gstin);
        gp.addRow(5, new Label("Cow Saving/L:"), cowSaving);
        gp.addRow(6, new Label("Buffalo Saving/L:"), buffaloSaving);
        getDialogPane().setContent(gp);

        if (existing != null) {
            name.setText(existing.getName());
            owner.setText(existing.getOwner());
            phone.setText(existing.getPhone());
            address.setText(existing.getAddress());
            gstin.setText(existing.getGstin());
            if (existing.getCowSavingPerLitre() != null)
                cowSaving.setText(existing.getCowSavingPerLitre().toPlainString());
            if (existing.getBuffaloSavingPerLitre() != null)
                buffaloSaving.setText(existing.getBuffaloSavingPerLitre().toPlainString());
        }

        Node saveBtn = getDialogPane().lookupButton(saveType);
        saveBtn.disableProperty().bind(name.textProperty().isEmpty());

        setResultConverter(btn -> {
            if (btn == saveType) {
                Outlet o = (existing == null) ? new Outlet() : existing;
                o.setName(name.getText().trim());
                o.setOwner(owner.getText().trim());
                o.setPhone(phone.getText().trim());
                o.setAddress(address.getText().trim());
                o.setGstin(gstin.getText().trim());
                o.setCowSavingPerLitre(cowSaving.getText().trim().isEmpty()
                        ? null : new BigDecimal(cowSaving.getText().trim()));
                o.setBuffaloSavingPerLitre(buffaloSaving.getText().trim().isEmpty()
                        ? null : new BigDecimal(buffaloSaving.getText().trim()));
                return o;
            }
            return null;
        });
    }
}
