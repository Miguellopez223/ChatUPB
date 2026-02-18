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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author rlaredo
 */
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
