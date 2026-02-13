package edu.upb.chatupb_v2.bl.message;

import java.util.regex.Pattern;

public class AceptacionInvitacion extends Message {

    private String idUsuario;
    private String nombre;

    public AceptacionInvitacion() {
        super("002");
    }

    public AceptacionInvitacion(String idUsuario, String nombre) {
        super("002");
        this.idUsuario = idUsuario;
        this.nombre = nombre;
    }

    public static AceptacionInvitacion parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if(split.length != 3) {
            throw new IllegalArgumentException("Formato de trama no válido para 002");
        }
        return new AceptacionInvitacion(split[1], split[2]);
    }

    @Override
    public String generarTrama() {
        return getCodigo() + "|" + idUsuario + "|" + nombre + System.lineSeparator();
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