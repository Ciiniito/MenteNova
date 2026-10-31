package com.mentenova.repository;
import com.mentenova.entity.Disponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DisponibilidadRepository extends JpaRepository<Disponibilidad,Long> {
    List<Disponibilidad> findByPsicologo_IdOrderByDiaSemanaAscHoraInicioAsc(Long id);
}
