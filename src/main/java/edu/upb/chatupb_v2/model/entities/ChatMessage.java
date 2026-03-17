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
        public static final String PINNED = "pinned";
        public static final String USER_ID = "user_id";
        public static final String VIEW_ONCE = "view_once";
    }

    private String id;
    private String senderCode;
    private String receiverCode;
    private String content;
    private String timestamp;
    private boolean confirmed;
    private boolean pinned;
    private String userId;
    private boolean viewOnce;

    @Override
    public void setId(String id) {
        this.id = id;
    }
}
