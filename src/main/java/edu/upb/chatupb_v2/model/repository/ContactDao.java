package edu.upb.chatupb_v2.model.repository;

import edu.upb.chatupb_v2.model.entities.Contact;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.sql.*;
import java.util.List;

/**
 * DAO para la tabla 'contact'.
 *
 * Schema:
 *   CREATE TABLE contact (
 *       id   INTEGER PRIMARY KEY AUTOINCREMENT,
 *       code TEXT NOT NULL UNIQUE,
 *       name TEXT NOT NULL,
 *       ip   TEXT NOT NULL
 *   );
 *   CREATE INDEX IF NOT EXISTS idx_contact_code ON contact(code);
 *   CREATE INDEX IF NOT EXISTS idx_contact_ip   ON contact(ip);
 */
@Slf4j
public class ContactDao {

    private DaoHelper<Contact> helper;

    public ContactDao() {
        helper = new DaoHelper<>();
    }

    /**
     * Crea la tabla 'contact' si no existe, con restriccion UNIQUE en code.
     * Si la tabla ya existe con un schema viejo (sin UNIQUE), la migra.
     * Tambien crea indices para code e ip.
     */
    public void createTableIfNotExists() {
        try (Connection conn = ConnectionDB.getInstance().getConection();
             Statement st = conn.createStatement()) {

            // Verificar si la tabla existe
            boolean tablaExiste = false;
            boolean tieneUnique = false;
            try (ResultSet rs = conn.getMetaData().getTables(null, null, "contact", null)) {
                if (rs.next()) {
                    tablaExiste = true;
                }
            }

            if (tablaExiste) {
                // Verificar si tiene UNIQUE en code revisando los indices
                try (ResultSet rs = st.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='contact'")) {
                    if (rs.next()) {
                        String createSql = rs.getString("sql");
                        if (createSql != null && createSql.toUpperCase().contains("UNIQUE")) {
                            tieneUnique = true;
                        }
                    }
                }

                if (!tieneUnique) {
                    // Migrar: recrear tabla con UNIQUE
                    log.info("Migrando tabla contact: agregando UNIQUE a code...");
                    st.execute("ALTER TABLE contact RENAME TO contact_old");
                    st.execute("CREATE TABLE contact ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "code TEXT NOT NULL UNIQUE, "
                            + "name TEXT NOT NULL, "
                            + "ip TEXT NOT NULL"
                            + ")");
                    st.execute("INSERT OR IGNORE INTO contact(id, code, name, ip) "
                            + "SELECT id, code, name, ip FROM contact_old");
                    st.execute("DROP TABLE contact_old");
                    log.info("Migracion de tabla contact completada.");
                }
            } else {
                // Crear tabla nueva desde cero
                st.execute("CREATE TABLE contact ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "code TEXT NOT NULL UNIQUE, "
                        + "name TEXT NOT NULL, "
                        + "ip TEXT NOT NULL"
                        + ")");
            }

            // Crear indices si no existen
            st.execute("CREATE INDEX IF NOT EXISTS idx_contact_code ON contact(code)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_contact_ip ON contact(ip)");

        } catch (SQLException e) {
            log.error("Error al crear/migrar tabla contact: {}", e.getMessage());
        }
    }

    // --- ResultReader ---

    DaoHelper.ResultReader<Contact> resultReader = result -> {
        Contact contact = new Contact();
        if (existColumn(result, Contact.Column.ID)) {
            contact.setId(result.getLong(Contact.Column.ID));
        }
        if (existColumn(result, Contact.Column.CODE)) {
            contact.setCode(result.getString(Contact.Column.CODE));
        }
        if (existColumn(result, Contact.Column.NAME)) {
            contact.setName(result.getString(Contact.Column.NAME));
        }
        if (existColumn(result, Contact.Column.IP)) {
            contact.setIp(result.getString(Contact.Column.IP));
        }
        return contact;
    };

    /**
     * Verifica si una columna existe en el ResultSet actual.
     * Usado por todos los DAOs para lectura defensiva de columnas.
     */
    public static boolean existColumn(ResultSet result, String columnName) {
        try {
            result.findColumn(columnName);
            return true;
        } catch (SQLException sqlex) {
            // Columna no existe en el ResultSet
        }
        return false;
    }

    // --- CRUD ---

    public void save(Contact contact) throws Exception {
        String query = "INSERT INTO contact(code, name, ip) VALUES (?, ?, ?)";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, contact.getCode());
            pst.setString(2, contact.getName());
            pst.setString(3, contact.getIp());
        };
        helper.insert(query, params, contact);
    }

    public void update(Contact contact) throws Exception {
        String query = "UPDATE contact SET ip = ?, name = ? WHERE code = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, contact.getIp());
            pst.setString(2, contact.getName());
            pst.setString(3, contact.getCode());
        };
        helper.update(query, params);
    }

    public void delete(long id) throws ConnectException, SQLException {
        String query = "DELETE FROM contact WHERE id = ?";
        DaoHelper.QueryParameters params = pst -> pst.setLong(1, id);
        helper.update(query, params);
    }

    // --- Queries ---

    public List<Contact> findAll() throws ConnectException, SQLException {
        String query = "SELECT * FROM contact ORDER BY name ASC";
        return helper.executeQuery(query, resultReader);
    }

    public Contact findByCode(String code) throws ConnectException, SQLException {
        String query = "SELECT * FROM contact WHERE code = ?";
        DaoHelper.QueryParameters params = pst -> pst.setString(1, code);
        List<Contact> list = helper.executeQuery(query, params, resultReader);
        return list.isEmpty() ? null : list.get(0);
    }

    public Contact findByIp(String ip) throws ConnectException, SQLException {
        String query = "SELECT * FROM contact WHERE ip = ?";
        DaoHelper.QueryParameters params = pst -> pst.setString(1, ip);
        List<Contact> list = helper.executeQuery(query, params, resultReader);
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean existByCode(String code) throws ConnectException, SQLException {
        String query = "SELECT count(*) FROM contact WHERE code = ?";
        DaoHelper.QueryParameters params = pst -> pst.setString(1, code);
        return helper.executeQueryCount(query, params) >= 1;
    }
}
