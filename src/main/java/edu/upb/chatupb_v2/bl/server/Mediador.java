package edu.upb.chatupb_v2.bl.server;

import edu.upb.chatupb_v2.bl.message.AceptacionInvitacion;
import edu.upb.chatupb_v2.bl.message.ConfirmacionMensaje;
import edu.upb.chatupb_v2.bl.message.Despedida;
import edu.upb.chatupb_v2.bl.message.EnvioMensaje;
import edu.upb.chatupb_v2.bl.message.Invitacion;
import edu.upb.chatupb_v2.bl.message.RechazoInvitacion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Mediador central de conexiones.
 * Patron Singleton: solo existe una instancia en toda la aplicacion.
 * Patron Mediador: centraliza el registro de SocketClients por IP,
 * el envio de tramas y la entrega de eventos a la interfaz grafica.
 *
 * El Mediador se suscribe a cada SocketClient y reenvia los eventos
 * a los listeners de la UI, de modo que la UI nunca se suscribe
 * directamente a un SocketClient.
 */
public class Mediador implements ChatEventListener {

    // --- Singleton ---
    private static Mediador instancia;

    private Mediador() {
        // Constructor privado para evitar instancias externas
    }

    public static Mediador getInstancia() {
        if (instancia == null) {
            instancia = new Mediador();
        }
        return instancia;
    }

    // --- HashMap: IP -> SocketClient ---
    private final HashMap<String, SocketClient> clientes = new HashMap<>();

    // ---- PREGUNTA 4 ------
    // --- Listeners de la UI que reciben los eventos reenviados ---
    private final List<ChatEventListener> listeners = new ArrayList<>();

    // Registra un listener de UI para recibir eventos.
    public void addChatEventListener(ChatEventListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Registra un SocketClient asociado a su IP.
     * El Mediador se auto-suscribe al SocketClient para interceptar sus eventos.
     */
    public void registrar(SocketClient socketClient) {
        String ip = socketClient.getIp();
        clientes.put(ip, socketClient);
        // El Mediador se suscribe al SocketClient para recibir sus eventos
        socketClient.addChatEventListener(this);
        System.out.println("[Mediador] Cliente registrado: " + ip
                + " | Total conectados: " + clientes.size());
    }

    /**
     * Elimina un SocketClient del registro por su IP.
     */
    public void eliminar(String ip) {
        clientes.remove(ip);
        System.out.println("[Mediador] Cliente eliminado: " + ip
                + " | Total conectados: " + clientes.size());
    }

    /**
     * Busca un SocketClient por su IP.
     * @return el SocketClient asociado, o null si no existe.
     */
    public SocketClient obtener(String ip) {
        return clientes.get(ip);
    }

    /**
     * Verifica si existe un cliente registrado con esa IP.
     */
    public boolean existe(String ip) {
        return clientes.containsKey(ip);
    }

    /**
     * Devuelve el HashMap completo de clientes (lectura).
     */
    public HashMap<String, SocketClient> getClientes() {
        return clientes;
    }

    /**
     * Retorna la cantidad de clientes conectados.
     */
    public int getCantidadConectados() {
        return clientes.size();
    }

    /**
     * Envia una trama a un cliente especifico identificado por su IP.
     * Centraliza el envio para que la UI no acceda directamente al socket.
     */
    public void enviarMensaje(String ip, String trama) throws IOException {
        SocketClient cliente = clientes.get(ip);
        if (cliente != null) {
            cliente.send(trama);
        } else {
            System.out.println("[Mediador] No se encontró cliente con IP: " + ip);
        }
    }

    /**
     * Envia una trama a todos los clientes conectados (broadcast).
     */
    public void enviarATodos(String trama) throws IOException {
        for (SocketClient cliente : clientes.values()) {
            cliente.send(trama);
        }
    }

    // ----------------------------------------------------------------
    // Implementacon de ChatEventListener:
    // El Mediador recibe eventos de cada SocketClient y los reenvia
    // a todos los listeners de UI registrados.
    // -----------------------------------------------------------------

    @Override
    public void onInvitacionRecibida(Invitacion inv, SocketClient sender) {
        for (ChatEventListener listener : listeners) {
            listener.onInvitacionRecibida(inv, sender);
        }
    }

    @Override
    public void onAceptacionRecibida(AceptacionInvitacion acc, SocketClient sender) {
        for (ChatEventListener listener : listeners) {
            listener.onAceptacionRecibida(acc, sender);
        }
    }

    @Override
    public void onRechazoRecibido(RechazoInvitacion rechazo, SocketClient sender) {
        for (ChatEventListener listener : listeners) {
            listener.onRechazoRecibido(rechazo, sender);
        }
    }

    @Override
    public void onMensajeRecibido(EnvioMensaje msg, SocketClient sender) {
        for (ChatEventListener listener : listeners) {
            listener.onMensajeRecibido(msg, sender);
        }
    }

    @Override
    public void onConfirmacionRecibida(ConfirmacionMensaje conf, SocketClient sender) {
        for (ChatEventListener listener : listeners) {
            listener.onConfirmacionRecibida(conf, sender);
        }
    }

    @Override
    public void onDespedidaRecibida(Despedida despedida, SocketClient sender) {
        for (ChatEventListener listener : listeners) {
            listener.onDespedidaRecibida(despedida, sender);
        }
    }
}
