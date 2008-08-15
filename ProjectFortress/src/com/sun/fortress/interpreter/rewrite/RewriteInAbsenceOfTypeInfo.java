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

package com.sun.fortress.interpreter.rewrite;

import java.util.List;

import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.ExprMI;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Juxt;
import com.sun.fortress.nodes.MathItem;
import com.sun.fortress.nodes.MathPrimary;
import com.sun.fortress.nodes.MethodInvocation;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.TupleExpr;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes._RewriteFnRef;
import com.sun.fortress.nodes_util.ExprFactory;
import static com.sun.fortress.exceptions.InterpreterBug.bug;

public class RewriteInAbsenceOfTypeInfo extends Rewrite {

    public static RewriteInAbsenceOfTypeInfo Only = new RewriteInAbsenceOfTypeInfo();

    static Expr translateQualifiedToFieldRef(VarRef vr) {
        Id idn = vr.getVar();
        if (idn.getApi().isNone())
            return vr;

        List<Id> ids = idn.getApi().unwrap().getIds();

        return ExprFactory.makeFieldRef(vr.getSpan(),
                                        translateQualifiedToFieldRef(ids), idn);
    }

    static Expr translateFnRef(FnRef fr) {
        List<Id> fns = fr.getFns();
        List<StaticArg> sargs = fr.getStaticArgs();
        Id idn = fns.get(0);
        if (sargs.size() == 0) {
            // Call it a var or field ref for now.
            if (idn.getApi().isNone()) {
                return new VarRef(idn.getSpan(), idn);

            } else {
                List<Id> ids = idn.getApi().unwrap().getIds();

                return ExprFactory.makeFieldRef(fr.getSpan(),
                                                translateQualifiedToFieldRef(ids),
                                                idn);
            }
        } else {
        if (idn.getApi().isNone()) {
            return new _RewriteFnRef(fr.getSpan(),
                    false,
                    new VarRef(idn.getSpan(), idn),
                    sargs);
        } else {
            List<Id> ids = idn.getApi().unwrap().getIds();

            return new _RewriteFnRef(fr.getSpan(),
                        false,
                        ExprFactory.makeFieldRef(fr.getSpan(),
                                                 translateQualifiedToFieldRef(ids),
                                                 idn),
                        sargs);
        }
        }
    }

    static Expr translateQualifiedToFieldRef(List<Id> ids) {
        // id is trailing (perhaps only) id
        Id id = ids.get(ids.size()-1);

        if (ids.size() == 1) {
            return new VarRef(id.getSpan(), id);

        }
        // TODO fix span -- it needs to cover the whole list.
        return ExprFactory.makeFieldRef(id.getSpan(),
                translateQualifiedToFieldRef(ids.subList(0, ids.size()-1)),
                id
                );
    }

    @Override
    public AbstractNode visit(AbstractNode node) {
        if (node instanceof VarRef)
            return translateQualifiedToFieldRef((VarRef)node);

        if (node instanceof FnRef)

            return visit(translateFnRef((FnRef)node));

        if (node instanceof TightJuxt && looksLikeMethodInvocation((Juxt) node)) {
            return visit(translateJuxtOfDotted((Juxt) node));
        }

        if (node instanceof MathPrimary && looksLikeMethodInvocation((MathPrimary) node)) {
            return visit(translateJuxtOfDotted((MathPrimary) node));
        }


        return visitNode(node);
    }

    private AbstractNode translateJuxtOfDotted(Juxt node) {
        List<Expr> exprs = node.getExprs();
        VarRef first = (VarRef) exprs.get(0);
        Id idn = first.getVar();
        if (idn.getApi().isNone())
            bug(idn, "Missing an API name...");
        List<Id> ids = idn.getApi().unwrap().getIds();

        return new MethodInvocation(node.getSpan(),
                                false,
                                translateQualifiedToFieldRef(ids),
                                idn, exprs.size() == 2 ? exprs.get(1) :
                                    new TupleExpr(exprs.subList(1, exprs.size())));
    }

    private AbstractNode translateJuxtOfDotted(MathPrimary node) {
        List<MathItem> exprs = node.getRest();
        VarRef first = (VarRef) node.getFront();
        Id idn = first.getVar();
        if (idn.getApi().isNone())
            bug(idn, "Missing an API name...");
        List<Id> ids = idn.getApi().unwrap().getIds();

        return new MethodInvocation(node.getSpan(),
                                false,
                                translateQualifiedToFieldRef(ids),
                                    idn,
                                    ((ExprMI)exprs.get(0)).getExpr()
                                    /*
                                    exprs.size() == 1 ? exprs.get(1) :
                                    new TupleExpr(exprs.subList(1, exprs.size()))
                                    */
                                    );
    }

    private boolean looksLikeMethodInvocation(Juxt node) {
        Expr first = node.getExprs().get(0);
        return (first instanceof VarRef && ! first.isParenthesized() &&
                ((VarRef)first).getVar().getApi().isSome());
    }

    private boolean looksLikeMethodInvocation(MathPrimary node) {
        Expr first = node.getFront();
        List<MathItem> exprs = node.getRest();
        return (first instanceof VarRef && ! first.isParenthesized() &&
                ((VarRef)first).getVar().getApi().isSome() &&
                exprs.size() == 1 && exprs.get(0) instanceof ExprMI);
    }

}
