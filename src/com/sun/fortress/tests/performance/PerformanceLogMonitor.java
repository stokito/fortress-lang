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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PerformanceLogMonitor {

    public static TestSuiteData testingData;
        
    private static void readDataFile(String dataFile) {
        FileInputStream inputStream = null;
        GZIPInputStream gzipStream = null;
        ObjectInputStream objectStream = null;        
        try {
            inputStream = new FileInputStream(dataFile);
            gzipStream = new GZIPInputStream(inputStream);
            objectStream = new ObjectInputStream(gzipStream);
            testingData = (TestSuiteData) objectStream.readObject();
        } catch (FileNotFoundException e) {
            System.err.println("Could not find file: " + dataFile);
            testingData = new TestSuiteData();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());                
        } catch (ClassCastException e) {
            System.err.println(e.getMessage());
        } finally {
            if (testingData == null) {
                testingData = new TestSuiteData();
            }
            closeStream(objectStream);
            closeStream(gzipStream);
            closeStream(inputStream);
        }
    }    
    
    private static Integer readLogFile(String xmlLogFile) {
        FileInputStream logInputStream = null;        
        try {
            DOMParser parser = new DOMParser();
            logInputStream = new FileInputStream(xmlLogFile);
            InputSource inputSource = new InputSource(logInputStream);
            parser.parse(inputSource);
            Document doc = parser.getDocument();
            Integer revision = getRevisionNumber(doc);
            addCurrentRevision(doc, revision);
            return revision;
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (SAXException e) {
            System.err.println(e.getMessage());
        } finally {
            closeStream(logInputStream);
        }        
        return 0;        
    }

    private static void writeDataFile(String dataFile) {
        FileOutputStream dataStream = null;
        GZIPOutputStream gzipStream = null;
        ObjectOutputStream objectStream = null;
        try {
            dataStream = new FileOutputStream(dataFile);
            gzipStream = new GZIPOutputStream(dataStream);
            objectStream = new ObjectOutputStream(gzipStream);        
            objectStream.writeObject(testingData);                    
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            closeStream(objectStream);
            closeStream(gzipStream);
            closeStream(dataStream);
        }
    }

    /**
     * Parse all test cases for this revision number.  Calls 
     * parseTestcase(Node testcase, Integer revision) on each test case
     * across all test suites.
     */
    private static void addCurrentRevision(Document doc, Integer revision) {
        NodeList cases = doc.getElementsByTagName("testcase");
        for(int i = 0; i < cases.getLength(); i++) {
                Node testcase = cases.item(i);
                parseTestcase(testcase, revision);
        }
    }

    /**
     * Parse each test case, and add the data to the appropriate data structures
     * in the TestSuiteData object.  Before the data can be added, first check
     * that this test case did not throw an error.
     */
    private static void parseTestcase(Node testcase, Integer revision) {
        boolean testPassed = true;
        NamedNodeMap attributes = testcase.getAttributes();        
        String testName = attributes.getNamedItem("name").getNodeValue();
        testName = testName.substring(testName.lastIndexOf(File.separatorChar) + 1);
        NodeList children = testcase.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals("error")) {
                testPassed = false;
            }
        }
        if (testPassed) {
            Double testTime = Double.parseDouble(attributes.getNamedItem("time").getNodeValue());
            testingData.addTimingInformation(testName, revision, testTime);
        }
    }

    /**
     * Look up the revision number in this log file.
     */
    private static Integer getRevisionNumber(Document doc) {
        NodeList revisions = doc.getElementsByTagName("revision");
        Integer revisionInt = 0;
        if (revisions.getLength() > 0) {
            Node revision = revisions.item(revisions.getLength() - 1);
            String revisionString = revision.getFirstChild().getNodeValue();
            revisionInt = Integer.parseInt(revisionString);
            Node parent = revision.getParentNode();
            Node child = parent.getFirstChild();
            while (child != null) {
                if (child.getNodeName().equals("date")) {
                    String dateString = child.getFirstChild().getNodeValue();
                    testingData.putRevisionDate(revisionInt, dateString);
                    child = null;
                } else {
                    child = child.getNextSibling();
                }
            }
        }
        return revisionInt;
    }
    
    /**
     * Close an input or output stream.
     */
    public static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();                
            }                
        }
    }
    
    public static void main(String[] args) {
	System.setProperty("java.awt.headless","true"); 
        if (!((args.length == 3) || (args.length == 2))) {
            System.err.println("First argument is the xml log file.");
            System.err.println("Second argument is the data file to read and write.");
            System.err.println("(optional) Third argument is the directory name to put all the charts.");
            return;
        }
        String xmlLogFile = args[0];
        String dataFile = args[1];
        String chartDirectory = null;
        if (args.length == 3) {
            chartDirectory = args[2];
        }
        readDataFile(dataFile);
        readLogFile(xmlLogFile);
        if (chartDirectory != null) {
            testingData.writeCharts(chartDirectory);
            testingData.writeHtml(chartDirectory);
        }
        writeDataFile(dataFile);
    }

}
