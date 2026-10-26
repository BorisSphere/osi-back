package com.osinergmin.osinergmin.reportes;

import java.awt.Color;
import java.awt.Paint;
import java.util.Map;
import net.sf.jasperreports.charts.JRChart;
import net.sf.jasperreports.charts.JRChartCustomizer;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.category.BarRenderer;

/** Gráfico "Requerimientos por estado": cada barra con el color de su estado, como en el Dashboard. */
public class GraficoEstadosCustomizer implements JRChartCustomizer {

    private static final Map<String, Color> COLORES = Map.of(
            "Registrados", EstiloGrafico.AZUL,
            "Estimados", EstiloGrafico.AMBAR,
            "Aprobados", EstiloGrafico.VERDE,
            "Rechazados", EstiloGrafico.ROJO);

    @Override
    public void customize(JFreeChart chart, JRChart jasperChart) {
        BarRenderer renderer = new BarRenderer() {
            @Override
            public Paint getItemPaint(int row, int column) {
                Comparable<?> estado = getPlot().getDataset().getColumnKey(column);
                return COLORES.getOrDefault(String.valueOf(estado), EstiloGrafico.AZUL);
            }
        };
        EstiloGrafico.aplicar(chart, renderer, "#,##0");
        ((NumberAxis) chart.getCategoryPlot().getRangeAxis()).setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    }
}
