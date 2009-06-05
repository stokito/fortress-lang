/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.typechecker;

import static com.sun.fortress.nodes_util.NodeFactory.makeEffect;
import static com.sun.fortress.nodes_util.NodeFactory.makeId;
import static com.sun.fortress.nodes_util.NodeFactory.makeKeywordType;
import static com.sun.fortress.nodes_util.NodeFactory.typeSpan;
import static edu.rice.cs.plt.tuple.Option.none;
import static edu.rice.cs.plt.tuple.Option.some;
import static edu.rice.cs.plt.tuple.Option.wrap;
import static com.sun.fortress.scala_src.useful.Lists.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.index.CompilationUnitIndex;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.index.Method;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.compiler.index.Variable;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AnonymousFnName;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.ConstructorFnName;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.KeywordType;
import com.sun.fortress.nodes.KindBool;
import com.sun.fortress.nodes.KindDim;
import com.sun.fortress.nodes.KindInt;
import com.sun.fortress.nodes.KindNat;
import com.sun.fortress.nodes.KindOp;
import com.sun.fortress.nodes.KindType;
import com.sun.fortress.nodes.KindUnit;
import com.sun.fortress.nodes.LValue;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes._InferenceVarType;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Option;

/**
 * This class is used by the type checker to represent static type environments,
 * mapping bound variables to their types.
 * Where-clause bound variables are not yet supported.
 */
public abstract class TypeEnv {

    /**
     * Construct a new TypeEnv for a given CompilationUnitIndex.
     */
    public static TypeEnv make(CompilationUnitIndex cu) {
        TypeEnv typeEnv = TypeEnv.make();

        // Add all top-level function names to the component-level environment.
        typeEnv = typeEnv.extendWithFunctions(cu.functions());

        // Iterate over top-level variables, adding each to the component-level environment.
        typeEnv = typeEnv.extend(cu.variables());

        //Iterate over top level types, adding each to component-level environment

        typeEnv = typeEnv.extendWithTypeConses(cu.typeConses());


        return typeEnv;
    }

    /**
     * Construct a new TypeEnv from the given bindings.
     */
    public static TypeEnv make(LValue... entries) {
        return EmptyTypeEnv.ONLY.extend(entries);
    }

    /**
     * Construct a new TypeEnv from the given bindings.
     */
    public static TypeEnv make(Map<Id, Variable> entries) {
        return EmptyTypeEnv.ONLY.extend(entries);
    }

    /**
     * Get a type from a Param.
     */
    protected static Option<Type> typeFromParam(Param param) {
        if ( ! NodeUtil.isVarargsParam(param) ) {
            return param.getIdType();
        } else { // a varargs param
            // Convert the declared varargs type into a reference to
            // FortressBuiltin.ImmutableHeapSequence.
            Type result = Types.makeVarargsParamType(param.getVarargsType().unwrap());
            return some(result);
        }
    }

    protected static ArrowType genericArrowFromDecl(FnDecl decl) {
        return NodeFactory.makeArrowType(NodeUtil.getSpan(decl), false,
                             domainFromParams(NodeUtil.getParams(decl)),
                             // all types have been filled in at this point
                             NodeUtil.getReturnType(decl).unwrap(),
                             makeEffect(NodeUtil.getSpan(decl).getEnd(),
                                        NodeUtil.getThrowsClause(decl)),
                             NodeUtil.getStaticParams(decl),
                             NodeUtil.getWhereClause(decl));
    }

    /**
     * Get a domain from a list of params.
     */
    protected static Type domainFromParams(List<Param> params) {
        List<Type> paramTypes = new ArrayList<Type>();
        List<KeywordType> keywordTypes = new ArrayList<KeywordType>();
        Option<Type> varargsType = none();

        for (Param param: params) {
            if ( ! NodeUtil.isVarargsParam(param) ) {
                Option<Type> maybeType = param.getIdType();

                if (maybeType.isSome()) { // An explicit type is declared.
                    if (param.getDefaultExpr().isSome()) { // We have a keyword param.
                        keywordTypes.add(makeKeywordType(param.getName(), maybeType.unwrap()));
                    } else { // We have an ordinary param.
                        paramTypes.add(maybeType.unwrap());
                    }
                } else { // No type is explicitly declared for this parameter.
                    if (param.getDefaultExpr().isSome()) { // We have a keyword param.
                        keywordTypes.add(makeKeywordType(param.getName(), NodeFactory.make_InferenceVarType(NodeUtil.getSpan(param))));
                    } else { // We have an ordinary param.
                        paramTypes.add(NodeFactory.make_InferenceVarType(NodeUtil.getSpan(param)));
                    }
                }
            } else { // We have a varargs param.
                varargsType = some(param.getVarargsType().unwrap());
            }
        }

        return NodeFactory.makeDomain(NodeFactory.makeSpan("TypeEnv_bogus_span_for_empty_list", params), paramTypes, varargsType, keywordTypes);
    }

