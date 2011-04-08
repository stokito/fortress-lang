/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

public class DebugletPrintStream extends PrintStream {

    static Object serLock = new Object();
    static int nextSerial = 1;
    String thisName;

    public static DebugletPrintStream make(String dirname) throws IOException {
        File f = new File(dirname);
        if (!f.exists()) {
            if (! f.mkdirs())
                throw new IOException();
        }
        String s = nextName(dirname);
        try {
            return new DebugletPrintStream(s);
        }
        catch (FileNotFoundException ex) {
            throw new Error("Debuglet file " + s + " could not be opened");
        }
    }

    private DebugletPrintStream(String s) throws FileNotFoundException {
        super(s);
        thisName = s;
    }

    private static String nextName(String dirname) {
        synchronized (serLock) {
            return dirname + "/" + (nextSerial++);
        }
    }

    public String toString() {
        return thisName;
    }

    public PrintStream backtrace() {
        (new Throwable()).printStackTrace(this);
        return this;
    }

}
