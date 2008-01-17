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
import com.sun.fortress.interpreter.drivers.ASTIO;
import com.sun.fortress.interpreter.drivers.ProjectProperties;
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
    
    public ApiIndex getApi(APIName name) throws FileNotFoundException,
            IOException {
        ApiIndex ci = apis.get(name);
        if (ci != null)
            return ci;
        String s = apiFileName(name);

        File f = new File(s);
        if (!f.exists()) {
            throw new FileNotFoundException(s);
        }
        Option<CompilationUnit> candidate = ASTIO.readJavaAst(s);
        if (candidate.isNone()) {
            throw new RepositoryError(
                    "Could not deserialize contents of repository file " + s);
        }
        ci = IndexBuilder.builder.buildApiIndex((Api) Option
                .unwrap(candidate), f.lastModified());
        apis.put(name, ci);
        return ci;
    }
    public ComponentIndex getComponent(APIName name) throws FileNotFoundException, IOException {
        ComponentIndex ci = components.get(name);
        if (ci != null)
            return ci;
        String s = compFileName(name);
        
        File f = new File(s);
        if (! f.exists()) {
            throw new FileNotFoundException(s);
        }
        Option<CompilationUnit> candidate = ASTIO.readJavaAst(s);
        if (candidate.isNone()) {
            throw new RepositoryError("Could not deserialize contents of repository file " + s);
        }
        ci = IndexBuilder.builder.buildComponentIndex((Component) Option.unwrap(candidate), f.lastModified());
        components.put(name, ci);
        return ci;
        
    }

    public void addApi(APIName name, ApiIndex def) {
        CompilationUnit ast = def.ast();
        checkName(name, ast);
        
        apis.put(name, def);
        
        try {
            
            if (ast instanceof Component) {
                ASTIO.writeJavaAst(ast, pwd + SEP + ast.getName() + 
                                    DOT + ProjectProperties.COMP_TREE_SUFFIX);
            }
            else { // ast instanceof Api
                ASTIO.writeJavaAst(ast, pwd + SEP + ast.getName() + 
                                    DOT + ProjectProperties.API_TREE_SUFFIX);
            }
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }

    private void checkName(APIName name, CompilationUnit ast)
            throws RepositoryError {
        APIName actual = ast.getName();
        if (! actual.equals(name)) {
            boolean flag = actual.equals(name);
            throw new RepositoryError(ast.getName() + " cannot be cached under name " + name);
        }
    }
    
    public void addApis(Map<APIName, ApiIndex> newApis) {
        for (Map.Entry<APIName, ApiIndex> entry: newApis.entrySet()) {
            addApi(entry.getKey(), entry.getValue());
        }
    }
    
    public void addComponent(APIName name, ComponentIndex def) {
        CompilationUnit ast = def.ast();
        checkName(name, ast);
        // Cache component for quick retrieval.
        components.put(name, def);
        
        try {
            name = ast.getName();
            ASTIO.writeJavaAst(ast, compFileName(name));
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }

    private String compFileName(APIName name) {
        return pwd + SEP + name + DOT + ProjectProperties.COMP_TREE_SUFFIX;
    }

    private String apiFileName(APIName name) {
        return pwd + SEP + name + DOT + ProjectProperties.API_TREE_SUFFIX;
    }

    private long dateFromFile(APIName name, String s, String tag)
            throws FileNotFoundException {
        File f = new File(s);
            if (! f.exists())
                throw new FileNotFoundException(tag + name.toString() + " (file " + s + ")");
            return f.lastModified();
    }

    public long getModifiedDateForApi(APIName name) throws FileNotFoundException {
        ApiIndex i = apis.get(name);
        
        if (i != null)
            return i.modifiedDate();
        
       String s = apiFileName(name);
       String tag = "API ";
       return dateFromFile(name, s, tag);
    }

    public long getModifiedDateForComponent(APIName name) throws FileNotFoundException {
        ComponentIndex i = components.get(name);
        if (i != null)
            return i.modifiedDate();
        
       String s = compFileName(name);
       String tag = "Component ";
       return dateFromFile(name, s, tag);
    }
}
