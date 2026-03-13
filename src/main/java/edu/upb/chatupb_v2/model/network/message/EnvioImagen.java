package edu.upb.chatupb_v2.model.network.message;

import edu.upb.chatupb_v2.model.network.SocketClient;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Trama 021 - Envio de imagen pequena (codificada en Base64).
 * Formato: 021|idUsuario|idMensaje|contenidoBase64
 * Patron Command: el metodo execute() envia la trama a traves del socket.
 */
public class EnvioImagen extends Message {

    private String idUsuario;
    private String idMensaje;
    private String contenidoBase64;

    public EnvioImagen() {
        super("021");
    }

    public EnvioImagen(String idUsuario, String idMensaje, String contenidoBase64) {
        super("021");
        this.idUsuario = idUsuario;
        this.idMensaje = idMensaje;
        this.contenidoBase64 = contenidoBase64;
    }

    public static EnvioImagen parse(String trama) {
        String[] split = trama.split(Pattern.quote("|"));
        if (split.length != 4) {
            throw new IllegalArgumentException("Formato de trama no valido para 021");
        }
        return new EnvioImagen(split[1], split[2], split[3]);
    }

    @Override
    public String generarTrama() {
        return getCodigo() + "|" + idUsuario + "|" + idMensaje + "|" + contenidoBase64;
    }


    //  Patron Command: ejecuta el envio de la imagen a traves del socket.

    @Override
    public void execute(SocketClient client) throws IOException {
        client.send(this);
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

    public String getContenidoBase64() {
        return contenidoBase64;
    }

    public void setContenidoBase64(String contenidoBase64) {
        this.contenidoBase64 = contenidoBase64;
    }
}
