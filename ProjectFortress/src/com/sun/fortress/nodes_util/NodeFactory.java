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

import java.io.File;
import java.util.*;
import java.math.BigInteger;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.lambda.Lambda;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;

import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.parser_util.precedence_resolver.PrecedenceMap;
import com.sun.fortress.parser_util.FortressUtil;

public class NodeFactory {
    /**
     * For use only when there is no hope of
     * attaching a true span.
     * @param villain
     * @return a span from a string.
     * @deprecated
     */
    public static Span makeSpan(String villain) {
        SourceLoc sl = new SourceLocRats(villain,0,0,0);
        return new Span(sl,sl);
    }

    /**
     *
     * @param start
     * @return  the span from a node.
     */public static Span makeSpan(ASTNode node) {
        return node.getSpan();
    }

    /**
     *
     * @param start
     * @param finish
     * @return the span encompassing both spans.
     */public static Span makeSpan(Span start, Span finish) {
        return new Span(start.getBegin(), finish.getEnd());
    }

    /**
     *
     * @param start
     * @param finish
     * @return the span encompassing the spans of both nodes.
     */
     public static Span makeSpan(ASTNode start, ASTNode finish) {
        return makeSpan(start.getSpan(), finish.getSpan());
    }

    /**
     *
     * @param start
     * @param l
     * @return the span encompassing the spans of node start to the span of the end of the list.
     */
     public static Span makeSpan(ASTNode start, List<? extends ASTNode> l) {
         int s = l.size();
        return makeSpan(start, s == 0 ? start : l.get(s-1));
    }

     /**
      *
      * @param start
      * @param l
      * @return the span encompassing the spans of list start to node finish.
      */
      public static Span makeSpan(List<? extends ASTNode> l, ASTNode finish) {
         int s = l.size();
        return makeSpan(s == 0 ? finish : l.get(0), finish);
    }

     /**
      *
      * @param start
      * @param l
      * @return the span encompassing the spans the first and last nodes of the list.
      */
     public static Span makeSpan(String ifEmpty, List<? extends ASTNode> l) {
         int s = l.size();
        return s==0 ? makeSpan(ifEmpty) : makeSpan(l.get(0), l.get(s-1));
    }
    /**
     * In some situations, a begin-to-end span is not really right, and something
     * more like a set of spans ought to be used.  Even though this is not yet
     * implemented, the name is provided to allow expression of intent.
     *
     * @param start
     * @param l
     * @return the span encompassing the spans of node start to the span of the end of the list.
     */
     public static Span makeSetSpan(ASTNode start, List<? extends ASTNode> l) {
         return makeSpan(start, l);
     }
     /**
      * In some situations, a begin-to-end span is not really right, and something
      * more like a set of spans ought to be used.  Even though this is not yet
      * implemented, the name is provided to allow expression of intent.
      *
      * @param start
      * @param l
      * @return the span encompassing the spans {a, b}
      */
     public static Span makeSetSpan(ASTNode a, ASTNode b) {
         return makeSpan(a,b);
     }
     /**
     * In some situations, a begin-to-end span is not really right, and something
     * more like a set of spans ought to be used.  Even though this is not yet
     * implemented, the name is provided to allow expression of intent.
     *
     * @param l
     * @return the span encompassing the spans the first and last nodes of the list.
     */
    public static Span makeSetSpan(String ifEmpty, List<? extends ASTNode> l) {
        return makeSpan(ifEmpty, l);
    }

    public static Component makeComponent(Span span, APIName name,
                                          List<Import> imports,
                                          List<Decl> decls,
                                          List<APIName> exports) {
        return makeComponent(span, name, imports, decls, false, exports);
    }

    public static Component makeComponent(Span span, APIName name,
                                          List<Import> imports,
                                          List<Decl> decls,
                                          boolean isNative,
                                          List<APIName> exports) {
        return new Component(span, name, imports, decls, isNative, exports);
    }

    public static AliasedSimpleName makeAliasedSimpleName(IdOrOpOrAnonymousName name) {
        return new AliasedSimpleName(name.getSpan(), name,
                                     Option.<IdOrOpOrAnonymousName>none());
    }

    public static AliasedSimpleName makeAliasedSimpleName(IdOrOpOrAnonymousName name,
                                                          IdOrOpOrAnonymousName alias) {
        return new AliasedSimpleName(FortressUtil.spanTwo(name, alias), name,
                                     Option.<IdOrOpOrAnonymousName>some(alias));
    }

    public static AliasedAPIName makeAliasedAPIName(APIName api) {
        return new AliasedAPIName(api.getSpan(), api, Option.<Id>none());
    }

    public static AliasedAPIName makeAliasedAPIName(APIName api, Id alias) {
        return new AliasedAPIName(FortressUtil.spanTwo(api, alias), api,
                                  Option.<Id>some(alias));
    }

    public static TraitDecl makeTraitDecl(Span span, Id name,
                                          List<StaticParam> sparams,
                                          List<TraitTypeWhere> extendsC) {
        return makeTraitDecl(span, Collections.<Modifier>emptyList(), name,
                             sparams, extendsC, Option.<WhereClause>none(),
                             Collections.<Decl>emptyList(),
                             Collections.<BaseType>emptyList(),
                             Option.<List<BaseType>>none());
    }

    public static TraitDecl makeTraitDecl(Span span, List<Modifier> mods, Id name,
                                          List<StaticParam> sparams,
                                          List<TraitTypeWhere> extendsC,
                                          Option<WhereClause> whereC,
                                          List<Decl> decls,
                                          List<BaseType> excludesC,
                                          Option<List<BaseType>> comprisesC) {
        return new TraitDecl(span, mods, name, sparams, extendsC, whereC, decls,
                             excludesC, comprisesC);
    }

    public static ObjectDecl makeObjectDecl(Span span, Id name,
                                            List<TraitTypeWhere> extendsC,
                                            List<Decl> decls) {
        return makeObjectDecl(span, Collections.<Modifier>emptyList(), name,
                              Collections.<StaticParam>emptyList(),
                              extendsC, Option.<WhereClause>none(), decls,
                              Option.<List<Param>>none(),
                              Option.<List<BaseType>>none(),
                              Option.<Contract>none());
    }

