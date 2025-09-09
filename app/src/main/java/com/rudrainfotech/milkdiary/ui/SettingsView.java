package com.rudrainfotech.milkdiary.ui;

import com.rudrainfotech.milkdiary.service.AppSettingsService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.nio.file.Path;

public class SettingsView extends Stage {

    // Services
    private final AppSettingsService svc = new AppSettingsService();

    // Controls (kept as fields where we need them at save-time)
    private TextField printedBy;
    private CheckBox fillMissing;
    private Spinner<Double> gapPt;
    private Spinner<Integer> defaultCap;
    private CheckBox lockOnGen;
    private ComboBox<String> lang;

    private TextField backupDir;
    private Spinner<Integer> keep;
    private Button backupNowBtn;
    private Button openFolderBtn;
    private Button restoreBtn;

    private PasswordField oldPin;
    private PasswordField newPin;
    private PasswordField newPin2;

    private Button saveBtn;
    private Button closeBtn;

    public SettingsView() {
        setTitle("Settings");
        initModality(Modality.APPLICATION_MODAL);

        var root = buildUI();
        var scene = new Scene(root, 560, 640);
        // Attach your stylesheet(s) outside, or here if you wish:
        // scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        setScene(scene);

        // Keyboard niceties
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> saveBtn.fire();
                case ESCAPE -> close();
            }
        });
    }

    /* =======================================================================
     * UI
     * ======================================================================= */
    private Parent buildUI() {
        // Header
        var header = buildHeader();

        // Sections
        var generalCard = buildGeneralSection();
        var pdfCard     = buildPdfSection();
        var capCard     = buildCapAndLockSection();
        var langCard    = buildLanguageSection();
        var pinCard     = buildAdminPinSection();
        var backupCard  = buildBackupSection();

        // Footer (sticky actions)
        var footer = buildFooter();

        var content = new VBox(14,
                generalCard,
                pdfCard,
                capCard,
                langCard,
                pinCard,
                backupCard,
                spacer(6)
        );
        content.setPadding(new Insets(16));
        content.getStyleClass().add("settings-content");

        var scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("settings-scroll");

        var root = new BorderPane();
        root.setTop(header);
        root.setCenter(scroll);
        root.setBottom(footer);
        root.getStyleClass().add("settings-root");

        return root;
    }

    private Node buildHeader() {
        var title = new Label("Application Settings");
        title.getStyleClass().add("settings-title");

        var subtitle = new Label("Tweak behavior for PDFs, caps, backups, and admin access.");
        subtitle.getStyleClass().add("settings-subtitle");

        var box = new VBox(4, title, subtitle);
        box.setPadding(new Insets(16, 16, 8, 16));
        box.getStyleClass().add("settings-header");
        return box;
    }

    /* ------------------------- Section: General ------------------------- */
    private Node buildGeneralSection() {
        printedBy = new TextField(svc.getString(AppSettingsService.PRINTED_BY, "Operator"));
        printedBy.setPromptText("Name printed on PDFs, exports, etc.");

        var grid = makeFormGrid();
        int r = 0;
        grid.add(new Label("Printed By"), 0, r);
        grid.add(printedBy, 1, r++);

        return sectionCard("General", grid);
    }

    /* --------------------------- Section: PDF --------------------------- */
    private Node buildPdfSection() {
        fillMissing = new CheckBox("Fill missing days");
        fillMissing.setSelected(svc.getBool(AppSettingsService.PDF_FILL_MISSING, true));

        gapPt = buildDoubleSpinner(
                parseDoubleSafe(svc.getString(AppSettingsService.PDF_GAP_PT, "50"), 50),
                0.0, 100.0, 0.5
        );
        gapPt.setEditable(true);
        gapPt.setPrefWidth(120);

        var grid = makeFormGrid();
        int r = 0;
        grid.add(fillMissing, 0, r++, 2, 1);
        grid.add(new Label("Gap Between Rows (pt)"), 0, r);
        grid.add(gapPt, 1, r++);

        var help = smallNote("“Fill missing days” will auto-generate empty rows in date spans so printed PDFs look continuous.");
        var box = new VBox(10, grid, help);
        return sectionCard("PDF Output", box);
    }

    /* ----------------------- Section: Caps & Locking -------------------- */
    private Node buildCapAndLockSection() {
        defaultCap = new Spinner<>(1, 3, svc.getInt(AppSettingsService.DEFAULT_CAP, 1), 1);
        defaultCap.setEditable(true);
        defaultCap.setPrefWidth(120);

        lockOnGen = new CheckBox("Lock cap after Generate/Recalc");
        lockOnGen.setSelected(svc.getBool(AppSettingsService.LOCK_ON_GENERATE, false));

        var grid = makeFormGrid();
        int r = 0;
        grid.add(new Label("Default Cap"), 0, r);
        grid.add(defaultCap, 1, r++);
        grid.add(lockOnGen, 0, r++, 2, 1);

        var help = smallNote("When enabled, caps become read-only immediately after you generate or recompute, preventing accidental edits.");
        var box = new VBox(10, grid, help);
        return sectionCard("Caps & Locking", box);
    }

    /* --------------------------- Section: Language ---------------------- */
    private Node buildLanguageSection() {
        lang = new ComboBox<>();
        lang.getItems().addAll("en", "mr-IN");
        lang.setEditable(false);
        lang.getSelectionModel().select(svc.getString(AppSettingsService.UI_LANG, "en"));
        lang.setPrefWidth(160);

        var grid = makeFormGrid();
        int r = 0;
        grid.add(new Label("UI Language"), 0, r);
        grid.add(lang, 1, r++);

        var note = smallNote("Language change requires app restart to load fonts and resources.");
        var box = new VBox(10, grid, note);
        return sectionCard("Language", box);
    }

    /* --------------------------- Section: Admin PIN --------------------- */
    private Node buildAdminPinSection() {
        oldPin = new PasswordField();
        oldPin.setPromptText("Current PIN (if set)");

        newPin = new PasswordField();
        newPin.setPromptText("New PIN");

        newPin2 = new PasswordField();
        newPin2.setPromptText("Confirm New PIN");

        var pinNote = smallNote("Leave PIN fields blank to keep the current PIN unchanged.");

        var grid = makeFormGrid();
        int r = 0;
        grid.add(new Label("Old PIN"), 0, r); grid.add(oldPin, 1, r++);
        grid.add(new Label("New PIN"), 0, r); grid.add(newPin, 1, r++);
        grid.add(new Label("Confirm PIN"), 0, r); grid.add(newPin2, 1, r++);

        var box = new VBox(10, grid, pinNote);
        return sectionCard("Admin PIN", box);
    }

    /* ---------------------------- Section: Backups ---------------------- */
    private Node buildBackupSection() {
        backupDir = new TextField(svc.getString(AppSettingsService.BACKUP_DIR, "backups"));
        backupDir.setPromptText("Relative or absolute folder");

        keep = new Spinner<>(1, 120, svc.getInt(AppSettingsService.BACKUPS_KEEP, 14), 1);
        keep.setEditable(true);
        keep.setPrefWidth(120);

        backupNowBtn = new Button("Backup Now");
        openFolderBtn = new Button("Open Folder");
        restoreBtn = new Button("Restore…");

        // Handlers
        backupNowBtn.setOnAction(e -> onBackupNow());
        openFolderBtn.setOnAction(e -> onOpenFolder());
        restoreBtn.setOnAction(e -> onRestore());

        var grid = makeFormGrid();
        int r = 0;
        grid.add(new Label("Backup Folder"), 0, r); grid.add(backupDir, 1, r++);
        grid.add(new Label("Backups to Keep"), 0, r); grid.add(keep, 1, r++);

        var inlineBtns = new HBox(8, backupNowBtn, openFolderBtn, restoreBtn);
        inlineBtns.setAlignment(Pos.CENTER_LEFT);

        var tip = smallNote("Backups are SQLite DB snapshots. “Restore” will queue the selected file and apply it on the next app start.");

        var box = new VBox(10, grid, inlineBtns, tip);
        return sectionCard("Backups", box);
    }

    /* ------------------------------- Footer ----------------------------- */
    private Node buildFooter() {
        saveBtn = new Button("Save");
        closeBtn = new Button("Close");

        saveBtn.getStyleClass().add("btn-primary");
        closeBtn.getStyleClass().add("btn-secondary");

        saveBtn.setDefaultButton(true);   // ENTER triggers save
        closeBtn.setCancelButton(true);   // ESC triggers close

        saveBtn.setOnAction(e -> onSave());
        closeBtn.setOnAction(e -> close());

        var leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        var box = new HBox(10, leftSpacer, closeBtn, saveBtn);
        box.setPadding(new Insets(10, 16, 12, 16));
        box.setAlignment(Pos.CENTER_RIGHT);
        box.getStyleClass().add("settings-footer");
        return box;
    }

    /* =======================================================================
     * Actions
     * ======================================================================= */
    private void onBackupNow() {
        try {
            var f = new com.rudrainfotech.milkdiary.service.BackupService().backupNow();
            info("Backup saved:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            error("Backup failed:\n" + rootMsg(ex));
        }
    }

    private void onOpenFolder() {
        try {
            var dir = Path.of(svc.getString(AppSettingsService.BACKUP_DIR, "backups")).toAbsolutePath();
            java.awt.Desktop.getDesktop().open(dir.toFile());
        } catch (Exception ex) {
            error("Open failed:\n" + rootMsg(ex));
        }
    }

    private void onRestore() {
        try {
            var backupSvc = new com.rudrainfotech.milkdiary.service.BackupService();
            var chooser = new javafx.stage.FileChooser();
            chooser.setTitle("Choose backup to restore");
            chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("SQLite DB", "*.db"));

            var dir = Path.of(svc.getString(AppSettingsService.BACKUP_DIR, "backups")).toFile();
            if (dir.isDirectory()) chooser.setInitialDirectory(dir);

            var f = chooser.showOpenDialog(getOwner());
            if (f == null) return;

            var confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Restore this backup?\n\n" + f.getName() + "\n\nThe app will apply it on next start.",
                    ButtonType.OK, ButtonType.CANCEL);
            confirm.setHeaderText("Confirm restore");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

            backupSvc.queueRestore(f);
            info("Restore queued.\nPlease restart the application.");
        } catch (Exception ex) {
            error("Restore failed:\n" + rootMsg(ex));
        }
    }

    private void onSave() {
        try {
            // Persist basic settings
            svc.setString(AppSettingsService.PRINTED_BY, printedBy.getText().trim());
            svc.setBool(AppSettingsService.PDF_FILL_MISSING, fillMissing.isSelected());
            svc.setString(AppSettingsService.PDF_GAP_PT, String.valueOf(gapPt.getValue()));
            svc.setInt(AppSettingsService.DEFAULT_CAP, defaultCap.getValue());
            svc.setBool(AppSettingsService.LOCK_ON_GENERATE, lockOnGen.isSelected());
            svc.setString(AppSettingsService.BACKUP_DIR, backupDir.getText().trim());
            svc.setInt(AppSettingsService.BACKUPS_KEEP, keep.getValue());
            svc.setString(AppSettingsService.UI_LANG, lang.getValue());

            // Admin PIN logic
            boolean wantsNewPin = !(newPin.getText().isBlank() && newPin2.getText().isBlank());
            if (wantsNewPin) {
                if (!newPin.getText().equals(newPin2.getText()))
                    throw new IllegalArgumentException("New PIN and Confirm PIN do not match.");

                String stored = svc.getString(AppSettingsService.ADMIN_PIN_HASH, null);
                if (stored != null && !stored.isBlank()) {
                    if (!svc.verifyAdminPin(oldPin.getText()))
                        throw new IllegalArgumentException("Old PIN is incorrect.");
                }
                if (newPin.getText().length() < 4)
                    throw new IllegalArgumentException("PIN must be at least 4 digits/characters.");
                svc.setAdminPin(newPin.getText());
            }

            info("Saved. Please restart the app to apply language.");
        } catch (Exception ex) {
            var a = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
            a.setHeaderText("Save failed");
            a.showAndWait();
        }
    }

    /* =======================================================================
     * Helpers
     * ======================================================================= */
    private GridPane makeFormGrid() {
        GridPane gp = new GridPane();
        gp.setHgap(12);
        gp.setVgap(10);
        gp.getStyleClass().add("form-grid");

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(140);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        gp.getColumnConstraints().setAll(c0, c1);

        return gp;
    }

    private VBox sectionCard(String title, Node content) {
        var titleLbl = new Label(title);
        titleLbl.getStyleClass().add("section-title");

        var header = new HBox(titleLbl);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("section-header");

        var body = new VBox(content);
        body.setSpacing(10);
        body.getStyleClass().add("section-body");

        var card = new VBox(header, body);
        card.setPadding(new Insets(14));
        card.setSpacing(10);
        card.getStyleClass().add("section-card");
        return card;
    }

    private Label smallNote(String text) {
        var l = new Label(text);
        l.getStyleClass().add("small-note");
        l.setWrapText(true);
        return l;
    }

    private Region spacer(double h) {
        var r = new Region();
        r.setMinHeight(h);
        return r;
    }

    private Spinner<Double> buildDoubleSpinner(double value, double min, double max, double step) {
        Spinner<Double> sp = new Spinner<>(min, max, value, step);
        sp.setEditable(true);
        // Pretty converter to avoid showing scientific or too many decimals
        sp.getValueFactory().setConverter(new StringConverter<>() {
            @Override public String toString(Double v) {
                if (v == null) return "";
                // trim trailing zeros
                String s = String.format(java.util.Locale.US, "%.3f", v);
                while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) {
                    s = s.substring(0, s.length() - 1);
                }
                return s;
            }
            @Override public Double fromString(String s) {
                try { return Double.parseDouble(s.trim()); }
                catch (Exception e) { return value; }
            }
        });
        return sp;
    }

    private double parseDoubleSafe(String s, double fallback) {
        try { return Double.parseDouble(s); } catch (Exception e) { return fallback; }
    }

    private void info(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void error(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private static String rootMsg(Throwable t) {
        Throwable x = t;
        while (x.getCause() != null) x = x.getCause();
        return x.getMessage();
    }
}


//package com.rudrainfotech.milkdiary.ui;
//
//import com.rudrainfotech.milkdiary.service.AppSettingsService;
//import javafx.geometry.Insets;
//import javafx.scene.Scene;
//import javafx.scene.control.*;
//import javafx.scene.layout.ColumnConstraints;
//import javafx.scene.layout.GridPane;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.Priority;
//import javafx.stage.Modality;
//import javafx.stage.Stage;
//
//public class SettingsView extends Stage {
//
//    public SettingsView() {
//        setTitle("Settings");
//        initModality(Modality.APPLICATION_MODAL);
//
//        AppSettingsService svc = new AppSettingsService();
//
//        TextField printedBy = new TextField(svc.getString(AppSettingsService.PRINTED_BY, "Operator"));
//        CheckBox fillMissing = new CheckBox("Fill missing days");
//        fillMissing.setSelected(svc.getBool(AppSettingsService.PDF_FILL_MISSING, true));
//
//        Spinner<Double> gapPt = new Spinner<>(0.0, 100.0, Double.parseDouble(
//                svc.getString(AppSettingsService.PDF_GAP_PT, "50")), 0.5);
//        gapPt.setEditable(true);
//
//        Spinner<Integer> defaultCap = new Spinner<>(1,3, svc.getInt(AppSettingsService.DEFAULT_CAP,1),1);
//        defaultCap.setEditable(true);
//
//        CheckBox lockOnGen = new CheckBox("Lock cap after Generate/Recalc");
//        lockOnGen.setSelected(svc.getBool(AppSettingsService.LOCK_ON_GENERATE, false));
//
//        TextField backupDir = new TextField(svc.getString(AppSettingsService.BACKUP_DIR, "backups"));
//        Spinner<Integer> keep = new Spinner<>(1, 120, svc.getInt(AppSettingsService.BACKUPS_KEEP, 14), 1);
//        keep.setEditable(true);
//
//        ComboBox<String> lang = new ComboBox<>();
//        lang.getItems().addAll("en", "mr-IN");
//        lang.setEditable(false);
//        lang.getSelectionModel().select(svc.getString(AppSettingsService.UI_LANG, "en"));
//
//
//        PasswordField oldPin = new PasswordField();
//        oldPin.setPromptText("Current PIN (if set)");
//
//        PasswordField newPin = new PasswordField();
//        newPin.setPromptText("New PIN");
//
//        PasswordField newPin2 = new PasswordField();
//        newPin2.setPromptText("Confirm New PIN");
//        Label pinNote = new Label("Leave PIN fields blank to keep current PIN");
//
//        Button backupNow = new Button("Backup Now");
//        Button openFolder = new Button("Open Folder");
//        Button restoreBtn = new Button("Restore…");
//
//        Button save = new Button("Save");
//        Button close = new Button("Close");
//
//        // handlers
//        backupNow.setOnAction(e -> {
//            try {
//                var f = new com.rudrainfotech.milkdiary.service.BackupService().backupNow();
//                new Alert(Alert.AlertType.INFORMATION, "Backup saved:\n" + f.getAbsolutePath()).showAndWait();
//            } catch (Exception ex) {
//                new Alert(Alert.AlertType.ERROR, "Backup failed:\n" + rootMsg(ex)).showAndWait();
//            }
//        });
//
//        openFolder.setOnAction(e -> {
//            try {
//                var svc1 = new com.rudrainfotech.milkdiary.service.AppSettingsService();
//                var dir = java.nio.file.Path.of(svc1.getString(AppSettingsService.BACKUP_DIR, "backups")).toAbsolutePath();
//                java.awt.Desktop.getDesktop().open(dir.toFile());
//            } catch (Exception ex) {
//                new Alert(Alert.AlertType.ERROR, "Open failed:\n" + rootMsg(ex)).showAndWait();
//            }
//        });
//
//        restoreBtn.setOnAction(e -> {
//            try {
//                var svc1 = new com.rudrainfotech.milkdiary.service.BackupService();
//                var chooser = new javafx.stage.FileChooser();
//                chooser.setTitle("Choose backup to restore");
//                chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("SQLite DB", "*.db"));
//                // default dir to backup folder
//                var app = new com.rudrainfotech.milkdiary.service.AppSettingsService();
//                var dir = java.nio.file.Path.of(app.getString(AppSettingsService.BACKUP_DIR, "backups")).toFile();
//                if (dir.isDirectory()) chooser.setInitialDirectory(dir);
//                var f = chooser.showOpenDialog(getOwner());
//                if (f == null) return;
//
//                // Confirm (warn about restart)
//                var confirm = new Alert(Alert.AlertType.CONFIRMATION,
//                        "Restore this backup?\n\n" + f.getName() + "\n\nThe app will apply it on next start.",
//                        ButtonType.OK, ButtonType.CANCEL);
//                confirm.setHeaderText("Confirm restore");
//                if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
//
//                svc1.queueRestore(f);
//                new Alert(Alert.AlertType.INFORMATION, "Restore queued.\nPlease restart the application.").showAndWait();
//            } catch (Exception ex) {
//                new Alert(Alert.AlertType.ERROR, "Restore failed:\n" + rootMsg(ex)).showAndWait();
//            }
//        });
//
//        save.setOnAction(e -> {
//            try {
//                svc.setString(AppSettingsService.PRINTED_BY, printedBy.getText().trim());
//                svc.setBool(AppSettingsService.PDF_FILL_MISSING, fillMissing.isSelected());
//                svc.setString(AppSettingsService.PDF_GAP_PT, String.valueOf(gapPt.getValue()));
//                svc.setInt(AppSettingsService.DEFAULT_CAP, defaultCap.getValue());
//                svc.setBool(AppSettingsService.LOCK_ON_GENERATE, lockOnGen.isSelected());
//                svc.setString(AppSettingsService.BACKUP_DIR, backupDir.getText().trim());
//                svc.setInt(AppSettingsService.BACKUPS_KEEP, keep.getValue());
//                svc.setString(AppSettingsService.UI_LANG, lang.getValue());
//
//
//                // --- Admin PIN logic ---
//                boolean wantsNewPin = !(newPin.getText().isBlank() && newPin2.getText().isBlank());
//                if (wantsNewPin) {
//                    if (!newPin.getText().equals(newPin2.getText()))
//                        throw new IllegalArgumentException("New PIN and Confirm PIN do not match.");
//
//                    String stored = svc.getString(AppSettingsService.ADMIN_PIN_HASH, null);
//                    if (stored != null && !stored.isBlank()) {
//                        // require old pin if one is already set
//                        if (!svc.verifyAdminPin(oldPin.getText()))
//                            throw new IllegalArgumentException("Old PIN is incorrect.");
//                    }
//                    svc.setAdminPin(newPin.getText());
//                } else if (!oldPin.getText().isBlank() && newPin.getText().isBlank() && newPin2.getText().isBlank()) {
//                    // ignore lone oldPin typed without setting a new pin
//                }
//
//                new Alert(Alert.AlertType.INFORMATION, "Saved. Please restart the app to apply language.").showAndWait();
//            } catch (Exception ex) {
//                var a = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
//                a.setHeaderText("Save failed"); a.showAndWait();
//            }
//        });
//        close.setOnAction(e -> close());
//
//        GridPane gp = new GridPane();
//        gp.setHgap(10); gp.setVgap(8); gp.setPadding(new Insets(12));
//        int r=0;
//        gp.add(new Label("Printed By:"),0,r); gp.add(printedBy,1,r++);
//
//        gp.add(fillMissing,0,r++,2,1);
//        gp.add(new Label("PDF gap (pt):"),0,r); gp.add(gapPt,1,r++);
//
//        gp.add(new Label("Default Cap:"),0,r); gp.add(defaultCap,1,r++);
//        gp.add(lockOnGen,0,r++,2,1);
//
//        gp.add(new Label("Language:"), 0, r);
//        gp.add(lang, 1, r++);
//
//
//
//        int pinStartRow = r;
//        gp.add(new Separator(), 0, pinStartRow++, 2, 1);
//        gp.add(new Label("Admin PIN:"), 0, pinStartRow++);
//        gp.add(new Label("Old PIN:"), 0, pinStartRow); gp.add(oldPin, 1, pinStartRow++);
//        gp.add(new Label("New PIN:"), 0, pinStartRow); gp.add(newPin, 1, pinStartRow++);
//        gp.add(new Label("Confirm PIN:"), 0, pinStartRow); gp.add(newPin2, 1, pinStartRow++);
//        gp.add(pinNote, 1, pinStartRow++);
//
//        r = pinStartRow;
//
//        gp.add(new Label("Backup Folder:"),0,r); gp.add(backupDir,1,r++);
//        gp.add(new Label("Backups to Keep:"),0,r); gp.add(keep,1,r++);
//        gp.add(new HBox(8, backupNow, openFolder, restoreBtn), 1, r++);
//
//        gp.add(save,0,r); gp.add(close,1,r);
//
//        ColumnConstraints c0 = new ColumnConstraints();
//        ColumnConstraints c1 = new ColumnConstraints(); c1.setHgrow(Priority.ALWAYS);
//        gp.getColumnConstraints().setAll(c0, c1);
//
//        setScene(new Scene(gp, 420, 500));
//    }
//
//    private static String rootMsg(Throwable t){ Throwable x=t; while(x.getCause()!=null) x=x.getCause(); return x.getMessage(); }
//
//}
