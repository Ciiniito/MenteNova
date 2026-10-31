package com.mentenova.repository;
import com.mentenova.entity.ExcepcionDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDate;
public interface ExcepcionDisponibilidadRepository extends JpaRepository<ExcepcionDisponibilidad,Long> {
    List<ExcepcionDisponibilidad> findByPsicologo_IdOrderByFechaAscHoraInicioAsc(Long id);
    List<ExcepcionDisponibilidad> findByPsicologo_IdAndFecha(Long id,LocalDate fecha);
}
