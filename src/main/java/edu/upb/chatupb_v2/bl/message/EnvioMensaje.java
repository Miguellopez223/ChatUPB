package edu.upb.chatupb_v2.bl.message;

import java.util.regex.Pattern;

/**
 * Trama 007 - Envio de mensaje de texto.
 * Formato: 007|idUsuario|nombre|contenido
 *
 * Se envia cuando un usuario manda un mensaje de chat a la persona
 * con la que ya esta conectado (invitacion aceptada).
 *
 * @author rlaredo
 */
public class EnvioMensaje extends Message {

    private String idUsuario;
    private String nombre;
    private String contenido;

    public EnvioMensaje() {
        super("007");
    }

    public EnvioMensaje(String idUsuario, String nombre, String contenido) {
        super("007");
        this.idUsuario = idUsuario;
        this.nombre = nombre;
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
        return getCodigo() + "|" + idUsuario + "|" + nombre + "|" + contenido;
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

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }
}
