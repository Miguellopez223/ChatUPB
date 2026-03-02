package edu.upb.chatupb_v2.controller;

import edu.upb.chatupb_v2.bl.message.AceptacionInvitacion;
import edu.upb.chatupb_v2.bl.message.ConfirmacionMensaje;
import edu.upb.chatupb_v2.bl.message.EnvioMensaje;
import edu.upb.chatupb_v2.bl.message.Invitacion;
import edu.upb.chatupb_v2.bl.message.RechazoInvitacion;
import edu.upb.chatupb_v2.bl.server.ChatEventListener;
import edu.upb.chatupb_v2.bl.server.Mediador;
import edu.upb.chatupb_v2.bl.server.SocketClient;
import edu.upb.chatupb_v2.controller.exception.OperationException;
import edu.upb.chatupb_v2.repository.Contact;
import edu.upb.chatupb_v2.view.IChatView;

import javax.swing.SwingUtilities;
import java.util.HashMap;

/**
 * Controller de chat. Implementa ChatEventListener para suscribirse al Mediador.
 * El Mediador no baja al controller: el controller sube al Mediador registrandose
 * como listener. Asi la dependencia va de controller -> BL, nunca al reves.
 *
 * El manejo de SwingUtilities.invokeLater se hace aqui (en la capa del controller),
 * no en la BL.
 */
public class ChatController implements ChatEventListener {

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

    // --- Acciones iniciadas por el usuario (desde la UI) ---

    public void enviarInvitacion(String ip, String miNombre) {
        if (Mediador.getInstancia().existe(ip)) {
            view.mostrarError("Ya existe una conexión activa con " + ip);
            return;
        }
        try {
            Mediador.getInstancia().invitacion(ip, Contact.ME_CODE, miNombre);
            view.actualizarEstadoInvitacion(ip);
            view.appendChat("-> Invitación (001) enviada a " + ip + "\n");
        } catch (OperationException ex) {
            view.mostrarError(ex.getMessage());
        }
    }

    public void enviarMensaje(String ip, String mensaje) {
        try {
            String idMensaje = String.valueOf(System.currentTimeMillis());
            EnvioMensaje envio = new EnvioMensaje(Contact.ME_CODE, idMensaje, mensaje);
            Mediador.getInstancia().enviarMensaje(ip, envio.generarTrama());

            String nombre = getNombreConectado(ip);
            view.appendChat("Yo -> " + nombre + ": " + mensaje + "\n");
            view.limpiarMensaje();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // --- ChatEventListener: eventos reenviados por el Mediador ---
    // Se envuelven en SwingUtilities.invokeLater porque vienen del hilo del socket.

    @Override
    public void onInvitacionRecibida(Invitacion inv, SocketClient sender) {
        SwingUtilities.invokeLater(() -> procesarInvitacionRecibida(inv, sender));
    }

    @Override
    public void onAceptacionRecibida(AceptacionInvitacion acc, SocketClient sender) {
        SwingUtilities.invokeLater(() -> procesarAceptacion(acc, sender));
    }

    @Override
    public void onRechazoRecibido(RechazoInvitacion rechazo, SocketClient sender) {
        SwingUtilities.invokeLater(() -> procesarRechazo(rechazo, sender));
    }

    @Override
    public void onMensajeRecibido(EnvioMensaje msg, SocketClient sender) {
        SwingUtilities.invokeLater(() -> procesarMensajeRecibido(msg, sender));
    }

    @Override
    public void onConfirmacionRecibida(ConfirmacionMensaje conf, SocketClient sender) {
        SwingUtilities.invokeLater(() -> procesarConfirmacion(conf, sender));
    }

    // --- Procesamiento de eventos (ejecutado en el hilo de Swing) ---

    private void procesarInvitacionRecibida(Invitacion inv, SocketClient sender) {
        boolean aceptada = view.mostrarDialogoInvitacion(inv.getNombre(), sender.getIp());
        if (aceptada) {
            try {
                String miNombre = view.getMiNombre();
                AceptacionInvitacion acc = new AceptacionInvitacion(Contact.ME_CODE, miNombre);
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

    private void procesarAceptacion(AceptacionInvitacion acc, SocketClient sender) {
        agregarConexion(sender.getIp(), acc.getNombre());
        view.appendChat("<- " + acc.getNombre() + " (" + sender.getIp() + ") aceptó tu invitación (002). ¡Ya pueden hablar!\n");

        contactController.guardarContactoSiNoExiste(acc.getIdUsuario(), acc.getNombre(), sender.getIp());
    }

    private void procesarRechazo(RechazoInvitacion rechazo, SocketClient sender) {
        Mediador.getInstancia().eliminar(sender.getIp());
        view.appendChat("<- " + sender.getIp() + " rechazó tu invitación (003).\n");
        view.actualizarEstado(nombresConectados.size());
        view.refrescarEstadoContactos();
    }

    private void procesarMensajeRecibido(EnvioMensaje msg, SocketClient sender) {
        String nombre = getNombreConectado(sender.getIp());
        view.appendChat(nombre + ": " + msg.getContenido() + "\n");

        try {
            ConfirmacionMensaje conf = new ConfirmacionMensaje(msg.getIdMensaje());
            Mediador.getInstancia().enviarMensaje(sender.getIp(), conf.generarTrama());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void procesarConfirmacion(ConfirmacionMensaje conf, SocketClient sender) {
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
