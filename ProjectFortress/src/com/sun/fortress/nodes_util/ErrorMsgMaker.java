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
        return node.getClass().getSimpleName() + " at " + node.getSpan().begin.at();
    }

    public String forAbsVarDecl(AbsVarDecl node) {
        return "abstract " + Useful.listInParens(mapSelf(node.getLhs())) + node.getSpan();
    }

    public String forArrowType(ArrowType node) {
        String effectString = node.getEffect().accept(this);
        if (effectString.length() > 0) { effectString = " " + effectString; }
        return
            node.getDomain().accept(this)
            + "->"
            + node.getRange().accept(this)
            + effectString;
    }
    
    public String forDomain(Domain node) {
        StringBuilder result = new StringBuilder();
        result.append("(");
        boolean first = true;
        for (Type t : node.getArgs()) {
            if (first) { first = false; }
            else { result.append(", "); }
            result.append(t.accept(this));
        }
        if (node.getVarargs().isSome()) {
            if (first) { first = false; }
            else { result.append(", "); }
            result.append(node.getVarargs().unwrap().accept(this));
            result.append("...");
        }
        for (KeywordType k : node.getKeywords()) {
            if (first) { first = false; }
            else { result.append(", "); }
            result.append(k.accept(this));
        }
        result.append(")");
        return result.toString();
    }
    
    public String forEffect(Effect node) {
        if (node.getThrowsClause().isNone()) {
            return node.isIo() ? "io" : "";
        }
        else {
            return "throws " + Useful.listInCurlies(mapSelf(node.getThrowsClause().unwrap())) +
                (node.isIo() ? " io" : "");
        }
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
                + (node.getReturnType().isSome() ? (":" + node.getReturnType().unwrap().accept(this)) : "")
                ;//+ "@" + NodeUtil.getAt(node.getFnName());
    }


    public String forId(Id node) {
        return NodeUtil.nameString(node);
    }

    public String forOp(Op node) {
        return node.getText();
    }

    public String forEnclosing(Enclosing node) {
        return node.getOpen().getText() + node.getClose().getText();
    }

    public String forVarType(VarType node) {
        return node.getName().accept(this);
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

    public String forOpArg(OpArg node) {
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
            r = ":" + node.getType().unwrap().accept(this);
        }
        return NodeUtil.nameString(node.getName()) + r;
    }

    public String forNatParam(NatParam node) {
        return "nat " + NodeUtil.nameString(node.getName());
    }

    public String forName(Name n) {
        return NodeUtil.nameString(n);
    }

    public String forOpParam(OpParam node) {
        return "opr " + NodeUtil.nameString(node.getName());
    }

    public String forNormalParam(NormalParam node) {
        StringBuffer sb = new StringBuffer();
        sb.append(NodeUtil.nameString(node.getName()));
        if (node.getType().isSome()) {
            sb.append(":");
            sb.append(node.getType().unwrap().accept(this));
        }
        if (node.getDefaultExpr().isSome()) {
            sb.append("=");
            sb.append(node.getDefaultExpr().unwrap().accept(this));
        }
        return sb.toString();
    }

    public String forVarargsParam(VarargsParam node) {
        StringBuffer sb = new StringBuffer();
        sb.append(NodeUtil.nameString(node.getName()));
        sb.append(":");
        sb.append(node.getType().accept(this));
        sb.append("...");

        return sb.toString();
    }

    public String forTraitType(TraitType node) {
        return NodeUtil.nameString(node.getName()) +
            Useful.listInOxfords(mapSelf(node.getArgs()));
    }

    public String forTypeParam(TypeParam node) {
        return NodeUtil.nameString(node.getName());
    }

    public String forVarargTupleType(VarargTupleType node) {
        return
            "(" +
            Useful.listInDelimiters("", mapSelf(node.getElements()), "") +
            node.getVarargs().accept(this) + "..." + 
            ")";
    }

    public String forTupleType(TupleType node) {
        return Useful.listInDelimiters("(", mapSelf(node.getElements()), ")");
    }

    public String forTypeArg(TypeArg node) {
        return String.valueOf(node.getType().accept(this));
    }

    public String forVarDecl(VarDecl node) {
        return Useful.listInParens(mapSelf(node.getLhs())) + "=" + node.getInit().accept(this) + node.getSpan();
    }
    
    public String forAnyType(AnyType node) {
        return "Any";
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

    public String forUnionType(UnionType node) {
        // Could use U+2228 = OR if we supported Unicode output
        return "OR" + Useful.listInCurlies(mapSelf(node.getElements()));
    }

    public String forIntersectionType(IntersectionType node) {
        // Could use U+2227 = AND if we supported Unicode output
        return "AND" + Useful.listInCurlies(mapSelf(node.getElements()));
    }

    public String forItemSymbol(ItemSymbol item) {
        return item.getItem();
    }

    public String forPrefixedSymbol(PrefixedSymbol item) {
        Option oid = item.getId();
        SyntaxSymbol sym = item.getSymbol();
        if (oid.isSome()) {
            return oid.unwrap().toString() + "!FIXME!" + sym.toString();
        } else {
            return sym.toString();
        }
    }

}
