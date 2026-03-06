package edu.upb.chatupb_v2.model.entities;

public class Cobro {
    private String imagenQr; // Simular imagen con un String
    private double importe;
    private String red;      // Nulo en FIAT, "Base", "Polygon", etc. en Crypto

    public Cobro(String imagenQr, double importe, String red) {
        this.imagenQr = imagenQr;
        this.importe = importe;
        this.red = red;
    }

    public String getImagenQr() {
        return imagenQr;
    }

    public double getImporte() {
        return importe;
    }

    public String getRed() {
        return red;
    }
}