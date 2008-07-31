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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PerformanceLogMonitor {

    public static Map<Integer, TestSuiteData> logData = new HashMap<Integer, TestSuiteData>();
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Please supply an xml file to read and output file to write.");
            return;
        }
        FileInputStream inputStream = null;        
        FileOutputStream outputStream = null;
        ObjectOutputStream objectStream = null;
        try {        
            DOMParser parser = new DOMParser();
            inputStream = new FileInputStream(args[0]);
            outputStream = new FileOutputStream(args[1]);
            objectStream = new ObjectOutputStream(outputStream);
            InputSource inputSource = new InputSource(inputStream);
            parser.parse(inputSource);
            Document doc = parser.getDocument();
            Integer revision = getRevisionNumber(doc);
            logData.put(revision, new TestSuiteData());
            addCurrentRevision(doc, revision);
            objectStream.writeObject(logData);            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } finally {
            closeStream(inputStream);
            closeStream(objectStream);
            closeStream(outputStream);            
        }
    }

    private static void addCurrentRevision(Document doc, Integer revision) {
        NodeList cases = doc.getElementsByTagName("testcase");
        for(int i = 0; i < cases.getLength(); i++) {
                Node testcase = cases.item(i);
                parseTestcase(testcase, revision);
        }
    }

    private static void parseTestcase(Node testcase, Integer revision) {
        NamedNodeMap attributes = testcase.getAttributes();
        String testName = attributes.getNamedItem("name").getNodeValue();
        Double testTime = Double.parseDouble(attributes.getNamedItem("time").getNodeValue());        
        System.out.println(testName + " " + testTime);
        Node testsuite = testcase.getParentNode();
        NamedNodeMap parentAttributes = testsuite.getAttributes();
        String suiteName = parentAttributes.getNamedItem("name").getNodeValue();
        TestSuiteData testSuiteData = logData.get(revision);
        testSuiteData.add(suiteName, testName, testTime);
    }
    
    private static Integer getRevisionNumber(Document doc) {
        NodeList revisions = doc.getElementsByTagName("revision");
        String revisionString = revisions.item(0).getFirstChild().getNodeValue();
        Integer revision = Integer.parseInt(revisionString);
        return revision;
    }
    
    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();                
            }                
        }
    }

}
