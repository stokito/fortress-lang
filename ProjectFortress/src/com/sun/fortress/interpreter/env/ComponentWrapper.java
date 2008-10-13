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
package com.sun.fortress.interpreter.env;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes._RewriteObjectExpr;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.repository.CacheBasedRepository;
import com.sun.fortress.repository.DerivedFiles;
import com.sun.fortress.repository.IOAst;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;

public class ComponentWrapper extends CUWrapper {

    /* 
     * Next three lines are for the "cache" of rewritten ASTs
     */
    private static Fn<APIName, String> toCompFileName = new Fn<APIName, String>() {
        @Override
        public String apply(APIName x) {
            return ProjectProperties.compFileName(ProjectProperties.INTERPRETER_CACHE_DIR, CacheBasedRepository.deCaseName(x));
        } 
    };
    private static IOAst componentReaderWriter = new IOAst(toCompFileName);
    private static DerivedFiles<CompilationUnit> componentCache = 
        new DerivedFiles<CompilationUnit>(componentReaderWriter);
    
    public static boolean noCache;
    
    Component transformed;
    boolean cacheDisabled;
    
    
    private Component getCached(ComponentIndex comp) {
        if (cacheDisabled)
            return null;
        else
            return  (Component) componentCache.get(comp.ast().getName(), comp.modifiedDate());
    }
    
    public ComponentWrapper(ComponentIndex comp, HashMap<String, ComponentWrapper> linker,
            String[] implicitLibs) {
        super((Component) comp.ast(), linker, implicitLibs);
        cacheDisabled = noCache;
        
        transformed = getCached(comp);
        // TODO Auto-generated constructor stub
    }

    public ComponentWrapper(ComponentIndex comp, APIWrapper api,
            HashMap<String, ComponentWrapper> linker, String[] implicitLibs) {
        super((Component) comp.ast(), api, linker, implicitLibs);
        cacheDisabled = noCache;
       transformed = getCached(comp);
        // TODO Auto-generated constructor stub
    }

    public ComponentWrapper(ComponentIndex comp, List<APIWrapper> api_list,
            HashMap<String, ComponentWrapper> linker, String[] implicitLibs) {
        super((Component) comp.ast(), api_list, linker, implicitLibs);
        cacheDisabled = noCache;
        transformed = getCached(comp);
        // TODO Auto-generated constructor stub
    }

    public CompilationUnit populateOne() {
        if (visitState != IMPORTED)
            return bug("Component wrapper " + name() + " in wrong visit state: " + visitState);

        visitState = POPULATED;

        CompilationUnit cu = comp_unit;

        if (transformed == null) {
            transformed = (Component) desugarer.visit(cu); // Rewrites cu!
            if (!cacheDisabled)
                componentCache.put(transformed.getName(), transformed);
        }
        cu = transformed;
        be.visit(cu);
        // Reset the non-function names from the disambiguator.
        excludedImportNames = new BASet<String>(com.sun.fortress.useful.StringHashComparer.V);
        be.getEnvironment().visit(nameCollector);
        comp_unit = cu;
         
        for (String implicitLibraryName : implicitLibs) {
            be.importAPIName(implicitLibraryName);
        }
        
        for (CUWrapper api: exports.values()) {
            be.importAPIName(api.name());
        }
        
        for (APIWrapper api: exports.values()) {
            api.populateOne(this);
        }

        return cu;
    }
    
    /**
     * Adds, to the supplied environment, constructors for any object
     * expressions encountered in the tree(s) processed by this Disambiguator.
     * @param env
     */
    protected void registerObjectExprs(Environment env) {
        Component comp = (Component) comp_unit;
        
            for (_RewriteObjectExpr oe : comp.getObjectExprs()) {
                String name = oe.getGenSymName();
                List<StaticParam> params = oe.getStaticParams();
                if (params.isEmpty()) {
                    // Regular constructor
                    FTypeObject fto = new FTypeObject(name, env, oe, oe.getParams(),
                                                      oe.getDecls(), oe);
                    env.putType(name, fto);
                    BuildEnvironments.finishObjectTrait(NodeUtil.getTypes(oe.getExtendsClause()),
                                                        null, null, fto, env, oe);
                    Constructor con = new Constructor(env, fto, oe,
                                                      NodeFactory.makeId(name),
                                                      oe.getDecls(),
                                                      Option.<List<Param>>none());

                    env.putValue(name, con);
                    con.finishInitializing();
                } else {
                    // Generic constructor
                    FTypeGeneric fto = new FTypeGeneric(env, oe, oe.getDecls(), oe);
                    env.putType(name, fto);
                    GenericConstructor con = new GenericConstructor(env, oe, NodeFactory.makeId(name));
                    env.putValue(name, con);
                }
            }
        
    }
    public Set<String> getTopLevelRewriteNames() {
        return desugarer.getTopLevelRewriteNames();
    }

    public Set<String> getFunctionals() {
        if (transformed != null) {
            return Useful.set(transformed.getFunctionalMethodNames());
        }
        return desugarer.functionals;
    }
}
