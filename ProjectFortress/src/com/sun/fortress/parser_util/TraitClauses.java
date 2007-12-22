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
package com.sun.fortress.parser_util;

import java.util.List;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.useful.MagicNumbers;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

public class TraitClauses {

    private List<TraitType>         excludes  = FortressUtil.emptyTraitTypes();
    private Option<List<TraitType>> comprises = Option.<List<TraitType>>none();
    private WhereClause             where = FortressUtil.emptyWhereClause();
    private boolean setExcludes  = false;
    private boolean setComprises = false;
    private boolean setWhere     = false;

    public TraitClauses() {}

    public List<TraitType> getExcludes() {
        return excludes;
    }

    public Option<List<TraitType>> getComprises() {
        return comprises;
    }

    public WhereClause getWhere() {
        return where;
    }

    private void multiple(TraitClause t) {
        error(t.span().begin.at() + ": Trait declarations should not have " +
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
