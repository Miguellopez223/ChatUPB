package edu.upb.chatupb_v2;

import edu.upb.chatupb_v2.model.network.ChatServer;
import edu.upb.chatupb_v2.model.network.Mediador;
import edu.upb.chatupb_v2.model.repository.ChatMessageDao;
import edu.upb.chatupb_v2.model.repository.ContactDao;
import edu.upb.chatupb_v2.view.ChatUI;

public class ChatUPB_V2 {
    public static void main(String[] args) {

        // ============================================================
        // 1. INICIALIZACION DE BASE DE DATOS
        //    - Crear/migrar tablas con schema actualizado
        //    - contact: id, code (UNIQUE), name, ip + indices
        //    - message: id, sender_code, receiver_code, content,
        //               timestamp, confirmed + indices
        // ============================================================
        System.out.println("[DB] Inicializando base de datos...");
        new ContactDao().createTableIfNotExists();
        new ChatMessageDao().createTableIfNotExists();
        System.out.println("[DB] Base de datos lista.");

        // ============================================================
        // 2. INICIALIZACION DE UI
        // ============================================================
        ChatUI chatUI = new ChatUI();

        // El controller se suscribe como listener del Mediador.
        // El Mediador NO baja al controller; el controller sube al Mediador.
        Mediador.getInstancia().addChatEventListener(chatUI.getChatController());

        java.awt.EventQueue.invokeLater(() -> chatUI.setVisible(true));

        // ============================================================
        // 3. INICIALIZACION DE RED
        // ============================================================
        try {
            ChatServer chatServer = new ChatServer();
            // El Mediador es quien escucha los eventos del socket (ChatEventListener)
            chatServer.addChatEventListener(Mediador.getInstancia());
            chatServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ============================================================
        // 4. DETECCION DE PRESENCIA
        //    Enviar Hello (004) a todos los contactos guardados
        //    para notificar que estamos en linea
        // ============================================================
        chatUI.getChatController().iniciarHello();
    }
}
