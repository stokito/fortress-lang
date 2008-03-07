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

package com.sun.fortress.shell;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.compiler.RepositoryUpdater;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.MinimalMap;
import com.sun.fortress.useful.Path;
import com.sun.fortress.useful.ReversedList;
import com.sun.fortress.useful.Useful;

public class BatchCachingRepository implements FortressRepository {

    private final FortressRepository source;

    private final FortressRepository derived;

    private final RepositoryUpdater ru;
    private final Set<APIName> alreadyCachedComponent = new HashSet<APIName>();
    private final Set<APIName> alreadyCachedApi = new HashSet<APIName>();
    
    private final List<APIName> alreadyCachedApiList = new ArrayList<APIName>();
    
    public BatchCachingRepository(FortressRepository source,
            FortressRepository cache) {
        this(false, source, cache);
    }
    
    /**
     * This constructor helps us evade a circularity.
     * 
     * @param doLink
     * @param p
     * @param cache
     */
   public BatchCachingRepository(boolean doLink, Path p,
            FortressRepository cache) {
        // TODO is this a fix?
        this.source = new PathBasedSyntaxTransformingRepository(p); // WAS , this
        this.derived = cache;
        MinimalMap<APIName, Set<APIName>> linker = linker(doLink);
        this.ru = new RepositoryUpdater(source, derived, linker);
    }
   
   public BatchCachingRepository(Path p,
           FortressRepository cache) {
       this(false, p, cache);
   }
   
    public BatchCachingRepository(boolean doLink, FortressRepository source,
            FortressRepository cache) {
        this.source = source;
        this.derived = cache;

        MinimalMap<APIName, Set<APIName>> linker = linker(doLink);
  
        this.ru = new RepositoryUpdater(source, derived, linker);
    }

    private static MinimalMap<APIName, Set<APIName>> linker(boolean doLink) {
        MinimalMap<APIName, Set<APIName>> linker = doLink ? new MinimalMap<APIName, Set<APIName>>() {
                public Set<APIName> get(APIName key) {
                    return Useful.set(key);
                }
            } : new MinimalMap<APIName, Set<APIName>>() {
                public Set<APIName> get(APIName key) {
                    return Collections.emptySet();
                }
            };
        return linker;
    }

    public Iterable<APIName> staleApis() {
        return ru.staleApis;
    }
    
    public Iterable<APIName> staleComponents() {
        return ru.staleComponents;
    }
    
    public void addRootComponents(APIName... roots) {
        boolean anyChange = false;
        for (APIName n : roots) {
            if (!alreadyCachedComponent.contains(n)) {
                anyChange = true;
                ru.addComponent(n);
            }
        }
        if (anyChange) {
            refreshCache();
        }
    }

    public void addRootApis(APIName... roots) {
        boolean anyChange = false;
        for (APIName n : roots) {
            if (!alreadyCachedApi.contains(n)) {
                anyChange = true;
                ru.addApi(n);
            }
        }
        if (anyChange) {
            refreshCache();
        }
    }

    public void addRootApis(String... roots) {
        boolean anyChange = false;
        for (String s : roots) {
            APIName n = NodeFactory.makeAPIName(s);
            if (!alreadyCachedApi.contains(n)) {
                anyChange = true;
                ru.addApi(n);
            }
        }
        if (anyChange) {
            refreshCache();
        }
    }

    /**
     * Updates the derived repository with new versions of ASTs
     * identified as "stale" by the repository updater.
     */
    protected void refreshCache() {
        try {
            for (APIName name : ru.staleApis) {
                if (!alreadyCachedApi.contains(name)) {
                    addApi(name, source.getApi(name));
                }
            }
            for (APIName name : ru.staleComponents) {
                if (!alreadyCachedComponent.contains(name)) {
                    addComponent(name, source.getComponent(name));
                }
            }
        } catch (IOException ex) {
            /* Any exceptions seen here are reported elsewhere. */
   
        }
    }
    
    Fn<APIName, Api> toApi = new Fn<APIName, Api>() {

        @Override
        public Api apply(APIName x) {
            try {
                ApiIndex xi = source.getApi(x);
                return (Api) xi.ast();
            } catch (FileNotFoundException e) {
                throw new Error(e);
            } catch (IOException e) {
                throw new Error(e);
            }
            
        }
        
    };
    
    Fn<APIName, Component> toComponent = new Fn<APIName, Component>() {

        @Override
        public Component apply(APIName x) {
            try {
                ComponentIndex xi = source.getComponent(x);
                return (Component) xi.ast();
            } catch (FileNotFoundException e) {
                throw new Error(e);
            } catch (IOException e) {
                throw new Error(e);
            }
            
        }
        
    };
    
    public Set<APIName> newStaleApiNames() {
        return Useful.difference(ru.staleApis, alreadyCachedApi);
    }
    
    public Set<APIName> newStaleComponentNames() {
        return Useful.difference(ru.staleComponents, alreadyCachedComponent);
    }
    
    protected Set<Api> newStaleApis() {
        return Useful.applyToAll(newStaleApiNames(),
                toApi);
    }

    protected Set<Component> newStaleComponents() {
        return Useful.applyToAll(newStaleComponentNames(),
                toComponent);
    }
    
    protected void resetStale() {
        ru.staleApis.clear();
        ru.staleComponents.clear();
    }

    public void addApi(APIName name, ApiIndex definition) {
        if (alreadyCachedApi.contains(name))
            return;
        derived.addApi(name, definition);
        alreadyCachedApi.add(name);
        alreadyCachedApiList.add(name);
    }

    public void addComponent(APIName name, ComponentIndex definition) {
        if (alreadyCachedComponent.contains(name))
            return;
        derived.addComponent(name, definition);
        alreadyCachedComponent.add(name);
    }

    public Map<APIName, ApiIndex> apis() {
        return derived.apis();
    }

    private void resurrectException(Throwable th) throws IOException,
            RuntimeException, Error {
        if (th != null) {
            if (th instanceof IOException)
                throw (IOException) th;
            if (th instanceof RuntimeException)
                throw (RuntimeException) th;
            if (th instanceof Error)
                throw (Error) th;
            throw new Error(th);
        }
    }

    public ApiIndex getApi(APIName name) throws FileNotFoundException,
            IOException {
        addRootApis(name);
        Throwable th = ru.apiExceptions.get(name);
        resurrectException(th);
        return derived.getApi(name);
    }

    public ComponentIndex getComponent(APIName name)
    throws FileNotFoundException, IOException {
        addRootComponents(name);
        Throwable th = ru.componentExceptions.get(name);
        resurrectException(th);
        return derived.getComponent(name);
    }

    public ComponentIndex getLinkedComponent(APIName name)
    throws FileNotFoundException, IOException {
        ComponentIndex ci = getComponent(name);
        
        // Cannot use iterator, will fail with comodification exception.
        // Expect to add APIs as new components are inhaled.
        for (int i = 0; i < alreadyCachedApiList.size(); i++) {
            APIName n = alreadyCachedApiList.get(i);
            // TODO Someday we will have a REAL linker.
            APIName implementor = n;
            // For side-effect only; get the component, and all its APIs.
            ComponentIndex ici = getComponent(implementor);
        }
        
        return ci;
}

public long getModifiedDateForApi(APIName name)
            throws FileNotFoundException {

        return derived.getModifiedDateForApi(name);
    }

    public long getModifiedDateForComponent(APIName name)
            throws FileNotFoundException {

        return derived.getModifiedDateForComponent(name);
    }

}
