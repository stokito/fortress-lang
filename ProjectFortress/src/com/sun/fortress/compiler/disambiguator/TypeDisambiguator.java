/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.disambiguator;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.HasAt;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.LambdaUtil;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

import java.util.*;

/**
 * <p>Eliminates ambiguities in types:
 * <ul>
 * <li>VarTypes (and 0-ary TraitTypes, if they exist) referencing type Any are
 * replaced by AnyTypes.</li>
 * <li>Type names referring to APIs are made fully qualified.</li>
 * <li>VarTypes referring to traits, objects, and aliases become TraitTypes
 * (with 0 arguments).</li>
 * <li>TypeArgs wrapping VarTypes corresponding to other kinds of parameters (like UnitParams)
 * are converted to the corresponding kind of arg (like UnitArg).</li>
 * <li>TODO: UnitArgs corresponding to DimParams are converted to DimArgs.</li>
 * <li>TODO: TaggedUnitTypes for types that absorb dimensions become TaggedDimTypes.</li>
 * </ul>
 * (TODO: Verify that the parser prefers producing units over dimensions in ambiguous cases.)
 * </p>
 * <p>All name references in resolved types that are undefined or used incorrectly are
 * treated as static errors.  Similarly, incorrect arity or kinds of static arguments
 * are treated as errors.  Malformed types (currently, just Domains that don't appear
 * as an arrow's domain) also cause errors.</p>
 */
public class TypeDisambiguator extends NodeUpdateVisitor {

    private final TypeNameEnv _env;
    private final Set<IdOrOpOrAnonymousName> _onDemandImports;
    private final List<StaticError> _errors;
    private Boolean forTypecaseClause = false;
    private Boolean rewriteTypecaseClause = false;

    public TypeDisambiguator(TypeNameEnv env, Set<IdOrOpOrAnonymousName> onDemandImports, List<StaticError> errors) {
        _env = env;
        _onDemandImports = onDemandImports;
        _errors = errors;
    }

    private void error(String msg, HasAt loc) {
        _errors.add(StaticError.make(msg, loc));
    }

    /**
     * Return a new TypeDisambiguator
     * that includes the given type variables in its environment.
     */
    private TypeDisambiguator extend(List<StaticParam> typeVars) {
        TypeNameEnv newEnv = new LocalStaticParamEnv(_env, typeVars);
        return new TypeDisambiguator(newEnv, _onDemandImports, _errors);
    }

    /**
     * When recurring on a TraitDecl, we first need to extend the
     * environment with all the newly bound static parameters.
     */
    @Override
    public Node forTraitDecl(final TraitDecl that) {
        TypeDisambiguator v = this.extend(NodeUtil.getStaticParams(that));
        TraitTypeHeader header =
                (TraitTypeHeader) forTraitTypeHeaderOnly(that.getHeader(),
                                                         v.recurOnListOfStaticParam(NodeUtil.getStaticParams(that)),
                                                         (Id) NodeUtil.getName(that).accept(v),
                                                         v.recurOnOptionOfWhereClause(NodeUtil.getWhereClause(that)),
                                                         Option.<List<Type>>none(),
                                                         Option.<Contract>none(),
                                                         v.recurOnListOfTraitTypeWhere(NodeUtil.getExtendsClause(that)),
                                                         v.recurOnOptionOfListOfParam(NodeUtil.getParams(that)),
                                                         v.recurOnListOfDecl(NodeUtil.getDecls(that)));
        return forTraitDeclOnly(that,
                                that.getInfo(),
                                header,
                                v.recurOnOptionOfSelfType(that.getSelfType()),
                                v.recurOnListOfBaseType(NodeUtil.getExcludesClause(that)),
                                v.recurOnOptionOfListOfNamedType(NodeUtil.getComprisesClause(that)));
    }

    /**
     * When recurring on an ObjectDecl, we first need to extend the
     * environment with all the newly bound static parameters.
     */
    @Override
    public Node forObjectDecl(final ObjectDecl that) {
        TypeDisambiguator v = this.extend(NodeUtil.getStaticParams(that));
        TraitTypeHeader header =
                (TraitTypeHeader) forTraitTypeHeaderOnly(that.getHeader(),
                                                         v.recurOnListOfStaticParam(NodeUtil.getStaticParams(that)),
                                                         (Id) NodeUtil.getName(that).accept(v),
                                                         v.recurOnOptionOfWhereClause(NodeUtil.getWhereClause(that)),
                                                         Option.<List<Type>>none(),
                                                         Option.<Contract>none(),
                                                         v.recurOnListOfTraitTypeWhere(NodeUtil.getExtendsClause(that)),
                                                         v.recurOnOptionOfListOfParam(NodeUtil.getParams(that)),
                                                         v.recurOnListOfDecl(NodeUtil.getDecls(that)));

        return forObjectDeclOnly(that,
                                 that.getInfo(),
                                 header,
                                 v.recurOnOptionOfSelfType(that.getSelfType()));
    }

