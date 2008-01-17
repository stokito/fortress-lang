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

import java.io.IOException; 

import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.interpreter.rewrite.Desugarer;
import com.sun.fortress.interpreter.rewrite.RewriteInAbsenceOfTypeInfo;

/**
 * Till we get a linker, this is how we link.
 */

public class Libraries {

    public static String libraryBasename = "FortressLibrary";

    static private Component library = null;
  
    static String timestamp;

    public static Component theLibrary(FortressRepository fr) throws IOException {
        if (library == null) {
            library = (Component) Driver.readTreeOrSourceComponent(libraryBasename, libraryBasename, fr);
        }
        return library;
    }
    
//    public static Component link(BuildEnvironments be, Desugarer dis) throws IOException {
//        Component c = library;
//        
//        if (c == null)
//            c = (Component) Driver.readTreeOrSourceComponent(libraryBasename, libraryBasename);
//
//        if (c != null) {
//            library = c;
//            c = (Component) RewriteInAbsenceOfTypeInfo.Only.visit(c);
//            c = (Component) dis.visit(c);
//            be.forComponent1(c);
//            be.secondPass();
//            /*
//             * This is ugly, but it probably needs to be exposed
//             * like this.  Once linking-proper is working, each component
//             * must be pushed through pass N in turn before N is incremented.
//             */
//            be.forComponentDefs(c);
//            be.thirdPass();
//            be.forComponentDefs(c);
//            be.fourthPass();
//            be.forComponentDefs(c);
//            be.resetPass();
//        }
//        
//        return c;
//
//    }

}
