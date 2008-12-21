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
    public static int lexicalDepth = -2147483648;

    public static Span internalSpan = makeSpan("Compiler internal generated.");
    public static Span parserSpan = makeSpan("Parser generated.");
    public static Span macroSpan = makeSpan("Syntactic abstraction generated.");
    public static Span typeSpan = makeSpan("Type checker generated.");
    public static Span desugarerSpan = makeSpan("Desugarer generated.");
    public static Span interpreterSpan = makeSpan("Interpreter generated.");
    public static Span shellSpan = makeSpan("Shell generated.");
    public static Span unprinterSpan = makeSpan("Unprinter generated.");
    public static Span testSpan = makeSpan("Test generated.");

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
        return NodeUtil.getSpan(node);
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
        return makeSpan(NodeUtil.getSpan(start), NodeUtil.getSpan(finish));
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

    public static ASTNodeInfo makeASTNodeInfo(Span span) {
        return new SpanInfo(span);
    }

    public static SpanInfo makeSpanInfo(Span span) { return new SpanInfo(span); }

    public static SpanInfo makeSpanInfo(SpanInfo info, Span span) {
        return new SpanInfo(span);
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
        return new Component(makeSpanInfo(span), name, imports, decls, isNative, exports);
    }

    public static Api makeApi(Span span, APIName name,
                              List<Import> imports,
                              List<Decl> decls) {
        return new Api(makeSpanInfo(span), name, imports, decls);
    }

    public static ImportApi makeImportApi(Span span, Option<String> foreign,
                                          List<AliasedAPIName> names) {
        return new ImportApi(makeSpanInfo(span), foreign, names);
    }

    public static ImportStar makeImportStar(Span span, Option<String> foreign,
                                            APIName api,
                                            List<IdOrOpOrAnonymousName> names) {
        return new ImportStar(makeSpanInfo(span), foreign, api, names);
    }

    public static ImportNames makeImportNames(Span span, Option<String> foreign,
                                              APIName api,
                                              List<AliasedSimpleName> names) {
        return new ImportNames(makeSpanInfo(span), foreign, api, names);
    }

    public static AliasedSimpleName makeAliasedSimpleName(IdOrOpOrAnonymousName name) {
        return makeAliasedSimpleName(NodeUtil.getSpan(name), name,
                                     Option.<IdOrOpOrAnonymousName>none());
    }

    public static AliasedSimpleName makeAliasedSimpleName(IdOrOpOrAnonymousName name,
                                                          IdOrOpOrAnonymousName alias) {
        return makeAliasedSimpleName(FortressUtil.spanTwo(name, alias), name,
                                     Option.<IdOrOpOrAnonymousName>some(alias));
    }

    public static AliasedSimpleName makeAliasedSimpleName(Span span,
                                                          IdOrOpOrAnonymousName name,
                                                          Option<IdOrOpOrAnonymousName> alias) {
        return new AliasedSimpleName(makeSpanInfo(span), name, alias);
    }

    public static AliasedAPIName makeAliasedAPIName(APIName api) {
        return makeAliasedAPIName(NodeUtil.getSpan(api), api, Option.<Id>none());
    }

    public static AliasedAPIName makeAliasedAPIName(APIName api, Id alias) {
        return makeAliasedAPIName(FortressUtil.spanTwo(api, alias), api,
                                  Option.<Id>some(alias));
    }

    public static AliasedAPIName makeAliasedAPIName(Span span, APIName api,
                                                    Option<Id> alias) {
        return new AliasedAPIName(makeSpanInfo(span), api, alias);
    }

    public static TraitDecl makeTraitDecl(Span span, Id name,
                                          List<StaticParam> sparams,
                                          List<TraitTypeWhere> extendsC) {
        return makeTraitDecl(span, Modifiers.None, name,
                             sparams, extendsC, Option.<WhereClause>none(),
                             Collections.<Decl>emptyList(),
                             Collections.<BaseType>emptyList(),
                             Option.<List<BaseType>>none());
    }

    public static TraitDecl makeTraitDecl(Span span, Modifiers mods, Id name,
                                          List<StaticParam> sparams,
                                          List<TraitTypeWhere> extendsC,
                                          Option<WhereClause> whereC,
                                          List<Decl> decls,
                                          List<BaseType> excludesC,
                                          Option<List<BaseType>> comprisesC) {
        TraitTypeHeader header = makeTraitTypeHeader(mods, name, sparams, whereC,
                                                     Option.<List<BaseType>>none(),
                                                     Option.<Contract>none(),
                                                     extendsC, decls);
        return new TraitDecl(makeSpanInfo(span), header, excludesC, comprisesC);
    }

    public static ObjectDecl makeObjectDecl(Span span, Id name,
                                            List<TraitTypeWhere> extendsC,
                                            List<Decl> decls) {
        return makeObjectDecl(span, Modifiers.None, name,
                              Collections.<StaticParam>emptyList(),
                              extendsC, Option.<WhereClause>none(), decls,
                              Option.<List<Param>>none(),
                              Option.<List<BaseType>>none(),
                              Option.<Contract>none());
    }

    public static ObjectDecl makeObjectDecl(Span span, Id name,
                                            Option<List<Param>> params) {
        return makeObjectDecl(span, Modifiers.None, name,
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
        return makeObjectDecl(span, Modifiers.None, name,
                              sparams, extendsC,
                              Option.<WhereClause>none(), decls,
                              params,
                              Option.<List<BaseType>>none(),
                              Option.<Contract>none());
    }

    public static ObjectDecl makeObjectDecl(Span span, Modifiers mods, Id name,
                                            List<StaticParam> sparams,
                                            List<TraitTypeWhere> extendsC,
                                            Option<WhereClause> whereC,
                                            List<Decl> decls,
                                            Option<List<Param>> params,
                                            Option<List<BaseType>> throwsC,
                                            Option<Contract> contract) {
        TraitTypeHeader header = makeTraitTypeHeader(mods, name, sparams, whereC,
                                                     throwsC, contract, extendsC,
                                                     decls);
        return new ObjectDecl(makeSpanInfo(span), header, params);
    }

    public static FnDecl makeFnDecl(Span span, Modifiers mods,
                                    IdOrOpOrAnonymousName name,
                                    List<StaticParam> staticParams,
                                    List<Param> params,
                                    Option<Type> returnType) {
        return makeFnDecl(span, mods, name, staticParams, params, returnType,
                          Option.<List<BaseType>>none(),
                          Option.<WhereClause>none(),
                          Option.<Contract>none());
    }

    public static FnDecl makeFnDecl(Span span, Modifiers mods,
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

    public static FnDecl makeFnDecl(Span span, Modifiers mods,
                                    IdOrOpOrAnonymousName name,
                                    List<StaticParam> staticParams,
                                    List<Param> params,
                                    Option<Type> returnType,
                                    Option<List<BaseType>> throwsC,
                                    Option<WhereClause> whereC,
                                    Option<Contract> contract) {
        return makeFnDecl(span, mods, name, staticParams, params, returnType,
                          throwsC, whereC, contract,
                          makeId(span, "FN$"+span.toString()),
                          Option.<Expr>none(), Option.<Id>none());
    }

    public static FnDecl makeFnDecl(Span span, Modifiers mods,
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
                          makeId(span, "FN$"+span.toString()),
                          body, Option.<Id>none());
    }

    public static FnHeader makeFnHeader(Modifiers mods,
                                        IdOrOpOrAnonymousName name,
                                        List<StaticParam> staticParams,
                                        Option<WhereClause> whereClause,
                                        Option<List<BaseType>> throwsClause,
                                        Option<Contract> contract,
                                        List<Param> params,
                                        Option<Type> returnType) {
        return new FnHeader(staticParams, mods, name, whereClause,
                            throwsClause, contract, params, returnType);
    }

    public static FnDecl makeFnDecl(Span span, Modifiers mods,
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
        FnHeader header = makeFnHeader(mods, name, staticParams, whereC, throwsC,
                                       contract, params, returnType);
        return new FnDecl(makeSpanInfo(span), header, unambiguousName, body, implementsUnambiguousName);
    }

    public static DimDecl makeDimDecl(Span span, Id dim, Option<Type> derived) {
        return makeDimDecl(span, dim, derived, Option.<Id>none());
    }

    public static DimDecl makeDimDecl(Span span, Id dim, Option<Type> derived,
                                      Option<Id> defaultId) {
        return new DimDecl(makeSpanInfo(span), dim, derived, defaultId);
    }

    public static UnitDecl makeUnitDecl(Span span, boolean si_unit, List<Id> units,
                                        Option<Type> dim, Option<Expr> def) {
        return new UnitDecl(makeSpanInfo(span), si_unit, units, dim, def);
    }

    public static LValue makeLValue(Span span, Id name, Option<Type> type) {
        return makeLValue(span, name,
                          Modifiers.None,
                          type, false);
    }

    public static LValue makeLValue(Span span, Id id, Type ty) {
        return makeLValue(span, id, Option.<Type>some(ty));
    }

    public static LValue makeLValue(Span span, Id id) {
        return makeLValue(span, id, Option.<Type>none());
    }

    public static LValue makeLValue(Id id) {
        return makeLValue(NodeUtil.getSpan(id), id);
    }

    public static LValue makeLValue(Id name, Id type) {
        return makeLValue(FortressUtil.spanTwo(name, type),
                          name,
                          Modifiers.None,
                          Option.some((Type)makeVarType(NodeUtil.getSpan(type),type)),
                          false);
    }

    public static LValue makeLValue(Id id, Type ty) {
        return makeLValue(id, ty, Modifiers.None);
    }

    public static LValue makeLValue(Id id, Type ty,
                                    Modifiers mods) {
        return makeLValue(NodeUtil.getSpan(id), id, mods, Option.<Type>some(ty),
                          mods.isMutable());
    }

    public static LValue makeLValue(String name, Type type) {
        Span span = NodeUtil.getSpan(type);
        return makeLValue(span, makeId(span, name),
                          Modifiers.None, Option.some(type), false);
    }

    public static LValue makeLValue(String name, Type type, Modifiers mods) {
        Span span = NodeUtil.getSpan(type);
        return makeLValue(span, makeId(span, name), mods, Option.some(type), false);
    }

    public static LValue makeLValue(Span span, String name, String type) {
        return makeLValue(makeId(span, name), makeVarType(span, type));
    }

    public static LValue makeLValue(LValue lvb, Modifiers mods,
                                    boolean mutable) {
        return makeLValue(NodeUtil.getSpan(lvb), lvb.getName(), mods, lvb.getIdType(), mutable);
    }

    public static LValue makeLValue(LValue lvb, Id name) {
        return makeLValue(NodeUtil.getSpan(lvb), name, lvb.getMods(), lvb.getIdType(),
                          lvb.isMutable());
    }

    public static LValue makeLValue(LValue lvb, boolean mutable) {
        return makeLValue(NodeUtil.getSpan(lvb), lvb.getName(), lvb.getMods(), lvb.getIdType(),
                          mutable);
    }

    public static LValue makeLValue(LValue lvb, Modifiers mods) {
        boolean mutable = lvb.isMutable() || mods.isMutable();
        return makeLValue(NodeUtil.getSpan(lvb), lvb.getName(), mods, lvb.getIdType(), mutable);
    }

    public static LValue makeLValue(LValue lvb, Type ty) {
        return makeLValue(NodeUtil.getSpan(lvb), lvb.getName(), lvb.getMods(),
                          Option.<Type>some(ty), lvb.isMutable());
    }

    public static LValue makeLValue(LValue lvb, Type ty,
                                    boolean mutable) {
        return makeLValue(NodeUtil.getSpan(lvb), lvb.getName(), lvb.getMods(),
                          Option.<Type>some(ty), mutable);
    }

    public static LValue makeLValue(LValue lvb, Type ty,
                                    Modifiers mods) {
        boolean mutable = lvb.isMutable() || mods.isMutable();
        return makeLValue(NodeUtil.getSpan(lvb), lvb.getName(), mods,
                          Option.<Type>some(ty), mutable);
    }

    public static LValue makeLValue(Span span, Id name, Modifiers mods,
                                    Option<Type> type, boolean mutable) {
        return new LValue(makeSpanInfo(span), name, mods, type, mutable);
    }

    public static Param makeParam(Span span, Modifiers mods, Id name,
                                  Type type) {
        return makeParam(span, mods, name, Option.<Type>some(type));
    }

    public static Param makeParam(Span span, Modifiers mods, Id name,
                                  Option<Type> type) {
        return makeParam(span, mods, name, type,
                         Option.<Expr>none(), Option.<Type>none());
    }

    public static Param makeParam(Id name) {
        return makeParam(name, Option.<Type>none());
    }

    public static Param makeParam(Id id, Type type) {
        return makeParam(NodeUtil.getSpan(id), Modifiers.None, id,
                         Option.<Type>some(type));
    }

    public static Param makeParam(Id id, Option<Type> type) {
        return makeParam(NodeUtil.getSpan(id), Modifiers.None, id, type);
    }

    public static Param makeParam(Span span, Id id, Option<Type> type) {
        return makeParam(span, Modifiers.None, id, type);
    }

    public static Param makeParam(Param param, Expr expr) {
        return makeParam(NodeUtil.getSpan(param), param.getMods(), param.getName(),
                         param.getIdType(), Option.<Expr>some(expr),
                         param.getVarargsType());
    }

    public static Param makeParam(Param param, Modifiers mods) {
        return makeParam(NodeUtil.getSpan(param), mods, param.getName(),
                         param.getIdType(), param.getDefaultExpr(),
                         param.getVarargsType());
    }

    public static Param makeParam(Param param, Id newId) {
        return makeParam(NodeUtil.getSpan(param), param.getMods(), newId,
                         param.getIdType(), param.getDefaultExpr(),
                         param.getVarargsType());
    }

    public static Param makeVarargsParam(Id name, Type type) {
        return makeParam(NodeUtil.getSpan(name), Modifiers.None, name,
                         Option.<Type>none(), Option.<Expr>none(), Option.<Type>some(type));
    }

    public static Param makeVarargsParam(Param param, Modifiers mods) {
        return makeParam(NodeUtil.getSpan(param), mods, param.getName(),
                         Option.<Type>none(), Option.<Expr>none(),
                         param.getVarargsType());
    }

    public static Param makeVarargsParam(Span span, Modifiers mods,
                                         Id name, Type type) {
        return makeParam(span, mods, name,
                         Option.<Type>none(), Option.<Expr>none(),
                         Option.<Type>some(type));
    }

    public static Param makeAbsParam(Type type) {
        return makeParam(NodeUtil.getSpan(type), Modifiers.None,
                         makeId(NodeUtil.getSpan(type), "_"), type);
    }

    public static Param makeParam(Span span, Modifiers mods, Id name,
                                  Option<Type> type, Option<Expr> expr,
                                  Option<Type> varargsType) {
        return new Param(makeSpanInfo(span), name, mods, type, expr, varargsType);
    }

    public static ExprInfo makeExprInfo(ExprInfo org, Span span) {
        return makeExprInfo(span, org.isParenthesized(), org.getExprType());
    }

    public static ExprInfo makeExprInfo(Span span) {
        return makeExprInfo(span, false);
    }

    public static ExprInfo makeExprInfo(Span span, boolean parenthesized) {
        return makeExprInfo(span, parenthesized,
                            Option.<Type>none());
    }

    public static ExprInfo makeExprInfo(Span span, boolean parenthesized,
                                        Option<Type> ty) {
        return new ExprInfo(span, parenthesized, ty);
    }

    public static TypeInfo makeTypeInfo(TypeInfo org, Span span) {
        return makeTypeInfo(span, org.isParenthesized(), org.getStaticParams(),
                            org.getWhereClause());
    }

    public static TypeInfo makeTypeInfo(Span span) {
        return makeTypeInfo(span, false);
    }

    public static TypeInfo makeTypeInfo(Span span, boolean parenthesized) {
        return makeTypeInfo(span, parenthesized,
                            Collections.<StaticParam>emptyList(),
                            Option.<WhereClause>none());
    }

    public static TypeInfo makeTypeInfo(Span span, boolean parenthesized,
                                        List<StaticParam> sparams,
                                        Option<WhereClause> where) {
        return new TypeInfo(span, parenthesized, sparams, where);
    }

    public static ArrowType makeArrowType(Span span, Type domain, Type range,
                                          Effect effect) {
        return makeArrowType(span, false, domain, range, effect,
                             Collections.<StaticParam>emptyList(),
                             Option.<WhereClause>none());
    }

    public static ArrowType makeArrowType(Span span, Type domain, Type range) {
        return makeArrowType(span, domain, range,
                             makeEffect(NodeUtil.getSpan(range).getEnd()));
    }

    public static ArrowType makeArrowType(Span span, boolean parenthesized,
                                          Type domain, Type range, Effect effect,
                                          List<StaticParam> sparams,
                                          Option<WhereClause> where) {
        TypeInfo info = makeTypeInfo(span, parenthesized, sparams, where);
        return new ArrowType(info, domain, range, effect);
    }

    public static TupleType makeTupleType(TupleType t, List<Type> tys) {
        return makeTupleType(NodeUtil.getSpan(t), tys);
    }

    public static TupleType makeTupleType(Span span, List<Type> elements) {
        return makeTupleType(span, false, elements, Option.<Type>none(),
                             Collections.<KeywordType>emptyList());
    }

    public static TupleType makeTupleType(Span span, boolean parenthesized,
                                          List<Type> elements,
                                          Option<Type> varargs,
                                          List<KeywordType> keywords) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new TupleType(info, elements, varargs, keywords);
    }

    public static TaggedDimType makeTaggedDimType(TaggedDimType t, Type s,
                                                  DimExpr u) {
        return makeTaggedDimType(NodeUtil.getSpan(t), NodeUtil.isParenthesized(t), s, u,
                                 t.getUnitExpr());
    }

    public static TaggedDimType makeTaggedDimType(Span span, boolean parenthesized,
                                                  Type elem, DimExpr dim,
                                                  Option<Expr> unit) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new TaggedDimType(info, elem, dim, unit);
    }

    public static TraitType makeTraitType(TraitType original) {
        return makeTraitType(NodeUtil.getSpan(original), NodeUtil.isParenthesized(original),
                             original.getName(), original.getArgs(),
                             Collections.<StaticParam>emptyList());

    }

    public static TraitType makeTraitType(TraitType t,
                                          List<StaticArg> args) {
        return makeTraitType(NodeUtil.getSpan(t), NodeUtil.isParenthesized(t),
                             t.getName(), args);
    }

    public static TraitType makeTraitType(Id name, StaticArg... args) {
        return makeTraitType(NodeUtil.getSpan(name), false, name, Arrays.asList(args));
    }

    public static TraitType makeTraitType(Span span, String nameFirst, String... nameRest) {
        return makeTraitType(span, false, makeId(span, nameFirst, nameRest));
    }

    public static TraitType makeTraitType(Span span, String name,
                                          List<StaticArg> sargs) {
        return makeTraitType(span, false, makeId(span, name), sargs);
    }

    public static TraitType makeTraitType(Span span, boolean isParenthesized,
                                          Id name) {
        return makeTraitType(span, isParenthesized, name,
                             Collections.<StaticArg>emptyList());
    }

    public static TraitType makeTraitType(Span span, boolean isParenthesized,
                                          Id name, StaticArg... args) {
        return makeTraitType(span, isParenthesized, name, Arrays.asList(args));
    }

    public static TraitType makeTraitType(Span span, boolean isParenthesized,
                                          Id name, List<StaticArg> args) {
        return makeTraitType(span, isParenthesized, name, args,
                             Collections.<StaticParam>emptyList());
    }

    public static TraitType makeTraitType(Span span, Id name,
                                          List<StaticArg> args) {
        return makeTraitType(span, false, name, args,
                             Collections.<StaticParam>emptyList());
    }

    public static TraitType makeTraitType(Id name,
                                          List<StaticArg> sargs) {
        return makeTraitType(NodeUtil.getSpan(name), false, name, sargs);
    }

    public static TraitType makeTraitType(Id name) {
        return makeTraitType(NodeUtil.getSpan(name), false, name);
    }

    public static TraitType makeTraitType(Span span, boolean parenthesized,
                                          Id name, List<StaticArg> sargs,
                                          List<StaticParam> sparams) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new TraitType(info, name, sargs, sparams);
    }

    public static VarType makeVarType(Span span, String string) {
        return makeVarType(span, makeId(span, string));
    }

    public static VarType makeVarType(Span span, Id id) {
        return makeVarType(span, false, id, lexicalDepth);
    }

    public static VarType makeVarType(Span span, Id id, int depth) {
        return makeVarType(span, false, id, depth);
    }

    public static VarType makeVarType(VarType original, int lexicalNestedness) {
        return makeVarType(NodeUtil.getSpan(original), NodeUtil.isParenthesized(original),
                           original.getName(), lexicalNestedness);
    }

    public static VarType makeVarType(Span span, boolean parenthesized,
                                      Id name, int lexicalDepth) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new VarType(info, name, lexicalDepth);
    }

    public static DimBinaryOp makeDimBinaryOp(DimBinaryOp t, DimExpr s, DimExpr u, Op o) {
        return makeDimBinaryOp(NodeUtil.getSpan(t), NodeUtil.isParenthesized(t), s, u, o);
    }

    public static DimBinaryOp makeDimBinaryOp(Span span, boolean parenthesized,
                                              DimExpr left, DimExpr right, Op op) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new DimBinaryOp(info, left, right, op);
    }

    public static DimUnaryOp makeDimUnaryOp(DimUnaryOp t, DimExpr s) {
        return makeDimUnaryOp(NodeUtil.getSpan(t), NodeUtil.isParenthesized(t), s, t.getOp());
    }

    public static DimUnaryOp makeDimUnaryOp(Span span, boolean parenthesized,
                                            DimExpr dim, Op op) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new DimUnaryOp(info, dim, op);
    }

    public static DimExponent makeDimExponent(DimExponent t, Type s) {
        return makeDimExponent(NodeUtil.getSpan(t), NodeUtil.isParenthesized(t), s,
                               t.getPower());
    }

    public static DimExponent makeDimExponent(Span span, boolean parenthesized,
                                              Type base, IntExpr power) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new DimExponent(info, base, power);
    }

    public static DimRef makeDimRef(Span span, String name) {
        return makeDimRef(span, false, makeId(span, name));
    }

    public static DimRef makeDimRef(Span span, Id name) {
        return makeDimRef(span, false, name);
    }

    public static DimRef makeDimRef(Span span, boolean parenthesized, Id name) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new DimRef(info, name);
    }

    public static DimBase makeDimBase(Span span, boolean parenthesized) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new DimBase(info);
    }

    public static FixedPointType makeFixedPointType(FixedPointType t, Type s) {
        return makeFixedPointType(NodeUtil.getSpan(t), NodeUtil.isParenthesized(t), t.getName(), s);
    }

    public static FixedPointType makeFixedPointType(_InferenceVarType name, Type s) {
        return makeFixedPointType(NodeUtil.getSpan(s), NodeUtil.isParenthesized(s), name, s);
    }

    public static FixedPointType makeFixedPointType(Span span, boolean parenthesized,
                                                    _InferenceVarType name, Type body) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new FixedPointType(info, name, body);
    }

    public static UnionType makeUnionType(Type t1, Type t2) {
        return makeUnionType(FortressUtil.spanTwo(t1, t2), false,
                             Arrays.asList(t1, t2));
    }

    public static UnionType makeUnionType(Set<? extends Type> types){
        Span span;
        if ( types.isEmpty() )
            span = typeSpan;
        else
            span = FortressUtil.spanAll(types);
        return makeUnionType(span, false,
                             CollectUtil.makeList(types));
    }

    public static UnionType makeUnionType(Span span, boolean parenthesized,
                                          List<Type> elements) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new UnionType(info, elements);
    }

    public static List<Type> make_InferenceVarTypes(Span s, int size) {
        List<Type> result = new ArrayList<Type>(size);
        for (int i = 0; i < size; i++) { result.add(make_InferenceVarType(s)); }
        return result;
    }

    public static _InferenceVarType make_InferenceVarType(Span s) {
        return make_InferenceVarType(s, false, new Object());
    }

    public static _InferenceVarType make_InferenceVarType(Span span, boolean parenthesized, Object id) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new _InferenceVarType(info, id);
    }

    public static TaggedUnitType makeTaggedUnitType(TaggedUnitType t, Type s) {
        return makeTaggedUnitType(NodeUtil.getSpan(t), NodeUtil.isParenthesized(t), s,
                                  t.getUnitExpr());
    }

    public static TaggedUnitType makeTaggedUnitType(Span span, boolean parenthesized,
                                                    Type elem, Expr unit) {
        return makeTaggedUnitType(span, parenthesized, elem, unit);
    }


    public static MatrixType makeMatrixType(Span span, Type element,
                                            ExtentRange dimension) {
        List<ExtentRange> dims = new ArrayList<ExtentRange>();
        dims.add(dimension);
        dims = Useful.immutableTrimmedList(dims);
        return makeMatrixType(span, false, element, dims);
    }

    public static MatrixType makeMatrixType(Span span, Type element,
                                            ExtentRange dimension,
                                            List<ExtentRange> dimensions) {
        List<ExtentRange> dims = new ArrayList<ExtentRange>();
        dims.add(dimension);
        dims.addAll(dimensions);
        dims = Useful.immutableTrimmedList(dims);
        return makeMatrixType(span, false, element, dims);
    }

    public static MatrixType makeMatrixType(Span span, boolean parenthesized,
                                            Type elem, List<ExtentRange> dim) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new MatrixType(info, elem, dim);
    }

    public static ExtentRange makeExtentRange(Span span,
                                              Option<StaticArg> first,
                                              Option<StaticArg> second,
                                              Option<Op> op) {
        return new ExtentRange(makeSpanInfo(span), first, second, op);
    }

    public static Indices makeIndices(Span span, List<ExtentRange> extents) {
        return new Indices(makeSpanInfo(span), extents);
    }

    public static ArrayType makeArrayType(Span span, Type element,
                                          Option<Indices> ind) {
        Indices indices = ind.unwrap(makeIndices(span, Collections.<ExtentRange>emptyList()));
        return makeArrayType(span, false, element, indices);
    }

    public static ArrayType makeArrayType(Span span, boolean parenthesized,
                                          Type elem, Indices indices) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new ArrayType(info, elem, indices);
    }

    public static IntersectionType makeIntersectionType(Type t1, Type t2) {
        return makeIntersectionType(FortressUtil.spanTwo(t1, t2), false,
                                    Arrays.asList(t1, t2));
    }

    public static IntersectionType makeIntersectionType(Set<? extends Type> types) {
        Span span;
        if ( types.isEmpty() )
            span = typeSpan;
        else
            span = FortressUtil.spanAll(types);
        return makeIntersectionType(span, false, CollectUtil.makeList(types));
    }

    public static IntersectionType makeIntersectionType(Span span,
                                                        List<Type> elems) {
        return makeIntersectionType(span, false, elems);
    }

    public static IntersectionType makeIntersectionType(Span span, boolean parenthesized,
                                                        List<Type> elems) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new IntersectionType(info, elems);
    }

    /** Create an "empty" effect at the given location. */
    public static Effect makeEffect(SourceLoc loc) {
        return makeEffect(new Span(loc, loc));
    }

    public static Effect makeEffect(Span span) {
        return makeEffect(span, Option.<List<BaseType>>none(), false);
    }

    public static Effect makeEffect(List<BaseType> throwsClause) {
        Span span;
        if ( throwsClause.isEmpty() )
            span = typeSpan;
        else
            span = FortressUtil.spanAll(throwsClause);
        return makeEffect(span, Option.some(throwsClause), false);
    }

    public static Effect makeEffect(SourceLoc defaultLoc, List<BaseType> throwsClause) {
        return makeEffect(FortressUtil.spanAll(defaultLoc, throwsClause),
                          Option.some(throwsClause), false);
    }

    public static Effect makeEffect(Option<List<BaseType>> throwsClause) {
        Span span;
        if ( throwsClause.isNone() )
            span = typeSpan;
        else {
            if ( throwsClause.unwrap().isEmpty() )
                span = typeSpan;
            else
                span = FortressUtil.spanAll(throwsClause.unwrap());
        }
        return makeEffect(span, throwsClause, false);
    }

    public static Effect makeEffect(SourceLoc defaultLoc, Option<List<BaseType>> throwsClause) {
        Span span = FortressUtil.spanAll(defaultLoc,
                                         throwsClause.unwrap(Collections.<BaseType>emptyList()));
        return makeEffect(span, throwsClause, false);
    }

    public static Effect makeEffect(Span span, boolean ioEffect) {
        return makeEffect(span, Option.<List<BaseType>>none(), ioEffect);
    }


    public static Effect makeEffect(Span span, Option<List<BaseType>> throwsC,
                                    boolean ioEffect) {
        return new Effect(makeSpanInfo(span), throwsC, ioEffect);
    }

    public static TraitTypeWhere makeTraitTypeWhere(BaseType in_type) {
        Span sp = NodeUtil.getSpan(in_type);
        return makeTraitTypeWhere(sp, in_type,
                                  Option.<WhereClause>none());
    }

    public static TraitTypeWhere makeTraitTypeWhere(BaseType in_type, Option<WhereClause> in_where) {
        if ( in_where.isSome() )
            return makeTraitTypeWhere(new Span(NodeUtil.getSpan(in_type), NodeUtil.getSpan(in_where.unwrap())), in_type, in_where);
        else
            return makeTraitTypeWhere(NodeUtil.getSpan(in_type), in_type, in_where);
    }

    public static TraitTypeWhere makeTraitTypeWhere(Span span, BaseType type,
                                                    Option<WhereClause> where) {
        SpanInfo info = makeSpanInfo(span);
        return new TraitTypeWhere(info, type, where);
    }

    public static ConstructorFnName makeConstructorFnName(ObjectConstructor def) {
        return makeConstructorFnName(NodeUtil.getSpan(def), Option.<APIName>none(), def);
    }

    public static ConstructorFnName makeConstructorFnName(Span span,
                                                          Option<APIName> api,
                                                          ObjectConstructor def) {
        return new ConstructorFnName(makeSpanInfo(span), api, def);
    }

    public static AnonymousFnName makeAnonymousFnName(Span span,
                                                      Option<APIName> api) {
        SpanInfo info = makeSpanInfo(span);
        return new AnonymousFnName(info, api);
    }

    public static Id makeId(Span span, String name) {
        return makeId(span, Option.<APIName>none(), name);
    }

    public static Id makeId(Span span, Option<APIName> apiName, String text) {
        SpanInfo info = makeSpanInfo(span);
        return new Id(info, apiName, text);
    }


    public static Id bogusId(Span span) {
        return makeId(span, Option.<APIName>none(), "_");
    }

    public static Id makeId(Id id, String newName) {
        return makeId(NodeUtil.getSpan(id), id.getApiName(), newName);
    }

    public static Id makeId(Span span, Id id) {
        return makeId(span, id.getApiName(), id.getText());
    }

    public static Id makeId(Iterable<Id> apiIds, Id id) {
        Span span = FortressUtil.spanTwo(FortressUtil.spanAll(apiIds),
                                         NodeUtil.getSpan(id));
        Option<APIName> api;
        if (IterUtil.isEmpty(apiIds)) {
            api = Option.none();
        }
        else {
            APIName n = makeAPIName(span,apiIds);
            api = Option.some(n);
        }
        return makeId(span, api, id.getText());
    }

    public static Id makeId(Span span, String api, String name) {
        List<Id> apis = new ArrayList<Id>();
        apis.add(makeId(span, api));
        apis = Useful.immutableTrimmedList(apis);
        return makeId(span, Option.some(makeAPIName(span, apis)), name);
    }

    public static Id makeId(Span span, Iterable<Id> apiIds, Id id) {
        Option<APIName> api;
        if (IterUtil.isEmpty(apiIds)) { api = Option.none(); }
        else { api = Option.some(makeAPIName(span,apiIds)); }
        return makeId(span, api, id.getText());
    }

    public static Id makeId(Span span, Id id, Iterable<Id> ids) {
        Option<APIName> api;
        Id last;
        if (IterUtil.isEmpty(ids)) { api = Option.none(); last = id; }
        else { api = Option.some(makeAPIName(id, IterUtil.skipLast(ids)));
        last = IterUtil.last(ids);
        }
        return makeId(span, api, last.getText());
    }

    public static Id makeId(Span span, APIName api, Id id) {
        return makeId(span, Option.some(api), id.getText());
    }

    /** Assumes {@code ids} is nonempty. */
    public static Id makeId(Iterable<Id> ids) {
        return makeId(IterUtil.skipLast(ids), IterUtil.last(ids));
    }

    public static Id makeId(final Span span, String nameFirst, String... nameRest) {
        final Lambda<String, Id> STRING_TO_ID = new Lambda<String, Id>() {
            public Id value(String arg) { return makeId(span,arg); }
        };
        Iterable<Id> ids = IterUtil.map(IterUtil.asIterable(nameRest), STRING_TO_ID);
        return makeId(span, makeId(span, nameFirst), ids);
    }

    public static Id makeId(APIName api, Id name) {
        return makeId(FortressUtil.spanTwo(api, name), Option.some(api),
                name.getText());
    }

    public static Id makeId(APIName api, Id name, Span span) {
        return makeId(span, Option.some(api), name.getText());
    }

    public static Id makeId(Option<APIName> api, Id name) {
        return makeId(NodeUtil.getSpan(name), api, name.getText());
    }

    public static Id makeId(Span span, APIName api, String name) {
        return makeId(span, Option.some(api), name);
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
        return makeAPIName(FortressUtil.spanTwo(first, last), ids);
    }

    public static APIName makeAPIName(Id first, Id rest) {
        List<Id> ids = new ArrayList<Id>();
        ids.add(first);
        if (rest.getApiName().isSome()) {
            ids.addAll(rest.getApiName().unwrap().getIds());
        }
        ids.add(makeId(NodeUtil.getSpan(rest), rest.getText()));
        ids = Useful.immutableTrimmedList(ids);
        return makeAPIName(FortressUtil.spanTwo(first, rest), ids);
    }

    public static APIName makeAPIName(Span span, List<Id> apis) {
        return makeAPIName(span, apis, Useful.<Id>dottedList(apis).intern());
    }

    public static APIName makeAPIName(Span span, Id s) {
        return makeAPIName(span, Useful.list(s));
    }

    public static APIName makeAPIName(Id s) {
        return makeAPIName(NodeUtil.getSpan(s), Useful.list(s));
    }

    public static APIName makeAPIName(Span span, List<Id> apis, String text) {
        SpanInfo info = makeSpanInfo(span);
        return new APIName(info, apis, text);
    }

    private static List<Id> stringToIds(Span span, String path) {
        List<Id> ids = new ArrayList<Id>();

        StringTokenizer st = new StringTokenizer(path, ".");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            ids.add(makeId(span, e));
        }
        ids = Useful.immutableTrimmedList(ids);
        return ids;
    }

    public static APIName makeAPIName(Span span, String s) {
        return makeAPIName(span, stringToIds(span, s));
    }

    public static APIName makeAPIName(Id id, Iterable<Id> ids) {
        Span span;
        if ( IterUtil.isEmpty(ids) )
            span = NodeUtil.getSpan(id);
        else
            span = FortressUtil.spanTwo(NodeUtil.getSpan(id),
                                        FortressUtil.spanAll(ids));
        return makeAPIName(span, CollectUtil.makeList(IterUtil.compose(id, ids)));
    }

    public static APIName makeAPIName(Span span, Iterable<Id> ids) {
        return makeAPIName(span, CollectUtil.makeList(ids));
    }

    public static BoolRef makeBoolRef(Span span, String string) {
        return makeBoolRef(span, false, makeId(span, string), lexicalDepth);
    }

    public static BoolRef makeBoolRef(BoolRef old, int depth) {
        return makeBoolRef(NodeUtil.getSpan(old), old.isParenthesized(), old.getName(), depth);
    }

    public static BoolRef makeBoolRef(Span span, Id name) {
        return makeBoolRef(span, false, name, lexicalDepth);
    }

    public static BoolRef makeBoolRef(Span span, boolean parenthesized,
                                      Id name, int depth) {
        SpanInfo info = makeSpanInfo(span);
        return new BoolRef(info, parenthesized, name, depth);
    }

    public static BoolBinaryOp makeBoolBinaryOp(Span span,
                                                BoolExpr left, BoolExpr right, Op op) {
        return makeBoolBinaryOp(span, false, left, right, op);
    }

    public static BoolBinaryOp makeBoolBinaryOp(Span span, boolean parenthesized,
                                                BoolExpr left, BoolExpr right, Op op) {
        SpanInfo info = makeSpanInfo(span);
        return new BoolBinaryOp(info, parenthesized, left, right, op);
    }

    public static IntBinaryOp makeIntBinaryOp(Span span,
                                              IntExpr left, IntExpr right, Op op) {
        return makeIntBinaryOp(span, false, left, right, op);
    }

    public static IntBinaryOp makeIntBinaryOp(Span span, boolean parenthesized,
                                              IntExpr left, IntExpr right, Op op) {
        SpanInfo info = makeSpanInfo(span);
        return new IntBinaryOp(info, parenthesized, left, right, op);
    }

    public static IntRef makeIntRef(Span span, String string) {
        return makeIntRef(span, makeId(span, string));
    }

    public static IntRef makeIntRef(IntRef old, int depth) {
        return makeIntRef(NodeUtil.getSpan(old), old.isParenthesized(), old.getName(), depth);
    }

    public static IntRef makeIntRef(Span span, Id name) {
        return makeIntRef(span, false, name, lexicalDepth);
    }

    public static IntRef makeIntRef(Span span, boolean parenthesized,
                                    Id name, int depth) {
        return new IntRef(makeSpanInfo(span), parenthesized, name, depth);
    }

    public static UnitBinaryOp makeUnitBinaryOp(Span span, boolean parenthesized,
                                                UnitExpr left, UnitExpr right, Op op) {
        return new UnitBinaryOp(makeSpanInfo(span), parenthesized, left, right, op);
    }

    public static Contract makeContract(Span span,
                                        Option<List<Expr>> requiresClause,
                                        Option<List<EnsuresClause>> ensuresClause,
                                        Option<List<Expr>> invariantsClause) {
        return new Contract(makeSpanInfo(span), requiresClause, ensuresClause, invariantsClause);
    }

    public static EnsuresClause makeEnsuresClause(Span span, Expr post,
                                                  Option<Expr> pre) {
        return new EnsuresClause(makeSpanInfo(span), post, pre);
    }

    public static TraitTypeHeader makeTraitTypeHeader(TraitTypeHeader header,
                                                      List<TraitTypeWhere> extendsC) {
        return makeTraitTypeHeader(header.getMods(), header.getName(),
                                   header.getStaticParams(), header.getWhereClause(),
                                   header.getThrowsClause(), header.getContract(),
                                   extendsC, header.getDecls());
    }

    public static TraitTypeHeader makeTraitTypeHeaderWithDecls(TraitTypeHeader header,
                                                               List<Decl> decls) {
        return makeTraitTypeHeader(header, decls, header.getContract());
    }

    public static TraitTypeHeader makeTraitTypeHeader(TraitTypeHeader header,
                                                      List<Decl> decls,
                                                      Option<Contract> contract) {
        return makeTraitTypeHeader(header.getMods(), header.getName(),
                                   header.getStaticParams(), header.getWhereClause(),
                                   header.getThrowsClause(), contract,
                                   header.getExtendsClause(), decls);
    }

    public static TraitTypeHeader makeTraitTypeHeader(Span span,
                                                      List<TraitTypeWhere> extendsC,
                                                      List<Decl> decls) {
        return makeTraitTypeHeader(makeId(span,"_"), extendsC, decls);
    }

    public static TraitTypeHeader makeTraitTypeHeader(IdOrOpOrAnonymousName name,
                                                      List<TraitTypeWhere> extendsClause,
                                                      List<Decl> decls) {
        return makeTraitTypeHeader(Modifiers.None, name,
                                   Collections.<StaticParam>emptyList(),
                                   Option.<WhereClause>none(),
                                   Option.<List<BaseType>>none(), Option.<Contract>none(),
                                   extendsClause, decls);
    }

    public static TraitTypeHeader makeTraitTypeHeader(Modifiers mods,
                                                      IdOrOpOrAnonymousName name,
                                                      List<StaticParam> staticParams,
                                                      Option<WhereClause> whereClause,
                                                      Option<List<BaseType>> throwsClause,
                                                      Option<Contract> contract,
                                                      List<TraitTypeWhere> extendsClause,
                                                      List<Decl> decls) {
        return new TraitTypeHeader(staticParams, mods, name, whereClause,
                                   throwsClause, contract, extendsClause,
                                   decls);
    }

    public static AnyType makeAnyType(Span span) {
        return new AnyType(makeTypeInfo(span));
    }

    public static BottomType makeBottomType(Span span) {
        return new BottomType(makeTypeInfo(span));
    }

    public static LabelType makeLabelType(Span span) {
        return new LabelType(makeTypeInfo(span));
    }

    /***************************************************************************/

    public static Id makeTemporaryId(Span span) {
        return makeId(span, "$$bogus_name$$");
    }

    public static Id makeIdFromLast(Id id) {
        return makeId(NodeUtil.getSpan(id), id.getText());
    }

    public static KeywordType makeKeywordType(KeywordType t, Type s) {
        return makeKeywordType(NodeUtil.getSpan(t), t.getName(), s);
    }

    public static KeywordType makeKeywordType(Id name, Type type) {
        return makeKeywordType(FortressUtil.spanTwo(name,type), name, type);
    }

    public static KeywordType makeKeywordType(Span span, Id name, Type type) {
        return new KeywordType(makeSpanInfo(span), name, type);
    }

    public static TypeArg makeTypeArg(Span span, Type t) {
        return new TypeArg(makeSpanInfo(span), t);
    }

    public static TypeArg makeTypeArg(TypeArg t, Type s) {
        return makeTypeArg(NodeUtil.getSpan(t), s);
    }

    public static TypeArg makeTypeArg(Type ty) {
        return makeTypeArg(NodeUtil.getSpan(ty), ty);
    }

    public static TypeArg makeTypeArg(Span span, String string) {
        return makeTypeArg(span, makeVarType(span, makeId(span, string)));
    }

    public static DimArg makeDimArg(DimArg t, DimExpr s) {
        return makeDimArg(NodeUtil.getSpan(t), s);
    }

    public static DimArg makeDimArg(DimExpr s) {
        return makeDimArg(NodeUtil.getSpan(s), s);
    }

    public static DimArg makeDimArg(Span span, DimExpr d) {
        return new DimArg(makeSpanInfo(span), d);
    }

    public static UnitArg makeUnitArg(UnitExpr s) {
        return makeUnitArg(NodeUtil.getSpan(s), s);
    }

    public static UnitArg makeUnitArg(Span span, UnitExpr u) {
        return new UnitArg(makeSpanInfo(span), u);
    }

    public static UnitRef makeUnitRef(Span span, String name) {
        return makeUnitRef(span, false, makeId(span, name));
    }

    public static UnitRef makeUnitRef(Span span, boolean parenthesized,
                                      Id name) {
        return new UnitRef(makeSpanInfo(span), parenthesized, name);
    }

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
                return makeTupleType(span, elements);
        } else
            return makeTupleType(span, false, elements, varargs, keywords);
    }

    public static APIName makeAPINameFromPath(Span span, String path, String delimiter) {
        List<Id> ids = new ArrayList<Id>();
        String file = new File(path).getName();
        if (file.length() <= 4) {
            return error(makeId(span, "_"), "Invalid file name.");
        }
        for (String n : file.substring(0, file.length()-4).split(delimiter)) {
            ids.add(makeId(span, n));
        }
        ids = Useful.immutableTrimmedList(ids);
        return makeAPIName(span, ids);
    }

    public static Op makeEnclosing(Span in_span, String in_open, String in_close) {
        return makeOp(in_span, Option.<APIName>none(),
                      PrecedenceMap.ONLY.canon(in_open + " " + in_close),
                      enclosing, true);
    }

    public static Op makeOp(Span span, Option<APIName> api,
                            String text, Fixity fixity, boolean b) {
        return new Op(makeSpanInfo(span), api, text, fixity, b);
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

    public static Op makeOp(Span span, String name) {
        return makeOp(span, Option.<APIName>none(),
                      PrecedenceMap.ONLY.canon(name), unknownFix, false);
    }

    public static Op makeOp(Span span, String name, Fixity fixity) {
        return makeOp(span, Option.<APIName>none(),
                      PrecedenceMap.ONLY.canon(name), fixity, false);
    }

    public static Op makeOp(Op op, String name) {
        return makeOp(NodeUtil.getSpan(op), Option.<APIName>none(),
                      PrecedenceMap.ONLY.canon(name),
                      op.getFixity(), op.isEnclosing());
    }

    public static Op makeOpInfix(Span span, String name) {
        return makeOp(span, Option.<APIName>none(),
                      PrecedenceMap.ONLY.canon(name), infix, false);
    }

    public static Op makeOpInfix(Span span, String apiName, String name) {
        Op op =  makeOp(span, Option.some(NodeFactory.makeAPIName(span,apiName)),
                        PrecedenceMap.ONLY.canon(name), infix, false);
        return op;

    }

    public static Op makeOpInfix(Op op) {
        return makeOp(NodeUtil.getSpan(op), op.getApiName(), op.getText(), infix, op.isEnclosing());
    }

    public static Op makeOpPrefix(Span span, String name) {
        return makeOp(span, Option.<APIName>none(),
                      PrecedenceMap.ONLY.canon(name), prefix, false);
    }

    public static Op makeOpPrefix(Op op) {
        return makeOp(NodeUtil.getSpan(op), op.getApiName(), op.getText(), prefix, op.isEnclosing());
    }

    public static Op makeOpPostfix(Span span, String name) {
        return makeOp(span, Option.<APIName>none(),
                      PrecedenceMap.ONLY.canon(name), postfix, false);
    }

    public static Op makeOpPostfix(Op op) {
        return makeOp(NodeUtil.getSpan(op), op.getApiName(), op.getText(), postfix, op.isEnclosing());
    }

    /**
     * Rewrites the given Op with the given api. Dispatches on the
     * type of op, so that the same subtype of Op is created.
     */
    public static Op makeOp(final APIName api, Op op) {
        return makeOp(NodeUtil.getSpan(op), Option.some(api),
                      op.getText(), op.getFixity(), op.isEnclosing() );
    }

    public static Op makeOpNofix(Op op) {
        return makeOp(NodeUtil.getSpan(op), Option.<APIName>none(),
                      op.getText(), nofix, op.isEnclosing());
    }

    public static Op makeOpMultifix(Op op) {
        return makeOp(NodeUtil.getSpan(op), Option.<APIName>none(),
                      op.getText(), multifix, op.isEnclosing());
    }

    public static Op makeOpEnclosing(Span span, String name) {
        return makeOp(span, Option.<APIName>none(),
                      PrecedenceMap.ONLY.canon(name), enclosing, false);
    }

    public static Op makeOpBig(Span span, String name) {
        return makeOp(span, Option.<APIName>none(),
                      PrecedenceMap.ONLY.canon(name), big, false);
    }

    public static Op makeOpUnknown(Span span, String name) {
        return makeOp(span, Option.<APIName>none(),
                      PrecedenceMap.ONLY.canon(name), unknownFix, false);
    }

    public static Op makeBig(Op op) {
        if ( op.isEnclosing() ) {
            String _op = op.getText();
            String left  = _op.split(" ")[0];
            String right = "BIG " + _op.substring(left.length()+1);
            left  = "BIG " + left;
            return makeEnclosing(NodeUtil.getSpan(op), left, right);
        } else
            return makeOp(NodeUtil.getSpan(op), Option.<APIName>none(),
                          PrecedenceMap.ONLY.canon("BIG " + op.getText()), big, op.isEnclosing() );
    }

    public static StaticParam makeStaticParam(Span span,
                                              IdOrOp name, List<BaseType> tys,
                                              Option<Type> ty, boolean b,
                                              StaticParamKind k) {
        return new StaticParam(makeSpanInfo(span), name, tys, ty, b, k);
    }

    public static StaticParam makeTypeParam(Span span,
                                            Id id, List<BaseType> tys,
                                            Option<Type> ty, boolean b) {
        return makeStaticParam(span, id, tys, ty, b, new KindType());
    }

    public static StaticParam makeTypeParam(Span s, String name) {
        return makeStaticParam(s, makeId(s, name),
                               Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindType());
    }

    public static StaticParam makeTypeParam(Span s, String name, String sup) {
        List<BaseType> supers = new ArrayList<BaseType>(1);
        supers.add(makeVarType(s, sup));
        return makeStaticParam(s, makeId(s, name), supers,
                               Option.<Type>none(), false,
                               new KindType());
    }

    public static StaticParam makeOpParam(Span s, String name) {
        return makeStaticParam(s, makeOp(s, name),
                               Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindOp());
    }

    public static StaticParam makeBoolParam(Span s, String name) {
        return makeStaticParam(s, makeId(s, name),
                               Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindBool());
    }

    public static StaticParam makeDimParam(Span s, String name) {
        return makeStaticParam(s, makeId(s, name),
                               Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindDim());
    }

    public static StaticParam makeUnitParam(Span s, String name) {
        return makeStaticParam(s, makeId(s, name),
                               Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindUnit());
    }

    public static StaticParam makeIntParam(Span s, String name) {
        return makeStaticParam(s, makeId(s, name),
                               Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindInt());
    }

    public static StaticParam makeNatParam(Span s, String name) {
        return makeStaticParam(s, makeId(s, name),
                               Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindNat());
    }

    public static TupleType makeVoidType(Span span) {
        return makeTupleType(span, false, Collections.<Type>emptyList(),
                             Option.<Type>none(),
                             Collections.<KeywordType>emptyList());
    }

    public static BoolBase makeBoolBase(Span span,
                                        boolean parenthesized,
                                        boolean val) {
        return new BoolBase(makeSpanInfo(span), parenthesized, val);
    }

    public static BoolArg makeBoolArg(Span span, String string) {
        return makeBoolArg(span, makeBoolRef(span, string));
    }

    public static BoolArg makeBoolArg(Span span, BoolExpr b) {
        return new BoolArg(makeSpanInfo(span), b);
    }

    public static BoolUnaryOp makeBoolUnaryOp(Span span, BoolExpr b, Op op) {
        return makeBoolUnaryOp(span, b.isParenthesized(), b, op);
    }

    public static BoolUnaryOp makeBoolUnaryOp(Span span, boolean parenthesized,
                                              BoolExpr b, Op op) {
        return new BoolUnaryOp(makeSpanInfo(span), parenthesized, b, op);
    }

    public static IntBase makeIntBase(Span span, boolean parenthesized,
                                      IntLiteralExpr val) {
        return new IntBase(makeSpanInfo(span), parenthesized, val);
    }

    public static IntExpr makeIntVal(Span span, String i) {
        return makeIntBase(span, false,
                           ExprFactory.makeIntLiteralExpr(span,
                                                          new BigInteger(i)));
    }

    public static IntArg makeIntArg(Span span, String string) {
        return makeIntArg(span, makeIntRef(span, string));
    }

    public static IntArg makeIntArgVal(Span span, String i) {
        return makeIntArg(span, makeIntVal(span, i));
    }

    public static IntArg makeIntArg(Span span, IntExpr i) {
        return new IntArg(makeSpanInfo(span), i);
    }

    public static OpArg makeOpArg(Span span, String string) {
        return makeOpArg(span, ExprFactory.makeOpRef(makeOp(span, string)));
    }

    public static OpArg makeOpArg(Span span, FunctionalRef op) {
        return new OpArg(makeSpanInfo(span), op);
    }

    public static VarDecl makeVarDecl(Span span, List<LValue> lvals,
                                      Option<Expr> expr) {
        FortressUtil.validId(lvals);
        return new VarDecl(makeSpanInfo(span), lvals, expr);
    }

    public static VarDecl makeVarDecl(Span span, List<LValue> lvals) {
        FortressUtil.validId(lvals);
        return makeVarDecl(span, lvals, Option.<Expr>none());
    }

    public static VarDecl makeVarDecl(Span span, List<LValue> lvals, Expr init) {
        FortressUtil.validId(lvals);
        return makeVarDecl(span, lvals, Option.<Expr>some(init));
    }

    public static VarDecl makeVarDecl(Span span, Id name, Expr init) {
        FortressUtil.validId(name);
        LValue bind = new LValue(makeSpanInfo(span), name, Modifiers.None,
                                 Option.<Type>none(), true);
        return makeVarDecl(span, Useful.<LValue>list(bind), Option.<Expr>some(init));
    }

    public static VarDecl makeVarDecl(Span span, String name, Expr init) {
        Id id = makeId(span, name);
        FortressUtil.validId(id);
        LValue bind = new LValue(makeSpanInfo(span), id,
                                 Modifiers.None,
                                 Option.<Type>none(), false);
        return makeVarDecl(span, Useful.<LValue>list(bind), Option.<Expr>some(init));
    }

    public static BoolExpr makeInParentheses(BoolExpr be) {
        return be.accept(new NodeAbstractVisitor<BoolExpr>() {
            public BoolExpr forBoolBase(BoolBase b) {
                return makeBoolBase(NodeUtil.getSpan(b), true, b.isBoolVal());
            }
            public BoolExpr forBoolRef(BoolRef b) {
                return makeBoolRef(NodeUtil.getSpan(b), true, b.getName(), b.getLexicalDepth());
            }
            public BoolExpr forBoolUnaryOp(BoolUnaryOp b) {
                return makeBoolUnaryOp(NodeUtil.getSpan(b), true, b.getBoolVal(), b.getOp());
            }
            public BoolExpr forBoolBinaryOp(BoolBinaryOp b) {
                return makeBoolBinaryOp(NodeUtil.getSpan(b), true,
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
                return makeDimBase(NodeUtil.getSpan(t), true);
            }
            public DimExpr forDimRef(DimRef t) {
                return makeDimRef(NodeUtil.getSpan(t), true, t.getName());
            }
            public DimExpr forDimBinaryOp(DimBinaryOp t) {
                return makeDimBinaryOp(NodeUtil.getSpan(t), true, t.getLeft(),
                                       t.getRight(), t.getOp());
            }
            public DimExpr forDimExponent(DimExponent t) {
                return makeDimExponent(NodeUtil.getSpan(t), true, t.getBase(),
                                       t.getPower());
            }
            public DimExpr forDimUnaryOp(DimUnaryOp t) {
                return makeDimUnaryOp(NodeUtil.getSpan(t), true, t.getDimVal(), t.getOp());
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
                return makeIntBase(NodeUtil.getSpan(i), true, i.getIntVal());
            }
            public IntExpr forIntRef(IntRef i) {
                return makeIntRef(NodeUtil.getSpan(i), true, i.getName(), i.getLexicalDepth());
            }
            public IntExpr forIntBinaryOp(IntBinaryOp i) {
                return makeIntBinaryOp(NodeUtil.getSpan(i), true, i.getLeft(),
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
                return makeUnitRef(NodeUtil.getSpan(b), true, b.getName());
            }
            public UnitExpr forUnitBinaryOp(UnitBinaryOp i) {
                return makeUnitBinaryOp(NodeUtil.getSpan(i), true, i.getLeft(),
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
                return makeArrowType(NodeUtil.getSpan(t), true, t.getDomain(),
                                     t.getRange(), t.getEffect(),
                                     NodeUtil.getStaticParams(t),
                                     NodeUtil.getWhereClause(t));
            }
            public Type forArrayType(ArrayType t) {
                return makeArrayType(NodeUtil.getSpan(t), true, t.getElemType(),
                                     t.getIndices());
            }
            public Type forVarType(VarType t) {
                return makeVarType(NodeUtil.getSpan(t), true, t.getName(), t.getLexicalDepth());
            }
            public Type forMatrixType(MatrixType t) {
                return makeMatrixType(NodeUtil.getSpan(t), true, t.getElemType(),
                                      t.getDimensions());
            }
            public Type forTraitType(TraitType t) {
                return makeTraitType(NodeUtil.getSpan(t), true, t.getName(),
                                     t.getArgs(), t.getStaticParams());
            }
            public Type forTupleType(TupleType t) {
                return makeTupleType(NodeUtil.getSpan(t), true, t.getElements(),
                                     t.getVarargs(), t.getKeywords());
            }
            public Type forTaggedDimType(TaggedDimType t) {
                return makeTaggedDimType(NodeUtil.getSpan(t), true, t.getElemType(),
                                         t.getDimExpr(), t.getUnitExpr());
            }
            public Type forTaggedUnitType(TaggedUnitType t) {
                return makeTaggedUnitType(NodeUtil.getSpan(t), true, t.getElemType(),
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
        SpanInfo info = makeSpanInfo(s);
        return new SyntaxDef(info, modifier, syntaxSymbols, transformation);
    }

    public static SuperSyntaxDef makeSuperSyntaxDef(Span s, Option<String> modifier,
                                                    Id nonterminal, Id grammar) {
        SpanInfo info = makeSpanInfo(s);
        return new SuperSyntaxDef(info, modifier, nonterminal, grammar);
    }

    public static Import makeImportStar(APIName api, List<IdOrOpOrAnonymousName> excepts) {
        SpanInfo info = makeSpanInfo(makeSpan(api, excepts));
        return new ImportStar(info, Option.<String>none(), api, excepts);
    }

    public static Op makeListOp(Span span) {
        return makeEnclosing(span, "<|", "|>");
    }

    public static NamedType makeNamedType(APIName api, NamedType type) {
        if (type instanceof VarType) {
            return makeVarType(NodeUtil.getSpan(type),
                               NodeUtil.isParenthesized(type),
                               makeId(api, type.getName()),
                               lexicalDepth);
        }
        else { // type instanceof TraitType
            TraitType _type = (TraitType)type;
            return makeTraitType(NodeUtil.getSpan(_type),
                                 NodeUtil.isParenthesized(_type),
                                 makeId(api, _type.getName()),
                                 _type.getArgs(),
                                 _type.getStaticParams());
        }
    }

    public static TraitType makeGenericSingletonType(Id name, List<StaticParam> params) {
        return makeTraitType(NodeUtil.getSpan(name), false, name,
                             Collections.<StaticArg>emptyList(), params);
    }

    public static WhereExtends makeWhereExtends(Span span, Id name, List<BaseType> supers) {
        return new WhereExtends(makeSpanInfo(span), name, supers);
    }

    public static WhereTypeAlias makeWhereTypeAlias(Span span, TypeAlias alias) {
        return new WhereTypeAlias(makeSpanInfo(span), alias);
    }

    public static WhereCoerces makeWhereCoerces(Span span, Type left, Type right,
                                                boolean coerces, boolean widens) {
        return new WhereCoerces(makeSpanInfo(span), left, right, coerces, widens);
    }

    public static WhereEquals makeWhereEquals(Span span, Id left, Id right) {
        return new WhereEquals(makeSpanInfo(span), left, right);
    }

    public static BoolConstraintExpr makeBoolConstraintExpr(Span span, BoolConstraint c) {
        return new BoolConstraintExpr(makeSpanInfo(span), c);
    }

    public static UnitConstraint makeUnitConstraint(Span span, Id id) {
        return new UnitConstraint(makeSpanInfo(span), id);
    }

    public static IntConstraint makeIntConstraint(Span span, IntExpr left, IntExpr right, Op op) {
        return new IntConstraint(makeSpanInfo(span), left, right, op);
    }

    public static TypeAlias makeTypeAlias(Span span, Id name,
                                          List<StaticParam> sparams, Type def) {
        return new TypeAlias(makeSpanInfo(span), name, sparams, def);
    }

    public static TestDecl makeTestDecl(Span span, Id name, List<GeneratorClause> gens, Expr expr) {
        return new TestDecl(makeSpanInfo(span), name, gens, expr);
    }

    public static PropertyDecl makePropertyDecl(Span span, Option<Id> name, List<Param> params, Expr expr) {
        return new PropertyDecl(makeSpanInfo(span), name, params, expr);
    }

    public static WhereClause makeWhereClause(Span span, List<WhereBinding> bindings,
                                              List<WhereConstraint> constraints) {
        return new WhereClause(makeSpanInfo(span), bindings, constraints);
    }

    public static WhereBinding makeWhereBinding(Span span, Id name,
                                                List<BaseType> supers,
                                                StaticParamKind kind) {
        return new WhereBinding(makeSpanInfo(span), name, supers, kind);
    }

    public static IfClause makeIfClause(Span span,
                                        GeneratorClause testClause, Block body) {
        return new IfClause(makeSpanInfo(span), testClause, body);
    }

    public static CaseClause makeCaseClause(Span span,
                                            Expr matchClause,
                                            Block body,
                                            Option<FunctionalRef> op) {
        return new CaseClause(makeSpanInfo(span), matchClause, body, op);
    }

    public static CatchClause makeCatchClause(Span span,
                                              BaseType matchType,
                                              Block body) {
        return new CatchClause(makeSpanInfo(span), matchType, body);
    }

    public static GeneratorClause makeGeneratorClause(Span span,
                                                      List<Id> bind,
                                                      Expr init) {
        return new GeneratorClause(makeSpanInfo(span), bind, init);
    }

    public static Catch makeCatch(Span span, Id name,
                                  List<CatchClause> clauses) {
        return new Catch(makeSpanInfo(span), name, clauses);
    }

    public static ArrayComprehensionClause makeArrayComprehensionClause(Span span,
                                                                        List<Expr> bind,
                                                                        Expr init,
                                                                        List<GeneratorClause> gens) {
        return new ArrayComprehensionClause(makeSpanInfo(span), bind, init, gens);
    }

    public static KeywordExpr makeKeywordExpr(Span span,
                                              Id name, Expr init) {
        return new KeywordExpr(makeSpanInfo(span), name, init);
    }

    public static TypecaseClause makeTypecaseClause(Span span,
                                                    List<Type> matchType,
                                                    Block body) {
        return new TypecaseClause(makeSpanInfo(span), matchType, body);
    }

    public static Link makeLink(Span span,
                                FunctionalRef op, Expr expr) {
        return new Link(makeSpanInfo(span), op, expr);
    }

    public static _RewriteFnOverloadDecl make_RewriteFnOverloadDecl(Span span,
                                                                    IdOrOp overloading,
                                                                    List<IdOrOp> overloadings) {
        return new _RewriteFnOverloadDecl(makeSpanInfo(span), overloading, overloadings);
    }

    public static _RewriteObjectExprDecl make_RewriteObjectExprDecl(Span span,
                                                                    List<_RewriteObjectExpr> es) {
        return new _RewriteObjectExprDecl(makeSpanInfo(span), es);
    }

    public static _RewriteFunctionalMethodDecl make_RewriteFunctionalMethodDecl(Span span,
                                                                                List<String> names) {
        return new _RewriteFunctionalMethodDecl(makeSpanInfo(span), names);
    }

}
