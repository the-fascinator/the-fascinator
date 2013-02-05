package com.googlecode.fascinator.portal.report.type;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.httpclient.methods.GetMethod;

import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.IndexerException;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.common.BasicHttpClient;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.solr.SolrDoc;
import com.googlecode.fascinator.common.solr.SolrResult;
import com.googlecode.fascinator.portal.report.BarChartData;
import com.googlecode.fascinator.portal.report.ChartData;
import com.googlecode.fascinator.portal.report.ChartGenerator;
import com.googlecode.fascinator.portal.services.ScriptingServices;

public class PublishedRecordsByTypeChartHandler implements ChartHandler {

    private ScriptingServices scriptingServices;

    private ChartData chartData;
    private String query = "*:*";
    private int imgW = 550;
    private int imgH = 400;
    private Date fromDate = null;
    private Date toDate = null;

    public PublishedRecordsByTypeChartHandler() {
        chartData = new BarChartData("", "", "", BarChartData.LabelPos.HIDDEN,
                BarChartData.LabelPos.LEFT, imgW, imgH, false);
        ((BarChartData) chartData).setUseSeriesColor(true);
    }

    @Override
    public Date getFromDate() {
        return fromDate;
    }

    @Override
    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    @Override
    public Date getToDate() {
        return toDate;
    }

    @Override
    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    @Override
    public void renderChart(OutputStream outputStream) throws IOException,
            IndexerException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        ((BarChartData) chartData).setTitle("[Insert Title]");

        Indexer indexer = scriptingServices.getIndexer();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        query += " AND published:true AND oai_set:default AND date_created:["
                + dateFormat.format(fromDate) + "T00:00:00.000Z TO "
                + dateFormat.format(toDate) + "T23:59:59.999Z]";
        SearchRequest request = new SearchRequest(query);
        int start = 0;
        int pageSize = 10;
        request.setParam("start", "" + start);
        request.setParam("rows", "" + pageSize);
        indexer.search(request, result);
        SolrResult resultObject = new SolrResult(result.toString());
        int numFound = resultObject.getNumFound();
        int datasetCount = 0;

        while (true) {
            List<SolrDoc> results = resultObject.getResults();

            datasetCount += resultObject.getResults().size();

            start += pageSize;
            if (start > numFound) {
                break;
            }
            request.setParam("start", "" + start);
            result = new ByteArrayOutputStream();
            indexer.search(request, result);
            resultObject = new SolrResult(result.toString());
        }

        BasicHttpClient client = new BasicHttpClient(
                "http://localhost:9001/mint/default/api/query.script?callType=test&dateFrom="
                        + dateFormat.format(fromDate) + "&dateTo="
                        + dateFormat.format(toDate));
        GetMethod get = new GetMethod(
                "http://localhost:9001/mint/default/api/query.script?callType=test&dateFrom="
                        + dateFormat.format(fromDate) + "&dateTo="
                        + dateFormat.format(toDate));
        client.executeMethod(get);

        JsonSimple mintResult = new JsonSimple(get.getResponseBodyAsString());

        chartData.addEntry(
                mintResult.getInteger(0, "Parties")
                        + mintResult.getInteger(0, "Parties_People"), "Party",
                "Published Records", new Color(98, 157, 209));
        chartData.addEntry(datasetCount, "Collection", "Published Records",
                new Color(41, 127, 213));
        chartData.addEntry(mintResult.getInteger(0, "Activities"), "Activity",
                "Published Records", new Color(127, 143, 169));
        chartData.addEntry(mintResult.getInteger(0, "Services"), "Service",
                "Published Records", new Color(45, 127, 217));

        ChartGenerator
                .renderPNGBarChart(outputStream, (BarChartData) chartData);
    }

    @Override
    public void setImgW(int imgW) {
        ((BarChartData) chartData).setImgW(imgW);
    }

    @Override
    public void setImgH(int imgH) {
        ((BarChartData) chartData).setImgH(imgH);
    }

    @Override
    public void setScriptingServices(ScriptingServices scriptingServices) {
        this.scriptingServices = scriptingServices;
    }
}
