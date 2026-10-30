package com.mentenova.dto.reporte;

public record DistribucionPsicologoDTO(
        Long idPsicologo,
        String psicologo,
        long total,
        long pendientes,
        long confirmadas,
        long atendidas,
        long canceladas,
        long reprogramadas,
        long noAsistio) {
}
