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

package com.sun.fortress.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.Constructor;
import com.sun.fortress.compiler.index.DeclaredFunction;
import com.sun.fortress.compiler.index.DeclaredMethod;
import com.sun.fortress.compiler.index.DeclaredVariable;
import com.sun.fortress.compiler.index.Dimension;
import com.sun.fortress.compiler.index.FieldGetterMethod;
import com.sun.fortress.compiler.index.FieldSetterMethod;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.index.FunctionalMethod;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.Method;
import com.sun.fortress.compiler.index.NonterminalDefIndex;
import com.sun.fortress.compiler.index.NonterminalExtendIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.compiler.index.ObjectTraitIndex;
import com.sun.fortress.compiler.index.ParamVariable;
import com.sun.fortress.compiler.index.ProperTraitIndex;
import com.sun.fortress.compiler.index.SingletonVariable;
import com.sun.fortress.compiler.index.TraitIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.compiler.index.Unit;
import com.sun.fortress.compiler.index.Variable;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.collect.IndexedRelation;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Option;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

public class IndexBuilder {

    static public IndexBuilder builder = new IndexBuilder();

    /** Result of {@link #buildApis}. */
    public static class ApiResult extends StaticPhaseResult {
        private final Map<APIName, ApiIndex> _apis;

        public ApiResult(Map<APIName, ApiIndex> apis,
                Iterable<? extends StaticError> errors) {
            super(errors);
            _apis = apis;
        }

        public Map<APIName, ApiIndex> apis() { return _apis; }
    }

    /** Convert the given ASTs to ApiIndices. */
    public static ApiResult buildApis(Iterable<Api> asts, long modifiedDate) {
        IndexBuilder builder = new IndexBuilder();
        Map<APIName, ApiIndex> apis = new HashMap<APIName, ApiIndex>();
        for (Api ast : asts) { builder.buildApi(ast, apis, modifiedDate); }
        return new ApiResult(apis, builder.errors());
    }

    /** Convenience function that takes apis as varargs and builds an ApiResult. */
    public static ApiResult buildApis(long modifiedDate, Api... asts) {
        ArrayList<Api> apiList = new ArrayList<Api>();
        for (Api ast: asts) { apiList.add(ast); }
        return buildApis(apiList, modifiedDate);
    }


    /** Result of {@link #buildComponents}. */
    public static class ComponentResult extends StaticPhaseResult {
        private final Map<APIName, ComponentIndex> _components;

        public ComponentResult(Map<APIName, ComponentIndex> components,
                Iterable<? extends StaticError> errors) {
            super(errors);
            _components = components;
        }

        public Map<APIName, ComponentIndex> components() { return _components; }
    }

    /** Convert the given ASTs to ComponentIndices. */
    public static ComponentResult buildComponents(Iterable<Component> asts, long modifiedDate) {
        IndexBuilder builder = new IndexBuilder();
        Map<APIName, ComponentIndex> components =
            new HashMap<APIName, ComponentIndex>();
        for (Component ast : asts) { builder.buildComponent(ast, components, modifiedDate); }
        return new ComponentResult(components, builder.errors());
    }


    private List<StaticError> _errors;

    public IndexBuilder() { _errors = new LinkedList<StaticError>(); }

    private List<StaticError> errors() { return _errors; }

    private void error(String message, HasAt loc) {
        _errors.add(StaticError.make(message, loc));
    }

    /** Create an ApiIndex and add it to the given map. */
    private void buildApi(Api ast, Map<APIName, ApiIndex> apis, long modifiedDate) {
        ApiIndex api = buildApiIndex(ast, modifiedDate);
        apis.put(ast.getName(), api);
    }

