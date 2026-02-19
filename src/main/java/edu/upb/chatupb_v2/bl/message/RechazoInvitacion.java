package edu.upb.chatupb_v2.bl.message;

import java.util.regex.Pattern;

/**
 * Trama 003 - Rechazo de invitacion.
 * Formato: 003|idUsuario|nombre
 *
 * Se envia cuando el usuario receptor decide NO aceptar la invitacion (trama 001).
 *
 * @author rlaredo
 */
public class RechazoInvitacion extends Message {

    private String idUsuario;
    private String nombre;

    public RechazoInvitacion() {
        super("003");
    }

    public RechazoInvitacion(String idUsuario, String nombre) {
        super("003");
        this.idUsuario = idUsuario;
        this.nombre = nombre;
    }

    public static RechazoInvitacion parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (split.length != 3) {
            throw new IllegalArgumentException("Formato de trama no valido para 003");
        }
        return new RechazoInvitacion(split[1], split[2]);
    }

    @Override
    public String generarTrama() {
        return getCodigo() + "|" + idUsuario + "|" + nombre;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
