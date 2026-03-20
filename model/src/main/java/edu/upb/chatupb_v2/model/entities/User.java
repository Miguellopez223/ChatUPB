package edu.upb.chatupb_v2.model.entities;

import lombok.*;

import java.io.Serializable;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User implements Serializable, Model {

    public static final class Column {
        public static final String ID = "id";
        public static final String CODE = "code";
        public static final String NAME = "name";
    }

    private String id;
    private String code;
    private String name;

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return name; // To display correctly in the JComboBox
    }
}
