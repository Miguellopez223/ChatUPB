package edu.upb.chatupb_v2.model.network.message;

import java.util.regex.Pattern;

/**
 * Trama 009 - Eliminacion de mensaje.
 * Formato: 009|idMensaje
 *
 * Se envia cuando un usuario elimina un mensaje que el mismo envio.
 * Al recibirla, el destinatario pone el contenido del mensaje a null en la BD
 * y muestra "Este mensaje fue eliminado" en la burbuja.
 *
 * @author rlaredo
 */
public class EliminacionMensaje extends Message {

    private String idMensaje;

    public EliminacionMensaje() {
        super("009");
    }

    public EliminacionMensaje(String idMensaje) {
        super("009");
        this.idMensaje = idMensaje;
    }

    public static EliminacionMensaje parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (split.length != 2) {
            throw new IllegalArgumentException("Formato de trama no valido para 009");
        }
        return new EliminacionMensaje(split[1]);
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
