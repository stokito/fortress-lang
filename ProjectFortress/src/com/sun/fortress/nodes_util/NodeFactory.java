/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nodes_util;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.Types;
import com.sun.fortress.nodes.*;
import com.sun.fortress.parser_util.FnHeaderClause;
import com.sun.fortress.parser_util.FnHeaderFront;
import com.sun.fortress.parser_util.precedence_resolver.PrecedenceMap;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.scala_src.useful.Sets;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;

import java.io.BufferedWriter;
import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;

public class NodeFactory {
    public static final int lexicalDepth = -2147483648;
    public static final Span internalSpan = makeSpan("Compiler internal generated.");
    public static final Span parserSpan = makeSpan("Parser generated.");
    public static final Span macroSpan = makeSpan("Syntactic abstraction generated.");
    public static final Span typeSpan = makeSpan("Type checker generated.");
    public static final Span desugarerSpan = makeSpan("Desugarer generated.");
    public static final Span interpreterSpan = makeSpan("Interpreter generated.");
    public static final Span shellSpan = makeSpan("Shell generated.");
    public static final Span unprinterSpan = makeSpan("Unprinter generated.");
    public static final Span testSpan = makeSpan("Test generated.");
    public static final Span repoSpan = makeSpan("Repository generated.");
    
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

