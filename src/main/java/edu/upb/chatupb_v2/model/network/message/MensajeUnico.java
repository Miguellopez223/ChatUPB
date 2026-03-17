package edu.upb.chatupb_v2.model.network.message;

import java.util.regex.Pattern;

/**
 * Trama 012 - Mensaje de visualización única.
 * Formato: 012|idUsuario|idMensaje|contenido
 */
public class MensajeUnico extends Message {

    private String idUsuario;
    private String idMensaje;
    private String contenido;

    public MensajeUnico() {
        super("012");
    }

    public MensajeUnico(String idUsuario, String idMensaje, String contenido) {
        super("012");
        this.idUsuario = idUsuario;
        this.idMensaje = idMensaje;
        this.contenido = contenido;
    }

    public static MensajeUnico parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (split.length != 4) {
            throw new IllegalArgumentException("Formato de trama no válido para 012");
        }
        return new MensajeUnico(split[1], split[2], split[3]);
    }

    @Override
    public String generarTrama() {
        return getCodigo() + "|" + idUsuario + "|" + idMensaje + "|" + contenido;
    }

    public String getIdUsuario() { return idUsuario; }
    public String getIdMensaje() { return idMensaje; }
    public String getContenido() { return contenido; }
}