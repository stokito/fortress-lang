/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.interpreter.evaluator.types.TypeFixedDimIndices;
import com.sun.fortress.interpreter.evaluator.types.TypeIndices;
import com.sun.fortress.interpreter.evaluator.types.TypeRange;
import com.sun.fortress.nodes.ExtentRange;
import com.sun.fortress.nodes.Indices;
import com.sun.fortress.nodes.NodeAbstractVisitor;

import java.util.ArrayList;
import java.util.List;


public class EvalIndices extends NodeAbstractVisitor<TypeIndices> {

    EvalType evalType;

    public EvalIndices(EvalType type) {
        evalType = type;
    }

    /* (non-Javadoc)
    * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forIndices(com.sun.fortress.interpreter.nodes.Indices)
    */
    @Override
    public TypeIndices forIndices(Indices x) {
        List<ExtentRange> extents = x.getExtents();
        List<TypeRange> indices = new ArrayList<TypeRange>(extents.size());
        for (ExtentRange extent : extents) {
            indices.add(evalType.extentRangeToTypeRange(extent));
        }
        return new TypeFixedDimIndices(indices);
    }

}
