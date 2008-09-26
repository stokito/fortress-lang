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

import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.Shell;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.syntax_abstractions.rats.RatsUtil;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.useful.WireTappedPrintStream;

public class ObjectExpressionVisitorJUTest extends TestCase {

    private static final char SEP = File.separatorChar;
    private static final String testsDir =
        ProjectProperties.FORTRESS_AUTOHOME + "/ProjectFortress/tests";
    private static final String tester = testsDir + SEP + "testerTest.fss";

    public void setUp() throws Exception {
        try {
            // evaluate a tester so that we ensure FortressLibrary is compiled
            // this is a hack which should be removed once the
            // FortressLibrary pass type checker. 
            Shell.eval(tester);
        } catch(Throwable e) {
            throw new InterpreterBug("Fail to evaluate " + 
                tester + " in ObjectExpressionVisitorJUTest");
        }
        Shell.setTypeChecking(true);
        Shell.setObjExprDesugaring(true);
    }

    public void tearDown() throws Exception {
        Shell.setTypeChecking(false);
        Shell.setObjExprDesugaring(false);
    }

    public void testObjectCC()
        throws FileNotFoundException, IOException, Throwable {
        runFile("objectCC_immutable.fss");
    }

    public void testObjectCC_Mutables()
        throws FileNotFoundException, IOException, Throwable {
        runFile("objectCC_mutVar1.fss");
        runFile("objectCC_mutVar2.fss");
        runFile("objectCC_mutable.fss");
    }

    public void testObjectCC_Mutli_ObjExpr_Mutables()
        throws FileNotFoundException, IOException, Throwable {
        runFile("objectCC_multi_objExpr_mutVar1.fss");
        runFile("objectCC_multi_objExpr_mutVar2.fss");
    }

    public void testObjectCC_StaticParams() 
        throws FileNotFoundException, IOException, Throwable {
        runFile("objectCC_staticParams.fss");
    } 

    private void runFile(String fileName)
        throws FileNotFoundException, IOException, Throwable {
        String file = testsDir + SEP + fileName;

        // do not print stuff to stdout for JUTests
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        WireTappedPrintStream wt_err =
            WireTappedPrintStream.make(System.err, true);
        WireTappedPrintStream wt_out =
            WireTappedPrintStream.make(System.out, true);
        System.setErr(wt_err);
        System.setOut(wt_out);

        String name = file.substring( 0, file.lastIndexOf(".") );
        String tfs = name + ".tfs";

        String[] command = new String[]{ "desugar", "-out", tfs, file};
        // System.out.println("Command: " + 
        //                    "fortress desugar -out " + tfs + " " + file);
        Shell.main( command );

        String generated = RatsUtil.getTempDir() + fileName;
        command = new String[]{ "unparse", "-unqualified", "-unmangle", "-out", generated, tfs};
        // System.out.println("Command: fortress unparse " + 
        //     "-unqualified -unmangle -out " + generated + " " + tfs);
        Shell.main( command );
        ASTIO.deleteJavaAst( tfs );

        System.setErr(oldErr);
        System.setOut(oldOut);
    }

}


