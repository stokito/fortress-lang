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

package com.sun.fortress.compiler.desugarer;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;

import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.Shell;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.syntax_abstractions.rats.RatsUtil;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.useful.WireTappedPrintStream;

public class ObjectExpressionVisitorJUTest extends TestCase {
    private static final char SEP = File.separatorChar;
    private String file;

    public ObjectExpressionVisitorJUTest( String file ){
        super(file);
        this.file = file;
    }

    @Override public void runTest()
        throws FileNotFoundException, IOException, Throwable {
        String name = file.substring( 0, file.lastIndexOf(".") );
        String fileName = file.substring( file.lastIndexOf(SEP)+1 );
        String tfs = name + ".tfs";
        String[] command = new String[]{ "desugar", "-out", tfs, file};
        Shell.main( command );
        String generated = RatsUtil.getTempDir() + fileName;
        command = new String[]{ "unparse", "-noQualified", "-out", generated, tfs};
        Shell.main( command );
        ASTIO.deleteJavaAst( tfs );
        com.sun.fortress.compiler.StaticChecker.typecheck = false;

        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        WireTappedPrintStream wt_err =
            WireTappedPrintStream.make(System.err, true);
        WireTappedPrintStream wt_out =
            WireTappedPrintStream.make(System.out, true);
        System.setErr(wt_err);
        System.setOut(wt_out);
        assertEquals(Shell.eval(file), Shell.eval(generated));
        System.setErr(oldErr);
        System.setOut(oldOut);
    }

    public static Test suite()
        throws IOException, Throwable {
       TestSuite suite = new TestSuite("Tests closure conversion of object expressions." );
       String tests = ProjectProperties.FORTRESS_AUTOHOME + "/ProjectFortress/tests";
       String[] files = new String[]{
           "objectCC.fss" };
       for ( String file : files ){
           suite.addTest( new ObjectExpressionVisitorJUTest( tests + SEP + file ) );
       }
       return suite;
    }
}
