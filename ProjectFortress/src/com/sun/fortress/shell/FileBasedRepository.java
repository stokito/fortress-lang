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

package com.sun.fortress.shell;

import com.sun.fortress.compiler.*;
import com.sun.fortress.compiler.index.*;
import com.sun.fortress.nodes.DottedName;

import java.io.*;
import java.util.*;

public class FileBasedRepository implements FortressRepository {

    private final Map<DottedName, ApiIndex> apis = 
 new HashMap<DottedName, ApiIndex>(); 
    private final String home;
    
    public FileBasedRepository(String _home) {
        home = _home;
        initializeApis();
    }

    private void initializeApis() {
    }

    public Map<DottedName, ApiIndex> apis() { return apis; }

    public void addApi(DottedName name, ApiIndex def) {
 apis.put(name, def);
    }
    
    public void addComponent(DottedName name, ComponentIndex def) {
 // write component file to pwd.
    }
}