    public static ObjectDecl makeObjectDecl(Span span, Id name,
                                            Option<List<Param>> params) {
        return makeObjectDecl(span, Collections.<Modifier>emptyList(), name,
                              Collections.<StaticParam>emptyList(),
                              Collections.<TraitTypeWhere>emptyList(),
                              Option.<WhereClause>none(),
                              Collections.<Decl>emptyList(), params,
                              Option.<List<BaseType>>none(),
                              Option.<Contract>none());
    }

    public static ObjectDecl makeObjectDecl(Span span, Id name,
                                            List<StaticParam> sparams,
                                            List<TraitTypeWhere> extendsC,
                                            List<Decl> decls,
                                            Option<List<Param>> params) {
        return makeObjectDecl(span, Collections.<Modifier>emptyList(), name,
                              sparams, extendsC,
                              Option.<WhereClause>none(), decls,
                              params,
                              Option.<List<BaseType>>none(),
                              Option.<Contract>none());
    }

    public static ObjectDecl makeObjectDecl(Span span, List<Modifier> mods, Id name,
                                            List<StaticParam> sparams,
                                            List<TraitTypeWhere> extendsC,
                                            Option<WhereClause> whereC,
                                            List<Decl> decls,
                                            Option<List<Param>> params,
                                            Option<List<BaseType>> throwsC,
                                            Option<Contract> contract) {
        return new ObjectDecl(span, mods, name, sparams, extendsC, whereC, decls,
                              params, throwsC, contract);
    }

    public static FnDecl makeFnDecl(Span span, List<Modifier> mods,
                                    IdOrOpOrAnonymousName name,
                                    List<StaticParam> staticParams,
                                    List<Param> params,
                                    Option<Type> returnType) {
        return makeFnDecl(span, mods, name, staticParams, params, returnType,
                          Option.<List<BaseType>>none(),
                          Option.<WhereClause>none(),
                          Option.<Contract>none());
    }

    public static FnDecl makeFnDecl(Span span, List<Modifier> mods,
                                    IdOrOpOrAnonymousName name,
                                    List<Param> params,
                                    Option<Type> returnType,
                                    Option<Expr> body) {
        return makeFnDecl(span, mods, name,
                          Collections.<StaticParam>emptyList(),
                          params, returnType,
                          Option.<List<BaseType>>none(),
                          Option.<WhereClause>none(),
                          Option.<Contract>none(),
                          body);
    }

    public static FnDecl makeFnDecl(Span span, List<Modifier> mods,
                                    IdOrOpOrAnonymousName name,
                                    List<StaticParam> staticParams,
                                    List<Param> params,
                                    Option<Type> returnType,
                                    Option<List<BaseType>> throwsC,
                                    Option<WhereClause> whereC,
                                    Option<Contract> contract) {
        return makeFnDecl(span, mods, name, staticParams, params, returnType,
                          throwsC, whereC, contract,
                          new Id(span, "FN$"+span.toString()),
                          Option.<Expr>none(), Option.<Id>none());
    }

    public static FnDecl makeFnDecl(Span span, List<Modifier> mods,
                                    IdOrOpOrAnonymousName name,
                                    List<StaticParam> staticParams,
                                    List<Param> params,
                                    Option<Type> returnType,
                                    Option<List<BaseType>> throwsC,
                                    Option<WhereClause> whereC,
                                    Option<Contract> contract,
                                    Option<Expr> body) {
        return makeFnDecl(span, mods, name, staticParams, params, returnType,
                          throwsC, whereC, contract,
                          new Id(span, "FN$"+span.toString()),
                          body, Option.<Id>none());
    }

    public static FnDecl makeFnDecl(Span span, List<Modifier> mods,
                                    IdOrOpOrAnonymousName name,
                                    List<StaticParam> staticParams,
                                    List<Param> params,
                                    Option<Type> returnType,
                                    Option<List<BaseType>> throwsC,
                                    Option<WhereClause> whereC,
                                    Option<Contract> contract,
                                    Id unambiguousName,
                                    Option<Expr> body,
                                    Option<Id> implementsUnambiguousName) {
        return new FnDecl(span, mods, name, staticParams, params, returnType,
                          throwsC, whereC, contract, unambiguousName,
                          body, implementsUnambiguousName);
    }

    /*
    public static FnDecl makeFnDecl(Span span, List<Modifier> mods,
                                    Id name, Option<Type> type) {
        return makeFnDecl(span, mods, name, Collections.<Param>emptyList(), type);
    }

    public static FnDecl makeFnDecl(Span span, List<Modifier> mods,
                                    Id name, List<Param> params,
                                    Option<Type> type) {
        return new FnDecl(span, mods, name, Collections.<StaticParam>emptyList(),
                          params, type, Option.<Expr>none());
    }

    public static FnDecl makeFnDecl(Span s, List<Modifier> mods,
                                    IdOrOpOrAnonymousName name,
                                    List<StaticParam> staticParams,
                                    List<Param> params,
                                    Option<Type> returnType,
                                    Option<List<BaseType>> throwss,
                                    Option<WhereClause> where,
                                    Option<Contract> contract) {
        return new FnDecl(s, mods, name, staticParams, params, returnType,
                          throwss, where, contract, Option.<Expr>none(), Option.<Id>none());
    }

    public static FnDecl makeFnDecl(Span span, List<Modifier> mods,
                                  Id name, Option<Type> type, Expr body) {
        return makeFnDecl(span, mods, name, Collections.<Param>emptyList(), type,
                          Option.<Expr>some(body));
    }

    public static FnDecl makeFnDecl(Span span, List<Modifier> mods,
                                  Id name, Option<Type> type, Option<Expr> body) {
        return makeFnDecl(span, mods, name, Collections.<Param>emptyList(), type,
                          body);
    }

    public static FnDecl makeFnDecl(Span span, List<Modifier> mods,
                                  Id name, List<Param> params,
                                  Option<Type> type, Expr body) {
        return new FnDecl(span, mods, name, Collections.<StaticParam>emptyList(),
                          params, type, Option.<List<BaseType>>none(),
                          Option.<WhereClause>none(), Option.<Contract>none(),
                          Option.<Expr>some(body));
    }

    public static FnDecl makeFnDecl(Span span, List<Modifier> mods,
                                  Id name, List<Param> params,
                                  Option<Type> type, Option<Expr> body) {
        return new FnDecl(span, mods, name, Collections.<StaticParam>emptyList(),
                         params, type, Option.<List<BaseType>>none(),
                         Option.<WhereClause>none(), Option.<Contract>none(),
                         body);
    }
    */

