package com.mentenova.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoReporteDTO(
        Long idPago,
        Long idCita,
        LocalDateTime fechaPago,
        LocalDateTime fechaRegistro,
        String psicologo,
        BigDecimal monto,
        String medioPago,
        String estadoPago,
        String estadoCitaCodigo,
        String estadoCitaNombre) {
}
