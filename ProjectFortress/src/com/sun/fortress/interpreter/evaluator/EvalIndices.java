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

package com.sun.fortress.interpreter.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.types.TypeFixedDimIndices;
import com.sun.fortress.interpreter.evaluator.types.TypeIndices;
import com.sun.fortress.interpreter.evaluator.types.TypeRange;
import com.sun.fortress.interpreter.nodes.BaseNodeVisitor;
import com.sun.fortress.interpreter.nodes.ExtentRange;
import com.sun.fortress.interpreter.nodes.FixedDim;
import com.sun.fortress.interpreter.nodes.PolyDim;


public class EvalIndices extends BaseNodeVisitor<TypeIndices> {

    EvalType evalType;

    public EvalIndices(EvalType type) {
        evalType = type;
    }
    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forFixedDim(com.sun.fortress.interpreter.nodes.FixedDim)
     */
    @Override
    public TypeIndices forFixedDim(FixedDim x) {
        List<ExtentRange> extents = x.getExtents();
        List<TypeRange> indices = new ArrayList<TypeRange>(extents.size());
        for (ExtentRange extent : extents) {
             indices.add(evalType.extentRangeToTypeRange(extent));
        }
        return new TypeFixedDimIndices(indices);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.BaseNodeVisitor#forPolyDim(com.sun.fortress.interpreter.nodes.PolyDim)
     */
    @Override
    public TypeIndices forPolyDim(PolyDim x) {
        // TODO Auto-generated method stub
        return super.forPolyDim(x);
    }



}
