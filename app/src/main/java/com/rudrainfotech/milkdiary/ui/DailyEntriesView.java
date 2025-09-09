package com.rudrainfotech.milkdiary.ui;

import com.rudrainfotech.milkdiary.entity.*;
import com.rudrainfotech.milkdiary.i18n.I18n;
import com.rudrainfotech.milkdiary.service.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class DailyEntriesView extends BorderPane implements StandardActions {

    // Edit order: qty -> fat -> snf
    private java.util.List<TableColumn<DailyMilkEntry, ?>> editOrder;

    private final OutletService outletSvc = new OutletService();
    private final DailyEntryService svc = new DailyEntryService();
    private final MemberService memberSvc = new MemberService();

    private final Outlet outlet;

    private final Label lockLbl = new Label();
    private final CheckBox overrideCk = new CheckBox("Override lock (admin)");

    private final Button saveAllBtn   = new Button(I18n.t("button.saveAll"));
    private final Button deleteBtn    = new Button(I18n.t("button.deleteEntry"));
    private final Button recomputeBtn = new Button(I18n.t("button.recomputeRates"));

    private final DatePicker date = new DatePicker(LocalDate.now());
    private final ComboBox<SessionType> session = new ComboBox<>();
    private final ComboBox<String> speciesFilter = new ComboBox<>();

    private final TableView<DailyMilkEntry> table = new TableView<>();
    private final ObservableList<DailyMilkEntry> rows = FXCollections.observableArrayList();

    // status bar
    private final Label rowsLbl = new Label("Rows: 0");
    private final Label qtyLbl  = new Label("Σ Qty: 0.000 L");
    private final Label amtLbl  = new Label("Σ Amount: 0.00");

    // editable column order (for navigation)
    private List<TableColumn<DailyMilkEntry, ?>> editableOrderRef = List.of();

    private final StringConverter<BigDecimal> decConv = new StringConverter<>() {
        @Override public String toString(BigDecimal o) { return o==null ? "" : o.stripTrailingZeros().toPlainString(); }
        @Override public BigDecimal fromString(String s) {
            if (s==null) return null; String t = s.trim(); if (t.isEmpty()) return null; return new BigDecimal(t);
        }
    };

    public DailyEntriesView() {
        this.outlet = outletSvc.getActiveOutlet();

        // Top bar
        session.getItems().addAll(SessionType.values()); session.getSelectionModel().select(SessionType.AM);
        speciesFilter.getItems().addAll("All","Cow","Buffalo");
        speciesFilter.getSelectionModel().select("All");

        Button load = new Button( I18n.t("button.loadMembers"));
        Button refresh = new Button(I18n.t("button.refresh"));

        overrideCk.setSelected(false);
        overrideCk.selectedProperty().addListener((obs, was, now) -> {
            if (now) {
                TextInputDialog d = new TextInputDialog();
                d.setTitle("Admin PIN");
                d.setHeaderText("Enter Admin PIN to override lock");
                d.setContentText("PIN:");
                d.getEditor().setText("");
                d.getEditor().setPromptText("PIN");
                var res = d.showAndWait();
                boolean ok = res.isPresent() && new AppSettingsService().verifyAdminPin(res.get());
                if (!ok) {
                    overrideCk.setSelected(false);
                    showError("Invalid PIN.");
                }
            }
            refreshLockUI();
        });

        load.setOnAction(e -> loadMembersAsRows());
        refresh.setOnAction(e -> reload());
        recomputeBtn.setOnAction(e -> {
            rows.forEach(r -> svc.computeRateAndAmount(outlet, r));
            table.refresh();
            updateTotals();
            Toast.info(this, "Recomputed rates");
        });
        saveAllBtn.setOnAction(e -> onSave());
        deleteBtn.setOnAction(e -> onDeleteRow());

        date.setFocusTraversable(false);
        session.setFocusTraversable(false);
        speciesFilter.setFocusTraversable(false);
        overrideCk.setFocusTraversable(false);

        load.setFocusTraversable(false);
        refresh.setFocusTraversable(false);
        recomputeBtn.setFocusTraversable(false);
        saveAllBtn.setFocusTraversable(false);
        deleteBtn.setFocusTraversable(false);


        HBox row1 = new HBox(10,
            date, session, speciesFilter,
            load, refresh, recomputeBtn,
            saveAllBtn, deleteBtn
        );
        HBox row2 = new HBox(10, new Label("Lock:"), lockLbl, overrideCk);
        VBox top = new VBox(8, row1, row2);

        top.setPadding(new Insets(10));
        setTop(top);

        // Table
        table.setEditable(true);
        table.setItems(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Click Load Members or Refresh"));
        table.getSelectionModel().setCellSelectionEnabled(true); // cell-level ops

        TableColumn<DailyMilkEntry, String> cCode = new TableColumn<>("Code");
        cCode.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getMember().getCode()));
        cCode.setMinWidth(100);
        cCode.setEditable(false);

        TableColumn<DailyMilkEntry, String> cName = new TableColumn<>("Name");
        cName.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getMember().getName()));
        cName.setMinWidth(200);
        cName.setEditable(false);

        // Editable numeric columns with ↑/↓ stepping and enter-to-next
        var qtyCol = decEditableCol("Qty(L)", "qtyLitre", new BigDecimal("0.50"), new BigDecimal("1.00"));
        var fatCol = decEditableCol("Fat%",  "fatPct",   new BigDecimal("0.10"), new BigDecimal("0.50"));
        var snfCol = decEditableCol("SNF%",  "snfPct",   new BigDecimal("0.10"), new BigDecimal("0.50"));

        this.editOrder = java.util.List.of(qtyCol, fatCol, snfCol);

        TableColumn<DailyMilkEntry, BigDecimal> rateCol = new TableColumn<>("Rate/L");
        rateCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("rateApplied"));
        rateCol.setEditable(false);

        TableColumn<DailyMilkEntry, BigDecimal> amtCol = new TableColumn<>("Amount");
        amtCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("amount"));
        amtCol.setEditable(false);

        TableColumn<DailyMilkEntry, String> notes = new TableColumn<>("Notes");
        notes.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("notes"));
        notes.setCellFactory(TextFieldTableCell.forTableColumn());
        notes.setOnEditCommit(ev -> { ev.getRowValue().setNotes(ev.getNewValue()); updateTotals(); });

        table.getColumns().addAll(cCode, cName, qtyCol, fatCol, snfCol, rateCol, amtCol, notes);

        // set editable order (for nav)
        editableOrderRef = List.of(qtyCol, fatCol, snfCol, notes);

        // Spreadsheet-like nav & clipboard (+ refined Enter behavior)
