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

package com.sun.fortress.interpreter.nodes_util;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.math.BigInteger;
import com.sun.fortress.interpreter.nodes.*;
import com.sun.fortress.interpreter.useful.*;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.interpreter.parser.precedence.resolver.PrecedenceMap;

public class NodeFactory {
    public static AbsFnDecl makeAbsFnDecl(Span s, List<Modifier> mods,
                                          Option<Id> optSelfName, FnName name,
                                          Option<List<StaticParam>> staticParams,
                                          List<Param> params,
                                          Option<TypeRef> returnType,
                                          List<TypeRef> throwss,
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
        return new AliasedName(span, makeName(id.getSpan(), id),
                               new None<FnName>());
    }

    public static AliasedName makeAliasedName(Span span, Id id, DottedId alias) {
        return new AliasedName(span, makeName(id.getSpan(), id),
                               new Some<FnName>(alias));
    }

    public static AliasedName makeAliasedName(Span span, OprName op) {
        return new AliasedName(span, op, new None<FnName>());
    }

    public static AliasedName makeAliasedName(Span span, OprName op, OprName alias) {
        return new AliasedName(span, op, new Some<FnName>(alias));
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
                                          List<TypeRef> throws_) {
        List<TypeRef> domains;
        if (domain instanceof TupleType) {
            domains = ((TupleType)domain).getElements();
        } else {
            domains = new ArrayList<TypeRef>();
            domains.add(domain);
        }
        return new ArrowType(span, domains, range, throws_);
    }

    public static BaseNatRef makeBaseNatRef(Span span, IntLiteral value) {
        return new BaseNatRef(span, value.getVal().intValue());
    }

    public static BaseOprRef makeBaseOprRef(Span span, Op op) {
        return new BaseOprRef(span, new Opr(span, op));
    }

    public static ConstructorFnName makeConstructorFnName(DefOrDecl def) {
        return new ConstructorFnName(NodeUtil.getSpan(def), def);
    }

    public static Contract makeContract() {
        return new Contract(new Span(), Collections.<Expr> emptyList(),
                            Collections.<EnsuresClause> emptyList(),
                            Collections.<Expr> emptyList());
    }

    public static DottedId makeDottedId(Span span, String s) {
        return new DottedId(span, Useful.list(s));
    }

    public static DottedId makeDottedId(Span span, Id s) {
        return new DottedId(span, Useful.list(s.getName()));
    }

    public static DottedId makeDottedId(Span span, Id s, List<Id> ls) {
        return new DottedId(span, Useful.prependMapped(s, ls,
                                                // fn(x) => x.getName()
                                                new Fn<Id, String>() {
                                                    @Override
                                                    public String apply(Id x) {
                                                        return x.getName();
                                                    }
        }));
    }

    public static FnDecl makeFnDecl(Span s, List<Modifier> mods,
                                    Option<Id> optSelfName, FnName name,
                                    Option<List<StaticParam>> staticParams,
                                    List<Param> params,
                                    Option<TypeRef> returnType,
                                    List<TypeRef> throwss,
                                    List<WhereClause> where, Contract contract,
                                    Expr body) {
        String selfName;
        if (optSelfName.isPresent()) {
            selfName = optSelfName.getVal().getName();
        } else {
            selfName = WellKnownNames.defaultSelfName;
        }
        return new FnDecl(s, mods, name, staticParams, params, returnType,
                          throwss, where, contract, selfName, body);
    }

    public static Fun makeFun(Span span, String string) {
        return new Fun(span, new Id(span, string));
    }

    /**
     * Call this only for names that have no location. (When/if this constructor
     * disappears, it will be because we have a better plan for those names, and
     * its disappearance will identify all those places that need updating).
     */
    public static Fun makeFun(String string) {
        Span span = new Span();
        return new Fun(span, new Id(span, string));
    }

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

