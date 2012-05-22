/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeComparator;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.HasAt;
import edu.rice.cs.plt.tuple.Option;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;


public interface GenericFunctionOrMethod {

    IdOrOpOrAnonymousName getName();

    List<StaticParam> getStaticParams();

    List<Param> getParams();

    Option<Type> getReturnType();

    Simple_fcn typeApply(List<FType> argValues);

    Simple_fcn typeApply(List<FType> argValues, HasAt site);

    static class GenericComparer implements Comparator<GenericFunctionOrMethod>, Serializable {

        public int compare(GenericFunctionOrMethod a0, GenericFunctionOrMethod a1) {

            IdOrOpOrAnonymousName fn0 = a0.getName();
            IdOrOpOrAnonymousName fn1 = a1.getName();
            int x = NodeComparator.compare(fn0, fn1);
            if (x != 0) return x;


            List<StaticParam> oltp0 = a0.getStaticParams();
            List<StaticParam> oltp1 = a1.getStaticParams();

            return NodeComparator.compare(oltp0, oltp1);

        }

    }

    static final GenericComparer genComparer = new GenericComparer();


    static public class FunctionsAndState {
        protected static BATree<GenericFunctionOrMethod, List<FType>> symbolicStaticsByPartition =
                new BATree<GenericFunctionOrMethod, List<FType>>(genComparer);

        public static void reset() {
            symbolicStaticsByPartition = new BATree<GenericFunctionOrMethod, List<FType>>(genComparer);
        }
    }


    Environment getWithin();

}
