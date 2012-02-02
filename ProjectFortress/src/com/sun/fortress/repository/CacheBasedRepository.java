 /*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.repository;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.environments.SimpleClassLoader;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.shell.RepositoryError;
import com.sun.fortress.exceptions.shell.ShellException;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.nodes_util.NodeComparator;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.repository.graph.ApiGraphNode;
import com.sun.fortress.repository.graph.ComponentGraphNode;
import com.sun.fortress.scala_src.typechecker.IndexBuilder;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.exceptions.*;
import edu.rice.cs.plt.tuple.Option;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class CacheBasedRepository { // extends StubRepository implements FortressRepository {


    protected final BATree<APIName, ApiIndex> apis = new BATree<APIName, ApiIndex>(NodeComparator.apiNameComparer);

    protected final BATree<APIName, ComponentIndex> components =
            new BATree<APIName, ComponentIndex>(NodeComparator.apiNameComparer);

    protected final String pwd;
    
    public CacheBasedRepository(String _pwd) {
        pwd = _pwd;
    }

    public Map<APIName, ApiIndex> apis() {
        return apis;
    }

    public Map<APIName, ComponentIndex> components() {
        return components;
    }

    public ApiIndex getApi(APIName name, String sourcePath) throws FileNotFoundException, IOException {
        ApiIndex ci = apis.get(name);
        if (ci != null) return ci;
        String s = apiFileName(name, sourcePath);

        File f = new File(s);
        if (!f.exists()) {
            throw new FileNotFoundException(s);
        }
        Option<CompilationUnit> candidate = ASTIO.readJavaAst(s);
        if (candidate.isNone()) {
            throw new RepositoryError("Could not deserialize contents of repository file " + s);
        }
        ci = (ApiIndex)IndexBuilder.buildCompilationUnitIndex(candidate.unwrap(),
                                                              f.lastModified(), true);
        apis.put(name, ci);
        return ci;
    }

    public ComponentIndex getComponent(APIName name, String sourcePath) throws FileNotFoundException, IOException {
        ComponentIndex ci = components.get(name);
        if (ci != null) return ci;
        String s = compFileName(name, sourcePath);

        File f = new File(s);
        if (!f.exists()) {
            throw new FileNotFoundException(s);
        }
        Option<CompilationUnit> candidate = ASTIO.readJavaAst(s);
        if (candidate.isNone()) {
            throw new RepositoryError("Could not deserialize contents of repository file " + s);
        }
        ci = (ComponentIndex)IndexBuilder.buildCompilationUnitIndex(candidate.unwrap(),
                                                                    f.lastModified(), false);
        components.put(name, ci);
        return ci;

    }

    public void addApi(APIName name, ApiIndex def, String sourcePath) {
        CompilationUnit ast = def.ast();
        checkName(name, ast);

        Debug.debug(Debug.Type.REPOSITORY, 2, "Api ", name, " created at ", def.modifiedDate());
        apis.put(name, def);

        try {
            ASTIO.writeJavaAst(ast, apiFileName(ast.getName(), sourcePath));
        }
        catch (IOException e) {
            throw new ShellException(e);
        }
    }

    private void checkName(APIName name, CompilationUnit ast) throws RepositoryError {
        APIName actual = ast.getName();
        if (!actual.equals(name)) {
            //boolean flag = actual.equals(name);
            throw new RepositoryError(actual + " cannot be cached under name " + name);
        }
    }

    public void addComponent(APIName name, ComponentIndex def, String sourcePath) {
        CompilationUnit ast = def.ast();
        checkName(name, ast);
        Debug.debug(Debug.Type.REPOSITORY, 2, "addComponent: Component ", name, " created at ", def.modifiedDate());
        // Cache component for quick retrieval.
        components.put(name, def);

        try {
            ASTIO.writeJavaAst(ast, compFileName(ast.getName(), sourcePath));
        }
        catch (IOException e) {
            throw new ShellException(e);
        }
    }

    public void deleteApi(APIName name) {
        apis.remove(name);
    }

    public void deleteComponent(APIName name) {
        components.remove(name);
        SimpleClassLoader.reloadEnvironment(NodeUtil.nameString(name));
    }

    private String compFileName(APIName name, String sourcePath) {
        return NamingCzar.cachedPathNameForCompAst(pwd, sourcePath, name);
    }

    private String apiFileName(APIName name, String sourcePath) {
        return NamingCzar.cachedPathNameForApiAst(pwd, sourcePath, name);
    }

    private long dateFromFile(APIName name, String s, String tag) throws FileNotFoundException {
        File f = new File(s);
        if (!f.exists()) throw new FileNotFoundException(tag + name.toString() + " (file " + s + ")");
        return f.lastModified();
    }

    public long getModifiedDateForApi(ApiGraphNode node) throws FileNotFoundException {
        APIName name = node.getName();
        String sourcePath = node.getSourcePath();
        ApiIndex i = apis.get(name);

        if (i != null) {
            Debug.debug(Debug.Type.REPOSITORY, 2, "Cached modified date for api ", name, " is ", i.modifiedDate());
            return i.modifiedDate();
        }

        String s = apiFileName(name, sourcePath);
        String tag = "API ";
        return dateFromFile(name, s, tag);
    }

    public long getModifiedDateForComponent(ComponentGraphNode node) throws FileNotFoundException {
        APIName name = node.getName();
        String sourcePath = node.getSourcePath();

        ComponentIndex i = components.get(name);
        if (i != null) {
            Debug.debug(Debug.Type.REPOSITORY,
                        2,
                        "Cached modified date for component ",
                        name,
                        " is ",
                        i.modifiedDate());
            return i.modifiedDate();
        }

        String s = compFileName(name, sourcePath);
        String tag = "Component ";
        return dateFromFile(name, s, tag);
    }

    public void clear() {
        for (APIName apiName : apis.keySet()) {
            deleteApi(apiName);
        }
        for (APIName componentName : components.keySet()) {
            deleteComponent(componentName);
        }
    }
        

}
