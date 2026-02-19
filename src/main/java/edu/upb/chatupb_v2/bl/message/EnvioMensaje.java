package edu.upb.chatupb_v2.bl.message;

import java.util.regex.Pattern;

/**
 * Trama 007 - Envio de mensaje de texto.
 * Formato: 007|idUsuario|idMensaje|contenido
 *
 * Se envia cuando un usuario manda un mensaje de chat a la persona
 * con la que ya esta conectado (invitacion aceptada).
 *
 * @author rlaredo
 */
public class EnvioMensaje extends Message {

    private String idUsuario;
    private String idMensaje;
    private String contenido;

    public EnvioMensaje() {
        super("007");
    }

    public EnvioMensaje(String idUsuario, String idMensaje, String contenido) {
        super("007");
        this.idUsuario = idUsuario;
        this.idMensaje = idMensaje;
        this.contenido = contenido;
    }

    public static EnvioMensaje parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (split.length != 4) {
            throw new IllegalArgumentException("Formato de trama no valido para 007");
        }
        return new EnvioMensaje(split[1], split[2], split[3]);
    }

    @Override
    public String generarTrama() {
        return getCodigo() + "|" + idUsuario + "|" + idMensaje + "|" + contenido;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getIdMensaje() {
        return idMensaje;
    }

    public void setIdMensaje(String idMensaje) {
        this.idMensaje = idMensaje;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }
}
