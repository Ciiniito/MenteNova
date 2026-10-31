package com.mentenova.service;

public class SesionInvalidaException extends RuntimeException {

    public SesionInvalidaException() {
        super("La sesión ya no corresponde a un usuario activo");
    }
}
