package com.mentenova.repository;

import com.mentenova.dto.paciente.CambioPacienteDTO;
import com.mentenova.dto.paciente.CitaPacienteDTO;
import com.mentenova.dto.paciente.CriterioBusquedaPacientes;
import com.mentenova.dto.paciente.PacienteFilaDTO;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class PacienteConsultaRepository {

    private static final String NOMBRE_SIN_ACENTOS =
            "translate(lower(pa.nombres || ' ' || pa.apellidos), 'áéíóúàèìòùäëïöüâêîôûñç', 'aeiouaeiouaeiouaeiounc')";

    private static final String SELECT_CITA = """
            SELECT c.id_cita, c.fecha, c.hora_inicio, c.hora_fin, c.modalidad,
                   ec.codigo AS estado_codigo, ec.nombre AS estado_nombre,
                   up.nombres || ' ' || up.apellidos AS psicologo,
                   (SELECT CASE WHEN bool_or(p.estado = 'PAGADO') THEN 'PAGADO'
                                WHEN bool_or(p.estado = 'PENDIENTE') THEN 'PENDIENTE'
                                WHEN bool_or(p.estado = 'ANULADO') THEN 'ANULADO' END
                    FROM pago p WHERE p.id_cita = c.id_cita) AS estado_pago
            FROM cita c
            JOIN estado_cita ec ON ec.id_estado_cita = c.id_estado_cita
            JOIN psicologo ps ON ps.id_psicologo = c.id_psicologo
            JOIN usuario up ON up.id_usuario = ps.id_usuario
            """;

    private static final Map<String, String> ETIQUETAS_CAMPOS = Map.ofEntries(
            Map.entry("nombres", "Nombres"),
            Map.entry("apellidos", "Apellidos"),
            Map.entry("tipo_documento", "Tipo de documento"),
            Map.entry("numero_documento", "Número de documento"),
            Map.entry("fecha_nacimiento", "Fecha de nacimiento"),
            Map.entry("telefono", "Teléfono"),
            Map.entry("correo", "Correo"),
            Map.entry("direccion", "Dirección"),
            Map.entry("activo", "Estado"),
            Map.entry("id_usuario", "Cuenta de acceso"));

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock reloj;

    public PacienteConsultaRepository(NamedParameterJdbcTemplate jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    public long contar(CriterioBusquedaPacientes criterio) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String condiciones = condiciones(criterio, params);
        Long total = jdbc.queryForObject("SELECT count(*) FROM paciente pa WHERE " + condiciones, params, Long.class);
        return total == null ? 0 : total;
    }

    public List<PacienteFilaDTO> listar(CriterioBusquedaPacientes criterio, int limite, int desplazamiento) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limite", limite)
                .addValue("desplazamiento", desplazamiento);
        String condiciones = condiciones(criterio, params);
        String alcanceCitas = criterio.idPsicologo() == null ? "" : " AND c.id_psicologo = :idPsicologo";
        return jdbc.query("""
                SELECT pa.id_paciente, pa.nombres, pa.apellidos, pa.tipo_documento, pa.numero_documento,
                       pa.telefono, pa.activo,
                       (SELECT count(*) FROM cita c WHERE c.id_paciente = pa.id_paciente%s) AS citas
                FROM paciente pa
                WHERE %s
                ORDER BY pa.apellidos, pa.nombres, pa.id_paciente
                LIMIT :limite OFFSET :desplazamiento
                """.formatted(alcanceCitas, condiciones), params, (rs, i) -> PacienteFilaDTO.de(
                rs.getLong("id_paciente"), rs.getString("nombres"), rs.getString("apellidos"),
                rs.getString("tipo_documento"), rs.getString("numero_documento"), rs.getString("telefono"),
                rs.getBoolean("activo"), rs.getLong("citas")));
    }

    public Optional<Long> idPsicologoDeUsuario(Long idUsuario) {
        return jdbc.queryForList("SELECT id_psicologo FROM psicologo WHERE id_usuario = :idUsuario",
                        new MapSqlParameterSource("idUsuario", idUsuario), Long.class)
                .stream().findFirst();
    }

    public boolean tieneCitasCon(Long idPaciente, Long idPsicologo) {
        Boolean existe = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM cita WHERE id_paciente = :idPaciente AND id_psicologo = :idPsicologo)",
                new MapSqlParameterSource().addValue("idPaciente", idPaciente).addValue("idPsicologo", idPsicologo),
                Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    public long contarCitas(Long idPaciente, Long idPsicologo) {
        MapSqlParameterSource params = new MapSqlParameterSource("idPaciente", idPaciente);
        Long total = jdbc.queryForObject("SELECT count(*) FROM cita c WHERE c.id_paciente = :idPaciente"
                + alcance(idPsicologo, params), params, Long.class);
        return total == null ? 0 : total;
    }

    public List<CitaPacienteDTO> historial(Long idPaciente, Long idPsicologo, Integer limite) {
        MapSqlParameterSource params = new MapSqlParameterSource("idPaciente", idPaciente);
        String sql = SELECT_CITA + "WHERE c.id_paciente = :idPaciente" + alcance(idPsicologo, params)
                + " ORDER BY c.fecha DESC, c.hora_inicio DESC, c.id_cita DESC";
        if (limite != null) {
            sql += " LIMIT :limite";
            params.addValue("limite", limite);
        }
        return jdbc.query(sql, params, this::mapearCita);
    }

    public Optional<CitaPacienteDTO> proximaCita(Long idPaciente, Long idPsicologo) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("idPaciente", idPaciente)
                .addValue("hoy", LocalDate.now(reloj))
                .addValue("ahora", LocalTime.now(reloj));
        String sql = SELECT_CITA + """
                WHERE c.id_paciente = :idPaciente
                  AND ec.codigo IN ('PENDIENTE', 'CONFIRMADA')
                  AND (c.fecha > :hoy OR (c.fecha = :hoy AND c.hora_fin > :ahora))
                """ + alcance(idPsicologo, params) + " ORDER BY c.fecha, c.hora_inicio LIMIT 1";
        return jdbc.query(sql, params, this::mapearCita).stream().findFirst();
    }

    public Optional<String> ultimoPsicologo(Long idPaciente, Long idPsicologo) {
        MapSqlParameterSource params = new MapSqlParameterSource("idPaciente", idPaciente);
        return jdbc.queryForList("""
                SELECT up.nombres || ' ' || up.apellidos
                FROM cita c
                JOIN estado_cita ec ON ec.id_estado_cita = c.id_estado_cita
                JOIN psicologo ps ON ps.id_psicologo = c.id_psicologo
                JOIN usuario up ON up.id_usuario = ps.id_usuario
                WHERE c.id_paciente = :idPaciente
                  AND ec.codigo NOT IN ('CANCELADA', 'REPROGRAMADA')
                """ + alcance(idPsicologo, params) + " ORDER BY c.fecha DESC, c.hora_inicio DESC LIMIT 1",
                params, String.class).stream().findFirst();
    }

    public List<CambioPacienteDTO> cambios(Long idPaciente, int limite) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("idPaciente", idPaciente)
                .addValue("limite", limite);
        return jdbc.query("""
                SELECT a.fecha_evento, a.accion,
                       COALESCE(u.nombres || ' ' || u.apellidos, 'Sistema') AS usuario,
                       (SELECT string_agg(n.key, ',' ORDER BY n.key)
                        FROM jsonb_each(a.valor_nuevo) n
                        WHERE a.valor_anterior IS NOT NULL
                          AND n.value IS DISTINCT FROM a.valor_anterior -> n.key) AS campos
                FROM auditoria a
                LEFT JOIN usuario u ON u.id_usuario = a.id_usuario
                WHERE a.entidad = 'paciente' AND a.id_entidad = :idPaciente
                ORDER BY a.fecha_evento DESC, a.id_auditoria DESC
                LIMIT :limite
                """, params, (rs, i) -> new CambioPacienteDTO(
                aHoraLocal(rs.getObject("fecha_evento", OffsetDateTime.class)),
                rs.getString("usuario"),
                rs.getString("accion"),
                describirCambio(rs.getString("accion"), rs.getString("campos"))));
    }

    public void registrarUsuarioAuditoria(Long idUsuario) {
        jdbc.queryForObject("SELECT set_config('app.user_id', :idUsuario, true)",
                new MapSqlParameterSource("idUsuario", String.valueOf(idUsuario)), String.class);
    }

    private String condiciones(CriterioBusquedaPacientes criterio, MapSqlParameterSource params) {
        List<String> condiciones = new ArrayList<>();
        condiciones.add("TRUE");
        if (criterio.activo() != null) {
            condiciones.add("pa.activo = :activo");
            params.addValue("activo", criterio.activo());
        }
        if (criterio.idPsicologo() != null) {
            condiciones.add("EXISTS (SELECT 1 FROM cita cp WHERE cp.id_paciente = pa.id_paciente"
                    + " AND cp.id_psicologo = :idPsicologo)");
            params.addValue("idPsicologo", criterio.idPsicologo());
        }
        List<String> alternativas = new ArrayList<>();
        if (!criterio.palabras().isEmpty()) {
            List<String> porNombre = new ArrayList<>();
            for (int i = 0; i < criterio.palabras().size(); i++) {
                porNombre.add(NOMBRE_SIN_ACENTOS + " LIKE :palabra" + i);
                params.addValue("palabra" + i, "%" + escaparLike(criterio.palabras().get(i)) + "%");
            }
            alternativas.add("(" + String.join(" AND ", porNombre) + ")");
        }
        if (criterio.documento() != null) {
            alternativas.add("lower(pa.numero_documento) LIKE :documento");
            params.addValue("documento", escaparLike(criterio.documento()) + "%");
        }
        if (criterio.telefono() != null) {
            alternativas.add("regexp_replace(pa.telefono, '[^0-9]', '', 'g') LIKE :telefono");
            params.addValue("telefono", "%" + criterio.telefono() + "%");
        }
        if (!alternativas.isEmpty()) {
            condiciones.add("(" + String.join(" OR ", alternativas) + ")");
        }
        return String.join(" AND ", condiciones);
    }

    private String alcance(Long idPsicologo, MapSqlParameterSource params) {
        if (idPsicologo == null) {
            return "";
        }
        params.addValue("idPsicologo", idPsicologo);
        return " AND c.id_psicologo = :idPsicologo";
    }

    private CitaPacienteDTO mapearCita(ResultSet rs, int fila) throws SQLException {
        return new CitaPacienteDTO(
                rs.getLong("id_cita"),
                rs.getObject("fecha", LocalDate.class),
                rs.getObject("hora_inicio", LocalTime.class),
                rs.getObject("hora_fin", LocalTime.class),
                rs.getString("psicologo"),
                rs.getString("modalidad"),
                rs.getString("estado_codigo"),
                rs.getString("estado_nombre"),
                rs.getString("estado_pago"));
    }

    private static String describirCambio(String accion, String campos) {
        if ("INSERT".equals(accion)) {
            return "Registro del paciente";
        }
        if ("DELETE".equals(accion)) {
            return "Eliminación del registro";
        }
        if (campos == null || campos.isBlank()) {
            return "Sin cambios en los datos";
        }
        return Arrays.stream(campos.split(","))
                .map(ETIQUETAS_CAMPOS::get)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private LocalDateTime aHoraLocal(OffsetDateTime momento) {
        return momento == null ? null : momento.atZoneSameInstant(reloj.getZone()).toLocalDateTime();
    }
}
