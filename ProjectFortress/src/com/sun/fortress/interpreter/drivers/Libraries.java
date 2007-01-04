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

import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.interpreter.nodes.Component;
import com.sun.fortress.interpreter.rewrite.Disambiguate;
import com.sun.fortress.interpreter.useful.Useful;

/**
 * Till we get a linker, this is how we link.
 */

public class Libraries {

    static String librarySource = ProjectProperties.BASEDIR + "FortressLibrary.fss";

    static String libraryTree = ProjectProperties.BASEDIR + "FortressLibrary.jst";

    static String libraryTmp = ProjectProperties.BASEDIR + "FortressLibrary.ast";

    static private Component library = null;

    static String timestamp;

    public static void link(BuildEnvironments be, Disambiguate dis) {
        Component c = library;

        if (c == null)
            if (Useful.olderThanOrMissing(libraryTree, librarySource)) {
                try {
                    System.err
                            .println("Missing or stale preparsed AST, rebuilding from source");
                    long begin = System.currentTimeMillis();

                    c = (Component)  Driver.parseToJavaAst(librarySource,
                            Useful.utf8BufferedFileReader(librarySource));

                    System.err.println("Parsed " + librarySource + ": "
                            + (System.currentTimeMillis() - begin)
                            + " milliseconds");
                    Driver.writeJavaAst(c, libraryTree);
                } catch (Throwable ex) {
                    System.err.println("Trouble preparsing library AST.");
                    ex.printStackTrace();
                }

            } else
                try {
                    long begin = System.currentTimeMillis();
                    c = (Component) Driver.readJavaAst(libraryTree);
                    System.err.println("Read " + libraryTree + ": "
                            + (System.currentTimeMillis() - begin)
                            + " milliseconds");
                } catch (Throwable ex) {
                    System.err
                            .println("Trouble reading preparsed library AST.");
                    ex.printStackTrace();
                }

        if (c != null) {
            library = c;
            c = (Component) dis.visit(c);
            be.forComponent1(c);
            Glue.installHooks(be.getEnvironment());
            be.secondPass();
            be.forComponent2(c);
            be.resetPass();
        }

    }

}
