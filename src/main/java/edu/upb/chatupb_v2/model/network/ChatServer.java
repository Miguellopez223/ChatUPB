package edu.upb.chatupb_v2.model.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer extends Thread {

    private static final int port = 1900;
    private final ServerSocket server;
    private final List<ChatEventListener> listeners = new ArrayList<>();

    public ChatServer() throws IOException {
        this.server = new ServerSocket(port);
    }

    public void addChatEventListener(ChatEventListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void run() {
        while (true) {
            try {
                SocketClient socketClient = new SocketClient(this.server.accept());
                // Le pasamos todos los listeners al nuevo socket antes de iniciarlo
                for (ChatEventListener listener : this.listeners) {
                    socketClient.addChatEventListener(listener);
                }
                // Registramos el cliente en el Mediador (por IP)
                Mediador.getInstancia().registrar(socketClient);
                socketClient.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