    public ApiIndex buildApiIndex(Api ast, long modifiedDate) {
        final Map<Id, Variable> variables = new HashMap<Id, Variable>();
        final Relation<IdOrOpOrAnonymousName, Function> functions =
            new IndexedRelation<IdOrOpOrAnonymousName, Function>(false);
        final Map<Id, TypeConsIndex> typeConses =
            new HashMap<Id, TypeConsIndex>();
        final Map<Id, Dimension> dimensions =
            new HashMap<Id, Dimension>();
        final Map<Id, Unit> units = new HashMap<Id, Unit>();
        final Map<String, GrammarIndex> grammars =
            new HashMap<String, GrammarIndex>();
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
            @Override public void forDimDecl(DimDecl d) {
                buildDimension(d, dimensions);
            }
            @Override public void forUnitDecl(UnitDecl d) {
                buildUnit(d, units);
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
            @Override public void forGrammarDef(GrammarDef d) {
                buildGrammar(d, grammars);
            }
        };
        for (Decl decl : ast.getDecls()) {
            decl.accept(handleDecl);
        }
        ApiIndex api = new ApiIndex(ast, variables, functions, typeConses, dimensions, units, grammars, modifiedDate);
        return api;
    }

    /**
     * One doesn't generally store ObjectIndices for Object expressions because they cannot
     * really be referred to on their own as types. However, there are some circumstances where
     * having an index for one can be helpful.
     */
    public static ObjectTraitIndex buildObjectExprIndex(ObjectExpr obj) {
        Span fake_span = NodeFactory.makeSpan("FAKE_SPAN");
    	Id fake_object_name = new Id(fake_span, "FAKE_NAME");
    	IndexBuilder builder = new IndexBuilder();

    	// Make fake object
    	ObjectDecl decl = new ObjectDecl(fake_span, fake_object_name,
    			                         Collections.<StaticParam>emptyList(),
    			                         obj.getExtendsClause(),
    			                         obj.getDecls());

    	Map<Id,TypeConsIndex> index_holder = new HashMap<Id,TypeConsIndex>();
    	builder.buildObject(decl, index_holder, new IndexedRelation<IdOrOpOrAnonymousName,Function>(), new HashMap<Id,Variable>());
    	return (ObjectTraitIndex)index_holder.get(fake_object_name);
    }

    /** Create a ComponentIndex and add it to the given map. */
    private void buildComponent(Component ast,
            Map<APIName, ComponentIndex> components,
            long modifiedDate) {
        ComponentIndex comp = buildComponentIndex(ast, modifiedDate);
        components.put(ast.getName(), comp);
    }

    public ComponentIndex buildComponentIndex(Component ast, long modifiedDate) {
        final Map<Id, Variable> variables = new HashMap<Id, Variable>();
        final Set<VarDecl> initializers = new HashSet<VarDecl>();
        final Relation<IdOrOpOrAnonymousName, Function> functions =
            new IndexedRelation<IdOrOpOrAnonymousName, Function>(false);
        final Map<Id, TypeConsIndex> typeConses =
            new HashMap<Id, TypeConsIndex>();
        final Map<Id, Dimension> dimensions =
            new HashMap<Id, Dimension>();
        final Map<Id, Unit> units =
            new HashMap<Id, Unit>();
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
            @Override public void forDimDecl(DimDecl d) {
                buildDimension(d, dimensions);
            }
            @Override public void forUnitDecl(UnitDecl d) {
                buildUnit(d, units);
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
        };
        for (Decl decl : ast.getDecls()) {
            decl.accept(handleDecl);
        }
        ComponentIndex comp = new ComponentIndex(ast, variables, initializers,
                functions, typeConses, dimensions, units, modifiedDate);
        return comp;
    }


