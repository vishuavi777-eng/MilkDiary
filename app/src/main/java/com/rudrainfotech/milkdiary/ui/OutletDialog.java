package com.rudrainfotech.milkdiary.ui;

import com.rudrainfotech.milkdiary.entity.Outlet;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class OutletDialog extends Dialog<Outlet> {
    private final TextField name = new TextField();
    private final TextField owner = new TextField();
    private final TextField phone = new TextField();
    private final TextField address = new TextField();
    private final TextField gstin = new TextField();

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
        getDialogPane().setContent(gp);

        if (existing != null) {
            name.setText(existing.getName());
            owner.setText(existing.getOwner());
            phone.setText(existing.getPhone());
            address.setText(existing.getAddress());
            gstin.setText(existing.getGstin());
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
                return o;
            }
            return null;
        });
    }
}
