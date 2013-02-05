package com.googlecode.fascinator.portal.report.type;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import com.googlecode.fascinator.api.indexer.IndexerException;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.portal.services.ScriptingServices;

public interface ChartHandler {

    public abstract Date getFromDate();

    public abstract void setFromDate(Date fromDate);

    public abstract Date getToDate();

    public abstract void setToDate(Date toDate);

    public abstract void renderChart(OutputStream outputStream)
            throws IOException, IndexerException;

    public abstract void setImgW(int imgW);

    public abstract void setImgH(int imgH);

    public abstract void setScriptingServices(
            ScriptingServices scriptingServices);

    void setSystemConfig(JsonSimple systemConfig);

}