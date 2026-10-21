package com.osinergmin.osinergmin.repository;

import com.osinergmin.osinergmin.model.Estimacion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstimacionRepository extends JpaRepository<Estimacion, Long> {

    Optional<Estimacion> findByRequerimientoId(Long requerimientoId);
}
