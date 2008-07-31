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

package com.sun.fortress.compiler.typechecker;

import static com.sun.fortress.nodes_util.NodeFactory.makeEffect;
import static com.sun.fortress.nodes_util.NodeFactory.makeId;
import static com.sun.fortress.nodes_util.NodeFactory.makeKeywordType;
import static edu.rice.cs.plt.tuple.Option.none;
import static edu.rice.cs.plt.tuple.Option.some;
import static edu.rice.cs.plt.tuple.Option.wrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.index.CompilationUnitIndex;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.index.Method;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.compiler.index.Variable;
import com.sun.fortress.nodes.AnonymousFnName;
import com.sun.fortress.nodes.BoolArg;
import com.sun.fortress.nodes.BoolParam;
import com.sun.fortress.nodes.BoolRef;
import com.sun.fortress.nodes.ConstructorFnName;
import com.sun.fortress.nodes.DimArg;
import com.sun.fortress.nodes.DimParam;
import com.sun.fortress.nodes.DimRef;
import com.sun.fortress.nodes.Domain;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.IntArg;
import com.sun.fortress.nodes.IntParam;
import com.sun.fortress.nodes.IntRef;
import com.sun.fortress.nodes.IntersectionType;
import com.sun.fortress.nodes.KeywordType;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.NatParam;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NormalParam;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpArg;
import com.sun.fortress.nodes.OpParam;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.TypeParam;
import com.sun.fortress.nodes.UnitArg;
import com.sun.fortress.nodes.UnitParam;
import com.sun.fortress.nodes.UnitRef;
import com.sun.fortress.nodes.VarargsParam;
import com.sun.fortress.nodes._InferenceVarType;
import com.sun.fortress.nodes._RewriteGenericArrowType;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Option;

/**
 * This class is used by the type checker to represent static type environments,
 * mapping bound variables to their types.
 */
public abstract class TypeEnv {

    /**
     * Construct a new TypeEnv for a given ApiIndex.
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
    public static TypeEnv make(LValueBind... entries) {
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
        if (param instanceof NormalParam) {
            NormalParam _param = (NormalParam) param;
            return _param.getType();
        } else { // param instanceof VarargsParam
            // Convert the declared varargs type into a reference to
            // FortressBuiltin.ImmutableHeapSequence.
            VarargsParam _param = (VarargsParam) param;

            Type result = Types.makeVarargsParamType(_param.getType());
            return some(result);
        }
    }

    protected static _RewriteGenericArrowType genericArrowFromDecl(FnAbsDeclOrDecl decl) {
        return new _RewriteGenericArrowType(decl.getSpan(),
                                            decl.getStaticParams(),
                                            domainFromParams(decl.getParams()),
                                            // all types have been filled in at this point
                                            decl.getReturnType().unwrap(),
                                            makeEffect(decl.getSpan().getEnd(),
                                                       decl.getThrowsClause()),
                                            decl.getWhere());
    }

    /**
     * Get a domain from a list of params.
     */
    protected static Domain domainFromParams(List<Param> params) {
        List<Type> paramTypes = new ArrayList<Type>();
        List<KeywordType> keywordTypes = new ArrayList<KeywordType>();
        Option<Type> varargsType = none();

        for (Param param: params) {
            if (param instanceof NormalParam) {
                NormalParam _param = (NormalParam) param;
                Option<Type> maybeType = _param.getType();

                if (maybeType.isSome()) { // An explicit type is declared.
                    if (_param.getDefaultExpr().isSome()) { // We have a keyword param.
                        keywordTypes.add(makeKeywordType(_param.getName(), maybeType.unwrap()));
                    } else { // We have an ordinary param.
                        paramTypes.add(maybeType.unwrap());
                    }
                } else { // No type is explicitly declared for this parameter.
                    if (_param.getDefaultExpr().isSome()) { // We have a keyword param.
                        keywordTypes.add(makeKeywordType(_param.getName(), NodeFactory.make_InferenceVarType(_param.getSpan())));
                    } else { // We have an ordinary param.
                        paramTypes.add(NodeFactory.make_InferenceVarType(_param.getSpan()));
                    }
                }
            } else { // We have a varargs param.
                VarargsParam _param = (VarargsParam) param;
                varargsType = some(_param.getType());
            }
        }

        return new Domain(paramTypes, varargsType, keywordTypes);
    }

