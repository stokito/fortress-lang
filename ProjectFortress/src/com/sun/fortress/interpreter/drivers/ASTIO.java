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
package com.sun.fortress.interpreter.drivers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import xtc.parser.ParseError;
import xtc.parser.Result;
import xtc.parser.SemanticValue;

import com.sun.fortress.interpreter.reader.Lex;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes_util.Printer;
import com.sun.fortress.nodes_util.Unprinter;
import com.sun.fortress.parser.Fortress;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;

public class ASTIO {

    /**
     * @param p
     * @param fout
     * @throws IOException
     */
    public static void writeJavaAst(CompilationUnit p, BufferedWriter fout)
            throws IOException {
        (new Printer()).dump(p, fout, 0);
    }

    /**
     * Runs a command and captures its output and errors streams.
     *
     * @param command
     *            The command to run
     * @param output
     *            Output from the command is written here.
     * @param errors
     *            Errors from the command are written here.
     * @param exceptions
     *            If the execution of the command throws an exception, it is
     *            stored here.
     * @return true iff any errors were written.
     * @throws IOException
     */
    
    public static void writeJavaAst(CompilationUnit p, String s)
            throws IOException {
        BufferedWriter fout = Useful.utf8BufferedFileWriter(s);
        try { writeJavaAst(p, fout); }
        finally { fout.close(); }
    }

    /**
     * Convenience method for calling parseToJavaAst with a default BufferedReader.
     */
    public static Option<CompilationUnit> parseToJavaAst(String reportedFileName) throws IOException {
        BufferedReader r = Useful.utf8BufferedFileReader(reportedFileName);
        try { return ASTIO.parseToJavaAst(reportedFileName, r); }
        
        finally { r.close(); }
    }

    public static Option<CompilationUnit> parseToJavaAst (
            String reportedFileName, BufferedReader in)
        throws IOException
    {
        Fortress p =
            new Fortress(in,
                         reportedFileName,
                         (int) new File(reportedFileName).length());
        Result r = p.pFile(0);
    
        if (r.hasValue()) {
            SemanticValue v = (SemanticValue) r;
            CompilationUnit n = (CompilationUnit) v.value;
            
            return Option.some(n);
        }
        else {
            ParseError err = (ParseError) r;
            if (-1 == err.index) {
                System.err.println("  Parse error");
            }
            else {
                System.err.println("  " + p.location(err.index) + ": "
                        + err.msg);
            }
            return Option.none();
        }
    }

   /**
     * @param reportedFileName
     * @param br
     * @throws IOException
     */
    public static Option<CompilationUnit>
        readJavaAst(String reportedFileName, BufferedReader br)
        throws IOException
    {
        Lex lex = new Lex(br);
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
        }
    }

    public static Option<CompilationUnit> readJavaAst(String fileName)
            throws IOException {
        BufferedReader br = Useful.utf8BufferedFileReader(fileName);
        try { return readJavaAst(fileName, br); }
        finally { br.close(); }
    }

}
