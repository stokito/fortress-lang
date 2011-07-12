/*******************************************************************************
 Copyright 2011, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringBufferInputStream;
import java.nio.charset.Charset;

public class fileOps {

    public static BufferedReader jbrOpen(String name) {
	try {
	    return new BufferedReader(new InputStreamReader(new FileInputStream(name), Charset.forName("UTF-8")));
	} catch (FileNotFoundException e) {
	    return new BufferedReader(new InputStreamReader(new StringBufferInputStream(""), Charset.forName("UTF-8")));
	}
    }

    public static String jbrAsString(BufferedReader r) {
	return "[buffered reader " + r.toString() + "]";
    }

    public static int jbrRead(BufferedReader r) {
	try {
	    return r.read();
	} catch (IOException e) {
	    return 0xFFFF;
	}
    }

    public static String jbrReadLine(BufferedReader r) {
	try {
	    return r.readLine();
	} catch (IOException e) {
	    return "";
	}
    }

    public static boolean jbrReady(BufferedReader r) {
	try {
	    return r.ready();
	} catch (IOException e) {
	    return false;
	}
    }

    public static void jbrClose(BufferedReader r) {
	try {
	    r.close();
	} catch (IOException e) { }
    }


    public static BufferedWriter jbwOpen(String name) {
	try {
	    return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(name), Charset.forName("UTF-8")));
	} catch (FileNotFoundException e) {
	    return new BufferedWriter(new OutputStreamWriter(new ByteArrayOutputStream(), Charset.forName("UTF-8")));
	}
    }

    public static String jbwAsString(BufferedWriter w) {
        return "[buffered writer " + w.toString() + "]";
    }

    public static void jbwWriteChar(BufferedWriter w, int c) {
        try {
	    w.write(c);
	} catch (IOException e) { }
    }

    public static void jbwWriteString(BufferedWriter w, String s) {
	try {
	    w.write(s, 0, s.length());
	} catch (IOException e) { }
    }

    public static void jbwNewLine(BufferedWriter w) {
	try {
	    w.newLine();
	} catch (IOException e) { }
    }

    public static void jbwFlush(BufferedWriter w) {
	try {
	    w.flush();
	} catch (IOException e) { }
    }

    public static void jbwClose(BufferedWriter w) {
	try {
	    w.close();
	} catch (IOException e) { }
    }

}
