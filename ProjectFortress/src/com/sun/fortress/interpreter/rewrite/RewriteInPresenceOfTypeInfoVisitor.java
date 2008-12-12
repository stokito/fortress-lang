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

package com.sun.fortress.interpreter.rewrite;

import java.util.List;
import java.util.Collections;

import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes._RewriteFnRef;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import static com.sun.fortress.exceptions.InterpreterBug.bug;

public class RewriteInPresenceOfTypeInfoVisitor extends NodeUpdateVisitor {

    public static RewriteInPresenceOfTypeInfoVisitor Only = new RewriteInPresenceOfTypeInfoVisitor();

    public Node visit(Node n) {
        return n.accept(this);
    }

    @Override
    public Node forFnRef(FnRef fr) {

        List<IdOrOp> fns = fr.getNames(); // ignore this for now.
        List<StaticArg> sargs = fr.getStaticArgs();
        IdOrOp idn = fns.get(0);
        if ( ! (idn instanceof Id) ) {
            bug(idn, "The name field of FnRef should be Id.");
        }
        Id id = (Id)idn;

        if (sargs.size() > 0)
            return (new _RewriteFnRef(fr.getSpan(),
                                      fr.isParenthesized(),
                                      fr.getExprType(),
                                      ExprFactory.makeVarRef(id),
                                      sargs)).accept(this);

        else {
            //throw new Error("Unexpected FnRef " + fr);
            return (ExprFactory.makeVarRef(id.getSpan(), fr.isParenthesized(),
                                           fr.getExprType(), id)).accept(this);
        }

    }

    @Override
    public Node forTraitType(TraitType it) {
        if (it.getArgs().size() == 0) {
            return (NodeFactory.makeVarType(it.getSpan(), it.getName())).accept(this);
        }
        return super.forTraitType(it);
    }

    private RewriteInPresenceOfTypeInfoVisitor() {

    }

}
