/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.env;

import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.compiler.index.ComponentIndex;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.interpreter.rewrite.RewriteInPresenceOfTypeInfoVisitor;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.repository.DerivedFiles;
import com.sun.fortress.repository.GraphRepository;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.Useful;
import edu.rice.cs.plt.tuple.Option;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ComponentWrapper extends NonApiWrapper {

    /*
     * Next three lines are for the "cache" of rewritten ASTs
     */

    private final DerivedFiles<CompilationUnit> componentCache;

    public static boolean noCache;
    private final GraphRepository graphRepository;
    Component transformed;
    boolean cacheDisabled;


    private Component getCached(ComponentIndex comp) {
        if (cacheDisabled) return null;
        else return (Component) componentCache.get(graphRepository.pathTaggedComponent(comp.ast().getName()),
                                                   comp.modifiedDate());
    }

    // called from evalComponent
    public ComponentWrapper(ComponentIndex comp,
                            HashMap<String, NonApiWrapper> linker,
                            String[] implicitLibs,
                            GraphRepository gr) {
        super((Component) comp.ast(), linker, implicitLibs);
        cacheDisabled = noCache;
        graphRepository = gr;
        componentCache = gr.getDerivedComponentCache(ProjectProperties.INTERPRETER_CACHE_DIR);
        transformed = getCached(comp);
    }

    // called from ensureApiImplemented
    public ComponentWrapper(ComponentIndex comp,
                            APIWrapper api,
                            HashMap<String, NonApiWrapper> linker,
                            String[] implicitLibs,
                            GraphRepository gr) {
        this(comp, Useful.list(api), linker, implicitLibs, gr);
    }

    /**
     * Reads a "command line" component; do not leave in the cache.
     *
     * @param comp
     * @param api_list
     * @param linker
     * @param implicitLibs
     */
    // Called from CommandLineComponent and ComponentWrapper
    public ComponentWrapper(ComponentIndex comp,
                            List<APIWrapper> api_list,
                            HashMap<String, NonApiWrapper> linker,
                            String[] implicitLibs,
                            GraphRepository gr) {
        super((Component) comp.ast(), api_list, linker, implicitLibs);
        cacheDisabled = noCache;
        componentCache = gr.getDerivedComponentCache(ProjectProperties.INTERPRETER_CACHE_DIR);
        graphRepository = gr;
        transformed = getCached(comp);
    }

    public CompilationUnit populateOne() {
        if (visitState != IMPORTED) return bug("Component wrapper " + name() + " in wrong visit state: " + visitState);

        visitState = POPULATED;

        CompilationUnit cu = comp_unit;

        if (transformed == null) {
            cu = (Component) RewriteInPresenceOfTypeInfoVisitor.Only.visit(comp_unit);
            transformed = (Component) desugarer.visit(cu); // Rewrites cu!
            if (!cacheDisabled) {
                componentCache.put(graphRepository.pathTaggedComponent(transformed.getName()), transformed);
            }
        }

        if (!cacheDisabled && exportsMain(transformed)) {
            // It's not a library, no point keeping this copy in memory.
            componentCache.forget(graphRepository.pathTaggedComponent(transformed.getName()));
        }
        cu = transformed;
        getEnvBuilder().visit(cu);
        // Reset the non-function names from the disambiguator.
        excludedImportNames = new BASet<String>(com.sun.fortress.useful.StringHashComparer.V);
        getEnvBuilder().getEnvironment().visit(nameCollector);
        comp_unit = cu;

        for (String implicitLibraryName : implicitLibs) {
            getEnvBuilder().importAPIName(implicitLibraryName);
        }

        for (CUWrapper api : exports.values()) {
            getEnvBuilder().importAPIName(api.name());
        }

        for (APIWrapper api : exports.values()) {
            api.populateOne(this);
        }

        return cu;
    }

    private boolean exportsMain(Component transformed2) {
        List<APIName> _exports = transformed2.getExports();
        for (APIName a : _exports) {
            if (WellKnownNames.exportsMain(a.getText())) return true;
        }
        return false;
    }

    /**
     * Adds, to the supplied environment, constructors for any object
     * expressions encountered in the tree(s) processed by this Disambiguator.
     *
     * @param env
     */
    protected void registerObjectExprs(Environment env) {
        Component comp = (Component) comp_unit;

        for (_RewriteObjectExpr oe : NodeUtil.getObjectExprs(comp)) {
            String name = oe.getGenSymName();
            List<StaticParam> params = NodeUtil.getStaticParams(oe);
            Span span = NodeUtil.getSpan(oe);
            if (params.isEmpty()) {
                // Regular constructor
                FTypeObject fto = new FTypeObject(name, env, oe, NodeUtil.getParams(oe), NodeUtil.getDecls(oe), oe);
                env.putType(name, fto);
                BuildEnvironments.finishObjectTrait(NodeUtil.getTypes(NodeUtil.getExtendsClause(oe)),
                                                    null,
                                                    null,
                                                    fto,
                                                    env,
                                                    oe);
                Constructor con = new Constructor(env,
                                                  fto,
                                                  oe,
                                                  NodeFactory.makeId(span, name),
                                                  NodeUtil.getDecls(oe),
                                                  Option.<List<Param>>none());

                env.putValue(name, con);
                con.finishInitializing();
            } else {
                // Generic constructor
                FTypeGeneric fto = new FTypeGeneric(env, oe, NodeUtil.getDecls(oe), oe);
                env.putType(name, fto);
                GenericConstructor con = new GenericConstructor(env, oe, NodeFactory.makeId(span, name));
                env.putValue(name, con);
            }
        }

    }

    public Set<String> getTopLevelRewriteNames() {
        return desugarer.getTopLevelRewriteNames();
    }

    public Set<String> getFunctionals() {
        if (transformed != null) {
            return Useful.set(NodeUtil.getFunctionalMethodNames(transformed));
        }
        return desugarer.functionals;
    }
}
