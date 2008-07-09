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
package com.sun.fortress.nodes_util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import xtc.parser.ParseError;
import xtc.parser.Result;
import xtc.parser.SemanticValue;

import com.sun.fortress.compiler.Parser;
import com.sun.fortress.exceptions.ParserError;
import com.sun.fortress.interpreter.reader.Lex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.useful.StringEncodedAggregate;
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

    public static void writeJavaAst(CompilationUnit p, String s)
            throws IOException {
        BufferedWriter fout = Useful.utf8BufferedFileWriter(s);
        try { writeJavaAst(p, fout); }
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
     * Convenience method for calling parseToJavaAst with a default BufferedReader.
     */
    public static Option<CompilationUnit> parseToJavaAst(APIName api_name,
                                                         String reportedFileName)
        throws IOException {
        BufferedReader r = Useful.utf8BufferedFileReader(reportedFileName);
        try { return parseToJavaAst(api_name, reportedFileName, r); }
        finally { r.close(); }
    }

    public static Option<CompilationUnit> parseToJavaAst (APIName api_name,
                                                          String reportedFileName,
                                                          BufferedReader in)
        throws IOException {
        try {
            CompilationUnit cu = Parser.parseFile(api_name, in, reportedFileName);
            return Option.<CompilationUnit>some(cu);
        } catch (ParserError pe) {
            System.err.println("  " + pe.toString());
            return Option.<CompilationUnit>none();
        }
    }


   /**
     * @param reportedFileName
     * @param br
     * @throws IOException
     */
    public static Option<CompilationUnit> readJavaAst(String reportedFileName,
                                                      BufferedReader br)
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
