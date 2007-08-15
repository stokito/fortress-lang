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

package com.sun.fortress.compiler;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.collect.HashRelation;
import edu.rice.cs.plt.tuple.Option;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.nodes.*;
import com.sun.fortress.compiler.index.*;

import com.sun.fortress.useful.NI;

public class IndexBuilder {

    /** Result of {@link #buildApis}. */
    public static class ApiResult extends StaticPhaseResult {
        private final Map<String, ApiIndex> _apis;

        public ApiResult(Map<String, ApiIndex> apis,
                         Iterable<? extends StaticError> errors) {
            super(errors);
            _apis = apis;
        }

        public Map<String, ApiIndex> apis() { return _apis; }
    }

    /** Convert the given ASTs to ApiIndices. */
    public static ApiResult buildApis(Iterable<Api> asts) {
        IndexBuilder builder = new IndexBuilder();
        Map<String, ApiIndex> apis = new HashMap<String, ApiIndex>();
        for (Api ast : asts) { builder.buildApi(ast, apis); }
        return new ApiResult(apis, builder.errors());
    }


    /** Result of {@link #buildComponents}. */
    public static class ComponentResult extends StaticPhaseResult {
        private final Map<String, ComponentIndex> _components;

        public ComponentResult(Map<String, ComponentIndex> components,
                               Iterable<? extends StaticError> errors) {
            super(errors);
            _components = components;
        }

        public Map<String, ComponentIndex> components() { return _components; }
    }

    /** Convert the given ASTs to ComponentIndices. */
    public static ComponentResult buildComponents(Iterable<Component> asts) {
        IndexBuilder builder = new IndexBuilder();
        Map<String, ComponentIndex> components = new HashMap<String, ComponentIndex>();
        for (Component ast : asts) { builder.buildComponent(ast, components); }
        return new ComponentResult(components, builder.errors());
    }


    private List<StaticError> _errors;

    private IndexBuilder() { _errors = new LinkedList<StaticError>(); }

    private List<StaticError> errors() { return _errors; }

    private void error(String message, HasAt loc) {
        _errors.add(StaticError.make(message, loc));
    }

    /** Create an ApiIndex and add it to the given map. */
    private void buildApi(Api ast, Map<String, ApiIndex> apis) {
        final Map<String, Variable> variables = new HashMap<String, Variable>();
        final Relation<String, Function> functions =
            new HashRelation<String, Function>(true, false);
        final Map<String, TraitIndex> traits = new HashMap<String, TraitIndex>();
        NodeAbstractVisitor_void handleDecl = new NodeAbstractVisitor_void() {
            @Override public void forAbsTraitDecl(AbsTraitDecl d) {
                buildTrait(d, traits, functions);
            }
            @Override public void forAbsObjectDecl(AbsObjectDecl d) {
                buildObject(d, traits, functions, variables);
            }
            @Override public void forAbsVarDecl(AbsVarDecl d) {
                buildVariables(d, variables);
            }
            @Override public void forAbsFnDecl(AbsFnDecl d) {
                buildFunction(d, functions);
            }
            @Override public void forDimUnitDecl(DimUnitDecl d) {
                NI.nyi();
            }
            @Override public void forTypeAlias(TypeAlias d) {
                NI.nyi();
            }
            @Override public void forTestDecl(TestDecl d) {
                NI.nyi();
            }
            @Override public void forPropertyDecl(PropertyDecl d) {
                NI.nyi();
            }
            @Override public void forAbsExternalSyntax(AbsExternalSyntax d) {
                NI.nyi();
            }
        };
        for (AbsDecl decl : ast.getDecls()) {
            decl.accept(handleDecl);
        }
        ApiIndex api = new ApiIndex(ast, variables, functions, traits);
        apis.put(NodeUtil.getName(ast.getDottedId()), api);
    }

