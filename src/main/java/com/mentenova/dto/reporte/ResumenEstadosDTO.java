package com.mentenova.dto.reporte;

import java.util.List;

public record ResumenEstadosDTO(long total, List<ConteoEstadoDTO> estados) {

    public static ResumenEstadosDTO de(List<ConteoEstadoDTO> estados) {
        return new ResumenEstadosDTO(estados.stream().mapToLong(ConteoEstadoDTO::total).sum(), estados);
    }

    public long cantidad(String codigo) {
        return estados.stream()
                .filter(e -> e.codigo().equals(codigo))
                .mapToLong(ConteoEstadoDTO::total)
                .findFirst()
                .orElse(0);
    }
}