    public static List<StaticArg> staticParamsToArgs(List<StaticParam> params) {
        List<StaticArg> result = new ArrayList<StaticArg>();

        for (StaticParam param: params) {
            final IdOrOp name = param.getName();
            result.add(param.getKind().accept(new NodeAbstractVisitor<StaticArg>() {
                        public StaticArg forKindBool(KindBool k) {
                            return NodeFactory.makeBoolArg(typeSpan,
                                                           NodeFactory.makeBoolRef(typeSpan, (Id)name));
                        }
                        public StaticArg forKindDim(KindDim k) {
                            return NodeFactory.makeDimArg(typeSpan,
                                                          NodeFactory.makeDimRef(typeSpan, (Id)name));
                        }
                        public StaticArg forKindInt(KindInt k) {
                            return NodeFactory.makeIntArg(typeSpan,
                                                          NodeFactory.makeIntRef(typeSpan, (Id)name));
                        }
                        public StaticArg forKindNat(KindNat k) {
                            return NodeFactory.makeIntArg(typeSpan,
                                                          NodeFactory.makeIntRef(typeSpan, (Id)name));
                        }
                        public StaticArg forKindType(KindType k) {
                            return NodeFactory.makeTypeArg(typeSpan,
                                                           NodeFactory.makeVarType(typeSpan, (Id)name));
                        }
                        public StaticArg forKindUnit(KindUnit k) {
                            return NodeFactory.makeUnitArg(typeSpan, NodeFactory.makeUnitRef(typeSpan, false, (Id)name));
                        }
                        public StaticArg forKindOp(KindOp that) {
                            return NodeFactory.makeOpArg(typeSpan,
                                                         ExprFactory.makeOpRef((Op)name));
                        }
                    }));
        }
        return result;
    }

    static Id removeApi(Id id) {
    	return NodeFactory.makeIdFromLast(id);
    }

    static Op removeApi(Op id) {
    	return NodeFactory.makeOp(NodeUtil.getSpan(id), Option.<APIName>none(),
                                  id.getText(), id.getFixity(), id.isEnclosing());
    }

    static IdOrOpOrAnonymousName removeApi(IdOrOpOrAnonymousName id) {
    	return id.accept(new NodeDepthFirstVisitor<IdOrOpOrAnonymousName>(){
			@Override public IdOrOpOrAnonymousName forId(Id that) { return removeApi(that); }
			@Override
			public IdOrOpOrAnonymousName forOp(Op that) {
                            return NodeFactory.makeOp(NodeUtil.getSpan(that), Option.<APIName>none(),
                                          that.getText(), that.getFixity(), that.isEnclosing());
			}
			@Override
			public IdOrOpOrAnonymousName forAnonymousFnName(AnonymousFnName that) {
				return NodeFactory.makeAnonymousFnName(NodeUtil.getSpan(that),
                                                           Option.<APIName>none());
			}
			@Override
			public IdOrOpOrAnonymousName forConstructorFnName(ConstructorFnName that) {
				return NodeFactory.makeConstructorFnName(NodeUtil.getSpan(that),
                                                             Option.<APIName>none(),
                                                             that.getConstructor());
			}
    	});
    }

    /**
     * Return a BindingLookup that binds the given IdOrOpOrAnonymousName to a type
     * (if the given IdOrOpOrAnonymousName is in this type environment).
     */
    public abstract Option<BindingLookup> binding(IdOrOpOrAnonymousName var);

    /**
     * Return the {@code StaticParam} that declared the given id, or none if it
     * is not in scope.
     */
    public abstract Option<StaticParam> staticParam(IdOrOpOrAnonymousName id);

    /**
     * Return true iff the given id is bound as a StaticParam. 
     */
    public boolean boundStaticParam(Id id) { return staticParam(id).isSome(); }

