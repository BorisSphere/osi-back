package com.osinergmin.osinergmin.repository;

import com.osinergmin.osinergmin.model.EstadoRequerimiento;
import com.osinergmin.osinergmin.model.Requerimiento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequerimientoRepository extends JpaRepository<Requerimiento, Long> {

    // Se cargan aplicación y creador en la misma consulta para evitar una consulta por fila.
    @EntityGraph(attributePaths = {"aplicacion", "creadoPor"})
    List<Requerimiento> findAllByOrderByFechaCreacionDescIdDesc();

    @EntityGraph(attributePaths = {"aplicacion", "creadoPor"})
    List<Requerimiento> findAllByEstadoOrderByFechaCreacionDescIdDesc(EstadoRequerimiento estado);

    @EntityGraph(attributePaths = {"aplicacion", "creadoPor"})
    Optional<Requerimiento> findWithDetalleById(Long id);
}
