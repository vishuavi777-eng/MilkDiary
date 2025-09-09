package com.rudrainfotech.milkdiary.db;

import java.io.InputStream;
import java.util.Properties;

public final class Props {
    private static final Properties P = new Properties();
    static {
        try (InputStream in = Props.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) P.load(in);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static String jdbcUrl()    { return P.getProperty("db.url", "jdbc:sqlite:./data/milkdiary.db"); }
    public static String dialect()    { return P.getProperty("hibernate.dialect"); }
    public static String hbm2ddl()    { return P.getProperty("hibernate.hbm2ddl.auto","none"); }
    public static boolean showSql()   { return Boolean.parseBoolean(P.getProperty("hibernate.show_sql","false")); }
    public static boolean formatSql() { return Boolean.parseBoolean(P.getProperty("hibernate.format_sql","true")); }
    public static int poolSize()      { return Integer.parseInt(P.getProperty("hikari.maxPoolSize","5")); }
}
