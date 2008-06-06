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

import java.util.List;

import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;

import edu.rice.cs.plt.tuple.Option;

/** Comprises {@link Function} and {@link Method}. */
public abstract class Functional {

	/**
	 * Given the static arguments which must correspond with the static
	 * parameters returned by the method {@link Functional#staticParameters()},
	 * returns the type of this method as an arrow type. Note that some {@link Functionals},
	 * for instance methods, do not really have arrow type since they are not first class.
	 * Still we can express their types as such. 
	 */
    public abstract ArrowType instantiatedType(List<StaticArg> args);
    
    /**
     * Returns the type of this method as an arrow type, without any instantiated
     * type parameters. Note that some {@link Functionals}, for instance methods,
     * do not really have arrow type since they are not first class. Still we can
     * express their types as such.
     */
    public abstract ArrowType asArrowType();
    
    /**
     * Returns an instantiated version of this. We needed this because instantiatedType
     * does not deal with varargs and keywordargs. The contract of this method requires
     * that all implementing subtypes must return their own type, rather than a supertype.
     */
    public abstract Functional instantiate(List<StaticArg> args);
    
    public abstract Type getReturnType();
    
    public abstract List<StaticParam> staticParameters();
    
    public abstract List<Param> parameters();
    
    public abstract Iterable<BaseType> thrownTypes();
    
    public abstract Option<Expr> body();
}
