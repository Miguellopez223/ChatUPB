package edu.upb.chatupb_v2.controller;

import edu.upb.chatupb_v2.model.network.message.AceptacionInvitacion;
import edu.upb.chatupb_v2.model.network.message.ConfirmacionMensaje;
import edu.upb.chatupb_v2.model.network.message.EnvioMensaje;
import edu.upb.chatupb_v2.model.network.message.Hello;
import edu.upb.chatupb_v2.model.network.message.HelloRechazo;
import edu.upb.chatupb_v2.model.network.message.HelloResponse;
import edu.upb.chatupb_v2.model.network.message.Invitacion;
import edu.upb.chatupb_v2.model.network.message.RechazoInvitacion;
import edu.upb.chatupb_v2.model.network.message.CompartirContacto;
import edu.upb.chatupb_v2.model.network.ChatEventListener;
import edu.upb.chatupb_v2.model.network.Mediador;
import edu.upb.chatupb_v2.model.network.SocketClient;
import edu.upb.chatupb_v2.controller.exception.OperationException;
import edu.upb.chatupb_v2.model.entities.Contact;
import edu.upb.chatupb_v2.view.ChatMessageInfo;
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
    private final MessageController messageController;
    private final AnalizadorController analizadorController; // PREGUNTA 4
    private final HashMap<String, String> nombresConectados = new HashMap<>();
    // Mapa IP -> codigo UUID del contacto (para persistir mensajes por codigo)
    private final HashMap<String, String> codigosConectados = new HashMap<>();

    public ChatController(IChatView view, ContactController contactController) {
        this.view = view;
        this.contactController = contactController;
        this.messageController = new MessageController();
        this.analizadorController = new AnalizadorController(); // PREGUNTA 4
    }

    public boolean isConectado(String ip) {
        return nombresConectados.containsKey(ip);
    }

    public String getNombreConectado(String ip) {
        return nombresConectados.getOrDefault(ip, ip);
    }

    // --- Acciones iniciadas por el usuario (desde la UI) ---

    public void enviarInvitacion(String ip, String miNombre) {
        // Si el contacto ya esta guardado, no necesita invitacion
        if (contactController.existeContactoPorIp(ip)) {
            view.mostrarError("Este contacto ya esta guardado. Haz doble click sobre el para chatear.");
            return;
        }
        if (Mediador.getInstancia().existe(ip)) {
            view.mostrarError("Ya existe una conexion activa con " + ip);
            return;
        }
        try {
            Mediador.getInstancia().invitacion(ip, Contact.ME_CODE, miNombre);
            view.actualizarEstadoInvitacion(ip);
            view.appendChat("-> Invitacion (001) enviada a " + ip + "\n");
        } catch (OperationException ex) {
            view.mostrarError(ex.getMessage());
        }
    }

    public void enviarMensaje(String ip, String mensaje) {
        // PREGUNTA 4: Analizar texto: resolver sumas y ofuscar palabras vulgares
        mensaje = analizadorController.procesarTexto(mensaje);

        long timestamp = System.currentTimeMillis();
        String idMensaje = String.valueOf(timestamp);

        // Resolver codigo del contacto para guardar en BD
        String contactCode = codigosConectados.get(ip);
        if (contactCode == null) {
            contactCode = contactController.buscarCodigoPorIp(ip);
        }

        // Guardar en BD siempre (online u offline)
        if (contactCode != null) {
            messageController.guardarMensajeEnviado(contactCode, mensaje, timestamp);
        }

        // Intentar enviar por red si esta conectado
        if (nombresConectados.containsKey(ip) && Mediador.getInstancia().existe(ip)) {
            try {
                EnvioMensaje envio = new EnvioMensaje(Contact.ME_CODE, idMensaje, mensaje);
                Mediador.getInstancia().enviarMensaje(ip, envio.generarTrama());
                view.appendChatToContact(ip, "Yo: " + mensaje + "\n");
            } catch (Exception ex) {
                ex.printStackTrace();
                view.appendChatToContact(ip, "Yo: " + mensaje + " [error al enviar]\n");
            }
        } else {
            // Contacto offline: mensaje guardado localmente
            view.appendChatToContact(ip, "Yo: " + mensaje + " [pendiente - contacto sin conexion]\n");
        }
        view.limpiarMensaje();
    }

    /**
     * Abre el chat dedicado con un contacto, cargando su historial desde la BD.
     * Llamado cuando el usuario hace doble click en la tabla de contactos.
     */
    public void abrirChat(ContactInfo contacto) {
        List<ChatMessageInfo> historial = messageController.cargarHistorial(contacto.getCode());
        view.abrirChatConContacto(contacto, historial);
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


    // PREGUNTA 5
    @Override
    public void onContactoCompartidoRecibido(CompartirContacto contacto, SocketClient sender) {
        SwingUtilities.invokeLater(() -> procesarContactoCompartido(contacto, sender));
    }

    // --- Procesamiento de eventos (ejecutado en el hilo de Swing) ---

    private void procesarInvitacionRecibida(Invitacion inv, SocketClient sender) {
        boolean aceptada = view.mostrarDialogoInvitacion(inv.getNombre(), sender.getIp());
        if (aceptada) {
            try {
                String miNombre = view.getMiNombre();
                AceptacionInvitacion acc = new AceptacionInvitacion(Contact.ME_CODE, miNombre);
                Mediador.getInstancia().enviarMensaje(sender.getIp(), acc.generarTrama());

                codigosConectados.put(sender.getIp(), inv.getIdUsuario());
                agregarConexion(sender.getIp(), inv.getNombre());
                view.appendChat("<- Has aceptado la invitacion de " + inv.getNombre() + " (" + sender.getIp() + ")\n");

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
            view.appendChat("- Rechazaste la invitacion de " + inv.getNombre() + "\n");
        }
    }

    private void procesarAceptacion(AceptacionInvitacion acc, SocketClient sender) {
        codigosConectados.put(sender.getIp(), acc.getIdUsuario());
        agregarConexion(sender.getIp(), acc.getNombre());
        view.appendChat("<- " + acc.getNombre() + " (" + sender.getIp() + ") acepto tu invitacion (002).\n");

        contactController.guardarContactoSiNoExiste(acc.getIdUsuario(), acc.getNombre(), sender.getIp());
    }

    private void procesarRechazo(RechazoInvitacion rechazo, SocketClient sender) {
        Mediador.getInstancia().eliminar(sender.getIp());
        view.appendChat("<- " + sender.getIp() + " rechazo tu invitacion (003).\n");
        view.actualizarEstado(nombresConectados.size());
        view.refrescarEstadoContactos();
    }

    private void procesarMensajeRecibido(EnvioMensaje msg, SocketClient sender) {
        String nombre = getNombreConectado(sender.getIp());
        String contactCode = codigosConectados.get(sender.getIp());

        //  PREGUNTA 4: Analizar texto recibido: resolver sumas y ofuscar palabras vulgares
        String contenidoProcesado = analizadorController.procesarTexto(msg.getContenido());

        // Guardar mensaje recibido en BD
        if (contactCode != null) {
            long timestamp;
            try {
                timestamp = Long.parseLong(msg.getIdMensaje());
            } catch (NumberFormatException e) {
                timestamp = System.currentTimeMillis();
            }
            messageController.guardarMensajeRecibido(contactCode, contenidoProcesado, timestamp); //PREGUNTA 4
        }

        view.appendChatToContact(sender.getIp(), nombre + ": " + contenidoProcesado + "\n"); //PREGUNTA 4

        try {
            ConfirmacionMensaje conf = new ConfirmacionMensaje(msg.getIdMensaje());
            Mediador.getInstancia().enviarMensaje(sender.getIp(), conf.generarTrama());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void procesarConfirmacion(ConfirmacionMensaje conf, SocketClient sender) {
        String nombre = getNombreConectado(sender.getIp());
        messageController.marcarConfirmado(conf.getIdMensaje());
        view.appendChatToContact(sender.getIp(), "  [Mensaje confirmado por " + nombre + "]\n");
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
                    System.out.println("[Hello] " + c.getName() + " (" + c.getIp() + ") no esta en linea.");
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
            codigosConectados.put(sender.getIp(), hello.getIdUsuario());
            agregarConexion(sender.getIp(), nombre);
            view.appendChat("[Hello] " + nombre + " (" + sender.getIp() + ") esta en linea.\n");
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
        System.out.println("[HelloRechazo] " + sender.getIp() + " rechazo nuestro Hello (006).");
    }

    private void procesarHelloResponse(HelloResponse response, SocketClient sender) {
        String nombre = contactController.buscarNombrePorCodigo(response.getIdUsuario());
        if (nombre != null) {
            codigosConectados.put(sender.getIp(), response.getIdUsuario());
            agregarConexion(sender.getIp(), nombre);
            view.appendChat("[Hello] " + nombre + " (" + sender.getIp() + ") respondio. Conectado.\n");
        } else {
            System.out.println("[HelloResponse] Respuesta de usuario desconocido: " + response.getIdUsuario());
        }
    }

    private void agregarConexion(String ip, String nombre) {
        nombresConectados.put(ip, nombre);

        // Poblar codigosConectados si aun no esta
        if (!codigosConectados.containsKey(ip)) {
            String code = contactController.buscarCodigoPorIp(ip);
            if (code != null) {
                codigosConectados.put(ip, code);
            }
        }

        view.agregarConexionUI(ip, nombre);
        view.actualizarEstado(nombresConectados.size());
        view.refrescarEstadoContactos();
    }

    // PREGUNTA 5
    // --- Compartir contacto (020) ---

    //Envia un contacto a un amigo conectado.
     //Trama: 020|ID_USUARIO|NOMBRE|IP
    public void enviarContacto(String ipDestino, String idContacto, String nombreContacto, String ipContacto) {
        if (nombresConectados.containsKey(ipDestino) && Mediador.getInstancia().existe(ipDestino)) {
            try {
                CompartirContacto compartir = new CompartirContacto(idContacto, nombreContacto, ipContacto);
                Mediador.getInstancia().enviarMensaje(ipDestino, compartir.generarTrama());
                view.appendChat("-> Contacto " + nombreContacto + " compartido con " + getNombreConectado(ipDestino) + "\n");
            } catch (Exception e) {
                e.printStackTrace();
                view.mostrarError("Error al compartir el contacto");
            }
        } else {
            view.mostrarError("El destinatario no esta conectado");
        }
    }

    // Al recibir un contacto compartido (020), solo se guarda en la base de datos.
    private void procesarContactoCompartido(CompartirContacto contacto, SocketClient sender) {
        contactController.guardarContactoSiNoExiste(contacto.getIdUsuario(), contacto.getNombre(), contacto.getIp());
        System.out.println("[020] Contacto recibido y guardado: " + contacto.getNombre() + " (" + contacto.getIp() + ")");
    }
}
