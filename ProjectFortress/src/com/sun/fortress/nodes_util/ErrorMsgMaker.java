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

package com.sun.fortress.nodes_util;

import java.io.IOException;
import java.util.*;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionVisitor;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;
import com.sun.java_cup.internal.runtime.Symbol;

public class ErrorMsgMaker extends NodeAbstractVisitor<String> {
    public static final ErrorMsgMaker ONLY = new ErrorMsgMaker();

    public static String errorMsg(Object... messages) {
        StringBuffer fullMessage = new StringBuffer();
        for (Object message : messages) {
            if (message instanceof AbstractNode) {
                fullMessage.append(makeErrorMsg((AbstractNode)message));
            }
            else {
                fullMessage.append(message.toString());
            }
        }
        return fullMessage.toString();
    }

    public static String makeErrorMsg(AbstractNode node) {
        return node.accept(ErrorMsgMaker.ONLY);
    }

    private ErrorMsgMaker() {}

    public final List<String> mapSelf(List<? extends AbstractNode> that) {
        LinkedList<String> result = new LinkedList<String>();
        for (AbstractNode elt : that) {
            result.add(elt.accept(this));
        }
        return result;
    }

    private final String acceptIfPresent(Option<? extends Node> possibleNode) {
        return possibleNode.apply(new OptionVisitor<Node, String>() {
            public String forSome(Node n) { return n.accept(ErrorMsgMaker.this); }
            public String forNone() { return ""; }
        });
    }

    public String forAbstractNode(AbstractNode node) {
        return node.getClass().getSimpleName() + "@" + node.getSpan().begin.at();
    }

    public String forAbsVarDecl(AbsVarDecl node) {
        return "abs " + Useful.listInParens(mapSelf(node.getLhs())) + node.getSpan();
    }

    public String forArrowType(ArrowType node) {
        return
            node.getDomain().accept(this)
            + "->"
            + node.getRange().accept(this)
            + (node.getThrowsClause().isSome() ?
                   (" throws " + Useful.listInCurlies(mapSelf(Option.unwrap(node.getThrowsClause())))) :
                   "")
            + (node.isIo()? " io" : "");
    }

    public String forVoidLiteralExpr(VoidLiteralExpr e) {
        return "()";
    }

    public String forVoidType(VoidType t) {
        return "()";
    }

    public String forNumberConstraint(NumberConstraint node) {
        return "" + node.getVal().toString();
    }

    public String forBoolParam(BoolParam node) {
        return "bool " + NodeUtil.nameString(node.getName());
    }

    public String forFnAbsDeclOrDecl(FnAbsDeclOrDecl node) {
        return NodeUtil.nameString(node.getName())
                + (node.getStaticParams().size() > 0 ? Useful.listInOxfords(mapSelf(node.getStaticParams())) : "")
                + Useful.listInParens(mapSelf(node.getParams()))
                + (node.getReturnType().isSome() ? (":" + Option.unwrap(node.getReturnType()).accept(this)) : "")
                ;//+ "@" + NodeUtil.getAt(node.getFnName());
    }


    public String forId(Id node) {
        return node.getText();
    }

    public String forOp(Op node) {
        return node.getText();
    }

    public String forIdType(IdType node) {
        return node.getName().accept(this);
    }

    public String forIdArg(IdArg node) {
        return NodeUtil.nameString(node.getName());
    }

    public String forIntArg(IntArg node) {
        return node.getVal().toString();
    }

    public String forBoolArg(BoolArg node) {
        return node.getBool().toString();
    }

    public String forDimArg(DimArg node) {
        return node.getDim().accept(this);
    }

    public String forUnitArg(UnitArg node) {
        return node.getUnit().accept(this);
    }

    public String forOprArg(OprArg node) {
        return node.getName().accept(this);
    }

    public String forDimRef(DimRef node) {
        return NodeUtil.nameString(node.getName());
    }

    public String forUnitRef(UnitRef node) {
        return NodeUtil.nameString(node.getName());
    }

    public String forIntRef(IntRef node) {
        return NodeUtil.nameString(node.getName());
    }

