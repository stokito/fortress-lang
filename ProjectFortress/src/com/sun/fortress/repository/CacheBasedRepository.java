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

import static com.sun.fortress.useful.ConvenientStrings.DOT;
import static com.sun.fortress.useful.ConvenientStrings.SEP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.compiler.environments.SimpleClassLoader;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.shell.RepositoryError;
import com.sun.fortress.exceptions.shell.ShellException;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.tuple.Option;

public class CacheBasedRepository extends StubRepository implements FortressRepository {


    protected final Map<APIName, ApiIndex> apis =
        new HashMap<APIName, ApiIndex>();
    protected final Map<APIName, ComponentIndex> components =
        new HashMap<APIName, ComponentIndex>();

    protected final String pwd;

    public CacheBasedRepository(String _pwd) {
        pwd = _pwd;
    }

    public Map<APIName, ApiIndex> apis() { return apis; }

    public Map<APIName, ComponentIndex> components() { return components; }

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
        ci = IndexBuilder.builder.buildApiIndex((Api) candidate.unwrap(), f.lastModified());
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
        ci = IndexBuilder.builder.buildComponentIndex((Component) candidate.unwrap(), f.lastModified());
        components.put(name, ci);
        return ci;

    }

    public void addApi(APIName name, ApiIndex def) {
        CompilationUnit ast = def.ast();
        checkName(name, ast);

        Debug.debug( Debug.Type.REPOSITORY, 2, "Api ", name, " created at ", def.modifiedDate() );
        apis.put(name, def);

        try {
            ASTIO.writeJavaAst(ast, apiFileName(ast.getName() ));
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }

    private void checkName(APIName name, CompilationUnit ast)
            throws RepositoryError {
        APIName actual = ast.getName();
        if (! actual.equals(name)) {
            boolean flag = actual.equals(name);
            throw new RepositoryError(actual + " cannot be cached under name " + name);
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
        Debug.debug( Debug.Type.REPOSITORY, 2, "Component ", name, " created at ", def.modifiedDate() );
        // Cache component for quick retrieval.
        components.put(name, def);

        try {
            ASTIO.writeJavaAst(ast, compFileName(ast.getName()));
        } catch (IOException e) {
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

    public static String deCase(APIName s) {
        return "-" + Integer.toString(s.getText().hashCode()&0x7fffffff,16);
    }

    public static String deCaseName(APIName s) {
        return s + "-" + Integer.toString(s.getText().hashCode()&0x7fffffff,16);
    }

    public static String cachedCompFileName(String passedPwd, APIName name) {
        return ProjectProperties.compFileName(passedPwd,  deCaseName(name));
    }

    private String compFileName(APIName name) {
        return ProjectProperties.compFileName(pwd,  deCaseName(name));
    }

    private String apiFileName(APIName name) {
        return ProjectProperties.apiFileName(pwd,  deCaseName(name));
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

        if (i != null){
            Debug.debug( Debug.Type.REPOSITORY, 2, "Cached modified date for api ", name, " is ", i.modifiedDate() );
            return i.modifiedDate();
        }

       String s = apiFileName(name);
       String tag = "API ";
       return dateFromFile(name, s, tag);
    }

    public long getModifiedDateForComponent(APIName name) throws FileNotFoundException {
        ComponentIndex i = components.get(name);
        if (i != null){
            Debug.debug( Debug.Type.REPOSITORY, 2, "Cached modified date for component ", name, " is ", i.modifiedDate() );
            return i.modifiedDate();
        }

       String s = compFileName(name);
       String tag = "Component ";
       return dateFromFile(name, s, tag);
    }

    public void clear() {
        for(APIName apiName : apis.keySet()) {
            deleteApi(apiName);
        }
        for(APIName componentName : components.keySet()) {
            deleteComponent(componentName);
        }
    }

}
