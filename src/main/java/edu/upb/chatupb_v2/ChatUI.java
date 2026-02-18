package edu.upb.chatupb_v2;

import edu.upb.chatupb_v2.bl.message.AceptacionInvitacion;
import edu.upb.chatupb_v2.bl.message.Invitacion;
import edu.upb.chatupb_v2.bl.server.ChatEventListener;
import edu.upb.chatupb_v2.bl.server.SocketClient;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ChatUI extends JFrame implements ChatEventListener {

    private SocketClient clienteActivo;

    // Componentes visuales
    private JTextField txtIpDestino;
    private JTextField txtMiNombre;
    private JButton btnEnviarInvitacion;
    private JLabel lblEstado;

    private JTextArea areaChat;
    private JTextField txtMensaje;
    private JButton btnEnviarMensaje;

    public ChatUI() {
        configurarVentana();
    }

    private void configurarVentana() {
        setTitle("Chat P2P - Diseño de Patrones UPB");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 500);
        setLocationRelativeTo(null); // Centrar en pantalla
        setLayout(new BorderLayout(10, 10));

        // --- 1. PANEL SUPERIOR: Conexión e Invitaciones ---
        JPanel panelConexion = new JPanel(new GridLayout(3, 2, 5, 5));
        panelConexion.setBorder(new TitledBorder("1. Enviar Invitación (Trama 001)"));

        panelConexion.add(new JLabel("Mi Nombre:"));
        txtMiNombre = new JTextField("Estudiante"); // Puedes cambiarlo por defecto
        panelConexion.add(txtMiNombre);

        panelConexion.add(new JLabel("IP Destino:"));
        txtIpDestino = new JTextField("127.0.0.1"); // Localhost por defecto para pruebas
        panelConexion.add(txtIpDestino);

        lblEstado = new JLabel("Estado: Desconectado");
        lblEstado.setForeground(Color.RED);
        panelConexion.add(lblEstado);

        btnEnviarInvitacion = new JButton("Enviar Invitación");
        panelConexion.add(btnEnviarInvitacion);

        // --- 2. PANEL CENTRAL: Área de Chat ---
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        JScrollPane scrollChat = new JScrollPane(areaChat);
        scrollChat.setBorder(new TitledBorder("2. Conversación"));

        // --- 3. PANEL INFERIOR: Enviar Mensajes ---
        JPanel panelMensaje = new JPanel(new BorderLayout(5, 5));
        panelMensaje.setBorder(new TitledBorder("3. Enviar Mensaje de Texto"));
        txtMensaje = new JTextField();
        btnEnviarMensaje = new JButton("Enviar");
        btnEnviarMensaje.setEnabled(false); // Bloqueado hasta que acepten la invitación

        panelMensaje.add(txtMensaje, BorderLayout.CENTER);
        panelMensaje.add(btnEnviarMensaje, BorderLayout.EAST);

        // --- AGREGAR PANELES A LA VENTANA ---
        add(panelConexion, BorderLayout.NORTH);
        add(scrollChat, BorderLayout.CENTER);
        add(panelMensaje, BorderLayout.SOUTH);

        // --- CONFIGURAR BOTONES ---
        btnEnviarInvitacion.addActionListener(e -> enviarInvitacion());
        btnEnviarMensaje.addActionListener(e -> enviarMensajeChat());
    }

    // ACCIÓN: Cuando hacemos clic en "Enviar Invitación"
    private void enviarInvitacion() {
        try {
            String ip = txtIpDestino.getText().trim();
            String miNombre = txtMiNombre.getText().trim();

            // 1. Conectamos al socket destino
            clienteActivo = new SocketClient(ip);
            clienteActivo.addChatEventListener(this); // Escuchamos su respuesta
            clienteActivo.start();

            // 2. Armamos y enviamos la trama 001
            Invitacion inv = new Invitacion("ID_MI_PC", miNombre);
            clienteActivo.send(inv.generarTrama());

            // 3. Actualizamos la interfaz
            lblEstado.setText("Estado: Esperando respuesta...");
            lblEstado.setForeground(Color.ORANGE);
            areaChat.append("-> Invitación (001) enviada a " + ip + "\n");
            btnEnviarInvitacion.setEnabled(false); // Evitar doble clic

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al conectar a la IP: " + ex.getMessage());
            lblEstado.setText("Estado: Error de conexión");
        }
    }

    // ACCIÓN: Cuando enviamos un mensaje en el chat
    private void enviarMensajeChat() {
        if (clienteActivo != null) {
            try {
                String msg = txtMensaje.getText().trim();
                if (!msg.isEmpty()) {
                    // OJO: Por ahora envía texto plano. Pronto necesitaremos la trama 003.
                    clienteActivo.send(msg);
                    areaChat.append("Yo: " + msg + "\n");
                    txtMensaje.setText("");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // ======================================================================
    // IMPLEMENTACIÓN DE LOS EVENTOS DE RED (Llegan desde SocketClient)
    // ======================================================================

    @Override
    public void onInvitacionRecibida(Invitacion inv, SocketClient sender) {
        // SwingUtilities obliga a que este código modifique la interfaz de forma segura
        SwingUtilities.invokeLater(() -> {
            lblEstado.setText("Estado: Recibiendo invitación...");
            lblEstado.setForeground(Color.BLUE);

            int respuesta = JOptionPane.showConfirmDialog(
                    this,
                    "El usuario '" + inv.getNombre() + "' te ha enviado una invitación.\n¿Aceptas conectarte?",
                    "Invitación Recibida (Trama 001)",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
            );

            if (respuesta == JOptionPane.YES_OPTION) {
                try {
                    // 1. Armar y enviar trama 002 (Aceptación)
                    String miNombre = txtMiNombre.getText().trim();
                    AceptacionInvitacion acc = new AceptacionInvitacion("ID_MI_PC", miNombre);
                    sender.send(acc.generarTrama());

                    // 2. Guardar el socket y habilitar el chat
                    clienteActivo = sender;
                    lblEstado.setText("Estado: Conectado con " + inv.getNombre());
                    lblEstado.setForeground(new Color(0, 153, 0)); // Verde oscuro
                    btnEnviarMensaje.setEnabled(true);
                    areaChat.append("<- Has aceptado la invitación de " + inv.getNombre() + "\n");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                lblEstado.setText("Estado: Desconectado");
                lblEstado.setForeground(Color.RED);
                areaChat.append("- Rechazaste la invitación de " + inv.getNombre() + "\n");
            }
        });
    }

    @Override
    public void onAceptacionRecibida(AceptacionInvitacion acc, SocketClient sender) {
        SwingUtilities.invokeLater(() -> {
            // Guardar el socket validado y habilitar el chat
            clienteActivo = sender;
            lblEstado.setText("Estado: Conectado con " + acc.getNombre());
            lblEstado.setForeground(new Color(0, 153, 0)); // Verde oscuro
            btnEnviarMensaje.setEnabled(true);
            areaChat.append("<- " + acc.getNombre() + " aceptó tu invitación (002). ¡Ya pueden hablar!\n");
        });
    }
}