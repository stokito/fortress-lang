/*
 * Created on Dec 2, 2007
 *
 */
package com.sun.fortress.shell;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.DottedName;

public class CacheBasedRepository implements FortressRepository {

    public void addApi(DottedName name, ApiIndex definition) {
        // TODO Auto-generated method stub

    }

    public void addComponent(DottedName name, ComponentIndex definition) {
        // TODO Auto-generated method stub

    }

    public Map<DottedName, ApiIndex> apis() {
        // TODO Auto-generated method stub
        return null;
    }

    public ApiIndex getApi(DottedName name) throws FileNotFoundException,
            IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public ComponentIndex getComponent(DottedName name)
            throws FileNotFoundException, IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public long getModifiedDateForApi(DottedName name)
            throws FileNotFoundException {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getModifiedDateForComponent(DottedName name)
            throws FileNotFoundException {
        // TODO Auto-generated method stub
        return 0;
    }

}
