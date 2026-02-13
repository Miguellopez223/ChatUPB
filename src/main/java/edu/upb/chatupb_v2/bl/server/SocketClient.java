/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.upb.chatupb_v2.bl.server;

import edu.upb.chatupb_v2.bl.message.AceptacionInvitacion;
import edu.upb.chatupb_v2.bl.message.Invitacion;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.regex.Pattern;

/**
 * @author rlaredo
 */
public class SocketClient extends Thread {
    private final Socket socket;
    private final String ip;
    private final DataOutputStream dout;
    private final BufferedReader br;
    private ChatEventListener listener;

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

    public void setChatEventListener(ChatEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = br.readLine()) != null) {
                String[] split = message.split(java.util.regex.Pattern.quote("|"));
                if(split.length == 0) continue;

                switch (split[0]) {
                    case "001": {
                        Invitacion inv = Invitacion.parse(message);
                        // Avisamos a la UI que llegó una invitación
                        if (listener != null) {
                            listener.onInvitacionRecibida(inv, this);
                        }
                        break;
                    }
                    case "002": {
                        AceptacionInvitacion acc = AceptacionInvitacion.parse(message);
                        // Avisamos a la UI que aceptaron nuestra invitación
                        if (listener != null) {
                            listener.onAceptacionRecibida(acc, this);
                        }
                        //TODO: Guardar el contacto en la BD y habilitar el chat
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void send(String message) throws IOException {
        message = message + System.lineSeparator();
        try {
            dout.write(message.getBytes("UTF-8"));
            dout.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
