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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AbsFnDecl;
import com.sun.fortress.nodes.AbsObjectDecl;
import com.sun.fortress.nodes.AbsTraitDecl;
import com.sun.fortress.nodes.AnyType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BoolArg;
import com.sun.fortress.nodes.BoolParam;
import com.sun.fortress.nodes.BoolRef;
import com.sun.fortress.nodes.DimArg;
import com.sun.fortress.nodes.DimParam;
import com.sun.fortress.nodes.DimRef;
import com.sun.fortress.nodes.Domain;
import com.sun.fortress.nodes.Effect;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.IntArg;
import com.sun.fortress.nodes.IntParam;
import com.sun.fortress.nodes.IntRef;
import com.sun.fortress.nodes.NatParam;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NonterminalHeader;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.OpArg;
import com.sun.fortress.nodes.OpParam;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.SuperSyntaxDef;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TransformerDecl;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.TypeParam;
import com.sun.fortress.nodes.UnitArg;
import com.sun.fortress.nodes.UnitParam;
import com.sun.fortress.nodes.UnitRef;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes_util.ExprFactory;
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

/**
 * <p>Eliminates ambiguities in types:
 * <ul>
 * <li>VarTypes (and 0-ary TraitTypes, if they exist) referencing type Any are
 *     replaced by AnyTypes.</li>
 * <li>Type names referring to APIs are made fully qualified.</li>
 * <li>VarTypes referring to traits, objects, and aliases become TraitTypes
 *     (with 0 arguments).</li>
 * <li>TypeArgs wrapping VarTypes corresponding to other kinds of parameters (like UnitParams)
       are converted to the corresponding kind of arg (like UnitArg).</li>
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

    public TypeDisambiguator(TypeNameEnv env, Set<IdOrOpOrAnonymousName> onDemandImports,
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
                v.recurOnOptionOfWhereClause(that.getWhere()),
                v.recurOnListOfBaseType(that.getExcludes()),
                v.recurOnOptionOfListOfBaseType(that.getComprises()),
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
                v.recurOnOptionOfWhereClause(that.getWhere()),
                v.recurOnListOfBaseType(that.getExcludes()),
                v.recurOnOptionOfListOfBaseType(that.getComprises()),
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
                v.recurOnOptionOfWhereClause(that.getWhere()),
                v.recurOnOptionOfListOfParam(that.getParams()),
                v.recurOnOptionOfListOfBaseType(that.getThrowsClause()),
                v.recurOnOptionOfContract(that.getContract()),
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
                v.recurOnOptionOfWhereClause(that.getWhere()),
                v.recurOnOptionOfListOfParam(that.getParams()),
                v.recurOnOptionOfListOfBaseType(that.getThrowsClause()),
                v.recurOnOptionOfContract(that.getContract()),
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
                (IdOrOpOrAnonymousName) that.getName().accept(v),
                v.recurOnListOfStaticParam(that.getStaticParams()),
                v.recurOnListOfParam(that.getParams()),
                v.recurOnOptionOfType(that.getReturnType()),
                v.recurOnOptionOfListOfBaseType(that.getThrowsClause()),
                v.recurOnOptionOfWhereClause(that.getWhere()),
                v.recurOnOptionOfContract(that.getContract()),
                that.getUnambiguousName());
    }

    /**
     * When recurring on a FnDecl, we first need to extend the
     * environment with all the newly bound static parameters.
     */
    @Override public Node forFnDef(final FnDef that) {
        TypeDisambiguator v = this.extend(that.getStaticParams());

        return forFnDefOnly(that,
                v.recurOnListOfModifier(that.getMods()),
                (IdOrOpOrAnonymousName) that.getName().accept(v),
                v.recurOnListOfStaticParam(that.getStaticParams()),
                v.recurOnListOfParam(that.getParams()),
                v.recurOnOptionOfType(that.getReturnType()),
                v.recurOnOptionOfListOfBaseType(that.getThrowsClause()),
                v.recurOnOptionOfWhereClause(that.getWhere()),
                v.recurOnOptionOfContract(that.getContract()),
                that.getUnambiguousName(),
                (Expr) that.getBody().accept(v),
                that.getImplementsUnambiguousName());
    }

    @Override public Node forArrowType(final ArrowType that) {
        // make sure this.forDomain is *not* called
        Domain domainResult = (Domain) super.forDomain(that.getDomain());
        Type rangeResult = (Type) that.getRange().accept(this);
        Effect effectResult = (Effect) that.getEffect().accept(this);
        return forArrowTypeOnly(that, domainResult, rangeResult, effectResult);
    }

    @Override public Node forDomain(final Domain that) {
        // valid occurrences of Domain are recognized before this recursive
        // call takes place
        error("Tuple types are not allowed to have varargs or keyword types.", that);
        return that;
    }

    @Override public Node forVarType(final VarType that) {
        Thunk<Type> varHandler = LambdaUtil.<Type>valueLambda(that);
        Lambda<Id, Type> typeConsHandler =
            new Lambda<Id, Type>() {
            public Type value(Id n) {
                if (n.equals(Types.ANY_NAME)) {
                    return new AnyType(that.getSpan());
                }
                else {
                    TypeConsIndex typeCons = _env.typeConsIndex(n);
                    if (!typeCons.staticParameters().isEmpty()) {
                        error("Type requires static arguments: " + NodeUtil.nameString(n),
                              that);
                        return that;
                    }
                    return new TraitType(that.getSpan(), n,
                                                Collections.<StaticArg>emptyList());
                }
            }
        };
        return handleTypeName(that, that.getName(), varHandler, typeConsHandler);
    }

    @Override public Node forTraitType(final TraitType that) {
        Thunk<Type> varHandler = new Thunk<Type>() {
            public Type value() {
                error("Type parameter cannot be parameterized: " +
                        NodeUtil.nameString(that.getName()), that);
                return that;
            }
        };
        Lambda<Id, Type> typeConsHandler =
            new Lambda<Id, Type>() {
            public Type value(Id n) {
                List<StaticArg> args = that.getArgs();
                if (n.equals(Types.ANY_NAME) && args.isEmpty()) {
                    return new AnyType(that.getSpan());
                }
                else {
                    TypeConsIndex typeCons = _env.typeConsIndex(n);
                    List<StaticParam> params = typeCons.staticParameters();
                    if (params.size() != args.size()) {
                        error("Incorrect number of static arguments for type '" +
                              NodeUtil.nameString(n) + "': provided " + args.size() +
                              ", expected " + params.size(), that);
                        return that;
                    }
                    boolean changed = !n.equals(that.getName());
                    List<StaticArg> newArgs = new ArrayList<StaticArg>(args.size());
                    for (Pair<StaticParam, StaticArg> pair :
                             IterUtil.zip(params, args)) {
                        StaticArg updated = updateStaticArg(pair.second(), pair.first());
                        if (updated != pair.second()) { changed = true; }
                        newArgs.add(updated);
                    }
                    return changed ?
                        new TraitType(that.getSpan(), n, newArgs) : that;
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
    private Type handleTypeName(Type that, Id n,
                                Thunk<Type> variableHandler,
                                Lambda<Id, Type> typeConsHandler) {
        if (n.getApi().isSome()) {
            APIName originalApi = n.getApi().unwrap();
            Option<APIName> realApiOpt = _env.apiName(originalApi);
            if (realApiOpt.isNone()) {
                error("Undefined API: " + NodeUtil.nameString(originalApi), originalApi);
                return that;
            }
            APIName realApi = realApiOpt.unwrap();
            Id newN;
            if (originalApi == realApi) { newN = n; }
            else { newN = NodeFactory.makeId(realApi, n); }

            if (!_env.hasQualifiedTypeCons(newN)) {
                error(NodeUtil.nameString(newN) + " is undefined.", newN);
                return that;
            }
            return typeConsHandler.value(newN);
        }

        else {
            if (_env.hasTypeParam(n).isSome()) { return variableHandler.value(); }
            else {
                Set<Id> typeConses = _env.explicitTypeConsNames(n);
                if (typeConses.isEmpty()) {
                    typeConses = _env.onDemandTypeConsNames(n);
                    _onDemandImports.add(n);
                }
                if (typeConses.isEmpty()) {
                    error(NodeUtil.nameString(n)+" is undefined.", n);
                    return that;
                }
                if (typeConses.size() > 1) {
                    error("Type name may refer to: " + NodeUtil.namesString(typeConses),
                            n);
                    return that;
                }
                Id qname = IterUtil.first(typeConses);
                Type result = typeConsHandler.value(qname);

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


    private StaticArg updateStaticArg(final StaticArg a, final StaticParam p) {
        // return a;
        /* Commented out due to assumptions in the interpreter that this *isn't* implemented: */
        StaticArg fixed = a.accept(new NodeAbstractVisitor<StaticArg>() {

            @Override public StaticArg forTypeArg(final TypeArg a) {
                final Type t = a.getType();
                if (t instanceof VarType) {
                    final Span s = a.getSpan();
                    final Id name = ((VarType) t).getName();
                    return p.accept(new NodeAbstractVisitor<StaticArg>() {
                        @Override public StaticArg forStaticParam(StaticParam p) {
                            mismatch("an identifier");
                            return a;
                        }
                        @Override public StaticArg forBoolParam(BoolParam p) {
                            return new BoolArg(s, new BoolRef(s, name));
                        }
                        @Override public StaticArg forDimParam(DimParam p) {
                            return new DimArg(s, new DimRef(s, name));
                        }
                        @Override public StaticArg forIntParam(IntParam p) {
                            return new IntArg(s, new IntRef(s, name));
                        }
                        @Override public StaticArg forNatParam(NatParam p) {
                            return new IntArg(s, new IntRef(s, name));
                            // TODO: shouldn't there be a NatArg class?
                        }
                        @Override public StaticArg forTypeParam(TypeParam p) {
                            return a;
                        }
                        @Override public StaticArg forUnitParam(UnitParam p) {
                            return new UnitArg(s, new UnitRef(s, name));
                        }
                    });
                }
                else {
                    if (!(p instanceof TypeParam)) { mismatch("a type"); }
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

            @Override public StaticArg forOpArg(OpArg a) {
                if (!(p instanceof OpParam)) { mismatch("an operator"); }
                return a;
            }

            @Override public StaticArg forDimArg(DimArg a) {
                if (!(p instanceof DimParam)) { mismatch("a dimension"); }
                return a;
            }

            @Override public StaticArg forUnitArg(UnitArg a) {
                // TODO: convert units to dimensions
                if (!(p instanceof UnitParam)) { mismatch("a unit"); }
                return a;
            }

            private void mismatch(String given) {
                String expected = p.accept(new NodeAbstractVisitor<String>() {
                    @Override public String forOpParam(OpParam p) {
                        return "an operator";
                    }
                    @Override public String forBoolParam(BoolParam p) {
                        return "a bool expression";
                    }
                    @Override public String forDimParam(DimParam p) {
                        return "a dimension";
                    }
                    @Override public String forIntParam(IntParam p) {
                        return "an int expression";
                    }
                    @Override public String forNatParam(NatParam p) {
                        return "a nat expression";
                    }
                    @Override public String forTypeParam(TypeParam p) {
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

    }

    private Pair<List<Id>, Collection<GrammarIndex>> getExtendedGrammarIndecies(GrammarDef that) {
        List<Id> ls = new LinkedList<Id>();
        Collection<GrammarIndex> gs = new LinkedList<GrammarIndex>();
        for (Id name: that.getExtends()) {
            Id nname = handleGrammarName(name);
            ls.add(nname);
            Option<GrammarIndex> gi = this._env.grammarIndex(nname);
            if (gi.isSome()) {
                gs.add(gi.unwrap());
            }
            else {
                error("Undefined grammar: " + NodeUtil.nameString(nname), name);
            }
        }
        return new Pair<List<Id>, Collection<GrammarIndex>>(ls,gs);
    }

    @Override public Node forSuperSyntaxDefOnly(SuperSyntaxDef that, Id nonterminal_result, Id grammar_result) {
        Id disambiguatedGrammar = handleGrammarName(grammar_result);
        return new SuperSyntaxDef(that.getSpan(), that.getModifier(), nonterminal_result, disambiguatedGrammar);
    }

    @Override
    public Node forGrammarDefOnly(GrammarDef that, Id name_result,
            List<Id> extends_result,
            List<GrammarMemberDecl> members_result,
            List<TransformerDecl> transformers) {

        Pair<List<Id>, Collection<GrammarIndex>> p = getExtendedGrammarIndecies(that);

        Id name = handleGrammarName(name_result);

        GrammarDef disambiguatedGrammar = new GrammarDef(that.getSpan(), name, p.first(), members_result, transformers, that.isNative());

        List<StaticError> newErrs = new ArrayList<StaticError>();

        if (!newErrs.isEmpty()) {
            this._errors.addAll(newErrs);
        }

        return disambiguatedGrammar;
    }

    private Id handleGrammarName(Id name) {
        if (name.getApi().isSome()) {
            APIName originalApi = name.getApi().unwrap();
            Option<APIName> realApiOpt = _env.apiName(originalApi);
            if (realApiOpt.isNone()) {
                error("Undefined API: " + NodeUtil.nameString(originalApi), originalApi);
                return name;
            }
            APIName realApi = realApiOpt.unwrap();
            Id newN;
            if (originalApi == realApi) { newN = name; }
            else { newN = NodeFactory.makeId(name.getSpan(), realApi, name); }

            if (!_env.hasQualifiedGrammar(newN)) {
                error("Undefined grammar: " + NodeUtil.nameString(newN), newN);
                return name;
            }
            return newN;
        }
        else {
         String uqname = name.getText();
            if (_env.hasGrammar(uqname)) {
                Set<Id> grammars = _env.explicitGrammarNames(uqname);
                if (grammars.size() > 1) {
                    error("Grammar name may refer to: " + NodeUtil.namesString(grammars), name);
                    return name;
                }
                Id qname = IterUtil.first(grammars);
                return qname;
            }
            else {
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

//        System.err.println("T: "+that.getType());

        Option<Type> t = v.recurOnOptionOfType(that.getType());
//        System.err.println("t: "+t);
        return forNonterminalHeaderOnly(that,
            that.getModifier(),
            (Id) that.getName().accept(v),
            v.recurOnListOfNonterminalParameter(that.getParams()),
            v.recurOnListOfStaticParam(that.getStaticParams()),
            t ,
            v.recurOnOptionOfWhereClause(that.getWhereClause()));
    }

    /**
     * All Args are parsed as TypeArgs
     */
	@Override
	public Node forTypeArgOnly(final TypeArg arg, final Type t) {
		if(arg.getType() instanceof VarType){
			Id name = ((VarType)arg.getType()).getName();
			Option<StaticParam> param=this._env.hasTypeParam(name);
			if(param.isSome()){
				NodeAbstractVisitor<StaticArg> v =new NodeAbstractVisitor<StaticArg>(){

					@Override
					public StaticArg forBoolParam(BoolParam that) {
						return new BoolArg(arg.getSpan(), new BoolRef(arg.getSpan(),that.getName()));
					}

					@Override
					public StaticArg forDimParam(DimParam that) {
						return new DimArg(arg.getSpan(), new DimRef(arg.getSpan(),that.getName()));
					}

					@Override
					public StaticArg forIntParam(IntParam that) {
						return new IntArg(arg.getSpan(), new IntRef(arg.getSpan(),that.getName()));
					}

					@Override
					public StaticArg forNatParam(NatParam that) {
						return new IntArg(arg.getSpan(), new IntRef(arg.getSpan(),that.getName()));
					}

					@Override
					public StaticArg forOpParam(OpParam that) {
						return new OpArg(arg.getSpan(), ExprFactory.makeOpRef(that.getName()));
					}

					@Override
					public StaticArg forTypeParam(TypeParam that) {
						return new TypeArg(arg.getSpan(),t);
					}

					@Override
					public StaticArg forUnitParam(UnitParam that) {
						return new UnitArg(arg.getSpan(), new UnitRef(arg.getSpan(),that.getName()));
					}

				};
				return param.unwrap().accept(v);
			}
		}
		return new TypeArg(arg.getSpan(),t);
	}

}
