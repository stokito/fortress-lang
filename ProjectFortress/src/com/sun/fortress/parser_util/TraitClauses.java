/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

/*
 * Fortress trait clauses: excludes, comprises, and where clauses.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;

import static com.sun.fortress.exceptions.ProgramError.error;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.NamedType;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.useful.MagicNumbers;
import edu.rice.cs.plt.tuple.Option;

import java.util.Collections;
import java.util.List;

public class TraitClauses {

    private List<BaseType> excludes = Collections.<BaseType>emptyList();
    private Option<List<NamedType>> comprises = Option.<List<NamedType>>none();
    private Option<WhereClause> where = Option.<WhereClause>none();
    private boolean hasEllipses = false;
    private boolean setExcludes = false;
    private boolean setComprises = false;
    private boolean setWhere = false;

    public TraitClauses() {
    }

    public List<BaseType> getExcludes() {
        return excludes;
    }

    public Option<List<NamedType>> getComprises() {
        return comprises;
    }

    public boolean getEllipses() {
        return hasEllipses;
    }

    public Option<WhereClause> getWhere() {
        return where;
    }

    private void multiple(TraitClause t) {
        error(t.span().begin.at() + ": Trait declarations should not have " + "multiple " + t.message() + " clauses.");
    }

    public void set(TraitClause t) {
        if (t instanceof Excludes) {
            if (setExcludes) multiple(t);
            else {
                excludes = ((Excludes) t).getExcludes();
                setExcludes = true;
            }
        } else if (t instanceof Comprises) {
            if (setComprises) multiple(t);
            else {
                comprises = ((Comprises) t).getComprises();
                hasEllipses = ((Comprises) t).hasEllipses();
                setComprises = true;
            }
        } else if (t instanceof Where) {
            if (setWhere) multiple(t);
            else {
                where = ((Where) t).getWhere();
                setWhere = true;
            }
        }
    }

    public int hashCode() {
        return MagicNumbers.hashList(excludes, MagicNumbers.m) + comprises.hashCode() * MagicNumbers.a +
               where.hashCode() * MagicNumbers.t;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o.getClass().equals(this.getClass())) {
            TraitClauses tc = (TraitClauses) o;
            return excludes.equals(tc.getExcludes()) && comprises.equals(tc.getComprises()) &&
                   where.equals(tc.getWhere());
        }
        return false;
    }

    public String toString() {
        return "";
    }
}
