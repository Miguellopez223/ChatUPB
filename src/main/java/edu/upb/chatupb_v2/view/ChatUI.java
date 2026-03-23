package edu.upb.chatupb_v2.view;

import edu.upb.chatupb_v2.controller.ChatController;
import edu.upb.chatupb_v2.controller.ContactController;
import edu.upb.chatupb_v2.model.entities.User;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ChatUI extends JFrame implements IChatView {

    // Componentes visuales
    private JTextField txtIpDestino;
    private JTextField txtMiNombre;
    private JButton btnEnviarInvitacion;
    private JLabel lblEstado;

    private JTextArea areaChat; // Para logs globales
    private JTextField txtMensaje;
    private JButton btnEnviarMensaje;
    private JButton btnZumbido;
    private JButton btnMensajeUnico; // NUEVO: Boton para Trama 012
    private DefaultComboBoxModel<String> modeloDestinatarios;
    private JComboBox<String> comboDestinatarios;

    // Tabla de contactos
    private DefaultTableModel modeloTablaContactos;
    private JTable tablaContactos;
    private final ContactController contactController;
    private final ChatController chatController;
    private List<ContactInfo> contactosEnMemoria = new java.util.ArrayList<>();

    // Chat por contacto
    private final HashMap<String, JPanel> chatPanels = new HashMap<>();
    private String contactoActivo = null;
    private JScrollPane scrollChatActual;

    // Mapas para actualizar UI de mensajes
    private final HashMap<String, JLabel> checkLabels = new HashMap<>();
    private final HashMap<String, JLabel> messageLabels = new HashMap<>();
    private final HashMap<String, RoundedPanel> bubblePanels = new HashMap<>();
    private final HashMap<String, JLabel> pinLabels = new HashMap<>();
    private final HashMap<String, Boolean> bubbleIsMine = new HashMap<>();

    // Temas por contacto (IP -> idTema)
    private final HashMap<String, String> temasContacto = new HashMap<>();
    private JComboBox<String> comboTemas;
    private boolean suppressTemaEvent = false;

    // Barra de mensaje fijado en la parte superior del chat
    private JPanel pinnedMessageBar;
    private JLabel pinnedMessageLabel;
    private JButton pinnedUnpinButton;
    private String pinnedMessageId = null;
    private String previousPinnedId = null;

    // User selection
    private JComboBox<User> userComboBox;
    private DefaultComboBoxModel<User> userComboBoxModel;

    public ChatUI() {
        this.chatController = new ChatController(this);
        this.contactController = new ContactController(this);
        configurarVentana();
    }

    private void configurarVentana() {
        setTitle("Chat P2P");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- USER SELECTION PANEL ---
        JPanel userPanel = new JPanel(new BorderLayout(5, 5));
        userPanel.setBorder(new TitledBorder("Gestión de Usuario"));
        userComboBoxModel = new DefaultComboBoxModel<>();
        userComboBox = new JComboBox<>(userComboBoxModel);
        JButton btnAddUser = new JButton("Nuevo Usuario");
        userPanel.add(new JLabel("Usuario:"), BorderLayout.WEST);
        userPanel.add(userComboBox, BorderLayout.CENTER);
        userPanel.add(btnAddUser, BorderLayout.EAST);

        // --- 1. PANEL SUPERIOR: Conexion e Invitaciones ---
        JPanel panelConexion = new JPanel(new GridLayout(2, 2, 5, 5));
        panelConexion.setBorder(new TitledBorder("1. Enviar Invitacion (Trama 001) - Solo contactos nuevos"));

        panelConexion.add(new JLabel("IP Destino:"));
        txtIpDestino = new JTextField("127.0.0.1");
        panelConexion.add(txtIpDestino);

        lblEstado = new JLabel("Estado: Sin conexiones activas");
        lblEstado.setForeground(Color.RED);
        panelConexion.add(lblEstado);

        btnEnviarInvitacion = new JButton("Enviar Invitacion");
        panelConexion.add(btnEnviarInvitacion);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(userPanel, BorderLayout.NORTH);
        topPanel.add(panelConexion, BorderLayout.CENTER);

        // --- BARRA DE MENSAJE FIJADO ---
        pinnedMessageBar = new JPanel(new BorderLayout(5, 0));
        pinnedMessageBar.setBackground(new Color(255, 248, 220));
        pinnedMessageBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 200, 160)),
                new EmptyBorder(6, 10, 6, 10)
        ));

        JLabel pinIcon = new JLabel("\uD83D\uDCCC");
        pinIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        pinnedMessageBar.add(pinIcon, BorderLayout.WEST);

        pinnedMessageLabel = new JLabel("");
        pinnedMessageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pinnedMessageLabel.setForeground(new Color(80, 80, 80));
        pinnedMessageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        pinnedMessageBar.add(pinnedMessageLabel, BorderLayout.CENTER);

        pinnedUnpinButton = new JButton("\u2715");
        pinnedUnpinButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        pinnedUnpinButton.setMargin(new Insets(0, 4, 0, 4));
        pinnedUnpinButton.setFocusPainted(false);
        pinnedUnpinButton.setContentAreaFilled(false);
        pinnedUnpinButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        pinnedUnpinButton.setToolTipText("Desfijar mensaje");
        pinnedUnpinButton.addActionListener(e -> {
            if (contactoActivo != null && pinnedMessageId != null) {
                chatController.desfijarMensaje(contactoActivo, pinnedMessageId);
            }
        });
        pinnedMessageBar.add(pinnedUnpinButton, BorderLayout.EAST);
        pinnedMessageBar.setVisible(false);

        // --- 2. PANEL CENTRAL: Area de Chat ---
        areaChat = new JTextArea("Selecciona un usuario para comenzar.\n");
        areaChat.setEditable(false);
        scrollChatActual = new JScrollPane(areaChat);
        scrollChatActual.setBorder(new TitledBorder("2. Conversacion"));
        scrollChatActual.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollChatActual.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // --- 3. PANEL INFERIOR: Envio de mensajes ---
        JPanel panelMensaje = new JPanel(new BorderLayout(5, 5));
        panelMensaje.setBorder(new TitledBorder("3. Enviar Mensaje de Texto"));

        JPanel panelDestinatario = new JPanel(new BorderLayout(5, 0));
        panelDestinatario.add(new JLabel("Para: "), BorderLayout.WEST);
        modeloDestinatarios = new DefaultComboBoxModel<>();
        comboDestinatarios = new JComboBox<>(modeloDestinatarios);
        panelDestinatario.add(comboDestinatarios, BorderLayout.CENTER);

        JPanel panelInput = new JPanel(new BorderLayout(5, 0));
        txtMensaje = new JTextField();

        // Botones de envio
        btnEnviarMensaje = new JButton("Enviar");
        btnEnviarMensaje.setEnabled(false);

        btnZumbido = new JButton("\uD83D\uDCA5 Zumbido");
        btnZumbido.setEnabled(false);

        btnMensajeUnico = new JButton("\uD83D\uDCA3 Ver 1 Vez");
        btnMensajeUnico.setEnabled(false);
        btnMensajeUnico.setBackground(new Color(255, 200, 200));

        comboTemas = new JComboBox<>(new String[]{
                "1-Defecto", "2-Azul", "3-Rojo", "4-Amarillo", "5-Violeta"
        });
        comboTemas.setEnabled(false);
        comboTemas.setToolTipText("Cambiar tema del chat");

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        panelBotones.add(new JLabel("Tema:"));
        panelBotones.add(comboTemas);
        panelBotones.add(btnZumbido);
        panelBotones.add(btnMensajeUnico);
        panelBotones.add(btnEnviarMensaje);

        panelInput.add(txtMensaje, BorderLayout.CENTER);
        panelInput.add(panelBotones, BorderLayout.EAST);

        panelMensaje.add(panelDestinatario, BorderLayout.NORTH);
        panelMensaje.add(panelInput, BorderLayout.CENTER);

        // --- 4. PANEL IZQUIERDO: Tabla de Contactos ---
        JPanel panelContactos = new JPanel(new BorderLayout(5, 5));
        panelContactos.setBorder(new TitledBorder("Contactos (doble click para chatear)"));
        panelContactos.setPreferredSize(new Dimension(250, 0));

        modeloTablaContactos = new DefaultTableModel(new String[]{"Nombre", "IP", "Estado", "Msg"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaContactos = new JTable(modeloTablaContactos);
        tablaContactos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaContactos.setRowHeight(24);

        tablaContactos.getColumnModel().getColumn(0).setPreferredWidth(90);
        tablaContactos.getColumnModel().getColumn(1).setPreferredWidth(100);
        tablaContactos.getColumnModel().getColumn(2).setPreferredWidth(40);
        tablaContactos.getColumnModel().getColumn(2).setCellRenderer(new EstadoConexionRenderer());
        tablaContactos.getColumnModel().getColumn(3).setPreferredWidth(30);
        tablaContactos.getColumnModel().getColumn(3).setCellRenderer(new MensajePendienteRenderer());

        JScrollPane scrollContactos = new JScrollPane(tablaContactos);
        panelContactos.add(scrollContactos, BorderLayout.CENTER);

        JButton btnEliminarContacto = new JButton("Eliminar");
        panelContactos.add(btnEliminarContacto, BorderLayout.SOUTH);

        // --- WRAPPER CENTRAL ---
        JPanel chatWrapper = new JPanel(new BorderLayout());
        chatWrapper.add(pinnedMessageBar, BorderLayout.NORTH);
        chatWrapper.add(scrollChatActual, BorderLayout.CENTER);

        add(panelContactos, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);
        add(chatWrapper, BorderLayout.CENTER);
        add(panelMensaje, BorderLayout.SOUTH);

        // --- EVENTOS ---
        userComboBox.addActionListener(e -> {
            User selectedUser = (User) userComboBox.getSelectedItem();
            if (selectedUser != null) {
                chatController.cambiarUsuario(selectedUser);
            }
        });
        btnAddUser.addActionListener(e -> chatController.crearNuevoUsuario());
        btnEnviarInvitacion.addActionListener(e -> enviarInvitacion());
        btnEnviarMensaje.addActionListener(e -> enviarMensajeChat());
        btnZumbido.addActionListener(e -> enviarZumbido());
        btnEliminarContacto.addActionListener(e -> eliminarContacto());
        txtMensaje.addActionListener(e -> enviarMensajeChat());

        // Evento para el boton de Mensaje Unico
        btnMensajeUnico.addActionListener(e -> {
            String msg = txtMensaje.getText().trim();
            if (msg.isEmpty() || contactoActivo == null) return;
            chatController.enviarMensajeUnico(contactoActivo, msg);
        });

        // Evento para cambio de tema
        comboTemas.addActionListener(e -> {
            if (suppressTemaEvent || contactoActivo == null) return;
            String selected = (String) comboTemas.getSelectedItem();
            if (selected == null) return;
            String idTema = selected.substring(0, 1);
            chatController.enviarCambioTema(contactoActivo, idTema);
        });

        tablaContactos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tablaContactos.getSelectedRow();
                if (row >= 0 && row < contactosEnMemoria.size()) {
                    ContactInfo c = contactosEnMemoria.get(row);
                    txtIpDestino.setText(c.getIp());
                }
            }
        });

        tablaContactos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tablaContactos.getSelectedRow();
                    if (row >= 0 && row < contactosEnMemoria.size()) {
                        ContactInfo contacto = contactosEnMemoria.get(row);
                        chatController.abrirChat(contacto);
                    }
                }
            }
        });
    }

    public ChatController getChatController() {
        return chatController;
    }

    private void enviarInvitacion() {
        String ip = txtIpDestino.getText().trim();
        User currentUser = (User) userComboBox.getSelectedItem();
        if (currentUser == null) {
            mostrarError("Por favor, selecciona un usuario.");
            return;
        }
        chatController.enviarInvitacion(ip, currentUser.getName());
    }

    private void enviarMensajeChat() {
        String msg = txtMensaje.getText().trim();
        if (msg.isEmpty()) return;

        String itemSeleccionado = (String) comboDestinatarios.getSelectedItem();
        String ip = null;

        if (itemSeleccionado != null) {
            ip = extraerIp(itemSeleccionado);
        }

        if (ip == null && contactoActivo != null) {
            ip = contactoActivo;
        }

        if (ip == null) return;

        chatController.enviarMensaje(ip, msg);
    }

    private void enviarZumbido() {
        String itemSeleccionado = (String) comboDestinatarios.getSelectedItem();
        String ip = null;

        if (itemSeleccionado != null) {
            ip = extraerIp(itemSeleccionado);
        }

        if (ip == null && contactoActivo != null) {
            ip = contactoActivo;
        }

        if (ip == null) return;

        chatController.enviarZumbido(ip);
    }

    @Override
    public void mostrarZumbido(String ip, String nombreContacto) {
        JPanel panel = getOrCreateChatPanel(ip);
        addSystemLabel(panel, "\uD83D\uDCA5 " + nombreContacto + " ha enviado un zumbido!");
        if (ip.equals(contactoActivo)) {
            scrollChatActual.setViewportView(panel);
            scrollToBottom(panel);
        }

        final Point posOriginal = getLocation();
        final int duracion = 500;
        final int intervalo = 30;
        final int intensidad = 8;

        Timer shakeTimer = new Timer(intervalo, null);
        final long[] inicio = {System.currentTimeMillis()};
        shakeTimer.addActionListener(e -> {
            long transcurrido = System.currentTimeMillis() - inicio[0];
            if (transcurrido >= duracion) {
                shakeTimer.stop();
                setLocation(posOriginal);
            } else {
                int dx = (int) (Math.random() * intensidad * 2) - intensidad;
                int dy = (int) (Math.random() * intensidad * 2) - intensidad;
                setLocation(posOriginal.x + dx, posOriginal.y + dy);
            }
        });
        shakeTimer.start();
        Toolkit.getDefaultToolkit().beep();
    }

    private void eliminarContacto() {
        int row = tablaContactos.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un contacto para eliminar.");
            return;
        }
        ContactInfo contacto = contactosEnMemoria.get(row);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Eliminar a " + contacto.getName() + "?",
                "Confirmar eliminacion", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        contactController.eliminar(contacto.getId());
    }

    @Override
    public void onLoad(List<ContactInfo> contactos) {
        contactosEnMemoria = contactos;
        modeloTablaContactos.setRowCount(0);
        for (ContactInfo c : contactosEnMemoria) {
            boolean conectado = chatController.isConectado(c.getIp());
            boolean pendiente = chatController.tieneMensajePendiente(c.getIp());
            modeloTablaContactos.addRow(new Object[]{c.getName(), c.getIp(), conectado, pendiente});
        }
    }

    @Override
    public void appendChat(String texto) {
        areaChat.append(texto);
        areaChat.setCaretPosition(areaChat.getDocument().getLength());
    }

    @Override
    public boolean mostrarDialogoInvitacion(String nombre, String ip) {
        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "El usuario '" + nombre + "' (" + ip + ") te ha enviado una invitacion.\nAceptas conectarte?",
                "Invitacion Recibida (Trama 001)",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );
        return respuesta == JOptionPane.YES_OPTION;
    }

    @Override
    public void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje);
    }

    @Override
    public void agregarConexionUI(String ip, String nombre) {
        String item = nombre + " (" + ip + ")";
        for (int i = 0; i < modeloDestinatarios.getSize(); i++) {
            if (extraerIp(modeloDestinatarios.getElementAt(i)).equals(ip)) {
                return;
            }
        }
        modeloDestinatarios.addElement(item);
    }

    @Override
    public void limpiarConexionesUI() {
        modeloDestinatarios.removeAllElements();
        actualizarEstado(0);
    }

    @Override
    public void actualizarEstado(int numConexiones) {
        if (numConexiones == 0) {
            lblEstado.setText("Estado: Sin conexiones activas");
            lblEstado.setForeground(Color.RED);
            if (contactoActivo == null) {
                btnEnviarMensaje.setEnabled(false);
                btnZumbido.setEnabled(false);
                btnMensajeUnico.setEnabled(false);
            }
        } else {
            lblEstado.setText("Estado: " + numConexiones + " conexion(es) activa(s)");
            lblEstado.setForeground(new Color(0, 153, 0));
            btnEnviarMensaje.setEnabled(true);
            btnZumbido.setEnabled(true);
            btnMensajeUnico.setEnabled(true);
        }
    }

    @Override
    public void refrescarEstadoContactos() {
        for (int i = 0; i < contactosEnMemoria.size(); i++) {
            String ip = contactosEnMemoria.get(i).getIp();
            boolean conectado = chatController.isConectado(ip);
            boolean pendiente = chatController.tieneMensajePendiente(ip);
            modeloTablaContactos.setValueAt(conectado, i, 2);
            modeloTablaContactos.setValueAt(pendiente, i, 3);
        }
    }

    @Override
    public void actualizarEstadoInvitacion(String ip) {
        lblEstado.setText("Estado: Invitacion enviada a " + ip + "...");
        lblEstado.setForeground(Color.ORANGE);
    }

    @Override
    public void limpiarMensaje() {
        txtMensaje.setText("");
    }

    @Override
    public String getMiNombre() {
        User currentUser = (User) userComboBox.getSelectedItem();
        return currentUser != null ? currentUser.getName() : "";
    }

    private JPanel getOrCreateChatPanel(String ip) {
        return chatPanels.computeIfAbsent(ip, k -> {
            Color[] colores = getColoresTema(temasContacto.getOrDefault(ip, "1"));
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBackground(colores[0]);
            p.setBorder(new EmptyBorder(10, 10, 10, 10));
            return p;
        });
    }

    @Override
    public void abrirChatConContacto(ContactInfo contacto, List<ChatMessageInfo> historial) {
        contactoActivo = contacto.getIp();

        JPanel panel = getOrCreateChatPanel(contacto.getIp());
        panel.removeAll();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        for (ChatMessageInfo msg : historial) {
            String time = "";
            try {
                time = sdf.format(new Date(Long.parseLong(msg.getTimestamp())));
            } catch (NumberFormatException e) {
                time = msg.getTimestamp() != null ? msg.getTimestamp() : "";
            }
            String idMensaje = msg.getId();
            addBubble(panel, msg.getContent(), time, msg.isMine(), msg.isConfirmed(), idMensaje, msg.isPinned(), msg.isViewOnce());
        }

        panel.add(Box.createVerticalGlue());

        scrollChatActual.setViewportView(panel);
        scrollChatActual.setBorder(new TitledBorder("Chat con " + contacto.getName()));

        selectDestinatarioPorIp(contacto.getIp());
        btnEnviarMensaje.setEnabled(true);
        btnZumbido.setEnabled(true);
        btnMensajeUnico.setEnabled(true);
        comboTemas.setEnabled(true);

        // Seleccionar el tema actual del contacto sin disparar el evento
        String temaActual = temasContacto.getOrDefault(contacto.getIp(), "1");
        suppressTemaEvent = true;
        comboTemas.setSelectedIndex(Integer.parseInt(temaActual) - 1);
        suppressTemaEvent = false;

        // Aplicar colores del tema a la pinned bar
        Color[] coloresTema = getColoresTema(temaActual);
        pinnedMessageBar.setBackground(coloresTema[4]);
        pinnedMessageBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, coloresTema[5]),
                new EmptyBorder(6, 10, 6, 10)
        ));

        scrollToBottom(panel);
    }

    @Override
    public void appendChatToContact(String ip, String texto) {
        JPanel panel = getOrCreateChatPanel(ip);
        String cleanText = texto.trim();
        if (cleanText.isEmpty()) return;

        addSystemLabel(panel, cleanText);

        if (ip.equals(contactoActivo)) {
            scrollChatActual.setViewportView(panel);
            scrollToBottom(panel);
        }
    }

    // ACTUALIZADO: Metodo que implementa la interfaz
    @Override
    public void appendMensajeToContact(String ip, String content, boolean isMine, String idMensaje, boolean viewOnce) {
        JPanel panel = getOrCreateChatPanel(ip);
        String time = new SimpleDateFormat("HH:mm").format(new Date());

        addBubble(panel, content, time, isMine, false, idMensaje, false, viewOnce);

        if (ip.equals(contactoActivo)) {
            scrollChatActual.setViewportView(panel);
            scrollToBottom(panel);
        }
    }

    @Override
    public void actualizarBurbujaMensajeEliminado(String ip, String idMensaje) {
        JLabel msgLabel = messageLabels.get(idMensaje);
        if (msgLabel != null) {
            String html = "<html><body style='font-family: Segoe UI, sans-serif; font-size: 13px; color: #8c8c8c;'>"
                    + "<i>\uD83D\uDEAB Este mensaje fue eliminado</i></body></html>";
            msgLabel.setText(html);
            msgLabel.setComponentPopupMenu(null);
            msgLabel.revalidate();
            msgLabel.repaint();
        }

        // Si era un boton ViewOnce, lo reemplazamos por el label de eliminado
        RoundedPanel bubble = bubblePanels.get(idMensaje);
        if (bubble != null) {
            Component[] comps = bubble.getComponents();
            for (Component c : comps) {
                if (c instanceof JButton) {
                    bubble.remove(c);
                    String html = "<html><body style='font-family: Segoe UI, sans-serif; font-size: 13px; color: #8c8c8c;'>"
                            + "<i>\uD83D\uDEAB Mensaje único abierto</i></body></html>";
                    JLabel lbl = new JLabel(html);
                    lbl.setBorder(new EmptyBorder(8, 12, 4, 12));
                    bubble.add(lbl, BorderLayout.CENTER);
                }
            }
            bubble.setBgColor(new Color(245, 245, 245));
            bubble.setComponentPopupMenu(null);
            bubble.revalidate();
            bubble.repaint();
        }

        JLabel checkLabel = checkLabels.remove(idMensaje);
        if (checkLabel != null && checkLabel.getParent() != null) {
            java.awt.Container parent = checkLabel.getParent();
            parent.remove(checkLabel);
            parent.revalidate();
            parent.repaint();
        }
    }

    @Override
    public void actualizarCheckMensaje(String ip, String idMensaje) {
        JLabel checkLabel = checkLabels.get(idMensaje);
        if (checkLabel != null) {
            checkLabel.setText("\u2713\u2713");
            checkLabel.setForeground(new Color(53, 147, 234));
            checkLabel.repaint();
        }
    }

    // Metodo helper por retrocompatibilidad
    private void addBubble(JPanel panel, String text, String time, boolean isMine, boolean confirmed, String idMensaje) {
        addBubble(panel, text, time, isMine, confirmed, idMensaje, false, false);
    }

    // ACTUALIZADO: Constructor principal de la burbuja
    private void addBubble(JPanel panel, String text, String time, boolean isMine, boolean confirmed, String idMensaje, boolean pinned, boolean viewOnce) {
        boolean isDeleted = (text == null || text.isEmpty());

        JPanel rowWrapper = new JPanel(new BorderLayout());
        rowWrapper.setOpaque(false);
        rowWrapper.setBorder(new EmptyBorder(3, 0, 3, 0));

        Color[] temaColores = getColoresTema(getTemaActivo());
        Color bgColor;
        if (isDeleted) {
            bgColor = new Color(245, 245, 245);
        } else {
            bgColor = isMine ? temaColores[1] : temaColores[2];
        }
        RoundedPanel bubble = new RoundedPanel(bgColor, 14);
        bubble.setBorderColor(temaColores[3]);
        bubble.setLayout(new BorderLayout());

        JLabel messageLabel = null;
        JButton btnViewOnce = null;

        if (isDeleted) {
            String html = "<html><body style='font-family: Segoe UI, sans-serif; font-size: 13px; color: #8c8c8c;'>"
                    + "<i>\uD83D\uDEAB Este mensaje fue eliminado</i></body></html>";
            messageLabel = new JLabel(html);
            messageLabel.setBorder(new EmptyBorder(8, 12, 4, 12));
        } else if (viewOnce) {
            btnViewOnce = new JButton(isMine ? "\uD83D\uDCA3 Mensaje Único Enviado" : "\uD83D\uDCA3 Abrir Mensaje Oculto");
            btnViewOnce.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnViewOnce.setFocusPainted(false);
            btnViewOnce.setBackground(new Color(255, 220, 220));
            btnViewOnce.setCursor(new Cursor(Cursor.HAND_CURSOR));

            if (!isMine) {
                btnViewOnce.addActionListener(e -> mostrarPopUpMensajeUnico(text, idMensaje));
            } else {
                btnViewOnce.setEnabled(false); // El emisor no lo puede abrir
            }
        } else {
            String escapedText = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
            String widthStyle = text.length() < 30 ? "" : "width: 250px; ";
            String html = "<html><body style='" + widthStyle + "font-family: Segoe UI, sans-serif; font-size: 13px;'>"
                    + escapedText + "</body></html>";
            messageLabel = new JLabel(html);
            messageLabel.setBorder(new EmptyBorder(8, 12, 4, 12));
        }

        if (idMensaje != null && messageLabel != null) {
            messageLabels.put(idMensaje, messageLabel);
        }
        if (idMensaje != null) {
            bubblePanels.put(idMensaje, bubble);
            bubbleIsMine.put(idMensaje, isMine);
        }

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(0, 0, 6, 8));

        if (pinned && !isDeleted) {
            JLabel pinLabel = new JLabel("\uD83D\uDCCC");
            pinLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 10));
            bottomPanel.add(pinLabel);
            if (idMensaje != null) pinLabels.put(idMensaje, pinLabel);
        }

        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(new Color(140, 140, 140));
        bottomPanel.add(timeLabel);

        if (isMine && !isDeleted) {
            JLabel checkLabel = new JLabel(confirmed ? "\u2713\u2713" : "\u2713");
            checkLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            checkLabel.setForeground(confirmed ? new Color(53, 147, 234) : new Color(140, 140, 140));
            bottomPanel.add(checkLabel);
            if (idMensaje != null) checkLabels.put(idMensaje, checkLabel);
        }

        if (btnViewOnce != null) {
            bubble.add(btnViewOnce, BorderLayout.CENTER);
        } else {
            bubble.add(messageLabel, BorderLayout.CENTER);
            if (!isDeleted && idMensaje != null) {
                JPopupMenu popupMenu = new JPopupMenu();
                JMenuItem fijarItem = new JMenuItem("\uD83D\uDCCC Fijar mensaje");
                fijarItem.addActionListener(e -> chatController.fijarMensaje(contactoActivo, idMensaje));
                popupMenu.add(fijarItem);

                if (isMine) {
                    JMenuItem eliminarItem = new JMenuItem("Eliminar mensaje");
                    eliminarItem.addActionListener(e -> {
                        int confirm = JOptionPane.showConfirmDialog(this,
                                "Eliminar este mensaje? Esta accion no se puede deshacer.",
                                "Eliminar mensaje", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION && contactoActivo != null) {
                            chatController.eliminarMensaje(contactoActivo, idMensaje);
                        }
                    });
                    popupMenu.add(eliminarItem);
                }
                bubble.setComponentPopupMenu(popupMenu);
                messageLabel.setComponentPopupMenu(popupMenu);
            }
        }

        bubble.add(bottomPanel, BorderLayout.SOUTH);

        JPanel alignmentWrapper = new JPanel(new FlowLayout(isMine ? FlowLayout.RIGHT : FlowLayout.LEFT, 8, 0));
        alignmentWrapper.setOpaque(false);
        alignmentWrapper.add(bubble);

        rowWrapper.add(alignmentWrapper, BorderLayout.CENTER);

        panel.add(rowWrapper);
        panel.revalidate();
    }

    private void addSystemLabel(JPanel panel, String text) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(5, 0, 5, 0));

        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lbl.setForeground(Color.GRAY);

        wrapper.add(lbl);
        panel.add(wrapper);
        panel.revalidate();
    }

    private void scrollToBottom(JPanel panel) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (scrollChatActual != null && scrollChatActual.getVerticalScrollBar() != null) {
                    JScrollBar vertical = scrollChatActual.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                }
            } catch (Exception ignored) {}
        });
    }

    @Override
    public void setUserList(List<User> users) {
        userComboBoxModel.removeAllElements();
        for (User user : users) {
            userComboBoxModel.addElement(user);
        }
    }

    @Override
    public void showNewUserDialog() {
        String name = null;
        while (name == null || name.trim().isEmpty() || name.length() > 60) {
            name = JOptionPane.showInputDialog(this,
                    "Bienvenido a ChatUPB v2.\nPor favor ingresa tu nombre (max 60 caracteres):",
                    "Configuracion Inicial",
                    JOptionPane.QUESTION_MESSAGE);
            if (name == null) {
                if (userComboBoxModel.getSize() == 0) System.exit(0);
                return;
            }
            if (name.length() > 60) {
                JOptionPane.showMessageDialog(this, "El nombre es muy largo (max 60).");
            }
        }
        chatController.guardarNuevoUsuario(name.trim());
    }

    @Override
    public void setScreenTitle(String title) {
        setTitle(title);
    }

    @Override
    public String getContactoActivo() {
        return contactoActivo;
    }

    @Override
    public void mostrarMensajeFijado(String ip, ChatMessageInfo mensaje) {
        if (!ip.equals(contactoActivo)) return;
        if (mensaje == null || mensaje.getContent() == null || mensaje.getContent().isEmpty()) {
            ocultarMensajeFijado(ip);
            return;
        }
        pinnedMessageId = mensaje.getId();
        String textoTruncado = mensaje.getContent();
        if (textoTruncado.length() > 80) {
            textoTruncado = textoTruncado.substring(0, 80) + "...";
        }
        pinnedMessageLabel.setText(textoTruncado);
        pinnedMessageBar.setVisible(true);
        pinnedMessageBar.revalidate();
        pinnedMessageBar.repaint();
    }

    @Override
    public void ocultarMensajeFijado(String ip) {
        if (!ip.equals(contactoActivo)) return;
        pinnedMessageId = null;
        pinnedMessageBar.setVisible(false);
        pinnedMessageBar.revalidate();
        pinnedMessageBar.repaint();
    }

    @Override
    public void marcarBurbujaFijada(String ip, String idMensaje) {
        if (pinLabels.containsKey(idMensaje)) return;

        RoundedPanel bubble = bubblePanels.get(idMensaje);
        if (bubble != null) {
            Component southComp = ((BorderLayout) bubble.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
            if (southComp instanceof JPanel) {
                JPanel bottomPanel = (JPanel) southComp;
                JLabel pinLabel = new JLabel("\uD83D\uDCCC");
                pinLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 10));
                bottomPanel.add(pinLabel, 0);
                pinLabels.put(idMensaje, pinLabel);
                bottomPanel.revalidate();
                bottomPanel.repaint();
            }
        }
    }

    @Override
    public void desmarcarBurbujaFijada(String ip, String idMensaje) {
        JLabel pinLabel = pinLabels.remove(idMensaje);
        if (pinLabel != null && pinLabel.getParent() != null) {
            Container parent = pinLabel.getParent();
            parent.remove(pinLabel);
            parent.revalidate();
            parent.repaint();
        }
    }

    @Override
    public void clearChatHistory() {
        chatPanels.clear();
        checkLabels.clear();
        messageLabels.clear();
        bubblePanels.clear();
        pinLabels.clear();
        bubbleIsMine.clear();
        temasContacto.clear();
        pinnedMessageId = null;
        previousPinnedId = null;
        pinnedMessageBar.setVisible(false);
        scrollChatActual.setViewportView(areaChat);
        areaChat.setText("Selecciona un contacto para chatear.\n");
        scrollChatActual.setBorder(new TitledBorder("Conversacion"));
        contactoActivo = null;
    }

    private String extraerIp(String item) {
        int start = item.lastIndexOf('(');
        int end = item.lastIndexOf(')');
        return item.substring(start + 1, end);
    }

    private void selectDestinatarioPorIp(String ip) {
        for (int i = 0; i < modeloDestinatarios.getSize(); i++) {
            if (extraerIp(modeloDestinatarios.getElementAt(i)).equals(ip)) {
                comboDestinatarios.setSelectedIndex(i);
                return;
            }
        }
    }

    @Override
    public void notificarDesconexion(String ip) {
        refrescarEstadoContactos();
    }

    @Override
    public void mostrarIndicadorMensaje(String ip) {
        refrescarEstadoContactos();
    }

    @Override
    public void ocultarIndicadorMensaje(String ip) {
        refrescarEstadoContactos();
    }

    @Override
    public void aplicarTema(String ip, String idTema) {
        temasContacto.put(ip, idTema);

        // Actualizar combo si es el contacto activo
        if (ip.equals(contactoActivo)) {
            suppressTemaEvent = true;
            comboTemas.setSelectedIndex(Integer.parseInt(idTema) - 1);
            suppressTemaEvent = false;
        }

        Color[] colores = getColoresTema(idTema);

        // Actualizar fondo del panel de chat
        JPanel panel = chatPanels.get(ip);
        if (panel != null) {
            panel.setBackground(colores[0]);
        }

        // Actualizar colores de todas las burbujas del contacto activo
        for (java.util.Map.Entry<String, RoundedPanel> entry : bubblePanels.entrySet()) {
            RoundedPanel bubble = entry.getValue();
            String id = entry.getKey();
            Boolean isMine = bubbleIsMine.get(id);
            if (isMine == null) continue;

            // Solo actualizar burbujas que pertenecen al panel de este contacto
            if (panel != null && isComponentInPanel(bubble, panel)) {
                JLabel msgLabel = messageLabels.get(id);
                boolean isDeleted = (msgLabel != null && msgLabel.getText().contains("eliminado"))
                        || (msgLabel != null && msgLabel.getText().contains("Mensaje único abierto"));
                if (!isDeleted) {
                    bubble.setBgColor(isMine ? colores[1] : colores[2]);
                }
                bubble.setBorderColor(colores[3]);
                bubble.repaint();
            }
        }

        // Actualizar pinned bar si es el contacto activo
        if (ip.equals(contactoActivo)) {
            pinnedMessageBar.setBackground(colores[4]);
            pinnedMessageBar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, colores[5]),
                    new EmptyBorder(6, 10, 6, 10)
            ));
            pinnedMessageBar.repaint();
        }

        if (panel != null) {
            panel.revalidate();
            panel.repaint();
        }
    }

    private boolean isComponentInPanel(Component comp, JPanel panel) {
        Component current = comp;
        while (current != null) {
            if (current == panel) return true;
            current = current.getParent();
        }
        return false;
    }

    // NUEVO: Método para el PopUp de Mensaje Único
    private void mostrarPopUpMensajeUnico(String texto, String idMensaje) {
        JDialog dialog = new JDialog(this, "Mensaje de Visualización Única", true);
        dialog.setSize(350, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JTextArea area = new JTextArea("\n" + texto);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setEditable(false);
        area.setMargin(new Insets(10, 10, 10, 10));

        JButton btnLeido = new JButton("Leído (Destruir mensaje)");
        btnLeido.setBackground(new Color(255, 100, 100));
        btnLeido.setForeground(Color.WHITE);
        btnLeido.setFocusPainted(false);
        btnLeido.setFont(new Font("Segoe UI", Font.BOLD, 14));

        btnLeido.addActionListener(e -> {
            dialog.dispose();
            chatController.abrirMensajeUnico(contactoActivo, idMensaje);
        });

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                chatController.abrirMensajeUnico(contactoActivo, idMensaje);
            }
        });

        dialog.add(new JScrollPane(area), BorderLayout.CENTER);
        dialog.add(btnLeido, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // --- TEMA: Colores por tema ---
    // Cada tema devuelve: [0] = fondo panel, [1] = burbuja mia, [2] = burbuja otro, [3] = borde burbuja, [4] = fondo pinned bar, [5] = borde pinned bar
    private static Color[] getColoresTema(String idTema) {
        if (idTema == null) idTema = "1";
        switch (idTema) {
            case "2": // Azul
                return new Color[]{
                        new Color(230, 240, 250), new Color(200, 220, 245), Color.WHITE,
                        new Color(180, 200, 230), new Color(220, 235, 255), new Color(180, 200, 230)
                };
            case "3": // Rojo
                return new Color[]{
                        new Color(250, 235, 235), new Color(245, 210, 210), Color.WHITE,
                        new Color(230, 190, 190), new Color(255, 225, 225), new Color(230, 190, 190)
                };
            case "4": // Amarillo
                return new Color[]{
                        new Color(250, 248, 230), new Color(245, 240, 200), Color.WHITE,
                        new Color(230, 220, 180), new Color(255, 250, 215), new Color(230, 220, 180)
                };
            case "5": // Violeta
                return new Color[]{
                        new Color(242, 235, 250), new Color(225, 210, 245), Color.WHITE,
                        new Color(210, 190, 230), new Color(240, 225, 255), new Color(210, 190, 230)
                };
            default: // 1 - Defecto
                return new Color[]{
                        new Color(240, 240, 240), new Color(212, 245, 212), Color.WHITE,
                        new Color(210, 210, 210), new Color(255, 248, 220), new Color(210, 200, 160)
                };
        }
    }

    private String getTemaActivo() {
        if (contactoActivo == null) return "1";
        return temasContacto.getOrDefault(contactoActivo, "1");
    }

    private static class RoundedPanel extends JPanel {
        private Color bgColor;
        private Color borderColor;
        private final int radius;

        public RoundedPanel(Color bgColor, int radius) {
            this.bgColor = bgColor;
            this.borderColor = new Color(210, 210, 210);
            this.radius = radius;
            setOpaque(false);
        }

        public void setBgColor(Color bgColor) {
            this.bgColor = bgColor;
        }

        public void setBorderColor(Color borderColor) {
            this.borderColor = borderColor;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
        }
    }

    private static class EstadoConexionRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);

            boolean conectado = value instanceof Boolean && (Boolean) value;
            label.setIcon(new EsferaIcon(conectado ? new Color(0, 180, 0) : new Color(220, 30, 30), 12));
            label.setToolTipText(conectado ? "Conectado" : "Desconectado");
            return label;
        }
    }

    private static class MensajePendienteRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);

            boolean pendiente = value instanceof Boolean && (Boolean) value;
            if (pendiente) {
                label.setIcon(new EsferaIcon(new Color(0, 180, 0), 10));
                label.setToolTipText("Mensaje nuevo");
            } else {
                label.setIcon(null);
                label.setToolTipText(null);
            }
            return label;
        }
    }

    private static class EsferaIcon implements Icon {
        private final Color color;
        private final int size;

        public EsferaIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y, size, size);
            g2.setColor(color.darker());
            g2.drawOval(x, y, size - 1, size - 1);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}