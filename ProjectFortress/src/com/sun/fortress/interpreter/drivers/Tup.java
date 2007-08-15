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

package com.sun.fortress.interpreter.drivers;

import java.io.BufferedReader;

import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes_util.Unprinter;
import com.sun.fortress.interpreter.reader.Lex;
import com.sun.fortress.useful.Useful;

public class Tup extends MainBase {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java Tup [-ast] [-fileout=suffix] jastFile1 jastFile2 ... ");
            System.err.println("Parses each ast file in order, reporting problems/exceptions along the way");
            System.err.println("-fileout directs unparsed output to basename.suffix");
        }
        Tup t = new Tup();
        t.inSuffix = ".tfs";
        t.doit(args);
    }

    public void subDoit(String s) throws Throwable {
        BufferedReader br = Useful.utf8BufferedFileReader(s);
        Lex lex = new Lex(br);

        try {
            System.err.println("Reading " + s);
            Unprinter up = new Unprinter(lex);
//            Option<com.sun.fortress.interpreter.nodes.CompilationUnit> p = up.readOption();
            lex.name();
            CompilationUnit p = (CompilationUnit) up.readNode(lex.name());
            finish(s, p);
          } finally {
            if (! lex.atEOF() )
                System.out.println("Parse of " + s + " ended EARLY at line = " + lex.line() + ",  column = " + lex.column());
          }

    }
}
