package edu.upb.chatupb_v2.model.payment;

import edu.upb.chatupb_v2.model.entities.Cobro;
import java.util.Random;
import java.util.UUID;

public class CobroCryptoImpl implements ICobro {

    private final String[] redesDisponibles = {"Polygon", "Base"};

    @Override
    public Cobro cobrar(double monto) {
        String qrSimulado = "QR_CRYPTO_WALLET_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Elegir una red random para el ejemplo
        String redSeleccionada = redesDisponibles[new Random().nextInt(redesDisponibles.length)];

        return new Cobro(qrSimulado, monto, redSeleccionada);
    }
}