package com.rudrainfotech.milkdiary.ui;

import com.rudrainfotech.milkdiary.entity.AdjustmentType;
import com.rudrainfotech.milkdiary.entity.MonthlyBill;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

public class AdjustmentDialog {

    @FunctionalInterface
    public interface Saver {
        void save(AdjustmentType type, BigDecimal amount, String remark);
    }

    public static void show(MonthlyBill bill, Saver saver) {
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Add Adjustment — " + bill.getMember().getCode());

        ButtonType addType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CLOSE);

        ComboBox<AdjustmentType> type = new ComboBox<>();
        type.getItems().addAll(AdjustmentType.values());
        type.getSelectionModel().select(AdjustmentType.OTHER);

        TextField amt = new TextField();
        TextField remark = new TextField();

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new Insets(12));
        gp.addRow(0, new Label("Type:"), type);
        gp.addRow(1, new Label("Amount:"), amt);
        gp.addRow(2, new Label("Remark:"), remark);

        d.getDialogPane().setContent(gp);

        d.setResultConverter(btn -> {
            if (btn == addType) {
                try {
                    BigDecimal a = new BigDecimal(amt.getText().trim());
                    saver.save(type.getValue(), a, remark.getText().trim());
                } catch (Exception ex) {
                    var err = new Alert(Alert.AlertType.ERROR, "Invalid amount.", ButtonType.OK);
                    err.setHeaderText("Error"); err.showAndWait();
                }
            }
            return null;
        });

        d.showAndWait();
    }
}
