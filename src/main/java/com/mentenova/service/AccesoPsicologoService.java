package com.mentenova.service;

import com.mentenova.entity.*;
import com.mentenova.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly=true)
public class AccesoPsicologoService {
    private final UsuarioRepository usuarios;
    private final PsicologoRepository psicologos;
    public AccesoPsicologoService(UsuarioRepository usuarios,PsicologoRepository psicologos) {
        this.usuarios=usuarios;this.psicologos=psicologos;
    }
    private Usuario actual() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated()) throw new AccessDeniedException("Inicia sesión.");
        var u=usuarios.findByCorreo(auth.getName()).orElseThrow(()->new AccessDeniedException("Cuenta no disponible."));
        if(!Boolean.TRUE.equals(u.getActivo()) || !Boolean.TRUE.equals(u.getRol().getActivo()))
            throw new AccessDeniedException("Cuenta no disponible.");
        return u;
    }
    public Psicologo consultar(Long id) {
        var u=actual();String rol=u.getRol().getNombre();
        if(!rol.equals("ADMINISTRADOR") && !rol.equals("RECEPCIONISTA") && !rol.equals("PSICOLOGO"))
            throw new AccessDeniedException("Acceso no autorizado.");
        // Comprueba la propiedad antes de buscar el identificador solicitado.
        if(rol.equals("PSICOLOGO")) {
            var propio=psicologos.findByUsuario_Id(u.getId()).orElseThrow(()->new AccessDeniedException("Sin ficha profesional."));
            if(!propio.getId().equals(id)) throw new AccessDeniedException("Solo puedes acceder a tus propios datos.");
            return propio;
        }
        return psicologos.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"El psicólogo no existe."));
    }
    public Psicologo modificar(Long id) {
        if(actual().getRol().getNombre().equals("RECEPCIONISTA")) throw new AccessDeniedException("Solo tienes permiso de consulta.");
        var p=consultar(id);
        if(!Boolean.TRUE.equals(p.getActivo()) || !Boolean.TRUE.equals(p.getUsuario().getActivo()))
            throw new IllegalArgumentException("Activa al psicólogo antes de modificar su disponibilidad.");
        return p;
    }
    public Long propio() {
        var u=actual();
        if(!u.getRol().getNombre().equals("PSICOLOGO")) throw new AccessDeniedException("Acceso exclusivo para psicólogos.");
        return psicologos.findByUsuario_Id(u.getId()).map(Psicologo::getId)
                .orElseThrow(()->new AccessDeniedException("Sin ficha profesional."));
    }
    public Long filtro(Long solicitado) {
        if(actual().getRol().getNombre().equals("PSICOLOGO")) {
            Long propio=propio();
            if(solicitado!=null && !propio.equals(solicitado)) throw new AccessDeniedException("Solo puedes consultar tu agenda.");
            return propio;
        }
        if(solicitado!=null) consultar(solicitado);
        else {
            String rol=actual().getRol().getNombre();
            if(!rol.equals("ADMINISTRADOR")&&!rol.equals("RECEPCIONISTA")) throw new AccessDeniedException("Acceso no autorizado.");
        }
        return solicitado;
    }
}