    /**
     * When recurring on a FnDecl, we first need to extend the
     * environment with all the newly bound static parameters.
     */
    @Override
    public Node forFnDecl(final FnDecl that) {
        TypeDisambiguator v = this.extend(NodeUtil.getStaticParams(that));

        FnHeader header = (FnHeader) forFnHeaderOnly(that.getHeader(),
                                                     v.recurOnListOfStaticParam(NodeUtil.getStaticParams(that)),
                                                     (IdOrOpOrAnonymousName) NodeUtil.getName(that).accept(v),
                                                     v.recurOnOptionOfWhereClause(NodeUtil.getWhereClause(that)),
                                                     v.recurOnOptionOfListOfType(NodeUtil.getThrowsClause(that)),
                                                     v.recurOnOptionOfContract(NodeUtil.getContract(that)),
                                                     v.recurOnListOfParam(NodeUtil.getParams(that)),
                                                     v.recurOnOptionOfType(NodeUtil.getReturnType(that)));

        return forFnDeclOnly(that,
                             that.getInfo(),
                             header,
                             that.getUnambiguousName(),
                             v.recurOnOptionOfExpr(NodeUtil.getBody(that)),
                             that.getImplementsUnambiguousName());
    }

    @Override
    public Node forArrowType(final ArrowType that) {
        Type domain = that.getDomain();
        Type domainResult;
        if (domain instanceof TupleType) domainResult = (Type) super.forTupleType((TupleType) domain);
        else domainResult = (Type) domain.accept(this);
        Type rangeResult = (Type) that.getRange().accept(this);
        Effect effectResult = (Effect) that.getEffect().accept(this);
        Option<MethodInfo> methodInfo = that.getMethodInfo();
        TypeInfo infoResult = NodeFactory.makeTypeInfo(NodeUtil.getSpan(that),
                                                       NodeUtil.isParenthesized(that),
                                                       NodeUtil.getStaticParams(that),
                                                       NodeUtil.getWhereClause(that));
        return forArrowTypeOnly(that, infoResult, domainResult, rangeResult, effectResult, methodInfo);
    }

    @Override
    public Node forVarType(final VarType that) {
        Thunk<Type> varHandler = LambdaUtil.<Type>valueLambda(that);
        Lambda<Id, Type> typeConsHandler = new Lambda<Id, Type>() {
            public Type value(Id n) {
                if (n.equals(Types.ANY_NAME)) {
                    return NodeFactory.makeAnyType(NodeUtil.getSpan(that));
                } else {
                    TypeConsIndex typeCons = _env.typeConsIndex(n);
                    if (typeCons == null) {
                        error("Undefined type: " + NodeUtil.nameString(n), that);
                        return that;
                    }
                    if (!typeCons.staticParameters().isEmpty()) {
                        error("Type requires static arguments: " + NodeUtil.nameString(n), that);
                        return that;
                    }
                    return NodeFactory.makeTraitType(NodeUtil.getSpan(that), false, n);
                }
            }
        };
        return handleTypeName(that, that.getName(), varHandler, typeConsHandler);
    }

    private PatternBinding typeToPatternBinding(Type t) {
        Span span = NodeUtil.getSpan(t);
        if (t instanceof TupleType) {
            TupleType ty = (TupleType)t;
            List<PatternBinding> ps = new ArrayList<PatternBinding>();
            for (Type tty : ty.getElements()) {
                ps.add(typeToPatternBinding(tty));
            }
            return NodeFactory.makeNestedPattern(span,
                                                 NodeFactory.makePattern(span, Option.<Type>none(),
                                                                         NodeFactory.makePatternArgs(span, ps)));
        } else if (t instanceof VarType) {
            VarType ty = (VarType)t;
            Id id = ty.getName();
            if (_env.hasTypeParam(id).isNone())
                return NodeFactory.makePlainPattern(span, Option.<Id>none(), id,
                                                    Modifiers.None,
                                                    Option.<TypeOrPattern>some(NodeFactory.makeAnyType(span)));
            else return NodeFactory.makeTypePattern(span, Option.<Id>none(), ty);
        } else {
            return NodeFactory.makeTypePattern(span, Option.<Id>none(), t);
        }
    }

