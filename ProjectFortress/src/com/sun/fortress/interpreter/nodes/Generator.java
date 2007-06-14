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

package com.sun.fortress.interpreter.nodes;

import java.util.List;

/* Generator bindings (as occur in 'for' loop expressions, for example) 
 * are parsed to instances of this class. 
 * 
 * Generator ::= Id ? Expr 
 *             | ( Id , IdList ) ? Expr 
 *             | Expr 
 * IdList ::= Id( , Id)? 
 *
 * Simple example:
 * 
 * i <- 0#10
 */
public class Generator extends Node {

    List<Id> bind;

    Expr init;

    public Generator(Span span, List<Id> bind, Expr init) {
        super(span);
        this.bind = bind;
        this.init = init;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forGenerator(this);
    }

    Generator(Span span) {
        super(span);
    }

    /**
     * @return Returns the bind.
     */
    public List<Id> getBind() {
        return bind;
    }

    /**
     * @return Returns the init.
     */
    public Expr getInit() {
        return init;
    }
}
