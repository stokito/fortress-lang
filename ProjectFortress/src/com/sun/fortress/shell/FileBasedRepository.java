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
import com.sun.fortress.interpreter.drivers.*;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.DottedName;

import java.io.*;
import java.util.*;

import static com.sun.fortress.shell.ConvenientStrings.*; 


public class FileBasedRepository implements FortressRepository {

    private final Map<DottedName, ApiIndex> apis = 
        new HashMap<DottedName, ApiIndex>(); 
    private final Map<DottedName, ComponentIndex> components = 
        new HashMap<DottedName, ComponentIndex>();
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
        // Cache component for quick retrieval.
        components.put(name, def);
        
        try {
            CompilationUnit ast = def.ast();
            Driver.writeJavaAst(ast, home + SEP + ast.getName() + fs.JAVA_AST_SUFFIX);
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }
}