//        installTableKeyNav(table, editableOrderRef);//TODO IMPORTANT remover this comment

        setCenter(table);
        installTableKeyNav();

        // Status bar
        HBox status = new HBox(18, rowsLbl, qtyLbl, amtLbl);
        status.setPadding(new Insets(8, 12, 10, 12));
        setBottom(status);

        // initial load
        reload();
        refreshLockUI();
        updateTotals();

        // reacting controls
        date.valueProperty().addListener((o, ov, nv) -> { reload(); refreshLockUI(); });
        session.valueProperty().addListener((o, ov, nv) -> reload());
        speciesFilter.valueProperty().addListener((o, ov, nv) -> reload());

        // list changes -> recompute totals
        rows.addListener((ListChangeListener<DailyMilkEntry>) c -> updateTotals());

        // Global accelerators
        sceneProperty().addListener((obs, oldSc, sc) -> {
            if (sc == null) return;
            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), saveAllBtn::fire);
            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN), recomputeBtn::fire);
            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN), this::loadMembersAsRows);
            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN), deleteBtn::fire);
            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), this::reload);

            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.LEFT,  KeyCombination.SHORTCUT_DOWN),
                    () -> date.setValue(date.getValue().minusDays(1)));
            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.SHORTCUT_DOWN),
                    () -> date.setValue(date.getValue().plusDays(1)));

            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.SHORTCUT_DOWN),
                    () -> { session.getSelectionModel().select(SessionType.AM); reload(); });
            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.SHORTCUT_DOWN),
                    () -> { session.getSelectionModel().select(SessionType.PM); reload(); });

            // species filter
            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN),
                    () -> speciesFilter.getSelectionModel().select("All"));
            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN),
                    () -> speciesFilter.getSelectionModel().select("Cow"));
            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN),
                    () -> speciesFilter.getSelectionModel().select("Buffalo"));

            // fill-down
            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.J, KeyCombination.SHORTCUT_DOWN),
                    () -> fillDown(table));
        });
    }

    /** Editable BigDecimal column with ↑/↓ stepping and enter-to-next. */
    private TableColumn<DailyMilkEntry, BigDecimal> decEditableCol(
            String title, String prop, BigDecimal step, BigDecimal stepCoarse) {

        TableColumn<DailyMilkEntry, BigDecimal> c = new TableColumn<>(title);
        c.setEditable(true);
        c.setMinWidth(90);
        c.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(prop));

        c.setCellFactory(col -> new TextFieldTableCell<DailyMilkEntry, BigDecimal>(decConv) {
            @Override public void startEdit() {
                super.startEdit();
                // Ensure caret appears in editor each time edit starts
                javafx.scene.Node g = getGraphic();
                if (g instanceof javafx.scene.control.TextField tf) {
                    tf.requestFocus();
                    tf.selectAll();
                    javafx.application.Platform.runLater(() -> {
                        tf.requestFocus();
                        tf.selectAll();
                    });
                    // key handling for editor
                    tf.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
                        switch (e.getCode()) {
                            case ENTER -> {
                                BigDecimal v = null;
                                try { v = decConv.fromString(tf.getText()); } catch (Exception ignore) {}
                                commitEdit(v);
                                e.consume();
                                // After commit, advance to next cell and enter edit there
                                javafx.application.Platform.runLater(() -> moveToNextCell(true));
                            }
                            case TAB -> { e.consume(); commitEdit(decConv.fromString(tf.getText())); javafx.application.Platform.runLater(() -> moveToNextCell(!e.isShiftDown())); }
                            case RIGHT -> { e.consume(); commitEdit(decConv.fromString(tf.getText())); javafx.application.Platform.runLater(() -> moveToNextCell(true)); }
                            case LEFT -> { e.consume(); commitEdit(decConv.fromString(tf.getText())); javafx.application.Platform.runLater(() -> moveToNextCell(false)); }
                            default -> { /* let typing through */ }
                        }
                    });
                }
            }
        });

        c.setOnEditCommit(ev -> {
            DailyMilkEntry row = ev.getRowValue();
            switch (prop) {
                case "qtyLitre" -> row.setQtyLitre(ev.getNewValue());
                case "fatPct"   -> row.setFatPct(ev.getNewValue());
                case "snfPct"   -> row.setSnfPct(ev.getNewValue());
            }
            // ensure keys and recompute
            try {
                if (row.getDate()==null) row.setDate(date.getValue());
                if (row.getSession()==null) row.setSession(session.getValue());
                if (row.getOutlet()==null) row.setOutlet(outlet);
                row.setSpecies(row.getMember().getSpecies()); // enforce from member
                svc.computeRateAndAmount(outlet, row);
                table.refresh();
                Toast.info(this, "Recomputed rates");
            } catch (Exception ex) {
                showError("No matching rate: " + rootMsg(ex));
                row.setRateApplied(null); row.setAmount(null);
                table.refresh();
            }
            updateTotals();
        });

        return c;
    }

    private void reload() {
        if (outlet == null) { rows.clear(); updateTotals(); return; }

        String f = speciesFilter.getValue();
        Species sp = switch (f) {
            case "Cow" -> Species.COW;
            case "Buffalo" -> Species.BUFFALO;
            default -> null; // All
        };

        var list = (sp == null)
                ? svc.list(outlet, date.getValue(), session.getValue())
                : svc.list(outlet, date.getValue(), session.getValue(), sp);

        rows.setAll(list);
        updateTotals();
    }

    private void refreshLockUI() {
        if (outlet == null || date.getValue()==null) {
            lockLbl.setText("No outlet");
            table.setEditable(false);
            saveAllBtn.setDisable(true);
            deleteBtn.setDisable(true);
            return;
        }

        int y = date.getValue().getYear();
        int m = date.getValue().getMonthValue();
        int cap = BillLockService.capNoFor(date.getValue());

        boolean locked = new BillLockService().isCapLocked(outlet.getId(), y, m, cap);
        lockLbl.setText(locked ? "LOCKED (cap " + cap + ")" : "Unlocked");

        boolean disableWrites = locked && !overrideCk.isSelected();
        table.setEditable(!disableWrites);
        saveAllBtn.setDisable(disableWrites);
        deleteBtn.setDisable(disableWrites);
    }

    private void loadMembersAsRows() {
        Map<Long, DailyMilkEntry> byMember = rows.stream().collect(Collectors.toMap(
                r -> r.getMember().getId(), r -> r, (a,b)->a, LinkedHashMap::new));

        var members = memberSvc.listByOutlet(outlet).stream()
                .filter(Member::isActive)
                .filter(m -> {
                    String f = speciesFilter.getValue();
                    return "All".equals(f) ||
                            ("Cow".equals(f) && m.getSpecies()==Species.COW) ||
                            ("Buffalo".equals(f) && m.getSpecies()==Species.BUFFALO);
                })
                .sorted(Comparator.comparing(Member::getCode))
                .toList();

        for (Member m : members) {
            if (!byMember.containsKey(m.getId())) {
                DailyMilkEntry e = new DailyMilkEntry();
                e.setOutlet(outlet);
                e.setMember(m);
                e.setDate(date.getValue());
                e.setSession(session.getValue());
                e.setSpecies(m.getSpecies());
                byMember.put(m.getId(), e);
            }
        }
        rows.setAll(byMember.values());
        table.refresh();
        updateTotals();
    }

    private void onSave() {
        try {
            for (DailyMilkEntry r : rows) {
                r.setOutlet(outlet);
                r.setDate(date.getValue());
                r.setSession(session.getValue());
                r.setSpecies(r.getMember().getSpecies()); // enforce
            }
            svc.upsertAll(outlet, date.getValue(), session.getValue(), rows, overrideCk.isSelected());
            Toast.success(this, "Saved");
            reload();
            refreshLockUI();
        } catch (Exception ex) {
            showError("Save failed:\n" + rootMsg(ex));
        }
    }

    private void onDeleteRow() {
        DailyMilkEntry sel = table.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getId()==null) return;

        var confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this entry?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                svc.deleteEntry(outlet, sel, overrideCk.isSelected());
                Toast.success(this, "Deleted");
                reload();
                refreshLockUI();
            } catch (Exception ex) {
                showError("Delete failed:\n" + rootMsg(ex));
            }
        }
    }

    // ---------- status/totals ----------
    private void updateTotals() {
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal amt = BigDecimal.ZERO;
        int n = rows.size();

        for (DailyMilkEntry e : rows) {
            if (e.getQtyLitre()!=null) qty = qty.add(e.getQtyLitre());
            if (e.getAmount()!=null)   amt = amt.add(e.getAmount());
        }
        qty = qty.setScale(3, RoundingMode.HALF_UP);
        amt = amt.setScale(2, RoundingMode.HALF_UP);

        rowsLbl.setText("Rows: " + n);
        qtyLbl.setText("Σ Qty: " + qty.toPlainString() + " L");
        amtLbl.setText("Σ Amount: " + amt.toPlainString());
    }

    // normalize focus to the first editable column if user is on Code/Name/etc.
    private TableColumn<DailyMilkEntry, ?> normalizeToEditable(TableColumn<DailyMilkEntry, ?> col) {
        if (col == null || editOrder == null || editOrder.isEmpty()) return col;
        return editOrder.contains(col) ? col : editOrder.get(0);
    }

    private void moveToNextCell(boolean forward) {
        TableView<DailyMilkEntry> tv = table;
        if (tv.getItems().isEmpty()) return;

        var fm  = tv.getFocusModel();
        var pos = fm.getFocusedCell();
        int row = (pos == null ? 0 : Math.max(0, pos.getRow()));
        TableColumn<DailyMilkEntry, ?> col = (pos == null ? null : pos.getTableColumn());
        col = normalizeToEditable(col);

        int idx = (col == null ? -1 : editOrder.indexOf(col));
        if (idx < 0) idx = 0;

        if (forward) {
            if (idx == editOrder.size() - 1) { // end of row -> next row, first col
                row = Math.min(row + 1, tv.getItems().size() - 1);
                idx = 0;
            } else {
                idx++;
            }
        } else {
            if (idx == 0) { // before first -> previous row, last col
                row = Math.max(row - 1, 0);
                idx = editOrder.size() - 1;
            } else {
                idx--;
            }
        }

        final int targetRow = row;
        final TableColumn<DailyMilkEntry, ?> targetCol = editOrder.get(idx);

//        javafx.application.Platform.runLater(() -> {
//            tv.scrollTo(targetRow);
//            tv.getSelectionModel().clearAndSelect(targetRow, targetCol);
//            tv.getFocusModel().focus(targetRow, targetCol);
//            tv.requestFocus();
//            tv.edit(targetRow, targetCol); // opens editor and caret
//
//        });
        startEditWithRetry(targetRow, targetCol);
    }

    // ---------- power-user helpers ----------
    // install table-level key handling (when NOT editing a cell)
    private void installTableKeyNav() {
        table.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            // If an editor is active, let the editor handler do its job
            if (table.getEditingCell() != null) return;

            switch (e.getCode()) {
                case ENTER -> {
                    e.consume();
                    var pos = table.getFocusModel().getFocusedCell();
                    TableColumn<DailyMilkEntry, ?> col = normalizeToEditable(
                            pos == null ? null : pos.getTableColumn());
                    int row = (pos == null ? 0 : Math.max(0, pos.getRow()));
                    // Requirement: Enter on a focused (non-editing) cell should ENTER EDIT, not move.
                    table.getSelectionModel().clearAndSelect(row, col);
//                    javafx.application.Platform.runLater(() -> {
//                        table.requestFocus();
//                        table.edit(row, col);
//                    });
                    startEditWithRetry(row, col);
                }
                case TAB -> { e.consume(); moveToNextCell(!e.isShiftDown()); }
                case RIGHT -> { e.consume(); moveToNextCell(true); }
                case LEFT -> { e.consume(); moveToNextCell(false); }
                case DOWN -> {
                    e.consume();
                    // move same column, next row; then edit
                    moveToNextCell(true); // since our order wraps, RIGHT/ENTER behavior is consistent
                }
                case UP -> {
                    e.consume();
                    moveToNextCell(false);
                }
                default -> { /* let others pass */ }
            }
        });
    }

    // Ensures the target cell not only gains focus but actually enters edit mode.
    // Retries for a few pulses so virtualization/layout can't race us.
    private void startEditWithRetry(int row, TableColumn<DailyMilkEntry, ?> col) {
        final int maxAttempts = 6;
        final int[] attempts = {0};

        Runnable tryEdit = new Runnable() {
            @Override public void run() {
                attempts[0]++;

                table.scrollTo(row);
                table.getSelectionModel().clearAndSelect(row, col);
                table.getFocusModel().focus(row, col);

                // Force layout so the target TableCell is realized
                table.layout();
                table.requestFocus();
                table.edit(row, col);

                var ec = table.getEditingCell();
                boolean editing =
                        ec != null && ec.getRow() == row && ec.getTableColumn() == col;

                if (!editing && attempts[0] < maxAttempts) {
                    // Try again next frame (tiny delay works best with VirtualFlow)
                    javafx.animation.PauseTransition pt =
                            new javafx.animation.PauseTransition(javafx.util.Duration.millis(18));
                    pt.setOnFinished(e -> javafx.application.Platform.runLater(this));
                    pt.play();
                }
            }
        };

        javafx.application.Platform.runLater(tryEdit);
    }



    private void installTableKeyNav(TableView<DailyMilkEntry> tv,
                                    List<TableColumn<DailyMilkEntry, ?>> editableOrder) {

        tv.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            // If a TextField editor has focus, let it handle its own keys
            if (e.getTarget() instanceof TextField) return;

            var fm  = tv.getFocusModel();
            var pos = fm.getFocusedCell();

            // Enter behavior:
            // - NOT editing → start editing current cell (if editable)
            // - Editing handled in editor (commit + move next)
            if (e.getCode() == KeyCode.ENTER) {
                e.consume(); // prevent DatePicker from eating it
                javafx.application.Platform.runLater(() -> {
                    tv.requestFocus();
                    if (pos != null && pos.getTableColumn() != null && pos.getTableColumn().isEditable()) {
                        tv.edit(pos.getRow(), pos.getTableColumn()); // enter edit on current cell
                    } else {
                        moveFocus(tv, editableOrderRef, true); // or !e.isShiftDown if you kept that
                    }
                });
                return;
            }


            // Arrow navigation (when NOT editing)
            if (e.getCode()==KeyCode.LEFT)  { moveHorizontal(tv, editableOrder, -1); e.consume(); return; }
            if (e.getCode()==KeyCode.RIGHT) { moveHorizontal(tv, editableOrder, +1); e.consume(); return; }
            if (e.getCode()==KeyCode.UP)    { moveVertical(tv,  -1); e.consume(); return; }
            if (e.getCode()==KeyCode.DOWN)  { moveVertical(tv,  +1); e.consume(); return; }

            // table-scoped copy/paste/fill-down
            if (e.getCode()==KeyCode.C && e.isShortcutDown()) { copyCell(tv); e.consume(); return; }
            if (e.getCode()==KeyCode.V && e.isShortcutDown()) { pasteCell(tv); updateTotals(); e.consume(); return; }
            if (e.getCode()==KeyCode.J && e.isShortcutDown()) { fillDown(tv); updateTotals(); e.consume(); return; }
        });
    }

    private void moveHorizontal(TableView<DailyMilkEntry> tv,
                                List<TableColumn<DailyMilkEntry, ?>> order,
                                int dir) {
        var fm = tv.getFocusModel();
        var pos = fm.getFocusedCell();
        if (pos == null) return;

        int colIdx = order.indexOf(pos.getTableColumn());
        if (colIdx < 0) {
            // jump to first editable
            if (!order.isEmpty()) {
                var first = order.get(0);
                tv.getSelectionModel().clearAndSelect(pos.getRow(), first);
                fm.focus(pos.getRow(), first);
            }
            return;
        }
        int next = colIdx + dir;
        if (next < 0) next = 0;
        if (next >= order.size()) next = order.size()-1;

        var targetCol = order.get(next);
        tv.getSelectionModel().clearAndSelect(pos.getRow(), targetCol);
        fm.focus(pos.getRow(), targetCol);
    }

    private void moveVertical(TableView<DailyMilkEntry> tv, int dir) {
        var fm = tv.getFocusModel();
        var pos = fm.getFocusedCell();
        if (pos == null) return;
        int row = Math.max(0, Math.min(tv.getItems().size()-1, pos.getRow() + dir));
        tv.getSelectionModel().clearAndSelect(row, pos.getTableColumn());
        fm.focus(row, pos.getTableColumn());
    }

    private void moveFocus(TableView<DailyMilkEntry> tv,
                           List<TableColumn<DailyMilkEntry, ?>> order,
                           boolean forward) {
        var fm = tv.getFocusModel();
        var pos = fm.getFocusedCell();
        if (pos == null) return;

        int colIdx = order.indexOf(pos.getTableColumn());
        if (colIdx < 0) {
            if (!order.isEmpty()) {
                var first = order.get(0);
                tv.getSelectionModel().clearAndSelect(pos.getRow(), first);
                fm.focus(pos.getRow(), first);
                tv.edit(pos.getRow(), first);
            }
            return;
        }
        int next = colIdx + (forward ? 1 : -1);
        int row = pos.getRow();

        if (next >= order.size()) { next = 0; row = Math.min(row+1, tv.getItems().size()-1); }
        if (next < 0)             { next = order.size()-1; row = Math.max(row-1, 0); }

        var targetCol = order.get(next);
        tv.getSelectionModel().clearAndSelect(row, targetCol);
        fm.focus(row, targetCol);
        tv.edit(row, targetCol); // put next cell into edit mode
    }

    private void copyCell(TableView<DailyMilkEntry> tv) {
        var pos = tv.getFocusModel().getFocusedCell();
        if (pos == null) return;
        Object val = pos.getTableColumn().getCellObservableValue(pos.getRow()).getValue();
        ClipboardContent cc = new ClipboardContent();
        cc.putString(val == null ? "" : val.toString());
        Clipboard.getSystemClipboard().setContent(cc);
    }

    @SuppressWarnings("unchecked")
    private void pasteCell(TableView<DailyMilkEntry> tv) {
        var pos = tv.getFocusModel().getFocusedCell();
        if (pos == null) return;

        TableColumn<DailyMilkEntry, ?> col = (TableColumn<DailyMilkEntry, ?>) pos.getTableColumn();
        String s = Clipboard.getSystemClipboard().getString();
        if (s == null) return;

        if ("Notes".equals(col.getText())) {
            tv.getItems().get(pos.getRow()).setNotes(s);
            tv.refresh();
        } else {
            try {
                BigDecimal v = decConv.fromString(s);
                var any = (TableColumn<DailyMilkEntry, BigDecimal>) col;
                any.getOnEditCommit().handle(new TableColumn.CellEditEvent<>(
                        tv, new TablePosition<>(tv, pos.getRow(), any),
                        TableColumn.editCommitEvent(), v));
            } catch (Exception ignore) {}
        }
    }

    private void fillDown(TableView<DailyMilkEntry> tv) {
        var sm = tv.getSelectionModel();
        var cells = sm.getSelectedCells();
        if (cells.isEmpty()) return;

        var sorted = cells.stream()
                .map(p -> new Pair<>(p.getRow(), p.getTableColumn()))
                .sorted(Comparator.comparingInt(Pair::getKey))
                .toList();

        int startRow = sorted.get(0).getKey();
        @SuppressWarnings("unchecked")
        TableColumn<DailyMilkEntry, ?> col = sorted.get(0).getValue();

        Object sourceVal = col.getCellObservableValue(startRow).getValue();
        for (int i = 1; i < sorted.size(); i++) {
            int r = sorted.get(i).getKey();
            if ("Notes".equals(col.getText())) {
                tv.getItems().get(r).setNotes(sourceVal == null ? "" : sourceVal.toString());
            } else {
                try {
                    BigDecimal v = (sourceVal == null) ? null : new BigDecimal(sourceVal.toString());
                    @SuppressWarnings("unchecked")
                    var any = (TableColumn<DailyMilkEntry, BigDecimal>) col;
                    any.getOnEditCommit().handle(new TableColumn.CellEditEvent<>(
                            tv, new TablePosition<>(tv, r, any),
                            TableColumn.editCommitEvent(), v));
                } catch (Exception ignore) {}
            }
        }
        tv.refresh();
    }

    private static void showInfo(String m){ var a=new Alert(Alert.AlertType.INFORMATION,m,ButtonType.OK); a.setHeaderText(null); a.showAndWait();}
    private static void showError(String m){ var a=new Alert(Alert.AlertType.ERROR,m,ButtonType.OK); a.setHeaderText("Error"); a.showAndWait();}
    private static String rootMsg(Throwable t){ Throwable x=t; while(x.getCause()!=null) x=x.getCause(); return x.getMessage(); }


    @Override public void actionSave()     { onSave(); }
    @Override public void actionRefresh()  { reload(); }
    @Override public void actionRecompute(){
        rows.forEach(r -> svc.computeRateAndAmount(outlet, r));
        table.refresh();
        Toast.info(this, "Recomputed rates");
    }
}

