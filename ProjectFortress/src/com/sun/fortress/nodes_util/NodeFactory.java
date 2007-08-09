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

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.math.BigInteger;
import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.parser_util.precedence_resolver.PrecedenceMap;

public class NodeFactory {
    /** Alternatively, you can invoke the AbsFnDecl constructor without a self name */
    public static AbsFnDecl makeAbsFnDecl(Span s, List<Modifier> mods,
                                          Option<Id> optSelfName, FnName name,
                                          List<StaticParam> staticParams,
                                          List<Param> params,
                                          Option<TypeRef> returnType,
                                          Option<List<TraitType>> throwss,
                                          List<WhereClause> where,
                                          Contract contract) {
        String selfName;
        if (optSelfName.isPresent()) {
            selfName = optSelfName.getVal().getName();
        } else {
            selfName = WellKnownNames.defaultSelfName;
        }
        return new AbsFnDecl(s, mods, name, staticParams, params, returnType,
                             throwss, where, contract, selfName);
    }

    public static AliasedName makeAliasedName(Span span, Id id) {
        return new AliasedName(span, makeFnName(id.getSpan(), id),
                               None.<FnName>make());
    }

    public static AliasedName makeAliasedName(Span span, Id id, DottedId alias) {
        return new AliasedName(span, makeFnName(id.getSpan(), id),
                               Some.<FnName>make(alias));
    }

    /** Alternatively, you can invoke the AbsFnDecl constructor without an alias */
    public static AliasedName makeAliasedName(Span span, OprName op) {
        return new AliasedName(span, op, None.<FnName>make());
    }

    public static AliasedName makeAliasedName(Span span, OprName op,
                                              OprName alias) {
        return new AliasedName(span, op, Some.<FnName>make(alias));
    }

    public static ArrayType makeArrayType(Span span, TypeRef element,
                                          Option<FixedDim> ind) {
        FixedDim indices;
        if (ind.isPresent()) indices = (FixedDim)((Some)ind).getVal();
        else indices = new FixedDim(span, Collections.<ExtentRange>emptyList());
        return new ArrayType(span, element, indices);
    }

    public static ArrowType makeArrowType(Span span, TypeRef domain,
                                          TypeRef range,
                                          Option<List<TraitType>> throws_) {
        return new ArrowType(span, domain, range, throws_);
    }

    public static BaseNatRef makeBaseNatRef(Span span, IntLiteral value) {
        return new BaseNatRef(span, value.getVal().intValue());
    }

    public static BaseOprRef makeBaseOprRef(Span span, Op op) {
        return new BaseOprRef(span, new Opr(span, op));
    }

    public static ConstructorFnName makeConstructorFnName(GenericWithParams def) {
        return new ConstructorFnName(def.getSpan(), def);
    }

  /** Alternatively, you can invoke the Contract constructor without any parameters */
    public static Contract makeContract() {
        return new Contract(new Span(), None.<List<Expr>> make(),
                            None.<List<EnsuresClause>> make(),
                            None.<List<Expr>> make());
    }

    public static DottedId makeDottedId(Span span, String s) {
        return new DottedId(span, Useful.list(makeId(s)));
    }

    public static DottedId makeDottedId(Span span, Id s) {
        return new DottedId(span, Useful.list(s));
    }

