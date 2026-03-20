package edu.upb.chatupb_v2.model.network.message;

import java.util.regex.Pattern;

/**
 * Trama 004 - Hello.
 * Formato: 004|idUsuario
 *
 * Se envia al iniciar la aplicacion a todos los contactos guardados en la BD,
 * para notificarles que este usuario esta en linea.
 */
public class Hello extends Message {

    private String idUsuario;

    public Hello() {
        super("004");
    }

    public Hello(String idUsuario) {
        super("004");
        this.idUsuario = idUsuario;
    }

    public static Hello parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (split.length < 2) {
            throw new IllegalArgumentException("Formato de trama no valido para 004");
        }
        return new Hello(split[1]);
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
