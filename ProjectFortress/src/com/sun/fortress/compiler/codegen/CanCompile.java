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
package com.sun.fortress.compiler.codegen;

import java.util.*;
import edu.rice.cs.plt.tuple.Option;
import com.sun.fortress.nodes.*;

/**
 * Returns Boolean.TRUE if the AST visited can be compiled.
 * "Can be compiled" depends on what we've implemented.
 */
public class CanCompile extends NodeAbstractVisitor<Boolean> {

    public CanCompile() {
        // TODO Auto-generated constructor stub
    }

    public Boolean defaultCase(Node x) {
        throw new RuntimeException("CanCompile reverting to default case for " +
                                   x + " as class " + x.getClass().getName());
    }

    public Boolean forComponent(Component x) {
        for ( Decl d : x.getDecls() ) {
            if ( ! d.accept(this) ) return false;
        }
        return true;
    }

    public Boolean forImport(Import x) {
        // We'll figure out the import later.
        return true;
    }

    public Boolean forDecl(Decl x) {
        if (x instanceof TraitDecl)
            return ((TraitDecl)x).accept(this);
        else if (x instanceof FnDecl)
            return ((FnDecl)x).accept(this);
        else
            return false;
    }

    public Boolean forTraitDecl(TraitDecl s) {
        return true;
    }

    public Boolean forFnDecl(FnDecl x) {
        Option<Expr> body = x.getBody();
        if ( ! body.isSome() ) return true;
        return body.unwrap().accept(this);
    }

    public Boolean forDo(Do x) {
        for ( Block b : x.getFronts() ) {
            if ( ! b.accept(this) ) return false;
        }
        return true;
    }

    public Boolean forBlock(Block x) {
        for ( Expr e : x.getExprs() ) {
            if ( ! e.accept(this) ) return false;
        }
        return true;
    }

    // Very wimpy for now.  Of course we can't compile all function refs.
    public Boolean forFnRef(FnRef x) {
        return true;
    }

    public Boolean for_RewriteFnApp(_RewriteFnApp x) {
        return x.getFunction().accept(this) && x.getArgument().accept(this);
    }

    public Boolean forStringLiteralExpr(StringLiteralExpr x) {
        return true;
    }

}
