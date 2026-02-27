/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package edu.upb.chatupb_v2;

import edu.upb.chatupb_v2.bl.server.ChatServer;
import edu.upb.chatupb_v2.bl.server.Mediador;
import edu.upb.chatupb_v2.repository.ContactDao;

public class ChatUPB_V2 {
    public static void main(String[] args) {

        // Crear tabla de contactos si no existe
        new ContactDao().createTableIfNotExists();

        ChatUI chatUI = new ChatUI();

        // Configurar el Mediador con el ChatController para que pueda delegar eventos
        Mediador.getInstancia().setChatController(chatUI.getChatController());

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                chatUI.setVisible(true);
            }
        });

        try{
            ChatServer chatServer = new ChatServer();
            // El Mediador es quien escucha los eventos del socket (ChatEventListener)
            chatServer.addChatEventListener(Mediador.getInstancia());
            chatServer.start();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
