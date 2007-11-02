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

import java.util.ArrayList;
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
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.nodes.*;
import com.sun.fortress.compiler.index.*;

import com.sun.fortress.useful.NI;

public class IndexBuilder {

    /** Result of {@link #buildApis}. */
    public static class ApiResult extends StaticPhaseResult {
        private final Map<DottedName, ApiIndex> _apis;

        public ApiResult(Map<DottedName, ApiIndex> apis,
                         Iterable<? extends StaticError> errors) {
            super(errors);
            _apis = apis;
        }

        public Map<DottedName, ApiIndex> apis() { return _apis; }
    }

    /** Convert the given ASTs to ApiIndices. */
    public static ApiResult buildApis(Iterable<Api> asts) {
        IndexBuilder builder = new IndexBuilder();
        Map<DottedName, ApiIndex> apis = new HashMap<DottedName, ApiIndex>();
        for (Api ast : asts) { builder.buildApi(ast, apis); }
        return new ApiResult(apis, builder.errors());
    }
    
    /** Convenience function that takes apis as varargs and builds an ApiResult. */
    public static ApiResult buildApis(Api... asts) {
        ArrayList<Api> apiList = new ArrayList<Api>();
        for (Api ast: asts) { apiList.add(ast); }
        return buildApis(apiList);
    }


    /** Result of {@link #buildComponents}. */
    public static class ComponentResult extends StaticPhaseResult {
        private final Map<DottedName, ComponentIndex> _components;

        public ComponentResult(Map<DottedName, ComponentIndex> components,
                               Iterable<? extends StaticError> errors) {
            super(errors);
            _components = components;
        }

        public Map<DottedName, ComponentIndex> components() { return _components; }
    }

