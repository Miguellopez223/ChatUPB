package edu.upb.chatupb_v2;

import edu.upb.chatupb_v2.bl.server.SocketClient;

public class ClienteMain {
    public static void main(String[] args) {
        try {
            SocketClient socketClient = new SocketClient("localhost");
            socketClient.start();

            socketClient.send("Hello");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
