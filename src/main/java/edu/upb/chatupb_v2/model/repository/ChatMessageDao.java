package edu.upb.chatupb_v2.model.repository;

import edu.upb.chatupb_v2.model.entities.ChatMessage;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.sql.*;
import java.util.List;
import java.util.UUID;

/**
 * DAO para la tabla 'message'.
 */
@Slf4j
public class ChatMessageDao {

    private DaoHelper<ChatMessage> helper;
    private String currentUserId;

    public ChatMessageDao() {
        this.helper = new DaoHelper<>();
        this.currentUserId = null;
    }

    public ChatMessageDao(String currentUserId) {
        this.helper = new DaoHelper<>();
        this.currentUserId = currentUserId;
    }

    public void createTableIfNotExists() {
        try (Connection conn = ConnectionDB.getInstance().getConection();
             Statement st = conn.createStatement()) {

            st.execute("CREATE TABLE IF NOT EXISTS message ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "sender_code TEXT DEFAULT NULL, "
                    + "receiver_code TEXT DEFAULT NULL, "
                    + "content TEXT DEFAULT NULL, "
                    + "timestamp TEXT DEFAULT NULL, "
                    + "confirmed INTEGER DEFAULT NULL, "
                    + "user_id VARCHAR(36) DEFAULT NULL"
                    + ")");

            // Agregar columna pinned si no existe (migracion para DBs existentes)
            try {
                st.execute("ALTER TABLE message ADD COLUMN pinned INTEGER DEFAULT 0");
            } catch (SQLException ignored) {
                // La columna ya existe, ignorar
            }

            try {
                st.execute("ALTER TABLE message ADD COLUMN view_once INTEGER DEFAULT 0");
            } catch (SQLException ignored) {}

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
            msg.setId(result.getString(ChatMessage.Column.ID));
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
            msg.setTimestamp(result.getString(ChatMessage.Column.TIMESTAMP));
        }
        if (ContactDao.existColumn(result, ChatMessage.Column.CONFIRMED)) {
            int val = result.getInt(ChatMessage.Column.CONFIRMED);
            msg.setConfirmed(!result.wasNull() && val == 1);
        }
        if (ContactDao.existColumn(result, ChatMessage.Column.PINNED)) {
            int pinVal = result.getInt(ChatMessage.Column.PINNED);
            msg.setPinned(!result.wasNull() && pinVal == 1);
        }
        if (ContactDao.existColumn(result, ChatMessage.Column.USER_ID)) {
            msg.setUserId(result.getString(ChatMessage.Column.USER_ID));
        }
        if (ContactDao.existColumn(result, ChatMessage.Column.VIEW_ONCE)) {
            int voVal = result.getInt(ChatMessage.Column.VIEW_ONCE);
            msg.setViewOnce(!result.wasNull() && voVal == 1);
        }
        return msg;
    };

    // --- CRUD ---

    public void save(ChatMessage message) throws Exception {
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().toString());
        }
        message.setUserId(currentUserId);
        String query = "INSERT OR IGNORE INTO message(id, sender_code, receiver_code, content, timestamp, confirmed, pinned, view_once, user_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, message.getId());
            pst.setString(2, message.getSenderCode());
            pst.setString(3, message.getReceiverCode());
            pst.setString(4, message.getContent());
            pst.setString(5, message.getTimestamp());
            pst.setInt(6, message.isConfirmed() ? 1 : 0);
            pst.setInt(7, message.isPinned() ? 1 : 0);
            pst.setInt(8, message.isViewOnce() ? 1 : 0);
            pst.setString(9, message.getUserId());
        };
        helper.insert(query, params);
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
            pst.setString(5, currentUserId);
        };
        return helper.executeQuery(query, params, resultReader);
    }

    public void markConfirmed(String idMensaje) throws ConnectException, SQLException {
        String query = "UPDATE message SET confirmed = 1 WHERE id = ? AND confirmed = 0 AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, idMensaje);
            pst.setString(2, currentUserId);
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
            pst.setString(3, currentUserId);
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
            pst.setString(3, currentUserId);
        };
        helper.update(query, params);
    }

    /**
     * Pone el contenido del mensaje a null (eliminacion logica).
     * Se usa cuando se recibe la trama 009.
     */
    public void setContentNull(String idMensaje) throws ConnectException, SQLException {
        String query = "UPDATE message SET content = '' WHERE id = ? AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, idMensaje);
            pst.setString(2, currentUserId);
        };
        helper.update(query, params);
    }

    /**
     * Fija un mensaje en la conversacion. Primero desfija todos los mensajes
     * de esa conversacion y luego fija el mensaje indicado.
     * Solo puede haber un mensaje fijado por conversacion.
     */
    public void pinMessage(String idMensaje, String myCode, String contactCode)
            throws ConnectException, SQLException {
        // Primero desfijar todos los mensajes de la conversacion
        String unpinAll = "UPDATE message SET pinned = 0 WHERE "
                + "((sender_code = ? AND receiver_code = ?) OR "
                + "(sender_code = ? AND receiver_code = ?)) "
                + "AND user_id = ? AND pinned = 1";
        DaoHelper.QueryParameters unpinParams = pst -> {
            pst.setString(1, myCode);
            pst.setString(2, contactCode);
            pst.setString(3, contactCode);
            pst.setString(4, myCode);
            pst.setString(5, currentUserId);
        };
        helper.update(unpinAll, unpinParams);

        // Fijar el mensaje seleccionado
        String pin = "UPDATE message SET pinned = 1 WHERE id = ? AND user_id = ?";
        DaoHelper.QueryParameters pinParams = pst -> {
            pst.setString(1, idMensaje);
            pst.setString(2, currentUserId);
        };
        helper.update(pin, pinParams);
    }

    /**
     * Desfija un mensaje.
     */
    public void unpinMessage(String idMensaje) throws ConnectException, SQLException {
        String query = "UPDATE message SET pinned = 0 WHERE id = ? AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, idMensaje);
            pst.setString(2, currentUserId);
        };
        helper.update(query, params);
    }

    /**
     * Busca el mensaje fijado de una conversacion.
     * Retorna null si no hay mensaje fijado.
     */
    public ChatMessage findPinnedMessage(String myCode, String contactCode)
            throws ConnectException, SQLException {
        String query = "SELECT * FROM message WHERE "
                + "((sender_code = ? AND receiver_code = ?) OR "
                + "(sender_code = ? AND receiver_code = ?)) "
                + "AND user_id = ? AND pinned = 1 LIMIT 1";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, myCode);
            pst.setString(2, contactCode);
            pst.setString(3, contactCode);
            pst.setString(4, myCode);
            pst.setString(5, currentUserId);
        };
        List<ChatMessage> results = helper.executeQuery(query, params, resultReader);
        return results.isEmpty() ? null : results.get(0);
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
            pst.setString(5, currentUserId);
        };
        helper.update(query, params);
    }

    public ChatMessage findById(String idMensaje) throws ConnectException, SQLException {
        String query = "SELECT * FROM message WHERE id = ? AND user_id = ?";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, idMensaje);
            pst.setString(2, currentUserId);
        };
        List<ChatMessage> list = helper.executeQuery(query, params, resultReader);
        return list.isEmpty() ? null : list.get(0);
    }
}