    @Override
    public Node forTypecaseClause(final TypecaseClause that) {
        forTypecaseClause = true;
        Span span = NodeUtil.getSpan(that);
        Option<Id> name_result = that.getName();
        TypeOrPattern tp = that.getMatchType();
        TypeOrPattern matchType_result;
        matchType_result = (TypeOrPattern) recur(tp);
        if (name_result.isNone() && matchType_result instanceof VarType &&
            _env.hasTypeParam(((VarType)matchType_result).getName()).isNone()) {
            name_result = Option.<Id>some(((VarType)matchType_result).getName());
            matchType_result = NodeFactory.makeAnyType(span);
        }
        if (rewriteTypecaseClause && matchType_result instanceof TupleType) {
            List<PatternBinding> ps = new ArrayList<PatternBinding>();
            for (Type t : ((TupleType)matchType_result).getElements()) {
                ps.add(typeToPatternBinding(t));
            }
            matchType_result = NodeFactory.makePattern(span, Option.<Type>none(),
                                                       NodeFactory.makePatternArgs(span, ps));
        }
        Block body_result = (Block) recur(that.getBody());
        forTypecaseClause = false;
        rewriteTypecaseClause = false;
        return forTypecaseClauseOnly(that, that.getInfo(), name_result, matchType_result, body_result);
    }

    @Override
    public Node forPlainPattern(final PlainPattern that) {
        Id id = that.getName();
        Boolean isTypeName = true;
        if (id.getApiName().isSome()) {
            APIName originalApi = id.getApiName().unwrap();
            Option<APIName> realApiOpt = _env.apiName(originalApi);
            if (realApiOpt.isNone()) isTypeName = false;
            else {
                APIName realApi = realApiOpt.unwrap();
                Id newN;
                if (originalApi == realApi)
                    newN = id;
                else newN = NodeFactory.makeId(realApi, id);
                if (!_env.hasQualifiedTypeCons(newN))
                    isTypeName = false;
            }
        } else if (_env.hasTypeParam(id).isNone()) {
            Set<Id> typeConses = _env.explicitTypeConsNames(id);
            if (typeConses.isEmpty()) {
                typeConses = _env.onDemandTypeConsNames(id);
                _onDemandImports.add(id);
            }
            if (typeConses.isEmpty()) isTypeName = false;
            else if (typeConses.size() > 1) isTypeName = false;
        }
        final Span span = NodeUtil.getSpan(that);
        // if it is a type name, replace PlainPattern with TypePattern
        if (that.getIdType().isNone() && isTypeName)
            return super.forTypePattern(NodeFactory.makeTypePattern(span, NodeFactory.makeVarType(span, id)));
        else
            return super.forPlainPattern(that);
    }

    @Override
    public Node forTraitType(final TraitType that) {
        Thunk<Type> varHandler = new Thunk<Type>() {
            public Type value() {
                error("Type parameter cannot be parameterized: " + NodeUtil.nameString(that.getName()), that);
                return that;
            }
        };
        Lambda<Id, Type> typeConsHandler = new Lambda<Id, Type>() {
            public Type value(Id n) {
                List<StaticArg> args = that.getArgs();
                if (n.equals(Types.ANY_NAME) && args.isEmpty()) {
                    return NodeFactory.makeAnyType(NodeUtil.getSpan(that));
                } else {
                    TypeConsIndex typeCons = _env.typeConsIndex(n);
                    List<StaticParam> params = typeCons.staticParameters();
                    if (params.size() != args.size()) {
                        error("Incorrect number of static arguments for type '" + NodeUtil.nameString(n) +
                              "': provided " + args.size() + ", expected " + params.size(), that);
                        return that;
                    }
                    boolean changed = !n.equals(that.getName());
                    List<StaticArg> newArgs = new ArrayList<StaticArg>(args.size());
                    for (Pair<StaticParam, StaticArg> pair : IterUtil.zip(params, args)) {
                        StaticArg updated = updateStaticArg(pair.second(), pair.first());
                        if (updated != pair.second()) {
                            changed = true;
                        }
                        newArgs.add(updated);
                    }
                    return changed ? NodeFactory.makeTraitType(NodeUtil.getSpan(that), n, newArgs) : that;
                }
            }
        };
        return handleTypeName(that, that.getName(), varHandler, typeConsHandler);
    }

