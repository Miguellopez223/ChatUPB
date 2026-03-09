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

            // Si la palabra contiene "+" es una suma como "2+2"
            if (palabra.contains("+")) {
                String[] partes = palabra.split("\\+");
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

                // Solo reemplazar si tiene al menos 2 numeros y todos eran validos
                if (esSuma && partes.length >= 2) {
                    palabra = String.valueOf(suma);
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
