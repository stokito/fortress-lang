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

import java.io.File;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.math.BigInteger;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;
import com.sun.fortress.interpreter.evaluator.InterpreterBug;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.parser_util.precedence_resolver.PrecedenceMap;
import com.sun.fortress.parser_util.FortressUtil;

public class NodeFactory {
    /** Alternatively, you can invoke the AbsFnDecl constructor without a self name */
    public static AbsFnDecl makeAbsFnDecl(Span s, List<Modifier> mods,
                                          Option<Id> optSelfName, FnName name,
                                          List<StaticParam> staticParams,
                                          List<Param> params,
                                          Option<Type> returnType,
                                          Option<List<TraitType>> throwss,
                                          List<WhereClause> where,
                                          Contract contract) {
        String selfName;
        if (optSelfName.isSome()) {
            selfName = Option.unwrap(optSelfName).getName();
        } else {
            selfName = WellKnownNames.defaultSelfName;
        }
        return new AbsFnDecl(s, mods, name, staticParams, params, returnType,
                             throwss, where, contract, selfName);
    }

    public static AliasedName makeAliasedName(Span span, Id id) {
        return new AliasedName(span, makeFnName(id.getSpan(), id),
                               Option.<FnName>none());
    }

    public static AliasedName makeAliasedName(Span span, Id id, DottedId alias) {
        return new AliasedName(span, makeFnName(id.getSpan(), id),
                               Option.<FnName>some(alias));
    }

    /** Alternatively, you can invoke the AbsFnDecl constructor without an alias */
    public static AliasedName makeAliasedName(Span span, OprName op) {
        return new AliasedName(span, op, Option.<FnName>none());
    }

    public static AliasedName makeAliasedName(Span span, OprName op,
                                              OprName alias) {
        return new AliasedName(span, op, Option.<FnName>some(alias));
    }

    public static ArrayType makeArrayType(Span span, Type element,
                                          Option<FixedDim> ind) {
        FixedDim indices = Option.unwrap(ind, new FixedDim(span,
                                                  Collections.<ExtentRange>emptyList()));
        return new ArrayType(span, element, indices);
    }

    public static ArrowType makeArrowType(Span span, Type domain,
                                          Type range,
                                          Option<List<TraitType>> throws_) {
        return new ArrowType(span, domain, range, throws_);
    }

    public static BaseNatStaticArg makeBaseNatStaticArg(Span span,
                                                        IntLiteral value) {
        return new BaseNatStaticArg(span, value.getVal().intValue());
    }

    public static BaseOprStaticArg makeBaseOprStaticArg(Span span, Op op) {
        return new BaseOprStaticArg(span, new Opr(span, op));
    }

    public static BoolConstraintExpr makeBoolConstraintExpr(BoolConstraint bc) {
        return new BoolConstraintExpr(bc.getSpan(), bc);
    }

    public static ConstructorFnName makeConstructorFnName(GenericWithParams def) {
        return new ConstructorFnName(def.getSpan(), def);
    }

  /** Alternatively, you can invoke the Contract constructor without any parameters */
    public static Contract makeContract() {
        return new Contract(new Span(), Option.<List<Expr>>none(),
                            Option.<List<EnsuresClause>>none(),
                            Option.<List<Expr>>none());
    }

    public static DimUnitDecl makeDimUnitDecl(Span span, Id dim,
                                              Option<DimExpr> derived,
                                              Option<Id> defaultId) {
        return new DimUnitDecl(span, Option.some(dim), derived, defaultId,
                               false, Collections.<Id>emptyList(),
                               Option.<Expr>none());
    }

    public static DimUnitDecl makeDimUnitDecl(Span span, Option<DimExpr> derived,
                                              String unit, List<Id> ids,
                                              Option<Expr> def) {
        boolean si_unit;
        if (unit.equals("SI_unit")) si_unit = true;
        else                        si_unit = false;
        return new DimUnitDecl(span, Option.<Id>none(), derived, Option.<Id>none(),
                               si_unit, ids, def);
    }

