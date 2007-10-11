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
import com.sun.fortress.nodes.*;

import edu.rice.cs.plt.tuple.Option;

import java.io.*;
import java.util.*;

import static com.sun.fortress.shell.ConvenientStrings.*; 

public class FileBasedRepository implements FortressRepository {

    private final Map<DottedName, ApiIndex> apis = 
        new HashMap<DottedName, ApiIndex>(); 
    private final Map<DottedName, ComponentIndex> components = 
        new HashMap<DottedName, ComponentIndex>();
    private final String pwd;
    private final String path = Shell.fortressLocation();
    
    public FileBasedRepository(String _pwd) throws IOException {
        pwd = _pwd;
        initialize();
    }

    private void initialize() throws IOException { 
        
        List<File> files = Arrays.asList(Files.ls(path));
        
        for (File file: files) {
            Option<CompilationUnit> candidate = 
                Driver.readJavaAst(file.getName());
            
            if (candidate.isNone()) {
                throw new RepositoryError ("Compilation aborted. " +
                                           "There were problems reading back the compiled file " + 
                                           file.getCanonicalPath());
            }
            else {
                CompilationUnit _candidate = Option.unwrap(candidate);
                
                if (_candidate instanceof Api) {
                    ArrayList<Api> _candidates = new ArrayList<Api>();
                    _candidates.add((Api)_candidate);
                    apis.putAll(IndexBuilder.buildApis(_candidates).apis());
                } else if (_candidate instanceof Component) {
                    ArrayList<Component> _candidates = new ArrayList<Component>();
                    _candidates.add((Component)_candidate);
                    components.putAll(IndexBuilder.buildComponents(_candidates).components());
                } else {
                    throw new RuntimeException("The file " + file.getName() + " parsed to something other than a component or API!");
                }   
            }                                 
        }
    }
             

    public Map<DottedName, ApiIndex> apis() { return apis; }

    public void addApi(DottedName name, ApiIndex def) {
        apis.put(name, def);
        
        try {
            CompilationUnit ast = def.ast();
            Driver.writeJavaAst(ast, pwd + SEP + ast.getName() + fs.JAVA_AST_SUFFIX);
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }
    
    public void addComponent(DottedName name, ComponentIndex def) {
        // Cache component for quick retrieval.
        components.put(name, def);
        
        try {
            CompilationUnit ast = def.ast();
            Driver.writeJavaAst(ast, pwd + SEP + ast.getName() + fs.JAVA_AST_SUFFIX);
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }
}