    public static DottedId makeDottedId(Span span, Id s, List<Id> ls) {
        return new DottedId(span, Useful.prepend(s, ls));
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
                                   Option<TypeRef> returnType,
                                   Option<List<TraitType>> throwss,
                                   List<WhereClause> where, Contract contract,
                                   Expr body) {
        String selfName;
        if (optSelfName.isPresent()) {
            selfName = optSelfName.getVal().getName();
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
        return new IdType(span, makeDottedId(span, id));
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

    public static LValueBind makeLValue(LValueBind lvb, TypeRef ty) {
        return new LValueBind(lvb.getSpan(), lvb.getId(),
                              Some.<TypeRef>make(ty), lvb.getMods(),
                              lvb.isMutable());
    }

    public static LValueBind makeLValue(LValueBind lvb, TypeRef ty,
                                        boolean mutable) {
        return new LValueBind(lvb.getSpan(), lvb.getId(),
                              Some.<TypeRef>make(ty), lvb.getMods(), mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, TypeRef ty,
                                        List<Modifier> mods) {
        boolean mutable = lvb.isMutable();
        for (Modifier m : mods) {
            if (m instanceof ModifierVar || m instanceof ModifierSettable)
                mutable = true;
        }
        return new LValueBind(lvb.getSpan(), lvb.getId(),
                              Some.<TypeRef>make(ty), mods, mutable);
    }

    public static MatrixType makeMatrixType(Span span, TypeRef element,
                                            ExtentRange dimension) {
        List<ExtentRange> dims = new ArrayList<ExtentRange>();
        dims.add(dimension);
        return new MatrixType(span, element, dims);
    }

    public static MatrixType makeMatrixType(Span span, TypeRef element,
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
                                            List<TraitType> traits,
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
                                  TypeRef type) {
        return new NormalParam(span, mods, name, Some.<TypeRef>make(type),
                         None.<Expr>make());
    }

    public static NormalParam makeParam(Id name, TypeRef type) {
        return new NormalParam(name.getSpan(), Collections.<Modifier>emptyList(), name,
                         Some.<TypeRef>make(type), None.<Expr>make());
    }

    public static NormalParam makeParam(Id name) {
        return new NormalParam(name.getSpan(), Collections.<Modifier>emptyList(), name,
                         None.<TypeRef>make(), None.<Expr>make());
    }

    public static NormalParam makeParam(NormalParam param, Expr expr) {
        return new NormalParam(param.getSpan(), param.getMods(), param.getId(),
                         param.getType(), Some.<Expr>make(expr));
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
    public static TupleType makeTupleType(Span span, List<TypeRef> elements, Option<VarargsType> varargs) {
        return new TupleType(span, elements, varargs,
                             Collections.<KeywordType>emptyList());
    }


    public static TypeArg makeTypeArg(Span span, String string) {
        return new TypeArg(span, new IdType(span, makeDottedId(span, string)));
    }

    public static VarDecl makeVarDecl(Span span, Id id, Expr init) {
        return new VarDecl(span, Useful.<LValueBind>list(
                                new LValueBind(span, id,
                                               None.<TypeRef>make(),
                                               Collections.<Modifier>emptyList(),
                                               true)),
                           init);
    }

    public static TypeRef makeInParentheses(TypeRef ty) {
        return ty.accept(new NodeAbstractVisitor<TypeRef>() {
            public TypeRef forArrowType(ArrowType t) {
                return new ArrowType(t.getSpan(), true, t.getDomain(),
                                     t.getRange(), t.getThrowsClause());
            }
            public TypeRef forArrayType(ArrayType t) {
                return new ArrayType(t.getSpan(), true, t.getElement(),
                                     t.getIndices());
            }
            public TypeRef forIdType(IdType t) {
                return new IdType(t.getSpan(), true, t.getDottedId());
            }
            public TypeRef forMatrixType(MatrixType t) {
                return new MatrixType(t.getSpan(), true, t.getElement(),
                                      t.getDimensions());
            }
            public TypeRef forInstantiatedType(InstantiatedType t) {
                return new InstantiatedType(t.getSpan(), true, t.getDottedId(),
                                            t.getArgs());
            }
            public TypeRef forTupleType(TupleType t) {
                return new TupleType(t.getSpan(), true, t.getElements(),
                                     t.getVarargs(), t.getKeywords());
            }
            public TypeRef forVoidType(VoidType t) {
                return new VoidType(t.getSpan(), true);
            }
            public TypeRef forDimRef(DimRef t) {
                return new DimRef(t.getSpan(), true, t.getVal());
            }
            public TypeRef forProductDimType(ProductDimType t) {
                return new ProductDimType(t.getSpan(), true, t.getMultiplier(),
                                          t.getMultiplicand());
            }
            public TypeRef forQuotientDimType(QuotientDimType t) {
                return new QuotientDimType(t.getSpan(), true, t.getNumerator(),
                                           t.getDenominator());
            }
            public TypeRef forProductUnitType(ProductUnitType t) {
                return new ProductUnitType(t.getSpan(), true, t.getMultiplier(),
                                           t.getMultiplicand());
            }
            public TypeRef forQuotientUnitType(QuotientUnitType t) {
                return new QuotientUnitType(t.getSpan(), true, t.getNumerator(),
                                            t.getDenominator());
            }
            public TypeRef forDimTypeConversion(DimTypeConversion t) {
                return new DimTypeConversion(t.getSpan(), true, t.getType(),
                                             t.getDim());
            }
            public TypeRef forBaseNatRef(BaseNatRef t) {
                return new BaseNatRef(t.getSpan(), true, t.getValue());
            }
            public TypeRef forBaseOprRef(BaseOprRef t) {
                return new BaseOprRef(t.getSpan(), true, t.getFnName());
            }
            public TypeRef forBaseDimRef(BaseDimRef t) {
                return new BaseDimRef(t.getSpan(), true);
            }
            public TypeRef forBaseUnitRef(BaseUnitRef t) {
                return new BaseUnitRef(t.getSpan(), true);
            }
            public TypeRef forBaseBoolRef(BaseBoolRef t) {
                return new BaseBoolRef(t.getSpan(), true, t.isBool());
            }
            public TypeRef forNotBoolRef(NotBoolRef t) {
                return new NotBoolRef(t.getSpan(), true, t.getVal());
            }
            public TypeRef forOrBoolRef(OrBoolRef t) {
                return new OrBoolRef(t.getSpan(), true, t.getLeft(),
                                     t.getRight());
            }
            public TypeRef forAndBoolRef(AndBoolRef t) {
                return new AndBoolRef(t.getSpan(), true, t.getLeft(),
                                      t.getRight());
            }
            public TypeRef forImpliesBoolRef(ImpliesBoolRef t) {
                return new ImpliesBoolRef(t.getSpan(), true, t.getLeft(),
                                          t.getRight());
            }
            public TypeRef forSumStaticArg(SumStaticArg t) {
                return new SumStaticArg(t.getSpan(), true, t.getValues());
            }
            public TypeRef forProductStaticArg(ProductStaticArg t) {
                return new ProductStaticArg(t.getSpan(), true, t.getValues());
            }
            public TypeRef forQuotientStaticArg(QuotientStaticArg t) {
                return new QuotientStaticArg(t.getSpan(), true, t.getNumerator(),
                                             t.getDenominator());
            }
            public TypeRef forExponentStaticArg(ExponentStaticArg t) {
                return new ExponentStaticArg(t.getSpan(), true, t.getBase(),
                                             t.getPower());
            }
            public TypeRef forDimensionStaticArg(DimensionStaticArg t) {
                return new DimensionStaticArg(t.getSpan(), true, t.getVal(),
                                              t.getOp());
            }
            public TypeRef forTypeArg(TypeArg t) {
                return new TypeArg(t.getSpan(), true, t.getType());
            }
            public TypeRef defaultCase(Node x) {
                throw new InterpreterError("makeInParentheses: " + x.getClass() +
                                           " is not a subtype of TypeRef.");
            }
        });
    }
}
