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

import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeComparator;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.HasAt;

import edu.rice.cs.plt.tuple.Option;


public interface  GenericFunctionOrMethod {

    SimpleName getName();
    List<StaticParam> getStaticParams();
    List<Param> getParams();
    Option<Type> getReturnType();

    Simple_fcn typeApply(HasAt location, List<FType> argValues);
    
    static class GenericComparer implements Comparator<GenericFunctionOrMethod> {

        public int compare(GenericFunctionOrMethod a0, GenericFunctionOrMethod a1) {
            

            SimpleName fn0 = a0.getName();
            SimpleName fn1 = a1.getName();
            int x = NodeComparator.compare(fn0, fn1);
            if (x != 0)
                return x;


            List<StaticParam>  oltp0 = a0.getStaticParams();
            List<StaticParam>  oltp1 = a1.getStaticParams();

            return NodeComparator.compare(oltp0, oltp1);

        }

    }

    static final GenericComparer genComparer = new GenericComparer();
    public static final BATree<GenericFunctionOrMethod, List<FType>>
    symbolicStaticsByPartition = new BATree<GenericFunctionOrMethod, List<FType>>(genComparer);

}
