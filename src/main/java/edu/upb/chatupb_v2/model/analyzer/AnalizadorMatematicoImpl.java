package edu.upb.chatupb_v2.model.analyzer;

// ---------- PREGUNTA 4 ----------------

public class AnalizadorMatematicoImpl implements IAnalizadorTexto {

    @Override
    public String analizar(String texto) {
        // Separar el texto en palabras por espacios
        String[] palabras = texto.split(" ");
        String resultado = "";

        for (int i = 0; i < palabras.length; i++) {
            String palabra = palabras[i];

            // Si la palabra contiene "+" y termina con "=" es una suma como "2+2="
            if (palabra.contains("+") && palabra.endsWith("=")) {
                // Quitar el "=" del final para obtener solo la expresion
                String expresion = palabra.substring(0, palabra.length() - 1);
                String[] partes = expresion.split("\\+");
                boolean esSuma = true;
                int suma = 0;

                for (int j = 0; j < partes.length; j++) {
                    try {
                        suma += Integer.parseInt(partes[j]);
                    } catch (NumberFormatException e) {
                        esSuma = false;
                        break;
                    }
                }

                // Conservar la expresion original y agregar el resultado: "2+2=4"
                if (esSuma && partes.length >= 2) {
                    palabra = expresion + "=" + suma;
                }
            }

            if (i > 0) {
                resultado += " ";
            }
            resultado += palabra;
        }

        return resultado;
    }
}
