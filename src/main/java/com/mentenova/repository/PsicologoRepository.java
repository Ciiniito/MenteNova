package com.mentenova.repository;

import com.mentenova.entity.Psicologo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PsicologoRepository extends JpaRepository<Psicologo, Long> {
    List<Psicologo> findAllByOrderByUsuario_ApellidosAscUsuario_NombresAsc();
    Optional<Psicologo> findByUsuario_Id(Long idUsuario);
    Optional<Psicologo> findByNumeroColegiatura(String numeroColegiatura);
    boolean existsByNumeroColegiatura(String numeroColegiatura);
    boolean existsByNumeroColegiaturaAndIdNot(String numeroColegiatura, Long id);
    boolean existsByUsuario_Id(Long idUsuario);
}
