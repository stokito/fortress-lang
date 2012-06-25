/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.repository;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.APIName;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

abstract public class StubRepository implements FortressRepository {

    private boolean verbose;

    /**
     * Sets "verbose" flag to new_verbose, returns old value.
     *
     * @param new_verbose
     * @return
     */
    public boolean setVerbose(boolean new_verbose) {
        boolean old_verbose = verbose;
        verbose = new_verbose;
        return old_verbose;
    }

    public boolean verbose() {
        return verbose;
    }


    public void addApi(APIName name, ApiIndex definition) {
        throw new UnsupportedOperationException();
    }

    public void addComponent(APIName name, ComponentIndex definition) {
        throw new UnsupportedOperationException();

    }

    /**
     * Removes the AST for the component form any in-memory caches and/or maps,
     * and optionally remove it from any stable storage as well.
     * <p/>
     * Used to avoid memory leaks in unit testing, and to clear non-standard
     * scribbles from the cache.
     *
     * @param name
     * @param andTheFileToo
     */
    public void deleteComponent(APIName name, boolean andTheFileToo) {
        throw new UnsupportedOperationException();

    }

    public Map<APIName, ApiIndex> apis() {
        throw new UnsupportedOperationException();
    }

    public Map<APIName, ComponentIndex> components() {
        throw new UnsupportedOperationException();
    }

    public ApiIndex getApi(APIName name) throws FileNotFoundException, IOException {
        throw new FileNotFoundException("No such api");
    }

    public ComponentIndex getComponent(APIName name) throws FileNotFoundException, IOException {
        throw new FileNotFoundException("No such component");
    }

    public long getModifiedDateForApi(APIName name) throws FileNotFoundException {
        throw new FileNotFoundException();
    }

    public long getModifiedDateForComponent(APIName name) throws FileNotFoundException {
        throw new FileNotFoundException();
    }

    public ComponentIndex getLinkedComponent(APIName name) throws FileNotFoundException, IOException {
        throw new FileNotFoundException();
    }

    public void clear() {
    }

}
