package edu.upb.chatupb_v2.bl.server;

import edu.upb.chatupb_v2.bl.message.AceptacionInvitacion;
import edu.upb.chatupb_v2.bl.message.ConfirmacionMensaje;
import edu.upb.chatupb_v2.bl.message.EnvioMensaje;
import edu.upb.chatupb_v2.bl.message.Invitacion;
import edu.upb.chatupb_v2.bl.message.RechazoInvitacion;
import edu.upb.chatupb_v2.controller.ChatController;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.HashMap;

/**
 * Mediador central de conexiones.
 * Patron Singleton: solo existe una instancia en toda la aplicacion.
 * Patron Mediador: centraliza el registro y busqueda de SocketClients por IP.
 * Implementa ChatEventListener para escuchar los eventos de los sockets
 * y delegarlos al ChatController.
 *
 * La clave del HashMap es la IP del usuario y el valor es su instancia de SocketClient.
 *
 */
public class Mediador implements ChatEventListener {

    // --- Singleton ---
    private static Mediador instancia;
    private ChatController chatController;

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

    /**
     * Registra un SocketClient asociado a su IP.
     * Si ya existia una conexion con esa IP, la reemplaza.
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

    // --- ChatController para delegar eventos ---

    public void setChatController(ChatController chatController) {
        this.chatController = chatController;
    }

    // --- ChatEventListener: escucha eventos del socket y delega al controller ---

    @Override
    public void onInvitacionRecibida(Invitacion inv, SocketClient sender) {
        SwingUtilities.invokeLater(() -> chatController.procesarInvitacionRecibida(inv, sender));
    }

    @Override
    public void onAceptacionRecibida(AceptacionInvitacion acc, SocketClient sender) {
        SwingUtilities.invokeLater(() -> chatController.procesarAceptacion(acc, sender));
    }

    @Override
    public void onRechazoRecibido(RechazoInvitacion rechazo, SocketClient sender) {
        SwingUtilities.invokeLater(() -> chatController.procesarRechazo(rechazo, sender));
    }

    @Override
    public void onMensajeRecibido(EnvioMensaje msg, SocketClient sender) {
        SwingUtilities.invokeLater(() -> chatController.procesarMensajeRecibido(msg, sender));
    }

    @Override
    public void onConfirmacionRecibida(ConfirmacionMensaje conf, SocketClient sender) {
        SwingUtilities.invokeLater(() -> chatController.procesarConfirmacion(conf, sender));
    }
}
