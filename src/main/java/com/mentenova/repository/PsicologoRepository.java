package com.mentenova.repository;

import com.mentenova.entity.Psicologo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PsicologoRepository extends JpaRepository<Psicologo, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Psicologo p where p.id = :id")
    Optional<Psicologo> bloquearPorId(@Param("id") Long id);

    List<Psicologo> findAllByOrderByUsuario_ApellidosAscUsuario_NombresAsc();
    Optional<Psicologo> findByUsuario_Id(Long idUsuario);
    Optional<Psicologo> findByNumeroColegiatura(String numeroColegiatura);
    boolean existsByNumeroColegiatura(String numeroColegiatura);
    boolean existsByNumeroColegiaturaAndIdNot(String numeroColegiatura, Long id);
    boolean existsByUsuario_Id(Long idUsuario);
}
