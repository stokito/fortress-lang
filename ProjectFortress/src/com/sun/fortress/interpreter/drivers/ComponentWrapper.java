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

package com.sun.fortress.interpreter.drivers;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.BuildNativeEnvironment;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.interpreter.rewrite.Desugarer;
import com.sun.fortress.interpreter.rewrite.RewriteInAbsenceOfTypeInfo;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.Visitor2;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class ComponentWrapper {
    CompilationUnit p;

    HashMap<String, ComponentWrapper> exports = new  HashMap<String, ComponentWrapper>();

    BuildEnvironments be;
    
    BASet<String> ownNonFunctionNames = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
    
    Desugarer dis;
    boolean isNative; 

    int visitState;
    private final static int UNVISITED=0, IMPORTED=1, POPULATED=2, TYPED=3, FUNCTIONED=4, FINISHED=5;

    Visitor2<String, Object> nameCollector = new Visitor2<String, Object>() {

        @Override
        public void visit(String t, Object u) {
            if (! (u instanceof Fcn) && ! (u instanceof GenericConstructor)) {
                if (t.equals(":"))
                    System.err.println(": import of " + u);
                ownNonFunctionNames.add(t);
            }
        }
    };

    public ComponentWrapper(CompilationUnit comp, boolean is_native) {
        if (comp == null)
            throw new NullPointerException("Null compilation unit not allowed");
        p = comp;
        BetterEnv e = BetterEnv.empty();
        e.setTopLevel();
        isNative = is_native;
        be = isNative ? new BuildNativeEnvironment(e) : new BuildEnvironments(e);
    }

    /**
     * Simple/stupid wrapper constructor for the non-general 1-1 case.
     * @param comp
     * @param api
     */
    public ComponentWrapper(Component comp, ComponentWrapper api, boolean is_native) {
        this(comp, is_native);
        
        exports.put(NodeUtil.nameString(api.getComponent().getName()), api);
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

    public void getExports(boolean isLibrary) {
        if (visitState != UNVISITED)
            return;
        
        visitState = IMPORTED;

        dis = new Desugarer(isLibrary);
        
        for (ComponentWrapper api: exports.values()) {
            api.getExports(isLibrary);
        }
    }

    public void preloadTopLevel() {
        
        dis.preloadTopLevel(p);
        /* Need to capture these names early so that rewriter
         * name injection will follow the same no-duplicates
         * rules as other name visibility.
         */
        ownNonFunctionNames.addAll(dis.getTopLevelRewriteNames());
        for (ComponentWrapper api: exports.values()) {
            api.preloadTopLevel();
        }
        
    }
    
     /**
     *
     */
    public CompilationUnit populateOne() {
        if (visitState != IMPORTED)
            return bug("Component wrapper in wrong visit state: " + visitState);
        
        visitState = POPULATED;

        CompilationUnit cu = p;
        
        cu = (CompilationUnit) RewriteInAbsenceOfTypeInfo.Only.visit(cu);
        cu = (CompilationUnit) dis.visit(cu); // Rewrites p!
                                      // Caches information in dis!
        be.visit(cu);
        // Reset the non-function names from the disambiguator.
        ownNonFunctionNames = new BASet<String>(com.sun.fortress.useful.StringComparer.V);
        be.getEnvironment().visit(nameCollector);
        p = cu;
        
        for (ComponentWrapper api: exports.values()) {
            api.populateOne();
        }
        
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
            dis.registerObjectExprs(be.getEnvironment());

        } else if (visitState == UNVISITED)
            throw new IllegalStateException("Must be populated, typed, and functioned before init vars");
    }

    public BetterEnv getEnvironment() {
        return be.getEnvironment();
    }

    public CompilationUnit getComponent() {
       return p;
    }

    /**
     * Returns the component wrapper for the API apiname that this component
     * exports.
     */
    public ComponentWrapper getExportedCW(String apiname) {
        return exports.get(apiname);
    }
    
    public boolean isOwnNonFunctionName(String s) {
        return ownNonFunctionNames.contains(s);
    }


}
