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
package com.sun.fortress.compiler.codegen;

import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;

/**
 * Returns Boolean.TRUE if the AST visited can be compiled.
 * "Can be compiled" depends on what we've implemented.
 * 
 * @author dr2chase
 */
public class CanCompile extends NodeAbstractVisitor<Boolean> {

    public CanCompile() {
        // TODO Auto-generated constructor stub
    }

    public Boolean defaultCase(Node that) {
        return Boolean.FALSE;
    }
    

}
