package com.mentenova.dto.reporte;

import java.math.BigDecimal;

public record MedioPagoDTO(String medioPago, long cantidad, BigDecimal monto, double porcentaje) {
}