//package com.rudrainfotech.milkdiary.ui;
//
//import com.rudrainfotech.milkdiary.entity.*;
//import com.rudrainfotech.milkdiary.service.*;
//import javafx.beans.property.SimpleObjectProperty;
//import javafx.collections.FXCollections;
//import javafx.collections.ListChangeListener;
//import javafx.collections.ObservableList;
//import javafx.geometry.Insets;
//import javafx.scene.control.*;
//import javafx.scene.control.cell.TextFieldTableCell;
//import javafx.scene.input.*;
//import javafx.scene.layout.BorderPane;
//import javafx.scene.layout.HBox;
//import javafx.util.Pair;
//import javafx.util.StringConverter;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.LocalDate;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class DailyEntriesView extends BorderPane {
//    private final OutletService outletSvc = new OutletService();
//    private final DailyEntryService svc = new DailyEntryService();
//    private final MemberService memberSvc = new MemberService();
//
//    private final Outlet outlet;
//
//    private final Label lockLbl = new Label();
//    private final CheckBox overrideCk = new CheckBox("Override lock (admin)");
//
//    private final Button saveAllBtn   = new Button("Save All");
//    private final Button deleteBtn    = new Button("Delete Row");
//    private final Button recomputeBtn = new Button("Recompute Rates");
//
//    private final DatePicker date = new DatePicker(LocalDate.now());
//    private final ComboBox<SessionType> session = new ComboBox<>();
//    private final ComboBox<String> speciesFilter = new ComboBox<>();
//
//    private final TableView<DailyMilkEntry> table = new TableView<>();
//    private final ObservableList<DailyMilkEntry> rows = FXCollections.observableArrayList();
//
//    // status bar
//    private final Label rowsLbl = new Label("Rows: 0");
//    private final Label qtyLbl  = new Label("Σ Qty: 0.000 L");
//    private final Label amtLbl  = new Label("Σ Amount: 0.00");
//
//    // editable column order (for navigation)
//    private List<TableColumn<DailyMilkEntry, ?>> editableOrderRef = List.of();
//
//    private final StringConverter<BigDecimal> decConv = new StringConverter<>() {
//        @Override public String toString(BigDecimal o) { return o==null ? "" : o.stripTrailingZeros().toPlainString(); }
//        @Override public BigDecimal fromString(String s) {
//            if (s==null) return null; String t = s.trim(); if (t.isEmpty()) return null; return new BigDecimal(t);
//        }
//    };
//
//    public DailyEntriesView() {
//        this.outlet = outletSvc.getActiveOutlet();
//
//        // Top bar
//        session.getItems().addAll(SessionType.values()); session.getSelectionModel().select(SessionType.AM);
//        speciesFilter.getItems().addAll("All","Cow","Buffalo");
//        speciesFilter.getSelectionModel().select("All");
//
//        Button load = new Button("Load Members");
//        Button refresh = new Button("Refresh");
//
//        overrideCk.setSelected(false);
//        overrideCk.selectedProperty().addListener((obs, was, now) -> {
//            if (now) {
//                TextInputDialog d = new TextInputDialog();
//                d.setTitle("Admin PIN");
//                d.setHeaderText("Enter Admin PIN to override lock");
//                d.setContentText("PIN:");
//                d.getEditor().setText("");
//                d.getEditor().setPromptText("PIN");
//                var res = d.showAndWait();
//                boolean ok = res.isPresent() && new AppSettingsService().verifyAdminPin(res.get());
//                if (!ok) {
//                    overrideCk.setSelected(false);
//                    showError("Invalid PIN.");
//                }
//            }
//            refreshLockUI();
//        });
//
//        load.setOnAction(e -> loadMembersAsRows());
//        refresh.setOnAction(e -> reload());
//        recomputeBtn.setOnAction(e -> { rows.forEach(r -> svc.computeRateAndAmount(outlet, r)); table.refresh(); updateTotals(); });
//        saveAllBtn.setOnAction(e -> onSave());
//        deleteBtn.setOnAction(e -> onDeleteRow());
//
//        HBox top = new HBox(10,
//                new Label("Date:"), date,
//                new Label("Session:"), session,
//                new Label("Filter:"), speciesFilter,
//                new Separator(), load, refresh, recomputeBtn, saveAllBtn, deleteBtn,
//                new Separator(), new Label("Lock:"), lockLbl, overrideCk
//        );
//        top.setPadding(new Insets(10));
//        setTop(top);
//
//        // Table
//        table.setEditable(true);
//        table.setItems(rows);
//        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
//        table.setPlaceholder(new Label("Click Load Members or Refresh"));
//        table.getSelectionModel().setCellSelectionEnabled(true); // cell-level ops
//
//        TableColumn<DailyMilkEntry, String> cCode = new TableColumn<>("Code");
//        cCode.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getMember().getCode()));
//        cCode.setMinWidth(100);
//        cCode.setEditable(false);
//
//        TableColumn<DailyMilkEntry, String> cName = new TableColumn<>("Name");
//        cName.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getMember().getName()));
//        cName.setMinWidth(200);
//        cName.setEditable(false);
//
//        // Editable numeric columns with ↑/↓ stepping and enter-to-next
//        var qtyCol = decEditableCol("Qty(L)", "qtyLitre", new BigDecimal("0.50"), new BigDecimal("1.00"));
//        var fatCol = decEditableCol("Fat%",  "fatPct",   new BigDecimal("0.10"), new BigDecimal("0.50"));
//        var snfCol = decEditableCol("SNF%",  "snfPct",   new BigDecimal("0.10"), new BigDecimal("0.50"));
//
//        TableColumn<DailyMilkEntry, BigDecimal> rateCol = new TableColumn<>("Rate/L");
//        rateCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("rateApplied"));
//        rateCol.setEditable(false);
//
//        TableColumn<DailyMilkEntry, BigDecimal> amtCol = new TableColumn<>("Amount");
//        amtCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("amount"));
//        amtCol.setEditable(false);
//
//        TableColumn<DailyMilkEntry, String> notes = new TableColumn<>("Notes");
//        notes.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("notes"));
//        notes.setCellFactory(TextFieldTableCell.forTableColumn());
//        notes.setOnEditCommit(ev -> { ev.getRowValue().setNotes(ev.getNewValue()); updateTotals(); });
//
//        table.getColumns().addAll(cCode, cName, qtyCol, fatCol, snfCol, rateCol, amtCol, notes);
//
//        // set editable order (for nav)
//        editableOrderRef = List.of(qtyCol, fatCol, snfCol, notes);
//
//        // Spreadsheet-like nav & clipboard (+ refined Enter behavior)
//        installTableKeyNav(table, editableOrderRef);
//
//        setCenter(table);
//
//        // Status bar
//        HBox status = new HBox(18, rowsLbl, qtyLbl, amtLbl);
//        status.setPadding(new Insets(8, 12, 10, 12));
//        setBottom(status);
//
//        // initial load
//        reload();
//        refreshLockUI();
//        updateTotals();
//
//        // reacting controls
//        date.valueProperty().addListener((o, ov, nv) -> { reload(); refreshLockUI(); });
//        session.valueProperty().addListener((o, ov, nv) -> reload());
//        speciesFilter.valueProperty().addListener((o, ov, nv) -> reload());
//
//        // list changes -> recompute totals
//        rows.addListener((ListChangeListener<DailyMilkEntry>) c -> updateTotals());
//
//        // Global accelerators
//        sceneProperty().addListener((obs, oldSc, sc) -> {
//            if (sc == null) return;
//            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), saveAllBtn::fire);
//            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN), recomputeBtn::fire);
//            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN), this::loadMembersAsRows);
//            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN), deleteBtn::fire);
//            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), this::reload);
//
//            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.LEFT,  KeyCombination.SHORTCUT_DOWN),
//                    () -> date.setValue(date.getValue().minusDays(1)));
//            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.SHORTCUT_DOWN),
//                    () -> date.setValue(date.getValue().plusDays(1)));
//
//            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.SHORTCUT_DOWN),
//                    () -> { session.getSelectionModel().select(SessionType.AM); reload(); });
//            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.SHORTCUT_DOWN),
//                    () -> { session.getSelectionModel().select(SessionType.PM); reload(); });
//
//            // species filter
//            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN),
//                    () -> speciesFilter.getSelectionModel().select("All"));
//            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN),
//                    () -> speciesFilter.getSelectionModel().select("Cow"));
//            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN),
//                    () -> speciesFilter.getSelectionModel().select("Buffalo"));
//
//            // fill-down
//            sc.getAccelerators().put(new KeyCodeCombination(KeyCode.J, KeyCombination.SHORTCUT_DOWN),
//                    () -> fillDown(table));
//        });
//    }
//
//    /** Editable BigDecimal column with ↑/↓ stepping and enter-to-next. */
//    private TableColumn<DailyMilkEntry, BigDecimal> decEditableCol(
//            String title, String prop, BigDecimal step, BigDecimal stepCoarse) {
//
//        TableColumn<DailyMilkEntry, BigDecimal> c = new TableColumn<>(title);
//        c.setEditable(true);
//        c.setMinWidth(90);
//        c.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(prop));
//
//        c.setCellFactory(col -> new TextFieldTableCell<>(decConv) {
//            private TextField tf;
//
//            @Override
//            public void startEdit() {
//                super.startEdit();
//                tf = (TextField) getGraphic();
//                if (tf != null) installEditingKeys(tf, c);
//            }
//
//            @Override public void updateItem(BigDecimal item, boolean empty) {
//                super.updateItem(item, empty);
//                if (!isEmpty() && isEditing() && getGraphic() instanceof TextField t) {
//                    tf = t; installEditingKeys(tf, c);
//                }
//            }
//
//            private void installEditingKeys(TextField field, TableColumn<DailyMilkEntry, BigDecimal> thisCol) {
//                field.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
//                    // numeric step while editing
//                    if (e.getCode() == KeyCode.UP || e.getCode() == KeyCode.DOWN) {
//                        BigDecimal cur;
//                        try { cur = decConv.fromString(field.getText()); } catch (Exception ex) { cur = BigDecimal.ZERO; }
//                        if (cur == null) cur = BigDecimal.ZERO;
//                        BigDecimal delta = e.isAltDown() ? stepCoarse : step;
//                        BigDecimal next = (e.getCode()==KeyCode.UP) ? cur.add(delta) : cur.subtract(delta);
//                        if (next.compareTo(BigDecimal.ZERO) < 0) next = BigDecimal.ZERO;
//                        field.setText(next.stripTrailingZeros().toPlainString());
//                        try { commitEdit(decConv.fromString(field.getText())); } catch (Exception ignore) {}
//                        e.consume();
//                        updateTotals();
//                        return;
//                    }
//                    // Enter in editing: commit then move next/prev
//                    if (e.getCode()==KeyCode.ENTER) {
//                        try { commitEdit(decConv.fromString(field.getText())); } catch (Exception ignore) {}
//                        // move to next or previous editable cell
//                        moveFocus(table, editableOrderRef, !e.isShiftDown());
//                        e.consume();
//                        updateTotals();
//                        return;
//                    }
//                    // copy/paste while editing
//                    if (e.getCode()==KeyCode.C && e.isShortcutDown()) {
//                        ClipboardContent cc = new ClipboardContent();
//                        cc.putString(field.getText()==null?"":field.getText());
//                        Clipboard.getSystemClipboard().setContent(cc);
//                        e.consume();
//                        return;
//                    }
//                    if (e.getCode()==KeyCode.V && e.isShortcutDown()) {
//                        String s = Clipboard.getSystemClipboard().getString();
//                        if (s != null) {
//                            field.setText(s);
//                            try { commitEdit(decConv.fromString(s)); } catch (Exception ignore) {}
//                            updateTotals();
//                        }
//                        e.consume();
//                    }
//                });
//            }
//        });
//
//        c.setOnEditCommit(ev -> {
//            DailyMilkEntry row = ev.getRowValue();
//            switch (prop) {
//                case "qtyLitre" -> row.setQtyLitre(ev.getNewValue());
//                case "fatPct"   -> row.setFatPct(ev.getNewValue());
//                case "snfPct"   -> row.setSnfPct(ev.getNewValue());
//            }
//            // ensure keys and recompute
//            try {
//                if (row.getDate()==null) row.setDate(date.getValue());
//                if (row.getSession()==null) row.setSession(session.getValue());
//                if (row.getOutlet()==null) row.setOutlet(outlet);
//                row.setSpecies(row.getMember().getSpecies()); // enforce from member
//                svc.computeRateAndAmount(outlet, row);
//                table.refresh();
//            } catch (Exception ex) {
//                showError("No matching rate: " + rootMsg(ex));
//                row.setRateApplied(null); row.setAmount(null);
//                table.refresh();
//            }
//            updateTotals();
//        });
//
//        return c;
//    }
//
//    private void reload() {
//        if (outlet == null) { rows.clear(); updateTotals(); return; }
//
//        String f = speciesFilter.getValue();
//        Species sp = switch (f) {
//            case "Cow" -> Species.COW;
//            case "Buffalo" -> Species.BUFFALO;
//            default -> null; // All
//        };
//
//        var list = (sp == null)
//                ? svc.list(outlet, date.getValue(), session.getValue())
//                : svc.list(outlet, date.getValue(), session.getValue(), sp);
//
//        rows.setAll(list);
//        updateTotals();
//    }
//
//    private void refreshLockUI() {
//        if (outlet == null || date.getValue()==null) {
//            lockLbl.setText("No outlet");
//            table.setEditable(false);
//            saveAllBtn.setDisable(true);
//            deleteBtn.setDisable(true);
//            return;
//        }
//
//        int y = date.getValue().getYear();
//        int m = date.getValue().getMonthValue();
//        int cap = BillLockService.capNoFor(date.getValue());
//
//        boolean locked = new BillLockService().isCapLocked(outlet.getId(), y, m, cap);
//        lockLbl.setText(locked ? "LOCKED (cap " + cap + ")" : "Unlocked");
//
//        boolean disableWrites = locked && !overrideCk.isSelected();
//        table.setEditable(!disableWrites);
//        saveAllBtn.setDisable(disableWrites);
//        deleteBtn.setDisable(disableWrites);
//    }
//
//    private void loadMembersAsRows() {
//        Map<Long, DailyMilkEntry> byMember = rows.stream().collect(Collectors.toMap(
//                r -> r.getMember().getId(), r -> r, (a,b)->a, LinkedHashMap::new));
//
//        var members = memberSvc.listByOutlet(outlet).stream()
//                .filter(Member::isActive)
//                .filter(m -> {
//                    String f = speciesFilter.getValue();
//                    return "All".equals(f) ||
//                            ("Cow".equals(f) && m.getSpecies()==Species.COW) ||
//                            ("Buffalo".equals(f) && m.getSpecies()==Species.BUFFALO);
//                })
//                .sorted(Comparator.comparing(Member::getCode))
//                .toList();
//
//        for (Member m : members) {
//            if (!byMember.containsKey(m.getId())) {
//                DailyMilkEntry e = new DailyMilkEntry();
//                e.setOutlet(outlet);
//                e.setMember(m);
//                e.setDate(date.getValue());
//                e.setSession(session.getValue());
//                e.setSpecies(m.getSpecies());
//                byMember.put(m.getId(), e);
//            }
//        }
//        rows.setAll(byMember.values());
//        table.refresh();
//        updateTotals();
//    }
//
//    private void onSave() {
//        try {
//            for (DailyMilkEntry r : rows) {
//                r.setOutlet(outlet);
//                r.setDate(date.getValue());
//                r.setSession(session.getValue());
//                r.setSpecies(r.getMember().getSpecies()); // enforce
//            }
//            svc.upsertAll(outlet, date.getValue(), session.getValue(), rows, overrideCk.isSelected());
//            showInfo("Saved.");
//            reload();
//            refreshLockUI();
//        } catch (Exception ex) {
//            showError("Save failed:\n" + rootMsg(ex));
//        }
//    }
//
//    private void onDeleteRow() {
//        DailyMilkEntry sel = table.getSelectionModel().getSelectedItem();
//        if (sel == null || sel.getId()==null) return;
//
//        var confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this entry?", ButtonType.YES, ButtonType.NO);
//        confirm.setHeaderText("Confirm");
//        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
//            try {
//                svc.deleteEntry(outlet, sel, overrideCk.isSelected());
//                showInfo("Deleted.");
//                reload();
//                refreshLockUI();
//            } catch (Exception ex) {
//                showError("Delete failed:\n" + rootMsg(ex));
//            }
//        }
//    }
//
//    // ---------- status/totals ----------
//    private void updateTotals() {
//        BigDecimal qty = BigDecimal.ZERO;
//        BigDecimal amt = BigDecimal.ZERO;
//        int n = rows.size();
//
//        for (DailyMilkEntry e : rows) {
//            if (e.getQtyLitre()!=null) qty = qty.add(e.getQtyLitre());
//            if (e.getAmount()!=null)   amt = amt.add(e.getAmount());
//        }
//        qty = qty.setScale(3, RoundingMode.HALF_UP);
//        amt = amt.setScale(2, RoundingMode.HALF_UP);
//
//        rowsLbl.setText("Rows: " + n);
//        qtyLbl.setText("Σ Qty: " + qty.toPlainString() + " L");
//        amtLbl.setText("Σ Amount: " + amt.toPlainString());
//    }
//
//    // ---------- power-user helpers ----------
//    private void installTableKeyNav(TableView<DailyMilkEntry> tv,
//                                    List<TableColumn<DailyMilkEntry, ?>> editableOrder) {
//
//        tv.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
//            // If a TextField editor has focus, let it handle its own keys
//            if (e.getTarget() instanceof TextField) return;
//
//            var fm  = tv.getFocusModel();
//            var pos = fm.getFocusedCell();
//
//            // Enter behavior:
//            // - NOT editing → start editing current cell (if editable)
//            // - Editing handled in editor (commit + move next)
//            if (e.getCode()==KeyCode.ENTER) {
//                if (pos != null && pos.getTableColumn()!=null && pos.getTableColumn().isEditable()) {
//                    tv.edit(pos.getRow(), pos.getTableColumn());
//                } else {
//                    // if non-editable col focused, fall back to move
//                    moveFocus(tv, editableOrder, !e.isShiftDown());
//                }
//                e.consume();
//                return;
//            }
//
//            // Arrow navigation (when NOT editing)
//            if (e.getCode()==KeyCode.LEFT)  { moveHorizontal(tv, editableOrder, -1); e.consume(); return; }
//            if (e.getCode()==KeyCode.RIGHT) { moveHorizontal(tv, editableOrder, +1); e.consume(); return; }
//            if (e.getCode()==KeyCode.UP)    { moveVertical(tv,  -1); e.consume(); return; }
//            if (e.getCode()==KeyCode.DOWN)  { moveVertical(tv,  +1); e.consume(); return; }
//
//            // table-scoped copy/paste/fill-down
//            if (e.getCode()==KeyCode.C && e.isShortcutDown()) { copyCell(tv); e.consume(); return; }
//            if (e.getCode()==KeyCode.V && e.isShortcutDown()) { pasteCell(tv); updateTotals(); e.consume(); return; }
//            if (e.getCode()==KeyCode.J && e.isShortcutDown()) { fillDown(tv); updateTotals(); e.consume(); return; }
//        });
//    }
//
//    private void moveHorizontal(TableView<DailyMilkEntry> tv,
//                                List<TableColumn<DailyMilkEntry, ?>> order,
//                                int dir) {
//        var fm = tv.getFocusModel();
//        var pos = fm.getFocusedCell();
//        if (pos == null) return;
//
//        int colIdx = order.indexOf(pos.getTableColumn());
//        if (colIdx < 0) {
//            // jump to first editable
//            if (!order.isEmpty()) {
//                var first = order.get(0);
//                tv.getSelectionModel().clearAndSelect(pos.getRow(), first);
//                fm.focus(pos.getRow(), first);
//            }
//            return;
//        }
//        int next = colIdx + dir;
//        if (next < 0) next = 0;
//        if (next >= order.size()) next = order.size()-1;
//
//        var targetCol = order.get(next);
//        tv.getSelectionModel().clearAndSelect(pos.getRow(), targetCol);
//        fm.focus(pos.getRow(), targetCol);
//    }
//
//    private void moveVertical(TableView<DailyMilkEntry> tv, int dir) {
//        var fm = tv.getFocusModel();
//        var pos = fm.getFocusedCell();
//        if (pos == null) return;
//        int row = Math.max(0, Math.min(tv.getItems().size()-1, pos.getRow() + dir));
//        tv.getSelectionModel().clearAndSelect(row, pos.getTableColumn());
//        fm.focus(row, pos.getTableColumn());
//    }
//
//    private void moveFocus(TableView<DailyMilkEntry> tv,
//                           List<TableColumn<DailyMilkEntry, ?>> order,
//                           boolean forward) {
//        var fm = tv.getFocusModel();
//        var pos = fm.getFocusedCell();
//        if (pos == null) return;
//
//        int colIdx = order.indexOf(pos.getTableColumn());
//        if (colIdx < 0) {
//            if (!order.isEmpty()) {
//                var first = order.get(0);
//                tv.getSelectionModel().clearAndSelect(pos.getRow(), first);
//                fm.focus(pos.getRow(), first);
//                tv.edit(pos.getRow(), first);
//            }
//            return;
//        }
//        int next = colIdx + (forward ? 1 : -1);
//        int row = pos.getRow();
//
//        if (next >= order.size()) { next = 0; row = Math.min(row+1, tv.getItems().size()-1); }
//        if (next < 0)             { next = order.size()-1; row = Math.max(row-1, 0); }
//
//        var targetCol = order.get(next);
//        tv.getSelectionModel().clearAndSelect(row, targetCol);
//        fm.focus(row, targetCol);
//        tv.edit(row, targetCol); // put next cell into edit mode
//    }
//
//    private void copyCell(TableView<DailyMilkEntry> tv) {
//        var pos = tv.getFocusModel().getFocusedCell();
//        if (pos == null) return;
//        Object val = pos.getTableColumn().getCellObservableValue(pos.getRow()).getValue();
//        ClipboardContent cc = new ClipboardContent();
//        cc.putString(val == null ? "" : val.toString());
//        Clipboard.getSystemClipboard().setContent(cc);
//    }
//
//    @SuppressWarnings("unchecked")
//    private void pasteCell(TableView<DailyMilkEntry> tv) {
//        var pos = tv.getFocusModel().getFocusedCell();
//        if (pos == null) return;
//
//        TableColumn<DailyMilkEntry, ?> col = (TableColumn<DailyMilkEntry, ?>) pos.getTableColumn();
//        String s = Clipboard.getSystemClipboard().getString();
//        if (s == null) return;
//
//        if ("Notes".equals(col.getText())) {
//            tv.getItems().get(pos.getRow()).setNotes(s);
//            tv.refresh();
//        } else {
//            try {
//                BigDecimal v = decConv.fromString(s);
//                var any = (TableColumn<DailyMilkEntry, BigDecimal>) col;
//                any.getOnEditCommit().handle(new TableColumn.CellEditEvent<>(
//                        tv, new TablePosition<>(tv, pos.getRow(), any),
//                        TableColumn.editCommitEvent(), v));
//            } catch (Exception ignore) {}
//        }
//    }
//
//    private void fillDown(TableView<DailyMilkEntry> tv) {
//        var sm = tv.getSelectionModel();
//        var cells = sm.getSelectedCells();
//        if (cells.isEmpty()) return;
//
//        var sorted = cells.stream()
//                .map(p -> new Pair<>(p.getRow(), p.getTableColumn()))
//                .sorted(Comparator.comparingInt(Pair::getKey))
//                .toList();
//
//        int startRow = sorted.get(0).getKey();
//        @SuppressWarnings("unchecked")
//        TableColumn<DailyMilkEntry, ?> col = sorted.get(0).getValue();
//
//        Object sourceVal = col.getCellObservableValue(startRow).getValue();
//        for (int i = 1; i < sorted.size(); i++) {
//            int r = sorted.get(i).getKey();
//            if ("Notes".equals(col.getText())) {
//                tv.getItems().get(r).setNotes(sourceVal == null ? "" : sourceVal.toString());
//            } else {
//                try {
//                    BigDecimal v = (sourceVal == null) ? null : new BigDecimal(sourceVal.toString());
//                    @SuppressWarnings("unchecked")
//                    var any = (TableColumn<DailyMilkEntry, BigDecimal>) col;
//                    any.getOnEditCommit().handle(new TableColumn.CellEditEvent<>(
//                            tv, new TablePosition<>(tv, r, any),
//                            TableColumn.editCommitEvent(), v));
//                } catch (Exception ignore) {}
//            }
//        }
//        tv.refresh();
//    }
//
//    private static void showInfo(String m){ var a=new Alert(Alert.AlertType.INFORMATION,m,ButtonType.OK); a.setHeaderText(null); a.showAndWait();}
//    private static void showError(String m){ var a=new Alert(Alert.AlertType.ERROR,m,ButtonType.OK); a.setHeaderText("Error"); a.showAndWait();}
//    private static String rootMsg(Throwable t){ Throwable x=t; while(x.getCause()!=null) x=x.getCause(); return x.getMessage(); }
//}