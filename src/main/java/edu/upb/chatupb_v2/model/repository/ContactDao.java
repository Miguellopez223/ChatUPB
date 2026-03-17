package edu.upb.chatupb_v2.model.repository;

import edu.upb.chatupb_v2.model.entities.Contact;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.sql.*;
import java.util.List;
import java.util.UUID;

/**
 * DAO para la tabla 'contact'.
 */
@Slf4j
public class ContactDao implements IContactDao {

    private DaoHelper<Contact> helper;
    private String currentUserId;

    public ContactDao() {
        this.helper = new DaoHelper<>();
        this.currentUserId = null;
    }

    public ContactDao(String currentUserId) {
        this.helper = new DaoHelper<>();
        this.currentUserId = currentUserId;
    }

    @Override
    public void createTableIfNotExists() {
        try (Connection conn = ConnectionDB.getInstance().getConection();
             Statement st = conn.createStatement()) {

            st.execute("CREATE TABLE IF NOT EXISTS contact ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "code TEXT DEFAULT NULL, "
                    + "name TEXT DEFAULT NULL, "
                    + "ip TEXT DEFAULT NULL, "
                    + "user_id VARCHAR(36) DEFAULT NULL"
                    + ")");

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
            contact.setId(result.getString(Contact.Column.ID));
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
            contact.setUserId(result.getString(Contact.Column.USER_ID));
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
        if (contact.getId() == null) {
            contact.setId(UUID.randomUUID().toString());
        }
        contact.setUserId(currentUserId);
        String query = "INSERT INTO contact(id, code, name, ip, user_id) VALUES (?, ?, ?, ?, ?)";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, contact.getId());
            pst.setString(2, contact.getCode());
            pst.setString(3, contact.getName());
            pst.setString(4, contact.getIp());
            pst.setString(5, contact.getUserId());
        };
        helper.insert(query, params);
    }

    @Override
    public void update(Contact contact) throws Exception {
        String query = "UPDATE contact SET ip = ?, name = ? WHERE code = ? AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, contact.getIp());
            pst.setString(2, contact.getName());
            pst.setString(3, contact.getCode());
            pst.setString(4, currentUserId);
        };
        helper.update(query, params);
    }

    @Override
    public void delete(String id) throws ConnectException, SQLException {
        String query = "DELETE FROM contact WHERE id = ? AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, id);
            pst.setString(2, currentUserId);
        };
        helper.update(query, params);
    }

    // --- Queries ---

    @Override
    public List<Contact> findAll() throws ConnectException, SQLException {
        String query = "SELECT * FROM contact WHERE user_id = ? ORDER BY name ASC";
        DaoHelper.QueryParameters params = pst -> pst.setString(1, currentUserId);
        return helper.executeQuery(query, params, resultReader);
    }

    @Override
    public Contact findByCode(String code) throws ConnectException, SQLException {
        String query = "SELECT * FROM contact WHERE code = ? AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, code);
            pst.setString(2, currentUserId);
        };
        List<Contact> list = helper.executeQuery(query, params, resultReader);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Contact findByIp(String ip) throws ConnectException, SQLException {
        String query = "SELECT * FROM contact WHERE ip = ? AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, ip);
            pst.setString(2, currentUserId);
        };
        List<Contact> list = helper.executeQuery(query, params, resultReader);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public boolean existByCode(String code) throws ConnectException, SQLException {
        String query = "SELECT count(*) FROM contact WHERE code = ? AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, code);
            pst.setString(2, currentUserId);
        };
        return helper.executeQueryCount(query, params) >= 1;
    }
}
