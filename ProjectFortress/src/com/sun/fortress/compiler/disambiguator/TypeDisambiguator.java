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

package com.sun.fortress.compiler.disambiguator;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Collections;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;
import edu.rice.cs.plt.collect.ConsList;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.lambda.LambdaUtil;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.useful.HasAt;

/**
 * <p>Eliminates ambiguities in types:
 * <ul>
 * <li>Type names referring to APIs are made fully qualified.
 * <li>IdTypes referring to traits, objects, and aliases become InstantiatedTypes
 *     (with 0 arguments).</li>
 * <li>TypeArgs wrapping IdTypes corresponding to other kinds of parameters (like UnitParams)
       are converted to the corresponding kind of arg (like UnitArg).</li>
 * <li>TODO: UnitArgs corresponding to DimParams are converted to DimArgs.</li>
 * <li>TODO: TaggedUnitTypes for types that absorb dimensions become TaggedDimTypes.</li>
 * </ul>
 * (TODO: Verify that the parser prefers producing units over dimensions in ambiguous cases.)
 * </p>
 * <p>All name references in resolved types that are undefined or used incorrectly are
 * treated as static errors.  Similarly, incorrect arity or kinds of static arguments
 * are treated as errors.</p>
 */
public class TypeDisambiguator extends NodeUpdateVisitor {

    private TypeNameEnv _env;
    private Set<SimpleName> _onDemandImports;
    private List<StaticError> _errors;

