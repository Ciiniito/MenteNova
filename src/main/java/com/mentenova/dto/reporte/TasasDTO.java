package com.mentenova.dto.reporte;

public record TasasDTO(
        String etiqueta,
        long registradas,
        long atendidas,
        long noAsistio,
        long canceladas,
        Double tasaInasistencia,
        Double tasaCancelacion) {

    public static TasasDTO de(String etiqueta, long registradas, long atendidas, long noAsistio, long canceladas) {
        return new TasasDTO(etiqueta, registradas, atendidas, noAsistio, canceladas,
                porcentaje(noAsistio, atendidas + noAsistio),
                porcentaje(canceladas, registradas));
    }

    private static Double porcentaje(long parte, long total) {
        return total == 0 ? null : Math.round(parte * 1000.0 / total) / 10.0;
    }
}
