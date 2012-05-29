/*******************************************************************************
 Copyright 2011, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

import com.sun.fortress.compiler.runtimeValues.FException;
import com.sun.fortress.compiler.runtimeValues.FortressBufferedReader;
import com.sun.fortress.compiler.runtimeValues.FortressBufferedWriter;
import com.sun.fortress.compiler.runtimeValues.Utility;

import java.lang.reflect.Constructor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.nio.charset.Charset;

public class fileOps {

    public static FortressBufferedReader jbrOpen(String name) throws FException {
	try {
	    return new FortressBufferedReader(name, new BufferedReader(new InputStreamReader(new FileInputStream(name), Charset.forName("UTF-8"))));
	} catch (FileNotFoundException e) {
	    throw Utility.makeFortressException("fortress.CompilerBuiltin$FileNotFoundException", e);
	}
    }

    public static String jbrAsString(FortressBufferedReader r) {
	return "[buffered reader " + r.toString() + "]";
    }

    public static int jbrRead(FortressBufferedReader r) throws FException {
	return r.read();
    }

    public static String jbrReadLine(FortressBufferedReader r) throws FException {
	return r.readLine();
    }

    public static String jbrReadk(FortressBufferedReader r, int k) throws FException {
	return r.readk(k);
    }

    public static boolean jbrEof(FortressBufferedReader r) {
	return r.eof();
    }

    public static boolean jbrReady(FortressBufferedReader r) throws FException {
	return r.ready();
    }

    public static void jbrClose(FortressBufferedReader r) throws FException {
	r.close();
    }

    public static void jbrWhenUnconsumed(FortressBufferedReader r) throws FException {
	r.whenUnconsumed();
    }

    public static void jbrConsume(FortressBufferedReader r) throws FException {
	r.consume();
    }


    public static FortressBufferedWriter jbwOpen(String name) throws FException {
	try {
	    return new FortressBufferedWriter(name, new BufferedWriter(new OutputStreamWriter(new FileOutputStream(name), Charset.forName("UTF-8"))));
	} catch (FileNotFoundException e) {
	    throw Utility.makeFortressException("fortress.CompilerBuiltin$FileNotFoundException", e);
	}
    }

    public static String jbwAsString(FortressBufferedWriter w) {
        return "[buffered writer " + w.toString() + "]";
    }

    public static void jbwWriteChar(FortressBufferedWriter w, int c) throws FException {
	w.write(c);
    }

    public static void jbwWriteString(FortressBufferedWriter w, String s) throws FException {
	w.write(s);
    }

    public static void jbwNewLine(FortressBufferedWriter w) throws FException {
	w.newLine();
    }

    public static void jbwFlush(FortressBufferedWriter w) throws FException {
	w.flush();
    }

    public static void jbwClose(FortressBufferedWriter w) throws FException {
	w.close();
    }

}
