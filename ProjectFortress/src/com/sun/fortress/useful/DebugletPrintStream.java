/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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
package com.sun.fortress.useful;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;

public class DebugletPrintStream extends PrintStream {

    static Object serLock = new Object();
    static int nextSerial = 1;
    String thisName;
    
    public static DebugletPrintStream make(String dirname) {
        File f = new File(dirname);
        if (! f.exists())
            f.mkdirs();
        String s = nextName(dirname);
        try {
            return new DebugletPrintStream(s);
        } catch (FileNotFoundException ex) {
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
