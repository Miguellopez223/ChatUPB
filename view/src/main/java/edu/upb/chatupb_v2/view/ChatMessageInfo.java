package edu.upb.chatupb_v2.view;

/**
 * DTO para transportar datos de mensaje al View sin exponer la entidad del repositorio.
 */
public class ChatMessageInfo {

    private final String id;
    private final String senderCode;
    private final String content;
    private final String timestamp;
    private final boolean confirmed;
    private final boolean mine;
    private final boolean pinned;
    private final boolean viewOnce; // <-- Nuevo campo para Trama 012

    // Constructor original (por retrocompatibilidad)
    public ChatMessageInfo(String id, String senderCode, String content, String timestamp,
                           boolean confirmed, boolean mine) {
        this(id, senderCode, content, timestamp, confirmed, mine, false, false);
    }

    // Constructor con pinned (por retrocompatibilidad)
    public ChatMessageInfo(String id, String senderCode, String content, String timestamp,
                           boolean confirmed, boolean mine, boolean pinned) {
        this(id, senderCode, content, timestamp, confirmed, mine, pinned, false);
    }

    // <-- NUEVO CONSTRUCTOR COMPLETO (8 parámetros) que usa el MessageController
    public ChatMessageInfo(String id, String senderCode, String content, String timestamp,
                           boolean confirmed, boolean mine, boolean pinned, boolean viewOnce) {
        this.id = id;
        this.senderCode = senderCode;
        this.content = content;
        this.timestamp = timestamp;
        this.confirmed = confirmed;
        this.mine = mine;
        this.pinned = pinned;
        this.viewOnce = viewOnce;
    }

    public String getId() {
        return id;
    }

    public String getSenderCode() {
        return senderCode;
    }

    public String getContent() {
        return content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isMine() {
        return mine;
    }

    public boolean isPinned() {
        return pinned;
    }

    public boolean isViewOnce() {
        return viewOnce;
    }
}