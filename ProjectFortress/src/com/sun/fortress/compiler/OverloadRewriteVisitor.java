/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeComparator;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.DefaultComparator;

import edu.rice.cs.plt.tuple.Option;

public class OverloadRewriteVisitor extends NodeUpdateVisitor {

    static class TypedIdOrOpList  {
        final List<IdOrOp> names;
        final  Option<Type> type;

        TypedIdOrOpList (List<IdOrOp> names, Option<Type> type) {
            this.names = names;
            this.type = type;
        }

    }


    final private Map<String, TypedIdOrOpList> overloadedFunctions = new BATree<String, TypedIdOrOpList>(DefaultComparator.Vreversed);
    final private Map<String, TypedIdOrOpList> overloadedOperators = new BATree<String, TypedIdOrOpList>(DefaultComparator.Vreversed);

    @Override
    public Node forFnRefOnly(FnRef that, ExprInfo info,
                             List<StaticArg> staticArgs, IdOrOp originalName, List<IdOrOp> fns,
                             Option<List<FunctionalRef>> overloadings,
                             List<Overloading> newOverloadings,
                             Option<Type> type_result) {
        if (fns.size() > 1) {
            Collections.<IdOrOp>sort(fns, NodeComparator.idOrOpComparer);
            StringBuffer buffer = new StringBuffer();
            buffer.append(NodeUtil.nameString(originalName));
            buffer.append('{');
            for(int i = 0; i < fns.size(); i++) {
                buffer.append(NodeUtil.nameString(fns.get(i)));
                if (i < (fns.size() - 1)) {
                    buffer.append(',');
                }
            }
            buffer.append('}');
            String overloadingName = buffer.toString();
            if (!overloadedFunctions.containsKey(overloadingName)) {
                overloadedFunctions.put(overloadingName, new TypedIdOrOpList(fns, that.getInfo().getExprType()));
            }
            IdOrOp overloadingId = NodeFactory.makeId(NodeUtil.getSpan(that), overloadingName);
            fns = Collections.unmodifiableList(Collections.singletonList(overloadingId));
        }
        return super.forFnRefOnly(that, info, staticArgs , originalName, fns,
                                  overloadings, Collections.<Overloading>emptyList(), type_result);
    }


    @Override
    public Node forOpRefOnly(OpRef that, ExprInfo info,
                             List<StaticArg> staticArgs, IdOrOp originalName, List<IdOrOp> ops,
                             Option<List<FunctionalRef>> overloadings,
                             List<Overloading> newOverloadings,
                             Option<Type> type_result) {
        if (ops.size() > 1) {
            Collections.<IdOrOp>sort(ops, NodeComparator.idOrOpComparer);
            StringBuffer buffer = new StringBuffer();
            buffer.append(NodeUtil.nameString(originalName));
            buffer.append('{');
            for(int i = 0; i < ops.size(); i++) {
                buffer.append(NodeUtil.nameString(ops.get(i)));
                if (i < (ops.size() - 1)) {
                    buffer.append(',');
                }
            }
            buffer.append('}');
            String overloadingName = buffer.toString();
            if (!overloadedOperators.containsKey(overloadingName)) {
                overloadedOperators.put(overloadingName, new TypedIdOrOpList(ops, that.getInfo().getExprType()));
            }
            IdOrOp overloadingOp = NodeFactory.makeOp(NodeFactory.makeSpan(that), overloadingName);
            ops = Collections.unmodifiableList(Collections.singletonList(overloadingOp));
        }
        return super.forOpRefOnly(that, info, staticArgs, originalName, ops,
                                  overloadings, newOverloadings, type_result);
    }


    public Map<String, TypedIdOrOpList> getOverloadedFunctions() {
        return overloadedFunctions;
    }

    public Map<String, TypedIdOrOpList> getOverloadedOperators() {
        return overloadedOperators;
    }


}
