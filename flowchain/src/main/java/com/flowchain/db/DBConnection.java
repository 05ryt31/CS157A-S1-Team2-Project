package com.flowchain.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Loads MySQL connection settings from db.properties on the classpath
 * and hands out fresh JDBC Connections.
 *
 * Each call to get() opens a new connection; callers are responsible
 * for closing it (use try-with-resources).
 */
public final class DBConnection {

    private static final String PROPERTIES_FILE = "db.properties";
    private static final Properties PROPS = loadProperties();

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                "MySQL JDBC driver not found. Ensure mysql-connector-j is in WEB-INF/lib/.", e);
        }
    }

    private DBConnection() {
        // utility class
    }

    public static Connection get() throws SQLException {
        String url      = required("db.url");
        String user     = required("db.user");
        String password = required("db.password");
        return DriverManager.getConnection(url, user, password);
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new IllegalStateException(
                    PROPERTIES_FILE + " not found on classpath. "
                    + "Copy db.properties.example to src/main/resources/db.properties.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + PROPERTIES_FILE, e);
        }
        return props;
    }

    private static String required(String key) {
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return value;
    }
}
