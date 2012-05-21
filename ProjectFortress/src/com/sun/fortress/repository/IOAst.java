/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.repository;

import com.sun.fortress.compiler.PathTaggedApiName;
import com.sun.fortress.exceptions.shell.RepositoryError;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.useful.Fn;
import edu.rice.cs.plt.tuple.Option;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class IOAst implements IO<PathTaggedApiName, CompilationUnit> {

    Fn<PathTaggedApiName, String> toFileName;

    public IOAst(Fn<PathTaggedApiName, String> toFileName) {
        this.toFileName = toFileName;
    }

    public CompilationUnit read(PathTaggedApiName name) throws IOException {
        String s = toFileName.apply(name);
        File f = new File(s);
        if (!f.exists()) {
            throw new FileNotFoundException(s);
        }
        Option<CompilationUnit> candidate = ASTIO.readJavaAst(s);
        if (candidate.isNone()) {
            throw new RepositoryError("Could not deserialize contents of repository file " + s);
        }
        return candidate.unwrap();
    }

    public void write(PathTaggedApiName name, CompilationUnit data) throws IOException {
        String s = toFileName.apply(name);
        ASTIO.writeJavaAst(data, s);
    }

    public long lastModified(PathTaggedApiName name) {
        String s = toFileName.apply(name);
        File f = new File(s);
        return f.lastModified();
    }


}
