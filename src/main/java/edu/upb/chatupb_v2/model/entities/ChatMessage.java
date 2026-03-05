package edu.upb.chatupb_v2.model.entities;

import lombok.*;

import java.io.Serializable;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage implements Serializable, Model {

    public static final class Column {
        public static final String ID = "id";
        public static final String SENDER_CODE = "sender_code";
        public static final String RECEIVER_CODE = "receiver_code";
        public static final String CONTENT = "content";
        public static final String TIMESTAMP = "timestamp";
        public static final String CONFIRMED = "confirmed";
    }

    private long id;
    private String senderCode;
    private String receiverCode;
    private String content;
    private long timestamp;
    private boolean confirmed;

    @Override
    public void setId(long id) {
        this.id = id;
    }

    public boolean isMine() {
        return Contact.ME_CODE.equals(senderCode);
    }
}
