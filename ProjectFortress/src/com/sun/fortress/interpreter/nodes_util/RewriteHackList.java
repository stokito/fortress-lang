/*
 * Created on May 4, 2007
 *
 */
package com.sun.fortress.interpreter.nodes_util;

import com.sun.fortress.interpreter.nodes.*;
import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.interpreter.useful.NI;

/**
 * If the name isn't a clue, this class exists only to make it
 * easier to add or delete items to a list during rewriting.
 * These should never be actually inserted into an AST, and are
 * a subtype of Node only to make the type of the rewrite methods
 * a little cleaner.
 *
 * @author chase
 */
public class RewriteHackList extends AbstractNode {

    transient private List<AbstractNode> nodes;

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return NI.<T>na("Instances of this helper class should never be spliced into an AST");
    }

    public RewriteHackList() {
        nodes = java.util.Collections.<AbstractNode>emptyList();
    }

    public RewriteHackList(List<AbstractNode> n) {
        nodes = n;
    }

    public List<AbstractNode> getNodes() {
        return nodes;
    }

}
