package com.mentenova.dto.reporte;

import java.util.List;

public record ReporteCitasDTO(
        ResumenEstadosDTO resumen,
        List<DistribucionPsicologoDTO> porPsicologo,
        List<CitaReporteDTO> detalle,
        boolean detalleTruncado) {
}
