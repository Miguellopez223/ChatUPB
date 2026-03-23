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

    // ACTUALIZADO: Se añade el parametro boolean viewOnce al final
    void appendMensajeToContact(String ip, String content, boolean isMine, String idMensaje, boolean viewOnce);

    void actualizarCheckMensaje(String ip, String idMensaje);
    void actualizarBurbujaMensajeEliminado(String ip, String idMensaje);
    void abrirChatConContacto(ContactInfo contacto, List<ChatMessageInfo> historial);
    void clearChatHistory();

    // --- Pinned Message ---
    void mostrarMensajeFijado(String ip, ChatMessageInfo mensaje);
    void ocultarMensajeFijado(String ip);
    void marcarBurbujaFijada(String ip, String idMensaje);
    void desmarcarBurbujaFijada(String ip, String idMensaje);

    // --- UI State & Feedback ---
    boolean mostrarDialogoInvitacion(String nombre, String ip);
    void mostrarError(String mensaje);
    void agregarConexionUI(String ip, String nombre);
    void limpiarConexionesUI();
    void actualizarEstado(int numConexiones);
    void refrescarEstadoContactos();
    void actualizarEstadoInvitacion(String ip);
    void limpiarMensaje();
    void mostrarZumbido(String ip, String nombreContacto);
    String getMiNombre();
    String getContactoActivo();

    // --- Theme ---
    void aplicarTema(String ip, String idTema);

    // --- Connection State ---
    void notificarDesconexion(String ip);
    void mostrarIndicadorMensaje(String ip);
    void ocultarIndicadorMensaje(String ip);
}