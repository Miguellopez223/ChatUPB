package edu.upb.chatupb_v2.view;

import edu.upb.chatupb_v2.repository.Contact;

import java.util.List;

public interface IChatView {

    // Contactos
    void onLoad(List<Contact> contacts);

    // Chat
    void appendChat(String texto);

    boolean mostrarDialogoInvitacion(String nombre, String ip);

    void mostrarError(String mensaje);

    void agregarConexionUI(String ip, String nombre);

    void actualizarEstado(int numConexiones);

    void refrescarEstadoContactos();

    void actualizarEstadoInvitacion(String ip);

    void limpiarMensaje();

    String getMiNombre();
}
