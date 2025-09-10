package com.rudrainfotech.milkdiary.ui;

import com.rudrainfotech.milkdiary.entity.Outlet;
import com.rudrainfotech.milkdiary.service.MemberSavingService;
import com.rudrainfotech.milkdiary.service.MemberSavingService.MemberSaving;
import com.rudrainfotech.milkdiary.service.SavingPeriodService;
import com.rudrainfotech.milkdiary.service.SavingPeriodService;
import com.rudrainfotech.milkdiary.service.SavingPeriodService.SavingPeriod;
import com.rudrainfotech.milkdiary.service.OutletService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.converter.DoubleStringConverter;

import java.util.List;

/**
 * View for configuring saving periods and viewing member balances.
 */
public class FinancialPeriodView extends BorderPane implements StandardActions {

    private final SavingPeriodService periodSvc = new SavingPeriodService();
    private final MemberSavingService memberSvc = new MemberSavingService();
    private final OutletService outletSvc = new OutletService();

    private final DatePicker startDate = new DatePicker();
    private final DatePicker endDate = new DatePicker();
    private final TableView<MemberSaving> table = new TableView<>();
    private final ObservableList<MemberSaving> rows = FXCollections.observableArrayList();

    public FinancialPeriodView() {
        Button save = new Button("Save");
        Button refresh = new Button("Refresh");
        Button closeBtn = new Button("Close Period");

        save.setOnAction(e -> actionSave());
        refresh.setOnAction(e -> reload());
        closeBtn.setOnAction(e -> onClosePeriod());

        HBox top = new HBox(10,
                new Label("Start:"), startDate,
                new Label("End:"), endDate,
                save, refresh, closeBtn
        );
        top.setPadding(new Insets(10));
        setTop(top);

        TableColumn<MemberSaving, String> cMember = new TableColumn<>("Member");
        cMember.setCellValueFactory(ms -> new SimpleStringProperty(
                ms.getValue().getMember() == null ? "" : ms.getValue().getMember().getName()
        ));
        cMember.setPrefWidth(200);

        TableColumn<MemberSaving, Double> cBal = new TableColumn<>("Balance");
        cBal.setCellValueFactory(new PropertyValueFactory<>("balance"));
        cBal.setPrefWidth(120);

        TableColumn<MemberSaving, Double> cInit = new TableColumn<>("Initial Amount");
        cInit.setCellValueFactory(new PropertyValueFactory<>("initialAmount"));
        cInit.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        cInit.setOnEditCommit(e -> {
            MemberSaving ms = e.getRowValue();
            ms.setInitialAmount(e.getNewValue());
        });
        cInit.setPrefWidth(140);

        table.getColumns().addAll(cMember, cBal, cInit);
        table.setItems(rows);
        table.setEditable(true);
        table.setPlaceholder(new Label("No members"));
        setCenter(table);

        reload();
    }

    private void reload() {
        Outlet outlet = outletSvc.getActiveOutlet();
        SavingPeriod period = periodSvc.getCurrentPeriod(outlet);
        if (period != null) {
            startDate.setValue(period.getStartDate());
            endDate.setValue(period.getEndDate());
        }
        List<MemberSaving> list = memberSvc.listBalances(outlet);
        rows.setAll(list);
    }

    @Override
    public void actionSave() {
        Outlet outlet = outletSvc.getActiveOutlet();
        periodSvc.updatePeriod(outlet, startDate.getValue(), endDate.getValue());
        for (MemberSaving ms : rows) {
            memberSvc.updateInitialAmount(ms, ms.getInitialAmount());
        }
        Toast.success(this, "Saved");
    }

    private void onClosePeriod() {
        Outlet outlet = outletSvc.getActiveOutlet();
        periodSvc.closePeriod(outlet);
        memberSvc.disburseForOutlet(outlet);
        periodSvc.initNextPeriod(outlet);
        reload();
        Toast.info(this, "Period closed");
    }
}