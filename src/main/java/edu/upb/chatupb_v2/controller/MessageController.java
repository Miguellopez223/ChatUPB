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
        this.messageDao = new ChatMessageDao();
    }

    public void setUsuario(User user) {
        this.currentUser = user;
        this.messageDao = new ChatMessageDao(user.getId());
    }

    public ChatMessage guardarMensajeEnviado(String receiverCode, String content, String idMensaje, String timestamp, boolean viewOnce) {
        if (currentUser == null) return null;
        ChatMessage msg = ChatMessage.builder()
                .id(idMensaje)
                .senderCode(currentUser.getCode())
                .receiverCode(receiverCode)
                .content(content)
                .timestamp(timestamp)
                .confirmed(false)
                .viewOnce(viewOnce) // Agregado para trama 012
                .build();
        try {
            messageDao.save(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return msg;
    }

    public ChatMessage guardarMensajeRecibido(String senderCode, String content, String idMensaje, String timestamp, boolean viewOnce) {
        if (currentUser == null) return null;
        ChatMessage msg = ChatMessage.builder()
                .id(idMensaje)
                .senderCode(senderCode)
                .receiverCode(currentUser.getCode())
                .content(content)
                .timestamp(timestamp)
                .confirmed(false)
                .viewOnce(viewOnce) // Agregado para trama 012
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
                        m.getId(),
                        m.getSenderCode(),
                        m.getContent(),
                        m.getTimestamp(),
                        m.isConfirmed(),
                        m.getSenderCode().equals(currentUser.getCode()),
                        m.isPinned(),
                        m.isViewOnce() // Agregado mapeo de viewOnce
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // --- NUEVO METODO PARA TRAMA 012 ---
    public ChatMessage obtenerMensajePorId(String idMensaje) {
        if (currentUser == null) return null;
        try {
            return messageDao.findById(idMensaje);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
     * Elimina el contenido de un mensaje (lo pone a null en la BD).
     */
    public void eliminarContenidoMensaje(String idMensaje) {
        if (currentUser == null) return;
        try {
            messageDao.setContentNull(idMensaje);
        } catch (Exception e) {
            e.printStackTrace();
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

    /**
     * Fija un mensaje en la conversacion con un contacto.
     * Desfija automaticamente cualquier mensaje previamente fijado.
     */
    public void fijarMensaje(String idMensaje, String contactCode) {
        if (currentUser == null) return;
        try {
            messageDao.pinMessage(idMensaje, currentUser.getCode(), contactCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Desfija un mensaje.
     */
    public void desfijarMensaje(String idMensaje) {
        if (currentUser == null) return;
        try {
            messageDao.unpinMessage(idMensaje);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Obtiene el mensaje fijado de una conversacion, o null si no hay ninguno.
     */
    public ChatMessageInfo obtenerMensajeFijado(String contactCode) {
        if (currentUser == null) return null;
        try {
            ChatMessage m = messageDao.findPinnedMessage(currentUser.getCode(), contactCode);
            if (m == null) return null;
            return new ChatMessageInfo(
                    m.getId(),
                    m.getSenderCode(),
                    m.getContent(),
                    m.getTimestamp(),
                    m.isConfirmed(),
                    m.getSenderCode().equals(currentUser.getCode()),
                    m.isPinned(),
                    m.isViewOnce() // Agregado mapeo de viewOnce
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}