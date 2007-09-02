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
import com.sun.fortress.interpreter.evaluator.ProgramError;
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
            selfName = Option.unwrap(optSelfName).getText();
        } else {
            selfName = WellKnownNames.defaultSelfName;
        }
        return new AbsFnDecl(s, mods, name, staticParams, params, returnType,
                             throwss, where, contract, selfName);
    }

    public static AliasedName makeAliasedName(Span span, Id id) {
        return new AliasedName(span, makeIdName(id), Option.<FnName>none());
    }

    public static AliasedName makeAliasedName(Span span, Id id, Id alias) {
        return new AliasedName(span, makeIdName(id),
                               Option.<FnName>some(makeIdName(alias)));
    }

    /** Alternatively, you can invoke the AbsFnDecl constructor without an alias */
    public static AliasedName makeAliasedName(Span span, OpName op) {
        return new AliasedName(span, op, Option.<FnName>none());
    }

    public static AliasedName makeAliasedName(Span span, OpName op,
                                              OpName alias) {
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

    public static OprArg makeOprArg(Span span, Op op) {
        return new OprArg(span, new Opr(span, op));
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

    public static DimUnitDecl makeDimUnitDecl(Span span, IdName dim,
                                              Option<DimExpr> derived,
                                              Option<IdName> defaultId) {
        return new DimUnitDecl(span, Option.some(dim), derived, defaultId,
                               false, Collections.<IdName>emptyList(),
                               Option.<ExprOrUnitExpr>none());
    }

    public static DimUnitDecl makeDimUnitDecl(Span span, Option<DimExpr> derived,
                                              String unit, List<IdName> ids,
                                              Option<ExprOrUnitExpr> def) {
        boolean si_unit;
        if (unit.equals("SI_unit")) si_unit = true;
        else                        si_unit = false;
        return new DimUnitDecl(span, Option.<IdName>none(), derived,
                               Option.<IdName>none(), si_unit, ids, def);
    }

    public static DimUnitDecl makeDimUnitDecl(Span span, IdName dim,
                                              Option<DimExpr> derived,
                                              String unit, List<IdName> ids,
                                              Option<ExprOrUnitExpr> def) {
        boolean si_unit;
        if (unit.equals("SI_unit")) si_unit = true;
        else                        si_unit = false;
        return new DimUnitDecl(span, Option.some(dim), derived,
                               Option.<IdName>none(), si_unit, ids, def);
    }

    public static DottedName makeDottedName(Span span, String s) {
        return new DottedName(span, Useful.list(new Id(span, s)));
    }

    public static DottedName makeDottedName(Span span, Id s) {
        return new DottedName(span, Useful.list(s));
    }

    public static DottedName makeDottedName(Id s) {
        return new DottedName(s.getSpan(), Useful.list(s));
    }

    public static DottedName makeDottedName(Iterable<Id> ids) {
        return new DottedName(FortressUtil.spanAll(ids), IterUtil.asList(ids));
    }

    public static DottedName makeDottedName(Span span, Iterable<Id> ids) {
        return new DottedName(span, IterUtil.asList(ids));
    }

    /** Create a DottedName from the name of the file with the given path. */
    public static DottedName makeDottedName(Span span, String path, String delimiter) {
        List<Id> ids = new ArrayList<Id>();
        String file = new File(path).getName();
        if (file.length() <= 4) {
            throw new ProgramError(new Id(span, "_"), "Invalid file name.");
        }
        else {
            for (String n : file.substring(file.length()-4).split(delimiter)) {
                ids.add(new Id(span, n));
            }
            return new DottedName(span, ids);
        }
    }

    public static QualifiedIdName makeQualifiedIdName(Span span, String s) {
        return new QualifiedIdName(span, Option.<DottedName>none(),
                                   makeIdName(span, s));
    }

    public static QualifiedIdName makeQualifiedIdName(Span span, Id id) {
        return new QualifiedIdName(span, Option.<DottedName>none(), makeIdName(id));
    }

    public static QualifiedIdName makeQualifiedIdName(Id id) {
        return new QualifiedIdName(id.getSpan(), Option.<DottedName>none(),
                                   makeIdName(id));
    }

    public static QualifiedIdName makeQualifiedIdName(IdName name) {
        return new QualifiedIdName(name.getSpan(), Option.<DottedName>none(), name);
    }

    public static QualifiedIdName makeQualifiedIdName(Iterable<Id> apiIds, Id id) {
        Span span;
        Option<DottedName> api;
        if (IterUtil.isEmpty(apiIds)) {
            span = id.getSpan();
            api = Option.none();
        }
        else {
            DottedName n = makeDottedName(apiIds);
            span = FortressUtil.spanTwo(n, id);
            api = Option.some(n);
        }
        return new QualifiedIdName(span, api, makeIdName(id));
    }

    public static QualifiedIdName makeQualifiedIdName(Span span, Iterable<Id> apiIds,
                                                      Id id) {
        Option<DottedName> api;
        if (IterUtil.isEmpty(apiIds)) { api = Option.none(); }
        else { api = Option.some(makeDottedName(apiIds)); }
        return new QualifiedIdName(span, api, makeIdName(id));
    }

    /** Assumes {@code ids} is nonempty. */
    public static QualifiedIdName makeQualifiedIdName(Iterable<Id> ids) {
        return makeQualifiedIdName(IterUtil.skipLast(ids), IterUtil.last(ids));
    }

    public static QualifiedIdName makeQualifiedIdName(DottedName api, IdName name) {
        return new QualifiedIdName(FortressUtil.spanTwo(api, name), Option.some(api),
                                   name);
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
            selfName = Option.unwrap(optSelfName).getText();
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
        return new IdType(span, makeQualifiedIdName(id));
    }

    public static LValueBind makeLValue(LValueBind lvb, Id id) {
        IdName name = makeIdName(id);
        return new LValueBind(lvb.getSpan(), name, lvb.getType(), lvb.getMods(),
                              lvb.isMutable());
    }

    public static LValueBind makeLValue(LValueBind lvb, boolean mutable) {
        return new LValueBind(lvb.getSpan(), lvb.getName(), lvb.getType(),
                              lvb.getMods(), mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, List<Modifier> mods) {
        boolean mutable = lvb.isMutable();
        for (Modifier m : mods) {
            if (m instanceof ModifierVar || m instanceof ModifierSettable)
                mutable = true;
        }
        return new LValueBind(lvb.getSpan(), lvb.getName(), lvb.getType(),
                              mods, mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, List<Modifier> mods,
                                            boolean mutable) {
        return new LValueBind(lvb.getSpan(), lvb.getName(), lvb.getType(),
                              mods, mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, Type ty) {
        return new LValueBind(lvb.getSpan(), lvb.getName(),
                              Option.some(ty), lvb.getMods(),
                              lvb.isMutable());
    }

    public static LValueBind makeLValue(LValueBind lvb, Type ty,
                                        boolean mutable) {
        return new LValueBind(lvb.getSpan(), lvb.getName(),
                              Option.some(ty), lvb.getMods(), mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, Type ty,
                                        List<Modifier> mods) {
        boolean mutable = lvb.isMutable();
        for (Modifier m : mods) {
            if (m instanceof ModifierVar || m instanceof ModifierSettable)
                mutable = true;
        }
        return new LValueBind(lvb.getSpan(), lvb.getName(),
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

    public static IdName makeIdName(String text) {
        Span span = new Span();
        return new IdName(span, new Id(span, text));
    }

    public static IdName makeIdName(Span span, String text) {
        return new IdName(span, new Id(span, text));
    }

    public static IdName makeIdName(Span span, Id id) {
        return new IdName(span, id);
    }

    public static IdName makeIdName(Id id) {
        return new IdName(id.getSpan(), id);
    }

    public static Opr makeOpr(Span span, Op op) {
        return new Opr(span, op);
    }

    public static Opr makeOpr(Op op) {
        return new Opr(op.getSpan(), op);
    }

    public static NatParam makeNatParam(String name) {
        Span s = new Span();
        return new NatParam(s, makeIdName(s, name));
    }

    /** Alternatively, you can invoke the ObjectDecl constructor without a span */
    public static ObjectDecl makeObjectDecl(List<Decl> defs2,
                                            List<Modifier> mods,
                                            IdName name,
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


    public static VarargsParam makeVarargsParam(IdName name, VarargsType type) {
        return new VarargsParam(name.getSpan(), Collections.<Modifier>emptyList(), name, type);
    }

    public static VarargsParam makeVarargsParam(VarargsParam param, List<Modifier> mods) {
        return new VarargsParam(param.getSpan(), mods, param.getName(),
                         param.getVarargsType());
    }

    public static VarargsParam makeVarargsParam(Span span, List<Modifier> mods,
                                                IdName name, VarargsType type) {
        return new VarargsParam(span, mods, name, type);
    }

    public static NormalParam makeParam(Span span, List<Modifier> mods, IdName name,
                                        Type type) {
        return new NormalParam(span, mods, name, Option.some(type), Option.<Expr>none());
    }

    public static NormalParam makeParam(Id id, Type type) {
        return new NormalParam(id.getSpan(), Collections.<Modifier>emptyList(),
                               makeIdName(id), Option.some(type),
                               Option.<Expr>none());
    }

    public static NormalParam makeParam(Id id) {
        return new NormalParam(id.getSpan(), Collections.<Modifier>emptyList(),
                               makeIdName(id), Option.<Type>none(), Option.<Expr>none());
    }

    public static NormalParam makeParam(IdName name) {
        return new NormalParam(name.getSpan(), Collections.<Modifier>emptyList(), name,
                         Option.<Type>none(), Option.<Expr>none());
    }

    public static NormalParam makeParam(NormalParam param, Expr expr) {
        return new NormalParam(param.getSpan(), param.getMods(), param.getName(),
                         param.getType(), Option.some(expr));
    }

    public static NormalParam makeParam(NormalParam param, List<Modifier> mods) {
        return new NormalParam(param.getSpan(), mods, param.getName(),
                         param.getType(), param.getDefaultExpr());
    }

    public static SimpleTypeParam makeSimpleTypeParam(String name) {
        Span s = new Span();
        return new SimpleTypeParam(s, makeIdName(s, name),
                                   Collections.<TraitType>emptyList(), false);
    }

    /** Alternatively, you can invoke the TupleType constructor without keywords */
    public static TupleType makeTupleType(Span span, List<Type> elements, Option<VarargsType> varargs) {
        return new TupleType(span, elements, varargs,
                             Collections.<KeywordType>emptyList());
    }


    public static TypeArg makeTypeArg(Span span, String string) {
        return new TypeArg(span, new IdType(span, makeQualifiedIdName(span, string)));
    }

    public static VarDecl makeVarDecl(Span span, IdName name, Expr init) {
        LValueBind bind = new LValueBind(span, name, Option.<Type>none(),
                                         Collections.<Modifier>emptyList(), true);
        return new VarDecl(span, Useful.<LValueBind>list(bind), init);
    }

    public static VarDecl makeVarDecl(Span span, String name, Expr init) {
        LValueBind bind = new LValueBind(span, makeIdName(span, name),
                                         Option.<Type>none(),
                                         Collections.<Modifier>emptyList(), true);
        return new VarDecl(span, Useful.<LValueBind>list(bind), init);
    }

    public static BoolExpr makeInParentheses(BoolExpr be) {
        return be.accept(new NodeAbstractVisitor<BoolExpr>() {
            public BoolExpr forBoolConstant(BoolConstant b) {
                return new BoolConstant(b.getSpan(), true, b.isBool());
            }
            public BoolExpr forBoolRef(BoolRef b) {
                return new BoolRef(b.getSpan(), true, b.getName());
            }
            public BoolExpr forNotConstraint(NotConstraint b) {
                return new NotConstraint(b.getSpan(), true, b.getBool());
            }
            public BoolExpr forOrConstraint(OrConstraint b) {
                return new OrConstraint(b.getSpan(), true, b.getLeft(),
                                        b.getRight());
            }
            public BoolExpr forAndConstraint(AndConstraint b) {
                return new AndConstraint(b.getSpan(), true, b.getLeft(),
                                         b.getRight());
            }
            public BoolExpr forImpliesConstraint(ImpliesConstraint b) {
                return new ImpliesConstraint(b.getSpan(), true, b.getLeft(),
                                             b.getRight());
            }
            public BoolExpr forBEConstraint(BEConstraint b) {
                return new BEConstraint(b.getSpan(), true, b.getLeft(),
                                        b.getRight());
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
            public DimExpr forDimId(DimRef t) {
                return new DimRef(t.getSpan(), true, t.getName());
            }
            public DimExpr forProductDim(ProductDim t) {
                return new ProductDim(t.getSpan(), true, t.getMultiplier(),
                                      t.getMultiplicand());
            }
            public DimExpr forQuotientDim(QuotientDim t) {
                return new QuotientDim(t.getSpan(), true, t.getNumerator(),
                                       t.getDenominator());
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

    public static UnitExpr makeInParentheses(UnitExpr dim) {
        return dim.accept(new NodeAbstractVisitor<UnitExpr>() {
            public UnitExpr forBaseUnit(BaseUnit t) {
                return new BaseUnit(t.getSpan(), true);
            }
            public UnitExpr forUnitId(UnitRef t) {
                return new UnitRef(t.getSpan(), true, t.getName());
            }
            public UnitExpr forProductUnit(ProductUnit t) {
                return new ProductUnit(t.getSpan(), true, t.getMultiplier(),
                                       t.getMultiplicand());
            }
            public UnitExpr forQuotientUnit(QuotientUnit t) {
                return new QuotientUnit(t.getSpan(), true, t.getNumerator(),
                                        t.getDenominator());
            }
            public UnitExpr forExponentUnit(ExponentUnit t) {
                return new ExponentUnit(t.getSpan(), true, t.getBase(),
                                        t.getPower());
            }
            public UnitExpr forOpUnit(OpUnit t) {
                return new OpUnit(t.getSpan(), true, t.getVal(), t.getOp());
            }
            public UnitExpr defaultCase(Node x) {
                throw new InterpreterBug(x,
                                         "makeInParentheses: " + x.getClass() +
                                         " is not a subtype of UnitExpr.");
            }
        });
    }

    public static IntExpr makeInParentheses(IntExpr ie) {
        return ie.accept(new NodeAbstractVisitor<IntExpr>() {
            public IntExpr forNumberConstraint(NumberConstraint i) {
                return new NumberConstraint(i.getSpan(), true, i.getVal());
            }
            public IntExpr forIntRef(IntRef i) {
                return new IntRef(i.getSpan(), true, i.getName());
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
            public StaticArg forBoolArg(BoolArg t) {
                return new BoolArg(t.getSpan(), true, t.getBool());
            }
            public StaticArg forIntArg(IntArg t) {
                return new IntArg(t.getSpan(), true, t.getVal());
            }
            public StaticArg forOprArg(OprArg t) {
                return new OprArg(t.getSpan(), true, t.getName());
            }
            public StaticArg forDimArg(DimArg t) {
                return new DimArg(t.getSpan(), true, t.getDim());
            }
            public StaticArg forUnitArg(UnitArg t) {
                return new UnitArg(t.getSpan(), true, t.getUnit());
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
                return new IdType(t.getSpan(), true, t.getName());
            }
            public Type forMatrixType(MatrixType t) {
                return new MatrixType(t.getSpan(), true, t.getElement(),
                                      t.getDimensions());
            }
            public Type forInstantiatedType(InstantiatedType t) {
                return new InstantiatedType(t.getSpan(), true, t.getName(),
                                            t.getArgs());
            }
            public Type forTupleType(TupleType t) {
                return new TupleType(t.getSpan(), true, t.getElements(),
                                     t.getVarargs(), t.getKeywords());
            }
            public Type forVoidType(VoidType t) {
                return new VoidType(t.getSpan(), true);
            }
            public Type forTaggedDimType(TaggedDimType t) {
                return new TaggedDimType(t.getSpan(), true, t.getType(),
                                         t.getDim(), t.getUnit());
            }
            public Type forTaggedUnitType(TaggedUnitType t) {
                return new TaggedUnitType(t.getSpan(), true, t.getType(),
                                          t.getUnit());
            }
            public Type defaultCase(Node x) {
                throw new InterpreterBug(x,
                                         "makeInParentheses: " + x.getClass() +
                                         " is not a subtype of Type.");
            }
        });
    }
}