    public static LValueBind makeLValue(LValueBind lvb, TypeRef ty) {
        return new LValueBind(lvb.getSpan(), lvb.getName(),
                              new Some<TypeRef>(ty), lvb.getMods(),
                              lvb.isMutable());
    }

    public static LValueBind makeLValue(LValueBind lvb, TypeRef ty,
                                        boolean mutable) {
        return new LValueBind(lvb.getSpan(), lvb.getName(),
                              new Some<TypeRef>(ty), lvb.getMods(), mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, TypeRef ty,
                                        List<Modifier> mods) {
        boolean mutable = lvb.isMutable();
        for (Modifier m : mods) {
            if (m instanceof ModifierVar || m instanceof ModifierSettable)
                mutable = true;
        }
        return new LValueBind(lvb.getSpan(), lvb.getName(),
                              new Some<TypeRef>(ty), mods, mutable);
    }

    public static MatrixType makeMatrixType(Span span, TypeRef element,
                                            ExtentRange dimension,
                                            List<ExtentRange> dimensions) {
        List<ExtentRange> dims = new ArrayList<ExtentRange>();
        dims.add(dimension);
        dims.addAll(dimensions);
        return new MatrixType(span, element, dims);
    }

    public static Name makeName(Span span, Id id) {
        return new Name(span, new Some<Id>(id), new None<Op>());
    }

    public static Name makeName(Span span, Op op) {
        return new Name(span, new None<Id>(), new Some<Op>(op));
    }

    public static NatParam makeNatParam(String name) {
        return new NatParam(new Span(), new Id(new Span(), name));
    }

    public static ObjectDecl makeObjectDecl(List<Decl> defs2,
                                            List<Modifier> mods,
                                            Id name,
                                            Option<List<StaticParam>> stParams,
                                            Option<List<Param>> params,
                                            Option<List<TypeRef>> traits,
                                            List<TypeRef> throws_,
                                            List<WhereClause> where,
                                            Contract contract) {
        return new ObjectDecl(new Span(), mods, name, stParams, params, traits,
                              throws_, where, contract, defs2);
    }

    public static Op makeOp(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name));
    }

    public static Param makeParam(Span span, List<Modifier> mods, Id name,
                                  TypeRef type) {
        return new Param(span, mods, name, new Some<TypeRef>(type),
                         new None<Expr>());
    }

    public static Param makeParam(Id name, TypeRef type) {
        return new Param(name.getSpan(), Collections.<Modifier>emptyList(), name,
                         new Some<TypeRef>(type), new None<Expr>());
    }

    public static Param makeParam(Id name) {
        return new Param(name.getSpan(), Collections.<Modifier>emptyList(), name,
                         new None<TypeRef>(), new None<Expr>());
    }

    public static Param makeParam(Param param, Expr expr) {
        return new Param(param.getSpan(), param.getMods(), param.getName(),
                         param.getType(), new Some<Expr>(expr));
    }

    public static Param makeParam(Param param, List<Modifier> mods) {
        return new Param(param.getSpan(), mods, param.getName(),
                         param.getType(), param.getDefaultExpr());
    }

    public static SimpleTypeParam makeSimpleTypeParam(String name) {
        return new SimpleTypeParam(new Span(), new Id(new Span(), name),
                                   new None<List<TypeRef>>(), false);
    }

    public static TupleType makeTupleType(Span span, List<TypeRef> elements) {
        return new TupleType(span, elements,
                             Collections.<KeywordType>emptyList());
    }


    public static TypeArg makeTypeArg(Span span, String string) {
        return new TypeArg(span, new IdType(span, makeDottedId(span, string)));
    }

    public static VarDecl makeVarDecl(Span span, Id id, Expr init) {
        return new VarDecl(span, Useful.<LValue>list(
                                new LValueBind(span, id,
                                               new None<TypeRef>(),
                                               Collections.<Modifier>emptyList(),
                                               true)),
                           init);
    }
}
