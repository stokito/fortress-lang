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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.sun.fortress.exceptions.shell.RepositoryError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.Pair;

import edu.rice.cs.plt.tuple.Option;

public class IOAst implements IO<APIName, CompilationUnit> {

    Fn<APIName, String> toFileName;
    
    public IOAst(Fn<APIName, String> toFileName) {
        this.toFileName = toFileName;
    }
    
    public CompilationUnit read(APIName name) throws IOException {
        String s = toFileName.apply(name);
        File f = new File(s);
        if (!f.exists()) {
            throw new FileNotFoundException(s);
        }
        Option<CompilationUnit> candidate = ASTIO.readJavaAst(s);
        if (candidate.isNone()) {
            throw new RepositoryError(
                    "Could not deserialize contents of repository file " + s);
        }
        return candidate.unwrap();
    }

    public void write(APIName name, CompilationUnit data) throws IOException {
        String s = toFileName.apply(name);
        ASTIO.writeJavaAst(data, s);
    }

    public long lastModified(APIName name) {
        String s = toFileName.apply(name);
        File f = new File(s);
        return f.lastModified();
    }
    
    

}
