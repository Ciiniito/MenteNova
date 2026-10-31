package com.mentenova.service;
import com.mentenova.dto.EspecialidadForm;
import com.mentenova.entity.*;
import com.mentenova.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class EspecialidadService {
    private final EspecialidadRepository especialidades;
    private final PsicologoRepository psicologos;
    public EspecialidadService(EspecialidadRepository especialidades,PsicologoRepository psicologos) {
        this.especialidades=especialidades;this.psicologos=psicologos;
    }
    @Transactional(readOnly=true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA','PSICOLOGO')")
    public List<Especialidad> listar() {return especialidades.findAllByOrderByNombreAsc();}
    @Transactional(readOnly=true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public EspecialidadForm formulario(Short id) {
        var e=requerir(id);var f=new EspecialidadForm();
        f.setNombre(e.getNombre());f.setDescripcion(e.getDescripcion());f.setActivo(e.getActivo());return f;
    }
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void guardar(Short id,EspecialidadForm f) {
        if(id==null?especialidades.existsByNombreIgnoreCase(f.getNombre()):especialidades.existsByNombreIgnoreCaseAndIdNot(f.getNombre(),id))
            throw new IllegalArgumentException("Ya existe una especialidad con ese nombre.");
        var e=id==null?new Especialidad():requerir(id);
        e.setNombre(f.getNombre());e.setDescripcion(f.getDescripcion());e.setActivo(f.getActivo());
        especialidades.saveAndFlush(e);
    }
    @Transactional(readOnly=true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Set<Short> asignadas(Long id) {
        var p=psicologos.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        var ids=new LinkedHashSet<Short>();p.getEspecialidades().forEach(e->ids.add(e.getId()));return ids;
    }
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void asignar(Long id,List<Short> seleccion) {
        var p=psicologos.bloquearPorId(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        Set<Short> ids=new LinkedHashSet<>(seleccion==null?List.of():seleccion);
        var elegidas=especialidades.findAllById(ids);
        if(elegidas.size()!=ids.size()) throw new IllegalArgumentException("Selecciona especialidades existentes.");
        Set<Short> anteriores=new HashSet<>();p.getEspecialidades().forEach(e->anteriores.add(e.getId()));
        if(elegidas.stream().anyMatch(e->!Boolean.TRUE.equals(e.getActivo())&&!anteriores.contains(e.getId())))
            throw new IllegalArgumentException("Solo puedes agregar especialidades activas.");
        p.getEspecialidades().clear();p.getEspecialidades().addAll(elegidas);psicologos.flush();
    }
    private Especialidad requerir(Short id) {
        return especialidades.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"La especialidad no existe."));
    }
}
