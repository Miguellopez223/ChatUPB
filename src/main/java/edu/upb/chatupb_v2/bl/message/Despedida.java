package edu.upb.chatupb_v2.bl.message;

import java.util.regex.Pattern;

// -------------- CLASE DESPEDIDA PARA EL PROTOCOLO 0018 DE PREGUNTA 5 EXAMEN --------------------

public class Despedida extends Message {

    private String idUsuario;

    public Despedida() {
        super("0018");
    }

    public Despedida(String idUsuario) {
        super("0018");
        this.idUsuario = idUsuario;
    }

    public static Despedida parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (split.length < 2) {
            throw new IllegalArgumentException("Formato de trama no valido para 0018");
        }
        return new Despedida(split[1]);
    }

    @Override
    public String generarTrama() {
        return getCodigo() + "|" + idUsuario;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }
}
