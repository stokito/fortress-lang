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

package com.sun.fortress.interpreter.evaluator;
import com.sun.fortress.interpreter.Driver;
import com.sun.fortress.interpreter.evaluator.types.BottomType;
import com.sun.fortress.interpreter.evaluator.types.FTypeArrow;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeOpr;
import com.sun.fortress.interpreter.evaluator.types.FTypeOverloadedArrow;
import com.sun.fortress.interpreter.evaluator.types.FTypeRange;
import com.sun.fortress.interpreter.evaluator.types.FTypeRest;
import com.sun.fortress.interpreter.evaluator.types.FTypeTop;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.interpreter.evaluator.types.FTypeVoid;
import com.sun.fortress.interpreter.evaluator.types.IntNat;
import com.sun.fortress.interpreter.evaluator.values.GenericFunctionOrMethod;
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.Useful;


/*
 * Created on Oct 27, 2006
 *
 */

public class Init {

    // For leak detection.
    static int countdown=16;
    
    public static void initializeEverything() {
        FTypeArrow.reset();
        FTypeTuple.reset();
        FTypeOpr.reset();
        FTypeRest.reset();
        FTypeOverloadedArrow.reset();
        FTypeGeneric.reset();
        IntNat.reset();
        GenericFunctionOrMethod.FunctionsAndState.reset();

        FTypeVoid.ONLY.resetState();
        FTypeTop.ONLY.resetState();

        BottomType.ONLY.resetState();
        FTypeRange.ONLY.resetState();

        NativeApp.reset();
        Driver.reset();
    }

    public static void allowForLeakChecks() {
        if (ProjectProperties.leakCheck) {
            // Clean the heap up nicely before a heap snapshot
            // (is the GC really necessary?)
            // note that if we snapshotting the heap,the Nth call to
            // initializeEverything will do its own GC and sleep, to catch
            // leaks in a seres of running tests.
            initializeEverything();
            for (int i = 0; i < 10; i++)
                System.gc();
            /* Hang for jmap probe */
            try {
                String pid = Useful.getPid();
                if (pid == null)
                    pid = "<this process id>";
                System.err.println(
                        "Now is a good time to attach to this process with \"jmap -heap:format=b " + pid + "\"\n" +
                        "After that, you can run \"hat heap.bin\" and browse the heap at http://localhost:7000 \n" +
                        "Now sleeping for a very long time...");
                 Thread.sleep(1000000000);
             } catch (InterruptedException ex) {
    
             }
         }
    }

}
