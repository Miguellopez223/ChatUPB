package edu.upb.chatupb_v2;

/**
 * Clase lanzadora que NO extiende Application.
 * Necesaria para que jpackage y el fat JAR funcionen correctamente con JavaFX.
 */
public class Launcher {
    public static void main(String[] args) {
        ChatUPB_V2_FX.main(args);
    }
}
