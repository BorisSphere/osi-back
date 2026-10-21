package com.osinergmin.osinergmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.osinergmin.osinergmin.service.EstimacionCalculator.Distribucion;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Prueba unitaria pura (sin Spring ni base de datos). Evidencia del criterio CA06. */
class EstimacionCalculatorTest {

    private final EstimacionCalculator calculator = new EstimacionCalculator();

    @Test
    void ca06_78HorasDeDesarrolloDan100HorasTotales() {
        Distribucion d = calculator.calcular(new BigDecimal("78"));

        assertThat(d.analisis()).isEqualByComparingTo("5.00");
        assertThat(d.desarrollo()).isEqualByComparingTo("78.00");
        assertThat(d.pruebas()).isEqualByComparingTo("15.00");
        assertThat(d.documentacion()).isEqualByComparingTo("2.00");
        assertThat(d.total()).isEqualByComparingTo("100.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "7.5", "50", "123.45", "999", "0.01"})
    void laSumaDeLasEtapasEsIgualAlTotal(String hh) {
        Distribucion d = calculator.calcular(new BigDecimal(hh));

        BigDecimal suma = d.analisis().add(d.desarrollo()).add(d.pruebas()).add(d.documentacion());
        assertThat(suma).isEqualByComparingTo(d.total());
        assertThat(d.documentacion().signum()).isGreaterThanOrEqualTo(0); // CK_LAB_EST_HH
    }

    @Test
    void todosLosValoresTienenDosDecimales() {
        Distribucion d = calculator.calcular(new BigDecimal("50"));

        assertThat(d.total().scale()).isEqualTo(2);
        assertThat(d.analisis().scale()).isEqualTo(2);
        assertThat(d.documentacion().scale()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5"})
    void rechazaHorasNoPositivas(String hh) {
        assertThatThrownBy(() -> calculator.calcular(new BigDecimal(hh)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaNulo() {
        assertThatThrownBy(() -> calculator.calcular(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