    /**
     * Generalization of name handling in {@code forVarType} and
     * {@code forTraitType}.  Disambiguate the given type name,
     * determine whether it is a variable or a type constructor, and delegate to the
     * appropriate handler.
     *
     * @param that            The type that is being translated (used as a result when errors
     *                        occur).
     * @param n               The type name to be resolved (provided by {@code that}).
     * @param variableHandler Produces the appropriate result where {@code n} represents
     *                        a type variable.  May assume that {@code n} has no
     *                        API and was not changed.
     * @param typeConsHandler Produce the appropriate result where {@code n} represents
     *                        a type constructor (trait, object, or type alias) name.
     *                        An updated, potentially-qualified version of {@code n} is
     *                        provided.  May assume that the named type constructor
     *                        exists.
     */
    private Type handleTypeName(Type that, Id n, Thunk<Type> variableHandler, Lambda<Id, Type> typeConsHandler) {
        if (n.getApiName().isSome()) {
            APIName originalApi = n.getApiName().unwrap();
            Option<APIName> realApiOpt = _env.apiName(originalApi);
            if (realApiOpt.isNone()) {
                // retry for debugging purposes
                //realApiOpt = _env.apiName(originalApi);
                error("Undefined API: " + NodeUtil.nameString(originalApi), originalApi);
                return that;
            }
            APIName realApi = realApiOpt.unwrap();
            Id newN;
            //System.err.println("original " + originalApi);
            //System.err.println("realApi: " + realApi);
            if (originalApi == realApi) {
                newN = n;
            } else {
                newN = NodeFactory.makeId(realApi, n);
            }

            if (!_env.hasQualifiedTypeCons(newN)) {
                error(NodeUtil.nameString(newN) + " is undefined.", newN);
                return that;
            }
            return typeConsHandler.value(newN);
        } else {
            if (_env.hasTypeParam(n).isSome()) {
                return variableHandler.value();
            } else {
                Set<Id> typeConses = _env.explicitTypeConsNames(n);
                if (typeConses.isEmpty()) {
                    typeConses = _env.onDemandTypeConsNames(n);
                    _onDemandImports.add(n);
                }
                if (typeConses.isEmpty()) {
                    if (forTypecaseClause)
                        rewriteTypecaseClause = true;
                    else
                        error(NodeUtil.nameString(n) + " is undefined.", n);
                    return that;
                }
                if (typeConses.size() > 1) {
                    error("Type name may refer to: " + NodeUtil.namesString(typeConses), n);
                    return that;
                }
                Id qname = IterUtil.first(typeConses);
                Type result = typeConsHandler.value(qname);
                //System.err.println("result type: " + result);
                return result;
            }
        }
    }

