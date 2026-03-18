package edu.upb.chatupb_v2.controller;

import edu.upb.chatupb_v2.model.network.message.*;
import edu.upb.chatupb_v2.model.network.ChatEventListener;
import edu.upb.chatupb_v2.model.network.Mediador;
import edu.upb.chatupb_v2.model.network.SocketClient;
import edu.upb.chatupb_v2.controller.exception.OperationException;
import edu.upb.chatupb_v2.model.entities.ChatMessage;
import edu.upb.chatupb_v2.model.entities.User;
import edu.upb.chatupb_v2.model.repository.UserDao;
import edu.upb.chatupb_v2.view.ChatMessageInfo;
import edu.upb.chatupb_v2.view.ContactInfo;
import edu.upb.chatupb_v2.view.IChatView;

import javax.swing.SwingUtilities;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ChatController implements ChatEventListener {

    private final IChatView view;
    private final ContactController contactController;
    private final MessageController messageController;
    private final UserDao userDao;
    private User currentUser;

    private final HashMap<String, String> nombresConectados = new HashMap<>();
    private final HashMap<String, String> codigosConectados = new HashMap<>();

    public ChatController(IChatView view) {
        this.view = view;
        this.userDao = new UserDao();
        this.contactController = new ContactController(view);
        this.messageController = new MessageController();
    }

    public void onAppStart() {
        try {
            List<User> users = userDao.findAll();
            view.setUserList(users);
            if (!users.isEmpty()) {
                cambiarUsuario(users.get(0));
            } else {
                view.showNewUserDialog();
            }
        } catch (Exception e) {
            e.printStackTrace();
            view.mostrarError("Error fatal al cargar la lista de usuarios.");
        }
    }

    public void cambiarUsuario(User user) {
        if (user == null) return;

        Mediador.getInstancia().cerrarTodasLasConexiones();
        nombresConectados.clear();
        codigosConectados.clear();
        view.limpiarConexionesUI();

        this.currentUser = user;
        contactController.setUsuario(user);
        messageController.setUsuario(user);
        view.setScreenTitle("Chat P2P - " + user.getName());
        view.clearChatHistory();

        iniciarHello();
    }

    public void crearNuevoUsuario() {
        view.showNewUserDialog();
    }

    public void guardarNuevoUsuario(String name) {
        try {
            User newUser = User.builder()
                    .code(UUID.randomUUID().toString())
                    .name(name)
                    .build();
            userDao.save(newUser);
            List<User> users = userDao.findAll();
            view.setUserList(users);
            cambiarUsuario(newUser);
        } catch (Exception e) {
            e.printStackTrace();
            view.mostrarError("No se pudo guardar el nuevo usuario.");
        }
    }

    public boolean isConectado(String ip) {
        return nombresConectados.containsKey(ip);
    }

    public String getNombreConectado(String ip) {
        return nombresConectados.getOrDefault(ip, ip);
    }

    public void enviarInvitacion(String ip, String miNombre) {
        if (currentUser == null) return;
        if (contactController.existeContactoPorIp(ip)) {
            view.mostrarError("Este contacto ya esta guardado. Haz doble click sobre el para chatear.");
            return;
        }
        if (Mediador.getInstancia().existe(ip)) {
            view.mostrarError("Ya existe una conexion activa con " + ip);
            return;
        }
        try {
            Mediador.getInstancia().invitacion(ip, currentUser.getCode(), miNombre);
            view.actualizarEstadoInvitacion(ip);
            view.appendChat("-> Invitacion (001) enviada a " + ip + "\n");
        } catch (OperationException ex) {
            view.mostrarError(ex.getMessage());
        }
    }

    public void enviarMensaje(String ip, String mensaje) {
        if (currentUser == null) return;
        String idMensaje = UUID.randomUUID().toString();
        String timestamp = String.valueOf(System.currentTimeMillis());

        String contactCode = codigosConectados.get(ip);
        if (contactCode == null) {
            contactCode = contactController.buscarCodigoPorIp(ip);
        }

        if (contactCode != null) {
            // Pasamos 'false' porque es un mensaje normal
            messageController.guardarMensajeEnviado(contactCode, mensaje, idMensaje, timestamp, false);
        }

        if (nombresConectados.containsKey(ip) && Mediador.getInstancia().existe(ip)) {
            try {
                EnvioMensaje envio = new EnvioMensaje(currentUser.getCode(), idMensaje, mensaje);
                Mediador.getInstancia().enviarMensaje(ip, envio.generarTrama());
                // Pasamos 'false' en viewOnce
                view.appendMensajeToContact(ip, mensaje, true, idMensaje, false);
            } catch (Exception ex) {
                ex.printStackTrace();
                view.appendChatToContact(ip, "[Error al enviar mensaje]\n");
            }
        } else {
            view.appendChatToContact(ip, "[Pendiente - contacto sin conexion]\n");
        }
        view.limpiarMensaje();
    }

    // --- NUEVA LÓGICA: Enviar Mensaje Único (Trama 012) ---
    public void enviarMensajeUnico(String ip, String mensaje) {
        if (currentUser == null) return;
        String idMensaje = UUID.randomUUID().toString();
        String timestamp = String.valueOf(System.currentTimeMillis());

        String contactCode = codigosConectados.get(ip);
        if (contactCode == null) {
            contactCode = contactController.buscarCodigoPorIp(ip);
        }

        if (contactCode != null) {
            // Guardamos indicando viewOnce = true
            messageController.guardarMensajeEnviado(contactCode, mensaje, idMensaje, timestamp, true);
        }

        if (nombresConectados.containsKey(ip) && Mediador.getInstancia().existe(ip)) {
            try {
                MensajeUnico envio = new MensajeUnico(currentUser.getCode(), idMensaje, mensaje);
                Mediador.getInstancia().enviarMensaje(ip, envio.generarTrama());
                // Mostramos en la UI con viewOnce = true
                view.appendMensajeToContact(ip, mensaje, true, idMensaje, true);
            } catch (Exception ex) {
                ex.printStackTrace();
                view.appendChatToContact(ip, "[Error al enviar mensaje único]\n");
            }
        } else {
            view.appendChatToContact(ip, "[Pendiente - contacto sin conexión]\n");
        }
        view.limpiarMensaje();
    }

    // --- NUEVA LÓGICA: Acción del botón Leído del PopUp ---
    public void abrirMensajeUnico(String ip, String idMensaje) {
        if (currentUser == null) return;

        // 1. Enviar 008 (Confirmación) al remitente para que sepa que lo abrimos
        if (nombresConectados.containsKey(ip) && Mediador.getInstancia().existe(ip)) {
            try {
                ConfirmacionMensaje conf = new ConfirmacionMensaje(idMensaje);
                Mediador.getInstancia().enviarMensaje(ip, conf.generarTrama());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        messageController.marcarConfirmado(idMensaje);

        // 2. Enviar 009 y eliminar localmente (Autodestrucción en BD y UI)
        eliminarMensaje(ip, idMensaje);
    }

    public void abrirChat(ContactInfo contacto) {
        if (currentUser == null) return;

        List<ChatMessage> noConfirmados = messageController.obtenerNoConfirmadosRecibidos(contacto.getCode());
        if (!noConfirmados.isEmpty() && nombresConectados.containsKey(contacto.getIp())
                && Mediador.getInstancia().existe(contacto.getIp())) {
            for (ChatMessage msg : noConfirmados) {
                // No enviamos confirmacion automatica a los view_once pendientes (para evitar que se autodestruyan sin abrir)
                if (!msg.isViewOnce()) {
                    try {
                        ConfirmacionMensaje conf = new ConfirmacionMensaje(msg.getId());
                        Mediador.getInstancia().enviarMensaje(contacto.getIp(), conf.generarTrama());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        messageController.marcarRecibidosComoVistos(contacto.getCode());

        List<ChatMessageInfo> historial = messageController.cargarHistorial(contacto.getCode());
        view.abrirChatConContacto(contacto, historial);

        ChatMessageInfo fijado = messageController.obtenerMensajeFijado(contacto.getCode());
        if (fijado != null) {
            view.mostrarMensajeFijado(contacto.getIp(), fijado);
        } else {
            view.ocultarMensajeFijado(contacto.getIp());
        }
    }

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

    // --- NUEVO LISTENER: Trama 012 Recibida ---
    @Override
    public void onMensajeUnicoRecibido(MensajeUnico msg, SocketClient sender) {
        SwingUtilities.invokeLater(() -> {
            String contactCode = codigosConectados.get(sender.getIp());
            if (contactCode != null) {
                messageController.guardarMensajeRecibido(contactCode, msg.getContenido(), msg.getIdMensaje(), String.valueOf(System.currentTimeMillis()), true);
            }
            // Agrega a la UI con viewOnce = true. NO manda el 008 automáticamente.
            view.appendMensajeToContact(sender.getIp(), msg.getContenido(), false, msg.getIdMensaje(), true);
        });
    }

    @Override
    public void onConfirmacionRecibida(ConfirmacionMensaje conf, SocketClient sender) {
        SwingUtilities.invokeLater(() -> procesarConfirmacion(conf, sender));
    }

    @Override
    public void onEliminacionRecibida(EliminacionMensaje elim, SocketClient sender) {
        SwingUtilities.invokeLater(() -> procesarEliminacion(elim, sender));
    }

    @Override
    public void onZumbidoRecibido(Zumbido zumbido, SocketClient sender) {
        SwingUtilities.invokeLater(() -> procesarZumbido(zumbido, sender));
    }

    @Override
    public void onFijarMensajeRecibido(FijarMensaje fijar, SocketClient sender) {
        SwingUtilities.invokeLater(() -> procesarFijarMensaje(fijar, sender));
    }

    @Override
    public void onCambioTemaRecibido(CambioTema cambio, SocketClient sender) {
        SwingUtilities.invokeLater(() -> {
            view.aplicarTema(sender.getIp(), cambio.getIdTema());
        });
    }

    public void enviarCambioTema(String ip, String idTema) {
        if (currentUser == null) return;
        if (nombresConectados.containsKey(ip) && Mediador.getInstancia().existe(ip)) {
            try {
                CambioTema cambio = new CambioTema(currentUser.getCode(), idTema);
                Mediador.getInstancia().enviarMensaje(ip, cambio.generarTrama());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        view.aplicarTema(ip, idTema);
    }

    private void procesarInvitacionRecibida(Invitacion inv, SocketClient sender) {
        if (currentUser == null) return;
        boolean aceptada = view.mostrarDialogoInvitacion(inv.getNombre(), sender.getIp());
        if (aceptada) {
            try {
                String miNombre = currentUser.getName();
                AceptacionInvitacion acc = new AceptacionInvitacion(currentUser.getCode(), miNombre);
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
        String contactCode = codigosConectados.get(sender.getIp());

        if (contactCode != null) {
            String idMensaje = msg.getIdMensaje();
            String timestamp = String.valueOf(System.currentTimeMillis());
            messageController.guardarMensajeRecibido(contactCode, msg.getContenido(), idMensaje, timestamp, false);
        }

        view.appendMensajeToContact(sender.getIp(), msg.getContenido(), false, msg.getIdMensaje(), false);

        if (sender.getIp().equals(view.getContactoActivo())) {
            try {
                ConfirmacionMensaje conf = new ConfirmacionMensaje(msg.getIdMensaje());
                Mediador.getInstancia().enviarMensaje(sender.getIp(), conf.generarTrama());
                messageController.marcarConfirmado(msg.getIdMensaje());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void procesarConfirmacion(ConfirmacionMensaje conf, SocketClient sender) {
        messageController.marcarConfirmado(conf.getIdMensaje());
        view.actualizarCheckMensaje(sender.getIp(), conf.getIdMensaje());

        // LÓGICA MENSAJE ÚNICO: Si el mensaje recién confirmado era view_once (012),
        // disparamos la eliminación lógica y mandamos 009 de vuelta al receptor.
        ChatMessage msg = messageController.obtenerMensajePorId(conf.getIdMensaje());
        if (msg != null && msg.isViewOnce()) {
            eliminarMensaje(sender.getIp(), conf.getIdMensaje());
        }
    }

    private void procesarEliminacion(EliminacionMensaje elim, SocketClient sender) {
        String idMensaje = elim.getIdMensaje();
        messageController.eliminarContenidoMensaje(idMensaje);
        view.actualizarBurbujaMensajeEliminado(sender.getIp(), idMensaje);
    }

    public void eliminarMensaje(String ip, String idMensaje) {
        if (currentUser == null) return;
        messageController.eliminarContenidoMensaje(idMensaje);
        if (nombresConectados.containsKey(ip) && Mediador.getInstancia().existe(ip)) {
            try {
                EliminacionMensaje elim = new EliminacionMensaje(idMensaje);
                Mediador.getInstancia().enviarMensaje(ip, elim.generarTrama());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        view.actualizarBurbujaMensajeEliminado(ip, idMensaje);
    }

    public void fijarMensaje(String ip, String idMensaje) {
        if (currentUser == null) return;

        String contactCode = codigosConectados.get(ip);
        if (contactCode == null) {
            contactCode = contactController.buscarCodigoPorIp(ip);
        }

        ChatMessageInfo anteriorFijado = null;
        if (contactCode != null) {
            anteriorFijado = messageController.obtenerMensajeFijado(contactCode);
        }

        if (contactCode != null) {
            messageController.fijarMensaje(idMensaje, contactCode);
        }

        if (anteriorFijado != null) {
            view.desmarcarBurbujaFijada(ip, anteriorFijado.getId());
        }

        view.marcarBurbujaFijada(ip, idMensaje);

        ChatMessageInfo nuevoFijado = (contactCode != null) ? messageController.obtenerMensajeFijado(contactCode) : null;
        if (nuevoFijado != null) {
            view.mostrarMensajeFijado(ip, nuevoFijado);
        }

        if (nombresConectados.containsKey(ip) && Mediador.getInstancia().existe(ip)) {
            try {
                FijarMensaje fijar = new FijarMensaje(idMensaje);
                Mediador.getInstancia().enviarMensaje(ip, fijar.generarTrama());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void desfijarMensaje(String ip, String idMensaje) {
        if (currentUser == null) return;
        messageController.desfijarMensaje(idMensaje);
        view.desmarcarBurbujaFijada(ip, idMensaje);
        view.ocultarMensajeFijado(ip);
    }

    private void procesarFijarMensaje(FijarMensaje fijar, SocketClient sender) {
        String idMensaje = fijar.getIdMensaje();
        String contactCode = codigosConectados.get(sender.getIp());

        if (contactCode != null) {
            ChatMessageInfo anteriorFijado = messageController.obtenerMensajeFijado(contactCode);
            messageController.fijarMensaje(idMensaje, contactCode);

            if (anteriorFijado != null) {
                view.desmarcarBurbujaFijada(sender.getIp(), anteriorFijado.getId());
            }

            view.marcarBurbujaFijada(sender.getIp(), idMensaje);

            ChatMessageInfo nuevoFijado = messageController.obtenerMensajeFijado(contactCode);
            if (nuevoFijado != null) {
                view.mostrarMensajeFijado(sender.getIp(), nuevoFijado);
            }
        }
    }

    private void procesarZumbido(Zumbido zumbido, SocketClient sender) {
        String nombre = nombresConectados.getOrDefault(sender.getIp(), sender.getIp());
        view.mostrarZumbido(sender.getIp(), nombre);
    }

    public void enviarZumbido(String ip) {
        if (currentUser == null) return;
        if (nombresConectados.containsKey(ip) && Mediador.getInstancia().existe(ip)) {
            try {
                Zumbido zumbido = new Zumbido(currentUser.getCode());
                Mediador.getInstancia().enviarMensaje(ip, zumbido.generarTrama());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String miNombre = view.getMiNombre();
        view.mostrarZumbido(ip, miNombre != null ? miNombre : "Tu");
    }

    public void iniciarHello() {
        if (currentUser == null) return;
        List<ContactInfo> contactos = contactController.getContactos();
        for (ContactInfo c : contactos) {
            new Thread(() -> {
                try {
                    Mediador.getInstancia().enviarHello(c.getIp(), currentUser.getCode());
                    System.out.println("[Hello] Enviado a " + c.getName() + " (" + c.getIp() + ")");
                } catch (OperationException e) {
                    System.out.println("[Hello] " + c.getName() + " (" + c.getIp() + ") no esta en linea.");
                }
            }, "Hello-" + c.getIp()).start();
        }
    }

    private void procesarHello(Hello hello, SocketClient sender) {
        if (currentUser == null) return;
        String nombre = contactController.buscarNombrePorCodigo(hello.getIdUsuario());
        if (nombre != null) {
            try {
                HelloResponse response = new HelloResponse(currentUser.getCode());
                Mediador.getInstancia().enviarMensaje(sender.getIp(), response.generarTrama());
            } catch (Exception e) {
                e.printStackTrace();
            }
            codigosConectados.put(sender.getIp(), hello.getIdUsuario());
            agregarConexion(sender.getIp(), nombre);
            view.appendChat("[Hello] " + nombre + " (" + sender.getIp() + ") esta en linea.\n");
        } else {
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
}