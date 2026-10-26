package com.osinergmin.osinergmin.reportes;

import net.sf.jasperreports.charts.JRChart;
import net.sf.jasperreports.charts.JRChartCustomizer;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.ui.TextAnchor;

/** Gráfico "HH aprobadas por aplicación": barras horizontales azules con el valor al final de cada una. */
public class GraficoHorasCustomizer implements JRChartCustomizer {

    @Override
    public void customize(JFreeChart chart, JRChart jasperChart) {
        BarRenderer renderer = new BarRenderer();
        renderer.setSeriesPaint(0, EstiloGrafico.AZUL_OSCURO);
        EstiloGrafico.aplicar(chart, renderer, "#,##0.00");

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setOrientation(PlotOrientation.HORIZONTAL);
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT); // escala abajo, no arriba
        // Valor a la derecha del final de la barra (antes quedaba encima y se cortaba en barras cortas).
        renderer.setDefaultPositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.OUTSIDE3, TextAnchor.CENTER_LEFT));
        plot.getRangeAxis().setUpperMargin(0.25); // espacio para el valor de la barra más larga
    }
}
