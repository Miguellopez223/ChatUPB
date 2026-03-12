package edu.upb.chatupb_v2.controller;

import edu.upb.chatupb_v2.model.entities.ChatMessage;
import edu.upb.chatupb_v2.model.entities.User;
import edu.upb.chatupb_v2.model.repository.ChatMessageDao;
import edu.upb.chatupb_v2.view.ChatMessageInfo;

import java.util.ArrayList;
import java.util.List;

public class MessageController {

    private ChatMessageDao messageDao;
    private User currentUser;

    public MessageController() {
        // Inicialmente sin usuario
        this.messageDao = new ChatMessageDao(0);
    }

    public void setUsuario(User user) {
        this.currentUser = user;
        this.messageDao = new ChatMessageDao(user.getId());
    }

    public ChatMessage guardarMensajeEnviado(String receiverCode, String content, long timestamp) {
        if (currentUser == null) return null;
        ChatMessage msg = ChatMessage.builder()
                .senderCode(currentUser.getCode())
                .receiverCode(receiverCode)
                .content(content)
                .timestamp(timestamp)
                .confirmed(false)
                .build();
        try {
            messageDao.save(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return msg;
    }

    public ChatMessage guardarMensajeRecibido(String senderCode, String content, long timestamp) {
        if (currentUser == null) return null;
        ChatMessage msg = ChatMessage.builder()
                .senderCode(senderCode)
                .receiverCode(currentUser.getCode())
                .content(content)
                .timestamp(timestamp)
                .confirmed(false) // No confirmado hasta que el usuario abra el chat y se envie 008
                .build();
        try {
            messageDao.save(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return msg;
    }

    public List<ChatMessageInfo> cargarHistorial(String contactCode) {
        if (currentUser == null) return new ArrayList<>();
        List<ChatMessageInfo> result = new ArrayList<>();
        try {
            List<ChatMessage> messages = messageDao.findConversation(currentUser.getCode(), contactCode);
            for (ChatMessage m : messages) {
                result.add(new ChatMessageInfo(
                        m.getSenderCode(),
                        m.getContent(),
                        m.getTimestamp(),
                        m.isConfirmed(),
                        m.getSenderCode().equals(currentUser.getCode())
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public void marcarConfirmado(String idMensaje) {
        if (currentUser == null) return;
        try {
            messageDao.markConfirmed(idMensaje);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Obtiene mensajes recibidos de un contacto que aun no fueron confirmados (no se envio 008).
     */
    public List<ChatMessage> obtenerNoConfirmadosRecibidos(String contactCode) {
        if (currentUser == null) return new ArrayList<>();
        try {
            return messageDao.findUnconfirmedReceived(currentUser.getCode(), contactCode);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Marca todos los mensajes recibidos de un contacto como vistos/confirmados.
     */
    public void marcarRecibidosComoVistos(String contactCode) {
        if (currentUser == null) return;
        try {
            messageDao.markReceivedAsConfirmed(currentUser.getCode(), contactCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
