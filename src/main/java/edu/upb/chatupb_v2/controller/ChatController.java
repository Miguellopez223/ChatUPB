package edu.upb.chatupb_v2.controller;

import edu.upb.chatupb_v2.bl.message.AceptacionInvitacion;
import edu.upb.chatupb_v2.bl.message.ConfirmacionMensaje;
import edu.upb.chatupb_v2.bl.message.EnvioMensaje;
import edu.upb.chatupb_v2.bl.message.Invitacion;
import edu.upb.chatupb_v2.bl.message.RechazoInvitacion;
import edu.upb.chatupb_v2.bl.server.ChatEventListener;
import edu.upb.chatupb_v2.bl.server.Mediador;
import edu.upb.chatupb_v2.bl.server.SocketClient;
import edu.upb.chatupb_v2.view.IChatView;

import java.util.HashMap;

public class ChatController {

    private final IChatView view;
    private final ContactController contactController;
    private final HashMap<String, String> nombresConectados = new HashMap<>();

    public ChatController(IChatView view, ContactController contactController) {
        this.view = view;
        this.contactController = contactController;
    }

    public boolean isConectado(String ip) {
        return nombresConectados.containsKey(ip);
    }

    public String getNombreConectado(String ip) {
        return nombresConectados.getOrDefault(ip, ip);
    }

    public void enviarInvitacion(String ip, String miNombre, ChatEventListener listener) {
        if (Mediador.getInstancia().existe(ip)) {
            view.mostrarError("Ya existe una conexión activa con " + ip);
            return;
        }
        try {
            SocketClient cliente = new SocketClient(ip);
            cliente.addChatEventListener(listener);
            Mediador.getInstancia().registrar(cliente);
            cliente.start();

            Invitacion inv = new Invitacion("ID_MIGUEL", miNombre);
            Mediador.getInstancia().enviarMensaje(ip, inv.generarTrama());

            view.actualizarEstadoInvitacion(ip);
            view.appendChat("-> Invitación (001) enviada a " + ip + "\n");
        } catch (Exception ex) {
            view.mostrarError("Error al conectar a la IP: " + ex.getMessage());
        }
    }

    public void enviarMensaje(String ip, String mensaje) {
        try {
            String idMensaje = String.valueOf(System.currentTimeMillis());
            EnvioMensaje envio = new EnvioMensaje("ID_MIGUEL", idMensaje, mensaje);
            Mediador.getInstancia().enviarMensaje(ip, envio.generarTrama());

            String nombre = getNombreConectado(ip);
            view.appendChat("Yo -> " + nombre + ": " + mensaje + "\n");
            view.limpiarMensaje();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void procesarInvitacionRecibida(Invitacion inv, SocketClient sender) {
        boolean aceptada = view.mostrarDialogoInvitacion(inv.getNombre(), sender.getIp());
        if (aceptada) {
            try {
                String miNombre = view.getMiNombre();
                AceptacionInvitacion acc = new AceptacionInvitacion("ID_MIGUEL", miNombre);
                Mediador.getInstancia().enviarMensaje(sender.getIp(), acc.generarTrama());

                agregarConexion(sender.getIp(), inv.getNombre());
                view.appendChat("<- Has aceptado la invitación de " + inv.getNombre() + " (" + sender.getIp() + ")\n");

                contactController.guardarContactoSiNoExiste(inv.getIdUsuario(), inv.getNombre(), sender.getIp());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                RechazoInvitacion rechazo = new RechazoInvitacion();
                Mediador.getInstancia().enviarMensaje(sender.getIp(), rechazo.generarTrama());
            } catch (Exception e) {
                e.printStackTrace();
            }
            Mediador.getInstancia().eliminar(sender.getIp());
            view.appendChat("- Rechazaste la invitación de " + inv.getNombre() + "\n");
        }
    }

    public void procesarAceptacion(AceptacionInvitacion acc, SocketClient sender) {
        agregarConexion(sender.getIp(), acc.getNombre());
        view.appendChat("<- " + acc.getNombre() + " (" + sender.getIp() + ") aceptó tu invitación (002). ¡Ya pueden hablar!\n");

        contactController.guardarContactoSiNoExiste(acc.getIdUsuario(), acc.getNombre(), sender.getIp());
    }

    public void procesarRechazo(RechazoInvitacion rechazo, SocketClient sender) {
        Mediador.getInstancia().eliminar(sender.getIp());
        view.appendChat("<- " + sender.getIp() + " rechazó tu invitación (003).\n");
        view.actualizarEstado(nombresConectados.size());
        view.refrescarEstadoContactos();
    }

    public void procesarMensajeRecibido(EnvioMensaje msg, SocketClient sender) {
        String nombre = getNombreConectado(sender.getIp());
        view.appendChat(nombre + ": " + msg.getContenido() + "\n");

        try {
            ConfirmacionMensaje conf = new ConfirmacionMensaje(msg.getIdMensaje());
            Mediador.getInstancia().enviarMensaje(sender.getIp(), conf.generarTrama());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void procesarConfirmacion(ConfirmacionMensaje conf, SocketClient sender) {
        String nombre = getNombreConectado(sender.getIp());
        view.appendChat("  [✓ Mensaje " + conf.getIdMensaje() + " recibido por " + nombre + "]\n");
    }

    private void agregarConexion(String ip, String nombre) {
        nombresConectados.put(ip, nombre);
        view.agregarConexionUI(ip, nombre);
        view.actualizarEstado(nombresConectados.size());
        view.refrescarEstadoContactos();
    }
}
