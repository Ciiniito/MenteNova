package com.mentenova.dto.dashboard;

import java.time.YearMonth;
import java.util.List;

public record CalendarioDTO(YearMonth mes, String titulo, List<List<DiaCalendarioDTO>> semanas) {

    public YearMonth anterior() {
        return mes.minusMonths(1);
    }

    public YearMonth siguiente() {
        return mes.plusMonths(1);
    }
}
