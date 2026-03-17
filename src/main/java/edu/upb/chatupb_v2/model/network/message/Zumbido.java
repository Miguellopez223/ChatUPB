package edu.upb.chatupb_v2.model.network.message;

import java.util.regex.Pattern;

/**
 * Trama 010 - Zumbido (Buzz).
 * Formato: 010|idUsuario
 *
 * Se envia cuando un usuario quiere llamar la atencion de otro.
 * Al recibirla, la interfaz del destinatario tiembla brevemente.
 *
 */
public class Zumbido extends Message {

    private String idUsuario;

    public Zumbido() {
        super("010");
    }

    public Zumbido(String idUsuario) {
        super("010");
        this.idUsuario = idUsuario;
    }

    public static Zumbido parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (split.length != 2) {
            throw new IllegalArgumentException("Formato de trama no valido para 010");
        }
        return new Zumbido(split[1]);
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
