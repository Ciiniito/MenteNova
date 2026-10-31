package com.mentenova.repository;

import com.mentenova.dto.cita.CitaDTO;
import com.mentenova.dto.cita.FiltroCitaDTO;
import com.mentenova.dto.paciente.FormatoPaciente;
import com.mentenova.dto.reporte.OpcionDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CitaConsultaRepository {

    private static final String NOMBRE_SIN_ACENTOS =
            "translate(lower(pa.nombres || ' ' || pa.apellidos), 'áéíóúàèìòùäëïöüâêîôûñç', 'aeiouaeiouaeiouaeiounc')";

    private static final String FROM_CITA = """
            FROM cita c
            JOIN paciente pa ON pa.id_paciente = c.id_paciente
            JOIN psicologo ps ON ps.id_psicologo = c.id_psicologo
            JOIN usuario up ON up.id_usuario = ps.id_usuario
            JOIN estado_cita ec ON ec.id_estado_cita = c.id_estado_cita
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public CitaConsultaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long contar(FiltroCitaDTO filtro) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        Long total = jdbc.queryForObject("SELECT count(*) " + FROM_CITA + " WHERE " + condiciones(filtro, params),
                params, Long.class);
        return total == null ? 0 : total;
    }

    public List<CitaDTO> listar(FiltroCitaDTO filtro, int limite, int desplazamiento) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limite", limite)
                .addValue("desplazamiento", desplazamiento);
        return jdbc.query("""
                SELECT c.id_cita, c.fecha, c.hora_inicio, c.hora_fin, c.modalidad,
                       pa.id_paciente, pa.nombres AS paciente_nombres, pa.apellidos AS paciente_apellidos,
                       up.nombres || ' ' || up.apellidos AS psicologo,
                       ec.codigo AS estado_codigo, ec.nombre AS estado_nombre
                """ + FROM_CITA + " WHERE " + condiciones(filtro, params) + """
                 ORDER BY c.fecha, c.hora_inicio, c.id_cita
                LIMIT :limite OFFSET :desplazamiento
                """, params, (rs, i) -> new CitaDTO(
                rs.getLong("id_cita"),
                rs.getObject("fecha", LocalDate.class),
                rs.getObject("hora_inicio", LocalTime.class),
                rs.getObject("hora_fin", LocalTime.class),
                rs.getLong("id_paciente"),
                FormatoPaciente.nombreCompleto(rs.getString("paciente_nombres"), rs.getString("paciente_apellidos")),
                rs.getString("psicologo"),
                rs.getString("modalidad"),
                rs.getString("estado_codigo"),
                rs.getString("estado_nombre")));
    }

    public List<OpcionDTO> estados() {
        return jdbc.query("SELECT codigo, nombre FROM estado_cita ORDER BY id_estado_cita",
                (rs, i) -> new OpcionDTO(rs.getString("codigo"), rs.getString("nombre")));
    }

    public List<OpcionDTO> pacientesActivos() {
        return jdbc.query("""
                SELECT id_paciente, nombres, apellidos, tipo_documento, numero_documento
                FROM paciente
                WHERE activo
                ORDER BY apellidos, nombres, id_paciente
                """, (rs, i) -> {
            String nombre = FormatoPaciente.nombreCompleto(rs.getString("nombres"), rs.getString("apellidos"));
            String documento = FormatoPaciente.documento(rs.getString("tipo_documento"), rs.getString("numero_documento"));
            return new OpcionDTO(String.valueOf(rs.getLong("id_paciente")),
                    documento == null ? nombre : nombre + " · " + documento);
        });
    }

    public List<OpcionDTO> psicologosActivos() {
        return jdbc.query("""
                SELECT ps.id_psicologo, u.nombres || ' ' || u.apellidos AS nombre
                FROM psicologo ps
                JOIN usuario u ON u.id_usuario = ps.id_usuario
                WHERE ps.activo AND u.activo
                ORDER BY u.apellidos, u.nombres, ps.id_psicologo
                """, (rs, i) -> new OpcionDTO(String.valueOf(rs.getLong("id_psicologo")), rs.getString("nombre")));
    }

    /** Vacío si el paciente no existe; de lo contrario, indica si está activo. */
    public Optional<Boolean> pacienteActivo(Long idPaciente) {
        return jdbc.queryForList("SELECT activo FROM paciente WHERE id_paciente = :id",
                new MapSqlParameterSource("id", idPaciente), Boolean.class).stream().findFirst();
    }

    /** Vacío si el psicólogo no existe; de lo contrario, indica si su ficha y su cuenta están activas. */
    public Optional<Boolean> psicologoActivo(Long idPsicologo) {
        return jdbc.queryForList("""
                SELECT ps.activo AND u.activo
                FROM psicologo ps JOIN usuario u ON u.id_usuario = ps.id_usuario
                WHERE ps.id_psicologo = :id
                """, new MapSqlParameterSource("id", idPsicologo), Boolean.class).stream().findFirst();
    }

    public Optional<Short> idEstado(String codigo) {
        return jdbc.queryForList("SELECT id_estado_cita FROM estado_cita WHERE codigo = :codigo",
                new MapSqlParameterSource("codigo", codigo), Short.class).stream().findFirst();
    }

    public boolean pacienteTieneCruce(Long idPaciente, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM cita c
                    JOIN estado_cita ec ON ec.id_estado_cita = c.id_estado_cita
                    WHERE c.id_paciente = :idPaciente AND c.fecha = :fecha
                      AND ec.codigo IN ('PENDIENTE', 'CONFIRMADA')
                      AND :inicio < c.hora_fin AND :fin > c.hora_inicio)
                """, new MapSqlParameterSource("idPaciente", idPaciente)
                .addValue("fecha", fecha)
                .addValue("inicio", inicio)
                .addValue("fin", fin), Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    public void registrarHistorialCreacion(Long idCita, Long idUsuario, Short idEstado, LocalDate fecha, LocalTime hora) {
        jdbc.update("""
                INSERT INTO historial_cita (id_cita, id_usuario, tipo_evento, id_estado_nuevo, fecha_nueva, hora_nueva)
                VALUES (:idCita, :idUsuario, 'CREADA', :idEstado, :fecha, :hora)
                """, new MapSqlParameterSource("idCita", idCita)
                .addValue("idUsuario", idUsuario)
                .addValue("idEstado", idEstado)
                .addValue("fecha", fecha)
                .addValue("hora", hora));
    }

    public void registrarUsuarioAuditoria(Long idUsuario) {
        jdbc.queryForObject("SELECT set_config('app.user_id', :idUsuario, true)",
                new MapSqlParameterSource("idUsuario", String.valueOf(idUsuario)), String.class);
    }

    private String condiciones(FiltroCitaDTO filtro, MapSqlParameterSource params) {
        List<String> condiciones = new ArrayList<>();
        condiciones.add("TRUE");
        if (filtro.fecha() != null) {
            condiciones.add("c.fecha = :fecha");
            params.addValue("fecha", filtro.fecha());
        } else if (filtro.desde() != null) {
            condiciones.add("c.fecha >= :desde");
            params.addValue("desde", filtro.desde());
        }
        if (filtro.estado() != null) {
            condiciones.add("ec.codigo = :estado");
            params.addValue("estado", filtro.estado());
        }
        if (filtro.paciente() != null) {
            List<String> porNombre = new ArrayList<>();
            String[] palabras = filtro.paciente().split("\\s+");
            for (int i = 0; i < palabras.length; i++) {
                porNombre.add(NOMBRE_SIN_ACENTOS + " LIKE :palabra" + i);
                params.addValue("palabra" + i, "%" + escaparLike(palabras[i]) + "%");
            }
            condiciones.add("((" + String.join(" AND ", porNombre) + ") OR lower(pa.numero_documento) LIKE :documento)");
            params.addValue("documento", escaparLike(filtro.paciente().replaceAll("\\s+", "")) + "%");
        }
        return String.join(" AND ", condiciones);
    }

    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
