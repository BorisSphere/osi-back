package com.osinergmin.osinergmin.repository;

import com.osinergmin.osinergmin.model.Aplicacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AplicacionRepository extends JpaRepository<Aplicacion, Long> {

    List<Aplicacion> findAllByOrderByCodigoAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    /** Mayor correlativo de los códigos LAB-nnn (0 si no hay ninguno). Numérico para que LAB-1000 > LAB-999. */
    @Query(value = "SELECT NVL(MAX(TO_NUMBER(SUBSTR(CODIGO, 5))), 0) FROM LAB_APLICACION"
            + " WHERE REGEXP_LIKE(CODIGO, '^LAB-[0-9]+$')", nativeQuery = true)
    long maxCorrelativo();
}
