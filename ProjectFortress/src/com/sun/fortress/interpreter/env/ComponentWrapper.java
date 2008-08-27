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
import com.sun.fortress.interpreter.rewrite.Desugarer;
import com.sun.fortress.interpreter.rewrite.RewriteInPresenceOfTypeInfoVisitor;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.Visitor2;

public class ComponentWrapper {
    
    private final static boolean loadCompiledEnvs =
        ProjectProperties.getBoolean("fortress.test.compiled.environments", false);
    static {
        if (loadCompiledEnvs)
            System.err.println("Loading compiled environments");
    }
    
    CompilationUnit p;

    HashMap<String, ComponentWrapper> exports = new  HashMap<String, ComponentWrapper>();

    public BuildTopLevelEnvironments be;

    /**
     * The names of libraries that are implicitly imported
     * (e.g., FortressLibrary, FortressBuiltin}
     */
    private String[]  implicitLibs;
    
    public BASet<String> ownNonFunctionNames = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
    public BASet<String> ownNames = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
    public BASet<String> ownTypeNames = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
    public BASet<String> excludedImportNames = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
    public BASet<String> importedNames = new BASet<String>(com.sun.fortress.useful.StringComparer.V);

    /**
     * If a variable/value/function name is missing when an API is initialized, 
     * store it here, in case it is found later (that is, imported into the 
     * component through some other API).
     */
    //public BASet<String> missingExportedVars = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
    /**
     * Any function that is exported, may be overloaded, and so any subsequently
     * discovered imports should also propagate.
     */
    public BASet<String> overloadableExportedFunction = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
    /**
     * If a type name is missing when an API is initialized, store it here, in
     * case it is found later  (that is, imported into the component through
     * some other API).
     */
    //public BASet<String> missingExportedTypes = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
    
    public Set<AbstractNode> unresolvedExports = new HashSet<AbstractNode>();
    
    // For debugging use/def annotation
    public BASet<String> topLevelUsesForDebugging;
    
    public Desugarer desugarer;

    int visitState;
    private final static int UNVISITED=0, IMPORTED=1, POPULATED=2, TYPED=3, FUNCTIONED=4, FINISHED=5;

    public void reset() {
        ownNonFunctionNames = null;
        ownNames = null;
        ownTypeNames = null;
        excludedImportNames = null;
        importedNames = null;
        if (exports != null)
            for (ComponentWrapper cw : exports.values()) {
                cw.reset();
            }
        this.exports = null;
        
    }
    
    Visitor2<String, Object> nameCollector = new Visitor2<String, Object>() {

        @Override
        public void visit(String t, Object u) {
            if (! overloadable(u)) {
                ownNonFunctionNames.add(t);
            } 
            if (u instanceof FType || u instanceof FTypeGeneric) {
                ownTypeNames.add(t);
            } 
            ownNames.add(t);
        }
    };


    public ComponentWrapper(CompilationUnit comp, HashMap<String, ComponentWrapper> linker, String[] implicitLibs) {
        if (comp == null)
            throw new NullPointerException("Null compilation unit not allowed");
        p = comp;
        p = (CompilationUnit) RewriteInPresenceOfTypeInfoVisitor.Only.visit(p);
        /*
        if (ProjectProperties.noStaticAnalysis)
            p = (CompilationUnit) RewriteInAbsenceOfTypeInfo.Only.visit(p);
        else
            p = (CompilationUnit) RewriteInPresenceOfTypeInfoVisitor.Only.visit(p);
            */
        
         String fortressFileName = comp.getName().getText();
        
         try {
            if (comp instanceof Component) {
                Environment e = loadCompiledEnvs ?
                    SimpleClassLoader.loadEnvironment(fortressFileName, false) :
                    new BetterEnvLevelZero(comp); // BetterEnv.empty(comp.at());
                e.setTopLevel();
                be = ((Component) comp).is_native() ?
                        new BuildNativeEnvironment(e, linker) :
                        new BuildTopLevelEnvironments(e, linker);
            } else { // comp instanceof Api
                Environment e = loadCompiledEnvs ?
                     SimpleClassLoader.loadEnvironment(fortressFileName, true) :
                         new BetterEnvLevelZero(comp); // BetterEnv.empty(comp.at());
                e.setTopLevel();
                be = new BuildApiEnvironment(e, linker);
            }
        } catch (IOException ex) {
            bug("Failed to load class (" + ex + ") for " + comp);
        } catch (InstantiationException ex) {
            bug("Failed to load class (" + ex + ") for " + comp);
        } catch (IllegalAccessException ex) {
            bug("Failed to load class (" + ex + ") for " + comp);
        }
        this.implicitLibs = implicitLibs;
    }

