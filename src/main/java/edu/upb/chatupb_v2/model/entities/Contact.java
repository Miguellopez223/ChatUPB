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
 * | id      | VARCHAR(36) PRIMARY KEY (UUID) |
 * | code    | TEXT DEFAULT NULL              |
 * | name    | TEXT DEFAULT NULL              |
 * | ip      | TEXT DEFAULT NULL              |
 * | user_id | VARCHAR(36) DEFAULT NULL (FK)  |
 * +---------+-------------------------------+
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Contact implements Serializable, Model {

    /** Constantes de nombres de columna en la BD */
    public static final class Column {
        public static final String ID   = "id";
        public static final String CODE = "code";
        public static final String NAME = "name";
        public static final String IP   = "ip";
        public static final String USER_ID = "user_id";
    }

    private String id;
    private String code;
    private String name;
    private String ip;
    private String userId;

    @Override
    public void setId(String id) {
        this.id = id;
    }
}
