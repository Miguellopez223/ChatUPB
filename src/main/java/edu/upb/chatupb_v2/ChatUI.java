package edu.upb.chatupb_v2;

import edu.upb.chatupb_v2.bl.message.AceptacionInvitacion;
import edu.upb.chatupb_v2.bl.message.ConfirmacionMensaje;
import edu.upb.chatupb_v2.bl.message.EnvioMensaje;
import edu.upb.chatupb_v2.bl.message.Invitacion;
import edu.upb.chatupb_v2.bl.message.RechazoInvitacion;
import edu.upb.chatupb_v2.bl.server.ChatEventListener;
import edu.upb.chatupb_v2.bl.server.Mediador;
import edu.upb.chatupb_v2.bl.server.SocketClient;

import edu.upb.chatupb_v2.repository.Contact;
import edu.upb.chatupb_v2.repository.ContactDao;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;

public class ChatUI extends JFrame implements ChatEventListener {

    // Mapa de IP -> nombre para mostrar en la UI
    private final HashMap<String, String> nombresConectados = new HashMap<>();

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
    private final ContactDao contactDao = new ContactDao();
    private java.util.List<Contact> contactosEnMemoria = new java.util.ArrayList<>();

    public ChatUI() {
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
        cargarContactos();
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
                    AceptacionInvitacion acc = new AceptacionInvitacion("ID_MIGUEL", miNombre);
                    Mediador.getInstancia().enviarMensaje(sender.getIp(), acc.generarTrama());

                    // 2. Registrar la conexión en la UI (sin sobrescribir conexiones anteriores)
                    agregarConexion(sender.getIp(), inv.getNombre());
                    areaChat.append("<- Has aceptado la invitación de " + inv.getNombre() + " (" + sender.getIp() + ")\n");

                    // 3. Guardar contacto en la base de datos
                    guardarContactoSiNoExiste(inv.getIdUsuario(), inv.getNombre(), sender.getIp());

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

            // Guardar contacto en la base de datos
            guardarContactoSiNoExiste(acc.getIdUsuario(), acc.getNombre(), sender.getIp());
        });
    }

    @Override
    public void onRechazoRecibido(RechazoInvitacion rechazo, SocketClient sender) {
        SwingUtilities.invokeLater(() -> {
            // Eliminar la conexión rechazada del Mediador
            Mediador.getInstancia().eliminar(sender.getIp());
            areaChat.append("<- " + sender.getIp() + " rechazó tu invitación (003).\n");
            actualizarEstado();
            refrescarEstadoContactos();
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

    // --- Métodos auxiliares para gestión de conexiones múltiples ---

    private void agregarConexion(String ip, String nombre) {
        nombresConectados.put(ip, nombre);
        String item = nombre + " (" + ip + ")";
        // Evitar duplicados en el combo
        for (int i = 0; i < modeloDestinatarios.getSize(); i++) {
            if (extraerIp(modeloDestinatarios.getElementAt(i)).equals(ip)) {
                actualizarEstado();
                refrescarEstadoContactos();
                return;
            }
        }
        modeloDestinatarios.addElement(item);
        actualizarEstado();
        refrescarEstadoContactos();
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

    // --- Métodos de gestión de contactos (SQLite) ---

    private void cargarContactos() {
        try {
            contactosEnMemoria = contactDao.findAll();
            modeloTablaContactos.setRowCount(0);
            for (Contact c : contactosEnMemoria) {
                boolean conectado = nombresConectados.containsKey(c.getIp());
                modeloTablaContactos.addRow(new Object[]{c.getName(), c.getIp(), conectado});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refrescarEstadoContactos() {
        for (int i = 0; i < contactosEnMemoria.size(); i++) {
            boolean conectado = nombresConectados.containsKey(contactosEnMemoria.get(i).getIp());
            modeloTablaContactos.setValueAt(conectado, i, 2);
        }
    }

    private void guardarContactoSiNoExiste(String idUsuario, String nombre, String ip) {
        try {
            if (!contactDao.existByCode(idUsuario)) {
                Contact contacto = Contact.builder()
                        .code(idUsuario)
                        .name(nombre)
                        .ip(ip)
                        .build();
                contactDao.save(contacto);
            } else {
                // Actualizar la IP si el contacto ya existe
                Contact contacto = contactDao.findByCode(idUsuario);
                contacto.setIp(ip);
                contactDao.update(contacto);
            }
            cargarContactos();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        try {
            contactDao.delete(contacto.getId());
            cargarContactos();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al eliminar contacto: " + e.getMessage());
        }
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
