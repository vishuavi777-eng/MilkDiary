package com.rudrainfotech.milkdiary.i18n;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public final class I18n {
    private static ResourceBundle BUNDLE =
            ResourceBundle.getBundle("i18n/strings", Locale.getDefault(), new UTF8Control());

    private I18n() {}

    public static void init(Locale locale) {
        Locale.setDefault(locale);
        BUNDLE = ResourceBundle.getBundle("i18n/strings", locale, new UTF8Control());
    }

    public static String t(String key, Object... args) {
        String s;
        try {
            s = BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            s = key; // fallback to key
        }
        return (args == null || args.length == 0) ? s : MessageFormat.format(s, args);
    }

    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(
                String baseName, Locale locale, String format, ClassLoader loader, boolean reload
        ) throws IllegalAccessException, InstantiationException, java.io.IOException {

            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            try (InputStream is = loader.getResourceAsStream(resourceName)) {
                if (is == null) return null;
                try (Reader rd = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    return new PropertyResourceBundle(rd);
                }
            }
        }
    }
}
