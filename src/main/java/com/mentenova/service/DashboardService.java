package com.mentenova.service;

import com.mentenova.dto.UsuarioSesionDTO;
import com.mentenova.dto.dashboard.CalendarioDTO;
import com.mentenova.dto.dashboard.DashboardDTO;
import com.mentenova.dto.dashboard.DiaCalendarioDTO;
import com.mentenova.dto.reporte.CitaReporteDTO;
import com.mentenova.dto.reporte.FiltroReporte;
import com.mentenova.repository.ReporteRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int LIMITE_PROXIMAS_CITAS = 8;
    private static final DateTimeFormatter FORMATO_MES =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-PE"));

    private final ReporteRepository reporteRepository;
    private final Clock reloj;

    public DashboardService(ReporteRepository reporteRepository, Clock reloj) {
        this.reporteRepository = reporteRepository;
        this.reloj = reloj;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'PACIENTE')")
    public DashboardDTO construir(UsuarioSesionDTO usuario, LocalDate desde, LocalDate hasta, YearMonth mes) {
        LocalDate hoy = LocalDate.now(reloj);
        LocalTime ahora = LocalTime.now(reloj);

        if (UsuarioSesionDTO.PACIENTE.equals(usuario.rol())) {
            Optional<Long> idPaciente = reporteRepository.buscarIdPaciente(usuario.idUsuario());
            List<CitaReporteDTO> proximas = idPaciente
                    .map(id -> reporteRepository.listarProximasCitas(hoy, ahora, null, id, LIMITE_PROXIMAS_CITAS))
                    .orElse(List.of());
            return new DashboardDTO(DashboardDTO.ALCANCE_PACIENTE, idPaciente.isPresent(), hoy,
                    null, proximas, null, null, null, null);
        }

        String alcance = DashboardDTO.ALCANCE_GENERAL;
        Long idPsicologo = null;

        YearMonth mesActual = YearMonth.from(hoy);
        FiltroReporte periodo = new FiltroReporte(desde, hasta, idPsicologo);
        periodo.completarPeriodo(mesActual.atDay(1), mesActual.atEndOfMonth());

        return new DashboardDTO(
                alcance,
                true,
                hoy,
                reporteRepository.resumirPorEstado(new FiltroReporte(hoy, hoy, idPsicologo)),
                reporteRepository.listarProximasCitas(hoy, ahora, idPsicologo, null, LIMITE_PROXIMAS_CITAS),
                construirCalendario(mes != null && FiltroReporte.admite(mes) ? mes : mesActual, hoy, idPsicologo),
                periodo.getDesde(),
                periodo.getHasta(),
                reporteRepository.resumirPorEstado(periodo));
    }

    private CalendarioDTO construirCalendario(YearMonth mes, LocalDate hoy, Long idPsicologo) {
        LocalDate inicio = mes.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fin = mes.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        Map<LocalDate, Long> citasPorDia = reporteRepository.contarCitasPorDia(inicio, fin, idPsicologo);

        List<List<DiaCalendarioDTO>> semanas = new ArrayList<>();
        for (LocalDate lunes = inicio; !lunes.isAfter(fin); lunes = lunes.plusWeeks(1)) {
            List<DiaCalendarioDTO> semana = new ArrayList<>(7);
            for (int i = 0; i < 7; i++) {
                LocalDate dia = lunes.plusDays(i);
                semana.add(new DiaCalendarioDTO(dia, YearMonth.from(dia).equals(mes), dia.equals(hoy),
                        citasPorDia.getOrDefault(dia, 0L)));
            }
            semanas.add(semana);
        }
        return new CalendarioDTO(mes, StringUtils.capitalize(mes.format(FORMATO_MES)), semanas);
    }
}
