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
                + (node.getThrowsClause().size() > 0 ? (" throws " +
                        Useful.listInCurlies(mapSelf(node.getThrowsClause()))) : "");
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

    public String forFnDefOrDecl(FnDefOrDecl node) {
        return NodeUtil.getName(node.getFnName())
                + (node.getStaticParams().isPresent() ?
                        Useful.listInOxfords(mapSelf(node.getStaticParams().getVal())) : "")
                + Useful.listInParens(mapSelf(node.getParams()))
                + (node.getReturnType().isPresent() ? (":" + node.getReturnType().getVal().accept(this)) : "")
                + "@" + NodeUtil.getAt(node.getFnName());
    }


    public String forFun(Fun node) {
        return node.getName().getName();
    }

    public String forId(Id node) {
        return node.getName();
    }

    public String forIdType(IdType node) {
        return node.getName().accept(this);
    }

    public String forIntLiteral(IntLiteral node) {
        return node.getVal().toString();
    }

    public String forIntParam(IntParam node) {
        return "int " + node.getId().getName();
    }

    public String forKeywordType(KeywordType node) {
        return "" + node.getName().accept(this) + ":" + node.getType().accept(this);
    }

    public String forLValueBind(LValueBind node) {
        String r = "";
        if (node.getType().isPresent()) {
            r = ":" + node.getType().getVal().accept(this);
        }
        return node.getName().accept(this) + r;
    }

    public String forName(Name node) {
        if (node.getId().isPresent()) {
            return node.getId().getVal().accept(this);
        }
        else if (node.getOp().isPresent()) {
            return node.getOp().getVal().accept(this);
        }
        else {
            throw new Error("Uninitialized Name.");
        }
    }

    public String forNatParam(NatParam node) {
        return "nat " + node.getId().getName();
    }

    public String forAbstractNode(AbstractNode node) {
        return node.getClass().getSimpleName() + "@" + node.getSpan().begin.at();
    }

    public String forOperatorParam(OperatorParam node) {
        return "opr " + node.getOp().getName();
    }

    public String forParam(Param node) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.valueOf(node.getName().accept(this)));
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

    public String forParamType(ParamType node) {
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
