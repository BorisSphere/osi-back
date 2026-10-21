package com.osinergmin.osinergmin.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Regla 5 / 78 / 15 / 2 (Apéndice B, Anexo 2 de las Bases Integradas).
 * El dato de entrada son las HH de desarrollo + prueba unitaria, que equivalen al 78 % del total:
 * 78.00 HH desarrollo / 0.78 = 100.00 HH total → 5.00 / 78.00 / 15.00 / 2.00.
 */
@Component
public class EstimacionCalculator {

    private static final BigDecimal DESARROLLO = new BigDecimal("0.78");
    private static final BigDecimal ANALISIS = new BigDecimal("0.05");
    private static final BigDecimal PRUEBAS = new BigDecimal("0.15");

    public record Distribucion(
            BigDecimal analisis, BigDecimal desarrollo, BigDecimal pruebas, BigDecimal documentacion, BigDecimal total) {}

    /**
     * Cada etapa se redondea a 2 decimales (HALF_UP) y la documentación absorbe la diferencia de
     * redondeo, así la suma de las etapas siempre es igual al total. Misma lógica que el front.
     */
    public Distribucion calcular(BigDecimal hhDesarrollo) {
        if (hhDesarrollo == null || hhDesarrollo.signum() <= 0) {
            throw new IllegalArgumentException("Las horas de desarrollo deben ser mayores a cero.");
        }
        BigDecimal desarrollo = hhDesarrollo.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = desarrollo.divide(DESARROLLO, 2, RoundingMode.HALF_UP);
        BigDecimal analisis = total.multiply(ANALISIS).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pruebas = total.multiply(PRUEBAS).setScale(2, RoundingMode.HALF_UP);
        BigDecimal documentacion = total.subtract(desarrollo).subtract(analisis).subtract(pruebas);
        return new Distribucion(analisis, desarrollo, pruebas, documentacion, total);
    }
}
