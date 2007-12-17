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

import static com.sun.fortress.shell.ConvenientStrings.DOT;
import static com.sun.fortress.shell.ConvenientStrings.SEP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.interpreter.drivers.Driver;
import com.sun.fortress.interpreter.drivers.fs;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;

import edu.rice.cs.plt.tuple.Option;

public class CacheBasedRepository implements FortressRepository {

    
    protected final Map<APIName, ApiIndex> apis = 
        new HashMap<APIName, ApiIndex>(); 
    protected final Map<APIName, ComponentIndex> components = 
        new HashMap<APIName, ComponentIndex>();
 
    protected final String pwd;
    
    public CacheBasedRepository(String _pwd) {
        pwd = _pwd;
    }

    public Map<APIName, ApiIndex> apis() { return apis; }    
    public ApiIndex getApi(APIName name) { return apis.get(name); }
    public ComponentIndex getComponent(APIName name) { return components.get(name); }

    public void addApi(APIName name, ApiIndex def) {
        apis.put(name, def);
        
        try {
            CompilationUnit ast = def.ast();
            if (ast instanceof Component) {
                Driver.writeJavaAst(ast, pwd + SEP + ast.getName() + 
                                    DOT + Driver.COMP_TREE_SUFFIX);
            }
            else { // ast instanceof Api
                Driver.writeJavaAst(ast, pwd + SEP + ast.getName() + 
                                    DOT + Driver.API_TREE_SUFFIX);
            }
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }
    
    public void addApis(Map<APIName, ApiIndex> newApis) {
        for (Map.Entry<APIName, ApiIndex> entry: newApis.entrySet()) {
            addApi(entry.getKey(), entry.getValue());
        }
    }
    
    public void addComponent(APIName name, ComponentIndex def) {
        // Cache component for quick retrieval.
        components.put(name, def);
        
        try {
            CompilationUnit ast = def.ast();
            Driver.writeJavaAst(ast, pwd + SEP + ast.getName() + fs.JAVA_AST_SUFFIX);
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }

    public long getModifiedDateForApi(APIName name) {
        return apis.get(name).modifiedDate();
       
    }

    public long getModifiedDateForComponent(APIName name) {
        return components.get(name).modifiedDate();
    }
}
