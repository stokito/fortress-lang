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

package com.sun.fortress.compiler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.APIName;

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
    
    public void addApi(APIName name, ApiIndex definition);
    
    public void addComponent(APIName name, ComponentIndex definition);
   
    public ApiIndex getApi(APIName name) throws FileNotFoundException, IOException;
    
    public ComponentIndex getComponent(APIName name) throws FileNotFoundException, IOException;
    
    public long getModifiedDateForApi(APIName name) throws FileNotFoundException ;
    
    public long getModifiedDateForComponent(APIName name) throws FileNotFoundException ;

    
}