    public TypeDisambiguator(TypeNameEnv env, Set<SimpleName> onDemandImports,
            List<StaticError> errors) {
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
     * When recurring on an AbsTraitDecl, we first need to extend the
     * environment with all the newly bound static parameters.
     */
    @Override public Node forAbsTraitDecl(final AbsTraitDecl that) {
        TypeDisambiguator v = this.extend(that.getStaticParams());

        return forAbsTraitDeclOnly(that,
                v.recurOnListOfModifier(that.getMods()),
                (Id) that.getName().accept(v),
                v.recurOnListOfStaticParam(that.getStaticParams()),
                v.recurOnListOfTraitTypeWhere(that.getExtendsClause()),
                (WhereClause) that.getWhere().accept(v),
                v.recurOnListOfTraitType(that.getExcludes()),
                v.recurOnOptionOfListOfTraitType(that.getComprises()),
                v.recurOnListOfAbsDecl(that.getDecls()));
    }

    /**
     * When recurring on a TraitDecl, we first need to extend the
     * environment with all the newly bound static parameters.
     */
    @Override public Node forTraitDecl(final TraitDecl that) {
        TypeDisambiguator v = this.extend(that.getStaticParams());

        return forTraitDeclOnly(that,
                v.recurOnListOfModifier(that.getMods()),
                (Id) that.getName().accept(v),
                v.recurOnListOfStaticParam(that.getStaticParams()),
                v.recurOnListOfTraitTypeWhere(that.getExtendsClause()),
                (WhereClause) that.getWhere().accept(v),
                v.recurOnListOfTraitType(that.getExcludes()),
                v.recurOnOptionOfListOfTraitType(that.getComprises()),
                v.recurOnListOfDecl(that.getDecls()));
    }


    /**
     * When recurring on an AbsObjectDecl, we first need to extend the
     * environment with all the newly bound static parameters.
     */
    @Override public Node forAbsObjectDecl(final AbsObjectDecl that) {
        TypeDisambiguator v = this.extend(that.getStaticParams());

        return forAbsObjectDeclOnly(that,
                v.recurOnListOfModifier(that.getMods()),
                (Id) that.getName().accept(v),
                v.recurOnListOfStaticParam(that.getStaticParams()),
                v.recurOnListOfTraitTypeWhere(that.getExtendsClause()),
                (WhereClause) that.getWhere().accept(v),
                v.recurOnOptionOfListOfParam(that.getParams()),
                v.recurOnOptionOfListOfTraitType(that.getThrowsClause()),
                (Contract) that.getContract().accept(v),
                v.recurOnListOfAbsDecl(that.getDecls()));
    }

    /**
     * When recurring on an ObjectDecl, we first need to extend the
     * environment with all the newly bound static parameters.
     */
    @Override public Node forObjectDecl(final ObjectDecl that) {
        TypeDisambiguator v = this.extend(that.getStaticParams());

        return forObjectDeclOnly(that,
                v.recurOnListOfModifier(that.getMods()),
                (Id) that.getName().accept(v),
                v.recurOnListOfStaticParam(that.getStaticParams()),
                v.recurOnListOfTraitTypeWhere(that.getExtendsClause()),
                (WhereClause) that.getWhere().accept(v),
                v.recurOnOptionOfListOfParam(that.getParams()),
                v.recurOnOptionOfListOfTraitType(that.getThrowsClause()),
                (Contract) that.getContract().accept(v),
                v.recurOnListOfDecl(that.getDecls()));
    }


    /**
     * When recurring on an AbsFnDecl, we first need to extend the
     * environment with all the newly bound static parameters.
     */
    @Override public Node forAbsFnDecl(final AbsFnDecl that) {
        TypeDisambiguator v = this.extend(that.getStaticParams());

        return forAbsFnDeclOnly(that,
                v.recurOnListOfModifier(that.getMods()),
                (SimpleName) that.getName().accept(v),
                v.recurOnListOfStaticParam(that.getStaticParams()),
                v.recurOnListOfParam(that.getParams()),
                v.recurOnOptionOfType(that.getReturnType()),
                v.recurOnOptionOfListOfTraitType(that.getThrowsClause()),
                (WhereClause) that.getWhere().accept(v),
                (Contract) that.getContract().accept(v));
    }

    /**
     * When recurring on a FnDecl, we first need to extend the
     * environment with all the newly bound static parameters.
     */
    @Override public Node forFnDef(final FnDef that) {
        TypeDisambiguator v = this.extend(that.getStaticParams());

        return forFnDefOnly(that,
                v.recurOnListOfModifier(that.getMods()),
                (SimpleName) that.getName().accept(v),
                v.recurOnListOfStaticParam(that.getStaticParams()),
                v.recurOnListOfParam(that.getParams()),
                v.recurOnOptionOfType(that.getReturnType()),
                v.recurOnOptionOfListOfTraitType(that.getThrowsClause()),
                (WhereClause) that.getWhere().accept(v),
                (Contract) that.getContract().accept(v),
                (Expr) that.getBody().accept(v));
    }

    @Override public Node forArgType(final ArgType that) {
        if (!((ArgType)that).isInArrow())
            error("Tuple types are not allowed to " +
                  "have varargs or keyword types.", that);
        return that;
    }

    @Override public Node forIdType(final IdType that) {
        Thunk<Type> varHandler = LambdaUtil.<Type>valueLambda(that);
        Lambda<QualifiedIdName, Type> typeConsHandler =
            new Lambda<QualifiedIdName, Type>() {
            public Type value(QualifiedIdName n) {
                TypeConsIndex typeCons = _env.typeConsIndex(n);
                // System.err.println(that.getSpan()+":");
                // System.err.println(" that: "+that);
                // System.err.println(" n: " + n);
                // System.err.println(" typeCons: " + typeCons);
                // System.err.println(" _env: "+_env);

                if (!typeCons.staticParameters().isEmpty()) {
                    error("Type requires static arguments: " + NodeUtil.nameString(n),
                          that);
                    return that;
                }
                return new InstantiatedType(n, Collections.<StaticArg>emptyList());
            }
        };
        return handleTypeName(that, that.getName(), varHandler, typeConsHandler);
    }

    @Override public Node forInstantiatedType(final InstantiatedType that) {
        Thunk<Type> varHandler = new Thunk<Type>() {
            public Type value() {
                error("Type parameter cannot be parameterized: " +
                        NodeUtil.nameString(that.getName()), that);
                return that;
            }
        };
        Lambda<QualifiedIdName, Type> typeConsHandler =
            new Lambda<QualifiedIdName, Type>() {
            public InstantiatedType value(QualifiedIdName n) {
                TypeConsIndex typeCons = _env.typeConsIndex(n);
                List<StaticParam> params = typeCons.staticParameters();
                List<StaticArg> args = that.getArgs();
                if (params.size() != args.size()) {
                    error("Incorrect number of static arguments for type '" +
                            NodeUtil.nameString(n) + "': provided " + args.size() +
                            ", expected " + params.size(), that);
                    return that;
                }
                boolean changed = false;
                List<StaticArg> newArgs = new ArrayList<StaticArg>(args.size());
                for (Pair<StaticParam, StaticArg> pair :
                    IterUtil.zip(params, args)) {
                    StaticArg updated = updateStaticArg(pair.second(), pair.first());
                    if (updated != pair.second()) { changed = true; }
                    newArgs.add(updated);
                }
                return changed ?
                        new InstantiatedType(that.getSpan(), n, newArgs) : that;
            }
        };
        return handleTypeName(that, that.getName(), varHandler, typeConsHandler);
    }

    /**
     * Generalization of name handling in {@code forIdType} and
     * {@code forInstantiatedType}.  Disambiguate the given type name,
     * determine whether it is a variable or a type constructor, and delegate to the
     * appropriate handler.
     * @param that  The type that is being translated (used as a result when errors
     *              occur).
     * @param n  The type name to be resolved (provided by {@code that}).
     * @param variableHandler  Produces the appropriate result where {@code n} represents
     *                         a type variable.  May assume that {@code n} has no
     *                         API and was not changed.
     * @param typeConsHandler  Produce the appropriate result where {@code n} represents
     *                         a type constructor (trait, object, or type alias) name.
     *                         An updated, potentially-qualified version of {@code n} is
     *                         provided.  May assume that the named type constructor
     *                         exists.
     */
    private Type handleTypeName(Type that, QualifiedIdName n,
                        Thunk<Type> variableHandler,
                              Lambda<QualifiedIdName, Type> typeConsHandler) {
        if (n.getApi().isSome()) {
            APIName originalApi = Option.unwrap(n.getApi());
            Option<APIName> realApiOpt = _env.apiName(originalApi);
            if (realApiOpt.isNone()) {
                error("Undefined API: " + NodeUtil.nameString(originalApi), originalApi);
                return that;
            }
            APIName realApi = Option.unwrap(realApiOpt);
            QualifiedIdName newN;
            if (originalApi == realApi) { newN = n; }
            else { newN = NodeFactory.makeQualifiedIdName(realApi, n.getName()); }

            if (!_env.hasQualifiedTypeCons(newN)) {
                error("Undefined type: " + NodeUtil.nameString(newN), newN);
                return that;
            }
            return typeConsHandler.value(newN);
        }

        else {
            Id id = n.getName();
            //System.err.println("id: " + id);

            if (_env.hasTypeParam(id)) { return variableHandler.value(); }
            else {
                Set<QualifiedIdName> typeConses = _env.explicitTypeConsNames(id);
                if (typeConses.isEmpty()) {
                    typeConses = _env.onDemandTypeConsNames(id);
                    _onDemandImports.add(id);
                }

                if (typeConses.isEmpty()) {
                    error("Undefined type: " + NodeUtil.nameString(n), n);
                    return that;
                }
                if (typeConses.size() > 1) {
                    error("Type name may refer to: " + NodeUtil.namesString(typeConses),
                            n);
                    return that;
                }
                QualifiedIdName qname = IterUtil.first(typeConses);
                Type result = typeConsHandler.value(qname);

                return result;
            }
        }
    }
    
    /**
     * Prevent recursion on function static args -- delayed until the function references
     * can be resolved.
     */
    @Override public Node forFnRef(FnRef that) {
        return that;
    }
    

    private StaticArg updateStaticArg(final StaticArg a, final StaticParam p) {
        return a;
        /* Commented out due to assumptions in the interpreter that this *isn't* implemented:
        StaticArg fixed = a.accept(new NodeAbstractVisitor<StaticArg>() {
        
            @Override public StaticArg forTypeArg(final TypeArg a) {
                final Type t = a.getType();
                if (t instanceof IdType) {
                    final Span s = a.getSpan();
                    final QualifiedIdName name = ((IdType) t).getName();
                    return p.accept(new NodeAbstractVisitor<StaticArg>() {
                        @Override public StaticArg forStaticParam(StaticParam p) {
                            mismatch("an identifier");
                            return a;
                        }
                        @Override public StaticArg forBoolParam(BoolParam p) {
                            return new BoolArg(s, new BoolRef(s, name));
                        }
                        @Override public StaticArg forDimensionParam(DimensionParam p) {
                            return new DimArg(s, new DimRef(s, name));
                        }
                        @Override public StaticArg forIntParam(IntParam p) {
                            return new IntArg(s, new IntRef(s, name));
                        }
                        @Override public StaticArg forNatParam(NatParam p) {
                            return new IntArg(s, new IntRef(s, name));
                            // TODO: shouldn't there be a NatArg class?
                        }
                        @Override public StaticArg forSimpleTypeParam(SimpleTypeParam p) {
                            return a;
                        }
                        @Override public StaticArg forUnitParam(UnitParam p) {
                            return new UnitArg(s, new UnitRef(s, name));
                        }
                    });
                }
                else {
                    if (!(p instanceof SimpleTypeParam)) { mismatch("a type"); }
                    return a;
                }
            }
            
            @Override public StaticArg forIntArg(IntArg a) {
                if (!(p instanceof IntParam || p instanceof NatParam)) {
                    mismatch("an int expression");
                }
                return a;
            }
            
            @Override public StaticArg forBoolArg(BoolArg a) {
                if (!(p instanceof BoolParam)) { mismatch("a bool expression"); }
                return a;
            }
            
            @Override public StaticArg forOprArg(OprArg a) {
                if (!(p instanceof OperatorParam)) { mismatch("an operator"); }
                return a;
            }
            
            @Override public StaticArg forDimArg(DimArg a) {
                if (!(p instanceof DimensionParam)) { mismatch("a dimension"); }
                return a;
            }
            
            @Override public StaticArg forUnitArg(UnitArg a) {
                // TODO: convert units to dimensions
                if (!(p instanceof UnitParam)) { mismatch("a unit"); }
                return a;
            }
            
            private void mismatch(String given) {
                String expected = p.accept(new NodeAbstractVisitor<String>() {
                    @Override public String forOperatorParam(OperatorParam p) {
                        return "an operator";
                    }
                    @Override public String forBoolParam(BoolParam p) {
                        return "a bool expression";
                    }
                    @Override public String forDimensionParam(DimensionParam p) {
                        return "a dimension";
                    }
                    @Override public String forIntParam(IntParam p) {
                        return "an int expression";
                    }
                    @Override public String forNatParam(NatParam p) {
                        return "a nat expression";
                    }
                    @Override public String forSimpleTypeParam(SimpleTypeParam p) {
                        return "a type";
                    }
                    @Override public String forUnitParam(UnitParam p) {
                        return "a unit";
                    }
                });
                error("Type parameter mismatch: given " + given + ", expected " + expected, a);
            }
            
        });
        return (StaticArg) fixed.accept(this);
        */
    }

    private Pair<List<QualifiedIdName>, Collection<GrammarIndex>> getExtendedGrammarIndecies(GrammarDef that) {
        List<QualifiedIdName> ls = new LinkedList<QualifiedIdName>();
        Collection<GrammarIndex> gs = new LinkedList<GrammarIndex>();
        for (QualifiedIdName name: that.getExtends()) {
            QualifiedIdName nname = handleGrammarName(name);
            ls.add(nname);

            Option<GrammarIndex> gi = this._env.grammarIndex(nname);
            if (gi.isSome()) {
                gs.add(Option.unwrap(gi));
            }
            else {
                error("Undefined grammar: " + NodeUtil.nameString(nname), name);
            }
        }
        return new Pair<List<QualifiedIdName>, Collection<GrammarIndex>>(ls,gs);
    }

    @Override
    public Node forGrammarDef(GrammarDef that) {

        Pair<List<QualifiedIdName>, Collection<GrammarIndex>> p = getExtendedGrammarIndecies(that);

        QualifiedIdName name = handleGrammarName(that.getName());

        GrammarDef disambiguatedGrammar = new GrammarDef(that.getSpan(),name,p.first(),that.getMembers());

        List<StaticError> newErrs = new ArrayList<StaticError>();
        Option<GrammarIndex> grammar = this._env.grammarIndex(name);
        if (grammar.isSome()) {
            GrammarIndex g = Option.unwrap(grammar);
            g.setAst(disambiguatedGrammar);
            g.setExtended(p.second());
            g.setAst(disambiguatedGrammar);
        }

        if (!newErrs.isEmpty()) {
            this._errors.addAll(newErrs);
        }

        return disambiguatedGrammar;
    }



    private QualifiedIdName handleGrammarName(QualifiedIdName name) {
        if (name.getApi().isSome()) {
            APIName originalApi = Option.unwrap(name.getApi());
            Option<APIName> realApiOpt = _env.apiName(originalApi);
            if (realApiOpt.isNone()) {
                error("Undefined API: " + NodeUtil.nameString(originalApi), originalApi);
                return name;
            }
            APIName realApi = Option.unwrap(realApiOpt);
            QualifiedIdName newN;
            if (originalApi == realApi) { newN = name; }
            else { newN = NodeFactory.makeQualifiedIdName(realApi, name.getName()); }

            if (!_env.hasQualifiedGrammar(newN)) {
                error("Undefined grammar: " + NodeUtil.nameString(newN), newN);
                return name;
            }
            return newN;
        }
        else {
            if (_env.hasGrammar(name)) {
                Set<QualifiedIdName> grammars = _env.explicitGrammarNames(name);
                if (grammars.size() > 1) {
                    error("Grammar name may refer to: " + NodeUtil.namesString(grammars), name);
                    return name;
                }
                QualifiedIdName qname = IterUtil.first(grammars);
                return qname;
            }
            else {
                Set<QualifiedIdName> grammars = _env.explicitGrammarNames(name);
                if (grammars.isEmpty()) {
                    grammars = _env.onDemandGrammarNames(name.getName());
                    _onDemandImports.add(name.getName());
                }

                if (grammars.isEmpty()) {
                    error("Undefined grammar: " + NodeUtil.nameString(name), name);
                    return name;
                }
                if (grammars.size() > 1) {
                    error("Grammar name may refer to: " + NodeUtil.namesString(grammars), name);
                    return name;
                }
                QualifiedIdName qname = IterUtil.first(grammars);
                return qname;
            }
        }
    }

}
