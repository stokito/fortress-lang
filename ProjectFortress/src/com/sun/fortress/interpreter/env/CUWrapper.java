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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.fortress.compiler.environments.SimpleClassLoader;
import com.sun.fortress.interpreter.evaluator.BuildApiEnvironment;
import com.sun.fortress.interpreter.evaluator.BuildNativeEnvironment;
import com.sun.fortress.interpreter.evaluator.BuildTopLevelEnvironments;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.interpreter.rewrite.DesugarerVisitor;
import com.sun.fortress.interpreter.rewrite.RewriteInPresenceOfTypeInfoVisitor;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Export;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.repository.DerivedFiles;
import com.sun.fortress.repository.IOAst;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.Visitor2;

public class CUWrapper {
    
    private final static boolean loadCompiledEnvs =
        ProjectProperties.getBoolean("fortress.test.compiled.environments", false);
    
    CompilationUnit comp_unit;

    HashMap<String, APIWrapper> exports = new  HashMap<String, APIWrapper>();

    public BuildTopLevelEnvironments be;

    /**
     * The names of libraries that are implicitly imported
     * (e.g., FortressLibrary, FortressBuiltin}
     */
    protected String[]  implicitLibs;
    
    public BASet<String> ownNames = new BASet<String>(com.sun.fortress.useful.StringHashComparer.V);
    public BASet<String> excludedImportNames = new BASet<String>(com.sun.fortress.useful.StringHashComparer.V);

    /**
     * If a variable/value/function name is missing when an API is initialized, 
     * store it here, in case it is found later (that is, imported into the 
     * component through some other API).
     */
    //public BASet<String> missingExportedVars = new BASet<String>(com.sun.fortress.useful.StringHashComparer.V);
    /**
     * Any function that is exported, may be overloaded, and so any subsequently
     * discovered imports should also propagate.
     */
    public BASet<String> overloadableExportedFunction = new BASet<String>(com.sun.fortress.useful.StringHashComparer.V);
    /**
     * If a type name is missing when an API is initialized, store it here, in
     * case it is found later  (that is, imported into the component through
     * some other API).
     */
    //public BASet<String> missingExportedTypes = new BASet<String>(com.sun.fortress.useful.StringHashComparer.V);
    
    public Set<AbstractNode> unresolvedExports = new HashSet<AbstractNode>();
       
    protected DesugarerVisitor desugarer;

    int visitState;
    protected final static int UNVISITED=0, IMPORTED=1, POPULATED=2, TYPED=3, FUNCTIONED=4, FINISHED=5;

    public void reset() {
        ownNames = null;
        excludedImportNames = null;
        if (exports != null)
            for (CUWrapper cw : exports.values()) {
                cw.reset();
            }
        this.exports = null;
        
    }
    
    Visitor2<String, Object> nameCollector = new Visitor2<String, Object>() {

        @Override
        public void visit(String t, Object u) {
            
            ownNames.add(t);
        }
    };


    public CUWrapper(Component comp, HashMap<String, ComponentWrapper> linker, String[] implicitLibs) {
       this.implicitLibs = implicitLibs;
       if (comp == null)
            throw new NullPointerException("Null component (1st parameter to constructor) not allowed");
       
       comp_unit = (Component) RewriteInPresenceOfTypeInfoVisitor.Only.visit(comp);
        
         String fortressFileName = comp_unit.getName().getText();
        
         try {
            
                Environment e = loadCompiledEnvs ?
                    SimpleClassLoader.loadEnvironment(fortressFileName, false) :
                    new BetterEnvLevelZero(comp);
                e.setTopLevel();
                be = (comp.is_native() ?
                        new BuildNativeEnvironment(e, linker) :
                        new BuildTopLevelEnvironments(e, linker));
                List<Export> component_exports = comp.getExports();
                for (Export exp : component_exports) {
                    // TODO work in progress, this might not be the best place
                }
            
        } catch (IOException ex) {
            bug("Failed to load class (" + ex + ") for " + comp);
        } catch (InstantiationException ex) {
            bug("Failed to load class (" + ex + ") for " + comp);
        } catch (IllegalAccessException ex) {
            bug("Failed to load class (" + ex + ") for " + comp);
        }
    }

