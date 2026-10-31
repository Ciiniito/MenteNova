package com.mentenova.dto.paciente;

public final class FormatoPaciente {

    private FormatoPaciente() {
    }

    public static String nombreCompleto(String nombres, String apellidos) {
        return (nombres + " " + apellidos).strip();
    }

    public static String iniciales(String nombres, String apellidos) {
        return inicial(nombres) + inicial(apellidos);
    }

    public static String documento(String tipo, String numero) {
        if (tipo == null || numero == null) {
            return null;
        }
        return switch (tipo) {
            case "CE" -> "CE " + numero;
            case "PASAPORTE" -> "Pasaporte " + numero;
            default -> tipo + " " + numero;
        };
    }

    private static String inicial(String texto) {
        return texto == null || texto.isBlank() ? "" : texto.strip().substring(0, 1).toUpperCase();
    }
}
