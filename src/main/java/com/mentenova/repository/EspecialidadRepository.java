package com.mentenova.repository;
import com.mentenova.entity.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EspecialidadRepository extends JpaRepository<Especialidad,Short> {
    List<Especialidad> findAllByOrderByNombreAsc();
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre,Short id);
}
