/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.upb.chatupb_v2.bl.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

/**
 *
 * @author rlaredo
 */
public class ChatServer extends Thread {

    private static final int port = 1900;
    private final ServerSocket server;
    private ChatEventListener listener;
    public ChatServer() throws IOException {
        this.server = new ServerSocket(port);
    }

    public void setChatEventListener(ChatEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        while (true) {
            try {
                SocketClient socketClient = new SocketClient(this.server.accept());
                // Le pasamos el avisador al nuevo socket antes de iniciarlo
                socketClient.setChatEventListener(this.listener);
                socketClient.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