    /**
     * Return the {@code Node} that declared the given id, or None if the id does not
     * exist. Note that this method should not be passed the id of a function, since
     * functions have multiple declaration sites. You can tell if an id is a function
     * or not by calling type() on the same id.
     *
     * @exception IllegalArgumentException If var is a function, since functions have
     * multiple declaration sites.
     */
    public abstract Option<Node> declarationSite(IdOrOpOrAnonymousName var);

    /**
     * Return the type of the given IdOrOpOrAnonymousName (if the given IdOrOpOrAnonymousName is in
     * this type environment).
     */
    public final Option<Type> getType(IdOrOpOrAnonymousName var) {
        Option<BindingLookup> _binding = binding(var);
        if (_binding.isSome()) {
            Option<Type> type = _binding.unwrap().getType();
            if (type.isSome()) {
                return type;
            } else {
                // When an explicit type is not given in the source code, the
                // type environment returns a fresh implicit type. Note that
                // a distinct implicit type is returned each time type() is
                // called. This is necessary because TypeEnvs are immutable.
                // It's up to the type checker to accumulate the constraints
                // on implicit types.
                return Option.<Type>wrap(NodeFactory.make_InferenceVarType(NodeUtil.getSpan(var)));
            }
        } else {
            return Option.none();
        }
    }

    /**
     * Return the list of modifiers for the given IdOrOpOrAnonymousName (if that
     * IdOrOpOrAnonymousName is in this type environment).
     */
    public final Option<Modifiers> mods(IdOrOpOrAnonymousName var) {
        Option<BindingLookup> binding = binding(var);

        if (binding.isSome()) { return wrap(binding.unwrap().getMods()); }
        else { return Option.none(); }
    }

    /**
     * Indicate whether the given IdOrOpOrAnonymousName is bound as a mutable
     * variable (if the given IdOrOpOrAnonymousName is in this type environment).
     */
    public final Option<Boolean> mutable(IdOrOpOrAnonymousName var) {
        Option<BindingLookup> binding = binding(var);

        if (binding.isSome()) { return wrap(binding.unwrap().isMutable()); }
        else { return Option.none(); }
    }

    /**
     * Convenience method that takes a String and returns the type of the
     * corresponding Id in this type environment.
     */
    public final Option<Type> type(String var) { return getType(makeId(NodeFactory.internalSpan,var)); }

    /**
     * Convenience method that takes a String and returns the modifiers for the
     * corresponding Id in this type environment.
     */
    public final Option<Modifiers> mods(String var) {
        return mods(makeId(NodeFactory.internalSpan,var));
    }

    /**
     * Convenience method that takes a String and indicates whether the
     * corresponding Id in this type environment is mutable or not.
     */
    public final Option<Boolean> mutable(String var) {
        return mutable(makeId(NodeFactory.internalSpan,var));
    }

    public abstract List<BindingLookup> contents();
    public final String description() {
        StringBuffer sb = new StringBuffer();
        sb.append('(');
        for (BindingLookup b : contents()) {
            sb.append(b);
            sb.append(", ");
        }
        sb.delete(sb.length()-2, sb.length());
        sb.append(')');
        return sb.toString();
    }

    /**
     * Produce a new type environment extending this with the given variable bindings.
     * Unfortunately, we must give some variants of 'extend' long names to allow the
     * compiler to distinguish them from other variants with the same _erased_ signature.
     */
    public final TypeEnv extend(LValue... entries) {
        if (entries.length == 0) { return this; }
        else { return new LValueTypeEnv(entries, this); }
    }

    public final TypeEnv extendWithLValues(List<LValue> entries) {
        if (entries.size() == 0) { return this; }
        else { return new LValueTypeEnv(entries, this); }
    }

    public final TypeEnv extend(LocalVarDecl decl) {
        if (decl.getLhs().size() == 0) { return this; }
        else { return new LocalVarTypeEnv(decl, this); }
    }

    public final TypeEnv extend(Map<Id, Variable> vars) {
        if (vars.size() == 0) { return this; }
        else { return new VarTypeEnv(vars, this); }
    }

    public final TypeEnv extendWithFunctions(Relation<IdOrOpOrAnonymousName, ? extends Function> fns) {
        if (fns.size() == 0) { return this; }
        else { return new FnTypeEnv(fns, this); }
    }

