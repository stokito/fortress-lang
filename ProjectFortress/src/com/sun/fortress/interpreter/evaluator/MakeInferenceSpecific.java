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

package com.sun.fortress.interpreter.evaluator;

import java.util.List;

import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.TypeLatticeOps;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.ArrayType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.IdType;
import com.sun.fortress.nodes.MatrixType;
import com.sun.fortress.nodes.NatParam;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor_void;
import com.sun.fortress.nodes.NonArrowType;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes.ArgType;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarargsType;
import com.sun.fortress.nodes.VoidType;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.BoundingMap;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

/**
 * This visitor "refines" a bounding map by making its inferences more
 * specific where needed.  In practice, this only matters in the dual
 * table, because consumers of a map automatically see the most-specific
 * inference.  This form of "lazy clamping" seems to get better results.
 *
 * Keep in mind that this is a feeble approximation of what we really
 * need from type inference.
 */
public class MakeInferenceSpecific extends NodeAbstractVisitor_void {

    BoundingMap<String, FType, TypeLatticeOps> abm;
    MakeInferenceSpecific dual;
    boolean doClamp; // TRUE for the dual map.

    MakeInferenceSpecific(BoundingMap<String, FType, TypeLatticeOps> abm) {
        this.abm = abm;
        dual = new MakeInferenceSpecific (abm.dual(), this);
    }

    private MakeInferenceSpecific(BoundingMap<String, FType, TypeLatticeOps> abm,
            MakeInferenceSpecific dual) {
        this.abm = abm;
        this.dual = dual;
        this.doClamp = true;
    }

    public void defaultCase(Node that) {
        bug(that, "Missing visitor for " + that.getClass());
    }

     protected void acceptList(List<? extends AbstractNode> nodes, NodeAbstractVisitor_void visitor) {
        for (AbstractNode node: nodes)
            node.accept(visitor);

    }

     /* (non-Javadoc)
      * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forVarargsType(com.sun.fortress.nodes.VarargsType)
      */
     @Override
     public void forVarargsType(VarargsType that) {
         that.getType().accept(this);
     }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forInstantiatedType(com.sun.fortress.nodes.InstantiatedType)
     */
    @Override
    public void forInstantiatedType(InstantiatedType that) {
        // TODO For now, do nothing....
        // I think this will require an environment, so "that" can be looked up,
        // and its where clauses interpreted for constraints on specificity.
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forIdType(com.sun.fortress.nodes.IdType)
     */
    @Override
    public void forIdType(IdType that) {
        String s = NodeUtil.nameString(that.getName());
        if (doClamp) { // True if COVARIANT
            FType t = abm.get(s); // originally, most general type.
            if (t != null) {
                abm.dual().put(s, t); // clamp result to most general.
            }
        }
    }



    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forArrayType(com.sun.fortress.nodes.ArrayType)
     */
    @Override
    public void forArrayType(ArrayType that) {
        that.getElement().accept(this);
        // Skip indices, not yet dealing with numerical constraints.
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forArrowType(com.sun.fortress.nodes.ArrowType)
     */
    @Override
    public void forArrowType(ArrowType that) {
        that.getRange().accept(this);
        that.getDomain().accept(dual);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forMatrixType(com.sun.fortress.nodes.MatrixType)
     */
    @Override
    public void forMatrixType(MatrixType that) {
        that.getElement().accept(this);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forArgType(com.sun.fortress.nodes.ArgType)
     */
    @Override
    public void forArgType(ArgType that) {
        // TODO: Handle ArgTypes with varargs and keywords
        acceptList(that.getElements(), this);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forTupleType(com.sun.fortress.nodes.TupleType)
     */
    @Override
    public void forTupleType(TupleType that) {
        // TODO: Handle TupleTypes with varargs and keywords
        acceptList(that.getElements(), this);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forVoidType(com.sun.fortress.nodes.VoidType)
     */
    @Override
    public void forVoidType(VoidType that) {
        // do nothing
    }



}
