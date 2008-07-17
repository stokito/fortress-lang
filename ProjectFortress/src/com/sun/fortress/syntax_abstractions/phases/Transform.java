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

package com.sun.fortress.syntax_abstractions.phases;

import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes._SyntaxTransformation;
import com.sun.fortress.nodes.TemplateUpdateVisitor;
import com.sun.fortress.useful.Debug;

public class Transform {
    /* cannot be instantiated */
    private Transform(){
    }

    public static Node transform( Node node ){
        return node.accept( new TemplateUpdateVisitor(){
            @Override public Node defaultTransformationNodeCase(_SyntaxTransformation that) {
                Debug.debug( Debug.Type.SYNTAX, 1, "Run transformation on " + that + " is " + that.getTransformation().invoke() );
                return that.getTransformation().invoke().accept( this );
                // run this recursively??
                // return that.invoke().accept( this );
            }
        });
    }
}
