/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.env;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.values.FunctionClosure;
import com.sun.fortress.interpreter.rewrite.InterpreterNameRewriter;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.repository.ForeignJava;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.StringHashComparer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ForeignComponentWrapper extends NonApiWrapper {

    Map<String, InterpreterNameRewriter> rewrites = new BATree<String, InterpreterNameRewriter>(StringHashComparer.V);

    private final Environment e;
    private final APIName apiname;

    public ForeignComponentWrapper(APIWrapper apicw, HashMap<String, NonApiWrapper> linker, String[] implicitLibs) {
        super(apicw, linker, implicitLibs);
        // For each name in the API, need to add something to getRewrites
        e = new BetterEnvLevelZero(apicw.getCompilationUnit());
        e.setTopLevel();
        apiname = apicw.getCompilationUnit().getName();
    }

    protected Map<String, InterpreterNameRewriter> getRewrites() {
        return rewrites;
    }

    public CompilationUnit populateOne() {
        if (visitState != IMPORTED) return bug("Component wrapper " + name() + " in wrong visit state: " + visitState);

        visitState = POPULATED;


        /* Insert code here to populate the environment from foreign code.

        */


        Set<Decl> decls = ForeignJava.only.apiToStaticDecls.get(apiname);
        for (Decl d : decls) {
            if (d instanceof FnDecl) {
                // static method of a class.
                FnDecl fd = (FnDecl) d;
                try {
                    String fname = NodeUtil.nameAsMethod(fd);
                    Applicable closure = ClosureMaker.closureForTopLevelFunction(apiname, fd);
                    e.putValue(fname, new FunctionClosure(e, closure));
                }
                catch (Exception ex) {
                    return error(ex);
                }
            } else if (d instanceof TraitDecl) {
                // class/interface
                TraitDecl td = (TraitDecl) d;
            }
        }

        // Reset the non-function names from the disambiguator.

        for (APIWrapper api : exports.values()) {
            api.populateOne(this);
        }

        return null;
    }

    public Environment getEnvironment() {
        return e;
    }

}
