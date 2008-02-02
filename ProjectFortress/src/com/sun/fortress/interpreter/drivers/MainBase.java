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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes_util.Printer;
import com.sun.fortress.useful.Useful;

public abstract class MainBase {
    static protected String baseName(boolean stripDir, String s, String suffix) {
        if (stripDir) {
          String sep = System.getProperty("file.separator");
          int slashI = s.lastIndexOf(sep);
          if (slashI != -1)
              s = s.substring(slashI + 1);
        }
        int sufI = s.lastIndexOf(suffix);
        if (sufI != -1)
            s = s.substring(0, sufI);
        return s;
    }

    boolean ast = false;

    boolean interpret = false;

    boolean firstFieldOnNewLine = true;

    boolean oneLineVarRef = true;

    boolean skipEmpty = true;

    boolean fileOut = false;

    String inSuffix = ".sexp";

    String outSuffix = ".tfs";

    List<String> fortressArgs = new ArrayList<String>();

    public void doit(String[] args) {

        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            if ("-ast".equals(s)) {
                ast = true;
            } else if (s.startsWith("-interp")) {
                interpret = true;
            } else if ("-sr".equals(s)) {
                firstFieldOnNewLine = true;
                oneLineVarRef = true;
            } else if ("-all".equals(s)) {
                skipEmpty = false;
            } else if (s.startsWith("-fileout")) {
                fileOut = true;
                if (s.startsWith("-fileout=")) {
                    int eqi = s.indexOf("=");
                    s = s.substring(eqi + 1);
                    if (s.startsWith(".")) {
                        outSuffix = s;
                    } else {
                        outSuffix = "." + s;
                    }
                }
                ast = true;
                if (inSuffix.equals(outSuffix))
                    outSuffix += "2";

            } else
                try {
                    subDoit(s);

                } catch (IOException ex) {
                    System.err.println("Trouble opening, reading, or parsing "
                            + s);
                    ex.printStackTrace();
                } catch (Error ex) {
                    System.err.println("Trouble opening, reading, or parsing "
                            + s);
                    ex.printStackTrace();
                } catch (Throwable ex) {
                    System.err.println("Trouble opening, reading, or parsing "
                            + s);
                    ex.printStackTrace();
                }
        }
    }

    abstract void subDoit(String s) throws Throwable;

    void finish(String s, CompilationUnit p) throws Throwable {
        if (ast) {
            Appendable out = System.err;
            BufferedWriter fout = null;
            if (fileOut) {

                fout = Useful.utf8BufferedFileWriter(baseName(false, s, inSuffix)
                        + outSuffix);
                out = fout;
            }
            try {
                new Printer(firstFieldOnNewLine, oneLineVarRef, skipEmpty).dump(p, out, 0);
            }
            finally {
                if (fout != null) { fout.close(); }
            }
        }
        if (interpret) {
            FortressRepository fr = Driver.defaultRepository();;
            Driver.runProgram(fr, p, false, fortressArgs);
        }
    }
    
    void finish(String s, Option<CompilationUnit> p) throws Throwable {
        if (ast) {
            Appendable out = System.err;
            BufferedWriter fout = null;
            if (fileOut) {

                fout = Useful.utf8BufferedFileWriter(baseName(false, s, inSuffix)
                        + outSuffix);
                out = fout;
            }
            try {
                new Printer(firstFieldOnNewLine, oneLineVarRef, skipEmpty).dump(p, out, 0);
            }
            finally {
                if (fout != null) { fout.close(); }
            }
        }
        if (interpret) {
            FortressRepository fr = Driver.defaultRepository();
            Driver.runProgram(fr, Option.unwrap(p), false, fortressArgs);
        }
    }

}
