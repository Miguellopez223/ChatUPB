/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package edu.upb.chatupb_v2;

import edu.upb.chatupb_v2.bl.server.ChatServer;

public class ChatUPB_V2 {
    public static void main(String[] args) {

        ChatUI chatUI = new ChatUI();

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                chatUI.setVisible(true);
            }
        });

        try{
            ChatServer chatServer = new ChatServer();
            // Le pasamos la vista al servidor para que escuche los eventos
            chatServer.addChatEventListener(chatUI);
            chatServer.start();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
