package edu.upb.chatupb_v2.model.repository;

import edu.upb.chatupb_v2.model.entities.ChatMessage;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.sql.*;
import java.util.List;

/**
 * DAO para la tabla 'message'.
 */
@Slf4j
public class ChatMessageDao {

    private DaoHelper<ChatMessage> helper;
    private long currentUserId;

    public ChatMessageDao() {
        this.helper = new DaoHelper<>();
        this.currentUserId = 0;
    }

    public ChatMessageDao(long currentUserId) {
        this.helper = new DaoHelper<>();
        this.currentUserId = currentUserId;
    }

    public void createTableIfNotExists() {
        try (Connection conn = ConnectionDB.getInstance().getConection();
             Statement st = conn.createStatement()) {

            // Verificar si la tabla existe
            boolean tablaExiste = false;
            try (ResultSet rs = conn.getMetaData().getTables(null, null, "message", null)) {
                if (rs.next()) {
                    tablaExiste = true;
                }
            }

            if (!tablaExiste) {
                // Crear tabla con schema correcta
                st.execute("CREATE TABLE message ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "sender_code TEXT NOT NULL, "
                        + "receiver_code TEXT NOT NULL, "
                        + "content TEXT, "
                        + "timestamp INTEGER NOT NULL, "
                        + "confirmed INTEGER NOT NULL DEFAULT 0, "
                        + "user_id INTEGER NOT NULL DEFAULT 0"
                        + ")");
            } else {
                // Verificar si tiene user_id
                boolean hasUserId = false;
                try (ResultSet rs = conn.getMetaData().getColumns(null, null, "message", "user_id")) {
                    if (rs.next()) hasUserId = true;
                }
                if (!hasUserId) {
                    log.info("Agregando columna user_id a tabla message...");
                    st.execute("ALTER TABLE message ADD COLUMN user_id INTEGER NOT NULL DEFAULT 0");
                }

                // Migrar content de NOT NULL a nullable (SQLite requiere recrear la tabla)
                boolean contentIsNotNull = false;
                try (ResultSet rs = st.executeQuery("PRAGMA table_info(message)")) {
                    while (rs.next()) {
                        if ("content".equals(rs.getString("name")) && rs.getInt("notnull") == 1) {
                            contentIsNotNull = true;
                            break;
                        }
                    }
                }
                if (contentIsNotNull) {
                    log.info("Migrando columna content a nullable...");
                    st.execute("CREATE TABLE message_tmp ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "sender_code TEXT NOT NULL, "
                            + "receiver_code TEXT NOT NULL, "
                            + "content TEXT, "
                            + "timestamp INTEGER NOT NULL, "
                            + "confirmed INTEGER NOT NULL DEFAULT 0, "
                            + "user_id INTEGER NOT NULL DEFAULT 0"
                            + ")");
                    st.execute("INSERT INTO message_tmp SELECT * FROM message");
                    st.execute("DROP TABLE message");
                    st.execute("ALTER TABLE message_tmp RENAME TO message");
                }
            }

            // Crear indices si no existen
            st.execute("CREATE INDEX IF NOT EXISTS idx_message_conversation "
                    + "ON message(sender_code, receiver_code)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_message_timestamp "
                    + "ON message(timestamp)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_message_user_id "
                    + "ON message(user_id)");

        } catch (SQLException e) {
            log.error("Error al crear/migrar tabla message: {}", e.getMessage());
        }
    }

    // --- ResultReader ---

    DaoHelper.ResultReader<ChatMessage> resultReader = result -> {
        ChatMessage msg = new ChatMessage();
        if (ContactDao.existColumn(result, ChatMessage.Column.ID)) {
            msg.setId(result.getLong(ChatMessage.Column.ID));
        }
        if (ContactDao.existColumn(result, ChatMessage.Column.SENDER_CODE)) {
            msg.setSenderCode(result.getString(ChatMessage.Column.SENDER_CODE));
        }
        if (ContactDao.existColumn(result, ChatMessage.Column.RECEIVER_CODE)) {
            msg.setReceiverCode(result.getString(ChatMessage.Column.RECEIVER_CODE));
        }
        if (ContactDao.existColumn(result, ChatMessage.Column.CONTENT)) {
            msg.setContent(result.getString(ChatMessage.Column.CONTENT));
        }
        if (ContactDao.existColumn(result, ChatMessage.Column.TIMESTAMP)) {
            msg.setTimestamp(result.getLong(ChatMessage.Column.TIMESTAMP));
        }
        if (ContactDao.existColumn(result, ChatMessage.Column.CONFIRMED)) {
            msg.setConfirmed(result.getInt(ChatMessage.Column.CONFIRMED) == 1);
        }
        if (ContactDao.existColumn(result, ChatMessage.Column.USER_ID)) {
            msg.setUserId(result.getLong(ChatMessage.Column.USER_ID));
        }
        return msg;
    };

