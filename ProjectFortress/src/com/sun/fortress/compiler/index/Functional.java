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

package com.sun.fortress.compiler.index;

import java.util.Map;
import edu.rice.cs.plt.tuple.Option;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Id;

import com.sun.fortress.useful.NI;

/** Comprises {@link Function} and {@link Method}. */
public abstract class Functional {
    
    public Type instantiatedType(Type... staticArgs) {
        return NI.nyi();
    }
    
    public Map<Id, StaticParam> staticParameters() {
        return NI.nyi();
    }
    
    public Map<Id, Param> parameters() {
        return NI.nyi();
    }
    
    public Iterable<Type> thrownTypes() {
        return NI.nyi();
    }
    
    public Option<Expr> body() {
        return NI.nyi();
    }
    
}
