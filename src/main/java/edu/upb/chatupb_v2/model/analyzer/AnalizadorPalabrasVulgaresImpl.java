package edu.upb.chatupb_v2.model.analyzer;

// ------- PREGUNTA 4 ----------

public class AnalizadorPalabrasVulgaresImpl implements IAnalizadorTexto {

    private final String[] palabrasVulgares = {"carajo", "mierda", "cabron", "maricon"};

    @Override
    public String analizar(String texto) {
        String resultado = texto;

        for (int i = 0; i < palabrasVulgares.length; i++) {
            String vulgar = palabrasVulgares[i];

            // Construir los asteriscos del mismo largo que la palabra
            String asteriscos = "";
            for (int j = 0; j < vulgar.length(); j++) {
                asteriscos += "*";
            }

            // Buscar y reemplazar sin importar mayusculas/minusculas
            String resultadoLower = resultado.toLowerCase();
            while (resultadoLower.contains(vulgar)) {
                int pos = resultadoLower.indexOf(vulgar);
                resultado = resultado.substring(0, pos) + asteriscos + resultado.substring(pos + vulgar.length());
                resultadoLower = resultado.toLowerCase();
            }
        }

        return resultado;
    }
}
