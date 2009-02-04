/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
package com.sun.fortress.nodes_util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;
import java.io.NotSerializableException;
import java.io.IOException;
import java.nio.charset.Charset;

import com.sun.fortress.compiler.Parser;
import com.sun.fortress.exceptions.ParserError;
import com.sun.fortress.interpreter.reader.Lex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.repository.ProjectProperties;

import edu.rice.cs.plt.tuple.Option;

public class ASTIO {
    private final static String LOG_FILE_NONE="";
    private final static boolean useJavaSerialization =
        ProjectProperties.getBoolean("fortress.astio.javaserialization",false);
    private static String logFileName =
        ProjectProperties.get("fortress.astio.logfile",LOG_FILE_NONE);

    private static volatile BufferedWriter logFile = null;

    private static long logStart() {
        if (logFileName==LOG_FILE_NONE) return 0;
        return System.nanoTime();
    }

    private static synchronized boolean initLogFile() {
        if (logFile!=null) return false;
        try {
            logFile = Useful.utf8BufferedFileWriter(logFileName, true);
        } catch (FileNotFoundException x) {
            System.err.println("WARNING: log file "+logFileName+
                               " couldn't be opened.\nTurning logging off.");
            logFileName = LOG_FILE_NONE;
            logFile = null;
            return true;
        }
        return false;
    }

    private static void logStop(long start, String eventName, String fileName) {
        if (logFileName==LOG_FILE_NONE) return;
        long nanos = System.nanoTime() - start;
        if (logFile==null && initLogFile()) return;
        String toWrite = eventName+","+fileName+","+nanos+"\n";
        try {
            logFile.write(toWrite);
            logFile.flush();
        } catch (IOException e) {
            System.err.print("WARNING: could not log event.\n"+toWrite);
        }
    }

    /**
     * @param p
     * @param fout
     * @throws IOException
     */
    public static void writeJavaAst(CompilationUnit p, String reportedFileName, OutputStream fout)
            throws IOException {
        long t0 = logStart();
        try {
            if (useJavaSerialization) {
                ObjectOutputStream foutObj = new ObjectOutputStream(fout);
                try {
                    foutObj.writeObject(p);
                    foutObj.flush();
                } catch (NotSerializableException ns) {
                    System.out.println("WRONG:");
                    System.out.println(ns.toString());
                    System.out.flush();
                }
            } else {
                BufferedWriter utf8fout =
                    new BufferedWriter(new OutputStreamWriter(fout, Charset.forName("UTF-8")));
                (new Printer()).dump(p, utf8fout, 0);
                utf8fout.flush();
            }
        } finally {
            logStop(t0,"W",reportedFileName);
        }
    }

    public static void writeJavaAst(CompilationUnit p, String s)
            throws IOException {
        OutputStream fout = new FileOutputStream(s);
        try { writeJavaAst(p, s, fout); }
        finally { fout.close(); }
    }

    public static void deleteJavaAst(String fileName) throws IOException {
        try {
            File target = new File(fileName);
            if (!target.exists()) {
                System.err.println("File " + fileName + "not found!");
                return;
            }
            if (!target.delete())
                System.err.println("Failed to delete " + fileName);
        } catch (SecurityException e) {
            System.err.println("Unable to delete " + fileName + "("
                               + e.getMessage() + ")");
        }
    }

    /**
     * @param reportedFileName
     * @param br
     * @throws IOException
     */
    public static Option<CompilationUnit> readJavaAst(String reportedFileName,
                                                      InputStream fin)
        throws IOException
    {
        long t0 = logStart();
        if (useJavaSerialization) {
            ObjectInputStream finObj = new ObjectInputStream(fin);
            try {
                CompilationUnit p = (CompilationUnit)finObj.readObject();
                if (p==null) return Option.none();
                return Option.some(p);
            } catch (ClassNotFoundException wrongClass) {
                return Option.none();
            } finally {
                logStop(t0,"R",reportedFileName);
            }
        } else {
            BufferedReader br =
                new BufferedReader(new InputStreamReader(fin, Charset.forName("UTF-8")));
            Lex lex = new Lex(br, reportedFileName);
            try {
                Unprinter up = new Unprinter(lex);
                lex.name();
                CompilationUnit p = (CompilationUnit) up.readNode(lex.name());
                if (p == null) { return Option.none(); }
                else { return Option.some(p); }
            }
            finally {
                if (!lex.atEOF())
                    System.out.println("Parse of " + reportedFileName
                                       + " ended EARLY at line = " + lex.line()
                                       + ",  column = " + lex.column());
                logStop(t0,"R",reportedFileName);
            }
        }
    }

    public static Option<CompilationUnit> readJavaAst(String fileName)
            throws IOException {
        InputStream fin = new FileInputStream(fileName);
        try { return readJavaAst(fileName, fin); }
        finally { fin.close(); }
    }


}
