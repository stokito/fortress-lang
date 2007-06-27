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
import java.util.Collections;
import java.util.List;

// / and contract = contract_rec node
// / and contract_rec =
// / {
// / contract_requires : expr list;
// / contract_ensures : ensures_clause list;
// / contract_invariant : expr list;
// / }
// /
public class Contract extends AbstractNode {
    List<Expr> requires;

    List<EnsuresClause> ensures;

    List<Expr> invariants;

    public Contract(Span span, List<Expr> requires,
            List<EnsuresClause> ensures, List<Expr> invariants) {
        super(span);
        this.requires = requires;
        this.ensures = ensures;
        this.invariants = invariants;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forContract(this);
    }

    Contract(Span span) {
        super(span);
    }

    /**
     * @return Returns the ensures.
     */
    public List<EnsuresClause> getEnsures() {
        return ensures;
    }

    /**
     * @return Returns the invariants.
     */
    public List<Expr> getInvariants() {
        return invariants;
    }

    /**
     * @return Returns the requires.
     */
    public List<Expr> getRequires() {
        return requires;
    }

    public static Contract make(Span span, List<Expr> requires,
            List<EnsuresClause> ensures, List<Expr> invariants) {
        return new Contract(span, requires, ensures, invariants);
    }

    public static Contract make() {
        return new Contract(new Span(), Collections.<Expr> emptyList(),
                Collections.<EnsuresClause> emptyList(), Collections
                        .<Expr> emptyList());
    }
}