    /** Create a ComponentIndex and add it to the given map. */
    private void buildComponent(Component ast,
                                Map<String, ComponentIndex> components) {
        final Map<String, Variable> variables = new HashMap<String, Variable>();
        final Set<VarDecl> initializers = new HashSet<VarDecl>();
        final Relation<String, Function> functions =
            new HashRelation<String, Function>(true, false);
        final Map<String, TraitIndex> traits = new HashMap<String, TraitIndex>();
        NodeAbstractVisitor_void handleDecl = new NodeAbstractVisitor_void() {
            @Override public void forTraitDecl(TraitDecl d) {
                buildTrait(d, traits, functions);
            }
            @Override public void forObjectDecl(ObjectDecl d) {
                buildObject(d, traits, functions, variables);
            }
            @Override public void forVarDecl(VarDecl d) {
                buildVariables(d, variables); initializers.add(d);
            }
            @Override public void forAbsFnDecl(AbsFnDecl d) {
                buildFunction(d, functions);
            }
            @Override public void forFnDecl(FnDecl d) {
                buildFunction(d, functions);
            }
            @Override public void forDimUnitDecl(DimUnitDecl d) {
                NI.nyi();
            }
            @Override public void forTypeAlias(TypeAlias d) {
                NI.nyi();
            }
            @Override public void forTestDecl(TestDecl d) {
                NI.nyi();
            }
            @Override public void forPropertyDecl(PropertyDecl d) {
                NI.nyi();
            }
            @Override public void forAbsExternalSyntax(AbsExternalSyntax d) {
                NI.nyi();
            }
        };
        for (Decl decl : ast.getDecls()) {
            decl.accept(handleDecl);
        }
        ComponentIndex comp = new ComponentIndex(ast, variables, initializers,
                                                 functions, traits);
        components.put(NodeUtil.getName(ast.getDottedId()), comp);
    }


    /**
     * Create a ProperTraitIndex and put it in the given map; add functional methods
     * to the given relation.
     */
    private void buildTrait(TraitAbsDeclOrDecl ast, Map<String, TraitIndex> traits,
                            final Relation<String, Function> functions) {
        final String name = ast.getId().getName();
        final Map<String, Method> getters = new HashMap<String, Method>();
        final Map<String, Method> setters = new HashMap<String, Method>();
        final Set<Function> coercions = new HashSet<Function>();
        final Relation<String, Method> dottedMethods =
            new HashRelation<String, Method>(true, false);
        final Relation<String, FunctionalMethod> functionalMethods =
            new HashRelation<String, FunctionalMethod>(true, false);
        NodeAbstractVisitor_void handleDecl = new NodeAbstractVisitor_void() {
            @Override public void forAbsVarDecl(AbsVarDecl d) {
                buildTraitFields(d, name, getters, setters);
            }
            @Override public void forFnAbsDeclOrDecl(FnAbsDeclOrDecl d) {
                buildMethod(d, name, getters, setters, coercions, dottedMethods,
                            functionalMethods, functions);
            }
            @Override public void forPropertyDecl(PropertyDecl d) {
                NI.nyi();
            }
        };
        for (AbsDeclOrDecl decl : ast.getDecls()) {
            decl.accept(handleDecl);
        }
        TraitIndex trait = new ProperTraitIndex(ast, getters, setters, coercions,
                                                dottedMethods, functionalMethods);
        traits.put(name, trait);
    }

