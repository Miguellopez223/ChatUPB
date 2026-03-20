package edu.upb.chatupb_v2;

import edu.upb.chatupb_v2.controller.ChatController;
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
        // 2 y 3. INICIALIZACIÓN UI, CONTROLLER Y RED
        // ============================================================
        // 1. Creamos la vista (no sabe nada del controlador aún)
        ChatUI chatUI = new ChatUI();

        // 2. Creamos el controlador pasándole la vista
        ChatController chatController = new ChatController(chatUI);

        // 3. ENLAZAMOS: Le inyectamos el controlador a la vista mediante el setter
        chatUI.setChatController(chatController);

        // 4. Registramos el controlador en el mediador
        Mediador.getInstancia().addChatEventListener(chatController);

        java.awt.EventQueue.invokeLater(() -> {
            chatUI.setVisible(true);
            chatUI.getChatController().onAppStart();
        });

        // Inicializamos el servidor de red
        try {
            ChatServer chatServer = new ChatServer();
            chatServer.addChatEventListener(Mediador.getInstancia());
            chatServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
