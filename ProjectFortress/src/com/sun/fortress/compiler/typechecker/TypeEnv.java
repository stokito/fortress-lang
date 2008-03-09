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

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.compiler.index.CompilationUnitIndex;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.index.Method;
import com.sun.fortress.compiler.index.Variable;
import com.sun.fortress.compiler.index.ParamVariable;
import com.sun.fortress.compiler.index.SingletonVariable;
import com.sun.fortress.compiler.index.DeclaredVariable;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Option;
import java.util.*;

import static com.sun.fortress.nodes_util.NodeFactory.*;
import static edu.rice.cs.plt.tuple.Option.*;

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
        //typeEnv.extend(component.functions());

        // Iterate over top-level variables, adding each to the component-level environment.
        typeEnv = typeEnv.extend(cu.variables());

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
            // Convert the declared VarargsType into a reference to
            // FortressBuiltin.ImmutableHeapSequence.
            VarargsParam _param = (VarargsParam) param;

            Type result = Types.fromVarargsType(_param.getVarargsType());
            return some(result);
        }
    }

    /**
     * Get a type from a list of params.
     */
    protected static Type typeFromParams(List<Param> params) {
        List<Type> paramTypes = new ArrayList<Type>();
        List<KeywordType> keywordTypes = new ArrayList<KeywordType>();
        Option<VarargsType> varargsType = none();

        for (Param param: params) {
            if (param instanceof NormalParam) {
                NormalParam _param = (NormalParam) param;
                Option<Type> maybeType = _param.getType();

                if (maybeType.isSome()) { // An explicit type is declared.
                    if (_param.getDefaultExpr().isSome()) { // We have a keyword param.
                        keywordTypes.add(makeKeywordType(_param.getName(), unwrap(maybeType)));
                    } else { // We have an ordinary param.
                        paramTypes.add(unwrap(maybeType));
                    }
                } else { // No type is explicitly declared for this parameter.
                    if (_param.getDefaultExpr().isSome()) { // We have a keyword param.
                        keywordTypes.add(makeKeywordType(_param.getName(), NodeFactory.makeInferenceVarType()));
                    } else { // We have an ordinary param.
                        paramTypes.add(NodeFactory.makeInferenceVarType());
                    }
                }
            } else { // We have a varargs param.
                VarargsParam _param = (VarargsParam) param;
                varargsType = wrap(_param.getVarargsType());
            }
        }
        return makeArgType(new Span(), paramTypes, keywordTypes, varargsType);
    }

    protected static List<StaticArg> staticParamsToArgs(List<StaticParam> params) {
        List<StaticArg> result = new ArrayList<StaticArg>();

        for (StaticParam param: params) {
            result.add(param.accept(new NodeAbstractVisitor<StaticArg>() {
                public StaticArg forOperatorParam(OperatorParam that) {
                    return new OprArg(new Span(), that.getName());
                }
                public StaticArg forIdStaticParam(IdStaticParam that) {
                    return new IdArg(new Span(), makeQualifiedIdName(that.getName()));
                }
                public StaticArg forBoolParam(BoolParam that) {
                    return new BoolArg(new Span(), new BoolRef(new Span(), makeQualifiedIdName(that.getName())));
                }
                public StaticArg forDimensionParam(DimensionParam that) {
                    return new DimArg(new Span(), new DimRef(new Span(), makeQualifiedIdName(that.getName())));
                }
                public StaticArg forIntParam(IntParam that) {
                    return new IntArg(new Span(), new IntRef(new Span(), makeQualifiedIdName(that.getName())));
                }
                public StaticArg forNatParam(NatParam that) {
                    return new IntArg(new Span(), new IntRef(new Span(), makeQualifiedIdName(that.getName())));
                }
                public StaticArg forSimpleTypeParam(SimpleTypeParam that) {
                    return new IdArg(new Span(), makeQualifiedIdName(that.getName()));
                }
                public StaticArg forUnitParam(UnitParam that) {
                    return new UnitArg(new Span(), new VarRef(new Span(), makeQualifiedIdName(that.getName())));
                }
            }));
        }
        return result;
    }

    /**
     * Return a BindingLookup that binds the given SimpleName to a type
     * (if the given SimpleName is in this type environment).
     */
    public abstract Option<BindingLookup> binding(SimpleName var);

    /**
     * Return the type of the given SimpleName (if the given SimpleName is in
     * this type environment).
     */
    public final Option<Type> type(SimpleName var) {
        Option<BindingLookup> _binding = binding(var);
        if (_binding.isSome()) {
            Option<Type> type = unwrap(_binding).getType();
            if (type.isSome()) {
                return type;
            } else {
                // When an explicit type is not given in the source code, the
                // type environment returns a fresh implicit type. Note that
                // a distinct implicit type is returned each time type() is
                // called. This is necessary because TypeEnvs are immutable.
                // It's up to the type checker to accumulate the constraints
                // on implicit types.
                return Option.<Type>wrap(NodeFactory.makeInferenceVarType());
            }
        } else {
            return Option.none();
        }
    }

    /**
     * Return the list of modifiers for the given SimpleName (if that
     * SimpleName is in this type environment).
     */
    public final Option<List<Modifier>> mods(SimpleName var) {
        Option<BindingLookup> binding = binding(var);

        if (binding.isSome()) { return wrap(unwrap(binding).getMods()); }
        else { return Option.none(); }
    }

    /**
     * Indicate whether the given SimpleName is bound as a mutable
     * variable (if the given SimpleName is in this type environment).
     */
    public final Option<Boolean> mutable(SimpleName var) {
        Option<BindingLookup> binding = binding(var);

        if (binding.isSome()) { return wrap(unwrap(binding).isMutable()); }
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
     * corresponding Id in this type environment.
     */
    public final Option<Boolean> mutable(String var) {
        return mutable(makeId(var));
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

    public final TypeEnv extendWithFunctions(Relation<SimpleName, ? extends Function> fns) {
        if (fns.size() == 0) { return this; }
        else { return new FnTypeEnv(fns, this); }
    }

    public final TypeEnv extendWithFnDefs(Relation<SimpleName, ? extends FnDef> fns) {
        if (fns.size() == 0) { return this; }
        else { return new FnDefTypeEnv(fns, this); }
    }

    public final TypeEnv extendWithMethods(Relation<SimpleName, Method> methods) {
        if (methods.size() == 0) { return this; }
        else { return new MethodTypeEnv(methods, this); }
    }

    public final TypeEnv extendWithParams(List<Param> params) {
        if (params.size() == 0) { return this; }
        else { return new ParamTypeEnv(params, this); }
    }

    public final TypeEnv extend(Option<List<Param>> params) {
        if (params.isNone()) { return this; }
        else { return extendWithParams(unwrap(params)); }
    }

    public final TypeEnv extend(Param param) {
        return new ParamTypeEnv(Arrays.asList(param), this);
    }
    
    /**
     * A wrapper around the binding found in the TypeEnv.  Since some bindings
     * do not have an Id to be indexed, there is no way to create the LValueBind
     * node to represent the binding.  In the case of operators, for example,
     * only a SimpleName exists, so the BindingLookup exports the same methods
     * that LValueBind does, since an LValueBind cannot be created.
     */
    public static class BindingLookup {

        private final SimpleName var;
        private final Option<Type> type;
        private final List<Modifier> mods;
        private final boolean mutable;

        public BindingLookup(LValueBind binding) {
            var = binding.getName();
            type = binding.getType();
            mods = binding.getMods();
            mutable = binding.isMutable();
        }

        public BindingLookup(SimpleName _var, FnAbsDeclOrDecl decl) {
            var = _var;
            type = wrap((Type)makeGenericArrowType(decl.getSpan(),
                    decl.getStaticParams(),
                    typeFromParams(decl.getParams()),
                    unwrap(decl.getReturnType()), // all types have been filled in at this point
                    decl.getThrowsClause(),
                    decl.getWhere()));
            mods = decl.getMods();
            mutable = false;
        }

        public BindingLookup(SimpleName _var, Collection<? extends FnAbsDeclOrDecl> decls) {
            var = _var;
            Type _type = Types.ANY;
            mods = Collections.<Modifier>emptyList();
            for (FnAbsDeclOrDecl decl : decls) {
                _type = new AndType(_type,
                        makeGenericArrowType(decl.getSpan(),
                                decl.getStaticParams(),
                                typeFromParams(decl.getParams()),
                                unwrap(decl.getReturnType()), // all types have been filled in at this point
                                decl.getThrowsClause(),
                                decl.getWhere()));
                mods.addAll(decl.getMods());
            }
            type = wrap(_type);
            mutable = false;
        }

        public BindingLookup(SimpleName _var, Type _type) {
            this(_var, _type, Collections.<Modifier>emptyList(), false);
        }

        public BindingLookup(SimpleName _var, Option<Type> _type) {
            this(_var, _type, Collections.<Modifier>emptyList(), false);
        }

        public BindingLookup(SimpleName _var, Type _type, List<Modifier> _mods) {
            this(_var, _type, _mods, false);
        }

        public BindingLookup(SimpleName _var, Option<Type> _type, List<Modifier> _mods) {
            this(_var, _type, _mods, false);
        }

        public BindingLookup(SimpleName _var, Type _type, List<Modifier> _mods, boolean _mutable) {
            var = _var;
            type = some(_type);
            mods = _mods;
            mutable = _mutable;
        }

        public BindingLookup(SimpleName _var, Option<Type> _type, List<Modifier> _mods, boolean _mutable) {
            var = _var;
            type = _type;
            mods = _mods;
            mutable = _mutable;
        }

        public SimpleName getVar() { return var; }
        public Option<Type> getType() { return type; }
        public List<Modifier> getMods() { return mods; }
        public boolean isMutable() { return mutable; }

    };
}
