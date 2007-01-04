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

package com.sun.fortress.interpreter.rewrite;

import java.util.HashMap;
import com.sun.fortress.interpreter.nodes.Id;
import com.sun.fortress.interpreter.nodes.Node;
import com.sun.fortress.interpreter.nodes.VarRefExpr;

/**
 * Walk through the tree, replacing instances of (typically) "*trait" with
 * "*1", "*2", etc.  Disambiguate already inserted selectors where they
 * are needed.
 */
public class MakeSelfish extends Rewrite  {

    public MakeSelfish(HashMap<String, String> rewriteTo) {
        super();
        this.rewriteTo = rewriteTo;
    }

    /**
     * Variables to com.sun.fortress.interpreter.rewrite:
     * (1) method, not wrapped
     * (2) method, wrapped
     * (3) getters, setters.
     * @param node
     * @return
     */

    HashMap<String, String> rewriteTo;

    public  Node visit(Node node) {
        if (node instanceof VarRefExpr) {
            VarRefExpr vre = (VarRefExpr) node;
            Id id = vre.getVar();
            String name = id.getName();
            Object toWhat = rewriteTo.get(name);
            if (toWhat != null) {
                return new VarRefExpr(vre.getSpan(), (String) toWhat);
            }
        }
        return visitNode(node);
    }


}