    /**
     * Prevent recursion on function static args -- delayed until the function references
     * can be resolved.
     */
    //    @Override public Node forFnRef(FnRef that) {
    //        return that;
    //    }
    private StaticArg updateStaticArg(final StaticArg arg, final StaticParam param) {
        // return a;
        /* Commented out due to assumptions in the interpreter that this *isn't* implemented: */
        StaticArg fixed = arg.accept(new NodeAbstractVisitor<StaticArg>() {

            @Override
            public StaticArg forTypeArg(final TypeArg a) {
                final Type t = a.getTypeArg();
                if (t instanceof VarType) {
                    final Span s = NodeUtil.getSpan(a);
                    final Id name = ((VarType) t).getName();
                    return param.getKind().accept(new NodeAbstractVisitor<StaticArg>() {
                        @Override
                        public StaticArg forKindBool(KindBool k) {
                            return NodeFactory.makeBoolArg(s, NodeFactory.makeBoolRef(s, name));
                        }

                        @Override
                        public StaticArg forKindDim(KindDim p) {
                            return NodeFactory.makeDimArg(s, NodeFactory.makeDimRef(s, name));
                        }

                        @Override
                        public StaticArg forKindInt(KindInt p) {
                            return NodeFactory.makeIntArg(s, NodeFactory.makeIntRef(s, name));
                        }

                        @Override
                        public StaticArg forKindNat(KindNat p) {
                            return NodeFactory.makeIntArg(s, NodeFactory.makeIntRef(s, name));
                            // TODO: shouldn't there be a NatArg class?
                        }

                        @Override
                        public StaticArg forKindType(KindType p) {
                            return a;
                        }

                        @Override
                        public StaticArg forKindUnit(KindUnit p) {
                            return NodeFactory.makeUnitArg(s, NodeFactory.makeUnitRef(s, false, name));
                        }

                        @Override
                        public StaticArg forKindOp(KindOp p) {
                            mismatch("an identifier");
                            return a;
                        }
                    });
                } else {
                    if (!NodeUtil.isTypeParam(param)) {
                        mismatch("a type");
                    }
                    return a;
                }
            }

            @Override
            public StaticArg forIntArg(IntArg a) {
                if (!NodeUtil.isIntParam(param)) {
                    mismatch("an int expression");
                }
                return a;
            }

            @Override
            public StaticArg forBoolArg(BoolArg a) {
                if (!NodeUtil.isBoolParam(param)) {
                    mismatch("a bool expression");
                }
                return a;
            }

            @Override
            public StaticArg forOpArg(OpArg a) {
                if (!NodeUtil.isOpParam(param)) {
                    mismatch("an operator");
                }
                return a;
            }

            @Override
            public StaticArg forDimArg(DimArg a) {
                if (!NodeUtil.isDimParam(param)) {
                    mismatch("a dimension");
                }
                return a;
            }

            @Override
            public StaticArg forUnitArg(UnitArg a) {
                // TODO: convert units to dimensions
                if (!NodeUtil.isUnitParam(param)) {
                    mismatch("a unit");
                }
                return a;
            }

            private void mismatch(String given) {
                String expected = param.getKind().accept(new NodeAbstractVisitor<String>() {
                    @Override
                    public String forKindBool(KindBool k) {
                        return "a bool expression";
                    }

                    @Override
                    public String forKindDim(KindDim k) {
                        return "a dimension";
                    }

                    @Override
                    public String forKindInt(KindInt k) {
                        return "an int expression";
                    }

                    @Override
                    public String forKindNat(KindNat k) {
                        return "a nat expression";
                    }

                    @Override
                    public String forKindType(KindType k) {
                        return "a type";
                    }

                    @Override
                    public String forKindUnit(KindUnit k) {
                        return "a unit";
                    }

                    @Override
                    public String forKindOp(KindOp k) {
                        return "an operator";
                    }
                });
                error("Type parameter mismatch: given " + given + ", expected " + expected, arg);
            }

        });
        return (StaticArg) fixed.accept(this);

    }

    private Pair<List<Id>, Collection<GrammarIndex>> getExtendedGrammarIndecies(GrammarDecl that) {
        List<Id> ls = new LinkedList<Id>();
        Collection<GrammarIndex> gs = new LinkedList<GrammarIndex>();
        for (Id name : that.getExtendsClause()) {
            Id nname = handleGrammarName(name);
            ls.add(nname);
            Option<GrammarIndex> gi = this._env.grammarIndex(nname);
            if (gi.isSome()) {
                gs.add(gi.unwrap());
            } else {
                error("Undefined grammar: " + NodeUtil.nameString(nname), name);
            }
        }
        return new Pair<List<Id>, Collection<GrammarIndex>>(ls, gs);
    }

    @Override
    public Node forSuperSyntaxDefOnly(SuperSyntaxDef that, ASTNodeInfo info, Id nonterminal_result, Id grammar_result) {
        Id disambiguatedGrammar = handleGrammarName(grammar_result);
        return new SuperSyntaxDef(info, that.getModifier(), nonterminal_result, disambiguatedGrammar);
    }

    @Override
    public Node forGrammarDeclOnly(GrammarDecl that,
                                   ASTNodeInfo info,
                                   Id name_result,
                                   List<Id> extends_result,
                                   List<GrammarMemberDecl> members_result,
                                   List<TransformerDecl> transformers) {

        Pair<List<Id>, Collection<GrammarIndex>> p = getExtendedGrammarIndecies(that);

        Id name = handleGrammarName(name_result);

        GrammarDecl disambiguatedGrammar = new GrammarDecl(info,
                                                           name,
                                                           p.first(),
                                                           members_result,
                                                           transformers,
                                                           that.isNativeDef());

        List<StaticError> newErrs = new ArrayList<StaticError>();

        if (!newErrs.isEmpty()) {
            this._errors.addAll(newErrs);
        }

        return disambiguatedGrammar;
    }

