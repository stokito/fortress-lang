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

package com.sun.fortress.repository;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.exceptions.StaticError;

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
    public long getModifiedDateForApi(APIName name) throws FileNotFoundException ;

    /**
     * Return the last modification date of a component.
     */
    public long getModifiedDateForComponent(APIName name) throws FileNotFoundException ;

    /**
     * Debugging methods.
     */
    public boolean setVerbose(boolean new_value);
    public boolean verbose();
}
