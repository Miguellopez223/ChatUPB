package edu.upb.chatupb_v2.model.repository;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton que gestiona la conexion a la base de datos SQLite.
 * Cada llamada a getConection() retorna una nueva conexion (patron open-per-request).
 * Habilita foreign keys de SQLite via PRAGMA.
 *
 */
public class ConnectionDB {

    private static final ConnectionDB connection = new ConnectionDB();
    //private static final String DB_URL = "jdbc:sqlite:chat_upb.sqlite";
    private String dbUrl = null; // Reemplazamos el DB_URL constante
    private static boolean driverLoaded = false;

    private ConnectionDB() {
    }

    public static ConnectionDB getInstance() {
        return connection;
    }

    // Genera una ruta segura en la carpeta del usuario (ej: C:\Users\Miguel\.chatupb\chat_upb.sqlite)
    private String getDbUrl() {
        if (dbUrl == null) {
            String userHome = System.getProperty("user.home");
            File appDir = new File(userHome, ".chatupb");
            if (!appDir.exists()) {
                appDir.mkdirs(); // Crea la carpeta si no existe
            }
            File dbFile = new File(appDir, "chat_upb.sqlite");
            dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            System.out.println("[ConnectionDB] Ruta de la BD: " + dbUrl);
        }
        return dbUrl;
    }

    public Connection getConection() {
        Connection conn = null;
        try {
            if (!driverLoaded) {
                Class.forName("org.sqlite.JDBC");
                driverLoaded = true;
            }
            //Usar la ruta dinámica
            conn = DriverManager.getConnection(getDbUrl());

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
