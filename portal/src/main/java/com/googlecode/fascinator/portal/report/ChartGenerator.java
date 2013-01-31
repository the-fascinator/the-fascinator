package com.googlecode.fascinator.portal.report;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;

public class ChartGenerator {

    public static JFreeChart getBarChart(OutputStream out, BarChartData data)
            throws IOException {
        // create a dataset...
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (BarChartData.DataEntry entry : data.getEntries()) {
            dataset.addValue(entry.getValue(), entry.getRowKey(),
                    entry.getColKey());
        }
        // create a chart...
        JFreeChart chart = null;
        if (data.isStacked()) {
            chart = ChartFactory.createStackedBarChart(data.getTitle(), // title
                    data.getColLabel(), // domain axis label
                    data.getRowLabel(), // range axis label
                    dataset, // data
                    PlotOrientation.VERTICAL, // orientation
                    !data.getColLegendPos()
                            .equals(BarChartData.LabelPos.HIDDEN), // legend
                    false, // tooltips?
                    false // URLs?
                    );
        } else {
            chart = ChartFactory.createBarChart(data.getTitle(), // title
                    data.getColLabel(), // domain axis label
                    data.getRowLabel(), // range axis label
                    dataset, // data
                    PlotOrientation.VERTICAL, // orientation
                    !data.getColLegendPos()
                            .equals(BarChartData.LabelPos.HIDDEN), // legend
                    false, // tooltips?
                    false // URLs?
                    );
        }

        chart.setBackgroundPaint(Color.white);

        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(55, 52, 80));
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.getRangeAxis().setStandardTickUnits(
                NumberAxis.createIntegerTickUnits());

        // disable bar outlines...
        final BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setItemMargin(0.10);

        if (!data.getColLegendPos().equals(BarChartData.LabelPos.HIDDEN)) {
            chart.getLegend().setFrame(BlockBorder.NONE);
        }

        if (!data.isUseSeriesColor()) {
            renderer.setBasePaint(data.getBaseSeriesColor());
            renderer.setAutoPopulateSeriesPaint(false);
        } else {
            int curSeries = 0;
            for (BarChartData.DataEntry entry : data.getEntries()) {
                renderer.setSeriesPaint(curSeries++, entry.getSeriesColor());
            }
        }

        if (data.getColLegendPos() == BarChartData.LabelPos.LEFT) {
            chart.getLegend().setPosition(RectangleEdge.LEFT);
        }

        if (data.getColLegendPos() == BarChartData.LabelPos.RIGHT) {
            chart.getLegend().setPosition(RectangleEdge.RIGHT);
        }

        renderer.setItemLabelsVisible(true);

        if (data.getColLabelPos().equals(BarChartData.LabelPos.SLANTED)) {
            final ItemLabelPosition p = new ItemLabelPosition(
                    ItemLabelAnchor.INSIDE12, TextAnchor.CENTER_RIGHT,
                    TextAnchor.CENTER_RIGHT, -Math.PI / 2.0);
            renderer.setPositiveItemLabelPosition(p);

            final ItemLabelPosition p2 = new ItemLabelPosition(
                    ItemLabelAnchor.OUTSIDE12, TextAnchor.CENTER_LEFT,
                    TextAnchor.CENTER_LEFT, -Math.PI / 2.0);
            renderer.setPositiveItemLabelPositionFallback(p2);
            final CategoryAxis domainAxis = plot.getDomainAxis();
            domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        }

        if (data.getColLabelPos().equals(BarChartData.LabelPos.VERTICAL)) {
            final CategoryAxis domainAxis = plot.getDomainAxis();
            domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        }

        return chart;
    }

    public static void renderCSV(OutputStream out,
            ArrayList<BarChartData> barChartDataList) {

    }

    public static void renderPNGBarChart(OutputStream out, BarChartData data)
            throws IOException {
        JFreeChart chart = getBarChart(out, data);
        ChartUtilities.writeBufferedImageAsPNG(out,
                chart.createBufferedImage(data.getImgW(), data.getImgH()));
    }
}