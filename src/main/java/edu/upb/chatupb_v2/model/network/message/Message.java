package edu.upb.chatupb_v2.model.network.message;

import edu.upb.chatupb_v2.model.network.SocketClient;

import java.io.IOException;

/**
 * Clase abstracta base para todos los mensajes del protocolo.
 * Implementa el patron Command: cada subclase define su propio
 * comportamiento de ejecucion en el metodo execute().
 *
 */
public abstract class Message {
    private String codigo;

    public Message(String codigo) {
        this.codigo = codigo;
    }
    public String getCodigo() {
        return codigo;
    }
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public abstract String generarTrama();

    /**
     * Patron Command: ejecuta la accion asociada a este mensaje
     * enviandolo a traves del SocketClient indicado.
     */
    public void execute(SocketClient client) throws IOException {
        client.send(this);
    }
}