    /** Convert the given ASTs to ComponentIndices. */
    public static ComponentResult buildComponents(Iterable<Component> asts) {
        IndexBuilder builder = new IndexBuilder();
        Map<DottedName, ComponentIndex> components =
          new HashMap<DottedName, ComponentIndex>();
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
    private void buildApi(Api ast, Map<DottedName, ApiIndex> apis) {
        final Map<IdName, Variable> variables = new HashMap<IdName, Variable>();
        final Relation<SimpleName, Function> functions =
          new HashRelation<SimpleName, Function>(true, false);
        final Map<IdName, TypeConsIndex> typeConses =
            new HashMap<IdName, TypeConsIndex>();
        NodeAbstractVisitor_void handleDecl = new NodeAbstractVisitor_void() {
            @Override public void forAbsTraitDecl(AbsTraitDecl d) {
                buildTrait(d, typeConses, functions);
            }
            @Override public void forAbsObjectDecl(AbsObjectDecl d) {
                buildObject(d, typeConses, functions, variables);
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
        ApiIndex api = new ApiIndex(ast, variables, functions, typeConses);
        apis.put(ast.getName(), api);
    }

    /** Create a ComponentIndex and add it to the given map. */
    private void buildComponent(Component ast,
                                Map<DottedName, ComponentIndex> components) {
        final Map<IdName, Variable> variables = new HashMap<IdName, Variable>();
        final Set<VarDecl> initializers = new HashSet<VarDecl>();
        final Relation<SimpleName, Function> functions =
            new HashRelation<SimpleName, Function>(true, false);
        final Map<IdName, TypeConsIndex> typeConses =
            new HashMap<IdName, TypeConsIndex>();
        NodeAbstractVisitor_void handleDecl = new NodeAbstractVisitor_void() {
            @Override public void forTraitDecl(TraitDecl d) {
                buildTrait(d, typeConses, functions);
            }
            @Override public void forObjectDecl(ObjectDecl d) {
                buildObject(d, typeConses, functions, variables);
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
                                                 functions, typeConses);
        components.put(ast.getName(), comp);
    }


    /**
     * Create a ProperTraitIndex and put it in the given map; add functional methods
     * to the given relation.
     */
    private void buildTrait(TraitAbsDeclOrDecl ast,
                            Map<IdName, TypeConsIndex> typeConses,
                            final Relation<SimpleName, Function> functions) {
        final IdName name = ast.getName();
        final Map<IdName, Method> getters = new HashMap<IdName, Method>();
        final Map<IdName, Method> setters = new HashMap<IdName, Method>();
        final Set<Function> coercions = new HashSet<Function>();
        final Relation<SimpleName, Method> dottedMethods =
            new HashRelation<SimpleName, Method>(true, false);
        final Relation<SimpleName, FunctionalMethod> functionalMethods =
            new HashRelation<SimpleName, FunctionalMethod>(true, false);
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
        typeConses.put(name, trait);
    }

    /**
     * Create an ObjectTraitIndex and put it in the given map; add functional methods
     * to the given relation; create a constructor function or singleton variable and
     * put it in the appropriate map.
     */
    private void buildObject(ObjectAbsDeclOrDecl ast,
                             Map<IdName, TypeConsIndex> typeConses,
                             final Relation<SimpleName, Function> functions,
                             Map<IdName, Variable> variables) {
        final IdName name = ast.getName();
        final Map<IdName, Variable> fields = new HashMap<IdName, Variable>();
        final Set<VarDecl> initializers = new HashSet<VarDecl>();
        final Map<IdName, Method> getters = new HashMap<IdName, Method>();
        final Map<IdName, Method> setters = new HashMap<IdName, Method>();
        final Set<Function> coercions = new HashSet<Function>();
        final Relation<SimpleName, Method> dottedMethods =
            new HashRelation<SimpleName, Method>(true, false);
        final Relation<SimpleName, FunctionalMethod> functionalMethods =
            new HashRelation<SimpleName, FunctionalMethod>(true, false);

        Option<Constructor> constructor;
        if (ast.getParams().isSome()) {
            for (Param p : Option.unwrap(ast.getParams())) {
                fields.put(p.getName(), new ParamVariable(p));
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
        typeConses.put(name, trait);
    }


    /**
     * Create a variable wrapper for each declared variable and add it to the given
     * map.
     */
    private void buildVariables(VarAbsDeclOrDecl ast,
                                Map<IdName, Variable> variables) {
        for (LValueBind b : ast.getLhs()) {
            variables.put(b.getName(), new DeclaredVariable(b));
        }
    }

    /**
     * Create and add to the given maps implicit getters and setters for a trait's
     * abstract fields.
     */
    private void buildTraitFields(AbsVarDecl ast,
                                  IdName declaringTrait,
                                  Map<IdName, Method> getters,
                                  Map<IdName, Method> setters) {
        for (LValueBind b : ast.getLhs()) {
            ModifierSet mods = extractModifiers(b.getMods());
            // TODO: check for correct modifiers?
            IdName name = b.getName();
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
                             IdName declaringTrait,
                             Map<IdName, Variable> fields,
                             Map<IdName, Method> getters,
                             Map<IdName, Method> setters) {
        for (LValueBind b : ast.getLhs()) {
            ModifierSet mods = extractModifiers(b.getMods());
            // TODO: check for correct modifiers?
            IdName name = b.getName();
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
                               Relation<SimpleName, Function> functions) {
        functions.add(ast.getName(), new DeclaredFunction(ast));
    }

    /**
     * Determine whether the given declaration is a getter, setter, coercion, dotted
     * method, or functional method, and add it to the appropriate map; also store
     * functional methods with top-level functions.
     */
    private void buildMethod(FnAbsDeclOrDecl ast,
                             IdName declaringTrait,
                             Map<IdName, Method> getters,
                             Map<IdName, Method> setters,
                             Set<Function> coercions,
                             Relation<SimpleName, Method> dottedMethods,
                             Relation<SimpleName, FunctionalMethod> functionalMethods,
                             Relation<SimpleName, Function> topLevelFunctions) {
        ModifierSet mods = extractModifiers(ast.getMods());
        // TODO: check for correct modifiers?
        SimpleName name = ast.getName();
        if (mods.isGetter) {
            if (name instanceof IdName) {
                getters.put((IdName) name, new DeclaredMethod(ast, declaringTrait));
            }
            else {
                String s = NodeUtil.nameString(name);
                error("Getter declared with an operator name, '" + s + "'", ast);
            }
        }
        else if (mods.isSetter) {
            if (name instanceof IdName) {
                setters.put((IdName) name, new DeclaredMethod(ast, declaringTrait));
            }
            else {
                String s = NodeUtil.nameString(name);
                error("Getter declared with an operator name, '" + s + "'", ast);
            }
        }
        else if (name.equals(COERCION_NAME)) {
            coercions.add(new DeclaredFunction(ast));
        }
        else {
            boolean functional = false;
            for (Param p : ast.getParams()) {
                // TODO: make sure param is valid (for ex., self doesn't have a type)
                if (p.getName().equals(SELF_NAME)) {
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
    
    private static final IdName COERCION_NAME = NodeFactory.makeIdName("coercion");
    private static final IdName SELF_NAME = NodeFactory.makeIdName("self");

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
