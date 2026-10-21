package com.osinergmin.osinergmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Requerimiento para las bandejas de estimación y aprobación. hhTotal es null si aún no se estimó;
 * motivoRechazo solo viene cuando el estado es RECHAZADO.
 */
public record RequerimientoBandejaResponse(
        long id,
        String codigo,
        String titulo,
        String aplicacion,
        String prioridad,
        String estado,
        BigDecimal hhTotal,
        LocalDate fechaRegistro,
        String motivoRechazo) {
}
