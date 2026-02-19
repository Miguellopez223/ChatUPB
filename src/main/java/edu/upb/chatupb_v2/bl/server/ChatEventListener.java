package edu.upb.chatupb_v2.bl.server;

import edu.upb.chatupb_v2.bl.message.AceptacionInvitacion;
import edu.upb.chatupb_v2.bl.message.ConfirmacionMensaje;
import edu.upb.chatupb_v2.bl.message.EnvioMensaje;
import edu.upb.chatupb_v2.bl.message.Invitacion;
import edu.upb.chatupb_v2.bl.message.RechazoInvitacion;

public interface ChatEventListener {
    void onInvitacionRecibida(Invitacion inv, SocketClient sender);
    void onAceptacionRecibida(AceptacionInvitacion acc, SocketClient sender);
    void onRechazoRecibido(RechazoInvitacion rechazo, SocketClient sender);
    void onMensajeRecibido(EnvioMensaje msg, SocketClient sender);
    void onConfirmacionRecibida(ConfirmacionMensaje conf, SocketClient sender);
}