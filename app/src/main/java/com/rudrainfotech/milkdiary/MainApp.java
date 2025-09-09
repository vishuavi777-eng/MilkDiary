package com.rudrainfotech.milkdiary;

import com.rudrainfotech.milkdiary.i18n.I18n;
import com.rudrainfotech.milkdiary.service.AppSettingsService;
import com.rudrainfotech.milkdiary.ui.AppShell;
import com.rudrainfotech.milkdiary.ui.SettingsView;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        // 1) Ensure ./data exists, run Flyway migrations, seed defaults / active outlet
        try {
            Bootstrap.runOnce();
        } catch (Exception ex) {
            error("Startup (Flyway/bootstrap) failed:\n" + rootMsg(ex));
        }

        // 2) Main shell (single window with left nav + status bar)
        AppShell shell = new AppShell();
        Scene scene = new Scene(shell);
//        Scene scene = new Scene(shell, 1200, 750);

        shell.attachToScene(scene);


        // 3) Global stylesheet
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/css/app.css")).toExternalForm()
        );

        // 4) Global accelerators (Settings, Backup)
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN),
                () -> new SettingsView().showAndWait()
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN),
                () -> {
                    try {
                        var svc = new AppSettingsService();
                        Path dataDir = Paths.get("./data");
                        Path backupDir = Paths.get(svc.getString(AppSettingsService.BACKUP_DIR, "backups"));
                        int keep = svc.getInt(AppSettingsService.BACKUPS_KEEP, 14);
                        Path zip = new com.rudrainfotech.milkdiary.service.BackupService()
                                .backupNow(dataDir, backupDir, keep);
                        info("Backup saved:\n" + zip.toAbsolutePath());
                    } catch (Exception ex) {
                        error("Backup failed:\n" + rootMsg(ex));
                    }
                }
        );

        // 5) Stage
        stage.setScene(scene);
        stage.setTitle(I18n.t("app.title"));

        // get screen dimensions
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        // compute 95%
        double w = screenBounds.getWidth() * 0.95;
        double h = screenBounds.getHeight() * 0.98;
        stage.setWidth(w);
        stage.setHeight(h);
        // center on screen
        stage.setX((screenBounds.getWidth() - w) / 2);
        stage.setY((screenBounds.getHeight() - h) / 2);

        stage.show();
    }

    private static void info(String m){ var a=new Alert(Alert.AlertType.INFORMATION,m, ButtonType.OK); a.setHeaderText(null); a.showAndWait();}
    private static void error(String m){ var a=new Alert(Alert.AlertType.ERROR,m,ButtonType.OK); a.setHeaderText("Error"); a.showAndWait();}
    private static String rootMsg(Throwable t){ Throwable x=t; while(x.getCause()!=null) x=x.getCause(); return x.getMessage(); }

    public static void main(String[] args) { launch(args); }
}