    private Id handleGrammarName(Id name) {
        if (name.getApiName().isSome()) {
            APIName originalApi = name.getApiName().unwrap();
            Option<APIName> realApiOpt = _env.apiName(originalApi);
            if (realApiOpt.isNone()) {
                error("Undefined API: " + NodeUtil.nameString(originalApi), originalApi);
                return name;
            }
            APIName realApi = realApiOpt.unwrap();
            Id newN;
            if (originalApi == realApi) {
                newN = name;
            } else {
                newN = NodeFactory.makeId(NodeUtil.getSpan(name), realApi, name);
            }

            if (!_env.hasQualifiedGrammar(newN)) {
                error("Undefined grammar: " + NodeUtil.nameString(newN), newN);
                return name;
            }
            return newN;
        } else {
            String uqname = name.getText();
            if (_env.hasGrammar(uqname)) {
                Set<Id> grammars = _env.explicitGrammarNames(uqname);
                if (grammars.size() > 1) {
                    error("Grammar name may refer to: " + NodeUtil.namesString(grammars), name);
                    return name;
                }
                Id qname = IterUtil.first(grammars);
                return qname;
            } else {
                Set<Id> grammars = _env.explicitGrammarNames(uqname);
                if (grammars.isEmpty()) {
                    grammars = _env.onDemandGrammarNames(uqname);
                    _onDemandImports.add(name);
                }

                if (grammars.isEmpty()) {
                    error("Undefined grammar: " + NodeUtil.nameString(name), name);
                    return name;
                }
                if (grammars.size() > 1) {
                    error("Grammar name may refer to: " + NodeUtil.namesString(grammars), name);
                    return name;
                }
                Id qname = IterUtil.first(grammars);
                return qname;
            }
        }
    }

    @Override
    public Node forNonterminalHeader(NonterminalHeader that) {
        TypeDisambiguator v = this.extend(that.getStaticParams());

        //        System.err.println("T: "+that.getParamType());

        Option<Type> t = v.recurOnOptionOfType(that.getParamType());
        //        System.err.println("t: "+t);
        return forNonterminalHeaderOnly(that,
                                        that.getInfo(),
                                        (Id) that.getName().accept(v),
                                        v.recurOnListOfNonterminalParameter(that.getParams()),
                                        v.recurOnListOfStaticParam(that.getStaticParams()),
                                        t,
                                        v.recurOnOptionOfWhereClause(that.getWhereClause()));
    }

    /**
     * All Args are parsed as TypeArgs
     */
    @Override
    public Node forTypeArgOnly(final TypeArg arg, ASTNodeInfo info, final Type t) {
        if (arg.getTypeArg() instanceof VarType) {
            Id _name = ((VarType) arg.getTypeArg()).getName();
            Option<StaticParam> param = this._env.hasTypeParam(_name);
            if (param.isSome()) {
                final IdOrOp name = param.unwrap().getName();
                NodeAbstractVisitor<StaticArg> v = new NodeAbstractVisitor<StaticArg>() {
                    @Override
                    public StaticArg forKindBool(KindBool k) {
                        return NodeFactory.makeBoolArg(NodeUtil.getSpan(arg), NodeFactory.makeBoolRef(NodeUtil.getSpan(
                                arg), (Id) name));
                    }

                    @Override
                    public StaticArg forKindDim(KindDim k) {
                        return NodeFactory.makeDimArg(NodeUtil.getSpan(arg),
                                                      NodeFactory.makeDimRef(NodeUtil.getSpan(arg), (Id) name));
                    }

                    @Override
                    public StaticArg forKindInt(KindInt k) {
                        return NodeFactory.makeIntArg(NodeUtil.getSpan(arg),
                                                      NodeFactory.makeIntRef(NodeUtil.getSpan(arg), (Id) name));
                    }

                    @Override
                    public StaticArg forKindNat(KindNat k) {
                        return NodeFactory.makeIntArg(NodeUtil.getSpan(arg),
                                                      NodeFactory.makeIntRef(NodeUtil.getSpan(arg), (Id) name));
                    }

                    @Override
                    public StaticArg forKindType(KindType k) {
                        return NodeFactory.makeTypeArg(NodeUtil.getSpan(arg), t);
                    }

                    @Override
                    public StaticArg forKindUnit(KindUnit k) {
                        return NodeFactory.makeUnitArg(NodeUtil.getSpan(arg), NodeFactory.makeUnitRef(NodeUtil.getSpan(
                                arg), false, (Id) name));
                    }

                    @Override
                    public StaticArg forKindOp(KindOp that) {
                        return NodeFactory.makeOpArg(NodeUtil.getSpan(arg), (Op) name);
                    }
                };
                return param.unwrap().getKind().accept(v);
            }
        }
        return NodeFactory.makeTypeArg(NodeUtil.getSpan(arg), t);
    }

}
