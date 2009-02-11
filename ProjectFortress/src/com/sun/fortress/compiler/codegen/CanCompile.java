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

    public Boolean defaultCase(Node x) {
        throw new RuntimeException("CanCompile reverting to default case for " + x + " asclass " + x.getClass().getName());
    }

    /** Process an instance of Component. */
    public Boolean forComponent(Component x) {
        if (x.is_native()) return true;
        List<Decl> decls = x.getDecls();

        for (Decl d : decls) {
            if (!forDecl(d))
                return false;
        }
        return true;
    }

    public Boolean forBlock(Block x) {
        List<Expr> exprs = x.getExprs();
        for (Expr e : exprs) {
            if (!e.accept(this))
                return false;
        }
        return true;
    }

    public Boolean forImport(Import x) {
        // We'll figure out the import later.
        return true;
    }

    public Boolean for_RewriteFnApp(_RewriteFnApp x) {
        Expr function = x.getFunction();
        Expr argument = x.getArgument();
        return function.accept(this) && argument.accept(this);
    }

    public Boolean forFnDecl(FnDecl x) {
        Id unambiguousName = x.getUnambiguousName();
        Option<Expr> body = x.getBody();
        Option<Id> id = x.getImplementsUnambiguousName();
        if (!body.isSome()) return true;
        Expr e = body.unwrap();
        return e.accept(this);
    }

    public Boolean forStringLiteralExpr(StringLiteralExpr x) {return true;}
 
    // Very wimpy for now.  Of course we can't compile all function refs.
    public Boolean forFnRef(FnRef x) {
        List<StaticArg> _staticArgs = x.getStaticArgs();
        IdOrOp originalName = x.getOriginalName();
        List<IdOrOp> names = x.getNames();
        return true;
    }

    public Boolean forDecl(Decl x) {
        if (x instanceof FnDecl) {
            return forFnDecl((FnDecl)x);
        } else {
            return false;
        }
    }

    public Boolean forDo(Do x) {
        List<Block> fronts = x.getFronts();
        for (Block b : fronts) {
            if (!b.accept(this))
                return false;
        }
        return true;
    }

}
