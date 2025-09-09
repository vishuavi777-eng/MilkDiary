package com.rudrainfotech.milkdiary.ui;

import com.rudrainfotech.milkdiary.entity.Outlet;
import com.rudrainfotech.milkdiary.entity.Species;
import com.rudrainfotech.milkdiary.service.RateResolver;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RateCalculatorDialog extends GridPane {
    private final Outlet outlet;

    private final DatePicker date = new DatePicker(LocalDate.now());
    private final ComboBox<Species> species = new ComboBox<>();
    private final TextField fat = new TextField();
    private final TextField snf = new TextField();
    private final Button calc = new Button("Calculate");
    private final Label result = new Label("-");
    private final Label matched = new Label("-");

    public RateCalculatorDialog(Outlet outlet) {
        this.outlet = outlet;
        setHgap(10); setVgap(10); setPadding(new Insets(12));

        species.getItems().addAll(Species.values());
        species.getSelectionModel().selectFirst();
        fat.setPromptText("e.g. 4.0"); snf.setPromptText("e.g. 8.8");

        addRow(0, new Label("Date:"), date);
        addRow(1, new Label("Species:"), species);
        addRow(2, new Label("Fat:"), fat);
        addRow(3, new Label("SNF:"), snf);
        add(calc, 1, 4);
        addRow(5, new Label("Rate/L:"), result);
        addRow(6, new Label("Matched:"), matched);

        calc.setOnAction(e -> onCalc());
    }

    private void onCalc() {
        try {
            BigDecimal f = fat.getText().isBlank()? null : new BigDecimal(fat.getText().trim());
            BigDecimal s = snf.getText().isBlank()? null : new BigDecimal(snf.getText().trim());
            var rr = new RateResolver().resolve(outlet, date.getValue(), species.getValue(), f, s);
            result.setText(rr.rate.stripTrailingZeros().toPlainString());
            matched.setText(rr.item.getType()+" (sort#"+rr.item.getSortOrder()+") in plan "+rr.plan.getName());
        } catch (Exception ex) {
            result.setText("-");
            matched.setText("-");
            var a = new Alert(Alert.AlertType.ERROR, rootMsg(ex), ButtonType.OK);
            a.setHeaderText("No rate found"); a.showAndWait();
        }
    }

    private static String rootMsg(Throwable t){ Throwable x=t; while(x.getCause()!=null) x=x.getCause(); return x.getMessage(); }
}