    /**
     * Create a ProperTraitIndex and put it in the given map; add functional methods
     * to the given relation.
     */
    private void buildTrait(TraitAbsDeclOrDecl ast,
            Map<Id, TypeConsIndex> typeConses,
            final Relation<IdOrOpOrAnonymousName, Function> functions) {
        final Id name = ast.getName();
        final Map<Id, Method> getters = new HashMap<Id, Method>();
        final Map<Id, Method> setters = new HashMap<Id, Method>();
        final Set<Function> coercions = new HashSet<Function>();
        final Relation<IdOrOpOrAnonymousName, Method> dottedMethods =
            new IndexedRelation<IdOrOpOrAnonymousName, Method>(false);
        final Relation<IdOrOpOrAnonymousName, FunctionalMethod> functionalMethods =
            new IndexedRelation<IdOrOpOrAnonymousName, FunctionalMethod>(false);
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
        for (Decl decl : ast.getDecls()) {
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
            Map<Id, TypeConsIndex> typeConses,
            final Relation<IdOrOpOrAnonymousName, Function> functions,
            Map<Id, Variable> variables) {
        final Id name = ast.getName();
        final Map<Id, Variable> fields = new HashMap<Id, Variable>();
        final Set<VarDecl> initializers = new HashSet<VarDecl>();
        final Map<Id, Method> getters = new HashMap<Id, Method>();
        final Map<Id, Method> setters = new HashMap<Id, Method>();
        final Set<Function> coercions = new HashSet<Function>();
        final Relation<IdOrOpOrAnonymousName, Method> dottedMethods =
            new IndexedRelation<IdOrOpOrAnonymousName, Method>(false);
        final Relation<IdOrOpOrAnonymousName, FunctionalMethod> functionalMethods =
            new IndexedRelation<IdOrOpOrAnonymousName, FunctionalMethod>(false);

        Option<Constructor> constructor;
        if (ast.getParams().isSome()) {
            for (Param p : ast.getParams().unwrap()) {
                ModifierSet mods = extractModifiers(p.getMods());
                Id paramName = p.getName();
                fields.put(paramName, new ParamVariable(p));
                if (!mods.isHidden) {
                    if ( p instanceof NormalParam)
                        getters.put(paramName, new FieldGetterMethod((NormalParam)p, name));
                    else
                        bug(p, "Varargs object parameters should not define getters.");
                }
                if (mods.isSettable || mods.isVar) {
                    if ( p instanceof NormalParam)
                        setters.put(paramName, new FieldSetterMethod((NormalParam)p, name));
                    else
                        bug(p, "Varargs object parameters should not define setters.");
                }
            }
            Constructor c = new Constructor(name,
                    ast.getStaticParams(),
                    ast.getParams(),
                    ast.getThrowsClause(),
                    ast.getWhere());
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
        for (Decl decl : ast.getDecls()) {
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
            Map<Id, Variable> variables) {
        for (LValue b : ast.getLhs()) {
            variables.put(b.getName(), new DeclaredVariable(b));
        }
    }

    /**
     * Create and add to the given maps implicit getters and setters for a trait's
     * abstract fields.
     */
    private void buildTraitFields(AbsVarDecl ast,
            Id declaringTrait,
            Map<Id, Method> getters,
            Map<Id, Method> setters) {
        for (LValue b : ast.getLhs()) {
            ModifierSet mods = extractModifiers(b.getMods());
            // TODO: check for correct modifiers?
            Id name = b.getName();
            if (!mods.isHidden) {
                getters.put(name, new FieldGetterMethod(b, declaringTrait));
            }
            if (mods.isSettable || mods.isVar) {
                setters.put(name, new FieldSetterMethod(b, declaringTrait));
            }
        }
    }

    /**
     * Create field variables and add them to the given map; also create implicit
     * getters and setters.
     */
    private void buildFields(VarAbsDeclOrDecl ast,
            Id declaringTrait,
            Map<Id, Variable> fields,
            Map<Id, Method> getters,
            Map<Id, Method> setters) {
        for (LValue b : ast.getLhs()) {
            ModifierSet mods = extractModifiers(b.getMods());
            // TODO: check for correct modifiers?
            Id name = b.getName();
            fields.put(name, new DeclaredVariable(b));
            if (!mods.isHidden) {
                getters.put(name, new FieldGetterMethod(b, declaringTrait));
            }
            if (mods.isSettable || mods.isVar) {
                setters.put(name, new FieldSetterMethod(b, declaringTrait));
            }
        }
    }

    /**
     * Create a dimension wrapper for the declaration and put it in the given map.
     */
    private void buildDimension(DimDecl ast,
            Map<Id, Dimension> dimensions) {
        dimensions.put(ast.getDim(), new Dimension(ast));
    }

    /**
     * Create a unit wrapper for the declaration and put it in the given map.
     */
    private void buildUnit(UnitDecl ast,
            Map<Id, Unit> units) {
        for (Id unit: ast.getUnits()) {
            units.put(unit, new Unit(ast));
        }
    }

    /**
     * Create a function wrapper for the declaration and put it in the given
     * relation.
     */
    private void buildFunction(FnAbsDeclOrDecl ast,
            Relation<IdOrOpOrAnonymousName, Function> functions) {
        DeclaredFunction df = new DeclaredFunction(ast);
        functions.add(ast.getName(), df);
        functions.add(ast.getUnambiguousName(), df);
    }

    /**
     * Determine whether the given declaration is a getter, setter, coercion, dotted
     * method, or functional method, and add it to the appropriate map; also store
     * functional methods with top-level functions.
     */
    private void buildMethod(FnAbsDeclOrDecl ast,
            Id declaringTrait,
            Map<Id, Method> getters,
            Map<Id, Method> setters,
            Set<Function> coercions,
            Relation<IdOrOpOrAnonymousName, Method> dottedMethods,
            Relation<IdOrOpOrAnonymousName, FunctionalMethod> functionalMethods,
            Relation<IdOrOpOrAnonymousName, Function> topLevelFunctions) {
        ModifierSet mods = extractModifiers(ast.getMods());
        // TODO: check for correct modifiers?
        IdOrOpOrAnonymousName name = ast.getName();
        if (mods.isGetter) {
            if (name instanceof Id) {
                getters.put((Id) name, new DeclaredMethod(ast, declaringTrait));
            }
            else {
                String s = NodeUtil.nameString(name);
                error("Getter declared with an operator name, '" + s + "'", ast);
            }
        }
        else if (mods.isSetter) {
            if (name instanceof Id) {
                setters.put((Id) name, new DeclaredMethod(ast, declaringTrait));
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

    /**
     * Create a Grammar and put it in the given map.
     */
    private void buildGrammar(GrammarDef ast, Map<String, GrammarIndex> grammars) {
        String name = ast.getName().getText();
        GrammarIndex grammar = new GrammarIndex(ast, buildMembers(ast.getMembers()));
        if (grammars.containsKey(name)) {
            error("Multiple grammars declared with the same name: "+name, ast);
        }
        grammars.put(name, grammar);
    }


    private List<NonterminalIndex> buildMembers(List<GrammarMemberDecl> members) {
        List<NonterminalIndex> result = new ArrayList<NonterminalIndex>();
        Set<Id> names = new HashSet<Id>();
        for (GrammarMemberDecl m: members) {
            if (names.contains(m.getName())) {
                error("Nonterminal declared twice: "+m.getName(), m);
            }
            names.add(m.getName());
            result.add(m.accept(new NodeDepthFirstVisitor<NonterminalIndex>(){

                @Override
                public NonterminalIndex forNonterminalDef(NonterminalDef that) {
                    return new NonterminalDefIndex(that);
                }

                @Override
                public NonterminalIndex forNonterminalExtensionDef(NonterminalExtensionDef that) {
                    return new NonterminalExtendIndex(that);
                }
            }));
        }
        return result;
    }


    public static final Id COERCION_NAME = NodeFactory.makeId("coercion");
    public static final Id SELF_NAME = NodeFactory.makeId("self");

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
