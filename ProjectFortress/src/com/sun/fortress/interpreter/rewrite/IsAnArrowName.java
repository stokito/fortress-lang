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

import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.LValue;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.TestDecl;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeUtil;

import edu.rice.cs.plt.tuple.Option;

/**
 * Visitor, returning the "arrow-ness" of a name -- that is, it
 * can be invoked, but is not an object.  This is used to distinguish
 * method invocation from function invocation.
 */
public class IsAnArrowName extends NodeAbstractVisitor<ArrowOrFunctional> {

   public final static IsAnArrowName isAnArrowName = new IsAnArrowName();
    @Override
    public ArrowOrFunctional defaultCase(Node that) {
        return ArrowOrFunctional.NEITHER;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor#forLValue(com.sun.fortress.nodes.LValue)
     */
    @Override
    public ArrowOrFunctional forLValue(LValue that) {
        return optionTypeIsArrow(that.getIdType());
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor#forFnDecl(com.sun.fortress.nodes.FnDecl)
     */
    @Override
    public ArrowOrFunctional forFnDecl(FnDecl that) {
     // Return "is a self method"
        return NodeUtil.selfParameterIndex(that) >= 0 ? ArrowOrFunctional.FUNCTIONAL : ArrowOrFunctional.NEITHER;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor#forTestDecl(com.sun.fortress.nodes.TestDecl)
     */
    @Override
    public ArrowOrFunctional forTestDecl(TestDecl that) {
        // FALSE
        return ArrowOrFunctional.NEITHER;
    }



    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor#forParam(com.sun.fortress.nodes.Param)
     */
    @Override
    public ArrowOrFunctional forParam(Param that) {
        if ( that.getVarargsType().isNone() )
            return optionTypeIsArrow(that.getIdType());
        else
            return ArrowOrFunctional.NEITHER;
    }

    private ArrowOrFunctional optionTypeIsArrow(Option<Type> ot) {
        return ot.unwrap(null) instanceof ArrowType ? ArrowOrFunctional.ARROW : ArrowOrFunctional.NEITHER;
    }



}