    public final TypeEnv extendWithFnDecls(Relation<IdOrOpOrAnonymousName, FnDecl> fns) {
        if (fns.size() == 0) { return this; }
        else { return new FnDeclTypeEnv(fns, this); }
    }

    public final TypeEnv extendWithMethods(Relation<IdOrOpOrAnonymousName, Method> methods) {
        if (methods.size() == 0) { return this; }
        else { return new MethodTypeEnv(methods, this); }
    }

    public final TypeEnv extendWithParams(List<Param> params) {
        if (params.size() == 0) { return this; }
        else { return new ParamTypeEnv(params, this); }
    }

    public final TypeEnv extendWithParams(scala.List<Param> params) {
    	return extendWithParams(toJavaList(params));
    }

    public final TypeEnv extendWithStaticParams(List<StaticParam> params) {
    	if( params.size() == 0 ) {return this; }
    	else { return new StaticParamTypeEnv(params,this); }
    }

    public final TypeEnv extendWithStaticParams(scala.List<StaticParam> params) {
    	return extendWithStaticParams(toJavaList(params));
    }

    public final TypeEnv extendWithTypeConses(Map<Id, TypeConsIndex> typeConses) {
        if (typeConses.isEmpty()) {
            return this;
        } else {
            return new ObjectTypeEnv(typeConses, this);
        }
    }

    public final TypeEnv extend(Option<List<Param>> params) {
        if (params.isNone()) { return this; }
        else { return extendWithParams(params.unwrap()); }
    }

    public final TypeEnv extend(Param param) {
        return new ParamTypeEnv(Arrays.asList(param), this);
    }

    public final TypeEnv extendWithout(Node declSite, Set<? extends IdOrOpOrAnonymousName> entries) {
        return new ConcealingTypeEnv(declSite, entries, this);
    }

    /**
     * A wrapper around the binding found in the TypeEnv.  Since some bindings
     * do not have an Id to be indexed, there is no way to create the LValue
     * node to represent the binding.  In the case of operators, for example,
     * only a IdOrOpOrAnonymousName exists, so the BindingLookup exports the same methods
     * that LValue does, since an LValue cannot be created.
     */
    public static class BindingLookup {

        private final IdOrOpOrAnonymousName var;
        private final Option<Type> type;
        private final Modifiers mods;
        private final boolean mutable;

        public BindingLookup(LValue binding) {
            var = binding.getName();
            type = binding.getIdType();
            mods = binding.getMods();
            mutable = binding.isMutable();
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, FnDecl decl) {
            var = _var;
            type = Option.<Type>wrap(genericArrowFromDecl(decl));
            mods = NodeUtil.getMods(decl);
            mutable = false;
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, Collection<FnDecl> decls) {
            var = _var;
            List<Type> overloads = new ArrayList<Type>();
            Modifiers mods = Modifiers.None;
            for (FnDecl decl : decls) {
                overloads.add(genericArrowFromDecl(decl));
            }
            this.mods = mods;
            type = Option.<Type>some(NodeFactory.makeIntersectionType(NodeFactory.makeSetSpan("impossible", overloads), overloads));
            mutable = false;
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, Type _type) {
            this(_var, _type, Modifiers.None, false);
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, Option<Type> _type) {
            this(_var, _type, Modifiers.None, false);
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, Type _type, Modifiers _mods) {
            this(_var, _type, _mods, false);
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, Option<Type> _type, Modifiers _mods) {
            this(_var, _type, _mods, false);
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, Type _type, Modifiers _mods, boolean _mutable) {
            var = _var;
            type = some(_type);
            mods = _mods;
            mutable = _mutable;
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, Option<Type> _type, Modifiers _mods, boolean _mutable) {
            var = _var;
            type = _type;
            mods = _mods;
            mutable = _mutable;
        }

        public IdOrOpOrAnonymousName getVar() { return var; }
        public Option<Type> getType() { return type; }
        public Modifiers getMods() { return mods; }

        public boolean isMutable() {
            if( mutable )
                return true;

            return mods.isMutable();
        }

        @Override
        public String toString() {
            return String.format("%s:%s", var, type);
        }

    }

    /**
     * Replace all of the inference variables given with their corresponding types.
     */
	public abstract TypeEnv replaceAllIVars(Map<_InferenceVarType, Type> ivars);
}
