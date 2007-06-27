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

import com.sun.fortress.interpreter.nodes_util.Span;
import java.util.List;

/* For loops are parsed into elements of this class.
 * 
 * Syntax: 
 *   DelimitedExpr ::= for GeneratorList DoFront end 
 *   DoFront ::= ( at Expr)? ( atomic )? do BlockElems? 
 * 
 * Simple example:
 * 
 * for i <- 0#9 do
 *   huh
 * end
 */
public class For extends FlowExpr {

    List<Generator> gens;

    Expr body;

    public For(Span span, List<Generator> gens, Expr body) {
        super(span);
        this.gens = gens;
        this.body = body;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forFor(this);
    }

    For(Span span) {
        super(span);
    }

    /**
     * @return Returns the body.
     */
    public Expr getBody() {
        return body;
    }

    /**
     * @return Returns the gens.
     */
    public List<Generator> getGens() {
        return gens;
    }
}
