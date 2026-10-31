package com.mentenova.service;
import com.mentenova.dto.*;
import com.mentenova.entity.Psicologo;
import com.mentenova.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.*;
import java.util.*;
@Service
@PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA','PSICOLOGO')")
public class AgendaService {
    private final AgendaRepository agenda;
    private final AccesoPsicologoService acceso;
    private final PsicologoRepository psicologos;
    public AgendaService(AgendaRepository agenda,AccesoPsicologoService acceso,PsicologoRepository psicologos) {this.agenda=agenda;this.acceso=acceso;this.psicologos=psicologos;}
    @Transactional(readOnly=true)
    public List<DiaAgendaDTO> consultar(Long psicologoId,LocalDate fecha,boolean semanal) {
        Long id=acceso.filtro(psicologoId);
        LocalDate desde=semanal?fecha.minusDays(fecha.getDayOfWeek().getValue()-1):fecha;
        LocalDate hasta=semanal?desde.plusDays(6):desde;
        var citas=agenda.listar(id,desde,hasta);var dias=new ArrayList<DiaAgendaDTO>();
        for(var dia=desde;!dia.isAfter(hasta);dia=dia.plusDays(1)) {
            var actual=dia;dias.add(new DiaAgendaDTO(dia,citas.stream().filter(c->c.fecha().equals(actual)).toList()));
        }
        return List.copyOf(dias);
    }
    @Transactional(readOnly=true)
    public List<Psicologo> profesionales() {
        Long propio=acceso.filtro(null);
        return propio==null?psicologos.findAllByOrderByUsuario_ApellidosAscUsuario_NombresAsc():List.of(acceso.consultar(propio));
    }
}
