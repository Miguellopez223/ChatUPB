package edu.upb.chatupb_v2.model.network.message;

import java.util.regex.Pattern;

/**
 * Trama 013 - Cambio de Tema de UI.
 * Formato: 013|idUsuario|idTema
 * idTema: 1 (Defecto), 2 (Azul), 3 (Rojo), 4 (Amarillo), 5 (Violeta)
 */
public class CambioTema extends Message {

    private String idUsuario;
    private String idTema;

    public CambioTema() {
        super("013");
    }

    public CambioTema(String idUsuario, String idTema) {
        super("013");
        this.idUsuario = idUsuario;
        this.idTema = idTema;
    }

    public static CambioTema parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (split.length != 3) {
            throw new IllegalArgumentException("Formato de trama no válido para 013");
        }
        return new CambioTema(split[1], split[2]);
    }

    @Override
    public String generarTrama() {
        return getCodigo() + "|" + idUsuario + "|" + idTema;
    }

    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }
    public String getIdTema() { return idTema; }
    public void setIdTema(String idTema) { this.idTema = idTema; }
}