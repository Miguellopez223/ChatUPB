package edu.upb.chatupb_v2.model.network.message;

import java.util.regex.Pattern;

/**
 * Trama 011 - Fijar mensaje.
 * Formato: 011|idMensaje
 *
 * Se envia cuando un usuario fija un mensaje en la conversacion.
 * Solo se puede tener un mensaje fijado por conversacion.
 * Al recibirla, el destinatario fija ese mensaje en su BD y UI.
 *
 * @author rlaredo
 */
public class FijarMensaje extends Message {

    private String idMensaje;

    public FijarMensaje() {
        super("011");
    }

    public FijarMensaje(String idMensaje) {
        super("011");
        this.idMensaje = idMensaje;
    }

    public static FijarMensaje parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (split.length != 2) {
            throw new IllegalArgumentException("Formato de trama no valido para 011");
        }
        return new FijarMensaje(split[1]);
    }

    @Override
    public String generarTrama() {
        return getCodigo() + "|" + idMensaje;
    }

    public String getIdMensaje() {
        return idMensaje;
    }

    public void setIdMensaje(String idMensaje) {
        this.idMensaje = idMensaje;
    }
}
