/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nodes_util;

import java.util.*;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionVisitor;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;

public class ErrorMsgMaker extends NodeAbstractVisitor<String> {
    public static final ErrorMsgMaker ONLY = new ErrorMsgMaker();

    public static boolean printHashCodes = false;   // Some temporary debugging codes briefly bind this to true.

    private String hashCodify(Object node, String s) {
        return printHashCodes ? (s + "@" + node.hashCode()) : s;
    }

    public static String errorMsg(Object... messages) {
        StringBuilder fullMessage = new StringBuilder();
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

    public String forTraitObjectDecl(TraitObjectDecl node) {
        return node.getClass().getSimpleName() + " " + NodeUtil.getName(node) + " at " + NodeUtil.getSpan(node).begin.at();
    }

    public String forAbstractNode(AbstractNode node) {
        return node.getClass().getSimpleName() + " at " + NodeUtil.getSpan(node).begin.at();
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

    public String forTupleType(TupleType node) {
        StringBuilder result = new StringBuilder();
        result.append("(");
        boolean first = true;
        for (Type t : node.getElements()) {
            if (first) { first = false; }
            else { result.append(", "); }
            result.append(t.accept(this));
        }
        if ( NodeUtil.hasVarargs(node) ) {
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
            return node.isIoEffect() ? "io" : "";
        }
        else {
            return "throws " + Useful.listInCurlies(mapSelf(node.getThrowsClause().unwrap())) +
                (node.isIoEffect() ? " io" : "");
        }
    }

    public String forVoidLiteralExpr(VoidLiteralExpr e) {
        return "()";
    }

    public String forIntBase(IntBase node) {
        return node.getIntVal().accept(this);
    }

    public String forFnDecl(FnDecl node) {
        return NodeUtil.nameString(NodeUtil.getName(node))
                + (NodeUtil.getStaticParams(node).size() > 0 ? Useful.listInOxfords(mapSelf(NodeUtil.getStaticParams(node))) : "")
                + Useful.listInParens(mapSelf(NodeUtil.getParams(node)))
                + (NodeUtil.getReturnType(node).isSome() ? (":" + NodeUtil.getReturnType(node).unwrap().accept(this)) : "")
                ;//+ "@" + NodeUtil.getAt(NodeUtil.getFnName(node));
    }

    public String forApiName(APIName node) {
        String s = node.getText();
        return s;
    }
    
    public String forOptApiNameDot(Option<APIName> onode) {
        if (onode.isNone())
            return "";
        return forApiName(onode.unwrap()) + ".";
    }
    
    public String forOverloading(Overloading node) {
        // String oa = forOptApiNameDot(node.getOriginalName().getApiName());
        String ua = forOptApiNameDot(node.getUnambiguousName().getApiName());
        String o = forIdOrOp(node.getOriginalName());
        String u = forIdOrOp(node.getUnambiguousName());
        String a = " at " + NodeFactory.terseSpan(NodeUtil.getSpan(node));
        return "Overloading of " + o + " (" + ua+u +  ")" + a;
    }
    
    public String forIdOrOp(IdOrOp node) {
        if ( node instanceof Id )
            return forId((Id)node);
        else
            return forOp((Op)node);
    }

    public String forId(Id node) {
        return hashCodify(node, NodeUtil.nameString(node));
    }

    public String forOp(Op node) {
        return node.getText();
    }

    public String forVarType(VarType node) {
        return node.getName().accept(this);
    }

    public String forIntArg(IntArg node) {
        return node.getIntVal().toString();
    }

    public String forBoolArg(BoolArg node) {
        return node.getBoolArg().toString();
    }

    public String forDimArg(DimArg node) {
        return node.getDimArg().accept(this);
    }

    public String forUnitArg(UnitArg node) {
        return node.getUnitArg().accept(this);
    }

    public String forOpArg(OpArg node) {
        return node.getId().accept(this);
    }

    public String forDimRef(DimRef node) {
        return NodeUtil.nameString(node.getName());
    }

    public String forUnitRef(UnitRef node) {
        return NodeUtil.nameString(node.getName());
    }

    public String forUnitBinaryOp(UnitBinaryOp node) {
        return "(" + node.getLeft().accept(this) +
                     node.getOp().accept(this) +
                     node.getRight().accept(this) + ")";
    }

    public String forIntRef(IntRef node) {
        return NodeUtil.nameString(node.getName());
    }

    public String forBoolRef(BoolRef node) {
        return NodeUtil.nameString(node.getName());
    }

    public String forBoolUnaryOp(BoolUnaryOp node) {
        return node.getOp().accept(this) + "(" + node.getBoolVal().accept(this) + ")";
    }

    private String forBoolOpConstraint(BoolBinaryOp node) {
        return "(" + node.getLeft().accept(this) + node.getOp().accept(this) + node.getRight().accept(this) + ")";
    }

    public String forVarRef(VarRef node) {
        List<StaticArg> sargs = node.getStaticArgs();
        if ( NodeUtil.isSingletonObject(node) )
            return forId(node.getVarId()) +
                (sargs.size() > 0 ? Useful.listInOxfords(sargs) : "");
        else
            return NodeUtil.nameString(node.getVarId());
    }

    public String forIntBinaryOp(IntBinaryOp node) {
        return node.getLeft().accept(this) + node.getOp().accept(this) + node.getRight().accept(this);
    }

    public String forFieldRef(FieldRef node) {
        return node.getObj().accept(this) + "." + forId(node.getField());
    }

    public String forFnRef(FnRef node) {
        List<StaticArg> sargs = node.getStaticArgs();
        return forIdOrOp(node.getOriginalName()) +
               (sargs.size() > 0 ? Useful.listInOxfords(sargs) : "");
    }

    public String for_RewriteFnRef(_RewriteFnRef node) {
        List<StaticArg> sargs = node.getStaticArgs();
        return node.getFnExpr().accept(this) +
               (sargs.size() > 0 ? Useful.listInOxfords(sargs) : "");
    }

    public String forOpRef(OpRef node) {
        List<StaticArg> sargs = node.getStaticArgs();
        return forName(node.getOriginalName()) +
               (sargs.size() > 0 ? Useful.listInOxfords(sargs) : "");
    }

    public String forIntLiteralExpr(IntLiteralExpr node) {
        return node.getIntVal().toString();
    }

    public String forKeywordType(KeywordType node) {
        return "" + NodeUtil.nameString(node.getName()) + ":" + node.getKeywordType().accept(this);
    }

    public String forLValue(LValue node) {
        String r = "";
        String mods = "" + node.getMods();
        if (! mods.equals(""))
            mods += " ";
        if (node.getIdType().isSome()) {
            r = ":" + node.getIdType().unwrap().accept(this);
        }
        return mods + NodeUtil.nameString(node.getName()) + r;
    }

    public String forStaticParam(final StaticParam node) {
        final String name = NodeUtil.nameString(node.getName());
        return node.getKind().accept(new NodeAbstractVisitor<String>() {
                @Override public String forKindType(KindType k) {
                    String bound = "";
                    List<BaseType> supers = node.getExtendsClause();
                    if (! supers.isEmpty()) {
                        bound += " extends";
                        StringBuilder buf = new StringBuilder();
                        buf.append(bound);
                        for (BaseType t : supers) {
                            buf.append(" " + t);
                        }
                        bound = buf.toString();
                    }
                    return name + bound;
                }
                @Override public String forKindInt(KindInt k) { return "int " + name; }
                @Override public String forKindNat(KindNat k) { return "nat " + name; }
                @Override public String forKindBool(KindBool k) { return "bool " + name; }
                @Override public String forKindDim(KindDim k) { return "dim " + name; }
                @Override public String forKindUnit(KindUnit k) { return "unit " + name; }
                @Override public String forKindOp(KindOp k) { return "opr " + name; }
            } );
    }

    public String forName(Name n) {
        return NodeUtil.nameString(n);
    }

    public String forParam(Param node) {
        StringBuilder sb = new StringBuilder();
        sb.append(NodeUtil.nameString(node.getName()));
        if ( ! NodeUtil.isVarargsParam(node) ) {
            if (node.getIdType().isSome()) {
                sb.append(":");
                sb.append(node.getIdType().unwrap().accept(this));
            }
            if (node.getDefaultExpr().isSome()) {
                sb.append("=");
                sb.append(node.getDefaultExpr().unwrap().accept(this));
            }
        } else {
            sb.append(":");
            sb.append(node.getVarargsType().unwrap().accept(this));
            sb.append("...");
        }
        return sb.toString();
    }

    public String forTraitSelfType(TraitSelfType node) {
       StringBuilder result = new StringBuilder();
       if (! node.getComprised().isEmpty()) {
           result.append("(");
           result.append(node.getNamed().accept(this));
           result.append(" & {");
           boolean first = true;
           for (Type t : node.getComprised()) {
               if (first) { first = false; }
               else { result.append(", "); }
               result.append(t.accept(this));
           }
           result.append("})");
       } else
           result.append(node.getNamed().accept(this));
       return result.toString();
    }

    public String forObjectExprType(ObjectExprType node) {
        // Could use U+2227 = AND if we supported Unicode output
        //return "AND" + Useful.listInParens(mapSelf(node.getExtended()));
        List<BaseType> extended = node.getExtended();
        if (extended.size() == 1)
            return extended.get(0).accept(this);
        else
            return "AND" + Useful.listInParens(mapSelf(node.getExtended()));
    }

    public String forTraitType(TraitType node) {
        String constructorName = hashCodify(node.getName(), NodeUtil.shortNameString(node.getName()));
        if (node.getArgs().isEmpty()) {
            return constructorName;
        } else {
            return constructorName +
                Useful.listInOxfords(mapSelf(node.getArgs()));
        }
    }

    public String forBoolBase(BoolBase node) {
        if (node.isBoolVal()) { return "true"; }
        else { return "false"; }
    }

    public String forTypeArg(TypeArg node) {
        return String.valueOf(node.getTypeArg().accept(this));
    }

    public String forVarDecl(VarDecl node) {
        if (node.getInit().isSome())
            return Useful.listInParens(mapSelf(node.getLhs())) + "=" + node.getInit().unwrap().accept(this) + NodeUtil.getSpan(node);
        else
            return "abstract " + Useful.listInParens(mapSelf(node.getLhs())) + NodeUtil.getSpan(node);
    }

    public String forAnyType(AnyType node) {
        return "Any";
    }

    public String forBottomType(BottomType node) {
        return "BottomType";
    }

    public String forUnknownType(UnknownType node) {
        return "_";
    }

    private static final int FOUR_DIGITS = 36 * 36 * 36 * 36;

    public String for_InferenceVarType(_InferenceVarType node) {
        if (node.getId().getClass().equals(Object.class)) {
            int id = System.identityHashCode(node.getId()) % FOUR_DIGITS;
            return "#" + Integer.toString(id, 36);
        }
        else { return "#" + node.getId(); }
    }

    public String forUnionType(UnionType node) {
        // Could use U+2228 = OR if we supported Unicode output
        return "OR" + Useful.listInParens(mapSelf(node.getElements()));
    }

    public String forIntersectionType(IntersectionType node) {
        // Could use U+2227 = AND if we supported Unicode output
        return "AND" + Useful.listInParens(mapSelf(node.getElements()));
    }

    public String forItemSymbol(ItemSymbol item) {
        return item.getItem();
    }

    public String forPrefixedSymbol(PrefixedSymbol item) {
        Id id = item.getId();
        SyntaxSymbol sym = item.getSymbol();
        return id.toString() + "!FIXME!" + sym.toString();
    }
}
