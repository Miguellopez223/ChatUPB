package edu.upb.chatupb_v2.view;

import edu.upb.chatupb_v2.model.entities.User;

import java.util.List;

public interface IChatView {

    // --- User Management ---
    void setUserList(List<User> users);
    void showNewUserDialog();
    void setScreenTitle(String title);

    // --- Contact List ---
    void onLoad(List<ContactInfo> contactos);

    // --- Chat Area ---
    void appendChat(String texto);
    void appendChatToContact(String ip, String texto);
    void appendMensajeToContact(String ip, String content, boolean isMine, String idMensaje);
    void actualizarCheckMensaje(String ip, String idMensaje);
    void actualizarBurbujaMensajeEliminado(String ip, String idMensaje);
    void abrirChatConContacto(ContactInfo contacto, List<ChatMessageInfo> historial);
    void clearChatHistory();

    // --- UI State & Feedback ---
    boolean mostrarDialogoInvitacion(String nombre, String ip);
    void mostrarError(String mensaje);
    void agregarConexionUI(String ip, String nombre);
    void limpiarConexionesUI(); // Nuevo metodo para limpiar conexiones visuales
    void actualizarEstado(int numConexiones);
    void refrescarEstadoContactos();
    void actualizarEstadoInvitacion(String ip);
    void limpiarMensaje();
    void mostrarZumbido(String ip, String nombreContacto);
    String getMiNombre();
    String getContactoActivo();
}