    public static Id makeTemporaryId() {
        return makeId("$$bogus_name$$");
    }

    public static Op makeTemporaryOp() {
        return makeOp("$$bogus_name$$");
    }

    public static APIName makeAPINameSkipLast(Id first, Id rest) {
        List<Id> ids = new ArrayList<Id>();
        Id last = first;
        ids.add(first);
        if (rest.getApiName().isSome()) {
            List<Id> apiNames = rest.getApiName().unwrap().getIds();
            ids.addAll(apiNames);
            if (!IterUtil.isEmpty(apiNames)) last = IterUtil.last(apiNames);
        }
        ids = Useful.immutableTrimmedList(ids);
        return new APIName(FortressUtil.spanTwo(first, last), ids);
    }

    public static APIName makeAPIName(Id first, Id rest) {
        List<Id> ids = new ArrayList<Id>();
        ids.add(first);
        if (rest.getApiName().isSome()) {
            ids.addAll(rest.getApiName().unwrap().getIds());
        }
        ids.add(new Id(rest.getSpan(), rest.getText()));
        ids = Useful.immutableTrimmedList(ids);
        return new APIName(FortressUtil.spanTwo(first, rest), ids);
    }

    public static Id makeIdFromLast(Id id) {
        return new Id(id.getSpan(), id.getText());
    }

    public static ArrayType makeArrayType(Span span, Type element,
            Option<Indices> ind) {
        Indices indices = ind.unwrap(new Indices(span, Collections.<ExtentRange>emptyList()));
        return new ArrayType(span, element, indices);
    }

    public static DimDecl makeDimDecl(Span span, Id dim) {
        return new DimDecl(span, dim);
    }

    public static DimExponent makeDimExponent(DimExponent t, Type s) {
        return new DimExponent(t.getSpan(), t.isParenthesized(), s,
                               t.getPower());
    }

    public static DimBinaryOp makeDimBinaryOp(DimBinaryOp t, DimExpr s, DimExpr u, Op o) {
        return new DimBinaryOp(t.getSpan(), t.isParenthesized(), s, u, o);
    }

    public static DimUnaryOp makeDimUnaryOp(DimUnaryOp t, DimExpr s) {
        return new DimUnaryOp(t.getSpan(), t.isParenthesized(), s, t.getOp());
    }

    public static TraitType makeTraitType(TraitType t,
                                          List<StaticArg> args) {
        return new TraitType(t.getSpan(), t.isParenthesized(),
                             t.getName(), args,
                             Collections.<StaticParam>emptyList());
    }

    public static TraitTypeWhere makeTraitTypeWhere(BaseType in_type) {
        Span sp = in_type.getSpan();
        return new TraitTypeWhere(sp, in_type,
                                  Option.<WhereClause>none());
    }

    public static TraitTypeWhere makeTraitTypeWhere(BaseType in_type, Option<WhereClause> in_where) {
        if ( in_where.isSome() )
            return new TraitTypeWhere(new Span(in_type.getSpan(), in_where.unwrap().getSpan()), in_type, in_where);
        else
            return new TraitTypeWhere(in_type.getSpan(), in_type, in_where);
    }

    public static _InferenceVarType make_InferenceVarType(Span s) {
        return new _InferenceVarType(s, new Object());
    }

    public static List<Type> make_InferenceVarTypes(Span s, int size) {
        List<Type> result = new ArrayList<Type>(size);
        for (int i = 0; i < size; i++) { result.add(make_InferenceVarType(s)); }
        return result;
    }

    public static TupleType makeTupleType(TupleType t, List<Type> tys) {
        return new TupleType(t.getSpan(), t.isParenthesized(), tys);
    }

//    public static ArgType makeArgType(ArgType t, List<Type> tys, Type varargs) {
//    return new ArgType(t.getSpan(), t.isParenthesized(), tys, varargs);
//    }

    public static KeywordType makeKeywordType(KeywordType t, Type s) {
        return new KeywordType(t.getSpan(), t.getName(), s);
    }

    public static TaggedDimType makeTaggedDimType(TaggedDimType t, Type s,
                                                  DimExpr u) {
        return new TaggedDimType(t.getSpan(), t.isParenthesized(), s, u,
                                 t.getUnitExpr());
    }

    public static TaggedUnitType makeTaggedUnitType(TaggedUnitType t, Type s) {
        return new TaggedUnitType(t.getSpan(), t.isParenthesized(), s,
                                  t.getUnitExpr());
    }

    public static TypeArg makeTypeArg(TypeArg t, Type s) {
        return new TypeArg(t.getSpan(), s);
    }

    public static DimArg makeDimArg(DimArg t, DimExpr s) {
        return new DimArg(t.getSpan(), s);
    }

    public static DimArg makeDimArg(DimExpr s) {
        return new DimArg(s.getSpan(), s);
    }

    public static DimRef makeDimRef(Span span, String name) {
        return new DimRef(span, makeId(name));
    }

    public static UnitArg makeUnitArg(UnitExpr s) {
        return new UnitArg(s.getSpan(), s);
    }

    public static UnitRef makeUnitRef(Span span, String name) {
        return new UnitRef(span, makeId(name));
    }

    public static FixedPointType makeFixedPointType(FixedPointType t, Type s) {
        return new FixedPointType(t.getSpan(), t.isParenthesized(), t.getName(),
                s);
    }

    public static FixedPointType makeFixedPointType(_InferenceVarType name, Type s) {
        return new FixedPointType(s.getSpan(), s.isParenthesized(), name, s);
    }

    public static TraitType makeTraitType(Span span, boolean isParenthesized,
                                          Id name, List<StaticArg> args) {
        return new TraitType(span, isParenthesized, name, args,
                             Collections.<StaticParam>emptyList());
    }

    public static TraitType makeTraitType(Span span, boolean isParenthesized,
            Id name, StaticArg... args) {
        return makeTraitType(span, isParenthesized, name, Arrays.asList(args));
    }

    public static TraitType makeTraitType(Id name, StaticArg... args) {
        return makeTraitType(name.getSpan(), false, name, Arrays.asList(args));
    }

    /** Signature separates the first element in order to guarantee a non-empty arg list. */
    public static TraitType makeTraitType(String nameFirst, String... nameRest) {
        // System.err.println("Please don't makeTraitType with a bogus span");
        return makeTraitType(new Span(), false, makeId(nameFirst, nameRest),
                Collections.<StaticArg>emptyList());
    }

