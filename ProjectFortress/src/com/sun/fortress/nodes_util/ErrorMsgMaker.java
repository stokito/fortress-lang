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

package com.sun.fortress.nodes_util;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;

import java.io.IOException;
import java.util.*;

public class ErrorMsgMaker extends NodeAbstractVisitor<String> {
    public static final ErrorMsgMaker ONLY = new ErrorMsgMaker();

    public static String makeErrorMsg(AbstractNode node) {
        return node.accept(ErrorMsgMaker.ONLY);
    }

    private ErrorMsgMaker() {}

    private final List<String> mapSelf(List<? extends AbstractNode> that) {
        LinkedList<String> result = new LinkedList<String>();
        for (AbstractNode elt : that) {
            result.add(elt.accept(this));
        }
        return result;
    }

    public String forAbsVarDecl(AbsVarDecl node) {
        return "abs " + Useful.listInParens(mapSelf(node.getLhs())) + node.getSpan();
    }

    public String forArrowType(ArrowType node) {
        return Useful.listInParens(mapSelf(node.getDomain()))
                + "->"
                + node.getRange().accept(this)
                + (node.getThrowsClause().isPresent() ? (" throws " +
                        Useful.listInCurlies(mapSelf(node.getThrowsClause().getVal()))) : "");
    }

    public String forBaseNatRef(BaseNatRef node) {
        return ("" + node.getValue());
    }

    public String forBoolParam(BoolParam node) {
        return "int " + node.getId().getName();
    }

    public String forDottedId(DottedId node) {
        return Useful.dottedList(node.getNames());
    }

    public String forFnAbsDeclOrDecl(FnAbsDeclOrDecl node) {
        return NodeUtil.getName(node.getFnName())
                + (node.getStaticParams().isPresent() ?
                        Useful.listInOxfords(mapSelf(node.getStaticParams().getVal())) : "")
                + Useful.listInParens(mapSelf(node.getParams()))
                + (node.getReturnType().isPresent() ? (":" + node.getReturnType().getVal().accept(this)) : "")
                ;//+ "@" + NodeUtil.getAt(node.getFnName());
    }


    public String forId(Id node) {
        return node.getName();
    }

    public String forIdType(IdType node) {
        return node.getDottedId().accept(this);
    }

    public String forIntLiteral(IntLiteral node) {
        return node.getVal().toString();
    }

    public String forIntParam(IntParam node) {
        return "int " + node.getId().getName();
    }

    public String forKeywordType(KeywordType node) {
        return "" + node.getId().accept(this) + ":" + node.getType().accept(this);
    }

    public String forLValueBind(LValueBind node) {
        String r = "";
        if (node.getType().isPresent()) {
            r = ":" + node.getType().getVal().accept(this);
        }
        return node.getId().accept(this) + r;
    }

    public String forNatParam(NatParam node) {
        return "nat " + node.getId().getName();
    }

    public String forOpr(Opr node) {
        return node.getOp().getName();
    }

    public String forAbstractNode(AbstractNode node) {
        return node.getClass().getSimpleName() + "@" + node.getSpan().begin.at();
    }

    public String forOperatorParam(OperatorParam node) {
        return "opr " + node.getOp().getName();
    }

    public String forNormalParam(NormalParam node) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.valueOf(node.getId().accept(this)));
        if (node.getType().isPresent()) {
            sb.append(":");
            sb.append(node.getType().getVal().accept(this));
        }
        if (node.getDefaultExpr().isPresent()) {
            sb.append("=");
            sb.append(node.getDefaultExpr().getVal().accept(this));
        }
        return sb.toString();
    }

    public String forVarargsParam(VarargsParam node) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.valueOf(node.getId().accept(this)));
        sb.append(":");
        sb.append(node.getVarargsType().accept(this));

        return sb.toString();
    }

    public String forInstantiatedType(InstantiatedType node) {
        return node.getGeneric().accept(this) + Useful.listInOxfords(mapSelf(node.getArgs()));
    }

    public String forVarargsType(VarargsType node) {
        return node.getType().accept(this) + "...";
    }

    public String forSimpleTypeParam(SimpleTypeParam node) {
        return node.getId().getName();
    }

    public String forTupleType(TupleType node) {
        return Useful.listInParens(mapSelf(node.getElements()));
    }

    public String forTypeArg(TypeArg node) {
        return String.valueOf(node.getType().accept(this));
    }

    public String forVarDecl(VarDecl node) {
        return Useful.listInParens(mapSelf(node.getLhs())) + "=" + node.getInit().accept(this) + node.getSpan();
    }
}
