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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.Comparator;
import java.util.List;

import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.FnName;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes_util.NodeComparator;


public interface  GenericFunctionOrMethod {

    abstract public Applicable getDef();

    static class GenericComparer implements Comparator<GenericFunctionOrMethod> {

        public int compare(GenericFunctionOrMethod arg0, GenericFunctionOrMethod arg1) {
            Applicable a0 = arg0.getDef();
            Applicable a1 = arg1.getDef();

            FnName fn0 = a0.getFnName();
            FnName fn1 = a1.getFnName();
            int x = NodeComparator.compare(fn0, fn1);
            if (x != 0)
                return x;


            List<StaticParam>  oltp0 = a0.getStaticParams();
            List<StaticParam>  oltp1 = a1.getStaticParams();

            return NodeComparator.compare(oltp0, oltp1);

        }

    }

    static final GenericComparer genComparer = new GenericComparer();

}
