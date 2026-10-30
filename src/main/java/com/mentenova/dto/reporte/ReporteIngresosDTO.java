package com.mentenova.dto.reporte;

import java.util.List;

public record ReporteIngresosDTO(
        TotalesPagosDTO totales,
        List<MedioPagoDTO> porMedioPago,
        List<PagoReporteDTO> detalle,
        boolean detalleTruncado) {
}
