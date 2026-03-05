package edu.upb.chatupb_v2.view;

/**
 * DTO para transportar datos de contacto al View sin exponer la entidad del repositorio.
 * Asi la capa de datos (Contact) no llega a la UI.
 */
public class ContactInfo {

    private final long id;
    private final String code;
    private final String name;
    private final String ip;

    public ContactInfo(long id, String code, String name, String ip) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.ip = ip;
    }

    public long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }
}
