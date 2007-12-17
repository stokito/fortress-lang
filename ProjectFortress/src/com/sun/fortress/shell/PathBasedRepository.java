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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.compiler.IndexBuilder.ComponentResult;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.interpreter.drivers.Driver;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.useful.Path;

import edu.rice.cs.plt.tuple.Option;

public class PathBasedRepository implements FortressRepository {

    final Path path;
    IndexBuilder builder = new IndexBuilder();
    
    private final Map<APIName, ApiIndex> apis = 
        new HashMap<APIName, ApiIndex>();
    
    private final Map<APIName, ComponentIndex> components = 
        new HashMap<APIName, ComponentIndex>();
   
    public PathBasedRepository(Path p) {
        path = p;
    }
    
    public void addApi(APIName name, ApiIndex definition) {
        throw new Error("Won't work");
    }

    public void addComponent(APIName name, ComponentIndex definition) {
        throw new Error("Won't work");
        
    }

    public Map<APIName, ApiIndex> apis() {
        // TODO Auto-generated method stub
        return null;
    }

    public ApiIndex getApi(APIName name) throws IOException {
        if (apis.containsKey(name))
            return apis.get(name);
        File fdot = findFile(name, ".fsi");
        // Attempt to parse fdot.
        
        Option<CompilationUnit> ocu = Driver.parseToJavaAst(fdot.getCanonicalPath());
        if (ocu.isNone()) {
            throw new Error("Parse error");
        } else {
            CompilationUnit cu = Option.unwrap(ocu);
            if (cu instanceof Api) {
                Api component = (Api) cu;
                ApiIndex ci = builder.buildApiIndex(component, fdot.lastModified());
                apis.put(name, ci);
                return ci;
            } else {
                throw new Error("Unexpected result type " +
                        cu.getClass().getSimpleName() +
                        " for api parse of " +
                        fdot.getCanonicalPath());
            }
         
        }
        
    }

    public ComponentIndex getComponent(APIName name) throws FileNotFoundException, IOException {
        if (components.containsKey(name))
            return components.get(name);
        File fdot = findFile(name, ".fss");
        // Attempt to parse fdot.
        
        Option<CompilationUnit> ocu = Driver.parseToJavaAst(fdot.getCanonicalPath());
        if (ocu.isNone()) {
            throw new Error("Parse error");
        } else {
            CompilationUnit cu = Option.unwrap(ocu);
            if (cu instanceof Component) {
                Component component = (Component) cu;
                ComponentIndex ci = builder.buildComponentIndex(component, fdot.lastModified());
                components.put(name, ci);
                return ci;
            } else {
                throw new Error("Unexpected result type " +
                        cu.getClass().getSimpleName() +
                        " for component parse of " +
                        fdot.getCanonicalPath());
            }
        } 
    }

    private File findFile(APIName name, String suffix) throws FileNotFoundException {
        String dotted = name.toString();
        String slashed = dotted.replaceAll(".", "/");
        dotted = dotted + suffix;
        slashed = slashed + suffix;
        File fdot;
        try {
            fdot = path.findFile(dotted);
        } catch (FileNotFoundException ex1) {
            fdot = path.findFile(slashed);
        }
        return fdot;
    }

    public long getModifiedDateForApi(APIName name) throws FileNotFoundException {
        if (apis.containsKey(name))
            return apis.get(name).modifiedDate();
        File fdot = findFile(name, ".fsi");
        return fdot.lastModified();
    }

    public long getModifiedDateForComponent(APIName name) throws FileNotFoundException {
        if (components.containsKey(name))
            return components.get(name).modifiedDate();
        File fdot = findFile(name, ".fss");
        return fdot.lastModified();
    }

}
