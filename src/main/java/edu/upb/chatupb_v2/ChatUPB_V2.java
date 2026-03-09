package edu.upb.chatupb_v2;

import edu.upb.chatupb_v2.model.network.ChatServer;
import edu.upb.chatupb_v2.model.network.Mediador;
import edu.upb.chatupb_v2.model.repository.ChatMessageDao;
import edu.upb.chatupb_v2.model.repository.ContactDao;
import edu.upb.chatupb_v2.model.repository.UserDao;
import edu.upb.chatupb_v2.view.ChatUI;

public class ChatUPB_V2 {
    public static void main(String[] args) {

        // ============================================================
        // 1. INICIALIZACION DE BASE DE DATOS
        // ============================================================
        System.out.println("[DB] Inicializando base de datos...");
        new UserDao().createTableIfNotExists();
        new ContactDao().createTableIfNotExists();
        new ChatMessageDao().createTableIfNotExists();
        System.out.println("[DB] Base de datos lista.");

        // ============================================================
        // 2. INICIALIZACION DE UI Y CONTROLLER
        // ============================================================
        ChatUI chatUI = new ChatUI();

        // El controller se suscribe como listener del Mediador.
        // El Mediador NO baja al controller; el controller sube al Mediador.
        Mediador.getInstancia().addChatEventListener(chatUI.getChatController());

        // Iniciar la logica de la aplicacion en el hilo de Swing
        java.awt.EventQueue.invokeLater(() -> {
            chatUI.setVisible(true);
            chatUI.getChatController().onAppStart();
        });

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
    }
}
