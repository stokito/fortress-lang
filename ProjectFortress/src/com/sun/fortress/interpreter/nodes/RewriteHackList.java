/*
 * Created on May 4, 2007
 *
 */
package com.sun.fortress.interpreter.nodes;

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
public class RewriteHackList extends Node {

    transient private List<Node> nodes;
    
    @Override
    <T> T acceptInner(NodeVisitor<T> v) {
        return NI.<T>na("Instances of this helper class should never be spliced into an AST");
    }

    public RewriteHackList() {
        super();
        nodes = java.util.Collections.<Node>emptyList();
    }

    public RewriteHackList(List<Node> n) {
        super();
        nodes = n;
    }

    public List<Node> getNodes() {
        return nodes;
    }
    
}
