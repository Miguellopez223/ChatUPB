package edu.upb.chatupb_v2.bl.server;

import edu.upb.chatupb_v2.bl.message.AceptacionInvitacion;
import edu.upb.chatupb_v2.bl.message.Invitacion;

public interface ChatEventListener {
    void onInvitacionRecibida(Invitacion inv, SocketClient sender);
    void onAceptacionRecibida(AceptacionInvitacion acc, SocketClient sender);
}