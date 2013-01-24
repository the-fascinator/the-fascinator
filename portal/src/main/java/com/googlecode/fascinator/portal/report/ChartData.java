package com.googlecode.fascinator.portal.report;

import java.awt.Color;

public interface ChartData {
    public void addEntry(Number value, String rowLabel, String colLabel);

    public void addEntry(Number value, String rowLabel, String colLabel,
            Color entryColor);
}