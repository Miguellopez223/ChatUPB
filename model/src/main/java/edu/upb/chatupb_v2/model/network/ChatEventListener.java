package edu.upb.chatupb_v2.model.network;

import edu.upb.chatupb_v2.model.network.message.AceptacionInvitacion;
import edu.upb.chatupb_v2.model.network.message.ConfirmacionMensaje;
import edu.upb.chatupb_v2.model.network.message.EliminacionMensaje;
import edu.upb.chatupb_v2.model.network.message.EnvioMensaje;
import edu.upb.chatupb_v2.model.network.message.Hello;
import edu.upb.chatupb_v2.model.network.message.HelloRechazo;
import edu.upb.chatupb_v2.model.network.message.HelloResponse;
import edu.upb.chatupb_v2.model.network.message.Invitacion;
import edu.upb.chatupb_v2.model.network.message.RechazoInvitacion;
import edu.upb.chatupb_v2.model.network.message.Zumbido;
import edu.upb.chatupb_v2.model.network.message.FijarMensaje;
import edu.upb.chatupb_v2.model.network.message.CambioTema;

public interface ChatEventListener {
    void onInvitacionRecibida(Invitacion inv, SocketClient sender);
    void onAceptacionRecibida(AceptacionInvitacion acc, SocketClient sender);
    void onRechazoRecibido(RechazoInvitacion rechazo, SocketClient sender);
    void onHelloRecibido(Hello hello, SocketClient sender);
    void onHelloResponseRecibido(HelloResponse response, SocketClient sender);
    void onHelloRechazoRecibido(HelloRechazo rechazo, SocketClient sender);
    void onMensajeRecibido(EnvioMensaje msg, SocketClient sender);
    void onConfirmacionRecibida(ConfirmacionMensaje conf, SocketClient sender);
    void onEliminacionRecibida(EliminacionMensaje elim, SocketClient sender);
    void onZumbidoRecibido(Zumbido zumbido, SocketClient sender);
    void onFijarMensajeRecibido(FijarMensaje fijar, SocketClient sender);
    void onMensajeUnicoRecibido(edu.upb.chatupb_v2.model.network.message.MensajeUnico msg, SocketClient sender);
    void onCambioTemaRecibido(CambioTema cambio, SocketClient sender);
}
