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
import com.sun.fortress.nodes.DottedName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.useful.Path;

import edu.rice.cs.plt.tuple.Option;

public class PathBasedRepository implements FortressRepository {

    final Path path;
    IndexBuilder builder = new IndexBuilder();
    
    private final Map<DottedName, ApiIndex> apis = 
        new HashMap<DottedName, ApiIndex>();
    
    private final Map<DottedName, ComponentIndex> components = 
        new HashMap<DottedName, ComponentIndex>();
   
    public PathBasedRepository(Path p) {
        path = p;
    }
    
    public void addApi(DottedName name, ApiIndex definition) {
        throw new Error("Won't work");
    }

    public void addComponent(DottedName name, ComponentIndex definition) {
        throw new Error("Won't work");
        
    }

    public Map<DottedName, ApiIndex> apis() {
        // TODO Auto-generated method stub
        return null;
    }

    public ApiIndex getApi(DottedName name) throws IOException {
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

    public ComponentIndex getComponent(DottedName name) throws FileNotFoundException, IOException {
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

    private File findFile(DottedName name, String suffix) throws FileNotFoundException {
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

    public long getModifiedDateForApi(DottedName name) throws FileNotFoundException {
        if (apis.containsKey(name))
            return apis.get(name).modifiedDate();
        File fdot = findFile(name, ".fsi");
        return fdot.lastModified();
    }

    public long getModifiedDateForComponent(DottedName name) throws FileNotFoundException {
        if (components.containsKey(name))
            return components.get(name).modifiedDate();
        File fdot = findFile(name, ".fss");
        return fdot.lastModified();
    }

}