    public static DimUnitDecl makeDimUnitDecl(Span span, Id dim,
                                              Option<DimExpr> derived,
                                              String unit, List<Id> ids,
                                              Option<Expr> def) {
        boolean si_unit;
        if (unit.equals("SI_unit")) si_unit = true;
        else                        si_unit = false;
        return new DimUnitDecl(span, Option.some(dim), derived,
                               Option.<Id>none(), si_unit, ids, def);
    }

    public static DottedId makeDottedId(Span span, String s) {
        return new DottedId(span, Useful.list(new Id(span, s)));
    }

    public static DottedId makeDottedId(Span span, String s, String delimiter) {
        List<Id> ids = new ArrayList<Id>();
        int size = s.split(File.separator).length;
        String file = s.split(File.separator)[size-1];
        for (String n : file.split(delimiter)) {
            ids.add(new Id(span, n));
        }
        return new DottedId(span, ids);
    }

    public static DottedId makeDottedId(Id s) {
        return new DottedId(s.getSpan(), Useful.list(s));
    }

    public static DottedId makeDottedId(Span span, Id s) {
        return new DottedId(span, Useful.list(s));
    }

    public static DottedId makeDottedId(Span span, Id s, List<Id> ls) {
        return new DottedId(span, Useful.prepend(s, ls));
    }

    /** A hack to allow conversion from a FieldRef to a DottedId.  Instead of using
      * this, the parser should parse the thing as a DottedId in the first place, if
      * that is possible.  If the VarRef contains something besides Ids, the result
      * is None.
      */
    public static Option<DottedId> makeDottedId(FieldRef ref) {
        LinkedList<Id> ids = new LinkedList<Id>();
        VarRef head = null;
        do {
            ids.addFirst(ref.getId());
            Expr receiver = ref.getObj();
            if (receiver instanceof FieldRef) { ref = (FieldRef) receiver; }
            else if (receiver instanceof VarRef) { head = (VarRef) receiver; ref = null; }
            else { ref = null; }
        } while (ref != null);

        if (head != null) {
            List<Id> res = IterUtil.asList(IterUtil.compose(head.getVar().getNames(),
                                                            ids));
            return Option.some(new DottedId(FortressUtil.spanAll(res), res));
        }
        else { return Option.none(); }
    }

    /**
     * Call this only for names that have no location. (When/if this constructor
     * disappears, it will be because we have a better plan for those names, and
     * its disappearance will identify all those places that need updating).
     */
    public static DottedId makeDottedId(String string) {
        Span span = new Span();
        return makeDottedId(span, string);
    }

    /**
     * Alternatively, you can invoke the FnDef constructor without a selfName
     */
    public static FnDef makeFnDecl(Span s, List<Modifier> mods,
                                   Option<Id> optSelfName, FnName name,
                                   List<StaticParam> staticParams,
                                   List<Param> params,
                                   Option<Type> returnType,
                                   Option<List<TraitType>> throwss,
                                   List<WhereClause> where, Contract contract,
                                   Expr body) {
        String selfName;
        if (optSelfName.isSome()) {
            selfName = Option.unwrap(optSelfName).getName();
        } else {
            selfName = WellKnownNames.defaultSelfName;
        }
        return new FnDef(s, mods, name, staticParams, params, returnType,
                         throwss, where, contract, selfName, body);
    }

    /** Alternatively, you can invoke the Id constructor without a span */
    public static Id makeId(String string) {
        return new Id(new Span(), string);
    }

    public static IdType makeIdType(Span span, Id id) {
        return new IdType(span, makeDottedId(id));
    }

    public static LValueBind makeLValue(LValueBind lvb, Id id) {
        return new LValueBind(lvb.getSpan(), id, lvb.getType(), lvb.getMods(),
                              lvb.isMutable());
    }

