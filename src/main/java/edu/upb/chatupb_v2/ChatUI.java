package edu.upb.chatupb_v2;

import edu.upb.chatupb_v2.bl.message.AceptacionInvitacion;
import edu.upb.chatupb_v2.bl.message.ConfirmacionMensaje;
import edu.upb.chatupb_v2.bl.message.EnvioMensaje;
import edu.upb.chatupb_v2.bl.message.Invitacion;
import edu.upb.chatupb_v2.bl.message.RechazoInvitacion;
import edu.upb.chatupb_v2.bl.server.ChatEventListener;
import edu.upb.chatupb_v2.bl.server.Mediador;
import edu.upb.chatupb_v2.bl.server.SocketClient;
import edu.upb.chatupb_v2.bl.message.Despedida;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashMap;

public class ChatUI extends JFrame implements ChatEventListener {

    // Mapa de IP -> nombre para mostrar en la UI
    private final HashMap<String, String> nombresConectados = new HashMap<>();

    // Componentes visuales
    private JTextField txtIpDestino;
    private JTextField txtMiNombre;
    private JButton btnEnviarInvitacion;
    private JButton btnFueraDeLinea; // Pregunta 5
    private JLabel lblEstado;

    private JTextArea areaChat;
    private JTextField txtMensaje;
    private JButton btnEnviarMensaje;
    private DefaultComboBoxModel<String> modeloDestinatarios;
    private JComboBox<String> comboDestinatarios;

    public ChatUI() {
        configurarVentana();
    }

    private void configurarVentana() {
        setTitle("Chat P2P - Diseño de Patrones UPB");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- 1. PANEL SUPERIOR: Conexión e Invitaciones ---
        JPanel panelConexion = new JPanel(new GridLayout(4, 2, 5, 5));
        panelConexion.setBorder(new TitledBorder("1. Enviar Invitación (Trama 001)"));

        panelConexion.add(new JLabel("Mi Nombre:"));
        txtMiNombre = new JTextField("Miguel Angel");
        panelConexion.add(txtMiNombre);

        panelConexion.add(new JLabel("IP Destino:"));
        txtIpDestino = new JTextField("127.0.0.1");
        panelConexion.add(txtIpDestino);

        lblEstado = new JLabel("Estado: Sin conexiones activas");
        lblEstado.setForeground(Color.RED);
        panelConexion.add(lblEstado);

        btnEnviarInvitacion = new JButton("Enviar Invitación");
        panelConexion.add(btnEnviarInvitacion);

        // ---------- PARA PREGUNTA 5 ------------
        btnFueraDeLinea = new JButton("Fuera de Línea (0018)");
        panelConexion.add(new JLabel(""));
        panelConexion.add(btnFueraDeLinea);

        // --- 2. PANEL CENTRAL: Área de Chat ---
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        JScrollPane scrollChat = new JScrollPane(areaChat);
        scrollChat.setBorder(new TitledBorder("2. Conversación"));

        // --- 3. PANEL INFERIOR: Selección de destinatario y envío de mensajes ---
        JPanel panelMensaje = new JPanel(new BorderLayout(5, 5));
        panelMensaje.setBorder(new TitledBorder("3. Enviar Mensaje de Texto"));

        JPanel panelDestinatario = new JPanel(new BorderLayout(5, 0));
        panelDestinatario.add(new JLabel("Para: "), BorderLayout.WEST);
        modeloDestinatarios = new DefaultComboBoxModel<>();
        comboDestinatarios = new JComboBox<>(modeloDestinatarios);
        panelDestinatario.add(comboDestinatarios, BorderLayout.CENTER);

        JPanel panelInput = new JPanel(new BorderLayout(5, 0));
        txtMensaje = new JTextField();
        btnEnviarMensaje = new JButton("Enviar");
        btnEnviarMensaje.setEnabled(false);
        panelInput.add(txtMensaje, BorderLayout.CENTER);
        panelInput.add(btnEnviarMensaje, BorderLayout.EAST);

        panelMensaje.add(panelDestinatario, BorderLayout.NORTH);
        panelMensaje.add(panelInput, BorderLayout.CENTER);

        // --- AGREGAR PANELES A LA VENTANA ---
        add(panelConexion, BorderLayout.NORTH);
        add(scrollChat, BorderLayout.CENTER);
        add(panelMensaje, BorderLayout.SOUTH);

        // --- CONFIGURAR BOTONES ---
        btnEnviarInvitacion.addActionListener(e -> enviarInvitacion());
        btnEnviarMensaje.addActionListener(e -> enviarMensajeChat());
        btnFueraDeLinea.addActionListener(e -> activarFueraDeLinea()); // -------- PARA PREGUNTA 5 --------------
    }

