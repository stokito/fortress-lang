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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.compiler.RepositoryUpdater;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.useful.MinimalMap;
import com.sun.fortress.useful.Useful;

public class BatchCachingRepository implements FortressRepository {

    private final FortressRepository source;

    private final FortressRepository derived;

    private final RepositoryUpdater ru;

    private final Set<APIName> alreadyCachedComponent = new HashSet<APIName>();

    private final Set<APIName> alreadyCachedApi = new HashSet<APIName>();

    MinimalMap<APIName, Set<APIName>> linker = new MinimalMap<APIName, Set<APIName>>() {
        public Set<APIName> get(APIName key) {
            return Useful.set(key);
        }
    };

    public BatchCachingRepository(FortressRepository source,
            FortressRepository cache) {
        this.source = source;
        this.derived = cache;
        this.ru = new RepositoryUpdater(source, derived, linker);
    }

    public void addRoots(APIName... roots) {
        for (APIName n : roots) {
            if (!alreadyCachedComponent.contains(n))
                ru.addComponent(n);
        }
        try {
            for (APIName name : ru.staleApis) {
                if (!alreadyCachedApi.contains(name)) {
                    derived.addApi(name, source.getApi(name));
                    alreadyCachedApi.add(name);
                }
            }
            for (APIName name : ru.staleComponents) {
                if (!alreadyCachedComponent.contains(name)) {
                    derived.addComponent(name, source.getComponent(name));
                    alreadyCachedComponent.add(name);
                }
            }
        } catch (IOException ex) {
            /* Any exceptions seen here are reported elsewhere. */

        }
    }

    public void addApi(APIName name, ApiIndex definition) {
        throw new Error();

    }

    public void addComponent(APIName name, ComponentIndex definition) {
        throw new Error();

    }

    public Map<APIName, ApiIndex> apis() {
        throw new Error();
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
        Throwable th = ru.apiExceptions.get(name);
        resurrectException(th);
        return derived.getApi(name);
    }

    public ComponentIndex getComponent(APIName name)
            throws FileNotFoundException, IOException {

        Throwable th = ru.componentExceptions.get(name);
        resurrectException(th);
        return derived.getComponent(name);
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
