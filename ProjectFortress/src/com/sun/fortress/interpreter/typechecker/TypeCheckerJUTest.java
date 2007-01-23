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

package com.sun.fortress.interpreter.typechecker;
import junit.framework.TestCase;

import com.sun.fortress.interpreter.evaluator.Init;
import com.sun.fortress.interpreter.nodes.Expr;
import com.sun.fortress.interpreter.nodes.Span;
import com.sun.fortress.interpreter.nodes.VarRefExpr;

public class TypeCheckerJUTest extends TestCase {

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        Init.initializeEverything();
    }

    public void testForVarRefExpr() {
        try {
            Expr e = new VarRefExpr(new Span(null, null), "var_not_found");
            e.accept(new TypeChecker());
        }
        catch (Error e) {
            return;
        }
        fail("Typechecking succeeded for unbound variable.");

    }

}
