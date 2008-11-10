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

public class StubRepository implements FortressRepository {

    public void forgetComponent(APIName name) {
        deleteComponent(name);
    }

    private boolean verbose;

    /**
     * Sets "verbose" flag to new_verbose, returns old value.
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

    public void deleteComponent(APIName name) {
        throw new UnsupportedOperationException();

    }

    public Map<APIName, ApiIndex> apis() {
        throw new UnsupportedOperationException();
    }

    public Map<APIName, ComponentIndex> components() {
        throw new UnsupportedOperationException();
    }

    public ApiIndex getApi(APIName name) throws FileNotFoundException,
            IOException {
	    throw new FileNotFoundException("No such api");
    }

    public ComponentIndex getComponent(APIName name)
            throws FileNotFoundException, IOException {
	    throw new FileNotFoundException("No such component");
    }

    public long getModifiedDateForApi(APIName name)
            throws FileNotFoundException {
        throw new FileNotFoundException();
    }

    public long getModifiedDateForComponent(APIName name)
            throws FileNotFoundException {
        throw new FileNotFoundException();
    }

    public ComponentIndex getLinkedComponent(APIName name) throws FileNotFoundException, IOException {
	    throw new FileNotFoundException();
    }

    public void clear() {}

}
