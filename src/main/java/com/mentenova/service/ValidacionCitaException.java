package com.mentenova.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ValidacionCitaException extends RuntimeException {

    public static final String GENERAL = "general";

    private final Map<String, String> errores;

    public ValidacionCitaException(Map<String, String> errores) {
        super("Datos de la cita no válidos: " + errores);
        this.errores = Collections.unmodifiableMap(new LinkedHashMap<>(errores));
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
