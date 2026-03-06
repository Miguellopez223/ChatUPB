package edu.upb.chatupb_v2.model.payment;

import edu.upb.chatupb_v2.model.entities.Cobro;

public interface ICobro {
    Cobro cobrar(double monto);
}