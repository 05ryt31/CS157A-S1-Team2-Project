package com.flowchain.db;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests run against H2 in-memory (MODE=MySQL) via src/test/resources/db.properties,
 * which shadows the real db.properties on the test classpath.
 */
class DBConnectionTest {

    @Test
    void classIsFinal_andConstructorIsPrivate() throws Exception {
        assertTrue(Modifier.isFinal(DBConnection.class.getModifiers()),
            "DBConnection must be final — it's a utility class");

        Constructor<DBConnection> ctor = DBConnection.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()),
            "constructor must be private");
    }

    @Test
    void mysqlDriver_isRegisteredAfterClassLoad() throws Exception {
        // Touch the class to trigger the static initializer.
        Class.forName(DBConnection.class.getName());

        Enumeration<java.sql.Driver> drivers = DriverManager.getDrivers();
        List<java.sql.Driver> loaded = Collections.list(drivers);

        boolean mysqlPresent = loaded.stream()
            .anyMatch(d -> d.getClass().getName().equals("com.mysql.cj.jdbc.Driver"));

        assertTrue(mysqlPresent,
            "MySQL driver should be registered by DBConnection's static init. Loaded: " + loaded);
    }

    @Test
    void get_returnsOpenConnection() throws Exception {
        try (Connection conn = DBConnection.get()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed(), "connection should be open");
        }
    }

    @Test
    void get_returnsFreshConnectionEachCall() throws Exception {
        try (Connection a = DBConnection.get();
             Connection b = DBConnection.get()) {
            assertNotSame(a, b, "each call must hand out a new Connection — no pooling/sharing");
            assertFalse(a.isClosed());
            assertFalse(b.isClosed());
        }
    }

    @Test
    void get_connectionExecutesSimpleQuery() throws Exception {
        try (Connection conn = DBConnection.get();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            assertTrue(rs.next(), "SELECT 1 should return one row");
            assertEquals(1, rs.getInt(1));
        }
    }
}
