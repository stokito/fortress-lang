/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeComparator;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.OprUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.AnyListComparer;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.DefaultComparator;
import com.sun.fortress.useful.F;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;

public class OverloadRewriteVisitor extends NodeUpdateVisitor {

    static class TypedIdOrOpList  {
        final List<IdOrOp> names;
        final  Option<Type> type;
        final String name;
        final Span span;

        TypedIdOrOpList (Span span, String name, List<IdOrOp> names, Option<Type> type) {
            this.names = names;
            this.type = type;
            this.name = name;
            this.span = span;
        }
    }


    final private static F<Overloading, IdOrOp> overloadingToIdOrOp = new F<Overloading, IdOrOp>() {
        @Override
        public IdOrOp apply(Overloading x) {
            return x.getUnambiguousName();
        }
    };

    public final static Comparator<Overloading> overloadComparator = new Comparator<Overloading>() {

        @Override
        public int compare(Overloading o1, Overloading o2) {
            int rc = NodeComparator.idOrOpComparer.compare(o1.getUnambiguousName(), o2.getUnambiguousName());
            if (rc != 0)
                return rc;
            Option<ArrowType> ot1 = o1.getType();
            Option<ArrowType> ot2 = o2.getType();
            if (ot1.isNone()) {
                if (ot2.isNone()) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (ot2.isNone()) {
                return 1;
            } else {
                ArrowType t1 = ot1.unwrap();
                ArrowType t2 = ot2.unwrap();
                return NodeComparator.typeComparer.compare(t1, t2);
            }
        }
    };

    public final static Comparator<List<? extends Overloading>> overloadListComparator = new AnyListComparer<Overloading>(overloadComparator);



    final private Map<List<? extends Overloading>, TypedIdOrOpList> overloadedFunctions = new BATree<List<? extends Overloading>, TypedIdOrOpList>(overloadListComparator);
    final private Map<List<? extends Overloading>, TypedIdOrOpList> overloadedOperators = new BATree<List<? extends Overloading>, TypedIdOrOpList>(overloadListComparator);

    final private boolean forInterpreter;

    public OverloadRewriteVisitor(boolean forInterpreter) {
        this.forInterpreter = forInterpreter;
    }


    @Override
    public Node forFnRefOnly(FnRef that, ExprInfo info,
                             List<StaticArg> staticArgs, IdOrOp originalName, List<IdOrOp> fns,
                             List<Overloading> interp_overloadings,
                             List<Overloading> newOverloadings,
                             Option<Type> type_result,
                             Option<Type> schema_result) {

        List<Overloading> the_overloads = forInterpreter ? interp_overloadings : newOverloadings;

        Collections.<Overloading>sort(the_overloads, overloadComparator);
        fns = Useful.applyToAll(the_overloads, overloadingToIdOrOp);

        if (the_overloads.size() > 1) {
            // Collections.<IdOrOp>sort(fns, NodeComparator.idOrOpComparer);
            StringBuilder buffer = new StringBuilder();
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
            // Type mismatch!!
            //if (!overloadedFunctions.containsKey(overloadingName)) {
                overloadedFunctions.put(the_overloads,
                                        new TypedIdOrOpList(NodeUtil.getSpan(that),
                                                            overloadingName, fns,
                                                            that.getInfo().getExprType()));
            //}
            IdOrOp overloadingId = NodeFactory.makeId(NodeUtil.getSpan(that), overloadingName);
            fns = Collections.singletonList(overloadingId);
        } else if (the_overloads.size() == 1 ){
            Overloading the_overload = the_overloads.get(0);
            IdOrOp thename = the_overload.getUnambiguousName();
            Option<ArrowType> oat = the_overload.getSchema();
            if (oat.isSome())
                schema_result = Option.<Type>wrap(oat.unwrap());
            fns = Collections.singletonList(thename);
        } else {
            fns = Collections.singletonList(originalName);
        }

        return super.forFnRefOnly(that, info, staticArgs , originalName, fns,
                Collections.<Overloading>emptyList(), Collections.<Overloading>emptyList(), type_result, schema_result);
    }


    @Override
    public Node forOpRefOnly(OpRef that, ExprInfo info,
                             List<StaticArg> staticArgs, IdOrOp originalName, List<IdOrOp> ops,
                             List<Overloading>  interp_overloadings,
                             List<Overloading> newOverloadings,
                             Option<Type> type_result,
                             Option<Type> schema_result) {
        Op originalOp = (Op)originalName;

        List<Overloading> the_overloads = forInterpreter ? interp_overloadings : newOverloadings;

        Collections.<Overloading>sort(the_overloads, overloadComparator);
        ops = Useful.applyToAll(the_overloads, overloadingToIdOrOp);
        List<IdOrOp> newOps = new ArrayList<IdOrOp>();
        List<Overloading> newNewOverloadings = new ArrayList<Overloading>();

        for (int i = 0; i < ops.size(); i++) {
            Op op_i = (Op)ops.get(i);
            if (OprUtil.equal(op_i.getFixity(), originalOp.getFixity())) {
                newOps.add(op_i);
                newNewOverloadings.add(the_overloads.get(i));
            }
        }

        ops = newOps;
        the_overloads = newNewOverloadings;

        if (the_overloads.size() > 1) {
            // Collections.<IdOrOp>sort(ops, NodeComparator.idOrOpComparer);

            StringBuilder buffer = new StringBuilder();
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
            // Type mismatch!!
            //if (!overloadedOperators.containsKey(overloadingName)) {
                overloadedOperators.put(the_overloads, new TypedIdOrOpList(NodeUtil.getSpan(that),
                                                                           overloadingName, ops,
                                                                           that.getInfo().getExprType()));
            //}
            IdOrOp overloadingOp = NodeFactory.makeOp(NodeUtil.getSpan(that), overloadingName);
            ops = Collections.singletonList(overloadingOp);
        } else if (the_overloads.size() == 1 ){
            Overloading the_overload = the_overloads.get(0);
            IdOrOp thename = the_overload.getUnambiguousName();
            Option<ArrowType> oat = the_overload.getSchema();
            if (oat.isSome())
                schema_result = Option.<Type>wrap(oat.unwrap());
            ops = Collections.singletonList(thename);
        } else {
            ops = Collections.singletonList(originalName);
        }
        return super.forOpRefOnly(that, info, staticArgs, originalName, ops,
                                  // interp_overloadings, newOverloadings,
                                  Collections.<Overloading>emptyList(), Collections.<Overloading>emptyList(),
                                  type_result, schema_result);
    }


    public Map<List<? extends Overloading>, TypedIdOrOpList> getOverloadedFunctions() {
        return overloadedFunctions;
    }

    public Map<List<? extends Overloading>, TypedIdOrOpList> getOverloadedOperators() {
        return overloadedOperators;
    }


}
