package edu.upb.chatupb_v2.view;

import edu.upb.chatupb_v2.model.entities.User;

/**
 * Interfaz que define qué puede hacer la Vista con el Controlador
 * sin conocer su implementación real.
 */
public interface IChatController {
    void onAppStart();
    void cambiarUsuario(User user);
    void crearNuevoUsuario();
    void guardarNuevoUsuario(String name);
    void enviarInvitacion(String ip, String miNombre);
    void enviarMensaje(String ip, String mensaje);
    void enviarMensajeUnico(String ip, String mensaje);
    void enviarZumbido(String ip);
    void enviarCambioTema(String ip, String idTema);
    void abrirChat(ContactInfo contacto);
    void fijarMensaje(String ip, String idMensaje);
    void desfijarMensaje(String ip, String idMensaje);
    void eliminarMensaje(String ip, String idMensaje);
    boolean isConectado(String ip);
    void abrirMensajeUnico(String ip, String idMensaje);
    void eliminarContacto(String id);
}