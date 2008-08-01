package com.sun.fortress.compiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeComparator;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;

public class OverloadRewriteVisitor extends NodeUpdateVisitor {

    final private Map<String, List<Id>> overloadedFunctions = new HashMap<String, List<Id>>();
    final private Map<String, List<OpName>> overloadedOperators = new HashMap<String, List<OpName>>();
    
    @Override
    public Node forFnRefOnly(FnRef that, Id originalName, List<Id> fns, 
            List<StaticArg> staticArgs) {
        if (fns.size() > 1) {
            Collections.<Id>sort(fns, NodeComparator.idComparer);
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
                overloadedFunctions.put(overloadingName, fns);
            }
            Id overloadingId = NodeFactory.makeId(overloadingName);
            fns = Collections.unmodifiableList(Collections.singletonList(overloadingId));
        }
        return super.forFnRefOnly(that, originalName, fns, staticArgs);
    }
    

    @Override    
    public Node forOpRefOnly(OpRef that, OpName originalName, List<OpName> ops,
            List<StaticArg> staticArgs) {
        if (ops.size() > 1) {
            Collections.<OpName>sort(ops, NodeComparator.opNameComparer);
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
                overloadedOperators.put(overloadingName, ops);
            }
            OpName overloadingOpName = NodeFactory.makeOp(overloadingName);
            ops = Collections.unmodifiableList(Collections.singletonList(overloadingOpName));
        }
        return super.forOpRefOnly(that, originalName, ops, staticArgs);
    }

    
    public Map<String, List<Id>> getOverloadedFunctions() { 
        return overloadedFunctions;
    }

    public Map<String, List<OpName>> getOverloadedOperators() {
        return overloadedOperators;
    }

    
}
