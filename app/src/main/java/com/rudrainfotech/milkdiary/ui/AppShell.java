package com.rudrainfotech.milkdiary.ui;

import com.rudrainfotech.milkdiary.Bootstrap;
import com.rudrainfotech.milkdiary.entity.Outlet;
import com.rudrainfotech.milkdiary.i18n.I18n;
import com.rudrainfotech.milkdiary.service.AppSettingsService;
import com.rudrainfotech.milkdiary.service.OutletService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.css.PseudoClass;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import java.util.Locale;

public class AppShell extends BorderPane {

    private final OutletService outletSvc = new OutletService();

    // status bar bits
    private final Label statusLeft  = new Label();
    private final Label statusRight = new Label();

    private final ToggleGroup navGroup = new ToggleGroup();
    private final PseudoClass PC_SELECTED = PseudoClass.getPseudoClass("selected");

    // keep handles if you want to setSelected programmatically
    private ToggleButton dailyBtn, membersBtn, rateBtn, monthBtn, savingsBtn, outletSumBtn, outletsBtn;

    public AppShell() {
        // 1) Locale from settings
        var svc = new com.rudrainfotech.milkdiary.service.AppSettingsService();
        String tag = svc.getString(AppSettingsService.UI_LANG, "en");
        Locale locale = tag.equalsIgnoreCase("mr-IN") ? new Locale("mr", "IN")
                : new Locale(tag);
        Locale.setDefault(locale);
        I18n.init(locale);

        // 2) Load fonts for Marathi
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/NotoSansDevanagari-Regular.ttf"), 12);
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/NotoSansDevanagari-Bold.ttf"), 12);

        setLeft(buildNav());
        setBottom(buildStatusBar());
        refreshStatus();
    }

//    private Node buildNav() {
//        var dailyBtn    = makeNavToggle(I18n.t("menu.daily"));
//        var membersBtn  = makeNavToggle(I18n.t("menu.members"));
//        var rateBtn  = makeNavToggle(I18n.t("menu.ratePlans"));
//        var monthBtn  = makeNavToggle(I18n.t("menu.billing"));
//        var outletSumBtn  = makeNavToggle(I18n.t("menu.outletSummary"));
//        var outletsBtn  = makeNavToggle(I18n.t("menu.outlets"));
//
//        // non-toggle: Settings opens as MODAL (your SettingsView)
//        var settingsBtn = new Button(I18n.t("menu.settings"));
//        settingsBtn.setMaxWidth(Double.MAX_VALUE);
//        settingsBtn.setOnAction(e -> {
//            new SettingsView().showAndWait();
//            refreshStatus(); // in case Printed By / anything else affects UI later
//        });
//
//        // handlers to swap center content
//        dailyBtn.setOnAction(e -> setCenter(new DailyEntriesView()));
//        rateBtn.setOnAction(e -> setCenter(new RatePlansView()));
//        monthBtn.setOnAction(e -> setCenter(new MonthlyBillingView()));
//        outletSumBtn.setOnAction(e -> setCenter(new OutletSummaryView()));
//        outletsBtn.setOnAction(e -> setCenter(new OutletsView()));
//        membersBtn.setOnAction(e -> openMembersForActiveOutlet());
//
//        var sep1 = new Separator();
//        var sep2 = new Separator();
//
//        VBox nav = new VBox(8,
//                title(I18n.t("app.title")),
//                sep1,
//                dailyBtn,
//                membersBtn,
//                rateBtn,
//                monthBtn,
//                outletSumBtn,
//                outletsBtn,
//                sep2,
//                settingsBtn
//        );
//        nav.setPadding(new Insets(12));
//        nav.setPrefWidth(220);
//        nav.getStyleClass().add("nav-pane");
//        return nav;
//    }

