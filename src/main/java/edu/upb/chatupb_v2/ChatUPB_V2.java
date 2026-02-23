/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package edu.upb.chatupb_v2;

import edu.upb.chatupb_v2.bl.server.ChatServer;
import edu.upb.chatupb_v2.bl.server.Mediador;

public class ChatUPB_V2 {
    public static void main(String[] args) {

        ChatUI chatUI = new ChatUI();

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                chatUI.setVisible(true);
            }
        });

        // Registrar la UI en el Mediador para que reciba todos los eventos
        Mediador.getInstancia().addChatEventListener(chatUI);

        try{
            ChatServer chatServer = new ChatServer();
            chatServer.start();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
