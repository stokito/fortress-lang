/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.parser_util.instrumentation;

/*
 * Based on com.sun.fortress.syntax_abstraction.rats.RatsUtil
 */

import com.sun.fortress.useful.Useful;
import edu.rice.cs.plt.io.IOUtil;
import xtc.parser.*;
import xtc.tree.Printer;
import xtc.type.JavaAST;

import java.io.*;

/*
 * Utility class. Should have no Fortress-specific knowledge.
 */

public class Util {

    private static final char SEP = '/'; // File.separatorChar

    public static File getTempDir() {
        try {
            return IOUtil.createAndMarkTempDirectory("instrumented", "rats");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Module getRatsModule(File file) {
        // System.err.println("Reading in " + file);
        try {
            BufferedReader in = Useful.utf8BufferedFileReader(file.getCanonicalPath());
            xtc.parser.PParser parser = new PParser(in, file.getCanonicalPath());
            Result r = parser.pModule(0);
            if (r.hasValue()) {
                SemanticValue v = (SemanticValue) r;
                return (Module) v.value;
            } else {
                ParseError err = (ParseError) r;
                throw new RuntimeException("Parse error: " + err.toString());
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeRatsModule(Module module, File tempDir) {
        FileOutputStream fo = null;
        PrettyPrinter pp = null;
        try {
            String basename = getModulePath(module.name.name);
            File file = new File(tempDir, basename + ".rats");
            makeSureDirectoryExists(file.getParentFile());
            // System.err.println("Writing out " + file);
            fo = new FileOutputStream(file);
            pp = new PrettyPrinter(new Printer(fo), new JavaAST(), true);
            pp.visit(module);
            pp.flush();
            fo.flush();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (fo != null) {
                try { fo.close(); }
                catch (IOException e) {}
            }
        }
    }

    private static void makeSureDirectoryExists(File dir) {
        if (!dir.isDirectory()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Could not create directories: " + dir.getAbsolutePath());
            }
        }
    }

    public static String getModulePath(String dottedName) {
        return dottedName.replace('.', File.separatorChar);
    }

    private static int freshid = 0;

    public static String getFreshName(String s) {
        return s + (++freshid);
    }

}
