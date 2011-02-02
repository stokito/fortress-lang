/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.repository;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * Allows the {@link Fortress} class to interface with a custom repository
 * implementation.  May be based on a file system, database, transient
 * memory, etc.
 */
public interface FortressRepository {

    /**
     * Provide an updating view of the apis present in the repository.
     * Need not support mutation.
     */
    public Map<APIName, ApiIndex> apis();

    /**
     * Provide an updating view of the components present in the repository.
     * Need not support mutation.
     */
    public Map<APIName, ComponentIndex> components();

    /**
     * Add a compiled/processed api to the repository.
     */
    public void addApi(APIName name, ApiIndex definition);

    /**
     * Add a compiled/processed component to the repository.
     */
    public void addComponent(APIName name, ComponentIndex definition);

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
    public void deleteComponent(APIName name, boolean andTheFileToo);

    /**
     * Retrieve an api from the repository given a name.
     */
    public ApiIndex getApi(APIName name) throws FileNotFoundException, IOException;

    /**
     * Retrieve a component from the repository given a name.
     */
    public ComponentIndex getComponent(APIName name) throws FileNotFoundException, IOException, StaticError;

    /**
     * Retrieve a component from the repository that is linked properly to other components.
     */
    public ComponentIndex getLinkedComponent(APIName name) throws FileNotFoundException, IOException, StaticError;

    /**
     * Return the last modification date of an api.
     */
    public long getModifiedDateForApi(APIName name) throws FileNotFoundException;

    /**
     * Return the last modification date of a component.
     */
    public long getModifiedDateForComponent(APIName name) throws FileNotFoundException;

    /**
     * True if this API has a foreign implementation.
     *
     * @param name
     * @return
     */
    public boolean isForeign(APIName name);

    /**
     * Debugging methods.
     */
    public boolean setVerbose(boolean new_value);

    public boolean verbose();

    /**
     * Clear
     */

    public void clear();
}
