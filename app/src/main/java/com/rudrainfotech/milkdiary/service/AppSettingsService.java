package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.entity.AppSetting;
import com.rudrainfotech.milkdiary.entity.Outlet;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

public class AppSettingsService {
    public static final String ACTIVE_OUTLET_ID = "active_outlet_id";

    public void delete(String key) {
        Tx.tx(s -> {
            AppSetting as = s.get(AppSetting.class, key);
            if (as != null) s.remove(as);
            return null;
        });
    }

    public Long getActiveOutletId() {
        return Tx.tx((Session s) -> {
            AppSetting st = s.find(AppSetting.class, ACTIVE_OUTLET_ID);
            if (st == null || st.getValue() == null || st.getValue().isBlank()) return null;
            try { return Long.parseLong(st.getValue()); } catch (NumberFormatException e) { return null; }
        });
    }
    public void setActiveOutletId(Long outletId) {
        Tx.tx((Session s) -> {
            AppSetting st = s.find(AppSetting.class, ACTIVE_OUTLET_ID);
            if (st == null) st = new AppSetting(ACTIVE_OUTLET_ID, null);
            st.setValue(outletId == null ? null : Long.toString(outletId));
            s.merge(st);
            return null;
        });
    }

    /** @return active outlet or null (does not auto-create/select) */
    public Outlet getActiveOutlet() {
        Long id = getActiveOutletId();
        if (id == null) return null;
        return Tx.tx(s -> s.get(Outlet.class, id));
    }

    /** If no active outlet is set but there is at least one outlet, set the earliest one. */
    // AppSettingsService.java
    public Outlet ensureActiveOutletExists() {
        return Tx.tx(s -> {
            // try saved id
            String raw = getString(ACTIVE_OUTLET_ID, null);
            if (raw != null && !raw.isBlank()) {
                try {
                    Long id = Long.parseLong(raw.trim());
                    Outlet o = s.get(Outlet.class, id);
                    if (o != null) return o; // good
                } catch (NumberFormatException ignore) {
                    // fall through to pick first outlet
                }
            }

            // pick first outlet and save it as active
            Outlet first = s.createQuery("from Outlet o order by o.id asc", Outlet.class)
                    .setMaxResults(1)
                    .uniqueResult();
            if (first != null) {
                setString(ACTIVE_OUTLET_ID, Long.toString(first.getId())); // persist setting
                return first;
            }

            // none exist at all
            throw new IllegalStateException("No outlet exists. Please create an outlet first.");
        });
    }


    public String getString(String key, String def){
        return Tx.tx(s -> {
            AppSetting as = s.get(AppSetting.class, key);
            return as==null ? def : as.getValue();
        });
    }
    public void setString(String key, String val){
        Tx.tx(s -> {
            AppSetting as = s.get(AppSetting.class, key);
            if (as==null) s.persist(new AppSetting(key, val));
            else { as.setValue(val); s.merge(as); }
            return null;
        });
    }

    public boolean getBool(String key, boolean def){
        String v = getString(key, String.valueOf(def));
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }
    public void setBool(String key, boolean val){ setString(key, String.valueOf(val)); }

    public int getInt(String key, int def){
        try { return Integer.parseInt(getString(key, String.valueOf(def))); }
        catch(Exception e){ return def; }
    }
    public void setInt(String key, int val){ setString(key, String.valueOf(val)); }

    public float getFloat(String key, float defVal) {
        try { return Float.parseFloat(getString(key, String.valueOf(defVal))); }
        catch (Exception e) { return defVal; }
    }
    public void setFloat(String key, float val) { setString(key, Float.toString(val)); }

    public BigDecimal getDec(String key, BigDecimal def){
        try { return new BigDecimal(getString(key, def.toPlainString())); }
        catch(Exception e){ return def; }
    }
    public void setDec(String key, BigDecimal val){ setString(key, val.toPlainString()); }

    // Common keys (avoid typos)
    public static final String PRINTED_BY = "printed_by";
    public static final String PDF_GAP_PT = "pdf_gap_pt";
    public static final String PDF_FILL_MISSING = "pdf_fill_missing_days";
    public static final String LOCK_ON_GENERATE = "lock_on_generate";
    public static final String DEFAULT_CAP = "default_cap";
    public static final String BILLING_ROUND_MODE = "billing_round_mode";
    public static final String BILLING_ROUND_TO = "billing_round_to";
    public static final String BACKUP_DIR = "backup_dir";
    public static final String BACKUPS_KEEP = "backups_keep";
    public static final String ADMIN_PIN_HASH = "admin_pin_hash";        // algo$salt$hash
    public static final String UI_LANG = "ui_lang"; // e.g. "en" or "mr-IN"


    // ---- defaults (call once at startup)
    public void ensureDefaults() {
        if (getString(PRINTED_BY, null) == null) setString(PRINTED_BY, System.getProperty("user.name", "user"));
        if (getString(PDF_GAP_PT, null) == null) setFloat(PDF_GAP_PT, 50f);
        if (getString(PDF_FILL_MISSING, null) == null) setBool(PDF_FILL_MISSING, true);
        if (getString(LOCK_ON_GENERATE, null) == null) setBool(LOCK_ON_GENERATE, false);
        // ADMIN_PIN_HASH intentionally not set by default
    }

    // ---- Admin PIN
    public void setAdminPin(String newPin) {
        if (newPin == null || newPin.isBlank()) {
            setString(ADMIN_PIN_HASH, null);
            return;
        }
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltHex = HexFormat.of().formatHex(salt);
        String hashHex = sha256Hex(newPin + ":" + saltHex);
        setString(ADMIN_PIN_HASH, "sha256$" + saltHex + "$" + hashHex);
    }

    public boolean verifyAdminPin(String pin) {
        String stored = getString(ADMIN_PIN_HASH, null);
        if (stored == null || stored.isBlank()) return false;
        String[] parts = stored.split("\\$");
        if (parts.length != 3) return false;
        String algo = parts[0], saltHex = parts[1], goodHash = parts[2];
        if (!"sha256".equalsIgnoreCase(algo)) return false;
        String got = sha256Hex(pin + ":" + saltHex);
        return MessageDigest.isEqual(goodHash.getBytes(), got.getBytes());
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

}
