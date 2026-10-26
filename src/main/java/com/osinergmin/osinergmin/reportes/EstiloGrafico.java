package com.osinergmin.osinergmin.reportes;

import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;

/**
 * Estilo común de los gráficos de barras del reporte: plano, sin leyenda, grilla tenue y el valor
 * escrito sobre cada barra (identidad nunca solo por color). Los colores son los mismos de la pantalla.
 */
final class EstiloGrafico {

    static final Color AZUL = new Color(0x3B82F6);
    static final Color VERDE = new Color(0x16A34A);
    static final Color AMBAR = new Color(0xF59E0B);
    static final Color ROJO = new Color(0xDC2626);
    static final Color AZUL_OSCURO = new Color(0x1D4ED8);
    private static final Color TEXTO = new Color(0x1E293B);
    private static final Color TEXTO_SUAVE = new Color(0x475569);
    private static final Color GRILLA = new Color(0xE2E8F0);

    private EstiloGrafico() {
    }

    static void aplicar(JFreeChart chart, BarRenderer renderer, String formatoValor) {
        chart.removeLegend();
        chart.setBackgroundPaint(Color.WHITE);
        chart.setBorderVisible(false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(GRILLA);
        plot.setDomainGridlinesVisible(false);

        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);
        renderer.setMaximumBarWidth(0.14);
        renderer.setItemMargin(0);
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator("{2}", new DecimalFormat(formatoValor)));
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelFont(new Font("DejaVu Sans", Font.BOLD, 8));
        renderer.setDefaultItemLabelPaint(TEXTO);
        plot.setRenderer(renderer);

        Font ejes = new Font("DejaVu Sans", Font.PLAIN, 8);
        plot.getDomainAxis().setTickLabelFont(ejes);
        plot.getDomainAxis().setTickLabelPaint(TEXTO_SUAVE);
        plot.getDomainAxis().setAxisLineVisible(false);
        // Menos margen entre categorías y etiquetas algo más anchas que la barra: así no se cortan
        // nombres como "Registrados".
        plot.getDomainAxis().setCategoryMargin(0.12);
        plot.getDomainAxis().setLowerMargin(0.02);
        plot.getDomainAxis().setUpperMargin(0.02);
        plot.getDomainAxis().setMaximumCategoryLabelWidthRatio(1.4f);
        plot.getRangeAxis().setTickLabelFont(ejes);
        plot.getRangeAxis().setTickLabelPaint(TEXTO_SUAVE);
        plot.getRangeAxis().setAxisLineVisible(false);
        plot.getRangeAxis().setUpperMargin(0.18); // espacio para el valor sobre la barra
    }
}
