/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.environments;

import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes_util.NodeFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/* maps variables to their types and stuff */
public class GapEnv {
    private final NTEnv ntEnv;
    private final Map<Id, Depth> varToDepth;
    private final Map<Id, Id> varToNT;
    private final Set<Id> stringVars;

    protected GapEnv(NTEnv ntEnv, Map<Id, Depth> varToDepth, Map<Id, Id> varToNT, Set<Id> stringVars) {
        this.ntEnv = ntEnv;
        this.varToDepth = varToDepth;
        this.varToNT = varToNT;
        this.stringVars = stringVars;
    }

    public NTEnv getNTEnv() {
        return ntEnv;
    }

    public boolean isGap(Id var) {
        return varToDepth.containsKey(var);
    }

    public Collection<Id> gaps() {
        return varToDepth.keySet();
    }

    public Depth getDepth(Id var) {
        return varToDepth.get(var);
    }

    /* Nonterminals */
    public boolean hasNonterminal(Id var) {
        return varToNT.containsKey(var);
    }

    public Id getNonterminal(Id var) {
        Id nt = varToNT.get(var);
        if (nt == null) {
            throw new MacroError(var, "Not bound to a nonterminal: " + var);
        } else {
            return nt;
        }
    }

    /* Types */
    /**
     * id must be bound to a nonterminal
     * the type does not take depth into account
     * (that is, e:Expr* => e has type Expr, not List[\Expr\]
     */
    public BaseType getAstType(Id var) {
        Id nt = varToNT.get(var);
        if (nt != null) {
            return ntEnv.getType(nt);
        } else if (hasJavaStringType(var)) {
            return NodeFactory.makeTraitType(NodeFactory.makeId(NodeFactory.macroSpan, "StringLiteralExpr"));
        } else {
            throw new MacroError(var, "Not a gap name bound to a nonterminal: " + var);
        }
    }

    public String getJavaType(Id var) {
        Id nt = varToNT.get(var);
        if (nt != null) {
            return ntEnv.getJavaType(nt);
        } else if (hasJavaStringType(var)) {
            return "String";
        } else {
            throw new MacroError(var, "Not a gap name: " + var);
        }
    }

    public boolean hasJavaStringType(Id id) {
        return stringVars.contains(id);
    }

    public String toString() {
        return varToNT.toString() + "::" + varToDepth.toString();
    }
}
