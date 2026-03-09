package edu.upb.chatupb_v2.model.entities;

import lombok.*;

import java.io.Serializable;

/**
 * Entidad que representa un contacto en la base de datos.
 *
 * Tabla: contact
 * +---------+-------------------------------+
 * | Columna | Tipo                          |
 * +---------+-------------------------------+
 * | id      | INTEGER PRIMARY KEY AUTO      |
 * | code    | TEXT NOT NULL UNIQUE (UUID)    |
 * | name    | TEXT NOT NULL                  |
 * | ip      | TEXT NOT NULL                  |
 * | user_id | INTEGER NOT NULL (FK)          |
 * +---------+-------------------------------+
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Contact implements Serializable, Model {

    /** UUID que identifica a este usuario local en la red P2P */
    // public static final String ME_CODE = "af3bc20a-766c-4cd4-813d-b1067a01fa9a"; // Deprecated, use User.code

    /** Constantes de nombres de columna en la BD */
    public static final class Column {
        public static final String ID   = "id";
        public static final String CODE = "code";
        public static final String NAME = "name";
        public static final String IP   = "ip";
        public static final String USER_ID = "user_id";
    }

    private long id;
    private String code;
    private String name;
    private String ip;
    private long userId;

    @Override
    public void setId(long id) {
        this.id = id;
    }
}