    public String forBoolRef(BoolRef node) {
        return NodeUtil.nameString(node.getName());
    }

    public String forVarRef(VarRef node) {
        return NodeUtil.nameString(node.getVar());
    }

    public String forIntLiteralExpr(IntLiteralExpr node) {
        return node.getVal().toString();
    }

    public String forIntParam(IntParam node) {
        return "int " + NodeUtil.nameString(node.getName());
    }

    public String forKeywordType(KeywordType node) {
        return "" + NodeUtil.nameString(node.getName()) + ":" + node.getType().accept(this);
    }

    public String forLValueBind(LValueBind node) {
        String r = "";
        if (node.getType().isSome()) {
            r = ":" + Option.unwrap(node.getType()).accept(this);
        }
        return NodeUtil.nameString(node.getName()) + r;
    }

    public String forNatParam(NatParam node) {
        return "nat " + NodeUtil.nameString(node.getName());
    }

    public String forName(Name n) {
        return NodeUtil.nameString(n);
    }

    public String forOperatorParam(OperatorParam node) {
        return "opr " + NodeUtil.nameString(node.getName());
    }

    public String forNormalParam(NormalParam node) {
        StringBuffer sb = new StringBuffer();
        sb.append(NodeUtil.nameString(node.getName()));
        if (node.getType().isSome()) {
            sb.append(":");
            sb.append(Option.unwrap(node.getType()).accept(this));
        }
        if (node.getDefaultExpr().isSome()) {
            sb.append("=");
            sb.append(Option.unwrap(node.getDefaultExpr()).accept(this));
        }
        return sb.toString();
    }

    public String forVarargsParam(VarargsParam node) {
        StringBuffer sb = new StringBuffer();
        sb.append(NodeUtil.nameString(node.getName()));
        sb.append(":");
        sb.append(node.getVarargsType().accept(this));

        return sb.toString();
    }

    public String forInstantiatedType(InstantiatedType node) {
        return NodeUtil.nameString(node.getName()) +
            Useful.listInOxfords(mapSelf(node.getArgs()));
    }

    public String forVarargsType(VarargsType node) {
        return node.getType().accept(this) + "...";
    }

    public String forSimpleTypeParam(SimpleTypeParam node) {
        return NodeUtil.nameString(node.getName());
    }

    public String forArgType(ArgType node) {
        return
            "(" +
            Useful.listInDelimiters("", mapSelf(node.getElements()), "") +
            acceptIfPresent(node.getVarargs()) +
            Useful.listInDelimiters("", mapSelf(node.getKeywords()), "") +
            ")";
    }

    public String forTupleType(TupleType node) {
        return
            "(" +
            Useful.listInDelimiters("", mapSelf(node.getElements()), "") +
            ")";
    }

    public String forTypeArg(TypeArg node) {
        return String.valueOf(node.getType().accept(this));
    }

    public String forVarDecl(VarDecl node) {
        return Useful.listInParens(mapSelf(node.getLhs())) + "=" + node.getInit().accept(this) + node.getSpan();
    }

    public String forBottomType(BottomType node) {
        return "BottomType";
    }

    private static final int FOUR_DIGITS = 36 * 36 * 36 * 36;

    public String forInferenceVarType(InferenceVarType node) {
        if (node.getId().getClass().equals(Object.class)) {
            int id = System.identityHashCode(node.getId()) % FOUR_DIGITS;
            return "#" + Integer.toString(id, 36);
        }
        else { return "#" + node.getId(); }
    }

    public String forOrType(OrType node) {
        return node.getFirst().accept(this) + " OR " + node.getSecond().accept(this);
    }

    public String forAndType(AndType node) {
        return node.getFirst().accept(this) + " AND " + node.getSecond().accept(this);
    }
    
    public String forItemSymbol(ItemSymbol item) {
        return item.getItem();
    }

    public String forPrefixedSymbol(PrefixedSymbol item) {
        Option oid = item.getId();
        SyntaxSymbol sym = item.getSymbol();
        if (oid.isSome()) {
            return Option.unwrap(oid).toString() + "!FIXME!" + sym.toString();
        } else {
            return sym.toString();
        }
    }

}
