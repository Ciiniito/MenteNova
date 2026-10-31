package com.mentenova.repository;
import com.mentenova.dto.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import java.time.*;
import java.util.List;

@Repository
public class AgendaRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public AgendaRepository(NamedParameterJdbcTemplate jdbc) {this.jdbc=jdbc;}
    public List<TramoHorario> ocupaciones(Long id,LocalDate fecha) {
        return jdbc.query("""
            SELECT c.hora_inicio,c.hora_fin FROM cita c
            JOIN estado_cita e ON e.id_estado_cita=c.id_estado_cita
            WHERE c.id_psicologo=:id AND c.fecha=:fecha AND e.codigo IN ('PENDIENTE','CONFIRMADA')
            ORDER BY c.hora_inicio
            """,new MapSqlParameterSource("id",id).addValue("fecha",fecha),
            (rs,i)->new TramoHorario(rs.getTime("hora_inicio").toLocalTime(),rs.getTime("hora_fin").toLocalTime()));
    }
    public List<CitaAgendaDTO> listar(Long id,LocalDate desde,LocalDate hasta) {
        var params=new MapSqlParameterSource("desde",desde).addValue("hasta",hasta);
        String filtro="";
        if(id!=null) {filtro=" AND c.id_psicologo=:id ";params.addValue("id",id);}
        return jdbc.query("""
            SELECT c.id_cita,c.fecha,c.hora_inicio,c.hora_fin,
                   pa.nombres || ' ' || pa.apellidos AS paciente,
                   u.nombres || ' ' || u.apellidos AS psicologo,
                   c.modalidad,e.codigo,e.nombre AS estado_nombre
            FROM cita c JOIN paciente pa ON pa.id_paciente=c.id_paciente
            JOIN psicologo p ON p.id_psicologo=c.id_psicologo
            JOIN usuario u ON u.id_usuario=p.id_usuario
            JOIN estado_cita e ON e.id_estado_cita=c.id_estado_cita
            WHERE c.fecha BETWEEN :desde AND :hasta
            """+filtro+" ORDER BY c.fecha,c.hora_inicio,c.id_cita",params,
            (rs,i)->new CitaAgendaDTO(rs.getLong("id_cita"),rs.getDate("fecha").toLocalDate(),
                rs.getTime("hora_inicio").toLocalTime(),rs.getTime("hora_fin").toLocalTime(),
                rs.getString("paciente"),rs.getString("psicologo"),rs.getString("modalidad"),rs.getString("codigo"),rs.getString("estado_nombre")));
    }
}