    // --- CRUD ---

    public void save(ChatMessage message) throws Exception {
        String query = "INSERT INTO message(sender_code, receiver_code, content, timestamp, confirmed, user_id) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, message.getSenderCode());
            pst.setString(2, message.getReceiverCode());
            pst.setString(3, message.getContent());
            pst.setLong(4, message.getTimestamp());
            pst.setInt(5, message.isConfirmed() ? 1 : 0);
            pst.setLong(6, currentUserId);
        };
        helper.insert(query, params, message);
    }

    // --- Queries ---

    public List<ChatMessage> findConversation(String myCode, String contactCode)
            throws ConnectException, SQLException {
        String query = "SELECT * FROM message WHERE "
                + "((sender_code = ? AND receiver_code = ?) OR "
                + "(sender_code = ? AND receiver_code = ?)) "
                + "AND user_id = ? "
                + "ORDER BY timestamp ASC";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, myCode);
            pst.setString(2, contactCode);
            pst.setString(3, contactCode);
            pst.setString(4, myCode);
            pst.setLong(5, currentUserId);
        };
        return helper.executeQuery(query, params, resultReader);
    }

    public void markConfirmed(String idMensaje) throws ConnectException, SQLException {
        String query = "UPDATE message SET confirmed = 1 WHERE timestamp = ? AND confirmed = 0 AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setLong(1, Long.parseLong(idMensaje));
            pst.setLong(2, currentUserId);
        };
        helper.update(query, params);
    }

    /**
     * Busca mensajes recibidos que aun no fueron confirmados (no se envio 008).
     */
    public List<ChatMessage> findUnconfirmedReceived(String myCode, String contactCode)
            throws ConnectException, SQLException {
        String query = "SELECT * FROM message WHERE "
                + "sender_code = ? AND receiver_code = ? "
                + "AND confirmed = 0 AND user_id = ? "
                + "ORDER BY timestamp ASC";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, contactCode);
            pst.setString(2, myCode);
            pst.setLong(3, currentUserId);
        };
        return helper.executeQuery(query, params, resultReader);
    }

    /**
     * Marca todos los mensajes recibidos de un contacto como confirmados (ya se envio 008).
     */
    public void markReceivedAsConfirmed(String myCode, String contactCode)
            throws ConnectException, SQLException {
        String query = "UPDATE message SET confirmed = 1 WHERE "
                + "sender_code = ? AND receiver_code = ? "
                + "AND confirmed = 0 AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, contactCode);
            pst.setString(2, myCode);
            pst.setLong(3, currentUserId);
        };
        helper.update(query, params);
    }

    /**
     * Pone el contenido del mensaje a null (eliminacion logica).
     * Se usa cuando se recibe la trama 009.
     */
    public void setContentNull(String idMensaje) throws ConnectException, SQLException {
        String query = "UPDATE message SET content = NULL WHERE timestamp = ? AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setLong(1, Long.parseLong(idMensaje));
            pst.setLong(2, currentUserId);
        };
        helper.update(query, params);
    }

    public void deleteConversation(String myCode, String contactCode)
            throws ConnectException, SQLException {
        String query = "DELETE FROM message WHERE "
                + "((sender_code = ? AND receiver_code = ?) OR "
                + "(sender_code = ? AND receiver_code = ?)) "
                + "AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, myCode);
            pst.setString(2, contactCode);
            pst.setString(3, contactCode);
            pst.setString(4, myCode);
            pst.setLong(5, currentUserId);
        };
        helper.update(query, params);
    }
}
