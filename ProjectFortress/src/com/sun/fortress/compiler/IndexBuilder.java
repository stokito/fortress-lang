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

package com.sun.fortress.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.index.*;
import com.sun.fortress.compiler.typechecker.TypeEnv;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.scala_src.linker.ApiLinker;
import com.sun.fortress.scala_src.linker.CompoundApiChecker;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.NI;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

import edu.rice.cs.plt.collect.IndexedRelation;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Option;

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
        return builder.buildApis(asts, apis, modifiedDate);
    }

    private ApiResult buildApis(Iterable<Api> asts, Map<APIName, ApiIndex> apis, 
                                long modifiedDate) 
    { 
        boolean apisAdded = false;
        for (Api ast : asts) { apisAdded = apisAdded || this.buildApi(ast, apis, modifiedDate); }
        if (apisAdded) { return  new IndexBuilder().buildApis(asts, apis, modifiedDate); }
        else { return new ApiResult(apis, this.errors()); }
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

    private void addErrors(List<StaticError> errors) { 
        _errors.addAll(errors);
    }

    /** Create an ApiIndex and add it to the given map. */
    private boolean buildApi(Api ast, Map<APIName, ApiIndex> apis, long modifiedDate) {
        if (apis.containsKey(ast.getName())) { 
            return false; 
        }
        else { 
            List<StaticError> errors = new CompoundApiChecker(apis).check(ast);
            if (! errors.isEmpty()) { 
                addErrors(errors); 
                return false;
            }
            else { 
                // If <code>ast</code> is a compound API, link it into a single API.
                ast = new ApiLinker(apis).link(ast); 
                ApiIndex api = buildApiIndex(ast, modifiedDate);
                apis.put(ast.getName(), api);
                return true;
            }
        }
    }

    public ApiIndex buildApiIndex(Api ast, long modifiedDate) {
        final Map<Id, Variable> variables = new HashMap<Id, Variable>();
        final Relation<IdOrOpOrAnonymousName, Function> functions =
            new IndexedRelation<IdOrOpOrAnonymousName, Function>(false);
        final Set<ParametricOperator> parametricOperators =
            new HashSet<ParametricOperator>();
        final Map<Id, TypeConsIndex> typeConses =
            new HashMap<Id, TypeConsIndex>();
        final Map<Id, Dimension> dimensions =
            new HashMap<Id, Dimension>();
        final Map<Id, Unit> units = new HashMap<Id, Unit>();
        final Map<String, GrammarIndex> grammars =
            new HashMap<String, GrammarIndex>();
        NodeAbstractVisitor_void handleDecl = new NodeAbstractVisitor_void() {
            @Override public void forTraitDecl(TraitDecl d) {
                buildTrait(d, typeConses, functions, parametricOperators);
            }
            @Override public void forObjectDecl(ObjectDecl d) {
                buildObject(d, typeConses, functions, parametricOperators, variables);
            }
            @Override public void forVarDecl(VarDecl d) {
                buildVariables(d, variables);
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
            @Override public void forGrammarDecl(GrammarDecl d) {
                buildGrammar(d, grammars);
            }
        };
        for (Decl decl : ast.getDecls()) {
            decl.accept(handleDecl);
        }

        NodeAbstractVisitor_void handleComprisesExcludes = new NodeAbstractVisitor_void() {
            @Override public void forTraitDecl(TraitDecl d) {
                Id dName = NodeUtil.getName(d);
                ProperTraitIndex dIndex = (ProperTraitIndex)typeConses.get(dName);
                if ( d.getComprisesClause().isSome() ) {
                    for ( BaseType t : d.getComprisesClause().unwrap() )
                        if ( t instanceof NamedType ) {
                            TraitType typ;
                            if ( t instanceof TraitType ) typ = (TraitType)t;
                            else typ = NodeFactory.makeTraitType(((NamedType)t).getName());
                            dIndex.addComprisesType(typ);
                        } else bug("TraitType is expected in the comprises clause of " + d +
                                   " but found " + t + " " + t.getClass() + ".");
                }
                for ( BaseType t : d.getExcludesClause() ) {
                    if ( t instanceof NamedType ) {
                        // add t to d's excludes clause
                        TraitType typ;
                        Id tName = ((NamedType)t).getName();
                        if ( t instanceof TraitType ) typ = (TraitType)t;
                        else typ = NodeFactory.makeTraitType(tName);
                        dIndex.addExcludesType(typ);
                        // add d to t's excludes clause
                        Option<Type> ty = d.getSelfType();
                        if ( ty.isSome() )
                            typ = (TraitType)ty.unwrap();
                        else
                            typ = NodeFactory.makeTraitType(dName, TypeEnv.staticParamsToArgs(NodeUtil.getStaticParams(d)));
                        // If t is a parameterized type instantiated with ground types,
                        // then do not add d to t's excludes clause.
                        if ( ( t instanceof VarType ||
                               ((TraitType)t).getArgs().isEmpty() ) &&
                             typeConses.get(tName) instanceof ProperTraitIndex &&
                             (ProperTraitIndex)typeConses.get(tName) != null ) {
                            ((ProperTraitIndex)typeConses.get(tName)).addExcludesType(typ);
                        }
                    } else bug("TraitType is expected in the excludes clause of " + d +
                               " but found " + t + ".");
                }
            }
        };
        for (Decl decl : ast.getDecls()) {
            decl.accept(handleComprisesExcludes);
        }

        ApiIndex api = new ApiIndex(ast, variables, functions, parametricOperators,
                                    typeConses, dimensions, units, grammars, modifiedDate);
        return api;
    }

    /**
     * One doesn't generally store ObjectIndices for Object expressions because they cannot
     * really be referred to on their own as types. However, there are some circumstances where
     * having an index for one can be helpful.
     */
    public static ObjectTraitIndex buildObjectExprIndex(ObjectExpr obj) {
        Span fake_span = NodeFactory.makeSpan("FAKE_SPAN");

        Id fake_object_name = NodeFactory.makeId(fake_span, "FAKE_NAME");
    	IndexBuilder builder = new IndexBuilder();

    	// Make fake object
    	ObjectDecl decl = NodeFactory.makeObjectDecl(fake_span, fake_object_name,
                                                     NodeUtil.getExtendsClause(obj),
                                                     NodeUtil.getDecls(obj),
                                                     Option.<Type>none());

    	Map<Id,TypeConsIndex> index_holder = new HashMap<Id,TypeConsIndex>();

        // TODO: Fix this so that the global function map and parametricOperator set are
        // threaded through to here.
    	builder.buildObject(decl, index_holder, new IndexedRelation<IdOrOpOrAnonymousName,Function>(),
                            new HashSet<ParametricOperator>(),
                            new HashMap<Id,Variable>());
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
        final Set<ParametricOperator> parametricOperators =
            new HashSet<ParametricOperator>();
        final Map<Id, TypeConsIndex> typeConses =
            new HashMap<Id, TypeConsIndex>();
        final Map<Id, Dimension> dimensions =
            new HashMap<Id, Dimension>();
        final Map<Id, Unit> units =
            new HashMap<Id, Unit>();
        NodeAbstractVisitor_void handleDecl = new NodeAbstractVisitor_void() {
            @Override public void forTraitDecl(TraitDecl d) {
                buildTrait(d, typeConses, functions, parametricOperators);
            }
            @Override public void forObjectDecl(ObjectDecl d) {
                buildObject(d, typeConses, functions, parametricOperators, variables);
            }
            @Override public void forVarDecl(VarDecl d) {
                buildVariables(d, variables); initializers.add(d);
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

        NodeAbstractVisitor_void handleComprisesExcludes = new NodeAbstractVisitor_void() {
            @Override public void forTraitDecl(TraitDecl d) {
                Id dName = NodeUtil.getName(d);
                ProperTraitIndex dIndex = (ProperTraitIndex)typeConses.get(dName);
                if ( d.getComprisesClause().isSome() ) {
                    for ( BaseType t : d.getComprisesClause().unwrap() )
                        if ( t instanceof NamedType ) {
                            TraitType typ;
                            if ( t instanceof TraitType ) typ = (TraitType)t;
                            else typ = NodeFactory.makeTraitType(((NamedType)t).getName());
                            dIndex.addComprisesType(typ);
                        } else bug("TraitType is expected in the comprises clause of " + d +
                                   " but found " + t + " " + t.getClass() + ".");
                }
                for ( BaseType t : d.getExcludesClause() ) {
                    if ( t instanceof NamedType ) {
                        // add t to d's excludes clause
                        TraitType typ;
                        Id tName = ((NamedType)t).getName();
                        if ( t instanceof TraitType ) typ = (TraitType)t;
                        else typ = NodeFactory.makeTraitType(tName);
                        dIndex.addExcludesType(typ);
                        // add d to t's excludes clause
                        Option<Type> ty = d.getSelfType();
                        if ( ty.isSome() )
                            typ = (TraitType)ty.unwrap();
                        else
                            typ = NodeFactory.makeTraitType(dName, TypeEnv.staticParamsToArgs(NodeUtil.getStaticParams(d)));
                        if ( ( t instanceof VarType ||
                               ((TraitType)t).getArgs().isEmpty() ) &&
                             typeConses.get(tName) instanceof ProperTraitIndex &&
                             (ProperTraitIndex)typeConses.get(tName) != null ) {
                            ((ProperTraitIndex)typeConses.get(tName)).addExcludesType(typ);
                        }
                    } else bug("TraitType is expected in the excludes clause of " + d +
                               " but found " + t + ".");
                }
            }
        };
        for (Decl decl : ast.getDecls()) {
            decl.accept(handleComprisesExcludes);
        }

        ComponentIndex comp = new ComponentIndex(ast, variables, initializers, functions,
                                                 parametricOperators, typeConses, dimensions,
                                                 units, modifiedDate);
        return comp;
    }


    /**
     * Create a ProperTraitIndex and put it in the given map; add functional methods
     * to the given relation.
     */
    private void buildTrait(final TraitDecl ast,
                            final Map<Id, TypeConsIndex> typeConses,
                            final Relation<IdOrOpOrAnonymousName, Function> functions,
                            final Set<ParametricOperator> parametricOperators)
    {
        final Id name = NodeUtil.getName(ast);
        final Map<Id, Method> getters = new HashMap<Id, Method>();
        final Map<Id, Method> setters = new HashMap<Id, Method>();
        final Set<Coercion> coercions = new HashSet<Coercion>();
        final Relation<IdOrOpOrAnonymousName, DeclaredMethod> dottedMethods =
            new IndexedRelation<IdOrOpOrAnonymousName, DeclaredMethod>(false);
        final Relation<IdOrOpOrAnonymousName, FunctionalMethod> functionalMethods =
            new IndexedRelation<IdOrOpOrAnonymousName, FunctionalMethod>(false);

        NodeAbstractVisitor_void handleFnDecl = new NodeAbstractVisitor_void() {
            @Override public void forFnDecl(FnDecl d) {
                buildMethod(d, name, NodeUtil.getStaticParams(ast),
                            getters, setters, coercions, dottedMethods,
                            functionalMethods, functions, parametricOperators);
            }
        };
        for (Decl decl : NodeUtil.getDecls(ast)) {
            decl.accept(handleFnDecl);
        }
        NodeAbstractVisitor_void handleDecl = new NodeAbstractVisitor_void() {
            @Override public void forVarDecl(VarDecl d) {
                buildTraitFields(d, name, getters, setters);
            }
            @Override public void forPropertyDecl(PropertyDecl d) {
                NI.nyi();
            }
        };
        for (Decl decl : NodeUtil.getDecls(ast)) {
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
    private void buildObject(final ObjectDecl ast,
                             final Map<Id, TypeConsIndex> typeConses,
                             final Relation<IdOrOpOrAnonymousName, Function> functions,
                             final Set<ParametricOperator> parametricOperators,
                             final Map<Id, Variable> variables)
    {
        final Id name = NodeUtil.getName(ast);
        final Map<Id, Variable> fields = new HashMap<Id, Variable>();
        final Set<VarDecl> initializers = new HashSet<VarDecl>();
        final Map<Id, Method> getters = new HashMap<Id, Method>();
        final Map<Id, Method> setters = new HashMap<Id, Method>();
        final Set<Coercion> coercions = new HashSet<Coercion>();
        final Relation<IdOrOpOrAnonymousName, DeclaredMethod> dottedMethods =
            new IndexedRelation<IdOrOpOrAnonymousName, DeclaredMethod>(false);
        final Relation<IdOrOpOrAnonymousName, FunctionalMethod> functionalMethods =
            new IndexedRelation<IdOrOpOrAnonymousName, FunctionalMethod>(false);

        NodeAbstractVisitor_void handleFnDecl = new NodeAbstractVisitor_void() {
            @Override public void forFnDecl(FnDecl d) {
                buildMethod(d, name, NodeUtil.getStaticParams(ast),
                            getters, setters, coercions, dottedMethods,
                            functionalMethods, functions, parametricOperators);
            }
        };
        for (Decl decl : NodeUtil.getDecls(ast)) {
            decl.accept(handleFnDecl);
        }
        for (Id id : getters.keySet()) {
            if ( dottedMethods.firstSet().contains(id) )
                error("Getter declarations should not be overloaded with method declarations.", id);
        }
        for (Id id : setters.keySet()) {
            if ( dottedMethods.firstSet().contains(id) )
                error("Setter declarations should not be overloaded with method declarations.", id);
        }

        NodeAbstractVisitor_void handleDecl = new NodeAbstractVisitor_void() {
            @Override public void forVarDecl(VarDecl d) {
                buildFields(d, name, fields, getters, setters);
                if (d.getInit().isSome())
                    initializers.add(d);
            }
            @Override public void forPropertyDecl(PropertyDecl d) {
                NI.nyi();
            }
        };
        for (Decl decl : NodeUtil.getDecls(ast)) {
            decl.accept(handleDecl);
        }

        Option<Constructor> constructor;
        if (ast.getParams().isSome()) {
            for (Param p : ast.getParams().unwrap()) {
                Modifiers mods = p.getMods();
                Id paramName = p.getName();
                fields.put(paramName, new ParamVariable(p));
                if (!mods.isHidden()) {
                    if ( ! NodeUtil.isVarargsParam(p) )
                        getters.put(paramName, new FieldGetterMethod(p, name));
                    else
                        error("Varargs object parameters should not define getters.", p);
                }
                if (mods.isSettable() || mods.isVar()) {
                    if ( ! NodeUtil.isVarargsParam(p) )
                        setters.put(paramName, new FieldSetterMethod(p, name));
                    else
                        error("Varargs object parameters should not define setters.", p);
                }
            }
            Constructor c = new Constructor(name,
                                            NodeUtil.getStaticParams(ast),
                                            ast.getParams(),
                                            NodeUtil.getThrowsClause(ast),
                                            NodeUtil.getWhereClause(ast));
            constructor = Option.some(c);
            functions.add(name, c);
        }
        else {
            constructor = Option.none();
            variables.put(name, new SingletonVariable(name));
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
    private void buildVariables(VarDecl ast,
            Map<Id, Variable> variables) {
        for (LValue b : ast.getLhs()) {
            variables.put(b.getName(), new DeclaredVariable(b, ast));
        }
    }

    /**
     * Create and add to the given maps implicit getters and setters for a trait's
     * abstract fields.
     */
    private void buildTraitFields(VarDecl ast,
                                  Id declaringTrait,
                                  Map<Id, Method> getters,
                                  Map<Id, Method> setters) {
        for (LValue b : ast.getLhs()) {
            Modifiers mods = b.getMods();
            // TODO: check for correct modifiers?
            Id name = b.getName();
            if (!mods.isHidden()) {
                getters.put(name, new FieldGetterMethod(b, declaringTrait));
            }
            if (mods.isMutable()) {
                setters.put(name, new FieldSetterMethod(b, declaringTrait));
            }
        }
    }

    /**
     * Create field variables and add them to the given map; also create implicit
     * getters and setters.
     */
    private void buildFields(VarDecl ast,
            Id declaringTrait,
            Map<Id, Variable> fields,
            Map<Id, Method> getters,
            Map<Id, Method> setters) {
        for (LValue b : ast.getLhs()) {
            Modifiers mods = b.getMods();
            // TODO: check for correct modifiers?
            Id name = b.getName();
            fields.put(name, new DeclaredVariable(b, ast));
            if (!mods.isHidden()) {
                getters.put(name, new FieldGetterMethod(b, declaringTrait));
            }
            if (mods.isSettable() || mods.isVar()) {
                setters.put(name, new FieldSetterMethod(b, declaringTrait));
            }
        }
    }

    /**
     * Create a dimension wrapper for the declaration and put it in the given map.
     */
    private void buildDimension(DimDecl ast,
            Map<Id, Dimension> dimensions) {
        dimensions.put(ast.getDimId(), new Dimension(ast));
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
    private void buildFunction(FnDecl ast,
            Relation<IdOrOpOrAnonymousName, Function> functions) {
        DeclaredFunction df = new DeclaredFunction(ast);
        functions.add(NodeUtil.getName(ast), df);
        functions.add(ast.getUnambiguousName(), df);
    }

    /**
     * Determine whether the given declaration is a getter, setter, coercion, dotted
     * method, or functional method, and add it to the appropriate map; also store
     * functional methods with top-level functions. Note that parametric operators
     * are also propagated to top-level, with their parametric names. These names
     * must be substituted with particular instantiations during lookup.
     */
    private void buildMethod(FnDecl ast,
                             Id declaringTrait,
                             List<StaticParam> enclosingParams,
                             Map<Id, Method> getters,
                             Map<Id, Method> setters,
                             Set<Coercion> coercions,
                             Relation<IdOrOpOrAnonymousName, DeclaredMethod> dottedMethods,
                             Relation<IdOrOpOrAnonymousName, FunctionalMethod> functionalMethods,
                             Relation<IdOrOpOrAnonymousName, Function> topLevelFunctions,
                             Set<ParametricOperator> parametricOperators) {
        Modifiers mods = NodeUtil.getMods(ast);
        // TODO: check for correct modifiers?
        IdOrOpOrAnonymousName name = NodeUtil.getName(ast);
        if (mods.isGetter()) {
            if (name instanceof Id) {
                if ( getters.keySet().contains((Id)name) )
                    error("Getter declarations should not be overloaded.", ast);
                else getters.put((Id)name, new DeclaredMethod(ast, declaringTrait));
            }
            else {
                String s = NodeUtil.nameString(name);
                error("Getter declared with an operator name, '" + s + "'", ast);
            }
        }
        else if (mods.isSetter()) {
            if (name instanceof Id) {
                if ( setters.keySet().contains((Id)name) )
                    error("Setter declarations should not be overloaded.", ast);
                else setters.put((Id)name, new DeclaredMethod(ast, declaringTrait));
            }
            else {
                String s = NodeUtil.nameString(name);
                error("Getter declared with an operator name, '" + s + "'", ast);
            }
        }
        else if (name instanceof Id && ((Id)name).getText().equals(COERCION_NAME)) {
            coercions.add(new Coercion(ast, declaringTrait, enclosingParams));
        }
        else {
            boolean functional = false;
            for (Param p : NodeUtil.getParams(ast)) {
                // TODO: make sure param is valid (for ex., self doesn't have a type)
                if (p.getName().equals(SELF_NAME)) {
                    if (functional) {
                        error("Parameter 'self' appears twice in a method declaration.", ast);
                        return;
                    }
                    functional = true;
                }
            }

            boolean operator = NodeUtil.isOp(ast);

            // Determine whether:
            //   (1) this declaration has a self parameter
            //   (2) this declaration is for an operator
            // Place the declaration in the appropriate bins according to the answer.
            if (functional && ! operator) {
                FunctionalMethod m = new FunctionalMethod(ast, declaringTrait, enclosingParams);

                functionalMethods.add(name, m);
                topLevelFunctions.add(name, m);
            } else if (functional && operator) {
                boolean parametric = false;

                for (StaticParam p : enclosingParams) {
                    if (NodeUtil.getName(ast).equals(NodeUtil.getName(p))) {
                        parametric = true;
                    }
                }
                if (parametric) {
                    ParametricOperator po = new ParametricOperator(ast, declaringTrait, enclosingParams);
                    parametricOperators.add(po);
                } else {
                    FunctionalMethod m = new FunctionalMethod(ast, declaringTrait, enclosingParams);

                    functionalMethods.add(name, m);
                    topLevelFunctions.add(name, m);
                }
            } else if ((! functional) && operator) {
                // In this case, we must have a subscripting operator method declaration
                // or a subscripted assignment operator method declaration. See F 1.0 beta Section 34.
                // TODO: Check that we are handling this case correctly!
                dottedMethods.add(name, new DeclaredMethod(ast, declaringTrait));
            } else { // ! functional && ! operator
                dottedMethods.add(name, new DeclaredMethod(ast, declaringTrait));
            }
        }
    }

    /**
     * Create a Grammar and put it in the given map.
     */
    private void buildGrammar(GrammarDecl ast, Map<String, GrammarIndex> grammars) {
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


    public static final String COERCION_NAME = "coerce";
    public static final Id SELF_NAME = NodeFactory.makeId(NodeFactory.internalSpan, "self");

}
