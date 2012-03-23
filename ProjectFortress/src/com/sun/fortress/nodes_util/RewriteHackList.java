/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.nodes_util;

import com.sun.fortress.nodes.*;
import java.io.Writer;
import java.util.List;

import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Voidoid;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

/**
 * If the name isn't a clue, this class exists only to make it
 * easier to add or delete items to a list during rewriting.
 * These should never be actually inserted into an AST, and are
 * a subtype of Node only to make the type of the rewrite methods
 * a little cleaner.
 */
public class RewriteHackList extends AbstractNode {

    transient private List<? extends AbstractNode> nodes = java.util.Collections.<AbstractNode>emptyList();

    public RewriteHackList() {
        super(NodeFactory.makeExprInfo(NodeFactory.interpreterSpan));
    }

    public RewriteHackList(List<? extends AbstractNode> n) {
        super(NodeFactory.makeExprInfo(NodeFactory.interpreterSpan));
        nodes = n;
    }

    public List<? extends AbstractNode> getNodes() {
        return nodes;
    }

    public <RetType> RetType accept(NodeVisitor<RetType> visitor) {
        return NI.<RetType>na("Instances of this helper class should never be spliced into an AST");
    }
    public <RetType> RetType accept(AbstractNodeVisitor<RetType> visitor) {
        return NI.<RetType>na("Instances of this helper class should never be spliced into an AST");
    }
    public void accept(NodeVisitor_void visitor) {}
    public void accept(AbstractNodeVisitor_void visitor) {}
    public int generateHashCode() { return hashCode(); }

    public String serialize() { return bug(this,"Cannot serialize RewriteHackList"); }
    public void serialize(Writer writer) { bug(this,"Cannot serialize RewriteHackList"); }
    public void walk(TreeWalker w) { bug(this,"Cannot walk RewriteHackList"); }
}
