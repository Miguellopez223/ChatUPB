package edu.upb.chatupb_v2.model.network.message;

import java.util.regex.Pattern;

/**
 * Trama 005 - Respuesta al Hello.
 * Formato: 005|idUsuario
 *
 * Se envia como respuesta a una trama 004 (Hello).
 * Al recibir esta respuesta, se cambia el estado del contacto a conectado.
 */
public class HelloResponse extends Message {

    private String idUsuario;

    public HelloResponse() {
        super("005");
    }

    public HelloResponse(String idUsuario) {
        super("005");
        this.idUsuario = idUsuario;
    }

    public static HelloResponse parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (split.length < 2) {
            throw new IllegalArgumentException("Formato de trama no valido para 005");
        }
        return new HelloResponse(split[1]);
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
