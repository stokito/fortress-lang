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

import com.sun.fortress.nodes.AbsFnDecl;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.NormalParam;
import com.sun.fortress.nodes.TestDecl;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarargsParam;
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
     * @see com.sun.fortress.nodes.NodeAbstractVisitor#forLValueBind(com.sun.fortress.nodes.LValueBind)
     */
    @Override
    public ArrowOrFunctional forLValueBind(LValueBind that) {
        return optionTypeIsArrow(that.getType());
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor#forAbsFnDecl(com.sun.fortress.nodes.AbsFnDecl)
     */
    @Override
    public ArrowOrFunctional forAbsFnDecl(AbsFnDecl that) {
        // Return "is a self method"
        return NodeUtil.selfParameterIndex(that) >= 0  ? ArrowOrFunctional.FUNCTIONAL : ArrowOrFunctional.NEITHER;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor#forFnDef(com.sun.fortress.nodes.FnDef)
     */
    @Override
    public ArrowOrFunctional forFnDef(FnDef that) {
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
     * @see com.sun.fortress.nodes.NodeAbstractVisitor#forNormalParam(com.sun.fortress.nodes.NormalParam)
     */
    @Override
    public ArrowOrFunctional forNormalParam(NormalParam that) {
        return optionTypeIsArrow(that.getType());
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor#forVarargsParam(com.sun.fortress.nodes.VarargsParam)
     */
    @Override
    public ArrowOrFunctional forVarargsParam(VarargsParam that) {
        return ArrowOrFunctional.NEITHER;
    }

    private ArrowOrFunctional optionTypeIsArrow(Option<Type> ot) {
        return ot.unwrap(null) instanceof ArrowType ? ArrowOrFunctional.ARROW : ArrowOrFunctional.NEITHER;
    }



}