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
import org.jfree.data.xy.XYSeries;

/**
 * Creating charts has been delegated to a thread because (1) this operation
 * is embarrassingly parallel, (2) the shared data structure 
 * {@link #testSuiteData} is accessed in a read-only fashion by all threads,
 * and (3) the runtime of {@link PerformanceLogMonitor} on the Niagara boxes
 * improves dramatically as a result of creating each chart independently.
 */
public class ChartWriter extends Thread {

    final private File chartDirectory;
    final private String testcaseName;
    final private TestSuiteData testSuiteData;
    
    ChartWriter(File chartDirectory, String testcaseName, TestSuiteData testSuiteData) {
        this.chartDirectory = chartDirectory;
        this.testcaseName = testcaseName;
        this.testSuiteData = testSuiteData;
    }
    
    @Override
    public void run() {
        XYSeries series = new XYSeries(testcaseName);
        SortedMap<Integer, Double> performance = testSuiteData.testData.get(testcaseName);
        
        for (Map.Entry<Integer, Double> entry : performance.entrySet()) {
            Integer revision = entry.getKey();
            series.add(revision, entry.getValue());
        }
        if (series.getItemCount() > 1) {
            JFreeChart chart = testSuiteData.createChartEntity(testcaseName, series, performance);
            
            ChartRenderingInfo info = new ChartRenderingInfo(
                    new StandardEntityCollection());
            OutputStream htmlOut = null;
            PrintWriter htmlWriter = null;
            try {
                File htmlPath = new File(chartDirectory, testcaseName + ".html");
                htmlOut = new BufferedOutputStream(new FileOutputStream(htmlPath));
                htmlWriter = new PrintWriter(htmlOut);
                htmlWriter.println("<html><head></head><body>");
                String filePath = chartDirectory + File.separator + testcaseName + ".png";
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