    public static LValueBind makeLValue(LValueBind lvb, boolean mutable) {
        return new LValueBind(lvb.getSpan(), lvb.getId(), lvb.getType(),
                              lvb.getMods(), mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, List<Modifier> mods) {
        boolean mutable = lvb.isMutable();
        for (Modifier m : mods) {
            if (m instanceof ModifierVar || m instanceof ModifierSettable)
                mutable = true;
        }
        return new LValueBind(lvb.getSpan(), lvb.getId(), lvb.getType(),
                              mods, mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, List<Modifier> mods,
                                            boolean mutable) {
        return new LValueBind(lvb.getSpan(), lvb.getId(), lvb.getType(),
                              mods, mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, Type ty) {
        return new LValueBind(lvb.getSpan(), lvb.getId(),
                              Option.some(ty), lvb.getMods(),
                              lvb.isMutable());
    }

    public static LValueBind makeLValue(LValueBind lvb, Type ty,
                                        boolean mutable) {
        return new LValueBind(lvb.getSpan(), lvb.getId(),
                              Option.some(ty), lvb.getMods(), mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, Type ty,
                                        List<Modifier> mods) {
        boolean mutable = lvb.isMutable();
        for (Modifier m : mods) {
            if (m instanceof ModifierVar || m instanceof ModifierSettable)
                mutable = true;
        }
        return new LValueBind(lvb.getSpan(), lvb.getId(),
                              Option.some(ty), mods, mutable);
    }

    public static MatrixType makeMatrixType(Span span, Type element,
                                            ExtentRange dimension) {
        List<ExtentRange> dims = new ArrayList<ExtentRange>();
        dims.add(dimension);
        return new MatrixType(span, element, dims);
    }

    public static MatrixType makeMatrixType(Span span, Type element,
                                            ExtentRange dimension,
                                            List<ExtentRange> dimensions) {
        List<ExtentRange> dims = new ArrayList<ExtentRange>();
        dims.add(dimension);
        dims.addAll(dimensions);
        return new MatrixType(span, element, dims);
    }

    public static FnName makeFnName(Span span, Id id) {
        return makeDottedId(span, id);
    }

    public static FnName makeFnName(Span span, Op op) {
        return new Opr(span, op);
    }

    public static NatParam makeNatParam(String name) {
        return new NatParam(new Span(), new Id(new Span(), name));
    }

    /** Alternatively, you can invoke the ObjectDecl constructor without a span */
    public static ObjectDecl makeObjectDecl(List<Decl> defs2,
                                            List<Modifier> mods,
                                            Id name,
                                            List<StaticParam> stParams,
                                            Option<List<Param>> params,
                                            List<TraitTypeWhere> traits,
                                            Option<List<TraitType>> throws_,
                                            List<WhereClause> where,
                                            Contract contract) {
        return new ObjectDecl(new Span(), mods, name, stParams, traits, where,
                              params, throws_, contract, defs2);
    }

    public static Op makeOp(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name));
    }


    public static VarargsParam makeVarargsParam(Id name, VarargsType type) {
        return new VarargsParam(name.getSpan(), Collections.<Modifier>emptyList(), name, type);
    }

    public static VarargsParam makeVarargsParam(VarargsParam param, List<Modifier> mods) {
        return new VarargsParam(param.getSpan(), mods, param.getId(),
                         param.getVarargsType());
    }

    public static VarargsParam makeVarargsParam(Span span, List<Modifier> mods, Id name,
                                        VarargsType type) {
        return new VarargsParam(span, mods, name, type);
    }

    public static NormalParam makeParam(Span span, List<Modifier> mods, Id name,
                                  Type type) {
        return new NormalParam(span, mods, name, Option.some(type), Option.<Expr>none());
    }

    public static NormalParam makeParam(Id name, Type type) {
        return new NormalParam(name.getSpan(), Collections.<Modifier>emptyList(), name,
                         Option.some(type), Option.<Expr>none());
    }

    public static NormalParam makeParam(Id name) {
        return new NormalParam(name.getSpan(), Collections.<Modifier>emptyList(), name,
                         Option.<Type>none(), Option.<Expr>none());
    }

    public static NormalParam makeParam(NormalParam param, Expr expr) {
        return new NormalParam(param.getSpan(), param.getMods(), param.getId(),
                         param.getType(), Option.some(expr));
    }

    public static NormalParam makeParam(NormalParam param, List<Modifier> mods) {
        return new NormalParam(param.getSpan(), mods, param.getId(),
                         param.getType(), param.getDefaultExpr());
    }

    public static SimpleTypeParam makeSimpleTypeParam(String name) {
        return new SimpleTypeParam(new Span(), new Id(new Span(), name),
                                   Collections.<TraitType>emptyList(), false);
    }

    /** Alternatively, you can invoke the TupleType constructor without keywords */
    public static TupleType makeTupleType(Span span, List<Type> elements, Option<VarargsType> varargs) {
        return new TupleType(span, elements, varargs,
                             Collections.<KeywordType>emptyList());
    }


    public static TypeArg makeTypeArg(Span span, String string) {
        return new TypeArg(span, new IdType(span, makeDottedId(span, string)));
    }

    public static VarDecl makeVarDecl(Span span, Id id, Expr init) {
        return new VarDecl(span, Useful.<LValueBind>list(
                                new LValueBind(span, id,
                                               Option.<Type>none(),
                                               Collections.<Modifier>emptyList(),
                                               true)),
                           init);
    }

    public static BoolExpr makeInParentheses(BoolExpr be) {
        return be.accept(new NodeAbstractVisitor<BoolExpr>() {
            public BoolExpr forTrueConstraint(TrueConstraint b) {
                return new TrueConstraint(b.getSpan(), true);
            }
            public BoolExpr forFalseConstraint(FalseConstraint b) {
                return new FalseConstraint(b.getSpan(), true);
            }
            public BoolExpr forBoolIdConstraint(BoolIdConstraint b) {
                return new BoolIdConstraint(b.getSpan(), true, b.getDottedId());
            }
            public BoolExpr forBoolConstraintExpr(BoolConstraintExpr b) {
                return new BoolConstraintExpr(b.getSpan(), true,
                                              b.getConstraint());
            }
            public BoolExpr defaultCase(Node x) {
                throw new InterpreterBug(x,
                                         "makeInParentheses: " + x.getClass() +
                                         " is not a subtype of BoolExpr.");
            }
        });
    }

    public static DimExpr makeInParentheses(DimExpr dim) {
        return dim.accept(new NodeAbstractVisitor<DimExpr>() {
            public DimExpr forBaseDim(BaseDim t) {
                return new BaseDim(t.getSpan(), true);
            }
            public DimExpr forDimId(DimId t) {
                return new DimId(t.getSpan(), true, t.getDottedId());
            }
            public DimExpr forProductDim(ProductDim t) {
                return new ProductDim(t.getSpan(), true, t.getLeft(),
                                      t.getRight());
            }
            public DimExpr forQuotientDim(QuotientDim t) {
                return new QuotientDim(t.getSpan(), true, t.getNumerator(),
                                       t.getDenominator());
            }
            public DimExpr forChangeDim(ChangeDim t) {
                return new ChangeDim(t.getSpan(), true, t.getVal(), t.getUnit());
            }
            public DimExpr forExponentDim(ExponentDim t) {
                return new ExponentDim(t.getSpan(), true, t.getBase(),
                                       t.getPower());
            }
            public DimExpr forOpDim(OpDim t) {
                return new OpDim(t.getSpan(), true, t.getVal(), t.getOp());
            }
            public DimExpr defaultCase(Node x) {
                throw new InterpreterBug(x,
                                         "makeInParentheses: " + x.getClass() +
                                         " is not a subtype of DimExpr.");
            }
        });
    }

    public static DimUnitExpr makeInParentheses(DimUnitExpr dim) {
        return dim.accept(new NodeAbstractVisitor<DimUnitExpr>() {
            public DimUnitExpr forBaseDimUnit(BaseDimId t) {
                return new BaseDimId(t.getSpan(), true);
            }
            public DimUnitExpr forBaseUnitUnit(BaseUnitId t) {
                return new BaseUnitId(t.getSpan(), true);
            }
            public DimUnitExpr forDimUnitId(DimUnitId t) {
                return new DimUnitId(t.getSpan(), true, t.getDottedId());
            }
            public DimUnitExpr forProductDimUnit(ProductDimUnit t) {
                return new ProductDimUnit(t.getSpan(), true, t.getMultiplier(),
                                          t.getMultiplicand());
            }
            public DimUnitExpr forQuotientDimUnit(QuotientDimUnit t) {
                return new QuotientDimUnit(t.getSpan(), true, t.getNumerator(),
                                           t.getDenominator());
            }
            public DimUnitExpr forChangeDimUnit(ChangeDimUnit t) {
                return new ChangeDimUnit(t.getSpan(), true, t.getVal(),
                                         t.getUnit());
            }
            public DimUnitExpr forInversionDimUnit(InversionDimUnit t) {
                return new InversionDimUnit(t.getSpan(), true, t.getVal());
            }
            public DimUnitExpr forExponentDimUnit(ExponentDimUnit t) {
                return new ExponentDimUnit(t.getSpan(), true, t.getBase(),
                                           t.getPower());
            }
            public DimUnitExpr forOpDimUnit(OpDimUnit t) {
                return new OpDimUnit(t.getSpan(), true, t.getVal(), t.getOp());
            }
            public DimUnitExpr defaultCase(Node x) {
                throw new InterpreterBug(x,
                                         "makeInParentheses: " + x.getClass() +
                                         " is not a subtype of DimUnitExpr.");
            }
        });
    }

    public static IntExpr makeInParentheses(IntExpr ie) {
        return ie.accept(new NodeAbstractVisitor<IntExpr>() {
            public IntExpr forNumberConstraint(NumberConstraint i) {
                return new NumberConstraint(i.getSpan(), true, i.getVal());
            }
            public IntExpr forIntIdConstraint(IntIdConstraint i) {
                return new IntIdConstraint(i.getSpan(), true, i.getDottedId());
            }
            public IntExpr forSumConstraint(SumConstraint i) {
                return new SumConstraint(i.getSpan(), true, i.getLeft(),
                                         i.getRight());
            }
            public IntExpr forMinusConstraint(MinusConstraint i) {
                return new MinusConstraint(i.getSpan(), true, i.getLeft(),
                                           i.getRight());
            }
            public IntExpr forProductConstraint(ProductConstraint i) {
                return new ProductConstraint(i.getSpan(), true,
                                             i.getMultiplier(),
                                             i.getMultiplicand());
            }
            public IntExpr forExponentConstraint(ExponentConstraint i) {
                return new ExponentConstraint(i.getSpan(), true, i.getBase(),
                                              i.getPower());
            }
            public IntExpr defaultCase(Node x) {
                throw new InterpreterBug(x,
                                         "makeInParentheses: " + x.getClass() +
                                         " is not a subtype of IntExpr.");
            }
        });
    }

    public static StaticArg makeInParentheses(StaticArg ty) {
        return ty.accept(new NodeAbstractVisitor<StaticArg>() {
            public StaticArg forBaseNatStaticArg(BaseNatStaticArg t) {
                return new BaseNatStaticArg(t.getSpan(), true, t.getValue());
            }
            public StaticArg forBaseOprStaticArg(BaseOprStaticArg t) {
                return new BaseOprStaticArg(t.getSpan(), true, t.getFnName());
            }
            public StaticArg forBaseDimStaticArg(BaseDimStaticArg t) {
                return new BaseDimStaticArg(t.getSpan(), true);
            }
            public StaticArg forBaseUnitStaticArg(BaseUnitStaticArg t) {
                return new BaseUnitStaticArg(t.getSpan(), true);
            }
            public StaticArg forBaseBoolStaticArg(BaseBoolStaticArg t) {
                return new BaseBoolStaticArg(t.getSpan(), true, t.isBool());
            }
            public StaticArg forNotStaticArg(NotStaticArg t) {
                return new NotStaticArg(t.getSpan(), true, t.getVal());
            }
            public StaticArg forOrStaticArg(OrStaticArg t) {
                return new OrStaticArg(t.getSpan(), true, t.getLeft(),
                                       t.getRight());
            }
            public StaticArg forAndStaticArg(AndStaticArg t) {
                return new AndStaticArg(t.getSpan(), true, t.getLeft(),
                                        t.getRight());
            }
            public StaticArg forImpliesStaticArg(ImpliesStaticArg t) {
                return new ImpliesStaticArg(t.getSpan(), true, t.getLeft(),
                                            t.getRight());
            }
            public StaticArg forSumStaticArg(SumStaticArg t) {
                return new SumStaticArg(t.getSpan(), true, t.getLeft(),
                                        t.getRight());
            }
            public StaticArg forMinusStaticArg(MinusStaticArg t) {
                return new MinusStaticArg(t.getSpan(), true, t.getLeft(),
                                          t.getRight());
            }
            public StaticArg forProductStaticArg(ProductStaticArg t) {
                return new ProductStaticArg(t.getSpan(), true, t.getMultiplier(),
                                            t.getMultiplicand());
            }
            public StaticArg forQuotientStaticArg(QuotientStaticArg t) {
                return new QuotientStaticArg(t.getSpan(), true, t.getNumerator(),
                                             t.getDenominator());
            }
            public StaticArg forChangeStaticArg(ChangeStaticArg t) {
                return new ChangeStaticArg(t.getSpan(), true, t.getVal(),
                                           t.getUnit());
            }
            public StaticArg forExponentStaticArg(ExponentStaticArg t) {
                return new ExponentStaticArg(t.getSpan(), true, t.getBase(),
                                             t.getPower());
            }
            public StaticArg forDimensionStaticArg(DimensionStaticArg t) {
                return new DimensionStaticArg(t.getSpan(), true, t.getVal(),
                                              t.getOp());
            }
            public StaticArg forEqualsStaticArg(EqualsStaticArg t) {
                return new EqualsStaticArg(t.getSpan(), true, t.getLeft(),
                                           t.getRight());
            }
            public StaticArg forTypeArg(TypeArg t) {
                return new TypeArg(t.getSpan(), true, t.getType());
            }
            public StaticArg defaultCase(Node x) {
                throw new InterpreterBug(x,
                                         "makeInParentheses: " + x.getClass() +
                                         " is not a subtype of StaticArg.");
            }
        });
    }

    public static Type makeInParentheses(Type ty) {
        return ty.accept(new NodeAbstractVisitor<Type>() {
            public Type forArrowType(ArrowType t) {
                return new ArrowType(t.getSpan(), true, t.getDomain(),
                                     t.getRange(), t.getThrowsClause());
            }
            public Type forArrayType(ArrayType t) {
                return new ArrayType(t.getSpan(), true, t.getElement(),
                                     t.getIndices());
            }
            public Type forIdType(IdType t) {
                return new IdType(t.getSpan(), true, t.getDottedId());
            }
            public Type forMatrixType(MatrixType t) {
                return new MatrixType(t.getSpan(), true, t.getElement(),
                                      t.getDimensions());
            }
            public Type forInstantiatedType(InstantiatedType t) {
                return new InstantiatedType(t.getSpan(), true, t.getDottedId(),
                                            t.getArgs());
            }
            public Type forTupleType(TupleType t) {
                return new TupleType(t.getSpan(), true, t.getElements(),
                                     t.getVarargs(), t.getKeywords());
            }
            public Type forVoidType(VoidType t) {
                return new VoidType(t.getSpan(), true);
            }
            public Type forProductType(ProductType t) {
                return new ProductType(t.getSpan(), true, t.getMultiplier(),
                                       t.getMultiplicand());
            }
            public Type forQuotientType(QuotientType t) {
                return new QuotientType(t.getSpan(), true, t.getNumerator(),
                                        t.getDenominator());
            }
            public Type forInversionType(InversionType t) {
                return new InversionType(t.getSpan(), true, t.getVal());
            }
            public Type forChangeDimType(ChangeDimType t) {
                return new ChangeDimType(t.getSpan(), true, t.getType(),
                                         t.getUnit());
            }
            public Type forOpDimType(OpDimType t) {
                return new OpDimType(t.getSpan(), true, t.getVal(), t.getOp());
            }
            public Type defaultCase(Node x) {
                throw new InterpreterBug(x,
                                         "makeInParentheses: " + x.getClass() +
                                         " is not a subtype of Type.");
            }
        });
    }
}
