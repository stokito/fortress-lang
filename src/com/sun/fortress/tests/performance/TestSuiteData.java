/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.tests.performance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

public class TestSuiteData implements Serializable {

    /**
     * Add a serial version UID to make Eclipse happy.
     */
    private static final long serialVersionUID = -2469807233782317273L;

    final Map<Integer, String> revisionDate;
    
    final Map<String, SortedMap<Integer, Double>> testData;

    /** The number of columns in our html table */
    private final static int NUMCOLUMNS = 2;

    /** The names of test cases to monitor */
    private final static String[] SPECIAL_TESTCASES = { "buffons",
            "WordCountSmall", "nestedTransactions1", "ArrayListQuick",
            "FuncOfFuncTest", "overloadTest6", "OverloadConstructor3",
            "whileTest", "caseTest", "genericTest4", "traitTest1" };

    TestSuiteData() {
        testData = new HashMap<String, SortedMap<Integer, Double>>();
        revisionDate = new HashMap<Integer, String>();
    }
    
    public void writeHtml(String chartDirectory) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>Performance Measures</title></head>\n");
        html.append("<body>");
        html.append("<h2>Performance Measures</h2>");
        html.append("Click on the image to see the imagemap.<p>\n");
        html.append("<table>\n");
        File directory = new File(chartDirectory);
        File[] images = directory.listFiles(new ExtensionFilenameFilter(".png"));
        makeHtmlEachFile(html, images);
        writeHtmlFile(chartDirectory, html);
    }
    
    /**
     * Helper function to {@link #writeHtml(String) writeHtml}.
     */    
    private void makeHtmlEachFile(StringBuilder html, File[] images) {
        int counter = 0;        
        for (File image : images) {
                if ((counter % NUMCOLUMNS) == 0) {
                    html.append("<tr>");
                }
                html.append("<td>\n");
                String linkname = image.getName().replaceFirst("\\.png","\\.html");
                html.append("<a href=\"");
                html.append(linkname);
                html.append("\">");
                html.append("<image src=\"");
                html.append(image.getName());
                html.append("\">");
                html.append("</a>\n");
                html.append("</td>\n");
                counter++;                
        }
    }

    /**
     * Helper function to {@link #writeHtml(String) writeHtml}.
     */
    private void writeHtmlFile(String chartDirectory, StringBuilder html) {
        String indexHtml = chartDirectory + File.separator + "index.html";            
        html.append("</table></body></html>");
        FileOutputStream output = null;
        PrintStream printer = null;
        try {
            output = new FileOutputStream(indexHtml);
            printer = new PrintStream(output);
            printer.print(html.toString());
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        } finally {
            PerformanceLogMonitor.closeStream(printer);
            PerformanceLogMonitor.closeStream(output);
        }
    }
    
    
    public double getSlope(double x1, double y1, double x2, double y2) {
        if (x1 == x2)
            return 0;
        else
            return (y2 - y1) / (x2 - x1);
    }

    public double getIntercept(double x1, double y1, double slope) {
        return (y1 - slope * x1);
    }

    public LineFunction2D makeLineFunction(
            SortedMap<Integer, Double> performance) {
        double x1 = performance.firstKey().doubleValue();
        double y1 = performance.get(performance.firstKey());
        double x2 = performance.lastKey().doubleValue();
        double y2 = performance.get(performance.lastKey());
        double slope = getSlope(x1, y1, x2, y2);
        double intercept = getIntercept(x1, y1, slope);
        LineFunction2D lineFunction = new LineFunction2D(intercept, slope);
        return lineFunction;
    }

    public void writeCharts(String chartDirectory) {
        for (String testcaseName : testData.keySet()) {
            ChartWriter writer = new ChartWriter(chartDirectory, testcaseName, this);
            writer.run();
        }
    }

    JFreeChart createChartEntity(String testcaseName, XYSeries series,
            XYSeries line, SortedMap<Integer, Double> performance) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        dataset.addSeries(line);
        NumberAxis xaxis = new NumberAxis("Revision");
        NumberAxis yaxis = new NumberAxis("Time (sec)");
        yaxis.setLowerBound(0);
        yaxis.setUpperBound(performance.get(performance.lastKey()) * 1.2);
        XYItemRenderer renderer = new XYLineAndShapeRenderer(true,
                false);
        renderer.setSeriesToolTipGenerator(0,
                new FeatureXYToolTipGenerator());
        renderer.setSeriesToolTipGenerator(1, null);
        XYPlot plot = new XYPlot(dataset, xaxis, yaxis, renderer);
        JFreeChart chart = new JFreeChart(testcaseName, plot);
        chart.removeLegend();
        xaxis.setAutoRange(true);
        xaxis.setAutoRangeIncludesZero(false);        
        String startDateString = revisionDate.get(performance.firstKey());
        String endDateString = revisionDate.get(performance.lastKey());
        Title dates = new TextTitle("Start: " + startDateString 
                + "                " + "End: " + endDateString);
        dates.setPosition(RectangleEdge.BOTTOM);
        dates.setPadding(1.0,1.0,10.0,1.0);
        chart.addSubtitle(dates);        
        return chart;
    }

    public SortedMap<Integer, Double> getTimingData(String testcase) {
        return testData.get(testcase);
    }
    
    public void putRevisionDate(Integer revision, String date) {
        revisionDate.put(revision, date);
    }

    public void addTimingInformation(String testName, Integer revision,
            Double testTime) {
        if (specialTestCase(testName)) {
            if (testData.get(testName) == null) {
                SortedMap<Integer, Double> timing = new TreeMap<Integer, Double>();
                timing.put(revision, testTime);
                testData.put(testName, timing);
            } else {
                SortedMap<Integer, Double> timing = testData.get(testName);
                timing.put(revision, testTime);
            }
        }
    }

    private boolean specialTestCase(String testCase) {
        for (String special : SPECIAL_TESTCASES) {
            if (special.equals(testCase))
                return true;
        }
        return false;
    }

}
