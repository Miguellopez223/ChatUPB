package edu.upb.chatupb_v2.bl.message;

import java.util.regex.Pattern;

/**
 * Trama 003 - Rechazo de invitacion.
 * Formato: 003|
 *
 * Se envia cuando el usuario receptor decide NO aceptar la invitacion (trama 001).
 * No lleva datos adicionales, solo el codigo de rechazo.
 *
 */
public class RechazoInvitacion extends Message {

    public RechazoInvitacion() {
        super("003");
    }

    public static RechazoInvitacion parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (!split[0].equals("003")) {
            throw new IllegalArgumentException("Formato de trama no valido para 003");
        }
        return new RechazoInvitacion();
    }

    @Override
    public String generarTrama() {
        return getCodigo() + "|";
    }
}
