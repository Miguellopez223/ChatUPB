package edu.upb.chatupb_v2;

import edu.upb.chatupb_v2.controller.ChatController;
import edu.upb.chatupb_v2.controller.ContactController;
import edu.upb.chatupb_v2.repository.Contact;
import edu.upb.chatupb_v2.view.IChatView;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ChatUI extends JFrame implements IChatView {

    // Componentes visuales
    private JTextField txtIpDestino;
    private JTextField txtMiNombre;
    private JButton btnEnviarInvitacion;
    private JLabel lblEstado;

    private JTextArea areaChat;
    private JTextField txtMensaje;
    private JButton btnEnviarMensaje;
    private DefaultComboBoxModel<String> modeloDestinatarios;
    private JComboBox<String> comboDestinatarios;

    // Tabla de contactos
    private DefaultTableModel modeloTablaContactos;
    private JTable tablaContactos;
    private final ContactController contactController;
    private final ChatController chatController;
    private List<Contact> contactosEnMemoria = new java.util.ArrayList<>();

    public ChatUI() {
        this.contactController = new ContactController(this);
        this.chatController = new ChatController(this, this.contactController);
        configurarVentana();
    }

    private void configurarVentana() {
        setTitle("Chat P2P - Diseño de Patrones UPB");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- 1. PANEL SUPERIOR: Conexión e Invitaciones ---
        JPanel panelConexion = new JPanel(new GridLayout(3, 2, 5, 5));
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

        // --- 4. PANEL IZQUIERDO: Tabla de Contactos ---
        JPanel panelContactos = new JPanel(new BorderLayout(5, 5));
        panelContactos.setBorder(new TitledBorder("Contactos"));
        panelContactos.setPreferredSize(new Dimension(250, 0));

        // Modelo de tabla no editable
        modeloTablaContactos = new DefaultTableModel(new String[]{"Nombre", "IP", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaContactos = new JTable(modeloTablaContactos);
        tablaContactos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaContactos.setRowHeight(24);

        // Ancho de columnas
        tablaContactos.getColumnModel().getColumn(0).setPreferredWidth(90);
        tablaContactos.getColumnModel().getColumn(1).setPreferredWidth(100);
        tablaContactos.getColumnModel().getColumn(2).setPreferredWidth(50);

        // Renderer personalizado para la columna "Estado" (esfera verde/roja)
        tablaContactos.getColumnModel().getColumn(2).setCellRenderer(new EstadoConexionRenderer());

        JScrollPane scrollContactos = new JScrollPane(tablaContactos);
        panelContactos.add(scrollContactos, BorderLayout.CENTER);

        JButton btnEliminarContacto = new JButton("Eliminar");
        panelContactos.add(btnEliminarContacto, BorderLayout.SOUTH);

        // --- AGREGAR PANELES A LA VENTANA ---
        add(panelContactos, BorderLayout.WEST);
        add(panelConexion, BorderLayout.NORTH);
        add(scrollChat, BorderLayout.CENTER);
        add(panelMensaje, BorderLayout.SOUTH);

        // --- CONFIGURAR BOTONES ---
        btnEnviarInvitacion.addActionListener(e -> enviarInvitacion());
        btnEnviarMensaje.addActionListener(e -> enviarMensajeChat());
        btnEliminarContacto.addActionListener(e -> eliminarContacto());

        // Al seleccionar un contacto en la tabla, auto-llenar la IP
        tablaContactos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tablaContactos.getSelectedRow();
                if (row >= 0 && row < contactosEnMemoria.size()) {
                    Contact c = contactosEnMemoria.get(row);
                    txtIpDestino.setText(c.getIp());
                }
            }
        });

        // Cargar contactos desde la base de datos
        contactController.onLoad();
    }

    public ChatController getChatController() {
        return chatController;
    }

    // --- Acciones delegadas a los controllers ---

    private void enviarInvitacion() {
        String ip = txtIpDestino.getText().trim();
        String miNombre = txtMiNombre.getText().trim();
        chatController.enviarInvitacion(ip, miNombre);
    }

    private void enviarMensajeChat() {
        String itemSeleccionado = (String) comboDestinatarios.getSelectedItem();
        if (itemSeleccionado == null) return;

        String ip = extraerIp(itemSeleccionado);
        String msg = txtMensaje.getText().trim();
        if (msg.isEmpty()) return;

        chatController.enviarMensaje(ip, msg);
    }

    private void eliminarContacto() {
        int row = tablaContactos.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un contacto para eliminar.");
            return;
        }
        Contact contacto = contactosEnMemoria.get(row);
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar a " + contacto.getName() + "?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        contactController.eliminar(contacto.getId());
    }

    // --- Implementaciones de IChatView ---

    @Override
    public void onLoad(List<Contact> contacts) {
        contactosEnMemoria = contacts;
        modeloTablaContactos.setRowCount(0);
        for (Contact c : contactosEnMemoria) {
            boolean conectado = chatController.isConectado(c.getIp());
            modeloTablaContactos.addRow(new Object[]{c.getName(), c.getIp(), conectado});
        }
    }

    @Override
    public void appendChat(String texto) {
        areaChat.append(texto);
    }

    @Override
    public boolean mostrarDialogoInvitacion(String nombre, String ip) {
        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "El usuario '" + nombre + "' (" + ip + ") te ha enviado una invitación.\n¿Aceptas conectarte?",
                "Invitación Recibida (Trama 001)",
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
        // Evitar duplicados en el combo
        for (int i = 0; i < modeloDestinatarios.getSize(); i++) {
            if (extraerIp(modeloDestinatarios.getElementAt(i)).equals(ip)) {
                return;
            }
        }
        modeloDestinatarios.addElement(item);
    }

    @Override
    public void actualizarEstado(int numConexiones) {
        if (numConexiones == 0) {
            lblEstado.setText("Estado: Sin conexiones activas");
            lblEstado.setForeground(Color.RED);
            btnEnviarMensaje.setEnabled(false);
        } else {
            lblEstado.setText("Estado: " + numConexiones + " conexión(es) activa(s)");
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
        lblEstado.setText("Estado: Invitación enviada a " + ip + "...");
        lblEstado.setForeground(Color.ORANGE);
    }

    @Override
    public void limpiarMensaje() {
        txtMensaje.setText("");
    }

    @Override
    public String getMiNombre() {
        return txtMiNombre.getText().trim();
    }

    // --- Utilidad ---

    private String extraerIp(String item) {
        int start = item.lastIndexOf('(');
        int end = item.lastIndexOf(')');
        return item.substring(start + 1, end);
    }

    // --- Renderer personalizado: esfera verde (conectado) / roja (desconectado) ---

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

    // Icono que dibuja una esfera (círculo relleno con borde)
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
