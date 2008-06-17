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

package com.sun.fortress.syntax_abstractions.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import xtc.util.Utilities;
import xtc.util.Pair;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.EnclosingFixity;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Fixity;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpExpr;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.syntax_abstractions.environments.SyntaxDeclEnv;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.tuple.Option;

/* Runtime support for ActionCreaterUtil
 */
public class ActionRuntime {
    
    public static Expr makeListAST(Pair<Expr> x) {
        return makeListAST(x.list());
    }

    /* FIXME: we shouldnt implicitly convert List<Expr> into a fortress list */
    public static Expr makeListAST(List<Expr> x) {
        // x is a rats-list of Expr
        Op op1 = new Op(Option.<APIName>none(),
                        "<|", 
                        Option.<Fixity>some(new EnclosingFixity()));
        Op op2 = new Op(Option.<APIName>none(),
                        "|>",
                        Option.<Fixity>some(new EnclosingFixity()));
        Enclosing enclosing = new Enclosing(Option.<APIName>none(), op1, op2);
        List<OpName> ls = new LinkedList<OpName>();
        ls.add(enclosing);
        OpRef opRef = new OpRef(false, enclosing, ls, new LinkedList<StaticArg>());
        List<Expr> tls = new LinkedList<Expr>();
        tls.addAll(x);
        OpExpr opExpr = new OpExpr(true, opRef, tls);
        return opExpr;
    }

    public static Expr makeMaybeAST(Expr x) {
        // x could be null
        Span span = new Span(); // FIXME: pass in src

        if (null != x) {
            List<Expr> justArgs = new LinkedList<Expr>();
            justArgs.add(x);
            return SyntaxAbstractionUtil.makeObjectInstantiation
                (span, 
                 SyntaxAbstractionUtil.FORTRESSAST,
                 SyntaxAbstractionUtil.JUST, 
                 justArgs, 
                 new LinkedList<StaticArg>());
        } else {
            return SyntaxAbstractionUtil.makeNoParamObjectInstantiation
                (span,
                 SyntaxAbstractionUtil.FORTRESSAST,
                 SyntaxAbstractionUtil.NOTHING,
                 new LinkedList<StaticArg>());
        }
    }

}
