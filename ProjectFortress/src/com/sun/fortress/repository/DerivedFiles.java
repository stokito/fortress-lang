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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.nodes.APIName;

public class DerivedFiles<T> {
    protected final Map<APIName, T> cached =
        new HashMap<APIName, T>();
    
    protected final IO<APIName, T> ioForAPINames;
    
    public DerivedFiles (IO<APIName, T> io) {
        this.ioForAPINames = io;
    }
    
    T get(APIName name, long mustBeNewerThan) {
        T x = cached.get(name);
        if (x == null) {
            try {
                long lastModified = ioForAPINames.lastModified(name);
                if (lastModified < mustBeNewerThan)
                    return null;
                x = ioForAPINames.read(name);
            } catch (IOException e) {
                /*
                 * This will probably never hit, because of the lastModified
                 * check above. 
                 */ 
                
                InterpreterBug.bug("Failed to read " + name);
            }
        }
        return x;
    }
    
    void put(APIName name, T x) {
        cached.put(name, x);
        try {
            ioForAPINames.write(name, x);
        } catch (IOException e) {
            InterpreterBug.bug("Failed to write " + name);
        }
    }

}
