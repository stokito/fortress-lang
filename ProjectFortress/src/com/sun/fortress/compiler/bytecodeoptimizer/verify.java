/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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
package com.sun.fortress.compiler.bytecodeoptimizer;
import java.io.*;

class verify {
  public static void main(String[] args) 
    {
        //        JavaClass cls = Class.forName(args[0]);
        try {
           Class cls = Class.forName(args[0]);
           System.out.println(cls);
        } catch (java.lang.ClassNotFoundException x) {
            System.out.println("RuhRohRaggy");
        }


        //        cls.Print();
        //        cls.Verify();
    }
}

	