    public static List<StaticArg> staticParamsToArgs(List<StaticParam> params) {
        List<StaticArg> result = new ArrayList<StaticArg>();

        for (StaticParam param: params) {
            result.add(param.accept(new NodeAbstractVisitor<StaticArg>() {
                public StaticArg forOpParam(OpParam that) {
                    return new OpArg(new Span(), ExprFactory.makeOpRef(that.getName()));
                }
                public StaticArg forBoolParam(BoolParam that) {
                    return new BoolArg(new Span(), new BoolRef(new Span(), that.getName()));
                }
                public StaticArg forDimParam(DimParam that) {
                    return new DimArg(new Span(), new DimRef(new Span(), that.getName()));
                }
                public StaticArg forIntParam(IntParam that) {
                    return new IntArg(new Span(), new IntRef(new Span(), that.getName()));
                }
                public StaticArg forNatParam(NatParam that) {
                    return new IntArg(new Span(), new IntRef(new Span(), that.getName()));
                }
                public StaticArg forTypeParam(TypeParam that) {
                    return new TypeArg(new Span(),
                                       NodeFactory.makeVarType(new Span(),
                                                              that.getName()));
                }
                public StaticArg forUnitParam(UnitParam that) {
                    return new UnitArg(new Span(), new UnitRef(new Span(), that.getName()));
                }
            }));
        }
        return result;
    }

    static Id removeApi(Id id) {
    	return NodeFactory.makeIdFromLast(id);
    }
    
    static Enclosing removeApi(Enclosing id) {
    	return NodeFactory.makeEnclosing(id.getSpan(), id.getOpen(), id.getClose());
    }
    
    static Op removeApi(Op id) {
    	return NodeFactory.makeOp(id.getSpan(), id.getText(), id.getFixity());
    }
    
    static IdOrOpOrAnonymousName removeApi(IdOrOpOrAnonymousName id) {
    	return id.accept(new NodeDepthFirstVisitor<IdOrOpOrAnonymousName>(){
			@Override 
			public IdOrOpOrAnonymousName forEnclosing(Enclosing that) {
				return NodeFactory.makeEnclosing(that.getSpan(), that.getOpen(), that.getClose());
			}
			@Override public IdOrOpOrAnonymousName forId(Id that) { return removeApi(that); }
			@Override
			public IdOrOpOrAnonymousName forOp(Op that) {
				return NodeFactory.makeOp(that.getSpan(), that.getText(), that.getFixity());
			}
			@Override
			public IdOrOpOrAnonymousName forAnonymousFnName(AnonymousFnName that) {
				return new AnonymousFnName(that.getSpan());
			}
			@Override
			public IdOrOpOrAnonymousName forConstructorFnName(ConstructorFnName that) {
				return new ConstructorFnName(that.getSpan(), that.getDef());
			}
    	});
    }
    
