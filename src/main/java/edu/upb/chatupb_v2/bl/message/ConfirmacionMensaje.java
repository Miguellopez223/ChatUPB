package edu.upb.chatupb_v2.bl.message;

import java.util.regex.Pattern;

/**
 * Trama 008 - Confirmacion de recepcion de mensaje.
 * Formato: 008|idUsuario|nombre|idMensaje
 *
 * Se envia como respuesta a la trama 007, confirmando que el mensaje
 * fue recibido exitosamente por el destinatario.
 *
 */
public class ConfirmacionMensaje extends Message {

    private String idUsuario;
    private String nombre;
    private String idMensaje;

    public ConfirmacionMensaje() {
        super("008");
    }

    public ConfirmacionMensaje(String idUsuario, String nombre, String idMensaje) {
        super("008");
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.idMensaje = idMensaje;
    }

    public static ConfirmacionMensaje parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (split.length != 4) {
            throw new IllegalArgumentException("Formato de trama no valido para 008");
        }
        return new ConfirmacionMensaje(split[1], split[2], split[3]);
    }

    @Override
    public String generarTrama() {
        return getCodigo() + "|" + idUsuario + "|" + nombre + "|" + idMensaje;
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

    public String getIdMensaje() {
        return idMensaje;
    }

    public void setIdMensaje(String idMensaje) {
        this.idMensaje = idMensaje;
    }
}
