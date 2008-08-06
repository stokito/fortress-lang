package com.sun.fortress.tests.performance;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.SortedMap;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.xy.XYSeries;

public class ChartWriter extends Thread {

    final private String chartDirectory;
    final private String testcaseName;
    final private TestSuiteData testSuiteData;
    
    ChartWriter(String chartDirectory, String testcaseName, TestSuiteData testSuiteData) {
        this.chartDirectory = chartDirectory;
        this.testcaseName = testcaseName;
        this.testSuiteData = testSuiteData;
    }
    
    @Override
    public void run() {
        XYSeries series = new XYSeries(testcaseName);
        XYSeries line = new XYSeries(testcaseName + " slope");
        SortedMap<Integer, Double> performance = testSuiteData.testData.get(testcaseName);
        LineFunction2D lineFunction = testSuiteData.makeLineFunction(performance);
        
        for (Map.Entry<Integer, Double> entry : performance.entrySet()) {
            Integer revision = entry.getKey();
            series.add(revision, entry.getValue());
            line.add(revision.doubleValue(), lineFunction.getValue(revision
                    .doubleValue()));
        }
        if (series.getItemCount() > 1) {
            JFreeChart chart = testSuiteData.createChartEntity(testcaseName, series, line,
                    performance);
            
            ChartRenderingInfo info = new ChartRenderingInfo(
                    new StandardEntityCollection());
            OutputStream htmlOut = null;
            PrintWriter htmlWriter = null;
            try {
                String htmlPath = chartDirectory + File.separator
                        + testcaseName + ".html";
                htmlOut = new BufferedOutputStream(new FileOutputStream(
                        new File(htmlPath)));
                htmlWriter = new PrintWriter(htmlOut);
                htmlWriter.println("<html><head></head><body>");
                String filePath = chartDirectory + File.separator
                        + testcaseName + ".png";
                ChartUtilities.saveChartAsPNG(new File(filePath), chart,
                        500, 300, info);
                htmlWriter.println("<IMG SRC=\"" + testcaseName + ".png"
                        + "\" " + "BORDER=0 USEMAP=\"#" + testcaseName
                        + "\">");
                ChartUtilities.writeImageMap(htmlWriter, testcaseName,
                        info, false);
                htmlWriter.println("</body></html>");                    
            } catch (IOException e) {
                System.err.println(e.getMessage());
            } finally {
                PerformanceLogMonitor.closeStream(htmlWriter);
                PerformanceLogMonitor.closeStream(htmlOut);
            }
        }        
    }

    
}
