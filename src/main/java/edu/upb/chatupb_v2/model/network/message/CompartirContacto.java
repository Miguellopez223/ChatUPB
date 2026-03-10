package edu.upb.chatupb_v2.model.network.message;

import java.util.regex.Pattern;


// ---------- PREGUNTA 5 --------------
 //Trama 020 - Compartir un contacto con un amigo.
 //Formato: 020|ID_USUARIO|NOMBRE|IP
public class CompartirContacto extends Message {

    private String idUsuario;
    private String nombre;
    private String ip;

    public CompartirContacto() {
        super("020");
    }

    public CompartirContacto(String idUsuario, String nombre, String ip) {
        super("020");
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.ip = ip;
    }

    public static CompartirContacto parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (split.length != 4) {
            throw new IllegalArgumentException("Formato de trama no valido para 020");
        }
        return new CompartirContacto(split[1], split[2], split[3]);
    }

    @Override
    public String generarTrama() {
        return getCodigo() + "|" + idUsuario + "|" + nombre + "|" + ip;
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

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
