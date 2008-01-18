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

import com.sun.fortress.compiler.*;
import com.sun.fortress.compiler.index.*;
import com.sun.fortress.interpreter.drivers.*;
import com.sun.fortress.nodes.*;

import edu.rice.cs.plt.tuple.Option;

import java.io.*;
import java.util.*;

import static com.sun.fortress.shell.ConvenientStrings.*; 

public class FileBasedRepository extends CacheBasedRepository implements FortressRepository {

    private final String path;
    
    public FileBasedRepository(String _pwd) throws IOException {
        this(_pwd, Shell.fortressLocation());
    }
    
    public FileBasedRepository(String _pwd, String _path) throws IOException {
        super(_pwd);
        path = _path;
        initialize();
    }

    private void initialize() throws IOException { 
         File[] fileArray = Files.ls(path);
        if (fileArray == null) {
            throw new IOException("Apparently there are no files in " + path);
        }
        List<File> files = Arrays.asList(fileArray);
        
        for (File file: files) {
            
            if ((! file.isDirectory()) &&
                (file.getName().matches("[\\S]+.tfs") ||
                 file.getName().matches("[\\S]+.tfi"))) 
            {
                System.err.println("Loading " + file);
                
                Option<CompilationUnit> candidate = 
                    ASTIO.readJavaAst(file.getCanonicalPath());
                
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
                        apis.putAll(IndexBuilder.buildApis(_candidates, file.lastModified()).apis());
                    } else if (_candidate instanceof Component) {
                        ArrayList<Component> _candidates = new ArrayList<Component>();
                        _candidates.add((Component)_candidate);
                        
                        components.putAll(IndexBuilder.buildComponents(_candidates, file.lastModified()).components());
                        
                        ArrayList<Id> _ids = new ArrayList<Id>();
                        for (Id id: _candidate.getName().getIds()) { _ids.add(new Id(new String(id.getText()))); }
                        
                        
                    } else {
                        throw new RuntimeException("The file " + file.getName() + " parsed to something other than a component or API!");
                    }   
                }                                 
            }
        }
    } 
}
