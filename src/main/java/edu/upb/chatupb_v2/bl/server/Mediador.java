package edu.upb.chatupb_v2.bl.server;

import java.util.HashMap;

/**
 * Mediador central de conexiones.
 * Patron Singleton: solo existe una instancia en toda la aplicacion.
 * Patron Mediador: centraliza el registro y busqueda de SocketClients por IP.
 *
 * La clave del HashMap es la IP del usuario y el valor es su instancia de SocketClient.
 *
 * @author rlaredo
 */
public class Mediador {

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
}
