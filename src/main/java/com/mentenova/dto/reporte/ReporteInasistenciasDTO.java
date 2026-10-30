package com.mentenova.dto.reporte;

import java.util.List;

public record ReporteInasistenciasDTO(
        TasasDTO global,
        String agrupacion,
        List<TasasDTO> tendencia,
        List<TasasDTO> porPsicologo) {
}
