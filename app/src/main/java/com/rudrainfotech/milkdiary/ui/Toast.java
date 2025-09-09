package com.rudrainfotech.milkdiary.ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/** Minimal reusable toast. Call Toast.success(this, "Saved") from any Node. */
public final class Toast {
    private Toast() {}

    public static void success(Node anchor, String text) { show(anchor, text, 2000, "toast", "toast-success"); }
    public static void info(Node anchor, String text)    { show(anchor, text, 2200, "toast", "toast-info"); }
    public static void warn(Node anchor, String text)    { show(anchor, text, 2600, "toast", "toast-warn"); }
    public static void error(Node anchor, String text)   { show(anchor, text, 3200, "toast", "toast-error"); }

    public static void show(Node anchor, String text, int millis, String... styleClasses) {
        if (anchor == null) return;
        Platform.runLater(() -> {
            Window w = anchor.getScene() == null ? null : anchor.getScene().getWindow();
            if (w == null) return;

            Label label = new Label(text);
            label.getStyleClass().addAll(styleClasses);
            label.setMinHeight(Region.USE_PREF_SIZE);
            label.setWrapText(true);

            // popup
            Popup popup = new Popup();
            popup.setAutoFix(true);
            popup.setAutoHide(true);
            popup.getContent().add(label);

            // size cap
            double maxWidth = Math.min(420, Math.max(260, w.getWidth() * 0.55));
            label.setMaxWidth(maxWidth);

            // show first to measure
            popup.show(w);

            // position bottom-center with small margin
            double x = w.getX() + (w.getWidth() - label.getWidth()) / 2.0;
            double y = w.getY() + w.getHeight() - label.getHeight() - 24.0;
            popup.setX(x);
            popup.setY(y);

            // animations
            label.setOpacity(0);
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(140), label);
            slideIn.setFromY(10); slideIn.setToY(0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(140), label);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);

            PauseTransition stay = new PauseTransition(Duration.millis(millis));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(180), label);
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> popup.hide());

            new SequentialTransition(new ParallelTransition(slideIn, fadeIn), stay, fadeOut).play();
        });
    }
}
