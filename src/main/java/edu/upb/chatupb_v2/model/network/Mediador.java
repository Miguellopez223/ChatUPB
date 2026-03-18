package edu.upb.chatupb_v2.model.network;

import edu.upb.chatupb_v2.model.network.message.AceptacionInvitacion;
import edu.upb.chatupb_v2.model.network.message.ConfirmacionMensaje;
import edu.upb.chatupb_v2.model.network.message.EliminacionMensaje;
import edu.upb.chatupb_v2.model.network.message.EnvioMensaje;
import edu.upb.chatupb_v2.model.network.message.Hello;
import edu.upb.chatupb_v2.model.network.message.HelloRechazo;
import edu.upb.chatupb_v2.model.network.message.HelloResponse;
import edu.upb.chatupb_v2.model.network.message.Invitacion;
import edu.upb.chatupb_v2.model.network.message.RechazoInvitacion;
import edu.upb.chatupb_v2.model.network.message.Zumbido;
import edu.upb.chatupb_v2.model.network.message.FijarMensaje;
import edu.upb.chatupb_v2.model.network.message.CambioTema;
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
     * Cierra todas las conexiones activas y limpia el registro.
     * Se invoca al cambiar de usuario para asegurar un estado de red limpio.
     */
    public void cerrarTodasLasConexiones() {
        System.out.println("[Mediador] Cerrando todas las conexiones activas...");
        // Iterar sobre una copia para evitar ConcurrentModificationException
        for (SocketClient cliente : new ArrayList<>(clientes.values())) {
            cliente.close();
        }
        clientes.clear();
        System.out.println("[Mediador] Todas las conexiones han sido cerradas.");
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

    // --- Operaciones salientes: crean socket, registran en mapa, envian trama ---

    /**
     * Inicia una invitacion saliente (trama 001).
     * Crea el SocketClient, lo registra en el mapa de clientes y envia la trama.
     */
    public void invitacion(String ip, String idUsuario, String nombre) {
        SocketClient client = conectarYRegistrar(ip);

        Invitacion invitacion = new Invitacion(idUsuario, nombre);
        try {
            client.send(invitacion);
        } catch (IOException e) {
            throw new OperationException("No se logró enviar la invitación");
        }
    }

    /**
     * Envia un Hello (trama 004) a un contacto.
     * Crea el SocketClient, lo registra en el mapa de clientes y envia la trama.
     */
    public void enviarHello(String ip, String idUsuario) {
        SocketClient client = conectarYRegistrar(ip);

        Hello hello = new Hello(idUsuario);
        try {
            client.send(hello);
        } catch (IOException e) {
            throw new OperationException("No se logró enviar el Hello");
        }
    }

    /**
     * Crea un SocketClient saliente, lo registra en el mapa y lo inicia.
     */
    private SocketClient conectarYRegistrar(String ip) {
        try {
            SocketClient client = new SocketClient(ip);
            client.addChatEventListener(this);
            registrar(client);
            client.start();
            return client;
        } catch (IOException e) {
            throw new OperationException("No se logró establecer la conexión con " + ip);
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
    public void onHelloRecibido(Hello hello, SocketClient sender) {
        for (ChatEventListener listener : listeners) {
            listener.onHelloRecibido(hello, sender);
        }
    }

    @Override
    public void onHelloResponseRecibido(HelloResponse response, SocketClient sender) {
        for (ChatEventListener listener : listeners) {
            listener.onHelloResponseRecibido(response, sender);
        }
    }

    @Override
    public void onHelloRechazoRecibido(HelloRechazo rechazo, SocketClient sender) {
        for (ChatEventListener listener : listeners) {
            listener.onHelloRechazoRecibido(rechazo, sender);
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
    public void onEliminacionRecibida(EliminacionMensaje elim, SocketClient sender) {
        for (ChatEventListener listener : listeners) {
            listener.onEliminacionRecibida(elim, sender);
        }
    }

    @Override
    public void onZumbidoRecibido(Zumbido zumbido, SocketClient sender) {
        for (ChatEventListener listener : listeners) {
            listener.onZumbidoRecibido(zumbido, sender);
        }
    }

    @Override
    public void onFijarMensajeRecibido(FijarMensaje fijar, SocketClient sender) {
        for (ChatEventListener listener : listeners) {
            listener.onFijarMensajeRecibido(fijar, sender);
        }
    }

    @Override
    public void onMensajeUnicoRecibido(edu.upb.chatupb_v2.model.network.message.MensajeUnico msg, SocketClient sender) {
        for (ChatEventListener listener : listeners) {
            listener.onMensajeUnicoRecibido(msg, sender);
        }
    }

    @Override
    public void onCambioTemaRecibido(CambioTema cambio, SocketClient sender) {
        for (ChatEventListener listener : listeners) {
            listener.onCambioTemaRecibido(cambio, sender);
        }
    }
}
