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

/*
 * Created on Jul 20, 2007
 *
 */
package com.sun.fortress.interpreter.evaluator;

import java.util.List;

import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.TypeLatticeOps;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.ArrayType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseNatRef;
import com.sun.fortress.nodes.BaseOprRef;
import com.sun.fortress.nodes.IdType;
import com.sun.fortress.nodes.ListType;
import com.sun.fortress.nodes.MapType;
import com.sun.fortress.nodes.MatrixType;
import com.sun.fortress.nodes.NatParam;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor_void;
import com.sun.fortress.nodes.NonArrowType;
import com.sun.fortress.nodes.ParamType;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.TypeApply;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.TypeRef;
import com.sun.fortress.nodes.VarargsType;
import com.sun.fortress.nodes.VectorType;
import com.sun.fortress.nodes.VoidType;
import com.sun.fortress.nodes_util.StringMaker;
import com.sun.fortress.useful.BoundingMap;

/**
 * This visitor "refines" a bounding map by making its inferences more
 * specific where needed.  In practice, this only matters in the dual
 * table, because consumers of a map automatically see the most-specific
 * inference.  This form of "lazy clamping" seems to get better results.
 * 
 * Keep in mind that this is a feeble approximation of what we really
 * need from type inference.
 * 
 * @author chase
 */
public class MakeInferenceSpecific extends NodeAbstractVisitor_void {
    
    BoundingMap<String, FType, TypeLatticeOps> abm;
    MakeInferenceSpecific dual;
    boolean doClamp;
    
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
        throw new Error("Missing visitor for " + that.getClass());
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
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forParamType(com.sun.fortress.nodes.ParamType)
     */
    @Override
    public void forParamType(ParamType that) {
        // TODO For now, do nothing....
        // I think this will require an environment, so "that" can be looked up,
        // and its where clauses interpreted for constraints on specificity.
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forIdType(com.sun.fortress.nodes.IdType)
     */
    @Override
    public void forIdType(IdType that) {
        String s = StringMaker.fromDottedId(that.getName());
        if (doClamp) {
            FType t = abm.get(s);
            if (t != null) {
                abm.dual().put(s, t);
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
        acceptList(that.getDomain(), dual);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forListType(com.sun.fortress.nodes.ListType)
     */
    @Override
    public void forListType(ListType that) {
        that.getElement().accept(this);
   }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forMapType(com.sun.fortress.nodes.MapType)
     */
    @Override
    public void forMapType(MapType that) {
        that.getValue().accept(this);
        that.getKey().accept(dual);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forMatrixType(com.sun.fortress.nodes.MatrixType)
     */
    @Override
    public void forMatrixType(MatrixType that) {
        that.getElement().accept(this);       
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forTupleType(com.sun.fortress.nodes.TupleType)
     */
    @Override
    public void forTupleType(TupleType that) {
        acceptList(that.getElements(), this);
    }

  
    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forVectorType(com.sun.fortress.nodes.VectorType)
     */
    @Override
    public void forVectorType(VectorType that) {
        that.getElement().accept(this);       
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forVoidType(com.sun.fortress.nodes.VoidType)
     */
    @Override
    public void forVoidType(VoidType that) {
        // do nothing
    }



}
