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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AliasedAPIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.nodes.ImportedNames;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.NodeAbstractVisitor_void;
import com.sun.fortress.shell.RepositoryError;
import com.sun.fortress.useful.MinimalMap;
import com.sun.fortress.useful.MultiMap;

public class RepositoryUpdater extends NodeAbstractVisitor<Boolean> {
    /* Depending upon whether the APIs are "linked" or not, the
     * visitor may traverse components, in which case it matters
     * which ones have been visited, or are being visited.
     * 
     * Otherwise, the only component visited will be the outermost one.
     * 
     */

    final FortressRepository source;

    final FortressRepository derived;
    
    final MinimalMap<APIName, Set<APIName>> linker;

    final Set<APIName> visitedApis = new HashSet<APIName>();

    final Set<APIName> visitedComponents = new HashSet<APIName>();

    public final Set<APIName> staleApis = new HashSet<APIName>();
    
    public final Set<APIName> staleComponents = new HashSet<APIName>();

    public final Map<APIName, Throwable> componentExceptions = new HashMap<APIName, Throwable>();
    public final Map<APIName, Throwable> apiExceptions = new HashMap<APIName, Throwable>();

    final IsStaleApi isStaleApi = new IsStaleApi();

    final IsStaleComponent isStaleComponent = new IsStaleComponent();

    
    APIName nowVisiting;
   
    boolean change;

    abstract static class IsStale {
        abstract boolean isStale(APIName n);
    }

    class IsStaleApi extends IsStale {

        @Override
        boolean isStale(APIName name) {
            try {
                long s = source.getModifiedDateForApi(name);
                long c = derived.getModifiedDateForApi(name);
                return c <= s;
            } catch (FileNotFoundException ex) {
                // Missing = "older"
            }
            return true;
        }

    }

    class IsStaleComponent extends IsStale {

        @Override
        boolean isStale(APIName name) {
            try {
                long s = source.getModifiedDateForComponent(name);
                long c = derived.getModifiedDateForComponent(name);
                return c <= s;
            } catch (FileNotFoundException ex) {
                // Missing = "older"
            }
            return true;
        }

    }

    public RepositoryUpdater(FortressRepository source,
            FortressRepository derived, MinimalMap<APIName, Set<APIName>> linker) {
        this.source = source;
        this.derived = derived;
        this.linker = linker;
    }
    
    public void addComponent(APIName c)  {
         try {
            ComponentIndex ci =
                isStaleComponent.isStale(c) ? source.getComponent(c) : derived.getComponent(c);
            CompilationUnit cu = ci.ast();
            if (!c.equals(cu.getName())) {
                throw new RepositoryError("File " + c + " actually contains component " + cu.getName());
            }
            iterateStaleTillStable(cu);
        } catch (Throwable ex) {
            componentExceptions.put(c , ex);
        }
    }

     public void addApi(APIName c)  {
         try {
             ApiIndex ci = 
             isStaleApi.isStale(c) ? source.getApi(c) : derived.getApi(c);

             CompilationUnit cu = ci.ast();
             if (!c.equals(cu.getName())) {
                 throw new RepositoryError("File " + c + " actually contains api " + cu.getName());
             }
             iterateStaleTillStable(cu);
         } catch (Throwable ex) {
             apiExceptions.put(c , ex);
         }
         
    }

     private void iterateStaleTillStable(CompilationUnit cu) {
         change = false;
         cu.accept(this);
         visitedComponents.clear();
         visitedApis.clear();
         while (change) {
             change = false;
             cu.accept(this);
             visitedComponents.clear();
             visitedApis.clear();
         }
     }
     
   private Boolean visitCommon(CompilationUnit that, Set<APIName> visited,
            Set<APIName> stale, IsStale isStale) {
        APIName savedNowVisiting = nowVisiting;
        try {
            nowVisiting = that.getName();
            if (visited.contains(nowVisiting)) {
                return stale.contains(nowVisiting);
            }

            visited.add(nowVisiting);

            /* If now-visiting is not in derived, 
             * or if source's version of now-visiting is younger,
             * then add now-visiting to stale.
             */
            if (!stale.contains(nowVisiting) && isStale.isStale(nowVisiting)) {
                stale.add(nowVisiting);
                change |= true;
            }

            List<Import> imports = that.getImports();
            boolean importsStale = false;
            for (Import i : imports) {
                importsStale |= i.accept(this);
            }

            if (importsStale && !stale.contains(nowVisiting)) {
                stale.add(nowVisiting);
                 change |= true;
            }

            return stale.contains(nowVisiting);
        } finally {
            nowVisiting = savedNowVisiting;
        }
    }

    @Override
    public Boolean forApi(Api that) {
        boolean stale = visitCommon(that, visitedApis, staleApis, isStaleApi);
        /*
         * Note that the code below will "over-visit" implementing components,
         * but they will return immediately with no harm done.
         */
        
        if (linker != null) {
            Set<APIName> implementers = linker.get(that.getName());
            for (APIName n : implementers) {
                stale |= checkComponent(n);
            }
        }
        return stale;
    }

    private boolean checkComponent(APIName c) {
        try {
            ComponentIndex ci =
                isStaleComponent.isStale(c) ? source.getComponent(c) : derived.getComponent(c);
            return ci.ast().accept(this);
        } catch (Throwable ex) {
            componentExceptions.put(c , ex);
        }
        return false;
    }

    private boolean checkApi(APIName n) {
        try {
           ApiIndex ai = 
                isStaleApi.isStale(n) ? source.getApi(n) : derived.getApi(n);

            return ai.ast().accept(this);
        } catch (Throwable ex) {
            apiExceptions.put(n , ex);
        }
        return false;
    }

    @Override
    public Boolean forComponent(Component that) {
        return visitCommon(that, visitedComponents, staleComponents, 
                isStaleComponent);
    }

    @Override
    public Boolean forImportApi(ImportApi that) {
        boolean anyStale = false;
        List<AliasedAPIName> aapis = that.getApis();
        for (AliasedAPIName an : aapis) {
            anyStale |= checkApi(an.getApi());
        }
        return anyStale;
    }

    @Override
    public Boolean forImportNames(ImportNames that) {
        APIName n = that.getApi();
        return checkApi(n);
    }

    @Override
    public Boolean forImportStar(ImportStar that) {
        APIName n = that.getApi();
        return checkApi(n);
    }

}