    // ACCIÓN: Cuando hacemos clic en "Enviar Invitación"
    private void enviarInvitacion() {
        try {
            String ip = txtIpDestino.getText().trim();
            String miNombre = txtMiNombre.getText().trim();

            if (Mediador.getInstancia().existe(ip)) {
                JOptionPane.showMessageDialog(this, "Ya existe una conexión activa con " + ip);
                return;
            }

            // 1. Conectamos al socket destino y registramos en el Mediador
            SocketClient cliente = new SocketClient(ip);
            cliente.addChatEventListener(this);
            Mediador.getInstancia().registrar(cliente);
            cliente.start();

            // 2. Delegamos el envío de la trama 001 al Mediador
            Invitacion inv = new Invitacion("ID_MIGUEL", miNombre);
            Mediador.getInstancia().enviarMensaje(ip, inv.generarTrama());

            // 3. Actualizamos la interfaz
            lblEstado.setText("Estado: Invitación enviada a " + ip + "...");
            lblEstado.setForeground(Color.ORANGE);
            areaChat.append("-> Invitación (001) enviada a " + ip + "\n");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al conectar a la IP: " + ex.getMessage());
        }
    }

    // ACCIÓN: Cuando enviamos un mensaje en el chat
    private void enviarMensajeChat() {
        String itemSeleccionado = (String) comboDestinatarios.getSelectedItem();
        if (itemSeleccionado == null) return;

        String ip = extraerIp(itemSeleccionado);
        String msg = txtMensaje.getText().trim();
        if (msg.isEmpty()) return;

        try {
            String idMensaje = String.valueOf(System.currentTimeMillis());
            EnvioMensaje envio = new EnvioMensaje("ID_MIGUEL", idMensaje, msg);
            // Delegamos el envío al Mediador en vez de usar un socket directo
            Mediador.getInstancia().enviarMensaje(ip, envio.generarTrama());

            String nombre = nombresConectados.getOrDefault(ip, ip);
            areaChat.append("Yo -> " + nombre + ": " + msg + "\n");
            txtMensaje.setText("");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onInvitacionRecibida(Invitacion inv, SocketClient sender) {
        SwingUtilities.invokeLater(() -> {
            int respuesta = JOptionPane.showConfirmDialog(
                    this,
                    "El usuario '" + inv.getNombre() + "' (" + sender.getIp() + ") te ha enviado una invitación.\n¿Aceptas conectarte?",
                    "Invitación Recibida (Trama 001)",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
            );

            if (respuesta == JOptionPane.YES_OPTION) {
                try {
                    // 1. Armar y enviar trama 002 (Aceptación) a través del Mediador
                    String miNombre = txtMiNombre.getText().trim();
                    AceptacionInvitacion acc = new AceptacionInvitacion("ID_MI_PC", miNombre);
                    Mediador.getInstancia().enviarMensaje(sender.getIp(), acc.generarTrama());

                    // 2. Registrar la conexión en la UI (sin sobrescribir conexiones anteriores)
                    agregarConexion(sender.getIp(), inv.getNombre());
                    areaChat.append("<- Has aceptado la invitación de " + inv.getNombre() + " (" + sender.getIp() + ")\n");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    // Enviar trama 003 (Rechazo) a través del Mediador
                    RechazoInvitacion rechazo = new RechazoInvitacion();
                    Mediador.getInstancia().enviarMensaje(sender.getIp(), rechazo.generarTrama());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Eliminar la conexión rechazada del Mediador
                Mediador.getInstancia().eliminar(sender.getIp());
                areaChat.append("- Rechazaste la invitación de " + inv.getNombre() + "\n");
            }
        });
    }

    @Override
    public void onAceptacionRecibida(AceptacionInvitacion acc, SocketClient sender) {
        SwingUtilities.invokeLater(() -> {
            // Registrar la nueva conexión en la UI (sin sobrescribir conexiones anteriores)
            agregarConexion(sender.getIp(), acc.getNombre());
            areaChat.append("<- " + acc.getNombre() + " (" + sender.getIp() + ") aceptó tu invitación (002). ¡Ya pueden hablar!\n");
        });
    }

    @Override
    public void onRechazoRecibido(RechazoInvitacion rechazo, SocketClient sender) {
        SwingUtilities.invokeLater(() -> {
            // Eliminar la conexión rechazada del Mediador
            Mediador.getInstancia().eliminar(sender.getIp());
            areaChat.append("<- " + sender.getIp() + " rechazó tu invitación (003).\n");
            actualizarEstado();
        });
    }

    @Override
    public void onMensajeRecibido(EnvioMensaje msg, SocketClient sender) {
        SwingUtilities.invokeLater(() -> {
            String nombre = nombresConectados.getOrDefault(sender.getIp(), sender.getIp());
            areaChat.append(nombre + ": " + msg.getContenido() + "\n");

            // Enviar trama 008 (Confirmación de recepción) a través del Mediador
            try {
                ConfirmacionMensaje conf = new ConfirmacionMensaje(msg.getIdMensaje());
                Mediador.getInstancia().enviarMensaje(sender.getIp(), conf.generarTrama());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onConfirmacionRecibida(ConfirmacionMensaje conf, SocketClient sender) {
        SwingUtilities.invokeLater(() -> {
            String nombre = nombresConectados.getOrDefault(sender.getIp(), sender.getIp());
            areaChat.append("  [✓ Mensaje " + conf.getIdMensaje() + " recibido por " + nombre + "]\n");
        });
    }

    // ----- PREGUNTA 5 EXAMEN: ACCION ACTIVAR FUERA DE LINEA -----
    private void activarFueraDeLinea() {
        if (nombresConectados.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay contactos activos para notificar");
            return;
        }

        int confirmar = JOptionPane.showConfirmDialog(
                this,
                "Deseas desconectarte? Se va a notificar a todos tus contactos activos.",
                "Confirmar desconexion",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmar == JOptionPane.YES_OPTION) {
            try {
                Despedida despedida = new Despedida("ID_MIGUEL");
                Mediador.getInstancia().enviarATodos(despedida.generarTrama());
                areaChat.append("Despedida (0018) enviada a todos los contactos. Estás fuera de línea.\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDespedidaRecibida(Despedida despedida, SocketClient emisor) {
        SwingUtilities.invokeLater(() -> {
            String nombre = nombresConectados.getOrDefault(emisor.getIp(), emisor.getIp());

            JOptionPane.showMessageDialog(
                    this,
                    "El usuario '" + nombre + "' (" + emisor.getIp() + ") se ha puesto fuera de línea.",
                    "Usuario Fuera de Línea (Trama 0018)",
                    JOptionPane.WARNING_MESSAGE
            );

            Mediador.getInstancia().eliminar(emisor.getIp());
            areaChat.append("El usuario " + nombre + " (" + emisor.getIp() + ") se ha puesto fuera de línea (0018).\n");
            actualizarEstado();
        });
    }

    // --- Métodos auxiliares para gestión de conexiones múltiples ---

    private void agregarConexion(String ip, String nombre) {
        nombresConectados.put(ip, nombre);
        String item = nombre + " (" + ip + ")";
        // Evitar duplicados en el combo
        for (int i = 0; i < modeloDestinatarios.getSize(); i++) {
            if (extraerIp(modeloDestinatarios.getElementAt(i)).equals(ip)) {
                return;
            }
        }
        modeloDestinatarios.addElement(item);
        actualizarEstado();
    }

    private String extraerIp(String item) {
        int start = item.lastIndexOf('(');
        int end = item.lastIndexOf(')');
        return item.substring(start + 1, end);
    }

    private void actualizarEstado() {
        int n = nombresConectados.size();
        if (n == 0) {
            lblEstado.setText("Estado: Sin conexiones activas");
            lblEstado.setForeground(Color.RED);
            btnEnviarMensaje.setEnabled(false);
        } else {
            lblEstado.setText("Estado: " + n + " conexión(es) activa(s)");
            lblEstado.setForeground(new Color(0, 153, 0));
            btnEnviarMensaje.setEnabled(true);
        }
    }
}
