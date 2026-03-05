package edu.upb.chatupb_v2.model.repository;

import edu.upb.chatupb_v2.model.entities.ChatMessage;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.sql.*;
import java.util.List;

/**
 * DAO para la tabla 'message'.
 *
 * Schema:
 *   CREATE TABLE message (
 *       id            INTEGER PRIMARY KEY AUTOINCREMENT,
 *       sender_code   TEXT NOT NULL,
 *       receiver_code TEXT NOT NULL,
 *       content       TEXT NOT NULL,
 *       timestamp     INTEGER NOT NULL,
 *       confirmed     INTEGER NOT NULL DEFAULT 0
 *   );
 *   CREATE INDEX IF NOT EXISTS idx_message_conversation ON message(sender_code, receiver_code);
 *   CREATE INDEX IF NOT EXISTS idx_message_timestamp    ON message(timestamp);
 *
 * Notas:
 * - sender_code y receiver_code contienen UUIDs (Contact.ME_CODE o contact.code)
 * - timestamp almacena epoch millis (System.currentTimeMillis())
 * - confirmed: 0 = pendiente, 1 = confirmado por el receptor (trama 008)
 * - No se usan FOREIGN KEYs porque ME_CODE no esta en la tabla contact
 */
@Slf4j
public class ChatMessageDao {

    private DaoHelper<ChatMessage> helper;

    public ChatMessageDao() {
        helper = new DaoHelper<>();
    }

    /**
     * Crea la tabla 'message' con el schema correcto.
     * Si existe una tabla 'message' antigua con columnas incompatibles
     * (ej: cod_message, recipient_code, room_code), la elimina y la recrea.
     */
    public void createTableIfNotExists() {
        try (Connection conn = ConnectionDB.getInstance().getConection();
             Statement st = conn.createStatement()) {

            // Verificar si la tabla existe
            boolean tablaExiste = false;
            boolean schemaCorrecta = false;

            try (ResultSet rs = conn.getMetaData().getTables(null, null, "message", null)) {
                if (rs.next()) {
                    tablaExiste = true;
                }
            }

            if (tablaExiste) {
                // Verificar si tiene las columnas correctas
                try (ResultSet rs = conn.getMetaData().getColumns(null, null, "message", "receiver_code")) {
                    if (rs.next()) {
                        schemaCorrecta = true; // tiene receiver_code -> es el nuevo schema
                    }
                }

                if (!schemaCorrecta) {
                    // Schema vieja (tiene cod_message, recipient_code, etc.) -> eliminar y recrear
                    log.info("Tabla 'message' tiene schema antigua incompatible. Eliminando y recreando...");
                    st.execute("DROP TABLE message");
                    log.info("Tabla 'message' antigua eliminada.");
                }
            }

            if (!tablaExiste || !schemaCorrecta) {
                // Crear tabla con schema correcta
                st.execute("CREATE TABLE message ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "sender_code TEXT NOT NULL, "
                        + "receiver_code TEXT NOT NULL, "
                        + "content TEXT NOT NULL, "
                        + "timestamp INTEGER NOT NULL, "
                        + "confirmed INTEGER NOT NULL DEFAULT 0"
                        + ")");
                log.info("Tabla 'message' creada con schema correcta.");
            }

            // Crear indices si no existen (para consultas eficientes por conversacion)
            st.execute("CREATE INDEX IF NOT EXISTS idx_message_conversation "
                    + "ON message(sender_code, receiver_code)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_message_timestamp "
                    + "ON message(timestamp)");

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
        return msg;
    };

    // --- CRUD ---

    /**
     * Inserta un nuevo mensaje en la base de datos.
     */
    public void save(ChatMessage message) throws Exception {
        String query = "INSERT INTO message(sender_code, receiver_code, content, timestamp, confirmed) "
                + "VALUES (?, ?, ?, ?, ?)";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, message.getSenderCode());
            pst.setString(2, message.getReceiverCode());
            pst.setString(3, message.getContent());
            pst.setLong(4, message.getTimestamp());
            pst.setInt(5, message.isConfirmed() ? 1 : 0);
        };
        helper.insert(query, params, message);
    }

    // --- Queries ---

    /**
     * Obtiene el historial de conversacion entre el usuario local y un contacto.
     * Retorna todos los mensajes donde (sender=yo AND receiver=contacto) OR viceversa,
     * ordenados por timestamp ascendente.
     *
     * @param myCode      UUID del usuario local (Contact.ME_CODE)
     * @param contactCode UUID del contacto
     * @return lista de mensajes ordenados cronologicamente
     */
    public List<ChatMessage> findConversation(String myCode, String contactCode)
            throws ConnectException, SQLException {
        String query = "SELECT * FROM message WHERE "
                + "(sender_code = ? AND receiver_code = ?) OR "
                + "(sender_code = ? AND receiver_code = ?) "
                + "ORDER BY timestamp ASC";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, myCode);
            pst.setString(2, contactCode);
            pst.setString(3, contactCode);
            pst.setString(4, myCode);
        };
        return helper.executeQuery(query, params, resultReader);
    }

    /**
     * Marca un mensaje como confirmado (el receptor envio trama 008).
     * Usa el timestamp como identificador del mensaje.
     *
     * @param idMensaje String con el timestamp en millis del mensaje
     */
    public void markConfirmed(String idMensaje) throws ConnectException, SQLException {
        String query = "UPDATE message SET confirmed = 1 WHERE timestamp = ? AND confirmed = 0";
        DaoHelper.QueryParameters params = pst -> {
            pst.setLong(1, Long.parseLong(idMensaje));
        };
        helper.update(query, params);
    }

    /**
     * Elimina todos los mensajes de una conversacion con un contacto.
     * Se usa cuando se elimina un contacto.
     *
     * @param contactCode UUID del contacto cuyos mensajes se eliminan
     * @param myCode      UUID del usuario local
     */
    public void deleteConversation(String myCode, String contactCode)
            throws ConnectException, SQLException {
        String query = "DELETE FROM message WHERE "
                + "(sender_code = ? AND receiver_code = ?) OR "
                + "(sender_code = ? AND receiver_code = ?)";
        DaoHelper.QueryParameters params = pst -> {
            pst.setString(1, myCode);
            pst.setString(2, contactCode);
            pst.setString(3, contactCode);
            pst.setString(4, myCode);
        };
        helper.update(query, params);
    }
}
