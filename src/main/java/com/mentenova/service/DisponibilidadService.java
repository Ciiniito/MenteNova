package com.mentenova.service;
import com.mentenova.dto.*;
import com.mentenova.entity.*;
import com.mentenova.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
@PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
public class DisponibilidadService {
    private final AccesoPsicologoService acceso;
    private final PsicologoRepository psicologos;
    private final DisponibilidadRepository horarios;
    private final ExcepcionDisponibilidadRepository excepciones;
    private final AgendaRepository agenda;
    public DisponibilidadService(AccesoPsicologoService acceso,PsicologoRepository psicologos,
            DisponibilidadRepository horarios,ExcepcionDisponibilidadRepository excepciones,AgendaRepository agenda) {
        this.acceso=acceso;this.psicologos=psicologos;this.horarios=horarios;this.excepciones=excepciones;this.agenda=agenda;
    }
    @Transactional(readOnly=true)
    public Psicologo profesional(Long id) {return acceso.consultar(id);}
    @Transactional(readOnly=true)
    public List<Disponibilidad> horarios(Long id) {
        acceso.consultar(id);return horarios.findByPsicologo_IdOrderByDiaSemanaAscHoraInicioAsc(id);
    }
    @Transactional(readOnly=true)
    public List<ExcepcionDisponibilidad> excepciones(Long id) {
        acceso.consultar(id);return excepciones.findByPsicologo_IdOrderByFechaAscHoraInicioAsc(id);
    }
    @Transactional(readOnly=true)
    public HorarioForm formularioHorario(Long id,Long horarioId) {
        acceso.consultar(id);var h=horarioPropio(id,horarioId);var f=new HorarioForm();
        f.setDiaSemana(h.getDiaSemana());f.setHoraInicio(h.getHoraInicio());f.setHoraFin(h.getHoraFin());
        f.setModalidad(h.getModalidad());f.setActivo(h.getActivo());return f;
    }
    @Transactional(readOnly=true)
    public ExcepcionForm formularioExcepcion(Long id,Long excepcionId) {
        acceso.consultar(id);var e=excepcionPropia(id,excepcionId);var f=new ExcepcionForm();
        f.setFecha(e.getFecha());f.setHoraInicio(e.getHoraInicio());f.setHoraFin(e.getHoraFin());f.setTipo(e.getTipo());f.setMotivo(e.getMotivo());return f;
    }
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void guardarHorario(Long id,Long horarioId,HorarioForm f) {
        acceso.modificar(id);var p=psicologos.bloquearPorId(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        validarHoras(f.getHoraInicio(),f.getHoraFin());
        if(f.getDiaSemana()==null||f.getDiaSemana()<1||f.getDiaSemana()>7||f.getModalidad()==null||f.getActivo()==null)
            throw new IllegalArgumentException("Completa el día, la modalidad y el estado.");
        var h=horarioId==null?new Disponibilidad():horarioPropio(id,horarioId);
        for(var otro:horarios.findByPsicologo_IdOrderByDiaSemanaAscHoraInicioAsc(id)) {
            if(Objects.equals(otro.getId(),horarioId)||!otro.getDiaSemana().equals(f.getDiaSemana()))continue;
            if(otro.getHoraInicio().equals(f.getHoraInicio())&&otro.getHoraFin().equals(f.getHoraFin())&&otro.getModalidad()==f.getModalidad())
                throw new IllegalArgumentException("Ya existe esa franja; puedes editarla o reactivarla.");
            if(Boolean.TRUE.equals(f.getActivo())&&Boolean.TRUE.equals(otro.getActivo())
                    &&f.getHoraInicio().isBefore(otro.getHoraFin())&&f.getHoraFin().isAfter(otro.getHoraInicio()))
                throw new IllegalArgumentException("La franja se cruza con otro horario activo, incluso si tiene otra modalidad.");
        }
        h.setPsicologo(p);h.setDiaSemana(f.getDiaSemana());h.setHoraInicio(f.getHoraInicio());h.setHoraFin(f.getHoraFin());h.setModalidad(f.getModalidad());h.setActivo(f.getActivo());
        horarios.saveAndFlush(h);
    }
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void guardarExcepcion(Long id,Long excepcionId,ExcepcionForm f) {
        acceso.modificar(id);var p=psicologos.bloquearPorId(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        validarHoras(f.getHoraInicio(),f.getHoraFin());
        if(f.getFecha()==null||f.getTipo()==null)throw new IllegalArgumentException("Completa la fecha y el tipo de excepción.");
        if(f.getMotivo()!=null && f.getMotivo().length()>250)throw new IllegalArgumentException("El motivo admite hasta 250 caracteres.");
        var e=excepcionId==null?new ExcepcionDisponibilidad():excepcionPropia(id,excepcionId);
        boolean duplicado=excepciones.findByPsicologo_IdAndFecha(id,f.getFecha()).stream().anyMatch(x->
            !Objects.equals(x.getId(),excepcionId)&&x.getHoraInicio().equals(f.getHoraInicio())&&x.getHoraFin().equals(f.getHoraFin())&&x.getTipo()==f.getTipo());
        if(duplicado)throw new IllegalArgumentException("Ya existe esa excepción para la fecha y el horario.");
        e.setPsicologo(p);e.setFecha(f.getFecha());e.setHoraInicio(f.getHoraInicio());e.setHoraFin(f.getHoraFin());e.setTipo(f.getTipo());e.setMotivo(f.getMotivo());excepciones.saveAndFlush(e);
    }
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void eliminarHorario(Long id,Long horarioId) {
        acceso.modificar(id);psicologos.bloquearPorId(id);horarios.delete(horarioPropio(id,horarioId));horarios.flush();
    }
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void eliminarExcepcion(Long id,Long excepcionId) {
        acceso.modificar(id);psicologos.bloquearPorId(id);excepciones.delete(excepcionPropia(id,excepcionId));excepciones.flush();
    }
    @Transactional(readOnly=true)
    public ConsultaDisponibilidadDTO consultar(Long id,LocalDate fecha,Modalidad modalidad,Short especialidadId) {
        var p=acceso.consultar(id);
        if(fecha==null||modalidad==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Indica fecha y modalidad.");
        if(!Boolean.TRUE.equals(p.getActivo())||!Boolean.TRUE.equals(p.getUsuario().getActivo()))
            return new ConsultaDisponibilidadDTO(id,fecha,modalidad,List.of());
        if(especialidadId!=null&&p.getEspecialidades().stream().noneMatch(e->e.getId().equals(especialidadId)&&Boolean.TRUE.equals(e.getActivo())))
            return new ConsultaDisponibilidadDTO(id,fecha,modalidad,List.of());
        var base=new ArrayList<TramoHorario>();var ocupados=new ArrayList<TramoHorario>();
        for(var h:horarios.findByPsicologo_IdOrderByDiaSemanaAscHoraInicioAsc(id))
            if(Boolean.TRUE.equals(h.getActivo())&&h.getDiaSemana()==fecha.getDayOfWeek().getValue()&&h.getModalidad()==modalidad)
                base.add(new TramoHorario(h.getHoraInicio(),h.getHoraFin()));
        for(var e:excepciones.findByPsicologo_IdAndFecha(id,fecha))
            (e.getTipo()==TipoExcepcion.HABILITACION?base:ocupados).add(new TramoHorario(e.getHoraInicio(),e.getHoraFin()));
        // Las citas ocupan al profesional con independencia de la modalidad consultada.
        ocupados.addAll(agenda.ocupaciones(id,fecha));
        return new ConsultaDisponibilidadDTO(id,fecha,modalidad,CalculadorDisponibilidad.restar(base,ocupados));
    }
    /** El módulo de citas debe llamar a este método dentro de SU transacción, antes de guardar. */
    @Transactional(propagation=Propagation.MANDATORY)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
    public void validarParaReserva(Long id,LocalDate fecha,LocalTime inicio,LocalTime fin,Modalidad modalidad) {
        acceso.consultar(id);
        psicologos.bloquearPorId(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        validarHoras(inicio,fin);
        boolean libre=consultar(id,fecha,modalidad,null).intervalos().stream()
            .anyMatch(t->!inicio.isBefore(t.inicio())&&!fin.isAfter(t.fin()));
        if(!libre)throw new IllegalArgumentException("El intervalo solicitado ya no está disponible.");
    }
    private Disponibilidad horarioPropio(Long id,Long horarioId) {
        return horarios.findById(horarioId).filter(h->h.getPsicologo().getId().equals(id))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"El horario no pertenece a este profesional."));
    }
    private ExcepcionDisponibilidad excepcionPropia(Long id,Long excepcionId) {
        return excepciones.findById(excepcionId).filter(e->e.getPsicologo().getId().equals(id))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"La excepción no pertenece a este profesional."));
    }
    private static void validarHoras(LocalTime inicio,LocalTime fin) {
        if(inicio==null||fin==null||!fin.isAfter(inicio))throw new IllegalArgumentException("La hora de fin debe ser posterior al inicio.");
        if(inicio.getSecond()!=0||fin.getSecond()!=0||inicio.getNano()!=0||fin.getNano()!=0)throw new IllegalArgumentException("Ingresa horas en minutos completos.");
    }
}
