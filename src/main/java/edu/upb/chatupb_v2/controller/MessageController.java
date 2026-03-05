package edu.upb.chatupb_v2.controller;

import edu.upb.chatupb_v2.model.entities.ChatMessage;
import edu.upb.chatupb_v2.model.entities.Contact;
import edu.upb.chatupb_v2.model.repository.ChatMessageDao;
import edu.upb.chatupb_v2.view.ChatMessageInfo;

import java.util.ArrayList;
import java.util.List;

public class MessageController {

    private final ChatMessageDao messageDao;

    public MessageController() {
        this.messageDao = new ChatMessageDao();
    }

    public ChatMessage guardarMensajeEnviado(String receiverCode, String content, long timestamp) {
        ChatMessage msg = ChatMessage.builder()
                .senderCode(Contact.ME_CODE)
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
        ChatMessage msg = ChatMessage.builder()
                .senderCode(senderCode)
                .receiverCode(Contact.ME_CODE)
                .content(content)
                .timestamp(timestamp)
                .confirmed(true)
                .build();
        try {
            messageDao.save(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return msg;
    }

    public List<ChatMessageInfo> cargarHistorial(String contactCode) {
        List<ChatMessageInfo> result = new ArrayList<>();
        try {
            List<ChatMessage> messages = messageDao.findConversation(Contact.ME_CODE, contactCode);
            for (ChatMessage m : messages) {
                result.add(new ChatMessageInfo(
                        m.getSenderCode(),
                        m.getContent(),
                        m.getTimestamp(),
                        m.isConfirmed(),
                        m.isMine()
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public void marcarConfirmado(String idMensaje) {
        try {
            messageDao.markConfirmed(idMensaje);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
