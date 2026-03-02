package edu.upb.chatupb_v2.bl.server;

import edu.upb.chatupb_v2.bl.message.AceptacionInvitacion;
import edu.upb.chatupb_v2.bl.message.ConfirmacionMensaje;
import edu.upb.chatupb_v2.bl.message.EnvioMensaje;
import edu.upb.chatupb_v2.bl.message.Invitacion;
import edu.upb.chatupb_v2.bl.message.RechazoInvitacion;
import edu.upb.chatupb_v2.controller.exception.OperationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Mediador central de conexiones.
 * Patron Singleton: solo existe una instancia en toda la aplicacion.
 * Patron Mediador: centraliza el registro y busqueda de SocketClients por IP.
 * Implementa ChatEventListener para escuchar los eventos de los sockets.
 *
 * El Mediador NO baja a la interfaz ni al controller. Solo gestiona el mapa de
 * clientes (HashMap) y reenvia eventos a los ChatEventListener registrados.
 * El controller se suscribe como listener; asi la dependencia va del controller
 * hacia la BL, nunca al reves.
 */
public class Mediador implements ChatEventListener {

    // --- Singleton ---
    private static Mediador instancia;

    private Mediador() {
    }

    public static Mediador getInstancia() {
        if (instancia == null) {
            instancia = new Mediador();
        }
        return instancia;
    }

    // --- Mapa de clientes: IP -> SocketClient ---
    private final HashMap<String, SocketClient> clientes = new HashMap<>();

    // --- Listeners suscritos (controllers u otros) ---
    private final List<ChatEventListener> listeners = new ArrayList<>();

    /**
     * Suscribe un listener para recibir eventos reenviados por el Mediador.
     * El controller implementa ChatEventListener y se registra aqui.
     */
    public void addChatEventListener(ChatEventListener listener) {
        listeners.add(listener);
    }

    // --- Gestion del mapa de clientes ---

    /**
     * Registra un SocketClient asociado a su IP.
     */
    public void registrar(SocketClient socketClient) {
        String ip = socketClient.getIp();
        clientes.put(ip, socketClient);
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

    // --- Envio de mensajes a traves del mapa de clientes ---

    /**
     * Envia una trama a un cliente especifico identificado por su IP.
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

    // --- Operacion de invitacion: crea socket, registra en mapa, envia trama ---

    /**
     * Inicia una invitacion saliente.
     * Crea el SocketClient, lo registra en el mapa de clientes y envia la trama 001.
     *
     * @param ip        IP destino
     * @param idUsuario identificador del usuario que envia
     * @param nombre    nombre del usuario que envia
     */
    public void invitacion(String ip, String idUsuario, String nombre) {
        SocketClient client;
        try {
            client = new SocketClient(ip);
            client.addChatEventListener(this);
            registrar(client);
            client.start();
        } catch (IOException e) {
            throw new OperationException("No se logró establecer la conexión");
        }

        Invitacion invitacion = new Invitacion(idUsuario, nombre);

        try {
            client.send(invitacion);
        } catch (IOException e) {
            throw new OperationException("No se logró enviar la invitación");
        }
    }

    // --- ChatEventListener: recibe eventos del socket y los reenvia a los listeners ---

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
}
