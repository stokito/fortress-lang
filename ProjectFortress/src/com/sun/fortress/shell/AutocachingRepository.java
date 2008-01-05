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
import java.util.Map;

import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.APIName;

public class AutocachingRepository implements FortressRepository {

    private final FortressRepository source;
    private final FortressRepository cache;
    
    public AutocachingRepository(FortressRepository source, FortressRepository cache) {
        this.source = source;
        this.cache = cache;
    }
    
    public void addApi(APIName name, ApiIndex definition) {
        cache.addApi(name, definition);

    }

    public void addComponent(APIName name, ComponentIndex definition) {
        cache.addComponent(name, definition);
    }

    public Map<APIName, ApiIndex> apis() {
        // TODO Auto-generated method stub
        return null;
    }

    public ApiIndex getApi(APIName name) throws FileNotFoundException,
            IOException {
        long s = source.getModifiedDateForApi(name) ;
        try {
            long c = cache.getModifiedDateForApi(name);
            if (c > s)
                return cache.getApi(name);
        } catch (FileNotFoundException ex) {
            // Missing = "older"
        }
        ApiIndex i = source.getApi(name);
        cache.addApi(name, i);
        return i;
    }

    public ComponentIndex getComponent(APIName name)
            throws FileNotFoundException, IOException {
        long s = source.getModifiedDateForComponent(name) ;
        try {
            long c = cache.getModifiedDateForComponent(name);
            if (c > s)
                return cache.getComponent(name);
        } catch (FileNotFoundException ex) {
            // Missing = "older"
        }
        ComponentIndex i = source.getComponent(name);
        cache.addComponent(name, i);
        return i;
    }

    public long getModifiedDateForApi(APIName name)
            throws FileNotFoundException {
        long s = source.getModifiedDateForApi(name) ;
        try {
            long c = cache.getModifiedDateForApi(name);
            if (c > s)
                return c;
        } catch (FileNotFoundException ex) {
            // Missing = "older"
        }
        
        return s;
    }

    public long getModifiedDateForComponent(APIName name)
            throws FileNotFoundException {
        long s = source.getModifiedDateForComponent(name) ;
        try {
            long c = cache.getModifiedDateForComponent(name);
            if (c > s)
                return c;
        } catch (FileNotFoundException ex) {
            // Missing = "older"
        }
        return s;
    }

}
