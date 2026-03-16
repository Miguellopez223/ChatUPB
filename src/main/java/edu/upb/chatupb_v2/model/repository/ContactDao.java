package edu.upb.chatupb_v2.model.repository;

import edu.upb.chatupb_v2.model.entities.Contact;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.sql.*;
import java.util.List;

/**
 * DAO para la tabla 'contact'.
 */
@Slf4j
public class ContactDao implements IContactDao {

    private DaoHelper<Contact> helper;
    private long currentUserId;

    public ContactDao() {
        this.helper = new DaoHelper<>();
        this.currentUserId = 0;
    }

    public ContactDao(long currentUserId) {
        this.helper = new DaoHelper<>();
        this.currentUserId = currentUserId;
    }

    @Override
    public void createTableIfNotExists() {
        try (Connection conn = ConnectionDB.getInstance().getConection();
             Statement st = conn.createStatement()) {

            // Verificar si la tabla existe
            boolean tablaExiste = false;
            try (ResultSet rs = conn.getMetaData().getTables(null, null, "contact", null)) {
                if (rs.next()) {
                    tablaExiste = true;
                }
            }

            if (!tablaExiste) {
                // Crear tabla nueva
                st.execute("CREATE TABLE contact ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "code TEXT NOT NULL UNIQUE, "
                        + "name TEXT NOT NULL, "
                        + "ip TEXT NOT NULL, "
                        + "user_id INTEGER NOT NULL DEFAULT 0"
                        + ")");
            } else {
                // Verificar si tiene user_id
                boolean hasUserId = false;
                try (ResultSet rs = conn.getMetaData().getColumns(null, null, "contact", "user_id")) {
                    if (rs.next()) hasUserId = true;
                }
                if (!hasUserId) {
                    log.info("Agregando columna user_id a tabla contact...");
                    st.execute("ALTER TABLE contact ADD COLUMN user_id INTEGER NOT NULL DEFAULT 0");
                }
            }

            // Crear indices si no existen
            st.execute("CREATE INDEX IF NOT EXISTS idx_contact_code ON contact(code)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_contact_ip ON contact(ip)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_contact_user_id ON contact(user_id)");

        } catch (SQLException e) {
            log.error("Error al crear/migrar tabla contact: {}", e.getMessage());
        }
    }

    // --- ResultReader ---

    DaoHelper.ResultReader<Contact> resultReader = result -> {
        Contact contact = new Contact();
        if (IContactDao.existColumn(result, Contact.Column.ID)) {
            contact.setId(result.getLong(Contact.Column.ID));
        }
        if (IContactDao.existColumn(result, Contact.Column.CODE)) {
            contact.setCode(result.getString(Contact.Column.CODE));
        }
        if (IContactDao.existColumn(result, Contact.Column.NAME)) {
            contact.setName(result.getString(Contact.Column.NAME));
        }
        if (IContactDao.existColumn(result, Contact.Column.IP)) {
            contact.setIp(result.getString(Contact.Column.IP));
        }
        if (IContactDao.existColumn(result, Contact.Column.USER_ID)) {
            contact.setUserId(result.getLong(Contact.Column.USER_ID));
        }
        return contact;
    };

    /**
     * Deprecated, use IContactDao.existColumn instead.
     */
    public static boolean existColumn(ResultSet result, String columnName) {
        return IContactDao.existColumn(result, columnName);
    }

    // --- CRUD ---

    @Override
    public void save(Contact contact) throws Exception {
        contact.setUserId(currentUserId);
        String query = "INSERT INTO contact(code, name, ip, user_id) VALUES (?, ?, ?, ?)";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, contact.getCode());
            pst.setString(2, contact.getName());
            pst.setString(3, contact.getIp());
            pst.setLong(4, contact.getUserId());
        };
        helper.insert(query, params, contact);
    }

    @Override
    public void update(Contact contact) throws Exception {
        String query = "UPDATE contact SET ip = ?, name = ? WHERE code = ? AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, contact.getIp());
            pst.setString(2, contact.getName());
            pst.setString(3, contact.getCode());
            pst.setLong(4, currentUserId);
        };
        helper.update(query, params);
    }

    @Override
    public void delete(long id) throws ConnectException, SQLException {
        String query = "DELETE FROM contact WHERE id = ? AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setLong(1, id);
            pst.setLong(2, currentUserId);
        };
        helper.update(query, params);
    }

    // --- Queries ---

    @Override
    public List<Contact> findAll() throws ConnectException, SQLException {
        String query = "SELECT * FROM contact WHERE user_id = ? ORDER BY name ASC";
        DaoHelper.QueryParameters params = pst -> pst.setLong(1, currentUserId);
        return helper.executeQuery(query, params, resultReader);
    }

    @Override
    public Contact findByCode(String code) throws ConnectException, SQLException {
        String query = "SELECT * FROM contact WHERE code = ? AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, code);
            pst.setLong(2, currentUserId);
        };
        List<Contact> list = helper.executeQuery(query, params, resultReader);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Contact findByIp(String ip) throws ConnectException, SQLException {
        String query = "SELECT * FROM contact WHERE ip = ? AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, ip);
            pst.setLong(2, currentUserId);
        };
        List<Contact> list = helper.executeQuery(query, params, resultReader);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public boolean existByCode(String code) throws ConnectException, SQLException {
        String query = "SELECT count(*) FROM contact WHERE code = ? AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, code);
            pst.setLong(2, currentUserId);
        };
        return helper.executeQueryCount(query, params) >= 1;
    }
}
