package edu.upb.chatupb_v2.model.payment;

import edu.upb.chatupb_v2.model.entities.Cobro;
import java.util.UUID;

public class CobroFIATImpl implements ICobro {
    @Override
    public Cobro cobrar(double monto) {
        // Generar un "QR" random (simulado con un texto)
        String qrSimulado = "QR_FIAT_BOB_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Retornar el cobro con la red en NULL
        return new Cobro(qrSimulado, monto, null);
    }
}