    /**
     * Simple/stupid wrapper constructor for the non-general 1-1 case.
     * @param comp
     * @param api
     */
    public ComponentWrapper(Component comp, ComponentWrapper api, HashMap<String, ComponentWrapper> linker, String[] implicitLibs) {
        this(comp, linker, implicitLibs);

        exports.put(NodeUtil.nameString(api.getCompilationUnit().getName()), api);
    }

    public static boolean overloadable(Object u) {
        return u instanceof Fcn || u instanceof GenericConstructor;
    }

    @Override
    public String toString() {
        return ("Wrapper for "+p.toString()+" exports: "+exports);
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

    public Collection<ComponentWrapper> getExports() {
        return exports.values();
    }
    
    public void getExports(boolean isLibrary) {
        if (visitState != UNVISITED)
            return;

        visitState = IMPORTED;

        desugarer = new Desugarer(isLibrary);

        for (ComponentWrapper api: exports.values()) {
            api.getExports(isLibrary);
        }
    }

    public void preloadTopLevel() {

        desugarer.preloadTopLevel(p);
        /* Need to capture these names early so that rewriter
         * name injection will follow the same no-duplicates
         * rules as other name visibility.
         */
        ownNames.addAll(desugarer.getTopLevelRewriteNames());
        for (ComponentWrapper api: exports.values()) {
            api.preloadTopLevel();
        }

    }

     public CompilationUnit populateOne() {
        if (visitState != IMPORTED)
            return bug("Component wrapper in wrong visit state: " + visitState);

        visitState = POPULATED;

        CompilationUnit cu = p;

        cu = (CompilationUnit) desugarer.visit(cu); // Rewrites cu!
                                      // Caches information in dis!
        be.visit(cu);
        // Reset the non-function names from the disambiguator.
        ownNonFunctionNames = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
        ownTypeNames = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
        excludedImportNames = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
        be.getEnvironment().visit(nameCollector);
        p = cu;
        topLevelUsesForDebugging = desugarer.topLevelUses.copy();
        topLevelUsesForDebugging.removeAll(ownNames);
        
        for (String implicitLibraryName : implicitLibs) {
            be.importAPIName(implicitLibraryName);
        }
        
        for (ComponentWrapper api: exports.values()) {
            be.importAPIName(api.name());
        }
        
        for (ComponentWrapper api: exports.values()) {
            api.populateOne(this);
        }

        return cu;
    }

     public CompilationUnit populateOne(ComponentWrapper exporter) {
         if (visitState != IMPORTED)
             return bug("Component wrapper in wrong visit state: " + visitState);

         visitState = POPULATED;

         be.setExporterAndApi(exporter, this);
         
         CompilationUnit cu = p;

         cu = (CompilationUnit) desugarer.visit(cu); // Rewrites cu!
                                       // Caches information in dis!
         be.visit(cu);
         // Reset the non-function names from the disambiguator.
         ownNonFunctionNames = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
         ownTypeNames = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
         excludedImportNames = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
         be.getEnvironment().visit(nameCollector);
         p = cu;
         topLevelUsesForDebugging = desugarer.topLevelUses.copy();
         topLevelUsesForDebugging.removeAll(ownNames);

         return cu;
     }

     
    public void initTypes() {
        if (visitState == POPULATED) {
            visitState = TYPED;

            be.secondPass();

            be.visit(p);

        } else
            throw new IllegalStateException("Must be populated before init types");
    }

    public void initFuncs() {
        if (visitState == TYPED) {
            visitState = FUNCTIONED;

            be.thirdPass();

            be.visit(p);

        } else
            throw new IllegalStateException("Must be typed before init funcs");
    }

    public void initVars() {
        if (visitState == FUNCTIONED) {
            visitState = FINISHED;

            be.fourthPass();
            be.visit(p);

                /*
                 * TODO Need to figure out why this works (or seems to).
                 */
            desugarer.registerObjectExprs(be.getEnvironment());

        } else if (visitState == UNVISITED)
            throw new IllegalStateException("Must be populated, typed, and functioned before init vars");
    }

    public Environment getEnvironment() {
        return be.getEnvironment();
    }

    public CompilationUnit getCompilationUnit() {
       return p;
    }

    public String name() {
        return NodeUtil.nameString(p.getName());
    }

    /**
     * Returns the component wrapper for the API apiname that this component
     * exports.
     */
    public ComponentWrapper getExportedCW(String apiname) {
        return exports.get(apiname);
    }

    public boolean isOwnName(String s) {
        return ownNames.contains(s);
    }


}