    public static TraitType makeTraitType(String name,
                                          List<StaticArg> sargs) {
        // System.err.println("Please don't makeTraitType with a bogus span");
        return new TraitType(new Span(),makeId(name),sargs,
                             Collections.<StaticParam>emptyList());
    }

    public static TraitType makeTraitType(Id name,
                                          List<StaticArg> sargs) {
        return new TraitType(name.getSpan(), name, sargs,
                             Collections.<StaticParam>emptyList());
    }

    public static TraitType makeTraitType(Id name) {
        return new TraitType(name.getSpan(), name, Collections.<StaticArg>emptyList(),
                             Collections.<StaticParam>emptyList());
    }

    public static IntersectionType makeIntersectionType(Type t1, Type t2) {
        return new IntersectionType(FortressUtil.spanTwo(t1, t2), Arrays.asList(t1, t2));
    }

    public static IntersectionType makeIntersectionType(Set<? extends Type> types){
        return new IntersectionType(FortressUtil.spanAll(types),CollectUtil.makeList(types));
    }

    public static UnionType makeUnionType(Type t1, Type t2) {
        return new UnionType(FortressUtil.spanTwo(t1, t2), Arrays.asList(t1, t2));
    }

    public static UnionType makeUnionType(Set<? extends Type> types){
        return new UnionType(FortressUtil.spanAll(types),CollectUtil.makeList(types));
    }

//    public static ArrowType makeArrowType(Span span, Type domain,
//    Type range,
//    Option<List<BaseType>> throws_) {
//    Option<List<Type>> throwsAsTypeList =
//    throws_.isSome() ?
//    Option.<List<Type>>some(new ArrayList<Type>(throws_.unwrap())) :
//    Option.<List<Type>>none();
//    return new ArrowType(span, domain, range, throwsAsTypeList);
//    }

    public static ArrowType makeArrowType(Span span, Type domain, Type range) {
        return new ArrowType(span, domain, range, makeEffect(range.getSpan().getEnd()));
    }

//    public static AbstractArrowType makeGenericArrowType(Span span,
//    List<StaticParam> staticParams,
//    Type domain,
//    Type range,
//    Option<List<BaseType>> throws_,
//    WhereClause where) {
//    if (staticParams.isEmpty() && where.getConstraints().isEmpty() && where.getBindings().isEmpty()) {
//    return makeArrowType(span, domain, range, throws_);
//    }
//    Option<List<Type>> throwsAsTypeList =
//    throws_.isSome() ?
//    Option.<List<Type>>some(new ArrayList<Type>(throws_.unwrap())) :
//    Option.<List<Type>>none();
//    return new _RewriteGenericArrowType(span, domain, range,
//    throwsAsTypeList, staticParams, where);
//    }

//    public static AbstractArrowType makeGenericArrowType(
//    Span span,
//    List<StaticParam> staticParams,
//    Type domain,
//    Type range) {
//    if (staticParams.isEmpty()) {
//    return makeArrowType(span, domain, range, Option.<List<BaseType>>none());
//    }
//    return new _RewriteGenericArrowType(span, domain, range,
//    Option.<List<Type>>none(), staticParams, new WhereClause());
//    }

    public static Type makeDomain(Span span, List<Type> elements,
                                    Option<Type> varargs,
                                    List<KeywordType> keywords) {
        if ( varargs.isNone() && keywords.isEmpty() ) {
            int size = elements.size();
            if ( size == 0 )
                return makeVoidType(span);
            else if ( size == 1 )
                return elements.get(0);
            else
                return new TupleType(span, elements);
        } else
            return new TupleType(span, elements, varargs, keywords);
    }

    /** Create an "empty" effect at the given location. */
    public static Effect makeEffect(SourceLoc loc) {
        return new Effect(new Span(loc, loc));
    }

    public static Effect makeEffect(List<BaseType> throwsClause) {
        return new Effect(FortressUtil.spanAll(throwsClause), Option.some(throwsClause));
    }

    public static Effect makeEffect(SourceLoc defaultLoc, List<BaseType> throwsClause) {
        return new Effect(FortressUtil.spanAll(defaultLoc, throwsClause),
                Option.some(throwsClause));
    }

    public static Effect makeEffect(Option<List<BaseType>> throwsClause) {
        Span span = FortressUtil.spanAll(throwsClause.unwrap(Collections.<BaseType>emptyList()));
        return new Effect(span, throwsClause);
    }

    public static Effect makeEffect(SourceLoc defaultLoc, Option<List<BaseType>> throwsClause) {
        Span span = FortressUtil.spanAll(defaultLoc,
                throwsClause.unwrap(Collections.<BaseType>emptyList()));
        return new Effect(span, throwsClause);
    }

    public static KeywordType makeKeywordType(Id name, Type type) {
        return new KeywordType(new Span(), name, type);
    }

    public static ConstructorFnName makeConstructorFnName(GenericWithParams def) {
        return new ConstructorFnName(def.getSpan(), def);
    }

    public static APIName makeAPIName(Span span, String s) {
        return new APIName(span, Useful.list(new Id(span, s)));
    }

    public static APIName makeAPIName(Span span, Id s) {
        return new APIName(span, Useful.list(s));
    }

    public static APIName makeAPIName(Id s) {
        return new APIName(s.getSpan(), Useful.list(s));
    }

