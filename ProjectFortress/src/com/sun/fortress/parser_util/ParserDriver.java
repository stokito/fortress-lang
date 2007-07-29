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

package com.sun.fortress.parser_util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

import xtc.parser.ParseError;
import xtc.parser.Result;
import xtc.parser.SemanticValue;

import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes_util.Printer;
import com.sun.fortress.parser.Fortress;

public class ParserDriver {

    public static void writeJavaAst(AbstractNode t, BufferedWriter fout)
            throws IOException {
        (new Printer()).dump(t, fout, 0);
    }

    public static void writeJavaAst(AbstractNode t, String s) throws IOException {
        BufferedWriter fout = new BufferedWriter(new FileWriter(s));
        writeJavaAst(t, fout);
        fout.close();
    }


  public static void main(String[] args) {
    if ((null == args) || (0 == args.length)) {
      System.err.println("Usage: <file-name>+");

    } else {
      for (int i=0; i<args.length; i++) {
        System.err.println("Processing " + args[i] + " ...");

        Reader     in = null;
        try {
          in          = new BufferedReader(new FileReader(args[i]));
          Fortress p  = new Fortress(in, args[i], (int)new File(args[i]).length());
          Result   r  = p.pFile(0);

          if (r.hasValue()) {
	    SemanticValue v = (SemanticValue)r;
	    AbstractNode n = (AbstractNode) v.value;
	    writeJavaAst(n, args[i] + ".out");
          } else {
            ParseError err = (ParseError)r;
            if (-1 == err.index) {
              System.err.println("  Parse error");
            } else {
              System.err.println("  " + p.location(err.index) + ": " + err.msg);
            }
          }

        } catch (Throwable x) {
          while (null != x.getCause()) {
            x = x.getCause();
          }
          x.printStackTrace();
        } finally {
          try {
            in.close();
          } catch (Throwable x) {
          }
        }
      }
    }
  }

}
