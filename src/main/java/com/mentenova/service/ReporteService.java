package com.mentenova.service;

import com.mentenova.dto.reporte.CitaReporteDTO;
import com.mentenova.dto.reporte.FiltroReporte;
import com.mentenova.dto.reporte.OpcionDTO;
import com.mentenova.dto.reporte.PagoReporteDTO;
import com.mentenova.dto.reporte.ReporteCitasDTO;
import com.mentenova.dto.reporte.ReporteInasistenciasDTO;
import com.mentenova.dto.reporte.ReporteIngresosDTO;
import com.mentenova.dto.reporte.TasasDTO;
import com.mentenova.repository.ReporteRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
@Transactional(readOnly = true)
public class ReporteService {

    public static final int LIMITE_DETALLE = 500;

    public static final String AGRUPACION_SEMANA = "SEMANA";
    public static final String AGRUPACION_MES = "MES";

    private static final List<String> MODALIDADES = List.of("PRESENCIAL", "VIRTUAL");
    private static final List<String> ESTADOS_CITA =
            List.of("PENDIENTE", "CONFIRMADA", "ATENDIDA", "CANCELADA", "REPROGRAMADA", "NO_ASISTIO");
    private static final List<String> MEDIOS_PAGO =
            List.of("EFECTIVO", "TARJETA", "TRANSFERENCIA", "YAPE", "PLIN", "OTRO");
    private static final List<String> ESTADOS_PAGO = List.of("PENDIENTE", "PAGADO", "ANULADO");

    private static final Locale ES = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter FORMATO_SEMANA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("MMMM yyyy", ES);

    private final ReporteRepository reporteRepository;
    private final Clock reloj;

    public ReporteService(ReporteRepository reporteRepository, Clock reloj) {
        this.reporteRepository = reporteRepository;
        this.reloj = reloj;
    }

    public ReporteCitasDTO reporteCitas(FiltroReporte filtro) {
        completarPeriodoMesActual(filtro);
        filtro.setModalidad(valorPermitido(filtro.getModalidad(), MODALIDADES));
        filtro.setEstado(valorPermitido(filtro.getEstado(), ESTADOS_CITA));
        filtro.setMedioPago(null);
        filtro.setEstadoPago(null);
        filtro.setAgrupacion(null);

        List<CitaReporteDTO> detalle = reporteRepository.listarCitas(filtro, LIMITE_DETALLE + 1);
        boolean truncado = detalle.size() > LIMITE_DETALLE;
        return new ReporteCitasDTO(
                reporteRepository.resumirPorEstado(filtro),
                reporteRepository.distribucionPorPsicologo(filtro),
                truncado ? detalle.subList(0, LIMITE_DETALLE) : detalle,
                truncado);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ReporteIngresosDTO reporteIngresos(FiltroReporte filtro) {
        completarPeriodoMesActual(filtro);
        filtro.setMedioPago(valorPermitido(filtro.getMedioPago(), MEDIOS_PAGO));
        filtro.setEstadoPago(valorPermitido(filtro.getEstadoPago(), ESTADOS_PAGO));
        filtro.setIdEspecialidad(null);
        filtro.setModalidad(null);
        filtro.setEstado(null);
        filtro.setAgrupacion(null);

        List<PagoReporteDTO> detalle = reporteRepository.listarPagos(filtro, LIMITE_DETALLE + 1);
        boolean truncado = detalle.size() > LIMITE_DETALLE;
        return new ReporteIngresosDTO(
                reporteRepository.resumirPagos(filtro),
                reporteRepository.resumirPorMedioPago(filtro),
                truncado ? detalle.subList(0, LIMITE_DETALLE) : detalle,
                truncado);
    }

    public ReporteInasistenciasDTO reporteInasistencias(FiltroReporte filtro, boolean incluirPorPsicologo) {
        LocalDate hoy = LocalDate.now(reloj);
        filtro.completarPeriodo(hoy.minusMonths(2).withDayOfMonth(1), hoy);
        filtro.setModalidad(valorPermitido(filtro.getModalidad(), MODALIDADES));
        filtro.setIdEspecialidad(null);
        filtro.setEstado(null);
        filtro.setMedioPago(null);
        filtro.setEstadoPago(null);
        filtro.setAgrupacion(resolverAgrupacion(filtro));

        boolean porMes = AGRUPACION_MES.equals(filtro.getAgrupacion());
        Map<LocalDate, TasasDTO> conteos = reporteRepository.tasasPorPeriodo(filtro, porMes);

        List<TasasDTO> tendencia = new ArrayList<>();
        LocalDate periodo = porMes ? filtro.getDesde().withDayOfMonth(1) : filtro.getDesde().with(DayOfWeek.MONDAY);
        while (!periodo.isAfter(filtro.getHasta())) {
            String etiqueta = porMes
                    ? StringUtils.capitalize(periodo.format(FORMATO_MES))
                    : "Sem. " + periodo.format(FORMATO_SEMANA);
            TasasDTO conteo = conteos.get(periodo);
            tendencia.add(conteo == null
                    ? TasasDTO.de(etiqueta, 0, 0, 0, 0)
                    : TasasDTO.de(etiqueta, conteo.registradas(), conteo.atendidas(),
                    conteo.noAsistio(), conteo.canceladas()));
            periodo = porMes ? periodo.plusMonths(1) : periodo.plusWeeks(1);
        }

        return new ReporteInasistenciasDTO(
                reporteRepository.calcularTasas(filtro, "Total del periodo"),
                filtro.getAgrupacion(),
                tendencia,
                incluirPorPsicologo ? reporteRepository.tasasPorPsicologo(filtro) : List.of());
    }

    public List<OpcionDTO> psicologos() {
        return reporteRepository.listarPsicologos();
    }

    public List<OpcionDTO> especialidades() {
        return reporteRepository.listarEspecialidades();
    }

    public List<OpcionDTO> estadosCita() {
        return reporteRepository.listarEstadosCita();
    }

    public List<OpcionDTO> modalidades() {
        return opciones(MODALIDADES);
    }

    public List<OpcionDTO> mediosPago() {
        return opciones(MEDIOS_PAGO);
    }

    public List<OpcionDTO> estadosPago() {
        return opciones(ESTADOS_PAGO);
    }

    public List<OpcionDTO> agrupaciones() {
        return List.of(new OpcionDTO(AGRUPACION_SEMANA, "Semanal"), new OpcionDTO(AGRUPACION_MES, "Mensual"));
    }

    private void completarPeriodoMesActual(FiltroReporte filtro) {
        YearMonth mesActual = YearMonth.now(reloj);
        filtro.completarPeriodo(mesActual.atDay(1), mesActual.atEndOfMonth());
    }

    private String resolverAgrupacion(FiltroReporte filtro) {
        long dias = ChronoUnit.DAYS.between(filtro.getDesde(), filtro.getHasta());
        String agrupacion = valorPermitido(filtro.getAgrupacion(), List.of(AGRUPACION_SEMANA, AGRUPACION_MES));
        if (agrupacion == null) {
            return dias > 120 ? AGRUPACION_MES : AGRUPACION_SEMANA;
        }
        return AGRUPACION_SEMANA.equals(agrupacion) && dias > 731 ? AGRUPACION_MES : agrupacion;
    }

    private static String valorPermitido(String valor, List<String> permitidos) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        return permitidos.contains(normalizado) ? normalizado : null;
    }

    private static List<OpcionDTO> opciones(List<String> codigos) {
        return codigos.stream()
                .map(codigo -> new OpcionDTO(codigo, StringUtils.capitalize(codigo.toLowerCase(ES))))
                .toList();
    }
}
