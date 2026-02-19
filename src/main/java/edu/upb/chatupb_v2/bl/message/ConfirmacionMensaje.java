package edu.upb.chatupb_v2.bl.message;

import java.util.regex.Pattern;

/**
 * Trama 008 - Confirmacion de recepcion de mensaje.
 * Formato: 008|idMensaje
 *
 * Se envia como respuesta a la trama 007, confirmando que el mensaje
 * fue recibido exitosamente por el destinatario.
 *
 */
public class ConfirmacionMensaje extends Message {

    private String idMensaje;

    public ConfirmacionMensaje() {
        super("008");
    }

    public ConfirmacionMensaje(String idMensaje) {
        super("008");
        this.idMensaje = idMensaje;
    }

    public static ConfirmacionMensaje parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (split.length != 2) {
            throw new IllegalArgumentException("Formato de trama no valido para 008");
        }
        return new ConfirmacionMensaje(split[1]);
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
