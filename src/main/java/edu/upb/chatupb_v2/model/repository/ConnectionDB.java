package edu.upb.chatupb_v2.model.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton que gestiona la conexion a la base de datos SQLite.
 * Cada llamada a getConection() retorna una nueva conexion (patron open-per-request).
 * Habilita foreign keys de SQLite via PRAGMA.
 *
 * @author rlaredo
 */
public class ConnectionDB {

    private static final ConnectionDB connection = new ConnectionDB();
    private static final String DB_URL = "jdbc:sqlite:chat_upb.sqlite";
    private static boolean driverLoaded = false;

    private ConnectionDB() {
    }

    public static ConnectionDB getInstance() {
        return connection;
    }

    public Connection getConection() {
        Connection conn = null;
        try {
            if (!driverLoaded) {
                Class.forName("org.sqlite.JDBC");
                driverLoaded = true;
            }
            conn = DriverManager.getConnection(DB_URL);

            // Habilitar foreign keys en SQLite (deshabilitadas por defecto)
            if (conn != null) {
                try (Statement st = conn.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON");
                }
            }
        } catch (SQLException e) {
            System.err.println("[ConnectionDB] Error de conexion: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("[ConnectionDB] Driver SQLite no encontrado: " + e.getMessage());
        }
        return conn;
    }
}
