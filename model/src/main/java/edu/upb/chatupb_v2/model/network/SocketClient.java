/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.upb.chatupb_v2.model.network;

import edu.upb.chatupb_v2.model.network.message.Message;
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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SocketClient extends Thread {
    private final Socket socket;
    private final String ip;
    private final DataOutputStream dout;
    private final BufferedReader br;
    private final List<ChatEventListener> listeners = new ArrayList<>();

    public SocketClient(Socket socket) throws IOException {
        this.socket = socket;
        this.ip = socket.getInetAddress().getHostAddress();
        dout = new DataOutputStream(socket.getOutputStream());
        br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    }

    public SocketClient(String ip) throws IOException {
        this.socket = new Socket(ip, 1900);
        this.ip = ip;
        dout = new DataOutputStream(socket.getOutputStream());
        br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    }

    public String getIp() {
        return ip;
    }

    public void addChatEventListener(ChatEventListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = br.readLine()) != null) {
                String[] split = message.split(java.util.regex.Pattern.quote("|"));
                if(split.length == 0) continue;

                System.out.println("[Recibido de " + ip + "] Trama " + split[0] + ": " + message);

                switch (split[0]) {
                    case "001": {
                        Invitacion inv = Invitacion.parse(message);
                        // Avisamos a todos los listeners que llegó una invitación
                        for (ChatEventListener listener : listeners) {
                            listener.onInvitacionRecibida(inv, this);
                        }
                        break;
                    }
                    case "002": {
                        AceptacionInvitacion acc = AceptacionInvitacion.parse(message);
                        // Avisamos a todos los listeners que aceptaron nuestra invitación
                        for (ChatEventListener listener : listeners) {
                            listener.onAceptacionRecibida(acc, this);
                        }
                        break;
                    }
                    case "003": {
                        RechazoInvitacion rechazo = RechazoInvitacion.parse(message);
                        for (ChatEventListener listener : listeners) {
                            listener.onRechazoRecibido(rechazo, this);
                        }
                        break;
                    }
                    case "004": {
                        Hello hello = Hello.parse(message);
                        for (ChatEventListener listener : listeners) {
                            listener.onHelloRecibido(hello, this);
                        }
                        break;
                    }
                    case "005": {
                        HelloResponse response = HelloResponse.parse(message);
                        for (ChatEventListener listener : listeners) {
                            listener.onHelloResponseRecibido(response, this);
                        }
                        break;
                    }
                    case "006": {
                        HelloRechazo helloRechazo = HelloRechazo.parse(message);
                        for (ChatEventListener listener : listeners) {
                            listener.onHelloRechazoRecibido(helloRechazo, this);
                        }
                        break;
                    }
                    case "007": {
                        EnvioMensaje msg = EnvioMensaje.parse(message);
                        // Avisamos a todos los listeners que llegó un mensaje de chat
                        for (ChatEventListener listener : listeners) {
                            listener.onMensajeRecibido(msg, this);
                        }
                        break;
                    }
                    case "008": {
                        ConfirmacionMensaje conf = ConfirmacionMensaje.parse(message);
                        // Avisamos a todos los listeners que el mensaje fue confirmado
                        for (ChatEventListener listener : listeners) {
                            listener.onConfirmacionRecibida(conf, this);
                        }
                        break;
                    }
                    case "009": {
                        EliminacionMensaje elim = EliminacionMensaje.parse(message);
                        for (ChatEventListener listener : listeners) {
                            listener.onEliminacionRecibida(elim, this);
                        }
                        break;
                    }
                    case "010": {
                        Zumbido zumbido = Zumbido.parse(message);
                        for (ChatEventListener listener : listeners) {
                            listener.onZumbidoRecibido(zumbido, this);
                        }
                        break;
                    }
                    case "011": {
                        edu.upb.chatupb_v2.model.network.message.FijarMensaje fijar = edu.upb.chatupb_v2.model.network.message.FijarMensaje.parse(message);
                        for (ChatEventListener listener : listeners) {
                            listener.onFijarMensajeRecibido(fijar, this);
                        }
                        break;
                    }
                    case "012": {
                        edu.upb.chatupb_v2.model.network.message.MensajeUnico msgUnico = edu.upb.chatupb_v2.model.network.message.MensajeUnico.parse(message);
                        for (ChatEventListener listener : listeners) {
                            listener.onMensajeUnicoRecibido(msgUnico, this);
                        }
                        break;
                    }
                    case "013": {
                        edu.upb.chatupb_v2.model.network.message.CambioTema cambio = edu.upb.chatupb_v2.model.network.message.CambioTema.parse(message);
                        for (ChatEventListener listener : listeners) {
                            listener.onCambioTemaRecibido(cambio, this);
                        }
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void send(String message) throws IOException {
        System.out.println("[Enviado a " + ip + "] Trama " + message.split(java.util.regex.Pattern.quote("|"))[0] + ": " + message);
        message = message + System.lineSeparator();
        try {
            dout.write(message.getBytes("UTF-8"));
            dout.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(Message message) throws IOException {
        send(message.generarTrama());
    }

    public void close() {
        try {
            this.socket.close();
            this.br.close();
            this.dout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
