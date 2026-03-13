package edu.upb.chatupb_v2.view;

import edu.upb.chatupb_v2.controller.ChatController;
import edu.upb.chatupb_v2.controller.ContactController;
import edu.upb.chatupb_v2.model.entities.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Base64;
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

    // Mapa de idMensaje -> JLabel de checks para actualizar de ✓ a ✓✓
    private final HashMap<String, JLabel> checkLabels = new HashMap<>();

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

        // --- TOP PANEL WRAPPER ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(userPanel, BorderLayout.NORTH);
        topPanel.add(panelConexion, BorderLayout.CENTER);

        // --- 2. PANEL CENTRAL: Area de Chat (intercambiable por contacto) ---
        areaChat = new JTextArea("Selecciona un usuario para comenzar.\n");
        areaChat.setEditable(false);
        // Inicialmente mostramos areaChat (logs globales)
        scrollChatActual = new JScrollPane(areaChat);
        scrollChatActual.setBorder(new TitledBorder("2. Conversacion"));
        scrollChatActual.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollChatActual.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // --- 3. PANEL INFERIOR: Seleccion de destinatario y envio de mensajes ---
        JPanel panelMensaje = new JPanel(new BorderLayout(5, 5));
        panelMensaje.setBorder(new TitledBorder("3. Enviar Mensaje de Texto / Imagen (Trama 007 / 021)"));

        JPanel panelDestinatario = new JPanel(new BorderLayout(5, 0));
        panelDestinatario.add(new JLabel("Para: "), BorderLayout.WEST);
        modeloDestinatarios = new DefaultComboBoxModel<>();
        comboDestinatarios = new JComboBox<>(modeloDestinatarios);
        panelDestinatario.add(comboDestinatarios, BorderLayout.CENTER);

        JPanel panelInput = new JPanel(new BorderLayout(5, 0));
        txtMensaje = new JTextField();
        btnEnviarMensaje = new JButton("Enviar");
        btnEnviarMensaje.setEnabled(false);
        JButton btnEnviarImagen = new JButton("Imagen");
        btnEnviarImagen.setToolTipText("Enviar imagen pequena (Trama 021 - Command Pattern)");

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        panelBotones.add(btnEnviarImagen);
        panelBotones.add(btnEnviarMensaje);

        panelInput.add(txtMensaje, BorderLayout.CENTER);
        panelInput.add(panelBotones, BorderLayout.EAST);

        panelMensaje.add(panelDestinatario, BorderLayout.NORTH);
        panelMensaje.add(panelInput, BorderLayout.CENTER);

        // --- 4. PANEL IZQUIERDO: Tabla de Contactos ---
        JPanel panelContactos = new JPanel(new BorderLayout(5, 5));
        panelContactos.setBorder(new TitledBorder("Contactos (doble click para chatear)"));
        panelContactos.setPreferredSize(new Dimension(250, 0));

        modeloTablaContactos = new DefaultTableModel(new String[]{"Nombre", "IP", "Estado"}, 0) {
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
        tablaContactos.getColumnModel().getColumn(2).setPreferredWidth(50);

        tablaContactos.getColumnModel().getColumn(2).setCellRenderer(new EstadoConexionRenderer());

        JScrollPane scrollContactos = new JScrollPane(tablaContactos);
        panelContactos.add(scrollContactos, BorderLayout.CENTER);

        JButton btnEliminarContacto = new JButton("Eliminar");
        panelContactos.add(btnEliminarContacto, BorderLayout.SOUTH);

        // --- AGREGAR PANELES A LA VENTANA ---
        add(panelContactos, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);
        add(scrollChatActual, BorderLayout.CENTER);
        add(panelMensaje, BorderLayout.SOUTH);

        // --- CONFIGURAR BOTONES ---
        userComboBox.addActionListener(e -> {
            User selectedUser = (User) userComboBox.getSelectedItem();
            if (selectedUser != null) {
                chatController.cambiarUsuario(selectedUser);
            }
        });
        btnAddUser.addActionListener(e -> chatController.crearNuevoUsuario());
        btnEnviarInvitacion.addActionListener(e -> enviarInvitacion());
        btnEnviarMensaje.addActionListener(e -> enviarMensajeChat());
        btnEnviarImagen.addActionListener(e -> seleccionarYEnviarImagen());
        btnEliminarContacto.addActionListener(e -> eliminarContacto());

        txtMensaje.addActionListener(e -> enviarMensajeChat());

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

    /**
     * Abre un JFileChooser para seleccionar una imagen pequena,
     * la redimensiona si es necesario, la codifica en Base64
     * y la envia usando la trama 021 (Patron Command).
     */
    private void seleccionarYEnviarImagen() {
        String ip = obtenerIpDestinatario();
        if (ip == null) {
            mostrarError("Selecciona un destinatario.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar imagen para enviar (Trama 021)");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Imagenes (PNG, JPG, GIF)", "png", "jpg", "jpeg", "gif"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fileChooser.getSelectedFile();
        try {
            BufferedImage originalImage = ImageIO.read(file);
            if (originalImage == null) {
                mostrarError("No se pudo leer la imagen seleccionada.");
                return;
            }

            // Redimensionar si es mayor a 200x200 para mantener imagenes pequenas
            BufferedImage resized = redimensionarImagen(originalImage, 200, 200);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resized, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            // Limite de seguridad: ~100KB en Base64
            if (imageBytes.length > 75000) {
                mostrarError("La imagen es demasiado grande. Selecciona una imagen mas pequena.");
                return;
            }

            chatController.enviarImagen(ip, imageBytes);
        } catch (Exception ex) {
            ex.printStackTrace();
            mostrarError("Error al procesar la imagen: " + ex.getMessage());
        }
    }

    private BufferedImage redimensionarImagen(BufferedImage original, int maxWidth, int maxHeight) {
        int w = original.getWidth();
        int h = original.getHeight();
        if (w <= maxWidth && h <= maxHeight) return original;

        double scale = Math.min((double) maxWidth / w, (double) maxHeight / h);
        int newW = (int) (w * scale);
        int newH = (int) (h * scale);

        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(original, 0, 0, newW, newH, null);
        g2.dispose();
        return resized;
    }

    private String obtenerIpDestinatario() {
        String itemSeleccionado = (String) comboDestinatarios.getSelectedItem();
        String ip = null;
        if (itemSeleccionado != null) {
            ip = extraerIp(itemSeleccionado);
        }
        if (ip == null && contactoActivo != null) {
            ip = contactoActivo;
        }
        return ip;
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
            modeloTablaContactos.addRow(new Object[]{c.getName(), c.getIp(), conectado});
        }
    }

    @Override
    public void appendChat(String texto) {
        // Logs globales van al JTextArea por defecto
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
            }
        } else {
            lblEstado.setText("Estado: " + numConexiones + " conexion(es) activa(s)");
            lblEstado.setForeground(new Color(0, 153, 0));
            btnEnviarMensaje.setEnabled(true);
        }
    }

    @Override
    public void refrescarEstadoContactos() {
        for (int i = 0; i < contactosEnMemoria.size(); i++) {
            boolean conectado = chatController.isConectado(contactosEnMemoria.get(i).getIp());
            modeloTablaContactos.setValueAt(conectado, i, 2);
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

    // --- MANEJO DE BURBUJAS DE CHAT ---

    private JPanel getOrCreateChatPanel(String ip) {
        return chatPanels.computeIfAbsent(ip, k -> {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBackground(new Color(240, 240, 240)); // Fondo gris claro
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
            String time = sdf.format(new Date(msg.getTimestamp()));
            String idMensaje = msg.isMine() ? String.valueOf(msg.getTimestamp()) : null;
            if (msg.isImage()) {
                addImageBubble(panel, msg.getContent(), time, msg.isMine(), msg.isConfirmed(), idMensaje);
            } else {
                addBubble(panel, msg.getContent(), time, msg.isMine(), msg.isConfirmed(), idMensaje);
            }
        }

        panel.add(Box.createVerticalGlue());

        scrollChatActual.setViewportView(panel);
        scrollChatActual.setBorder(new TitledBorder("Chat con " + contacto.getName()));

        selectDestinatarioPorIp(contacto.getIp());
        btnEnviarMensaje.setEnabled(true);

        scrollToBottom(panel);
    }

    @Override
    public void appendChatToContact(String ip, String texto) {
        JPanel panel = getOrCreateChatPanel(ip);

        String cleanText = texto.trim();
        if (cleanText.isEmpty()) return;

        // Solo para mensajes de sistema ahora
        addSystemLabel(panel, cleanText);

        if (ip.equals(contactoActivo)) {
            scrollChatActual.setViewportView(panel);
            scrollToBottom(panel);
        }
    }

    @Override
    public void appendMensajeToContact(String ip, String content, boolean isMine, String idMensaje) {
        JPanel panel = getOrCreateChatPanel(ip);
        String time = new SimpleDateFormat("HH:mm").format(new Date());

        addBubble(panel, content, time, isMine, false, idMensaje);

        if (ip.equals(contactoActivo)) {
            scrollChatActual.setViewportView(panel);
            scrollToBottom(panel);
        }
    }

    @Override
    public void appendImagenToContact(String ip, String base64, boolean isMine, String idMensaje) {
        JPanel panel = getOrCreateChatPanel(ip);
        String time = new SimpleDateFormat("HH:mm").format(new Date());

        addImageBubble(panel, base64, time, isMine, false, idMensaje);

        if (ip.equals(contactoActivo)) {
            scrollChatActual.setViewportView(panel);
            scrollToBottom(panel);
        }
    }

    @Override
    public void actualizarCheckMensaje(String ip, String idMensaje) {
        JLabel checkLabel = checkLabels.get(idMensaje);
        if (checkLabel != null) {
            checkLabel.setText("\u2713\u2713");
            checkLabel.setForeground(new Color(53, 147, 234)); // Azul tipo WhatsApp
            checkLabel.repaint();
        }
    }
    
    private void addBubble(JPanel panel, String text, String time, boolean isMine, boolean confirmed, String idMensaje) {
        JPanel rowWrapper = new JPanel(new BorderLayout());
        rowWrapper.setOpaque(false);
        rowWrapper.setBorder(new EmptyBorder(3, 0, 3, 0));

        // Burbuja con bordes redondeados
        Color bgColor = isMine ? new Color(212, 245, 212) : Color.WHITE;
        JPanel bubble = new RoundedPanel(bgColor, 14);
        bubble.setLayout(new BorderLayout());

        // Escapar HTML
        String escapedText = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");

        String widthStyle = text.length() < 30 ? "" : "width: 250px; ";
        String html = "<html><body style='" + widthStyle + "font-family: Segoe UI, sans-serif; font-size: 13px;'>"
                + escapedText + "</body></html>";

        JLabel messageLabel = new JLabel(html);
        messageLabel.setBorder(new EmptyBorder(8, 12, 4, 12));

        // Panel inferior: hora + checks
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(0, 0, 6, 8));

        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(new Color(140, 140, 140));
        bottomPanel.add(timeLabel);

        // Checks solo para mensajes enviados (mios)
        if (isMine) {
            JLabel checkLabel = new JLabel();
            checkLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            if (confirmed) {
                checkLabel.setText("\u2713\u2713"); // ✓✓
                checkLabel.setForeground(new Color(53, 147, 234)); // Azul
            } else {
                checkLabel.setText("\u2713"); // ✓
                checkLabel.setForeground(new Color(140, 140, 140)); // Gris
            }
            bottomPanel.add(checkLabel);

            // Registrar para actualizar luego cuando llegue 008
            if (idMensaje != null) {
                checkLabels.put(idMensaje, checkLabel);
            }
        }

        bubble.add(messageLabel, BorderLayout.CENTER);
        bubble.add(bottomPanel, BorderLayout.SOUTH);

        JPanel alignmentWrapper = new JPanel(new FlowLayout(isMine ? FlowLayout.RIGHT : FlowLayout.LEFT, 8, 0));
        alignmentWrapper.setOpaque(false);
        alignmentWrapper.add(bubble);

        rowWrapper.add(alignmentWrapper, BorderLayout.CENTER);

        panel.add(rowWrapper);
        panel.revalidate();
    }
    
    private void addImageBubble(JPanel panel, String base64, String time, boolean isMine, boolean confirmed, String idMensaje) {
        JPanel rowWrapper = new JPanel(new BorderLayout());
        rowWrapper.setOpaque(false);
        rowWrapper.setBorder(new EmptyBorder(3, 0, 3, 0));

        Color bgColor = isMine ? new Color(212, 245, 212) : Color.WHITE;
        JPanel bubble = new RoundedPanel(bgColor, 14);
        bubble.setLayout(new BorderLayout());

        // Decodificar Base64 a imagen
        JLabel imageLabel;
        try {
            byte[] imgBytes = Base64.getDecoder().decode(base64);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
            if (img != null) {
                imageLabel = new JLabel(new ImageIcon(img));
            } else {
                imageLabel = new JLabel("[Imagen no valida]");
            }
        } catch (Exception e) {
            imageLabel = new JLabel("[Error al decodificar imagen]");
        }
        imageLabel.setBorder(new EmptyBorder(8, 12, 4, 12));

        // Panel inferior: hora + checks
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(0, 0, 6, 8));

        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(new Color(140, 140, 140));
        bottomPanel.add(timeLabel);

        if (isMine) {
            JLabel checkLabel = new JLabel();
            checkLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            if (confirmed) {
                checkLabel.setText("\u2713\u2713");
                checkLabel.setForeground(new Color(53, 147, 234));
            } else {
                checkLabel.setText("\u2713");
                checkLabel.setForeground(new Color(140, 140, 140));
            }
            bottomPanel.add(checkLabel);

            if (idMensaje != null) {
                checkLabels.put(idMensaje, checkLabel);
            }
        }

        bubble.add(imageLabel, BorderLayout.CENTER);
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
                if (userComboBoxModel.getSize() == 0) System.exit(0); // Exit if no user and cancelled
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
    public void clearChatHistory() {
        chatPanels.clear();
        checkLabels.clear();
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

    /**
     * Panel con bordes redondeados para las burbujas de chat.
     */
    private static class RoundedPanel extends JPanel {
        private final Color bgColor;
        private final int radius;

        public RoundedPanel(Color bgColor, int radius) {
            this.bgColor = bgColor;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            // Borde sutil
            g2.setColor(new Color(210, 210, 210));
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
