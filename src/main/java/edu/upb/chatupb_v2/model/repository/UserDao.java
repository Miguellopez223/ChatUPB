package edu.upb.chatupb_v2.model.repository;

import edu.upb.chatupb_v2.model.entities.User;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.List;

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
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "code TEXT NOT NULL UNIQUE, "
                    + "name TEXT NOT NULL"
                    + ")");
        } catch (SQLException e) {
            log.error("Error al crear tabla user: {}", e.getMessage());
        }
    }

    DaoHelper.ResultReader<User> resultReader = result -> {
        User user = new User();
        if (ContactDao.existColumn(result, User.Column.ID)) {
            user.setId(result.getLong(User.Column.ID));
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
        String query = "INSERT INTO user(code, name) VALUES (?, ?)";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, user.getCode());
            pst.setString(2, user.getName());
        };
        helper.insert(query, params, user);
    }

    public List<User> findAll() throws Exception {
        String query = "SELECT * FROM user";
        return helper.executeQuery(query, resultReader);
    }
}
