package edu.upb.chatupb_v2.model.network.message;

import java.util.regex.Pattern;

/**
 * Trama 006 - Rechazo al Hello.
 * Formato: 006|
 *
 * Se envia como respuesta a un Hello (004) cuando el ID del remitente
 * NO existe en la base de datos de contactos del receptor.
 */
public class HelloRechazo extends Message {

    public HelloRechazo() {
        super("006");
    }

    public static HelloRechazo parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (!split[0].equals("006")) {
            throw new IllegalArgumentException("Formato de trama no valido para 006");
        }
        return new HelloRechazo();
    }

    @Override
    public String generarTrama() {
        return getCodigo() + "|";
    }
}
