package com.mentenova.repository;

import com.mentenova.entity.Paciente;
import com.mentenova.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    boolean existsByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumentoAndIdNot(String tipoDocumento, String numeroDocumento, Long id);

    Optional<Paciente> findByUsuario_Id(Long idUsuario);

    @Query("select u from Usuario u where lower(u.correo) = lower(:correo)")
    Optional<Usuario> buscarUsuarioPorCorreo(@Param("correo") String correo);
}
