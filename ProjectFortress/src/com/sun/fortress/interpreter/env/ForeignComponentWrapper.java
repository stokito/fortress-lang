/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.env;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.environments.SimpleClassLoader;
import com.sun.fortress.interpreter.evaluator.BuildApiEnvironment;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.rewrite.InterpreterNameRewriter;
import com.sun.fortress.interpreter.rewrite.RewriteInPresenceOfTypeInfoVisitor;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.repository.ForeignJava;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.NotFound;
import com.sun.fortress.useful.StringHashComparer;
import com.sun.fortress.useful.Useful;

public class ForeignComponentWrapper extends NonApiWrapper {

   Map<String, InterpreterNameRewriter> rewrites =
        new BATree<String, InterpreterNameRewriter>(StringHashComparer.V);
   
   private final Environment e;
   private final APIName apiname;
   
   public ForeignComponentWrapper(
            APIWrapper apicw, HashMap<String, NonApiWrapper> linker, String[] implicitLibs) {
        super(apicw, linker, implicitLibs);
        // For each name in the API, need to add something to getRewrites
        e = new BetterEnvLevelZero(apicw.getCompilationUnit());
        e.setTopLevel();
        apiname = apicw.getCompilationUnit().getName();
   }

    protected  Map<String, InterpreterNameRewriter> getRewrites() {
        return rewrites;
    }
    
    public CompilationUnit populateOne() {
        if (visitState != IMPORTED)
            return bug("Component wrapper " + name() + " in wrong visit state: " + visitState);

        visitState = POPULATED;

        
        /* Insert code here to populate the environment from foreign code.
            
        */
        
        
        Set<Decl> decls = ForeignJava.only.apiToStaticDecls.get(apiname);
        for (Decl d : decls) {
            if (d instanceof FnDecl) {
                // static method of a class.
                FnDecl fd = (FnDecl) d;
                try {
                    byte[] bytes = ClosureMaker.forTopLevelFunction(apiname, fd);
                } catch (Exception ex) {
                    return error(ex);
                }
            } else if (d instanceof TraitDecl) {
                // class/interface
                TraitDecl td = (TraitDecl) d;
            }
        }
        
        // Reset the non-function names from the disambiguator.
       
        for (APIWrapper api: exports.values()) {
            api.populateOne(this);
        }

        return null;
    }
    
    public Environment getEnvironment() {
        throw new Error("not implemented");
    }

}