    public CUWrapper(Api api, HashMap<String, ComponentWrapper> linker, String[] implicitLibs) {
        this.implicitLibs = implicitLibs;
        if (api == null)
            throw new NullPointerException("Null api (1st parameter to constructor) not allowed");
        
         //comp_unit = (Api) RewriteInPresenceOfTypeInfoVisitor.Only.visit(api);
        comp_unit = api; // do nothing?
        
         String fortressFileName = comp_unit.getName().getText();
        
         try {
                Environment e = loadCompiledEnvs ?
                     SimpleClassLoader.loadEnvironment(fortressFileName, true) :
                         new BetterEnvLevelZero(api);
                e.setTopLevel();
                be = new BuildApiEnvironment(e, linker);
            
        } catch (IOException ex) {
            bug("Failed to load class (" + ex + ") for " + api);
        } catch (InstantiationException ex) {
            bug("Failed to load class (" + ex + ") for " + api);
        } catch (IllegalAccessException ex) {
            bug("Failed to load class (" + ex + ") for " + api);
        }
    }

    /**
     * Simple/stupid wrapper constructor for the non-general 1-1 case.
     * @param comp
     * @param api
     */
    public CUWrapper(Component comp, APIWrapper api, HashMap<String, ComponentWrapper> linker, String[] implicitLibs) {
        this(comp, linker, implicitLibs);

        exports.put(NodeUtil.nameString(api.getCompilationUnit().getName()), api);
    }

    public CUWrapper(Component comp, List<APIWrapper> api_list, HashMap<String, ComponentWrapper> linker, String[] implicitLibs) {
        this(comp, linker, implicitLibs);
        for (APIWrapper api : api_list) 
            exports.put(NodeUtil.nameString(api.getCompilationUnit().getName()), api);
    }
    


    public static boolean overloadable(Object u) {
        return u instanceof Fcn || u instanceof GenericConstructor;
    }

    @Override
    public String toString() {
        return ("Wrapper for "+comp_unit.toString()+" exports: "+exports);
    }

    public boolean populated() {
        return visitState > UNVISITED;
    }

    public boolean finished() {
        return visitState == FINISHED;
    }

    public boolean populatedNotFinished() {
        return visitState == POPULATED;
    }

    public Collection<APIWrapper> getExports() {
        return exports.values();
    }
    
    public void touchExports(boolean suppressDebugDump) {
        if (visitState != UNVISITED)
            return;

        visitState = IMPORTED;

        if (comp_unit instanceof Component) {
            desugarer = new DesugarerVisitor(suppressDebugDump);
        } else {
            desugarer = new DesugarerVisitor(suppressDebugDump);
        }

        for (CUWrapper api: exports.values()) {
            api.touchExports(suppressDebugDump);
        }
    }

    public void preloadTopLevel() {

        desugarer.preloadTopLevel(comp_unit);
        /* Need to capture these names early so that rewriter
         * name injection will follow the same no-duplicates
         * rules as other name visibility.
         */
        ownNames.addAll(desugarer.getTopLevelRewriteNames());
        for (CUWrapper api: exports.values()) {
            api.preloadTopLevel();
        }

    }

    public Set<String> getFunctionals() {
        return desugarer.functionals;
    }

    public boolean injectAtTopLevel(String putName, String getName, CUWrapper getFrom, Set<String>excluded) {
        return desugarer.injectAtTopLevel(putName, getName, getFrom.desugarer, excluded);
    }
    
 

 
    public void initTypes() {
        if (visitState == POPULATED) {
            visitState = TYPED;

            be.secondPass();

            be.visit(comp_unit);

        } else
            throw new IllegalStateException("Must be populated before init types");
    }

    public void initFuncs() {
        if (visitState == TYPED) {
            visitState = FUNCTIONED;

            be.thirdPass();

            be.visit(comp_unit);

        } else
            throw new IllegalStateException("Must be typed before init funcs");
    }

    public void initVars() {
        if (visitState == FUNCTIONED) {
            visitState = FINISHED;

            be.fourthPass();
            be.visit(comp_unit);

                /*
                 * TODO Need to figure out why this works (or seems to).
                 */
            registerObjectExprs(be.getEnvironment());

        } else if (visitState == UNVISITED)
            throw new IllegalStateException("Must be populated, typed, and functioned before init vars");
    }

    protected void registerObjectExprs(Environment environment) {
        
    }

    public Environment getEnvironment() {
        return be.getEnvironment();
    }

    public CompilationUnit getCompilationUnit() {
       return comp_unit;
    }

    public String name() {
        return NodeUtil.nameString(comp_unit.getName());
    }

    /**
     * Returns the component wrapper for the API apiname that this component
     * exports.
     */
    public APIWrapper getExportedCW(String apiname) {
        return exports.get(apiname);
    }

    public boolean isOwnName(String s) {
        return ownNames.contains(s);
    }

    public List<Import> getImports() {
        return comp_unit.getImports();
    }



}
