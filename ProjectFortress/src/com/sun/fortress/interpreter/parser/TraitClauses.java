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

/*
 * Fortress trait clauses: excludes, comprises, and where clauses.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.interpreter.parser;
import java.util.List;

import com.sun.fortress.useful.Option;
import com.sun.fortress.useful.None;
import com.sun.fortress.nodes.TypeRef;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.useful.MagicNumbers;

public class TraitClauses {

    private List<TypeRef>         excludes  = FortressUtil.emptyTypeRefs();
    private Option<List<TypeRef>> comprises = None.<List<TypeRef>>make();
    private Option<List<WhereClause>> where = None.<List<WhereClause>>make();
    private boolean setExcludes  = false;
    private boolean setComprises = false;
    private boolean setWhere     = false;

    public TraitClauses() {}

    public List<TypeRef> getExcludes() {
        return excludes;
    }

    public Option<List<TypeRef>> getComprises() {
        return comprises;
    }

    public Option<List<WhereClause>> getWhere() {
        return where;
    }

    private void multiple(TraitClause t) {
        throw new ProgramError(t.span().begin.at() +
                               ": Trait declarations should not have " +
                               "multiple " + t.message() + " clauses.");
    }

    public void set(TraitClause t) {
        if (t instanceof Excludes) {
            if (setExcludes) multiple(t);
            else {
                excludes    = ((Excludes)t).getExcludes();
                setExcludes = true;
            }
        } else if (t instanceof Comprises) {
            if (setComprises) multiple(t);
            else {
                comprises    = ((Comprises)t).getComprises();
                setComprises = true;
            }
        } else if (t instanceof Where) {
            if (setWhere) multiple(t);
            else {
                where    = ((Where)t).getWhere();
                setWhere = true;
            }
        }
    }

    public int hashCode() {
        return MagicNumbers.hashList(excludes, MagicNumbers.m)
            + comprises.hashCode() * MagicNumbers.a
            + where.hashCode() * MagicNumbers.t;
    }

    public boolean equals(Object o) {
        if (o.getClass().equals(this.getClass())) {
            TraitClauses tc = (TraitClauses) o;
            return excludes.equals(tc.getExcludes())
                && comprises.equals(tc.getComprises())
                && where.equals(tc.getWhere());
        }
        return false;
    }

    public String toString() {
        return "";
    }
}
