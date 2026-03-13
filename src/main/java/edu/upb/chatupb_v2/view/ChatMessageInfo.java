package edu.upb.chatupb_v2.view;

/**
 * DTO para transportar datos de mensaje al View sin exponer la entidad del repositorio.
 */
public class ChatMessageInfo {

    private final String senderCode;
    private final String content;
    private final long timestamp;
    private final boolean confirmed;
    private final boolean mine;
    private final boolean image;

    public ChatMessageInfo(String senderCode, String content, long timestamp,
                           boolean confirmed, boolean mine) {
        this(senderCode, content, timestamp, confirmed, mine, false);
    }

    public ChatMessageInfo(String senderCode, String content, long timestamp,
                           boolean confirmed, boolean mine, boolean image) {
        this.senderCode = senderCode;
        this.content = content;
        this.timestamp = timestamp;
        this.confirmed = confirmed;
        this.mine = mine;
        this.image = image;
    }

    public String getSenderCode() {
        return senderCode;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isMine() {
        return mine;
    }

    public boolean isImage() {
        return image;
    }
}