    /**
     * Return a BindingLookup that binds the given IdOrOpOrAnonymousName to a type
     * (if the given IdOrOpOrAnonymousName is in this type environment).
     */
    public abstract Option<BindingLookup> binding(IdOrOpOrAnonymousName var);

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
    public final Option<Type> type(IdOrOpOrAnonymousName var) {
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
                return Option.<Type>wrap(NodeFactory.make_InferenceVarType(var.getSpan()));
            }
        } else {
            return Option.none();
        }
    }

    /**
     * Return the list of modifiers for the given IdOrOpOrAnonymousName (if that
     * IdOrOpOrAnonymousName is in this type environment).
     */
    public final Option<List<Modifier>> mods(IdOrOpOrAnonymousName var) {
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
    public final Option<Type> type(String var) { return type(makeId(var)); }

    /**
     * Convenience method that takes a String and returns the modifiers for the
     * corresponding Id in this type environment.
     */
    public final Option<List<Modifier>> mods(String var) {
        return mods(makeId(var));
    }

    /**
     * Convenience method that takes a String and indicates whether the
     * corresponding Id in this type environment is mutable or not.
     */
    public final Option<Boolean> mutable(String var) {
        return mutable(makeId(var));
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
    public final TypeEnv extend(LValueBind... entries) {
        if (entries.length == 0) { return this; }
        else { return new LValueTypeEnv(entries, this); }
    }

    public final TypeEnv extendWithLValues(List<LValueBind> entries) {
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

    public final TypeEnv extendWithFnDefs(Relation<IdOrOpOrAnonymousName, ? extends FnDef> fns) {
        if (fns.size() == 0) { return this; }
        else { return new FnDefTypeEnv(fns, this); }
    }

    public final TypeEnv extendWithMethods(Relation<IdOrOpOrAnonymousName, Method> methods) {
        if (methods.size() == 0) { return this; }
        else { return new MethodTypeEnv(methods, this); }
    }

    public final TypeEnv extendWithParams(List<Param> params) {
        if (params.size() == 0) { return this; }
        else { return new ParamTypeEnv(params, this); }
    }

    public final TypeEnv extendWithStaticParams(List<StaticParam> params) {
    	if( params.size() == 0 ) {return this; }
    	else { return new StaticParamTypeEnv(params,this); }
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
     * do not have an Id to be indexed, there is no way to create the LValueBind
     * node to represent the binding.  In the case of operators, for example,
     * only a IdOrOpOrAnonymousName exists, so the BindingLookup exports the same methods
     * that LValueBind does, since an LValueBind cannot be created.
     */
    public static class BindingLookup {

        private final IdOrOpOrAnonymousName var;
        private final Option<Type> type;
        private final List<Modifier> mods;
        private final boolean mutable;

        public BindingLookup(LValueBind binding) {
            var = binding.getName();
            type = binding.getType();
            mods = binding.getMods();
            mutable = binding.isMutable();
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, FnAbsDeclOrDecl decl) {
            var = _var;
            type = Option.<Type>wrap(genericArrowFromDecl(decl));
            mods = decl.getMods();
            mutable = false;
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, Collection<? extends FnAbsDeclOrDecl> decls) {
            var = _var;
            List<Type> overloads = new ArrayList<Type>();
            mods = Collections.<Modifier>emptyList();
            for (FnAbsDeclOrDecl decl : decls) {
                overloads.add(genericArrowFromDecl(decl));
                mods.addAll(decl.getMods());
            }
            type = Option.<Type>some(new IntersectionType(overloads));
            mutable = false;
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, Type _type) {
            this(_var, _type, Collections.<Modifier>emptyList(), false);
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, Option<Type> _type) {
            this(_var, _type, Collections.<Modifier>emptyList(), false);
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, Type _type, List<Modifier> _mods) {
            this(_var, _type, _mods, false);
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, Option<Type> _type, List<Modifier> _mods) {
            this(_var, _type, _mods, false);
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, Type _type, List<Modifier> _mods, boolean _mutable) {
            var = _var;
            type = some(_type);
            mods = _mods;
            mutable = _mutable;
        }

        public BindingLookup(IdOrOpOrAnonymousName _var, Option<Type> _type, List<Modifier> _mods, boolean _mutable) {
            var = _var;
            type = _type;
            mods = _mods;
            mutable = _mutable;
        }

        public IdOrOpOrAnonymousName getVar() { return var; }
        public Option<Type> getType() { return type; }
        public List<Modifier> getMods() { return mods; }
        public boolean isMutable() { return mutable; }

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
