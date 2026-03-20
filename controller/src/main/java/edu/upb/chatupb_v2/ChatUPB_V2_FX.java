package edu.upb.chatupb_v2;

import edu.upb.chatupb_v2.controller.ChatController;
import edu.upb.chatupb_v2.model.network.ChatServer;
import edu.upb.chatupb_v2.model.network.Mediador;
import edu.upb.chatupb_v2.model.repository.ChatMessageDao;
import edu.upb.chatupb_v2.model.repository.ContactDao;
import edu.upb.chatupb_v2.model.repository.UserDao;
import edu.upb.chatupb_v2.view.ChatUIFX;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Punto de entrada para la version JavaFX del chat.
 * Ejecutar con: mvn javafx:run
 * O configurar esta clase como Main Class en el IDE.
 */
public class ChatUPB_V2_FX extends Application {

    private ChatUIFX chatUI;
    private ChatController chatController;

    @Override
    public void init() {
        // 1. INICIALIZACION DE BASE DE DATOS
        System.out.println("[DB] Inicializando base de datos...");
        new UserDao().createTableIfNotExists();
        new ContactDao().createTableIfNotExists();
        new ChatMessageDao().createTableIfNotExists();
        System.out.println("[DB] Base de datos lista.");

        // Creamos la Vista y el Controlador
        chatUI = new ChatUIFX();
        chatUI.init(); // Tu método init interno de JavaFX

        // Inyectamos las dependencias
        chatController = new ChatController(chatUI);
        chatUI.setChatController(chatController);
    }

    @Override
    public void start(Stage primaryStage) {
        // 3. Registrar listener
        Mediador.getInstancia().addChatEventListener(chatController);
        //Mediador.getInstancia().addChatEventListener(chatUI.getChatController());

        // 4. Construir la UI y mostrar
        chatUI.start(primaryStage);

        chatController.onAppStart();

        // 5. INICIALIZACION DE RED (en un hilo aparte)
        new Thread(() -> {
            try {
                ChatServer chatServer = new ChatServer();
                chatServer.addChatEventListener(Mediador.getInstancia());
                chatServer.start();
                System.out.println("[Server] Servidor de chat iniciado.");
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> chatUI.mostrarError("Error al iniciar el servidor de red."));
            }
        }, "ChatServer-Init").start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
