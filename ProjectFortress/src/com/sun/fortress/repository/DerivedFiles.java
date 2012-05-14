/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.repository;

import com.sun.fortress.compiler.PathTaggedApiName;
import com.sun.fortress.exceptions.InterpreterBug;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DerivedFiles<T> {
    protected final Map<PathTaggedApiName, T> cached = new HashMap<PathTaggedApiName, T>();

    protected final IO<PathTaggedApiName, T> ioForPathTaggedApiNames;

    public DerivedFiles(IO<PathTaggedApiName, T> io) {
        this.ioForPathTaggedApiNames = io;
    }

    public T get(PathTaggedApiName name, long mustBeNewerThan) {
        T x = cached.get(name);
        if (x == null) {
            try {
                long lastModified = ioForPathTaggedApiNames.lastModified(name);
                if (lastModified < mustBeNewerThan) return null;
                x = ioForPathTaggedApiNames.read(name);
                cached.put(name, x);
            }
            catch (IOException e) {
                /*
                 * This will probably never hit, because of the lastModified
                 * check above.
                 *
                 * It DOES hit, if a compilation is interrupted.
                 */

                return null;
            }
        }
        return x;
    }

    public void put(PathTaggedApiName name, T x) {
        cached.put(name, x);
        try {
            ioForPathTaggedApiNames.write(name, x);
        }
        catch (IOException e) {
            InterpreterBug.bug("Failed to write " + name);
        }
    }

    public void forget(PathTaggedApiName name) {
        cached.remove(name);
    }

}