    public static Span makeSpan(String file, int line, int startC, int endC) {
        SourceLoc start = new SourceLocRats(file, line, startC, 0);
        SourceLoc end = new SourceLocRats(file, line, endC, 0);
        return new Span(start, end);
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

    public static Component makeComponent(Span span, APIName name) {
        return makeComponent(span, name,
                             Collections.<Import>emptyList(),
                             Collections.<Decl>emptyList(),
                             false, Collections.<APIName>emptyList());
    }

    public static Component makeComponent(Span span, APIName name,
                                          List<Import> imports,
                                          List<Decl> decls,
                                          boolean isNative,
                                          List<APIName> exports) {
        return makeComponent(span, name, imports, decls, Collections.<APIName>emptyList(),
                             isNative, exports);
    }

    public static Component makeComponent(Span span, APIName name,
                                          List<Import> imports,
                                          List<Decl> decls,
                                          List<APIName> comprises,
                                          boolean isNative,
                                          List<APIName> exports) {
        return new Component(makeSpanInfo(span), name, imports, decls, comprises,
                             isNative, exports);
    }

    public static Api makeApi(Span span, APIName name) {
        return makeApi(span, name,
                       Collections.<Import>emptyList(),
                       Collections.<Decl>emptyList());
    }

    public static Api makeApi(Span span, APIName name,
                              List<Import> imports,
                              List<Decl> decls) {
        return makeApi(span, name, imports, decls, Collections.<APIName>emptyList());
    }

    public static Api makeApi(Span span, APIName name,
                              List<Import> imports,
                              List<Decl> decls,
                              List<APIName> comprises) {
        return new Api(makeSpanInfo(span), name, imports, decls, comprises);
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
        return makeAliasedSimpleName(NodeUtil.spanTwo(name, alias), name,
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
        return makeAliasedAPIName(NodeUtil.spanTwo(api, alias), api,
                                  Option.<Id>some(alias));
    }

    public static AliasedAPIName makeAliasedAPIName(Span span, APIName api,
                                                    Option<Id> alias) {
        return new AliasedAPIName(makeSpanInfo(span), api, alias);
    }

    public static TraitDecl makeTraitDecl(Span span, Modifiers mods, Id name,
                                          List<StaticParam> sparams,
                                          Option<List<Param>> params,
                                          List<TraitTypeWhere> extendsC,
                                          Option<WhereClause> whereC,
                                          List<Decl> decls,
                                          List<BaseType> excludesC,
                                          Option<List<NamedType>> comprisesC,
                                          boolean comprisesEllipses,
                                          Option<SelfType> selfType) {
        TraitTypeHeader header = makeTraitTypeHeader(mods, name, sparams, whereC,
                                                     Option.<List<Type>>none(),
                                                     Option.<Contract>none(),
                                                     extendsC, params, decls);
        return makeTraitDecl(makeSpanInfo(span), header, excludesC, comprisesC,
                             comprisesEllipses,selfType);
    }

    public static TraitDecl makeTraitDecl(ASTNodeInfo info, TraitTypeHeader header,
                                          List<BaseType> excludesC,
                                          Option<List<NamedType>> comprisesC,
                                          boolean comprisesEllipses,
                                          Option<SelfType> selfType) {
        return new TraitDecl(info, header, selfType, excludesC, comprisesC, comprisesEllipses);
    }

    public static ObjectDecl makeObjectDecl(Span span, Id name,
                                            List<TraitTypeWhere> extendsC,
                                            List<Decl> decls,
                                            Option<SelfType> selfType) {
        return makeObjectDecl(span, Modifiers.None, name,
                              Collections.<StaticParam>emptyList(),
                              extendsC, Option.<WhereClause>none(), decls,
                              Option.<List<Param>>none(),
                              Option.<List<Type>>none(),
                              Option.<Contract>none(),
                              selfType);
    }

    public static ObjectDecl makeObjectDecl(Span span, Id name,
                                            Option<List<Param>> params,
                                            Option<SelfType> selfType) {
        return makeObjectDecl(span, Modifiers.None, name,
                              Collections.<StaticParam>emptyList(),
                              Collections.<TraitTypeWhere>emptyList(),
                              Option.<WhereClause>none(),
                              Collections.<Decl>emptyList(), params,
                              Option.<List<Type>>none(),
                              Option.<Contract>none(),
                              selfType);
    }

    public static ObjectDecl makeObjectDecl(Span span, Id name,
                                            List<StaticParam> sparams,
                                            List<TraitTypeWhere> extendsC,
                                            List<Decl> decls,
                                            Option<List<Param>> params,
                                            Option<SelfType> selfType) {
        return makeObjectDecl(span, Modifiers.None, name,
                              sparams, extendsC,
                              Option.<WhereClause>none(), decls,
                              params,
                              Option.<List<Type>>none(),
                              Option.<Contract>none(),selfType);
    }

    public static ObjectDecl makeObjectDecl(Span span, Modifiers mods, Id name,
                                            List<StaticParam> sparams,
                                            List<TraitTypeWhere> extendsC,
                                            Option<WhereClause> whereC,
                                            List<Decl> decls,
                                            Option<List<Param>> params,
                                            Option<List<Type>> throwsC,
                                            Option<Contract> contract,
                                            Option<SelfType> selfType) {
        TraitTypeHeader header = makeTraitTypeHeader(mods, name, sparams, whereC,
                                                     throwsC, contract, extendsC,
                                                     params, decls);
        return new ObjectDecl(makeSpanInfo(span), header, selfType);
    }

    public static FnDecl mkFnDecl(Span span, Modifiers mods,
                                  FnHeaderFront fhf, FnHeaderClause fhc) {
        Option<List<Type>> throws_ = fhc.getThrowsClause();
        Option<WhereClause> where_ = fhc.getWhereClause();
        Option<Contract> contract = fhc.getContractClause();
        return makeFnDecl(span, mods, fhf.getName(),
                          fhf.getStaticParams(), fhf.getParams(),
                          fhc.getReturnType(), throws_, where_,
                          contract);
    }


    public static FnDecl mkFnDecl(Span span, Modifiers mods,
                                  IdOrOpOrAnonymousName name, List<StaticParam> sparams,
                                  List<Param> params,
                                  FnHeaderClause fhc) {
        Option<List<Type>> throws_ = fhc.getThrowsClause();
        Option<WhereClause> where_ = fhc.getWhereClause();
        Option<Contract> contract = fhc.getContractClause();
        return makeFnDecl(span, mods, name, sparams, params,
                          Option.<Type>none(), throws_, where_, contract);
    }

    public static FnDecl mkFnDecl(Span span, Modifiers mods,
                                  IdOrOpOrAnonymousName name, List<Param> params,
                                  Type ty) {
        return makeFnDecl(span, mods, name,
                          Collections.<StaticParam>emptyList(),
                          params, Option.<Type>some(ty));
    }

    public static FnDecl mkFnDecl(Span span, Modifiers mods,
                                  FnHeaderFront fhf,
                                  FnHeaderClause fhc, Expr expr) {
        Option<List<Type>> throws_ = fhc.getThrowsClause();
        Option<WhereClause> where_ = fhc.getWhereClause();
        Option<Contract> contract = fhc.getContractClause();
        return makeFnDecl(span, mods, fhf.getName(),
                          fhf.getStaticParams(), fhf.getParams(),
                          fhc.getReturnType(), throws_, where_,
                          contract, Option.<Expr>some(expr));
    }

    public static FnDecl mkFnDecl(Span span, Modifiers mods, IdOrOpOrAnonymousName name,
                                  List<StaticParam> sparams, List<Param> params,
                                  FnHeaderClause fhc, Option<Expr> expr) {
        Option<List<Type>> throws_ = fhc.getThrowsClause();
        Option<WhereClause> where_ = fhc.getWhereClause();
        Option<Contract> contract = fhc.getContractClause();
        return makeFnDecl(span, mods, name,
                          sparams, params, Option.<Type>none(),
                          throws_, where_, contract, expr);
    }

    public static FnDecl makeFnDecl(Span span, Modifiers mods,
                                    IdOrOpOrAnonymousName name,
                                    List<StaticParam> staticParams,
                                    List<Param> params,
                                    Option<Type> returnType) {
        return makeFnDecl(span, mods, name, staticParams, params, returnType,
                          Option.<List<Type>>none(),
                          Option.<WhereClause>none(),
                          Option.<Contract>none());
    }

    public static FnDecl makeFnDecl(Span span, Modifiers mods,
                                    IdOrOpOrAnonymousName name,
                                    List<Param> params,
                                    Option<Type> returnType,
                                    Option<Expr> body) {
        return makeFnDecl(span,
                          mods,
                          name,
                          Collections.<StaticParam>emptyList(),
                          params, returnType,
                          Option.<List<Type>>none(),
                          Option.<WhereClause>none(),
                          Option.<Contract>none(),
                          body);
    }

    private static String uaName(String tag, Span span ) {
        String s = terseSpan(span);
        s = s.replace("/", "|");
        return s;
    }

    /**
     * @param span
     * @return
     */
    public static String terseSpan(Span span) {
        String s = span.toString();
        if (s.startsWith(ProjectProperties.FORTRESS_AUTOHOME)) {
            s = "_" + s.substring(ProjectProperties.FORTRESS_AUTOHOME.length());
        }
        return s;
    }

    public static FnDecl makeFnDecl(Span span, Modifiers mods,
                                    IdOrOpOrAnonymousName name,
                                    List<StaticParam> staticParams,
                                    List<Param> params,
                                    Option<Type> returnType,
                                    Option<List<Type>> throwsC,
                                    Option<WhereClause> whereC,
                                    Option<Contract> contract) {
        IdOrOp unambiguousName;
        if (name instanceof Op) {
            unambiguousName = makeOp((Op)name, uaName("OP", span));
        } else {
            unambiguousName = makeId(span, uaName("FN", span));
        }
        return makeFnDecl(span, mods, name, staticParams, params, returnType,
                          throwsC, whereC, contract, unambiguousName,
                          Option.<Expr>none(), Option.<IdOrOp>none());
    }

    public static FnDecl makeFnDecl(Span span, Modifiers mods,
                                    IdOrOpOrAnonymousName name,
                                    List<StaticParam> staticParams,
                                    List<Param> params,
                                    Option<Type> returnType,
                                    Option<List<Type>> throwsC,
                                    Option<WhereClause> whereC,
                                    Option<Contract> contract,
                                    Option<Expr> body) {
        IdOrOp unambiguousName;
        if (name instanceof Op) {
            unambiguousName = makeOp((Op)name, uaName("OP", span));
        } else {
            unambiguousName = makeId(span, uaName("FN", span));
        }
        return makeFnDecl(span, mods, name, staticParams, params, returnType,
                          throwsC, whereC, contract, unambiguousName,
                          body, Option.<IdOrOp>none());
    }

    public static FnHeader makeFnHeader(Modifiers mods,
                                        IdOrOpOrAnonymousName name,
                                        List<StaticParam> staticParams,
                                        Option<WhereClause> whereClause,
                                        Option<List<Type>> throwsClause,
                                        Option<Contract> contract,
                                        List<Param> params,
                                        Option<Type> returnType) {
        return new FnHeader(staticParams, mods, name, whereClause,
                            throwsClause, contract, params, returnType);
    }

    public static FnDecl makeFnDecl(Span span,
                                    Modifiers mods,
                                    IdOrOpOrAnonymousName name,
                                    List<StaticParam> staticParams,
                                    List<Param> params,
                                    Option<Type> returnType,
                                    Option<List<Type>> throwsC,
                                    Option<WhereClause> whereC,
                                    Option<Contract> contract,
                                    IdOrOp unambiguousName,
                                    Option<Expr> body,
                                    Option<IdOrOp> implementsUnambiguousName) {
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

    public static LValue makeLValue(Span span, Id name, Option<TypeOrPattern> type) {
        return makeLValue(span, name, Modifiers.None, type, false);
    }

    public static LValue makeLValue(Span span, Id id, TypeOrPattern ty) {
        return makeLValue(span, id, Option.<TypeOrPattern>some(ty));
    }

    public static LValue makeLValue(Span span, Id id) {
        return makeLValue(span, id, Option.<TypeOrPattern>none());
    }

    public static LValue makeLValue(Id id) {
        return makeLValue(NodeUtil.getSpan(id), id);
    }

    public static LValue makeLValue(Id name, Id type) {
        return makeLValue(name, makeVarType(NodeUtil.getSpan(type),type),
                          Modifiers.None);
    }

    public static LValue makeLValue(Id id, TypeOrPattern ty) {
        return makeLValue(id, ty, Modifiers.None);
    }

    public static LValue makeLValue(Id id, TypeOrPattern ty, Modifiers mods) {
        return makeLValue(NodeUtil.getSpan(id), id, mods, Option.<TypeOrPattern>some(ty),
                          mods.isMutable());
    }

    public static LValue makeLValue(String name, TypeOrPattern type) {
        Span span = NodeUtil.getSpan(type);
        return makeLValue(span, makeId(span, name), type);
    }

    public static LValue makeLValue(String name, TypeOrPattern type, Modifiers mods) {
        return makeLValue(makeId(NodeUtil.getSpan(type), name), type, mods);
    }

    public static LValue makeLValue(Span span, String name, String type) {
        return makeLValue(makeId(span, name), makeVarType(span, type));
    }

    //
    public static LValue makeLValue(LValue lvb, Modifiers mods,
                                    Option<TypeOrPattern> ty, boolean mutable) {
        return makeLValue(NodeUtil.getSpan(lvb), lvb.getName(), mods, ty, mutable);
    }

    //
    public static LValue makeLValue(LValue lvb, Modifiers mods,
                                    boolean mutable) {
        return makeLValue(NodeUtil.getSpan(lvb), lvb.getName(), mods,
                          lvb.getIdType(), mutable);
    }

    //
    public static LValue makeLValue(LValue lvb, Id name) {
        return makeLValue(NodeUtil.getSpan(lvb), name, lvb.getMods(),
                          lvb.getIdType(), lvb.isMutable());
    }

    public static LValue makeLValue(Param param) {
        return makeLValue(NodeUtil.getSpan(param), param.getName(), param.getMods(),
                          param.getIdType(), false);
    }

    public static LValue makeLValue(Span span, Id name, Modifiers mods,
                                    Option<TypeOrPattern> type, boolean mutable) {
        return new LValue(makeSpanInfo(span), name, mods, type, mutable);
    }

    public static Param bogusParam(Span span) {
        return makeParam(bogusId(span));
    }

    public static Param makeParam(Span span, Modifiers mods, Id name,
                                  TypeOrPattern type) {
        return makeParam(span, mods, name, Option.<TypeOrPattern>some(type));
    }

    public static Param makeParam(Span span, Modifiers mods, Id name,
                                  Option<TypeOrPattern> type) {
        return makeParam(span, mods, name, type,
                         Option.<Expr>none(), Option.<Type>none());
    }

    public static Param makeParam(Id name) {
        return makeParam(name, Option.<TypeOrPattern>none());
    }

    public static Param makeParam(Id id, TypeOrPattern type) {
        return makeParam(NodeUtil.getSpan(id), Modifiers.None, id,
                         Option.<TypeOrPattern>some(type));
    }

    public static Param makeParam(Id id, Option<TypeOrPattern> type) {
        return makeParam(NodeUtil.getSpan(id), Modifiers.None, id, type);
    }

    public static Param makeParam(Span span, Id id, Option<TypeOrPattern> type) {
        return makeParam(span, Modifiers.None, id, type);
    }

    public static Param makeParam(Param param, Expr expr) {
        return makeParam(NodeUtil.getSpan(param), param.getMods(), param.getName(),
                         param.getIdType(), Option.<Expr>some(expr),
                         param.getVarargsType());
    }

    public static Param makeParam(Param param, TypeOrPattern type) {
        return makeParam(NodeUtil.getSpan(param), param.getMods(), param.getName(),
                         Option.<TypeOrPattern>some(type), param.getDefaultExpr(),
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
                         Option.<TypeOrPattern>none(), Option.<Expr>none(),
                         Option.<Type>some(type));
    }

    public static Param makeVarargsParam(Param param, Modifiers mods) {
        return makeParam(NodeUtil.getSpan(param), mods, param.getName(),
                         Option.<TypeOrPattern>none(), Option.<Expr>none(),
                         param.getVarargsType());
    }

    public static Param makeVarargsParam(Span span, Modifiers mods,
                                         Id name, Type type) {
        return makeParam(span, mods, name,
                         Option.<TypeOrPattern>none(), Option.<Expr>none(),
                         Option.<Type>some(type));
    }

    public static Param makeAbsParam(TypeOrPattern type) {
        return makeParam(NodeUtil.getSpan(type), Modifiers.None,
                         makeId(NodeUtil.getSpan(type), "_"), type);
    }

    public static Param makeParam(Span span, Modifiers mods, Id name,
                                  Option<TypeOrPattern> type, Option<Expr> expr,
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

    public static Pattern makePattern(Span span, Type type, PatternArgs args) {
        return makePattern(span, Option.<Type>some(type), args);
    }

    public static Pattern makePattern(Span span, PatternArgs args) {
        return makePattern(span, Option.<Type>none(), args);
    }

    public static Pattern makePattern(Span span, Option<Type> type, PatternArgs args) {
        return new Pattern(makeSpanInfo(span), type, args);
    }

    public static PatternArgs makePatternArgs(Span span) {
        return makePatternArgs(span, Collections.<PatternBinding>emptyList());
    }

    public static PatternArgs makePatternArgs(Span span,
                                              List<PatternBinding> patterns) {
        return new PatternArgs(makeSpanInfo(span), patterns);
    }

    public static PlainPattern makePlainPattern(Span span, Id name, Modifiers mods,
                                                Option<TypeOrPattern> type) {
        return makePlainPattern(span, Option.<Id>none(), name, mods, type);
    }

    public static PlainPattern makePlainPattern(Span span, Option<Id> field,
                                                Id name, Modifiers mods,
                                                Option<TypeOrPattern> type) {
        return new PlainPattern(makeSpanInfo(span), field, name, mods, type);
    }

    public static TypePattern makeTypePattern(Span span, Type type) {
        return makeTypePattern(span, Option.<Id>none(), type);
    }

    public static TypePattern makeTypePattern(Span span, Option<Id> field, Type type) {
        return new TypePattern(makeSpanInfo(span), field, type);
    }

    public static NestedPattern makeNestedPattern(Span span, Pattern pattern) {
        return makeNestedPattern(span, Option.<Id>none(), pattern);
    }

    public static NestedPattern makeNestedPattern(Span span, Option<Id> field, Pattern pattern) {
        return new NestedPattern(makeSpanInfo(span), field, pattern);
    }

    public static PatternBinding makePatternKeyword(final Span span, PatternBinding pb, Id name) {
        final Option<Id> field = Option.<Id>some(name);
        return pb.accept(new NodeAbstractVisitor<PatternBinding>() {
                public PatternBinding forPlainPattern(PlainPattern pp) {
                    return makePlainPattern(span, field, pp.getName(),
                                            pp.getMods(), pp.getIdType());
                }
                public PatternBinding forTypePattern(TypePattern tp) {
                    return makeTypePattern(span, field, tp.getTyp());
                }
                public PatternBinding forNestedPattern(NestedPattern np) {
                    return makeNestedPattern(span, field, np.getPat());
                }
            });
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
                                          Effect effect,
                                          Option<MethodInfo> methodInfo) {
        return makeArrowType(span, false, domain, range, effect,
                             Collections.<StaticParam>emptyList(),
                             Option.<WhereClause>none(), methodInfo);
    }

//     public static ArrowType makeArrowType(Span span, Type domain, Type range,
//                                           Effect effect) {
//         return makeArrowType(span, false, domain, range, effect,
//                              Collections.<StaticParam>emptyList(),
//                              Option.<WhereClause>none());
//     }

    public static ArrowType makeArrowType(Span span, Type domain, Type range) {
        return makeArrowType(span, false, domain, range,
                             makeEffect(NodeUtil.getSpan(range).getEnd()),
                             Collections.<StaticParam>emptyList(),
                             Option.<WhereClause>none());
    }

    public static ArrowType makeArrowType(Span span, Type domain, Type range, List<StaticParam> sparams) {
	return makeArrowType(span, false, domain, range,
			     makeEffect(NodeUtil.getSpan(range).getEnd()),
			     sparams, Option.<WhereClause>none());
    }

    public static ArrowType makeArrowType(Span span, boolean parenthesized,
                                          Type domain, Type range, Effect effect,
                                          List<StaticParam> sparams,
                                          Option<WhereClause> where) {
        TypeInfo info = makeTypeInfo(span, parenthesized, sparams, where);
        return new ArrowType(info, domain, range, effect, false, Option.<MethodInfo>none());
    }

    public static ArrowType makeArrowType(Span span, boolean parenthesized,
                                          Type domain, Type range, Effect effect,
                                          List<StaticParam> sparams,
                                          Option<WhereClause> where,
                                          Option<MethodInfo> methodInfo) {
        TypeInfo info = makeTypeInfo(span, parenthesized, sparams, where);
        return new ArrowType(info, domain, range, effect, false, methodInfo);
    }

    public static Type makeMaybeTupleType(Span span, List<Type> elements) {
        if ( elements.size() > 1 )
            return makeTupleType(span, elements);
        else if ( elements.size() == 1 )
            return elements.get(0);
        else
            return makeVoidType(span);
    }

    public static TupleType makeTupleType(TupleType t, List<Type> tys) {
        return makeTupleType(NodeUtil.getSpan(t), tys);
    }

    public static Type makeTupleTypeOrType(TupleType t, List<Type> tys) {
        return makeTupleTypeOrType(NodeUtil.getSpan(t), tys);
    }

    public static TupleType makeTupleType(List<Type> elements) {
        return makeTupleType(NodeUtil.spanAll(elements), elements);
    }

    public static Type makeTupleTypeOrType(Span span, List<Type> elements) {
            if (elements.size() == 1)
                return elements.get(0);
            return makeTupleType(span, elements);
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

    public static TupleType makeVoidType(Span span) {
        return makeTupleType(span, false, Collections.<Type>emptyList(),
                             Option.<Type>none(),
                             Collections.<KeywordType>emptyList());
    }

    public static TupleType makeVarargsType(Span span, Type ty) {
        return makeTupleType(span, false, Collections.<Type>emptyList(),
                             Option.<Type>some(ty),
                             Collections.<KeywordType>emptyList());
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

    public static ObjectExprType makeObjectExprType(List<BaseType> extended) {
        return makeObjectExprType(NodeUtil.spanAll(extended), false, extended);
    }

    public static ObjectExprType makeObjectExprType(Span span, boolean parenthesized,
                                                    List<BaseType> extended) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new ObjectExprType(info, extended);
    }

    public static SelfType makeSelfType(TraitType self) {
        return makeTraitSelfType(NodeUtil.getSpan(self), false,
                                 self, Collections.<NamedType>emptyList());
    }

    public static SelfType makeSelfType(TraitType self, List<NamedType> comprised) {
        return makeTraitSelfType(NodeUtil.getSpan(self), false, self, comprised);
    }

    public static SelfType makeTraitSelfType(Span span, boolean parenthesized,
                                             BaseType self, List<NamedType> comprised) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new TraitSelfType(info, self, comprised);
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

    public static TraitType makeTraitTypeForScala(Id name,
                                          List<StaticArg> sargs) {
        return makeTraitType(NodeUtil.getSpan(name), false, name, sargs, Collections.<StaticParam>emptyList());
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

    /**
     * Non-standard -- stashes the params into the type-info, leaving the
     * sparams field empty.  (or maybe not)
     * 
     * @param name
     * @param sargs
     * @param sparams
     * @return
     */
    public static TraitType makeTraitType(
            Id name, List<StaticArg> sargs,
            List<StaticParam> sparams) {
        TypeInfo info = makeTypeInfo(NodeUtil.getSpan(name), false, sparams, Option.<WhereClause>none());
        return new TraitType(info, name, sargs, Collections.<StaticParam>emptyList()); // sparams);
    }

    // used in testing, and in makeLValue and makeTypeParam (in this file)
    public static VarType makeVarType(Span span, String string) {
        return makeVarType(span, makeId(span, string));
    }


    public static VarType makeVarType(Span span, Id id) {
        return makeVarType(span, false, id, lexicalDepth);
    }
    
    public static VarType makeVarType(Span span, Id id, List<StaticParam> lsp) {
        TypeInfo info =   makeTypeInfo(span, false, lsp, Option.<WhereClause>none());
        return new VarType(info, id, lexicalDepth);
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
        return makeUnionType(NodeUtil.spanTwo(t1, t2), false,
                             Arrays.asList(t1, t2));
    }

    public static UnionType makeUnionType(Set<? extends Type> types){
        Span span;
        if ( types.isEmpty() )
            span = typeSpan;
        else
            span = NodeUtil.spanAll(types);
        return makeUnionType(span, false,
                             CollectUtil.makeList(types));
    }

    /** Return either a single type or an union depending on the set. */
    public static Type makeMaybeUnionType(Iterable<? extends Type> types) {
        int size = IterUtil.sizeOf(types, 2);
        if (size == 0) {
            return Types.BOTTOM;
        }
        if (size == 1) {
            return types.iterator().next();
        }
        Span span = NodeUtil.spanAll(types);
        return makeUnionType(span, false, CollectUtil.makeList(types));
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
        // Doing this so we can dodge a type error at the Scala/Java interface.
        Id bogus = makeId(span, id.toString());
        return new _InferenceVarType(info, bogus, id);
    }
    
    public static _InferenceVarOp make_InferenceVarOp(Span span) {
        return new _InferenceVarOp(makeASTNodeInfo(span), Option.<APIName>none(), "Inference", unknownFix, false, new Object());
    }

    public static TaggedUnitType makeTaggedUnitType(TaggedUnitType t, Type s) {
        return makeTaggedUnitType(NodeUtil.getSpan(t), NodeUtil.isParenthesized(t),
                                  s, t.getUnitExpr());
    }

    public static TaggedUnitType makeTaggedUnitType(Span span, boolean parenthesized,
                                                    Type elem, Expr unit) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return new TaggedUnitType(info, elem, unit);
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
        return makeIntersectionType(NodeUtil.spanTwo(t1, t2), false,
                                    Arrays.asList(t1, t2));
    }

    public static IntersectionType makeIntersectionType(Set<? extends Type> types) {
        Span span;
        if ( types.isEmpty() )
            span = typeSpan;
        else
            span = NodeUtil.spanAll(types);
        return makeIntersectionType(span, false, CollectUtil.makeList(types));
    }

    /** Return either a single type or an intersection depending on the set. */
    public static Type makeMaybeIntersectionType(Iterable<? extends Type> types) {
        int size = IterUtil.sizeOf(types, 2);
        if (size == 0) {
            return Types.ANY;
        }
        if (size == 1) {
            return types.iterator().next();
        }
        Span span = NodeUtil.spanAll(types);
        return makeIntersectionType(span, false, CollectUtil.makeList(types));
    }

    public static IntersectionType makeIntersectionType(List<? extends Type> types) {
        Span span;
        if ( types.isEmpty() )
            span = typeSpan;
        else
            span = NodeUtil.spanAll(types);

        return makeIntersectionType(span, false, new ArrayList<Type>(types));
    }

    public static IntersectionType makeIntersectionType(Span span,
                                                        List<Type> elems) {
        return makeIntersectionType(span, false, elems);
    }

    public static IntersectionType makeIntersectionType(Span span, boolean parenthesized,
                                                        List<Type> elems) {
        TypeInfo info = makeTypeInfo(span, parenthesized);
        return makeIntersectionType(info, elems);
    }

    public static IntersectionType makeIntersectionType(TypeInfo info,
                                                        List<Type> elems) {
        return new IntersectionType(info, elems);
    }

    public static final Effect emptyEffect = makeEffect(makeSpan("singleton"));

    /** Create an "empty" effect at the given location. */
    public static Effect makeEffect(SourceLoc loc) {
        return makeEffect(new Span(loc, loc));
    }

    public static Effect makeEffect(Span span) {
        return makeEffect(span, Option.<List<Type>>none(), false);
    }

    public static Effect makeEffect(List<Type> throwsClause) {
        Span span;
        if ( throwsClause.isEmpty() ) {
            span = typeSpan;
            return makeEffect(span);
        } else {
            span = NodeUtil.spanAll(throwsClause);
            return makeEffect(span, Option.some(throwsClause), false);
        }
    }

    public static Effect makeEffect(SourceLoc defaultLoc, List<Type> throwsClause) {
        return makeEffect(NodeUtil.spanAll(defaultLoc, throwsClause),
                          Option.some(throwsClause), false);
    }

    public static Effect makeEffect(Option<List<Type>> throwsClause) {
        Span span;
        if ( throwsClause.isNone() )
            span = typeSpan;
        else {
            if ( throwsClause.unwrap().isEmpty() )
                span = typeSpan;
            else
                span = NodeUtil.spanAll(throwsClause.unwrap());
        }
        return makeEffect(span, throwsClause, false);
    }

    public static Effect makeEffect(SourceLoc defaultLoc, Option<List<Type>> throwsClause) {
        Span span = NodeUtil.spanAll(defaultLoc,
                                         throwsClause.unwrap(Collections.<Type>emptyList()));
        return makeEffect(span, throwsClause, false);
    }

    public static Effect makeEffect(Span span, boolean ioEffect) {
        return makeEffect(span, Option.<List<Type>>none(), ioEffect);
    }


    public static Effect makeEffect(Span span, Option<List<Type>> throwsC,
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
            return makeTraitTypeWhere(new Span(NodeUtil.getSpan(in_type),
                                               NodeUtil.getSpan(in_where.unwrap())), in_type, in_where);
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

    public static Id makeDottedId(Id id) {
        if ( id.getApiName().isSome() ) {
            APIName apiName = id.getApiName().unwrap();
            String name = "";
            StringBuilder buf = new StringBuilder();
            for (Id n: apiName.getIds()) {
                buf.append(n.getText() + ".");
            }
            name = buf.toString();
            return makeId(NodeUtil.getSpan(id), name + id.getText());
        } else
            return id;
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
        Span span = NodeUtil.spanTwo(NodeUtil.spanAll(apiIds),
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
        return makeId(NodeUtil.spanTwo(api, name), Option.some(api),
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
        return makeAPIName(NodeUtil.spanTwo(first, last), ids);
    }

    public static APIName makeAPIName(Id first, Id rest) {
        List<Id> ids = new ArrayList<Id>();
        ids.add(first);
        if (rest.getApiName().isSome()) {
            ids.addAll(rest.getApiName().unwrap().getIds());
        }
        ids.add(makeId(NodeUtil.getSpan(rest), rest.getText()));
        ids = Useful.immutableTrimmedList(ids);
        return makeAPIName(NodeUtil.spanTwo(first, rest), ids);
    }

    public static APIName makeAPIName(Span span) {
        return makeAPIName(span, span.getBegin().getFileName());
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
            span = NodeUtil.spanTwo(NodeUtil.getSpan(id),
                                        NodeUtil.spanAll(ids));
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
                                   extendsC, header.getParams(), header.getDecls());
    }

    public static TraitTypeHeader makeTraitTypeHeader(TraitTypeHeader header,
                                                      List<TraitTypeWhere> extendsC,
                                                      Option<List<Param>> params) {
        return makeTraitTypeHeader(header.getMods(), header.getName(),
                                   header.getStaticParams(), header.getWhereClause(),
                                   header.getThrowsClause(), header.getContract(),
                                   extendsC, params, header.getDecls());
    }

    public static TraitTypeHeader makeTraitTypeHeaderWithDecls(TraitTypeHeader header,
                                                               List<Decl> decls) {
        return makeTraitTypeHeader(header, decls, header.getContract(), header.getParams());
    }

    public static TraitTypeHeader makeTraitTypeHeader(TraitTypeHeader header,
                                                      List<Decl> decls,
                                                      Option<Contract> contract,
                                                      Option<List<Param>> params) {
        return makeTraitTypeHeader(header.getMods(), header.getName(),
                                   header.getStaticParams(), header.getWhereClause(),
                                   header.getThrowsClause(), contract,
                                   header.getExtendsClause(), params, decls);
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
                                   Option.<List<Type>>none(), Option.<Contract>none(),
                                   extendsClause, Option.<List<Param>>none(), decls);
    }

    public static TraitTypeHeader makeTraitTypeHeader(Modifiers mods,
                                                      IdOrOpOrAnonymousName name,
                                                      List<StaticParam> staticParams,
                                                      Option<WhereClause> whereClause,
                                                      Option<List<Type>> throwsClause,
                                                      Option<Contract> contract,
                                                      List<TraitTypeWhere> extendsClause,
                                                      Option<List<Param>> params,
                                                      List<Decl> decls) {
        return new TraitTypeHeader(staticParams, mods, name, whereClause,
                                   throwsClause, contract, extendsClause,
                                   params, decls);
    }

    public static AnyType makeAnyType(Span span) {
        return new AnyType(makeTypeInfo(span));
    }

    public static BottomType makeBottomType(Span span) {
        return makeBottomType(makeTypeInfo(span));
    }

    public static BottomType makeBottomType(TypeInfo info) {
        return new BottomType(info);
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
        return makeKeywordType(NodeUtil.spanTwo(name,type), name, type);
    }

    public static KeywordType makeKeywordType(Span span, Id name, Type type) {
        return new KeywordType(makeSpanInfo(span), name, type);
    }

    public static TypeArg makeTypeArg(Span span, Type t) {
        return new TypeArg(makeSpanInfo(span), t);
    }

    public static TypeArg makeTypeArg(Span span, Type t, boolean lifted) {
        return new TypeArg(makeSpanInfo(span), lifted, t);
    }

    public static TypeArg makeTypeArg(TypeArg t, Type s) {
        return makeTypeArg(NodeUtil.getSpan(t), s, t.isLifted());
    }

    public static TypeArg makeTypeArg(Type ty) {
        return makeTypeArg(NodeUtil.getSpan(ty), ty);
    }

    public static TypeArg makeTypeArg(Span span, String string) {
        return makeTypeArg(span, makeVarType(span, makeId(span, string)));
    }

    public static DimArg makeDimArg(DimArg t, DimExpr s) {
        return makeDimArg(NodeUtil.getSpan(t), s, t.isLifted());
    }

    public static DimArg makeDimArg(DimExpr s) {
        return makeDimArg(NodeUtil.getSpan(s), s);
    }

    public static DimArg makeDimArg(Span span, DimExpr d) {
        return new DimArg(makeSpanInfo(span), d);
    }

    public static DimArg makeDimArg(Span span, DimExpr d, boolean lifted) {
        return new DimArg(makeSpanInfo(span), lifted, d);
    }
    
    public static OpArg makeOpArg(Span span, Op op){
        return new OpArg(makeSpanInfo(span), false, op);
    }
    
    public static OpArg makeOpArg(Span span, Op op, boolean lifted){
        return new OpArg(makeSpanInfo(span), lifted, op);
    }
    
    public static OpArg makeOpArg(Span span, String text){
        return new OpArg(makeSpanInfo(span), false, makeOp(span, text));
    }
    
    public static UnitArg makeUnitArg(UnitExpr s) {
        return makeUnitArg(NodeUtil.getSpan(s), s);
    }

    public static UnitArg makeUnitArg(Span span, UnitExpr u) {
        return new UnitArg(makeSpanInfo(span), u);
    }

    public static UnitArg makeUnitArg(Span span, UnitExpr u, boolean lifted) {
        return new UnitArg(makeSpanInfo(span), lifted, u);
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

    public static APIName makeAPINameFromPath(BufferedWriter writer,
                                              Span span, String path, String delimiter) {
        List<Id> ids = new ArrayList<Id>();
        String file = new File(path).getName();
        if (file.length() <= 4) {
            return error(makeId(span, "_"), "Invalid file name.");
        }
        for (String n : file.substring(0, file.length()-4).split(delimiter)) {
            // writer == null for NodeFactoryJUTest and ImportCollector
            if ( writer != null && ! NodeUtil.validId(n) )
                NodeUtil.log(writer, span,
                             n + " is not a valid Fortress identifier.");
            ids.add(makeId(span, n));
        }
        ids = Useful.immutableTrimmedList(ids);
        return makeAPIName(span, ids);
    }

    public static Op makeEnclosing(Span in_span, String in_open, String in_close) {
        return makeOp(in_span, Option.<APIName>none(),
                      PrecedenceMap.ONLY.canon(in_open + "_" + in_close),
                      enclosing, true);
    }

    public static Op makeEnclosing(Span in_span, String in_open, String in_close, boolean subscript, boolean assignment) {
	String name = PrecedenceMap.ONLY.canon((subscript ? "_" : "") + in_open + "_" + in_close + (assignment ? ":=" : ""));
	return makeOp(in_span, Option.<APIName>none(), name, enclosing, true);
    }

    public static Op makeOp(Span span, Option<APIName> api,
                            String text, Fixity fixity, boolean b) {
        return new NamedOp(makeSpanInfo(span), api, text, fixity, b);
    }

    // All of these should go away, except for the gross overhead of allocating separate items.
    public static Fixity infix = new InFixity();
    public static Fixity prefix = new PreFixity();
    public static Fixity postfix = new PostFixity();
    public static Fixity nofix = new NoFixity();
    public static Fixity multifix = new MultiFixity();
    public static Fixity enclosing = new EnclosingFixity();
    public static Fixity big = new BigFixity();
    public static Fixity unknownFix = new UnknownFixity();

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
        Op op =  makeOp(span, Option.some(makeAPIName(span,apiName)),
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

    public static Op makeOp(Option<APIName> api, Op op) {
        return makeOp(NodeUtil.getSpan(op), api,
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
            String left  = _op.split("_")[0];
            String right = "BIG " + _op.substring(left.length()+1);
            left  = "BIG " + left;
            return makeEnclosing(NodeUtil.getSpan(op), left, right);
        } else
            return makeOp(NodeUtil.getSpan(op), Option.<APIName>none(),
                          PrecedenceMap.ONLY.canon("BIG " + op.getText()), big, op.isEnclosing() );
    }

    public static StaticParam makeStaticParamId(BufferedWriter writer, Span span,
                                                String variance, IdOrOp name, StaticParamKind k) {
        return makeStaticParamId(writer, span, variance, name,
                                 Collections.<BaseType>emptyList(), Collections.<BaseType>emptyList(),
                                 Option.<Type>none(), false, k);
    }

    public static StaticParam makeStaticParamId(BufferedWriter writer, Span span,
                                                String variance, IdOrOp name, List<BaseType> tys, List<BaseType> doms,
                                                Option<Type> ty, boolean b,
                                                StaticParamKind k) {
        if ( name instanceof Id )
            return makeStaticParam(span, variance, name, tys, doms, ty, b, k);
        else {
            NodeUtil.log(writer, span,
                         name + " is not a valid static prameter name.");
            return makeStaticParam(span, variance, bogusId(span), tys, doms, ty, b, k);
        }
    }
    
    public static StaticParam makeStaticParam(Span span,
    							                 String variance,
                                              IdOrOp name, List<BaseType> tys, List<BaseType> doms,
                                              Option<Type> ty, boolean b,
                                              StaticParamKind k) {
    	    int v = (variance == null ? 0 : variance.equals("covariant") ? 1 : variance.equals("contravariant") ? -1 : (Integer) bug(span,"Incorrect variance " + variance));
        return new StaticParam(makeSpanInfo(span), v, name, tys, doms, ty, b, k);
    }
    
    public static StaticParam makeStaticParam(StaticParam sp, Id name, List<BaseType> extendsClause) {
        return new StaticParam(sp.getInfo(), sp.getVariance(), name,
                extendsClause, Collections.<BaseType>emptyList(), sp.getDimParam(), sp.isAbsorbsParam(),
                sp.getKind()
                );
    }

    public static StaticParam makeTypeParam(Span span,
                                            Id id, List<BaseType> tys,
                                            Option<Type> ty, boolean b) {
        return makeStaticParam(span, null, id, tys, Collections.<BaseType>emptyList(),ty, b, new KindType());
    }

    public static StaticParam makeTypeParam(Span s, String name) {
        return makeStaticParam(s, null, makeId(s, name),
                               Collections.<BaseType>emptyList(),Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindType());
    }

    public static StaticParam makeTypeParam(Span s, String name, String sup) {
        List<BaseType> supers = new ArrayList<BaseType>(1);
        supers.add(makeVarType(s, sup));
        return makeStaticParam(s, null, makeId(s, name), supers,Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindType());
    }
    
    public static StaticParam makeOpParam(Span s, String name) {
        return makeStaticParam(s, null, makeOp(s, name),
                               Collections.<BaseType>emptyList(),Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindOp());
    }

    public static StaticParam makeBoolParam(Span s, String name) {
        return makeStaticParam(s, null, makeId(s, name),
                               Collections.<BaseType>emptyList(),Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindBool());
    }

    public static StaticParam makeDimParam(Span s, String name) {
        return makeStaticParam(s, null, makeId(s, name),
                               Collections.<BaseType>emptyList(),Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindDim());
    }

    public static StaticParam makeUnitParam(Span s, String name) {
        return makeStaticParam(s, null, makeId(s, name),
                               Collections.<BaseType>emptyList(),Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindUnit());
    }

    public static StaticParam makeIntParam(Span s, String name) {
        return makeStaticParam(s, null, makeId(s, name),
                               Collections.<BaseType>emptyList(),Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindInt());
    }

    public static StaticParam makeNatParam(Span s, String name) {
        return makeStaticParam(s, null, makeId(s, name),
                               Collections.<BaseType>emptyList(),Collections.<BaseType>emptyList(),
                               Option.<Type>none(), false,
                               new KindNat());
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

    public static BoolArg makeBoolArg(Span span, BoolExpr b, boolean lifted) {
        return new BoolArg(makeSpanInfo(span), lifted, b);
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

    public static IntArg makeIntArg(Span span, IntExpr i, boolean lifted) {
        return new IntArg(makeSpanInfo(span), lifted, i);
    }

/*
    public static OpArg makeOpArg(Span span, String string) {
        return makeOpArg(span, ExprFactory.makeOpRef(makeOp(span, string)));
    }

    public static OpArg makeOpArg(Span span, FunctionalRef op) {
        return new OpArg(makeSpanInfo(span), Option.some(op), Option.none());
    }

    public static OpArg makeOpArg(Span span, FunctionalRef op, boolean lifted) {
        return new OpArg(makeSpanInfo(span), lifted, Option.some(op), Option.none());
    }
*/
    public static VarDecl makeVarDecl(List<LValue> lvalues) {
        return makeVarDecl(NodeUtil.spanAll(lvalues), lvalues, Option.<Expr>none());
    }

    public static VarDecl makeVarDecl(Span span, List<LValue> lvals,
                                      Option<Expr> expr) {
        return new VarDecl(makeSpanInfo(span), lvals, expr);
    }

    public static VarDecl makeVarDecl(Span span, String name, Expr init) {
        Id id = makeId(span, name);
        LValue bind = new LValue(makeSpanInfo(span), id,
                                 Modifiers.None,
                                 Option.<TypeOrPattern>none(), false);
        return makeVarDecl(span, Useful.<LValue>list(bind),
                           Option.<Expr>some(init));
    }

    public static VarDecl makeVarDecl(BufferedWriter writer,
                                      Span span, List<LValue> lvals,
                                      Option<Expr> expr) {
        NodeUtil.validId(writer, lvals);
        return new VarDecl(makeSpanInfo(span), lvals, expr);
    }

    public static VarDecl makeVarDecl(BufferedWriter writer,
                                      Span span, List<LValue> lvals) {
        NodeUtil.validId(writer, lvals);
        return makeVarDecl(writer, span, lvals, Option.<Expr>none());
    }

    public static VarDecl makeVarDecl(BufferedWriter writer,
                                      Span span, List<LValue> lvals, Expr init) {
        NodeUtil.validId(writer, lvals);
        return makeVarDecl(writer, span, lvals, Option.<Expr>some(init));
    }

    public static VarDecl makeVarDecl(BufferedWriter writer,
                                      Span span, Id name, Expr init) {
        NodeUtil.validId(writer, name);
        LValue bind = new LValue(makeSpanInfo(span), name, Modifiers.None,
                                 Option.<TypeOrPattern>none(), true);
        return makeVarDecl(writer, span, Useful.<LValue>list(bind),
                           Option.<Expr>some(init));
    }

    public static VarDecl makeVarDecl(BufferedWriter writer,
                                      Span span, String name, Expr init) {
        Id id = makeId(span, name);
        NodeUtil.validId(writer, id);
        LValue bind = new LValue(makeSpanInfo(span), id,
                                 Modifiers.None,
                                 Option.<TypeOrPattern>none(), false);
        return makeVarDecl(writer, span, Useful.<LValue>list(bind),
                           Option.<Expr>some(init));
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
            public Type forTraitSelfType(TraitSelfType t) {
                return  makeTraitSelfType(NodeUtil.getSpan(t), true,
                                          t.getNamed(), t.getComprised());
            }
            public Type forObjectExprType(ObjectExprType t) {
                return  makeObjectExprType(NodeUtil.getSpan(t), true, t.getExtended());
            }
            public Type forTraitType(TraitType t) {
                return makeTraitType(NodeUtil.getSpan(t), true, t.getName(),
                                     t.getArgs(), t.getTraitStaticParams());
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
                                 _type.getTraitStaticParams());
        }
    }

    public static TraitType makeGenericSingletonType(Id name, List<StaticParam> params) {
        return makeTraitType(NodeUtil.getSpan(name), false, name,
                             Collections.<StaticArg>emptyList(), params);
    }

    public static WhereExtends makeWhereExtends(BufferedWriter writer, Span span,
                                                IdOrOp name, List<BaseType> supers) {
        if ( name instanceof Id )
            return new WhereExtends(makeSpanInfo(span), (Id)name, supers);
        else {
            NodeUtil.log(writer, span,
                         name + " is not a valid static parameter name.\n");
            return new WhereExtends(makeSpanInfo(span), bogusId(span), supers);
        }
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

    public static UnitConstraint makeUnitConstraint(BufferedWriter writer,
                                                    Span span, IdOrOp id) {
        if ( id instanceof Id )
            return new UnitConstraint(makeSpanInfo(span), (Id)id);
        else {
            NodeUtil.log(writer, span,
                         id + " is not a valid static parameter name.\n");
            return new UnitConstraint(makeSpanInfo(span), bogusId(span));
        }
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

    public static WhereBinding makeWhereBinding(BufferedWriter writer, Span span,
                                                IdOrOp name, StaticParamKind kind) {
        return makeWhereBinding(writer, span, name,
                                Collections.<BaseType>emptyList(), kind);
    }

    public static WhereBinding makeWhereBinding(BufferedWriter writer, Span span,
                                                IdOrOp name, List<BaseType> supers,
                                                StaticParamKind kind) {
        if ( name instanceof Id )
            return new WhereBinding(makeSpanInfo(span), (Id)name, supers, kind);
        else {
            NodeUtil.log(writer, span,
                         name + " is not a valid static parameter name.\n");
            return new WhereBinding(makeSpanInfo(span), bogusId(span), supers, kind);
        }
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
                                                    Option<Id> name,
                                                    TypeOrPattern matchType,
                                                    Block body) {
        return new TypecaseClause(makeSpanInfo(span), name, matchType, body);
    }

    public static Link makeLink(Span span,
                                FunctionalRef op, Expr expr) {
        return new Link(makeSpanInfo(span), op, expr);
    }

    public static _RewriteFnOverloadDecl make_RewriteFnOverloadDecl(Span span,
                                                                    IdOrOp overloading,
                                                                    List<IdOrOp> overloadings,
                                                                    Option<Type> t) {
        if (t.isNone()) {
          //   return bug("Overloading for " + overloading + " lacks type" );
        }
        return new _RewriteFnOverloadDecl((ASTNodeInfo) makeSpanInfo(span), overloading, overloadings, t);
    }

    public static _RewriteObjectExprDecl make_RewriteObjectExprDecl(Span span,
                                                                    List<_RewriteObjectExpr> es) {
        return new _RewriteObjectExprDecl(makeSpanInfo(span), es);
    }

    public static _RewriteFunctionalMethodDecl make_RewriteFunctionalMethodDecl(Span span,
                                                                                List<String> names) {
        return new _RewriteFunctionalMethodDecl(makeSpanInfo(span), names);
    }

    public static Overloading makeOverloading(
            ASTNodeInfo in_info, IdOrOp in_unambiguousName,
            IdOrOpOrAnonymousName in_originalName,  Option<ArrowType> in_type,
            Option<ArrowType> in_schema) {
        return new Overloading (in_info, in_unambiguousName, (IdOrOp) in_originalName,
                in_type, in_schema);
    }

    public static List<LValue> ids2Lvs(List<Id> ids) {
        List<LValue> lvs = new ArrayList<LValue>(ids.size());
        for (Id id : ids) {
            lvs.add(makeLValue(NodeUtil.getSpan(id), id, Modifiers.None,
                               Option.<TypeOrPattern>none(), false));
        }
        return lvs;
    }

    public static List<LValue> makeLvs(BufferedWriter writer, List<LValue> vars,
                                       Option<Modifiers> mods, boolean colonEqual) {
        List<LValue> lvs = new ArrayList<LValue>(vars.size());
        boolean mutable;
        if ( mods.isSome() )
            mutable = mods.unwrap().isMutable() || colonEqual;
        else
            mutable = colonEqual;
        for (LValue l : vars) {
            if ( mods.isSome() ) lvs.add(makeLValue(l, mods.unwrap(), mutable));
            else                 lvs.add(makeLValue(l, Modifiers.None, mutable));
        }
        NodeUtil.validId(writer, lvs);
        return lvs;
    }

    public static List<LValue> makeLvs(BufferedWriter writer, List<LValue> vars,
                                       Option<Modifiers> mods, Option<TypeOrPattern> ty,
                                       boolean colonEqual) {
        List<LValue> lvs = new ArrayList<LValue>(vars.size());
        boolean mutable;
        if ( mods.isSome() )
            mutable = mods.unwrap().isMutable() || colonEqual;
        else
            mutable = colonEqual;
        for (LValue l : vars) {
            if ( mods.isSome() ) lvs.add(makeLValue(l, mods.unwrap(), ty, mutable));
            else                 lvs.add(makeLValue(l, Modifiers.None, ty, mutable));
        }
        NodeUtil.validId(writer, lvs);
        return lvs;
    }

    public static List<LValue> makeLvs(BufferedWriter writer, List<LValue> vars,
                                       Option<Modifiers> mods, List<Type> tys,
                                       boolean colonEqual) {
        List<LValue> lvs = new ArrayList<LValue>(vars.size());
        boolean mutable;
        if ( mods.isSome() )
            mutable = mods.unwrap().isMutable() || colonEqual;
        else
            mutable = colonEqual;
        int ind = 0;
        for (LValue l : vars) {
            if ( mods.isSome() )
                lvs.add(makeLValue(l, mods.unwrap(),
                                   Option.<TypeOrPattern>some(tys.get(ind)), mutable));
            else
                lvs.add(makeLValue(l, Modifiers.None,
                                   Option.<TypeOrPattern>some(tys.get(ind)), mutable));
            ind += 1;
        }
        NodeUtil.validId(writer, lvs);
        return lvs;
    }

    public static FnHeaderClause makeFnClauses(BufferedWriter writer, Span span,
                                               List<FnHeaderClause> clauses) {
        Option<List<Type>> throwsC = Option.<List<Type>>none();
        Option<WhereClause> whereC = Option.<WhereClause>none();
        Option<List<Expr>> requiresC = Option.<List<Expr>>none();
        Option<List<EnsuresClause>> ensuresC = Option.<List<EnsuresClause>>none();
        Option<List<Expr>> invariantsC = Option.<List<Expr>>none();
        boolean seenThrows = false;
        boolean seenWhere = false;
        boolean seenRequires = false;
        boolean seenEnsures = false;
        boolean seenInvariants = false;
        for ( FnHeaderClause clause : clauses ) {
            if ( clause.getThrowsClause().isSome() ) {
                if ( seenThrows )
                    NodeUtil.log(writer, span,
                                 "Throws clause must not occur multiple times.");
                throwsC = clause.getThrowsClause();
                seenThrows = true;
                if ( seenWhere || seenRequires || seenEnsures || seenInvariants )
                    NodeUtil.log(writer, span,
                                 "Throws clauses should come before where/contract clauses.");
            } else if ( clause.getWhereClause().isSome() ) {
                if ( seenWhere )
                    NodeUtil.log(writer, NodeUtil.getSpan(clause.getWhereClause().unwrap()),
                                 "Where clause must not occur multiple times.");
                whereC = clause.getWhereClause();
                seenWhere = true;
                if ( seenRequires || seenEnsures || seenInvariants )
                    NodeUtil.log(writer, NodeUtil.getSpan(clause.getWhereClause().unwrap()),
                                 "Contract clauses should come after where clauses.");
            } else if ( clause.getContractClause().isSome() ) {
                Contract contract = clause.getContractClause().unwrap();
                if ( contract.getRequiresClause().isSome() ) {
                    if ( seenRequires )
                        NodeUtil.log(writer, NodeUtil.getSpan(clause.getContractClause().unwrap()),
                                     "Requires clause must not occur multiple times.");
                    requiresC = contract.getRequiresClause();
                    seenRequires = true;
                    if ( seenEnsures || seenInvariants )
                        NodeUtil.log(writer, NodeUtil.getSpan(clause.getWhereClause().unwrap()),
                                     "Requires clauses should come before ensures/invariants clauses.");
                } else if ( contract.getEnsuresClause().isSome() ) {
                    if ( seenEnsures )
                        NodeUtil.log(writer, NodeUtil.getSpan(clause.getContractClause().unwrap()),
                                     "Ensures clause must not occur multiple times.");
                    ensuresC = contract.getEnsuresClause();
                    seenEnsures = true;
                    if ( seenInvariants )
                        NodeUtil.log(writer, NodeUtil.getSpan(clause.getWhereClause().unwrap()),
                                     "Ensures clauses should come before invariants clauses.");
                } else if ( contract.getInvariantsClause().isSome() ) {
                    if ( seenInvariants )
                        NodeUtil.log(writer, NodeUtil.getSpan(clause.getContractClause().unwrap()),
                                     "Invariants clause must not occur multiple times.");
                    invariantsC = contract.getInvariantsClause();
                    seenInvariants = true;
                }
            }
        }
        Option<Contract> contractC;
        if ( requiresC.isNone() && ensuresC.isNone() && invariantsC.isNone() )
            contractC = Option.<Contract>none();
        else
            contractC = Option.<Contract>some(makeContract(span, requiresC,
                                                           ensuresC, invariantsC));
        return makeFnHeaderClause(throwsC, whereC, contractC, Option.<Type>none());
    }

    public static FnHeaderClause makeThrowsClause(Option<List<Type>> throwsC) {
        return makeFnHeaderClause(throwsC, Option.<WhereClause>none(),
                                  Option.<Contract>none(), Option.<Type>none());
    }

    public static FnHeaderClause makeWhereClause(Option<WhereClause> whereC) {
        return makeFnHeaderClause(Option.<List<Type>>none(), whereC,
                                  Option.<Contract>none(), Option.<Type>none());
    }

    public static FnHeaderClause makeRequiresClause(Option<List<Expr>> requiresC) {
        return makeFnHeaderClause(Option.<List<Type>>none(),
                                  Option.<WhereClause>none(),
                                  Option.<Contract>some(makeContract(parserSpan, requiresC,
                                                                     Option.<List<EnsuresClause>>none(),
                                                                     Option.<List<Expr>>none())),
                                  Option.<Type>none());
    }

    public static FnHeaderClause makeEnsuresClause(Option<List<EnsuresClause>> ensuresC) {
        return makeFnHeaderClause(Option.<List<Type>>none(),
                                  Option.<WhereClause>none(),
                                  Option.<Contract>some(makeContract(parserSpan, Option.<List<Expr>>none(),
                                                                     ensuresC, Option.<List<Expr>>none())),
                                  Option.<Type>none());
    }

    public static FnHeaderClause makeInvariantsClause(Option<List<Expr>> invariantsC) {
        return makeFnHeaderClause(Option.<List<Type>>none(),
                                  Option.<WhereClause>none(),
                                  Option.<Contract>some(makeContract(parserSpan,
                                                                     Option.<List<Expr>>none(),
                                                                     Option.<List<EnsuresClause>>none(),
                                                                     invariantsC)),
                                  Option.<Type>none());
    }

    public static FnHeaderClause makeFnHeaderClause(Option<List<Type>> throwsC,
                                                    Option<WhereClause> whereC,
                                                    Option<Contract> contractC,
                                                    Option<Type> ty) {
        return new FnHeaderClause(throwsC, whereC, contractC, ty);
    }

    private static boolean isNOT(FunctionalRef op) {
        IdOrOp name = op.getOriginalName();
        if ( name instanceof Op ) return name.getText().equals("NOT");
        else return false;
    }

    private static boolean isBoolBinaryOp(FunctionalRef op) {
        String[] all = new String[]{"OR", "AND", "->", "="};
        List<String> ops = new ArrayList<String>(Arrays.asList(all));
        IdOrOp name = op.getOriginalName();
        if ( name instanceof Op ) return ops.contains( name.getText() );
        else return false;
    }

    public static BoolExpr makeBoolExpr(final BufferedWriter writer, Expr expr) {
        return expr.accept(new NodeAbstractVisitor<BoolExpr>() {
            public BoolExpr forVarRef(VarRef e) {
                Span span = NodeUtil.getSpan(e);
                Id name = e.getVarId();
                if ( name.getText().equals("true") )
                    return makeBoolBase(span, false, true);
                else if ( name.getText().equals("false") )
                    return makeBoolBase(span, false, false);
                else
                    return makeBoolRef(span, name);
            }
            public BoolExpr forOpExpr(OpExpr e) {
                FunctionalRef op = e.getOp();
                Span span = NodeUtil.getSpan(e);
                Span opSpan = NodeUtil.getSpan(op);
                String name = op.getOriginalName().getText();
                List<Expr> args = e.getArgs();
                if ( args.size() == 1 && isNOT(op) ) {
                    return makeBoolUnaryOp(span, false, args.get(0).accept(this),
                                           makeOpPrefix(opSpan, name));
                } else if ( args.size() == 2 && isBoolBinaryOp(op) ) {
                    return makeBoolBinaryOp(span, args.get(0).accept(this),
                                            args.get(1).accept(this),
                                            makeOpInfix(opSpan, name));
                } else {
                    NodeUtil.log(writer, opSpan,
                                 "Invalid operator for boolean static parameters.");
                    return makeBoolBase(span, false, false);
                }
            }
            public BoolExpr forAmbiguousMultifixOpExpr(AmbiguousMultifixOpExpr e) {
                FunctionalRef op = e.getInfix_op();
                Span span = NodeUtil.getSpan(e);
                Span opSpan = NodeUtil.getSpan(op);
                String name = op.getOriginalName().getText();
                List<Expr> args = e.getArgs();
                if ( args.size() > 2 &&
                     // if an associative operator on boolean static parameters
                     ( name.equals("AND") || name.equals("OR") ) ) {
                    Op newOp = makeOpInfix(opSpan, name);
                    BoolExpr left = args.get(0).accept(this);
                    BoolExpr right = args.get(1).accept(this);
                    Span newSpan = NodeUtil.spanTwo(left, right);
                    BoolExpr result = makeBoolBinaryOp(newSpan, left, right, newOp);
                    for ( Expr _expr : args.subList(2, args.size()) ) {
                        left = result;
                        right = _expr.accept(this);
                        newSpan = NodeUtil.spanTwo(left, right);
                        result = makeBoolBinaryOp(newSpan, left, right, newOp);
                    }
                    return result;
                } else {
                    NodeUtil.log(writer, span,
                                 "Invalid operator for boolean static parameters.");
                    return makeBoolBase(span, false, false);
                }
            }
            public BoolExpr forChainExpr(ChainExpr e) {
                Span span = NodeUtil.getSpan(e);
                BoolExpr left = e.getFirst().accept(this);
                BoolExpr right;
                List<BoolExpr> bexprs = new ArrayList<BoolExpr>();
                for ( Link link : e.getLinks() ) {
                    right = link.getExpr().accept(this);
                    FunctionalRef op = link.getOp();
                    Span opSpan = NodeUtil.getSpan(op);
                    String name = op.getOriginalName().getText();
                    // if not a chainable operator on boolean static parameters
                    if ( ! name.equals("=") ) {
                        NodeUtil.log(writer, opSpan,
                                     "Invalid operator for boolean static parameters.");
                        return makeBoolBase(span, false, false);
                    }
                    bexprs.add(makeBoolBinaryOp(NodeUtil.spanTwo(left, right),
                                                left, right,
                                                makeOpInfix(opSpan, name)));
                    left = right;
                }
                if ( bexprs.isEmpty() ) {
                    NodeUtil.log(writer, span,
                                 "Invalid operator for boolean static parameters.");
                    return makeBoolBase(span, false, false);
                }
                BoolExpr result = bexprs.get(0);
                for ( BoolExpr be : bexprs.subList(1, bexprs.size()) ) {
                    Span newSpan = NodeUtil.spanTwo(result, be);
                    result = makeBoolBinaryOp(newSpan, result, be,
                                              makeOpInfix(newSpan, "AND"));
                }
                return result;
            }

        });
    }

    /**
     * Reallocates a block with the atomic flag jammed on.
     * @param block
     * @return
     */
    public static Block remakeAtomic(Block block) {
        block = new Block(block.getInfo(), block.getLoc(), true, block.isWithinDo(), block.getExprs());
        return block;
    }
    
    public static Id makeLocalId(Id fn) {
        return new Id(fn.getInfo(), Option.<APIName>none(), fn.getText());
    }
    public static Op makeLocalOp(Op fn) {
        return new NamedOp(fn.getInfo(), Option.<APIName>none(), fn.getText(), fn.getFixity(), fn.isEnclosing());
    }
    public static IdOrOp makeLocalIdOrOp(IdOrOp fn) {
        if (fn instanceof Id)
            return makeLocalId((Id)fn);
        else if (fn instanceof Op)
            return makeLocalOp((Op)fn);
        else
            return bug("Unexpected member of IdOrOp hierarchy " + fn.getClass());
    }

    public static UnknownType makeUnknownType() {
        TypeInfo info = makeTypeInfo(typeSpan, false);
        return new UnknownType(info);
    }

    public static Id makeLiftedCoercionId(Span span, Id trait) {
        return makeId(span, trait.getApiName(), NamingCzar.makeLiftedCoercionName(trait));
    }

    public static Id makeLiftedCoercionId(Span span, Id trait, APIName api) {
        return makeId(span, api, NamingCzar.makeLiftedCoercionName(trait));
    }

    public static IdOrOp makeIdOrOp(APIName a, IdOrOp originalName) {
        Option<APIName> oa = Option.some(a);
        if (originalName instanceof Id) {
            return makeId(oa, (Id) originalName);
        } else if (originalName instanceof Op) {
            return makeOp(oa, (Op) originalName);
        } else {
            return bug("Unexpected member of IdOrOp hierarchy " + originalName.getClass());
        }
    }
}
