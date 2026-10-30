package com.mentenova.dto.reporte;

import java.math.BigDecimal;

public record TotalesPagosDTO(
        BigDecimal ingresoTotalRegistrado,
        BigDecimal ingresoPagado,
        BigDecimal montoPendiente,
        BigDecimal montoAnulado,
        long citasAtendidasConPago) {
}
