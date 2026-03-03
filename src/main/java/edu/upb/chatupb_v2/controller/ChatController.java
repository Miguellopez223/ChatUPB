package edu.upb.chatupb_v2.controller;

import edu.upb.chatupb_v2.bl.message.AceptacionInvitacion;
import edu.upb.chatupb_v2.bl.message.ConfirmacionMensaje;
import edu.upb.chatupb_v2.bl.message.EnvioMensaje;
import edu.upb.chatupb_v2.bl.message.Hello;
import edu.upb.chatupb_v2.bl.message.HelloRechazo;
import edu.upb.chatupb_v2.bl.message.HelloResponse;
import edu.upb.chatupb_v2.bl.message.Invitacion;
import edu.upb.chatupb_v2.bl.message.RechazoInvitacion;
import edu.upb.chatupb_v2.bl.server.ChatEventListener;
import edu.upb.chatupb_v2.bl.server.Mediador;
import edu.upb.chatupb_v2.bl.server.SocketClient;
import edu.upb.chatupb_v2.controller.exception.OperationException;
import edu.upb.chatupb_v2.repository.Contact;
import edu.upb.chatupb_v2.view.ContactInfo;
import edu.upb.chatupb_v2.view.IChatView;

import javax.swing.SwingUtilities;
import java.util.HashMap;
import java.util.List;

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
    public void onHelloRecibido(Hello hello, SocketClient sender) {
        SwingUtilities.invokeLater(() -> procesarHello(hello, sender));
    }

    @Override
    public void onHelloResponseRecibido(HelloResponse response, SocketClient sender) {
        SwingUtilities.invokeLater(() -> procesarHelloResponse(response, sender));
    }

    @Override
    public void onHelloRechazoRecibido(HelloRechazo rechazo, SocketClient sender) {
        SwingUtilities.invokeLater(() -> procesarHelloRechazo(sender));
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

    // --- Hello (004) y HelloResponse (005) ---

    /**
     * Envia Hello (004) a todos los contactos guardados en la BD.
     * Se ejecuta al iniciar la aplicacion, en un hilo aparte para no bloquear la UI.
     * Si un contacto no esta en linea, se omite silenciosamente.
     */
    public void iniciarHello() {
        List<ContactInfo> contactos = contactController.getContactos();
        for (ContactInfo c : contactos) {
            new Thread(() -> {
                try {
                    Mediador.getInstancia().enviarHello(c.getIp(), Contact.ME_CODE);
                    System.out.println("[Hello] Enviado a " + c.getName() + " (" + c.getIp() + ")");
                } catch (OperationException e) {
                    System.out.println("[Hello] " + c.getName() + " (" + c.getIp() + ") no está en línea.");
                }
            }, "Hello-" + c.getIp()).start();
        }
    }

    private void procesarHello(Hello hello, SocketClient sender) {
        String nombre = contactController.buscarNombrePorCodigo(hello.getIdUsuario());
        if (nombre != null) {
            // Contacto conocido: responder con 005 y marcar como conectado
            try {
                HelloResponse response = new HelloResponse(Contact.ME_CODE);
                Mediador.getInstancia().enviarMensaje(sender.getIp(), response.generarTrama());
            } catch (Exception e) {
                e.printStackTrace();
            }
            agregarConexion(sender.getIp(), nombre);
            view.appendChat("[Hello] " + nombre + " (" + sender.getIp() + ") está en línea.\n");
        } else {
            // ID no existe en mi BD: enviar rechazo 006 y eliminar del mapa
            try {
                HelloRechazo rechazo = new HelloRechazo();
                Mediador.getInstancia().enviarMensaje(sender.getIp(), rechazo.generarTrama());
            } catch (Exception e) {
                e.printStackTrace();
            }
            Mediador.getInstancia().eliminar(sender.getIp());
            System.out.println("[Hello] Rechazo (006) enviado a " + sender.getIp() + " - ID desconocido: " + hello.getIdUsuario());
        }
    }

    private void procesarHelloRechazo(SocketClient sender) {
        Mediador.getInstancia().eliminar(sender.getIp());
        System.out.println("[HelloRechazo] " + sender.getIp() + " rechazó nuestro Hello (006).");
    }

    private void procesarHelloResponse(HelloResponse response, SocketClient sender) {
        String nombre = contactController.buscarNombrePorCodigo(response.getIdUsuario());
        if (nombre != null) {
            agregarConexion(sender.getIp(), nombre);
            view.appendChat("[Hello] " + nombre + " (" + sender.getIp() + ") respondió. Conectado.\n");
        } else {
            System.out.println("[HelloResponse] Respuesta de usuario desconocido: " + response.getIdUsuario());
        }
    }

    private void agregarConexion(String ip, String nombre) {
        nombresConectados.put(ip, nombre);
        view.agregarConexionUI(ip, nombre);
        view.actualizarEstado(nombresConectados.size());
        view.refrescarEstadoContactos();
    }
}