    /**
     * Create an ObjectTraitIndex and put it in the given map; add functional methods
     * to the given relation; create a constructor function or singleton variable and
     * put it in the appropriate map.
     */
    private void buildObject(ObjectAbsDeclOrDecl ast,
                             Map<String, TraitIndex> traits,
                             final Relation<String, Function> functions,
                             Map<String, Variable> variables) {
        final String name = ast.getId().getName();
        final Map<String, Variable> fields = new HashMap<String, Variable>();
        final Set<VarDecl> initializers = new HashSet<VarDecl>();
        final Map<String, Method> getters = new HashMap<String, Method>();
        final Map<String, Method> setters = new HashMap<String, Method>();
        final Set<Function> coercions = new HashSet<Function>();
        final Relation<String, Method> dottedMethods =
            new HashRelation<String, Method>(true, false);
        final Relation<String, FunctionalMethod> functionalMethods =
            new HashRelation<String, FunctionalMethod>(true, false);

        Option<Constructor> constructor;
        if (ast.getParams().isSome()) {
            for (Param p : Option.unwrap(ast.getParams())) {
                fields.put(p.getId().getName(), new ParamVariable(p));
            }
            Constructor c = new Constructor(name);
            constructor = Option.some(c);
            functions.add(name, c);
        }
        else {
            constructor = Option.none();
            variables.put(name, new SingletonVariable(name));
        }

        NodeAbstractVisitor_void handleDecl = new NodeAbstractVisitor_void() {
            @Override public void forAbsVarDecl(AbsVarDecl d) {
                buildFields(d, name, fields, getters, setters);
            }
            @Override public void forVarDecl(VarDecl d) {
                buildFields(d, name, fields, getters, setters);
                initializers.add(d);
            }
            @Override public void forFnAbsDeclOrDecl(FnAbsDeclOrDecl d) {
                buildMethod(d, name, getters, setters, coercions, dottedMethods,
                            functionalMethods, functions);
            }
            @Override public void forPropertyDecl(PropertyDecl d) {
                NI.nyi();
            }
        };
        for (AbsDeclOrDecl decl : ast.getDecls()) {
            decl.accept(handleDecl);
        }
        TraitIndex trait = new ObjectTraitIndex(ast, constructor, fields, initializers,
                                                getters, setters, coercions,
                                                dottedMethods, functionalMethods);
        traits.put(name, trait);
    }


    /**
     * Create a variable wrapper for each declared variable and add it to the given
     * map.
     */
    private void buildVariables(VarAbsDeclOrDecl ast,
                                Map<String, Variable> variables) {
        for (LValueBind b : ast.getLhs()) {
            variables.put(b.getId().getName(), new DeclaredVariable(b));
        }
    }

    /**
     * Create and add to the given maps implicit getters and setters for a trait's
     * abstract fields.
     */
    private void buildTraitFields(AbsVarDecl ast,
                                  String declaringTrait,
                                  Map<String, Method> getters,
                                  Map<String, Method> setters) {
        for (LValueBind b : ast.getLhs()) {
            ModifierSet mods = extractModifiers(b.getMods());
            // TODO: check for correct modifiers?
            String name = b.getId().getName();
            if (!mods.isHidden) {
                getters.put(name, new FieldGetterMethod(b, declaringTrait));
            }
            if (mods.isSettable) {
                setters.put(name, new FieldSetterMethod(b, declaringTrait));
            }
        }
    }

    /**
     * Create field variables and add them to the given map; also create implicit
     * getters and setters.
     */
    private void buildFields(VarAbsDeclOrDecl ast,
                             String declaringTrait,
                             Map<String, Variable> fields,
                             Map<String, Method> getters,
                             Map<String, Method> setters) {
        for (LValueBind b : ast.getLhs()) {
            ModifierSet mods = extractModifiers(b.getMods());
            // TODO: check for correct modifiers?
            String name = b.getId().getName();
            fields.put(name, new DeclaredVariable(b));
            if (!mods.isHidden) {
                getters.put(name, new FieldGetterMethod(b, declaringTrait));
            }
            if (mods.isSettable) {
                setters.put(name, new FieldSetterMethod(b, declaringTrait));
            }
        }
    }


    /**
     * Create a function wrapper for the declaration and put it in the given
     * relation.
     */
    private void buildFunction(FnAbsDeclOrDecl ast,
                               Relation<String, Function> functions) {
        functions.add(NodeUtil.getName(ast.getFnName()), new DeclaredFunction(ast));
    }

