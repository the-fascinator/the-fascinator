package com.googlecode.fascinator.portal.report;

import java.awt.Color;
import java.util.ArrayList;

public class BarChartData implements ChartData {

    public String rowLabel;
    protected String colLabel;
    protected String title;
    protected ArrayList<DataEntry> entries;

    protected LabelPos colLabelPos;
    protected LabelPos colLegendPos;

    protected boolean useSeriesColor;
    protected Color baseSeriesColor;

    protected int imgW;
    protected int imgH;
    protected boolean stacked;

    public BarChartData(String title, String rowLabel, String colLabel,
            LabelPos colLabelPos, LabelPos colLegendPos, int imgW, int imgH,
            boolean stacked) {
        this.rowLabel = rowLabel;
        this.colLabel = colLabel;
        this.title = title;
        this.colLabelPos = colLabelPos;
        this.colLegendPos = colLegendPos;
        this.imgW = imgW;
        this.imgH = imgH;
        this.stacked = stacked;
        useSeriesColor = false;
        entries = new ArrayList<DataEntry>();
    }

    public void addEntry(Number value, String rowKey, String colKey) {
        entries.add(new DataEntry(value, rowKey, colKey));
    }

    public void addEntry(Number value, String rowKey, String colKey,
            Color seriesColor) {
        entries.add(new DataEntry(value, rowKey, colKey, seriesColor));
    }

    public ArrayList<DataEntry> getEntries() {
        return entries;
    }

    public int getImgW() {
        return imgW;
    }

    public void setImgW(int imgW) {
        this.imgW = imgW;
    }

    public int getImgH() {
        return imgH;
    }

    public void setImgH(int imgH) {
        this.imgH = imgH;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getColLabel() {
        return colLabel;
    }

    public void setColLabel(String colLabel) {
        this.colLabel = colLabel;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public LabelPos getColLabelPos() {
        return colLabelPos;
    }

    public void setColLabelPos(LabelPos colLabelPos) {
        this.colLabelPos = colLabelPos;
    }

    public LabelPos getColLegendPos() {
        return colLegendPos;
    }

    public void setColLegendPos(LabelPos colLegendPos) {
        this.colLegendPos = colLegendPos;
    }

    public boolean isUseSeriesColor() {
        return useSeriesColor;
    }

    public void setUseSeriesColor(boolean useSeriesColor) {
        this.useSeriesColor = useSeriesColor;
    }

    public Color getBaseSeriesColor() {
        return baseSeriesColor;
    }

    public void setBaseSeriesColor(Color baseSeriesColor) {
        this.baseSeriesColor = baseSeriesColor;
    }

    public boolean isStacked() {
        return stacked;
    }

    public void setStacked(boolean stacked) {
        this.stacked = stacked;
    }

    public class DataEntry {
        protected Number value;
        protected String rowKey;
        protected String colKey;
        protected Color seriesColor;

        DataEntry(Number value, String rowKey, String colKey) {
            this.value = value;
            this.rowKey = rowKey;
            this.colKey = colKey;
        }

        DataEntry(Number value, String rowKey, String colKey, Color seriesColor) {
            this.value = value;
            this.rowKey = rowKey;
            this.colKey = colKey;
            this.seriesColor = seriesColor;
        }

        public Number getValue() {
            return value;
        }

        public String getRowKey() {
            return rowKey;
        }

        public String getColKey() {
            return colKey;
        }

        public Color getSeriesColor() {
            return seriesColor;
        }
    }

    public enum LabelPos {
        HIDDEN, HORIZONTAL, VERTICAL, SLANTED, LEFT, RIGHT
    }
}