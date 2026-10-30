package com.mentenova.repository;

import com.mentenova.dto.reporte.CitaReporteDTO;
import com.mentenova.dto.reporte.ConteoEstadoDTO;
import com.mentenova.dto.reporte.DistribucionPsicologoDTO;
import com.mentenova.dto.reporte.FiltroReporte;
import com.mentenova.dto.reporte.MedioPagoDTO;
import com.mentenova.dto.reporte.OpcionDTO;
import com.mentenova.dto.reporte.PagoReporteDTO;
import com.mentenova.dto.reporte.ResumenEstadosDTO;
import com.mentenova.dto.reporte.TasasDTO;
import com.mentenova.dto.reporte.TotalesPagosDTO;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ReporteRepository {

    private static final String FROM_CITA = """
            FROM cita c
            JOIN estado_cita ec ON ec.id_estado_cita = c.id_estado_cita
            JOIN psicologo ps ON ps.id_psicologo = c.id_psicologo
            JOIN usuario up ON up.id_usuario = ps.id_usuario
            """;

    private static final String SELECT_CITA = """
            SELECT c.id_cita, c.fecha, c.hora_inicio, c.hora_fin,
                   pa.nombres || ' ' || pa.apellidos AS paciente,
                   up.nombres || ' ' || up.apellidos AS psicologo,
                   c.modalidad, ec.codigo AS estado_codigo, ec.nombre AS estado_nombre,
                   c.fecha_creacion
            """ + FROM_CITA + """
            JOIN paciente pa ON pa.id_paciente = c.id_paciente
            """;

    private static final String FROM_PAGO = """
            FROM pago p
            JOIN cita c ON c.id_cita = p.id_cita
            JOIN estado_cita ec ON ec.id_estado_cita = c.id_estado_cita
            JOIN psicologo ps ON ps.id_psicologo = c.id_psicologo
            JOIN usuario up ON up.id_usuario = ps.id_usuario
            """;

    private static final String CONTEOS_TASAS = """
            COUNT(*) AS registradas,
            COUNT(*) FILTER (WHERE ec.codigo = 'ATENDIDA') AS atendidas,
            COUNT(*) FILTER (WHERE ec.codigo = 'NO_ASISTIO') AS no_asistio,
            COUNT(*) FILTER (WHERE ec.codigo = 'CANCELADA') AS canceladas
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock reloj;

    public ReporteRepository(NamedParameterJdbcTemplate jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    public List<OpcionDTO> listarPsicologos() {
        return jdbc.query("""
                SELECT ps.id_psicologo, up.nombres || ' ' || up.apellidos AS nombre, ps.activo
                FROM psicologo ps
                JOIN usuario up ON up.id_usuario = ps.id_usuario
                ORDER BY up.nombres, up.apellidos
                """, (rs, i) -> new OpcionDTO(
                rs.getString("id_psicologo"),
                rs.getString("nombre") + (rs.getBoolean("activo") ? "" : " (inactivo)")));
    }

    public List<OpcionDTO> listarEspecialidades() {
        return jdbc.query("SELECT id_especialidad, nombre FROM especialidad ORDER BY nombre",
                (rs, i) -> new OpcionDTO(rs.getString("id_especialidad"), rs.getString("nombre")));
    }

    public List<OpcionDTO> listarEstadosCita() {
        return jdbc.query("SELECT codigo, nombre FROM estado_cita ORDER BY id_estado_cita",
                (rs, i) -> new OpcionDTO(rs.getString("codigo"), rs.getString("nombre")));
    }

    public Optional<Long> buscarIdPsicologo(Long idUsuario) {
        return jdbc.queryForList("SELECT id_psicologo FROM psicologo WHERE id_usuario = :idUsuario",
                        new MapSqlParameterSource("idUsuario", idUsuario), Long.class)
                .stream().findFirst();
    }

    public Optional<Long> buscarIdPaciente(Long idUsuario) {
        return jdbc.queryForList("SELECT id_paciente FROM paciente WHERE id_usuario = :idUsuario",
                        new MapSqlParameterSource("idUsuario", idUsuario), Long.class)
                .stream().findFirst();
    }

    public ResumenEstadosDTO resumirPorEstado(FiltroReporte filtro) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String condiciones = String.join(" AND ", condicionesCita(filtro, params));
        List<ConteoEstadoDTO> estados = jdbc.query("""
                SELECT ec.codigo, ec.nombre, COUNT(c.id_cita) AS total,
                       COALESCE(ROUND(100.0 * COUNT(c.id_cita) / NULLIF(SUM(COUNT(c.id_cita)) OVER (), 0), 1), 0) AS porcentaje
                FROM estado_cita ec
                LEFT JOIN cita c ON c.id_estado_cita = ec.id_estado_cita AND %s
                GROUP BY ec.id_estado_cita, ec.codigo, ec.nombre
                ORDER BY ec.id_estado_cita
                """.formatted(condiciones), params, (rs, i) -> new ConteoEstadoDTO(
                rs.getString("codigo"), rs.getString("nombre"), rs.getLong("total"), rs.getDouble("porcentaje")));
        return ResumenEstadosDTO.de(estados);
    }

    public List<CitaReporteDTO> listarCitas(FiltroReporte filtro, int limite) {
        MapSqlParameterSource params = new MapSqlParameterSource("limite", limite);
        String condiciones = String.join(" AND ", condicionesCita(filtro, params));
        return jdbc.query(SELECT_CITA + "WHERE " + condiciones
                + " ORDER BY c.fecha, c.hora_inicio, c.id_cita LIMIT :limite", params, this::mapearCita);
    }

    public List<DistribucionPsicologoDTO> distribucionPorPsicologo(FiltroReporte filtro) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String condiciones = String.join(" AND ", condicionesCita(filtro, params));
        return jdbc.query("""
                SELECT ps.id_psicologo, up.nombres || ' ' || up.apellidos AS psicologo,
                       COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE ec.codigo = 'PENDIENTE') AS pendientes,
                       COUNT(*) FILTER (WHERE ec.codigo = 'CONFIRMADA') AS confirmadas,
                       COUNT(*) FILTER (WHERE ec.codigo = 'ATENDIDA') AS atendidas,
                       COUNT(*) FILTER (WHERE ec.codigo = 'CANCELADA') AS canceladas,
                       COUNT(*) FILTER (WHERE ec.codigo = 'REPROGRAMADA') AS reprogramadas,
                       COUNT(*) FILTER (WHERE ec.codigo = 'NO_ASISTIO') AS no_asistio
                """ + FROM_CITA + "WHERE " + condiciones + """

                GROUP BY ps.id_psicologo, up.nombres, up.apellidos
                ORDER BY total DESC, psicologo
                """, params, (rs, i) -> new DistribucionPsicologoDTO(
                rs.getLong("id_psicologo"), rs.getString("psicologo"), rs.getLong("total"),
                rs.getLong("pendientes"), rs.getLong("confirmadas"), rs.getLong("atendidas"),
                rs.getLong("canceladas"), rs.getLong("reprogramadas"), rs.getLong("no_asistio")));
    }

    public List<CitaReporteDTO> listarProximasCitas(LocalDate hoy, LocalTime ahora, Long idPsicologo,
                                                    Long idPaciente, int limite) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("hoy", hoy)
                .addValue("ahora", ahora)
                .addValue("limite", limite);
        StringBuilder sql = new StringBuilder(SELECT_CITA)
                .append("WHERE (c.fecha > :hoy OR (c.fecha = :hoy AND c.hora_fin > :ahora))");
        if (idPsicologo != null) {
            sql.append(" AND c.id_psicologo = :idPsicologo");
            params.addValue("idPsicologo", idPsicologo);
        }
        if (idPaciente != null) {
            sql.append(" AND c.id_paciente = :idPaciente");
            params.addValue("idPaciente", idPaciente);
        }
        sql.append(" ORDER BY c.fecha, c.hora_inicio, c.id_cita LIMIT :limite");
        return jdbc.query(sql.toString(), params, this::mapearCita);
    }

    public Map<LocalDate, Long> contarCitasPorDia(LocalDate desde, LocalDate hasta, Long idPsicologo) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("desde", desde)
                .addValue("hasta", hasta);
        StringBuilder sql = new StringBuilder("""
                SELECT c.fecha, COUNT(*) AS total
                FROM cita c
                JOIN estado_cita ec ON ec.id_estado_cita = c.id_estado_cita
                WHERE c.fecha BETWEEN :desde AND :hasta
                  AND ec.codigo NOT IN ('CANCELADA', 'REPROGRAMADA')
                """);
        if (idPsicologo != null) {
            sql.append(" AND c.id_psicologo = :idPsicologo");
            params.addValue("idPsicologo", idPsicologo);
        }
        sql.append(" GROUP BY c.fecha");
        Map<LocalDate, Long> resultado = new LinkedHashMap<>();
        jdbc.query(sql.toString(), params, rs -> {
            resultado.put(rs.getObject("fecha", LocalDate.class), rs.getLong("total"));
        });
        return resultado;
    }

    public TasasDTO calcularTasas(FiltroReporte filtro, String etiqueta) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String condiciones = String.join(" AND ", condicionesCita(filtro, params));
        return jdbc.queryForObject("SELECT " + CONTEOS_TASAS + FROM_CITA + "WHERE " + condiciones, params,
                (rs, i) -> mapearTasas(rs, etiqueta));
    }

    public Map<LocalDate, TasasDTO> tasasPorPeriodo(FiltroReporte filtro, boolean porMes) {
        MapSqlParameterSource params = new MapSqlParameterSource("unidad", porMes ? "month" : "week");
        String condiciones = String.join(" AND ", condicionesCita(filtro, params));
        Map<LocalDate, TasasDTO> resultado = new LinkedHashMap<>();
        jdbc.query("SELECT date_trunc(CAST(:unidad AS TEXT), c.fecha::timestamp)::date AS periodo, "
                + CONTEOS_TASAS + FROM_CITA + "WHERE " + condiciones
                + " GROUP BY 1 ORDER BY 1", params, rs -> {
            resultado.put(rs.getObject("periodo", LocalDate.class), mapearTasas(rs, null));
        });
        return resultado;
    }

    public List<TasasDTO> tasasPorPsicologo(FiltroReporte filtro) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String condiciones = String.join(" AND ", condicionesCita(filtro, params));
        return jdbc.query("SELECT up.nombres || ' ' || up.apellidos AS psicologo, " + CONTEOS_TASAS
                + FROM_CITA + "WHERE " + condiciones
                + " GROUP BY ps.id_psicologo, up.nombres, up.apellidos ORDER BY psicologo", params,
                (rs, i) -> mapearTasas(rs, rs.getString("psicologo")));
    }

    public TotalesPagosDTO resumirPagos(FiltroReporte filtro) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String condiciones = String.join(" AND ", condicionesPago(filtro, params));
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(p.monto) FILTER (WHERE p.estado <> 'ANULADO'), 0) AS registrado,
                       COALESCE(SUM(p.monto) FILTER (WHERE p.estado = 'PAGADO'), 0) AS pagado,
                       COALESCE(SUM(p.monto) FILTER (WHERE p.estado = 'PENDIENTE'), 0) AS pendiente,
                       COALESCE(SUM(p.monto) FILTER (WHERE p.estado = 'ANULADO'), 0) AS anulado,
                       COUNT(DISTINCT c.id_cita) FILTER (WHERE p.estado = 'PAGADO' AND ec.codigo = 'ATENDIDA')
                           AS citas_atendidas_con_pago
                """ + FROM_PAGO + "WHERE " + condiciones, params, (rs, i) -> new TotalesPagosDTO(
                rs.getBigDecimal("registrado"), rs.getBigDecimal("pagado"), rs.getBigDecimal("pendiente"),
                rs.getBigDecimal("anulado"), rs.getLong("citas_atendidas_con_pago")));
    }

    public List<MedioPagoDTO> resumirPorMedioPago(FiltroReporte filtro) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String condiciones = String.join(" AND ", condicionesPago(filtro, params));
        return jdbc.query("""
                SELECT p.medio_pago, COUNT(*) AS cantidad, SUM(p.monto) AS monto,
                       ROUND(100.0 * SUM(p.monto) / NULLIF(SUM(SUM(p.monto)) OVER (), 0), 1) AS porcentaje
                """ + FROM_PAGO + "WHERE " + condiciones + """
                 AND p.estado = 'PAGADO'
                GROUP BY p.medio_pago
                ORDER BY monto DESC, p.medio_pago
                """, params, (rs, i) -> new MedioPagoDTO(
                rs.getString("medio_pago"), rs.getLong("cantidad"), rs.getBigDecimal("monto"),
                rs.getDouble("porcentaje")));
    }

    public List<PagoReporteDTO> listarPagos(FiltroReporte filtro, int limite) {
        MapSqlParameterSource params = new MapSqlParameterSource("limite", limite);
        String condiciones = String.join(" AND ", condicionesPago(filtro, params));
        return jdbc.query("""
                SELECT p.id_pago, p.id_cita, p.fecha_pago, p.fecha_registro,
                       up.nombres || ' ' || up.apellidos AS psicologo,
                       p.monto, p.medio_pago, p.estado,
                       ec.codigo AS estado_cita_codigo, ec.nombre AS estado_cita_nombre
                """ + FROM_PAGO + "WHERE " + condiciones + """

                ORDER BY COALESCE(p.fecha_pago, p.fecha_registro) DESC, p.id_pago DESC
                LIMIT :limite
                """, params, (rs, i) -> new PagoReporteDTO(
                rs.getLong("id_pago"), rs.getLong("id_cita"),
                aHoraLocal(rs.getObject("fecha_pago", OffsetDateTime.class)),
                aHoraLocal(rs.getObject("fecha_registro", OffsetDateTime.class)),
                rs.getString("psicologo"), rs.getBigDecimal("monto"), rs.getString("medio_pago"),
                rs.getString("estado"), rs.getString("estado_cita_codigo"), rs.getString("estado_cita_nombre")));
    }

    private List<String> condicionesCita(FiltroReporte filtro, MapSqlParameterSource params) {
        List<String> condiciones = new ArrayList<>();
        condiciones.add("c.fecha BETWEEN :desde AND :hasta");
        params.addValue("desde", filtro.getDesde()).addValue("hasta", filtro.getHasta());
        if (filtro.getIdPsicologo() != null) {
            condiciones.add("c.id_psicologo = :idPsicologo");
            params.addValue("idPsicologo", filtro.getIdPsicologo());
        }
        if (filtro.getIdEspecialidad() != null) {
            condiciones.add("EXISTS (SELECT 1 FROM psicologo_especialidad pe"
                    + " WHERE pe.id_psicologo = c.id_psicologo AND pe.id_especialidad = :idEspecialidad)");
            params.addValue("idEspecialidad", filtro.getIdEspecialidad());
        }
        if (filtro.getModalidad() != null) {
            condiciones.add("c.modalidad = :modalidad");
            params.addValue("modalidad", filtro.getModalidad());
        }
        if (filtro.getEstado() != null) {
            condiciones.add("ec.codigo = :estado");
            params.addValue("estado", filtro.getEstado());
        }
        return condiciones;
    }

    private List<String> condicionesPago(FiltroReporte filtro, MapSqlParameterSource params) {
        List<String> condiciones = new ArrayList<>();
        condiciones.add("COALESCE(p.fecha_pago, p.fecha_registro) >= :desdeMomento");
        condiciones.add("COALESCE(p.fecha_pago, p.fecha_registro) < :hastaMomento");
        params.addValue("desdeMomento", inicioDelDia(filtro.getDesde()))
                .addValue("hastaMomento", inicioDelDia(filtro.getHasta().plusDays(1)));
        if (filtro.getIdPsicologo() != null) {
            condiciones.add("c.id_psicologo = :idPsicologo");
            params.addValue("idPsicologo", filtro.getIdPsicologo());
        }
        if (filtro.getMedioPago() != null) {
            condiciones.add("p.medio_pago = :medioPago");
            params.addValue("medioPago", filtro.getMedioPago());
        }
        if (filtro.getEstadoPago() != null) {
            condiciones.add("p.estado = :estadoPago");
            params.addValue("estadoPago", filtro.getEstadoPago());
        }
        return condiciones;
    }

    private CitaReporteDTO mapearCita(ResultSet rs, int fila) throws SQLException {
        return new CitaReporteDTO(
                rs.getLong("id_cita"),
                rs.getObject("fecha", LocalDate.class),
                rs.getObject("hora_inicio", LocalTime.class),
                rs.getObject("hora_fin", LocalTime.class),
                rs.getString("paciente"),
                rs.getString("psicologo"),
                rs.getString("modalidad"),
                rs.getString("estado_codigo"),
                rs.getString("estado_nombre"),
                aHoraLocal(rs.getObject("fecha_creacion", OffsetDateTime.class)));
    }

    private TasasDTO mapearTasas(ResultSet rs, String etiqueta) throws SQLException {
        return TasasDTO.de(etiqueta, rs.getLong("registradas"), rs.getLong("atendidas"),
                rs.getLong("no_asistio"), rs.getLong("canceladas"));
    }

    private OffsetDateTime inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay(reloj.getZone()).toOffsetDateTime();
    }

    private LocalDateTime aHoraLocal(OffsetDateTime momento) {
        return momento == null ? null : momento.atZoneSameInstant(reloj.getZone()).toLocalDateTime();
    }
}
