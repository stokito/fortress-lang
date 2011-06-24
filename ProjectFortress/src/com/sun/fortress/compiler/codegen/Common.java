/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.compiler.codegen;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.NodeAbstractVisitor_void;

/**
 * Common cases for compiling AST nodes.
 * Subclass for special cases as appropriate.
 * @author dr2chase
 */
public class Common extends NodeAbstractVisitor_void {
    
    /**
     * Builds a code generator starting in (top-level ?) environment e.
     * @param e
     */
    public Common(Environment e) {
        this.e = e;
    }
    
    Environment e;
    
    public Environment getEnv() {
        return this.e;
    }

    /*
     * For each top-level function in a component, generate two things:
     * 
     * 1) a static method that has some FValue-typed parameters
     *    (this will change in the future).
     * 2) a class, extending/implementing Closure, that has a single instance,
     *    that can be called by the interpreter.
     * 
     * The static method can be in that class (might as well, right).
     * 
     * If the function has static (type, opr, nat) parameters, it takes
     * them as parameters of the static method.  TBD, it looks like the
     * corresponding interpreter datastructure would be a GenericFunction,
     * and NOT YET.
     * 
     * Simplest case, foo():() = ().
     * 
     * We need to make all the closure boilerplate, and invoke the static
     * method, which should have type void, and the closure boilerplate should
     * obtain the () and return it (this is FVoid.V).
     * 
     * What would it take to compile
     *   println "Hello World"?
     * 
     * println is overloaded in FortressLibrary.
     * juxtaposition is overloaded in two different files.
     * 
     * Can we get type information for the parameters?
     * 
     */

}
