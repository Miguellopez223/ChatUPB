package edu.upb.chatupb_v2.controller;

import edu.upb.chatupb_v2.model.analyzer.AnalizadorMatematicoImpl;
import edu.upb.chatupb_v2.model.analyzer.AnalizadorPalabrasVulgaresImpl;
import edu.upb.chatupb_v2.model.analyzer.IAnalizadorTexto;

/**
 * Actua como Contexto de la Strategy para el analisis de texto.
 * Selecciona y aplica la estrategia correspondiente segun el contenido del mensaje.
 *
 */

// ---------- PREGUNTA 4 --------------
public class AnalizadorController {

    public String procesarTexto(String texto) {
        IAnalizadorTexto estrategia;

        // Estrategia 1: Resolver expresiones matematicas (sumas)
        estrategia = new AnalizadorMatematicoImpl();
        texto = estrategia.analizar(texto);

        // Estrategia 2: Ofuscar palabras vulgares
        estrategia = new AnalizadorPalabrasVulgaresImpl();
        texto = estrategia.analizar(texto);

        return texto;
    }
}