    private Node buildNav() {
        // build buttons
        dailyBtn     = makeNavToggle(I18n.t("menu.daily"));
        membersBtn   = makeNavToggle(I18n.t("menu.members"));
        rateBtn      = makeNavToggle(I18n.t("menu.ratePlans"));
        monthBtn     = makeNavToggle(I18n.t("menu.billing"));
        savingsBtn   = makeNavToggle(I18n.t("menu.savings"));
        outletSumBtn = makeNavToggle(I18n.t("menu.outletSummary"));
        outletsBtn   = makeNavToggle(I18n.t("menu.outlets"));

        // put them in one ToggleGroup so only one can be selected
        for (var b : new ToggleButton[]{ dailyBtn, membersBtn, rateBtn, monthBtn, savingsBtn, outletSumBtn, outletsBtn }) {
            b.setToggleGroup(navGroup);
        }

        // map each button to a center-view supplier (lazy construction)
        Map<ToggleButton, Supplier<Node>> routes = new LinkedHashMap<>();
        routes.put(dailyBtn,     DailyEntriesView::new);
        routes.put(membersBtn,   () -> { openMembersForActiveOutlet(); return getCenter(); }); // special
        routes.put(rateBtn,      RatePlansView::new);
        routes.put(monthBtn,     MonthlyBillingView::new);
        routes.put(savingsBtn,   FinancialPeriodView::new);
        routes.put(outletSumBtn, OutletSummaryView::new);
        routes.put(outletsBtn,   OutletsView::new);

        // when selection changes → swap center + restyle
        navGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            // visual state
            if (oldT instanceof ToggleButton ob) ob.pseudoClassStateChanged(PC_SELECTED, false);
            if (newT instanceof ToggleButton nb) nb.pseudoClassStateChanged(PC_SELECTED, true);

            // set center
            if (newT instanceof ToggleButton nb) {
                var factory = routes.get(nb);
                if (factory != null) setCenter(factory.get());
            }
        });

        // optional: set tooltips
        dailyBtn.setTooltip(new Tooltip(I18n.t("menu.daily")));
        membersBtn.setTooltip(new Tooltip(I18n.t("menu.members")));
        rateBtn.setTooltip(new Tooltip(I18n.t("menu.ratePlans")));
        monthBtn.setTooltip(new Tooltip(I18n.t("menu.billing")));
        savingsBtn.setTooltip(new Tooltip(I18n.t("menu.savings")));
        outletSumBtn.setTooltip(new Tooltip(I18n.t("menu.outletSummary")));
        outletsBtn.setTooltip(new Tooltip(I18n.t("menu.outlets")));

        // non-toggle button (modal)
        var settingsBtn = new Button(I18n.t("menu.settings"));
        settingsBtn.setMaxWidth(Double.MAX_VALUE);
        settingsBtn.getStyleClass().add("nav-settings-btn");
        settingsBtn.setOnAction(e -> {
            new SettingsView().showAndWait();
            refreshStatus();
        });

        var sep1 = new Separator();
        var sep2 = new Separator();

        VBox nav = new VBox(8,
                title(I18n.t("app.title")),
                sep1,
                dailyBtn,
                membersBtn,
                rateBtn,
                monthBtn,
                savingsBtn,
                outletSumBtn,
                outletsBtn,
                sep2,
                settingsBtn
        );
        nav.setPadding(new Insets(12));
        nav.setPrefWidth(220);
        nav.getStyleClass().add("nav-pane");

        dailyBtn.setSelected(true);

        return nav;
    }


    // call once after Scene is created
    public void attachToScene(javafx.scene.Scene scene) {
        var a = scene.getAccelerators();

        // Save: Cmd/Ctrl+S
        a.put(new javafx.scene.input.KeyCodeCombination(
                javafx.scene.input.KeyCode.S,
                javafx.scene.input.KeyCombination.SHORTCUT_DOWN), () -> {
            var c = getCenter();
            if (c instanceof StandardActions sa) sa.actionSave();
        });

        // Refresh: Cmd/Ctrl+R and F5
        Runnable refresh = () -> {
            var c = getCenter();
            if (c instanceof StandardActions sa) sa.actionRefresh();
        };
        a.put(new javafx.scene.input.KeyCodeCombination(
                javafx.scene.input.KeyCode.R,
                javafx.scene.input.KeyCombination.SHORTCUT_DOWN), refresh);
        a.put(new javafx.scene.input.KeyCodeCombination(
                javafx.scene.input.KeyCode.F5), refresh);

        // Recompute: Cmd/Ctrl+E
        a.put(new javafx.scene.input.KeyCodeCombination(
                javafx.scene.input.KeyCode.E,
                javafx.scene.input.KeyCombination.SHORTCUT_DOWN), () -> {
            var c = getCenter();
            if (c instanceof StandardActions sa) sa.actionRecompute();
        });
    }


//    private void openMembersForActiveOutlet() {
//        Outlet outlet = outletSvc.getActiveOutlet();
//        if (outlet == null) {
//            // try to bootstrap once (creates data dir, runs flyway, seeds outlet & active id)
//            Bootstrap.runOnce();
//            outlet = outletSvc.getActiveOutlet();
//        }
//        if (outlet == null) {
//            alert(Alert.AlertType.WARNING, I18n.t("warning.noOutletFound"));
//            setCenter(new OutletsView());
//            return;
//        }
//        setCenter(new MembersView(outlet));
//    }

    private void openMembersForActiveOutlet() {
        Outlet outlet = outletSvc.getActiveOutlet();
        if (outlet == null) {
            Bootstrap.runOnce();
            outlet = outletSvc.getActiveOutlet();
        }
        if (outlet == null) {
            alert(Alert.AlertType.WARNING, I18n.t("warning.noOutletFound"));
            outletsBtn.setSelected(true); // move user to Outlets screen
            return;
        }
        setCenter(new MembersView(outlet));
        // keep the left nav in sync
        membersBtn.setSelected(true);
    }


    // --- helpers ---

//    private ToggleButton makeNavToggle(String text) {
//        ToggleButton b = new ToggleButton(text);
//        b.setMaxWidth(Double.MAX_VALUE);
//        b.getStyleClass().add("nav-btn");
//        return b;
//    }

    private ToggleButton makeNavToggle(String text) {
        ToggleButton b = new ToggleButton(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.getStyleClass().add("nav-btn");
        b.setFocusTraversable(true);
        b.setMnemonicParsing(false);
        b.setOnAction(e -> {
            // ensure ToggleGroup selection is honored even if user clicks a selected one
            if (!b.isSelected()) b.setSelected(true);
        });
        return b;
    }


    private Label title(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("nav-title");
        return l;
    }

    private Node welcome() {
        var lbl = new Label(I18n.t("hint.selectModule"));
        lbl.getStyleClass().add("welcome-msg");
        var box = new StackPane(lbl);
        box.setPadding(new Insets(24));
        return box;
    }

    private Node buildStatusBar() {
        HBox bar = new HBox(12, statusLeft, new Region(), statusRight);
        HBox.setHgrow(bar.getChildren().get(1), Priority.ALWAYS);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 10, 6, 10));
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private void refreshStatus() {
        Outlet o = outletSvc.getActiveOutlet();
        statusLeft.setText(o == null ? "Outlet: (none)" : "Outlet: " + safe(o.getName()));
        statusRight.setText("Shortcuts: Ctrl/Cmd+, Settings | Ctrl/Cmd+B Backup");
    }

    private void alert(Alert.AlertType t, String msg) {
        var a = new Alert(t, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
