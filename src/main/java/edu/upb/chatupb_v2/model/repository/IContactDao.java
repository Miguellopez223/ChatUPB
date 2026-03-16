package edu.upb.chatupb_v2.model.repository;

import edu.upb.chatupb_v2.model.entities.Contact;

import java.net.ConnectException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Interfaz para el DAO de Contactos
 */
public interface IContactDao {
    void createTableIfNotExists();
    void save(Contact contact) throws Exception;
    void update(Contact contact) throws Exception;
    void delete(long id) throws ConnectException, SQLException;
    List<Contact> findAll() throws ConnectException, SQLException;
    Contact findByCode(String code) throws ConnectException, SQLException;
    Contact findByIp(String ip) throws ConnectException, SQLException;
    boolean existByCode(String code) throws ConnectException, SQLException;
    
    // Default method para existColumn ya que se usa estáticamente en otros DAOs
    static boolean existColumn(ResultSet result, String columnName) {
        try {
            result.findColumn(columnName);
            return true;
        } catch (SQLException sqlex) {
            return false;
        }
    }
}
