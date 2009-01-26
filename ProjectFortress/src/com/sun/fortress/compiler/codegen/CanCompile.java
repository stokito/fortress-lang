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
package com.sun.fortress.compiler.codegen;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes.Node;
import edu.rice.cs.plt.tuple.Option;
import java.util.*;

/**
 * Returns Boolean.TRUE if the AST visited can be compiled.
 * "Can be compiled" depends on what we've implemented.
 * 
 */
public class CanCompile extends NodeAbstractVisitor<Boolean> {

    public CanCompile() {
        // TODO Auto-generated constructor stub
    }

    public Boolean defaultCase(Node that) {
        return Boolean.FALSE;
    }

    public Boolean forFnDecl(FnDecl x) {
        Option<Expr> b = x.getBody();
        if (!b.isSome())
            return true;
        else 
            return false;
    }

    public Boolean forComponent(Component x) {
        List<Decl> decls = x.getDecls();
        Boolean result = true;
        for (Decl d : decls) {
            if (!forDecl(d))
                result = false;
        }
        return result;
    }

    public Boolean forDecl(Decl x) {
        if (x instanceof FnDecl) {
            return forFnDecl((FnDecl)x);
        } else {
            return false;
        }
    }
}