    private static List<Id> stringToIds(String path) {
        List<Id> ids = new ArrayList<Id>();

        StringTokenizer st = new StringTokenizer(path, ".");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            ids.add(makeId(e));
        }
        ids = Useful.immutableTrimmedList(ids);
        return ids;
    }

    public static APIName makeAPIName(String s) {
        return makeAPIName(stringToIds(s));
    }

    public static APIName makeAPIName(Iterable<Id> ids) {
        return new APIName(FortressUtil.spanAll(ids), CollectUtil.makeList(ids));
    }

    public static APIName makeAPIName(Id id, Iterable<Id> ids) {
        return makeAPIName(CollectUtil.makeList(IterUtil.compose(id, ids)));
    }

    public static APIName makeAPIName(Span span, Iterable<Id> ids) {
        return new APIName(span, CollectUtil.makeList(ids));
    }

    /**
     * Create a APIName from the name of the file with the given path.
     */
    /*
 public static APIName makeAPIName(Span span, String apiname, String delimiter) {
   List<Id> ids = new ArrayList<Id>();

   for (String n : path.split(delimiter)) {
    ids.add(new Id(span, n));
   }
   return new APIName(span, ids);

 }
     */

    public static APIName makeAPINameFromPath(Span span, String path, String delimiter) {
        List<Id> ids = new ArrayList<Id>();
        String file = new File(path).getName();
        if (file.length() <= 4) {
            return error(new Id(span, "_"), "Invalid file name.");
        }
        for (String n : file.substring(0, file.length()-4).split(delimiter)) {
            ids.add(new Id(span, n));
        }
        ids = Useful.immutableTrimmedList(ids);
        return new APIName(span, ids);
    }

    public static Id bogusId(Span span) {
        return new Id(span, Option.<APIName>none(), "_");
    }

    public static Id makeId(Id id, String newName) {
        return new Id(id.getSpan(), id.getApiName(), newName);
    }

    public static Id makeId(Span span, String s) {
        return new Id(span, Option.<APIName>none(), s);
    }

    public static Id makeId(Span span, Id id) {
        return new Id(span, id.getApiName(), id.getText());
    }

    public static Id makeId(Iterable<Id> apiIds, Id id) {
        Span span;
        Option<APIName> api;
        if (IterUtil.isEmpty(apiIds)) {
            span = id.getSpan();
            api = Option.none();
        }
        else {
            APIName n = makeAPIName(apiIds);
            span = FortressUtil.spanTwo(n, id);
            api = Option.some(n);
        }
        return new Id(span, api, id.getText());
    }

    public static Id makeId(Span span, String api, String name) {
        List<Id> apis = new ArrayList<Id>();
        apis.add(makeId(span, api));
        apis = Useful.immutableTrimmedList(apis);
        return new Id(span, Option.some(new APIName(span, apis)), name);
    }

    public static Id makeId(Span span, Iterable<Id> apiIds, Id id) {
        Option<APIName> api;
        if (IterUtil.isEmpty(apiIds)) { api = Option.none(); }
        else { api = Option.some(makeAPIName(apiIds)); }
        return new Id(span, api, id.getText());
    }

    public static Id makeId(Span span, Id id, Iterable<Id> ids) {
        Option<APIName> api;
        Id last;
        if (IterUtil.isEmpty(ids)) { api = Option.none(); last = id; }
        else { api = Option.some(makeAPIName(id, IterUtil.skipLast(ids)));
        last = IterUtil.last(ids);
        }
        return new Id(span, api, last.getText());
    }

    public static Id makeId(Span span, APIName api, Id id) {
        return new Id(span, Option.some(api), id.getText());
    }

    /** Assumes {@code ids} is nonempty. */
    public static Id makeId(Iterable<Id> ids) {
        return makeId(IterUtil.skipLast(ids), IterUtil.last(ids));
    }

    public static Id makeId(String nameFirst, String... nameRest) {
        Iterable<Id> ids = IterUtil.compose(makeId(nameFirst),
                IterUtil.map(IterUtil.asIterable(nameRest), STRING_TO_ID));
        return makeId(ids);
    }

    public static Id makeId(APIName api, Id name) {
        return new Id(FortressUtil.spanTwo(api, name), Option.some(api),
                name.getText());
    }

    public static Id makeId(APIName api, Id name, Span span) {
        return new Id(span, Option.some(api), name.getText());
    }

    public static Id makeId(Option<APIName> api, Id name) {
        return new Id(name.getSpan(), api, name.getText());
    }

    public static Id makeId(Span span, APIName api, String name) {
        return new Id(span, Option.some(api), name);
    }

    public static Id makeId(String string) {
        return new Id(new Span(), string);
    }

    public static final Lambda<String, Id> STRING_TO_ID = new Lambda<String, Id>() {
        public Id value(String arg) { return makeId(arg); }
    };

    public static VarType makeVarType(String string) {
        return makeVarType(new Span(), makeId(string));
    }

    public static VarType makeVarType(Span span, Id id) {
        return new VarType(span, id);
    }

    public static LValue makeLValue(Id id) {
        return new LValue(id.getSpan(), id);
    }

    public static LValue makeLValue(String name, String type) {
        return makeLValue(name, makeVarType(type));
    }

    public static LValue makeLValue(Id name, Id type) {
        return new LValue(new Span(name.getSpan(), type.getSpan()),
                          name,
                          Collections.<Modifier>emptyList(),
                          Option.some((Type)makeVarType(type.getSpan(),type)),
                          false);
    }

    public static LValue makeLValue(Id name, Type type) {
        return new LValue(new Span(name.getSpan(), type.getSpan()),
                          name,
                          Collections.<Modifier>emptyList(),
                          Option.some(type),
                          false);
    }

    public static LValue makeLValue(Id name, Option<Type> type) {
        return new LValue(name.getSpan(),
                          name,
                          Collections.<Modifier>emptyList(),
                          type,
                          false);
    }

    public static LValue makeLValue(String name, Type type) {
        return new LValue(type.getSpan(), makeId(name),
                          Collections.<Modifier>emptyList(), Option.some(type), false);
    }

    public static LValue makeLValue(String name, Type type, List<Modifier> mods) {
        LValue result = makeLValue(name, type);
        return makeLValue(result, mods);
    }

    public static LValue makeLValue(LValue lvb, Id name) {
        return new LValue(lvb.getSpan(), name, lvb.getMods(), lvb.getIdType(),
                          lvb.isMutable());
    }

    public static LValue makeLValue(LValue lvb, boolean mutable) {
        return new LValue(lvb.getSpan(), lvb.getName(), lvb.getMods(), lvb.getIdType(),
                          mutable);
    }

    public static LValue makeLValue(LValue lvb, List<Modifier> mods) {
        boolean mutable = lvb.isMutable();
        for (Modifier m : mods) {
            if (m instanceof ModifierVar || m instanceof ModifierSettable)
                mutable = true;
        }
        return new LValue(lvb.getSpan(), lvb.getName(), mods, lvb.getIdType(), mutable);
    }

    public static LValue makeLValue(LValue lvb, List<Modifier> mods,
            boolean mutable) {
        return new LValue(lvb.getSpan(), lvb.getName(), mods, lvb.getIdType(), mutable);
    }

    public static LValue makeLValue(LValue lvb, Type ty) {
        return new LValue(lvb.getSpan(), lvb.getName(), lvb.getMods(),
                          Option.some(ty), lvb.isMutable());
    }

    public static LValue makeLValue(LValue lvb, Type ty,
            boolean mutable) {
        return new LValue(lvb.getSpan(), lvb.getName(), lvb.getMods(),
                          Option.some(ty), mutable);
    }

    public static LValue makeLValue(LValue lvb, Type ty,
            List<Modifier> mods) {
        boolean mutable = lvb.isMutable();
        for (Modifier m : mods) {
            if (m instanceof ModifierVar || m instanceof ModifierSettable)
                mutable = true;
        }
        return new LValue(lvb.getSpan(), lvb.getName(), mods,
                          Option.some(ty), mutable);
    }

    public static LValue makeLValue(Param param) {
        return new LValue(param.getSpan(), param.getName(),
                          param.getMods(), param.getIdType(), false);
    }

    public static MatrixType makeMatrixType(Span span, Type element,
                                            ExtentRange dimension) {
        List<ExtentRange> dims = new ArrayList<ExtentRange>();
        dims.add(dimension);
        dims = Useful.immutableTrimmedList(dims);
        return new MatrixType(span, element, dims);
    }

    public static MatrixType makeMatrixType(Span span, Type element,
                                            ExtentRange dimension,
                                            List<ExtentRange> dimensions) {
        List<ExtentRange> dims = new ArrayList<ExtentRange>();
        dims.add(dimension);
        dims.addAll(dimensions);
        dims = Useful.immutableTrimmedList(dims);
        return new MatrixType(span, element, dims);
    }

    public static Op makeEnclosing(Span in_span, String in_open, String in_close) {
        return new Op(in_span, PrecedenceMap.ONLY.canon(in_open + " " + in_close),
                      enclosing, true);
    }

    // All of these should go away, except for the gross overhead of allocating separate items.
    private static Fixity infix = new InFixity();
    private static Fixity prefix = new PreFixity();
    private static Fixity postfix = new PostFixity();
    private static Fixity nofix = new NoFixity();
    private static Fixity multifix = new MultiFixity();
    private static Fixity enclosing = new EnclosingFixity();
    private static Fixity big = new BigFixity();
    private static Fixity unknownFix = new UnknownFixity();

    public static Op makeOp(String name) {
        return new Op(new Span(), PrecedenceMap.ONLY.canon(name), unknownFix, false);
    }

    public static Op makeOp(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), unknownFix, false);
    }

    public static Op makeOp(Span span, String name, Fixity fixity) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), fixity, false);
    }

    public static Op makeOp(Op op, String name) {
        return new Op(op.getSpan(), PrecedenceMap.ONLY.canon(name),
                      op.getFixity(), op.isEnclosing());
    }

    public static Op makeOpInfix(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), infix, false);
    }

    public static Op makeOpInfix(Span span, String apiName, String name) {
        Op op =  new Op(span, Option.some(NodeFactory.makeAPIName(apiName)),
                        PrecedenceMap.ONLY.canon(name), infix, false);
        return op;

    }

    public static Op makeOpInfix(Op op) {
        return new Op(op.getSpan(), op.getApiName(), op.getText(), infix, op.isEnclosing());
    }

    public static Op makeOpPrefix(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), prefix, false);
    }

    public static Op makeOpPrefix(Op op) {
        return new Op(op.getSpan(), op.getApiName(), op.getText(), prefix, op.isEnclosing());
    }

    public static Op makeOpPostfix(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), postfix, false);
    }

    public static Op makeOpPostfix(Op op) {
        return new Op(op.getSpan(), op.getApiName(), op.getText(), postfix, op.isEnclosing());
    }

    /**
     * Rewrites the given Op with the given api. Dispatches on the
     * type of op, so that the same subtype of Op is created.
     */
    public static Op makeOp(final APIName api, Op op) {
        return new Op(op.getSpan(), Option.some(api),
                      op.getText(), op.getFixity(), op.isEnclosing() );
    }

    public static Op makeOpNofix(Op op) {
        return new Op(op.getSpan(), op.getText(), nofix, op.isEnclosing());
    }

    public static Op makeOpMultifix(Op op) {
        return new Op(op.getSpan(), op.getText(), multifix, op.isEnclosing());
    }

    public static Op makeOpEnclosing(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), enclosing, false);
    }

    public static Op makeOpBig(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), big, false);
    }

    public static Op makeOpUnknown(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), unknownFix, false);
    }

    public static Op makeBig(Op op) {
        if ( op.isEnclosing() ) {
            String _op = op.getText();
            String left  = _op.split(" ")[0];
            String right = "BIG " + _op.substring(left.length()+1);
            left  = "BIG " + left;
            return makeEnclosing(op.getSpan(), left, right);
        } else
            return new Op(op.getSpan(), PrecedenceMap.ONLY.canon("BIG " + op.getText()), big, op.isEnclosing() );
    }

    public static Param makeVarargsParam(Id name, Type type) {
        return new Param(name.getSpan(), name, Collections.<Modifier>emptyList(),
                         Option.<Type>none(), Option.<Expr>none(), Option.<Type>some(type));
    }

    public static Param makeVarargsParam(Param param, List<Modifier> mods) {
        return new Param(param.getSpan(), param.getName(), mods,
                         Option.<Type>none(), Option.<Expr>none(),
                         param.getVarargsType());
    }

    public static Param makeVarargsParam(Span span, List<Modifier> mods,
                                         Id name, Type type) {
        return new Param(span, name, mods,
                         Option.<Type>none(), Option.<Expr>none(),
                         Option.<Type>some(type));
    }

    public static Param makeAbsParam(Type type) {
        Id id = new Id(type.getSpan(), "_");
        return new Param(type.getSpan(), id, Collections.<Modifier>emptyList(),
                         Option.some(type), Option.<Expr>none());
    }

    public static Param makeParam(Span span, List<Modifier> mods, Id name,
                                  Type type) {
        return new Param(span, name, mods, Option.some(type), Option.<Expr>none());
    }

    public static Param makeParam(Span span, List<Modifier> mods, Id name,
                                  Option<Type> type) {
        return new Param(span, name, mods, type, Option.<Expr>none());
    }

    public static Param makeParam(Id id, Type type) {
        return new Param(id.getSpan(), id, Collections.<Modifier>emptyList(),
                         Option.some(type), Option.<Expr>none());
    }

    public static Param makeParam(Id name) {
        return new Param(name.getSpan(), name, Collections.<Modifier>emptyList(),
                         Option.<Type>none(), Option.<Expr>none());
    }

    public static Param makeParam(Param param, Expr expr) {
        return new Param(param.getSpan(), param.getName(), param.getMods(),
                         param.getIdType(), Option.some(expr));
    }

    public static Param makeParam(Param param, List<Modifier> mods) {
        return new Param(param.getSpan(), param.getName(), mods,
                         param.getIdType(), param.getDefaultExpr());
    }

    public static Param makeParam(Param param, Id newId) {
        return new Param(param.getSpan(), newId, param.getMods(),
                         param.getIdType(), param.getDefaultExpr());
    }

    public static StaticParam makeTypeParam(String name) {
        Span s = new Span();
        return new StaticParam(s, new Id(s, name),
                               Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindType());
    }

    public static StaticParam makeTypeParam(String name, String sup) {
        Span s = new Span();
        List<BaseType> supers = new ArrayList<BaseType>(1);
        supers.add(makeVarType(sup));
        return new StaticParam(s, new Id(s, name), supers,
                               Option.<Type>none(), false,
                               new KindType());
    }

    public static StaticParam makeOpParam(String name) {
        return new StaticParam(new Span(), makeOp(name),
                               Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindOp());
    }

    public static StaticParam makeBoolParam(String name) {
        Span s = new Span();
        return new StaticParam(s, new Id(s, name),
                               Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindBool());
    }

    public static StaticParam makeDimParam(String name) {
        Span s = new Span();
        return new StaticParam(s, new Id(s, name),
                               Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindDim());
    }

    public static StaticParam makeUnitParam(String name) {
        Span s = new Span();
        return new StaticParam(s, new Id(s, name),
                               Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindUnit());
    }

    public static StaticParam makeIntParam(String name) {
        Span s = new Span();
        return new StaticParam(s, new Id(s, name),
                               Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindInt());
    }

    public static StaticParam makeNatParam(String name) {
        Span s = new Span();
        return new StaticParam(s, new Id(s, name),
                               Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindNat());
    }

    public static Param makeParam(Span span, Id name, Option<Type> type) {
        return new Param(span, name, type);
    }

    public static TupleType makeTupleType(List<Type> elements) {
        return new TupleType(new Span(), elements);
    }

    public static TupleType makeTupleType(Span span, List<Type> elements) {
        return new TupleType(span, elements);
    }

    public static TupleType makeVoidType(Span span) {
        return new TupleType(span, false, Collections.<Type>emptyList(),
                             Option.<Type>none(),
                             Collections.<KeywordType>emptyList());
    }

    public static TypeArg makeTypeArg(Type ty) {
        return new TypeArg(ty.getSpan(), ty);
    }

    public static TypeArg makeTypeArg(Span span, String string) {
        return new TypeArg(span, new VarType(span, makeId(span, string)));
    }

    public static TypeArg makeTypeArg(String string) {
        Span span = new Span();
        return new TypeArg(span,
                new VarType(span, makeId(span, string)));
    }

    public static BoolRef makeBoolRef(String string) {
        return new BoolRef(new Span(), makeId(string));
    }

    public static BoolArg makeBoolArg(String string) {
        return new BoolArg(new Span(), makeBoolRef(string));
    }

    public static IntRef makeIntRef(String string) {
        return new IntRef(new Span(), makeId(string));
    }

    public static IntExpr makeIntVal(String i) {
        Span span = new Span();
        return new IntBase(span, new IntLiteralExpr(span,
                new BigInteger(i)));
    }

    public static IntArg makeIntArg(String string) {
        return new IntArg(new Span(), makeIntRef(string));
    }

    public static IntArg makeIntArgVal(String i) {
        return new IntArg(new Span(), makeIntVal(i));
    }

    public static OpArg makeOpArg(String string) {
        return new OpArg(new Span(), ExprFactory.makeOpRef(makeOp(string)));
    }

    public static VarDecl makeVarDecl(Span span, List<LValue> lvals) {
        FortressUtil.validId(lvals);
        return new VarDecl(span, lvals, Option.<Expr>none());
    }

    public static VarDecl makeVarDecl(Span span, List<LValue> lvals, Expr init) {
        FortressUtil.validId(lvals);
        return new VarDecl(span, lvals, Option.<Expr>some(init));
    }

    public static VarDecl makeVarDecl(Span span, List<LValue> lvals, Option<Expr> init) {
        FortressUtil.validId(lvals);
        return new VarDecl(span, lvals, init);
    }

    public static VarDecl makeVarDecl(Span span, Id name, Expr init) {
        FortressUtil.validId(name);
        LValue bind = new LValue(span, name, Collections.<Modifier>emptyList(),
                                 Option.<Type>none(), true);
        return new VarDecl(span, Useful.<LValue>list(bind), Option.<Expr>some(init));
    }

    public static VarDecl makeVarDecl(Span span, String name, Expr init) {
        Id id = new Id(span, name);
        FortressUtil.validId(id);
        LValue bind = new LValue(span, id,
                                 Collections.<Modifier>emptyList(),
                                 Option.<Type>none(), false);
        return new VarDecl(span, Useful.<LValue>list(bind), Option.<Expr>some(init));
    }

    public static BoolExpr makeInParentheses(BoolExpr be) {
        return be.accept(new NodeAbstractVisitor<BoolExpr>() {
            public BoolExpr forBoolBase(BoolBase b) {
                return new BoolBase(b.getSpan(), true, b.isBoolVal());
            }
            public BoolExpr forBoolRef(BoolRef b) {
                return new BoolRef(b.getSpan(), true, b.getName());
            }
            public BoolExpr forBoolUnaryOp(BoolUnaryOp b) {
                return new BoolUnaryOp(b.getSpan(), true, b.getBoolVal(), b.getOp());
            }
            public BoolExpr forBoolBinaryOp(BoolBinaryOp b) {
                return new BoolBinaryOp(b.getSpan(), true,
                                        b.getLeft(), b.getRight(), b.getOp());
            }
            public BoolExpr defaultCase(Node x) {
                return bug(x, "makeInParentheses: " + x.getClass() +
                        " is not a subtype of BoolExpr.");
            }
        });
    }

    public static DimExpr makeInParentheses(DimExpr dim) {
        return dim.accept(new NodeAbstractVisitor<DimExpr>() {
            public DimExpr forDimBase(DimBase t) {
                return new DimBase(t.getSpan(), true);
            }
            public DimExpr forDimRef(DimRef t) {
                return new DimRef(t.getSpan(), true, t.getName());
            }
            public DimExpr forDimBinaryOp(DimBinaryOp t) {
                return new DimBinaryOp(t.getSpan(), true, t.getLeft(),
                                       t.getRight(), t.getOp());
            }
            public DimExpr forDimExponent(DimExponent t) {
                return new DimExponent(t.getSpan(), true, t.getBase(),
                        t.getPower());
            }
            public DimExpr forDimUnaryOp(DimUnaryOp t) {
                return new DimUnaryOp(t.getSpan(), true, t.getDimVal(), t.getOp());
            }
            public DimExpr defaultCase(Node x) {
                return bug(x, "makeInParentheses: " + x.getClass() +
                " is not a subtype of DimExpr.");
            }
        });
    }

    public static IntExpr makeInParentheses(IntExpr ie) {
        return ie.accept(new NodeAbstractVisitor<IntExpr>() {
            public IntExpr forIntBase(IntBase i) {
                return new IntBase(i.getSpan(), true, i.getIntVal());
            }
            public IntExpr forIntRef(IntRef i) {
                return new IntRef(i.getSpan(), true, i.getName());
            }
            public IntExpr forIntBinaryOp(IntBinaryOp i) {
                return new IntBinaryOp(i.getSpan(), true, i.getLeft(),
                                       i.getRight(), i.getOp());
            }
            public IntExpr defaultCase(Node x) {
                return bug(x, "makeInParentheses: " + x.getClass() +
                " is not a subtype of IntExpr.");
            }
        });
    }

    public static UnitExpr makeInParentheses(UnitExpr be) {
        return be.accept(new NodeAbstractVisitor<UnitExpr>() {
            public UnitExpr forUnitRef(UnitRef b) {
                return new UnitRef(b.getSpan(), true, b.getName());
            }
            public UnitExpr forUnitBinaryOp(UnitBinaryOp i) {
                return new UnitBinaryOp(i.getSpan(), true, i.getLeft(),
                                        i.getRight(), i.getOp());
            }
            public UnitExpr defaultCase(Node x) {
                return bug(x, "makeInParentheses: " + x.getClass() +
                " is not a subtype of UnitExpr.");
            }
        });
    }

    public static Type makeInParentheses(Type ty) {
        return ty.accept(new NodeAbstractVisitor<Type>() {
            public Type forArrowType(ArrowType t) {
                return new ArrowType(t.getSpan(), true, t.getDomain(),
                        t.getRange(), t.getEffect());
            }
            public Type forArrayType(ArrayType t) {
                return new ArrayType(t.getSpan(), true, t.getElemType(),
                        t.getIndices());
            }
            public Type forVarType(VarType t) {
                return new VarType(t.getSpan(), true, t.getName());
            }
            public Type forMatrixType(MatrixType t) {
                return new MatrixType(t.getSpan(), true, t.getElemType(),
                        t.getDimensions());
            }
            public Type forTraitType(TraitType t) {
                return new TraitType(t.getSpan(), true, t.getName(),
                                     t.getArgs(), t.getStaticParams());
            }
            public Type forTupleType(TupleType t) {
                return new TupleType(t.getSpan(), true, t.getElements(),
                                     t.getVarargs(), t.getKeywords());
            }
            public Type forTaggedDimType(TaggedDimType t) {
                return new TaggedDimType(t.getSpan(), true, t.getElemType(),
                                         t.getDimExpr(), t.getUnitExpr());
            }
            public Type forTaggedUnitType(TaggedUnitType t) {
                return new TaggedUnitType(t.getSpan(), true, t.getElemType(),
                                          t.getUnitExpr());
            }
            public Type forDimExpr(DimExpr t) {
                return makeInParentheses(t);
            }
            public Type defaultCase(Node x) {
                return bug(x, "makeInParentheses: " + x.getClass() +
                        " is not a subtype of Type.");
            }
        });
    }

    public static SyntaxDef makeSyntaxDef(Span s, Option<String> modifier,
                                          List<SyntaxSymbol> syntaxSymbols,
                                          TransformerDecl transformation) {
        return new SyntaxDef(s, modifier, syntaxSymbols, transformation);
    }

    public static SuperSyntaxDef makeSuperSyntaxDef(Span s, Option<String> modifier,
                                                    Id nonterminal, Id grammar) {
        return new SuperSyntaxDef(s, modifier, nonterminal, grammar);
    }

    public static Import makeImportStar(APIName api, List<IdOrOpOrAnonymousName> excepts) {
        return new ImportStar(makeSpan(api, excepts), Option.<String>none(), api, excepts);
    }

    public static BoolRef makeBoolRef(BoolRef old, int depth) {
        return new BoolRef(old.getSpan(), old.isParenthesized(), old.getName(), depth);
    }


    public static IntRef makeIntRef(IntRef old, int depth) {
        return new IntRef(old.getSpan(), old.isParenthesized(), old.getName(), depth);
    }

    public static Op makeListOp(Span span) {
        return makeEnclosing(span, "<|", "|>");
    }

    public static NamedType makeNamedType(APIName api, NamedType type) {
        if (type instanceof VarType) {
            return new VarType(type.getSpan(),
                    type.isParenthesized(),
                    makeId(api, type.getName()));
        }
        else { // type instanceof TraitType
            TraitType _type = (TraitType)type;
            return new TraitType(_type.getSpan(),
                                 _type.isParenthesized(),
                                 makeId(api, _type.getName()),
                                 _type.getArgs(),
                                 _type.getStaticParams());
        }
    }

    public static TraitType makeGenericSingletonType(Id name, List<StaticParam> params) {
        return new TraitType(name.getSpan(), name, Collections.<StaticArg>emptyList(), params);
    }

    public static VarType makeVarType(VarType original, int lexicalNestedness) {
        return new VarType(original.getSpan(), original.isParenthesized(), original.getName(), lexicalNestedness);
    }

    public static TraitType makeTraitType(TraitType original) {
        return new TraitType(original.getSpan(), original.isParenthesized(),
                             original.getName(), original.getArgs(),
                             Collections.<StaticParam>emptyList());

    }

    public static Import makeImportStar(String apiName) {
        return NodeFactory.makeImportStar(NodeFactory.makeAPIName(apiName), new LinkedList<IdOrOpOrAnonymousName>());
    }

}
