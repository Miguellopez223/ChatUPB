package edu.upb.chatupb_v2.bl.message;

public abstract class Message {
    private String codigo;

    public Message(String codigo) {
        this.codigo = codigo;
    }
    public String getCodigo() {
        return codigo;
    }
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public abstract String generarTrama();
}
