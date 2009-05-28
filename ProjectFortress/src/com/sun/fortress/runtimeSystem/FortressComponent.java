/********************************************************************************
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
********************************************************************************/

package com.sun.fortress.runtimeSystem;
import com.sun.fortress.compiler.runtimeValues.FVoid;

// Superclass of the generated component class.  We can't refer to that one until
// we have defined it and we need to pass an instance of it to the primordial task.
// We need a run method here because the one in the generated class isn't visisble yet.

public class FortressComponent {
  
    public FVoid run() {
        System.out.println("RuhRohRaggy");
        return FVoid.make();
    }

}

