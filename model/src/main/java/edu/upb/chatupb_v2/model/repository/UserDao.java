package edu.upb.chatupb_v2.model.repository;

import edu.upb.chatupb_v2.model.entities.User;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.List;
import java.util.UUID;

@Slf4j
public class UserDao {

    private DaoHelper<User> helper;

    public UserDao() {
        helper = new DaoHelper<>();
    }

    public void createTableIfNotExists() {
        try (Connection conn = ConnectionDB.getInstance().getConection();
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS user ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "code TEXT DEFAULT NULL, "
                    + "name TEXT DEFAULT NULL"
                    + ")");
        } catch (SQLException e) {
            log.error("Error al crear tabla user: {}", e.getMessage());
        }
    }

    DaoHelper.ResultReader<User> resultReader = result -> {
        User user = new User();
        if (ContactDao.existColumn(result, User.Column.ID)) {
            user.setId(result.getString(User.Column.ID));
        }
        if (ContactDao.existColumn(result, User.Column.CODE)) {
            user.setCode(result.getString(User.Column.CODE));
        }
        if (ContactDao.existColumn(result, User.Column.NAME)) {
            user.setName(result.getString(User.Column.NAME));
        }
        return user;
    };

    public void save(User user) throws Exception {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        String query = "INSERT INTO user(id, code, name) VALUES (?, ?, ?)";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, user.getId());
            pst.setString(2, user.getCode());
            pst.setString(3, user.getName());
        };
        helper.insert(query, params);
    }

    public List<User> findAll() throws Exception {
        String query = "SELECT * FROM user";
        return helper.executeQuery(query, resultReader);
    }
}
