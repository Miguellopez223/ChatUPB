package edu.upb.chatupb_v2.controller;

import edu.upb.chatupb_v2.model.entities.Cobro;
import edu.upb.chatupb_v2.model.entities.FormaPago;
import edu.upb.chatupb_v2.model.payment.CobroCryptoImpl;
import edu.upb.chatupb_v2.model.payment.CobroFIATImpl;
import edu.upb.chatupb_v2.model.payment.ICobro;

public class CobroController {

     // Actúa como Fachada y Contexto de la Estrategy.
    public Cobro procesarCobro(double monto, FormaPago formaPago) {
        ICobro estrategiaDeCobro;

        // Decidir la estrategia en función del Enum
        if (formaPago == FormaPago.CRYPTO) {
            estrategiaDeCobro = new CobroCryptoImpl();
        } else {
            estrategiaDeCobro = new CobroFIATImpl();
        }

        // Ejecutar el método polimórfico
        return estrategiaDeCobro.cobrar(monto);
    }
}