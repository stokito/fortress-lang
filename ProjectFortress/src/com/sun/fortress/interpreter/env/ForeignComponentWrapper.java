/*******************************************************************************
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
 ******************************************************************************/

package com.sun.fortress.interpreter.env;

import java.util.HashMap;
import java.util.Map;

import com.sun.fortress.interpreter.rewrite.InterpreterNameRewriter;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.StringHashComparer;

public class ForeignComponentWrapper extends NonApiWrapper {

   Map<String, InterpreterNameRewriter> rewrites =
        new BATree<String, InterpreterNameRewriter>(StringHashComparer.V);
    
   public ForeignComponentWrapper(
            APIWrapper apicw, HashMap<String, NonApiWrapper> linker, String[] implicitLibs) {
        super(apicw, linker, implicitLibs);
        // For each name in the API, need to add something to getRewrites
   }

    protected  Map<String, InterpreterNameRewriter> getRewrites() {
        return rewrites;
    }
}