    /**
     * Determine whether the given declaration is a getter, setter, coercion, dotted
     * method, or functional method, and add it to the appropriate map; also store
     * functional methods with top-level functions.
     */
    private void buildMethod(FnAbsDeclOrDecl ast,
                             String declaringTrait,
                             Map<String, Method> getters,
                             Map<String, Method> setters,
                             Set<Function> coercions,
                             Relation<String, Method> dottedMethods,
                             Relation<String, FunctionalMethod> functionalMethods,
                             Relation<String, Function> topLevelFunctions) {
        ModifierSet mods = extractModifiers(ast.getMods());
        // TODO: check for correct modifiers?
        String name = NodeUtil.getName(ast.getFnName());
        if (mods.isGetter) {
            getters.put(name, new DeclaredMethod(ast, declaringTrait));
        }
        else if (mods.isSetter) {
            setters.put(name, new DeclaredMethod(ast, declaringTrait));
        }
        else if (name.equals("coercion")) {
            coercions.add(new DeclaredFunction(ast));
        }
        else {
            boolean functional = false;
            for (Param p : ast.getParams()) {
                // TODO: make sure param is valid (for ex., self doesn't have a type)
                if (p.getId().getName().equals("self")) {
                    if (functional) {
                        error("'self' appears twice in a method declaration", ast);
                        return;
                    }
                    functional = true;
                }
            }
            if (functional) {
                FunctionalMethod m = new FunctionalMethod(ast, declaringTrait);
                functionalMethods.add(name, m);
                topLevelFunctions.add(name, m);
            }
            else { dottedMethods.add(name, new DeclaredMethod(ast, declaringTrait)); }
        }
    }


    private ModifierSet extractModifiers(List<Modifier> mods) {
        final ModifierSet result = new ModifierSet();
        NodeAbstractVisitor_void handleMod = new NodeAbstractVisitor_void() {
            @Override public void forModifierVar(ModifierVar m) {
                result.isVar = true;
            }
            @Override public void forModifierHidden(ModifierHidden m) {
                result.isHidden = true;
            }
            @Override public void forModifierSettable(ModifierSettable m) {
                result.isSettable = true;
            }
            @Override public void forModifierTransient(ModifierTransient m) {
                result.isTransient = true;
            }
            @Override public void forModifierAbstract(ModifierAbstract m) {
                result.isAbstract = true;
            }
            @Override public void forModifierAtomic(ModifierAtomic m) {
                result.isAtomic = true;
            }
            @Override public void forModifierIO(ModifierIO m) {
                result.isIO = true;
            }
            @Override public void forModifierGetter(ModifierGetter m) {
                result.isGetter = true;
            }
            @Override public void forModifierSetter(ModifierSetter m) {
                result.isSetter = true;
            }
            @Override public void forModifierOverride(ModifierOverride m) {
                result.isOverride = true;
            }
            @Override public void forModifierPrivate(ModifierPrivate m) {
                result.isPrivate = true;
            }
            @Override public void forModifierWidens(ModifierWidens m) {
                result.isWidens = true;
            }
            @Override public void forModifierTest(ModifierTest m) {
                result.isTest = true;
            }
            @Override public void forModifierValue(ModifierValue m) {
                result.isValue = true;
            }
        };
        // TODO: check for duplicates?
        for (Modifier m : mods) { m.accept(handleMod); }
        return result;
    }


    private static class ModifierSet {
        public boolean isVar = false;
        public boolean isHidden = false;
        public boolean isSettable = false;
        public boolean isTransient = false;
        public boolean isAbstract = false;
        public boolean isAtomic = false;
        public boolean isIO = false;
        public boolean isGetter = false;
        public boolean isSetter = false;
        public boolean isOverride = false;
        public boolean isPrivate = false;
        public boolean isWidens = false;
        public boolean isTest = false;
        public boolean isValue = false;
    }

}
