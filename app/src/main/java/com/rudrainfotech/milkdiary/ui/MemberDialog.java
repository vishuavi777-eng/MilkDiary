package com.rudrainfotech.milkdiary.ui;

import com.rudrainfotech.milkdiary.entity.Member;
import com.rudrainfotech.milkdiary.entity.Outlet;
import com.rudrainfotech.milkdiary.entity.Species;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class MemberDialog extends Dialog<Member> {
    private final TextField code = new TextField();
    private final TextField name = new TextField();
    private final TextField phone = new TextField();
    private final TextField address = new TextField();
    private final CheckBox active = new CheckBox("Active");
    private final ComboBox<Species> speciesBox = new ComboBox<>();

    public MemberDialog(Outlet outlet, Member existing) {
        setTitle(existing == null ? "Add Member" : "Edit Member");

        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        // Species choices
        speciesBox.getItems().setAll(Species.values()); // COW, BUFFALO
        speciesBox.getSelectionModel().select(Species.COW);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new Insets(10));
        int r = 0;
        gp.addRow(r++, new Label("Code*:"), code);
        gp.addRow(r++, new Label("Name*:"), name);
        gp.addRow(r++, new Label("Species*:"), speciesBox); // <-- NEW field
        gp.addRow(r++, new Label("Phone:"), phone);
        gp.addRow(r++, new Label("Address:"), address);
        gp.add(active, 1, r++);

        getDialogPane().setContent(gp);

        if (existing != null) {
            code.setText(existing.getCode());
            name.setText(existing.getName());
            phone.setText(existing.getPhone());
            address.setText(existing.getAddress());
            active.setSelected(existing.isActive());
            if (existing.getSpecies() != null) {
                speciesBox.getSelectionModel().select(existing.getSpecies());
            }
        } else {
            active.setSelected(true);
        }

        Node saveBtn = getDialogPane().lookupButton(saveBtnType);
        saveBtn.disableProperty().bind(
                code.textProperty().isEmpty().or(name.textProperty().isEmpty())
        );

        setResultConverter(btn -> {
            if (btn == saveBtnType) {
                Member m = (existing == null) ? new Member() : existing;
                m.setOutlet(outlet);
                m.setCode(code.getText().trim());
                m.setName(name.getText().trim());
                m.setPhone(phone.getText().trim());
                m.setAddress(address.getText().trim());
                m.setActive(active.isSelected());
                // NEW: persist species
                Species sp = speciesBox.getValue() == null ? Species.COW : speciesBox.getValue();
                m.setSpecies(sp);
                return m;
            }
            return null;
        });
    }
}


//package com.rudrainfotech.milkdiary.ui;
//
//import com.rudrainfotech.milkdiary.entity.Member;
//import com.rudrainfotech.milkdiary.entity.Outlet;
//import javafx.geometry.Insets;
//import javafx.scene.Node;
//import javafx.scene.control.*;
//import javafx.scene.layout.GridPane;
//
//public class MemberDialog extends Dialog<Member> {
//    private final TextField code = new TextField();
//    private final TextField name = new TextField();
//    private final TextField phone = new TextField();
//    private final TextField address = new TextField();
//    private final CheckBox active = new CheckBox("Active");
//
//    public MemberDialog(Outlet outlet, Member existing) {
//        setTitle(existing == null ? "Add Member" : "Edit Member");
//
//        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
//        getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);
//
//        GridPane gp = new GridPane();
//        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new Insets(10));
//        gp.addRow(0, new Label("Code*:"), code);
//        gp.addRow(1, new Label("Name*:"), name);
//        gp.addRow(2, new Label("Phone:"), phone);
//        gp.addRow(3, new Label("Address:"), address);
//        gp.add(active, 1, 4);
//
//        getDialogPane().setContent(gp);
//
//        if (existing != null) {
//            code.setText(existing.getCode());
//            name.setText(existing.getName());
//            phone.setText(existing.getPhone());
//            address.setText(existing.getAddress());
//            active.setSelected(existing.isActive());
//        } else {
//            active.setSelected(true);
//        }
//
//        Node saveBtn = getDialogPane().lookupButton(saveBtnType);
//        saveBtn.disableProperty().bind(
//            code.textProperty().isEmpty().or(name.textProperty().isEmpty())
//        );
//
//        setResultConverter(btn -> {
//            if (btn == saveBtnType) {
//                Member m = (existing == null) ? new Member() : existing;
//                m.setOutlet(outlet);
//                m.setCode(code.getText().trim());
//                m.setName(name.getText().trim());
//                m.setPhone(phone.getText().trim());
//                m.setAddress(address.getText().trim());
//                m.setActive(active.isSelected());
//                return m;
//            }
//            return null;
//        });
//    }
//}
