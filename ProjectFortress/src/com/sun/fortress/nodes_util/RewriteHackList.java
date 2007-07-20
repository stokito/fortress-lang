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
package com.sun.fortress.nodes_util;

import com.sun.fortress.nodes.*;
import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Voidoid;

/**
 * If the name isn't a clue, this class exists only to make it
 * easier to add or delete items to a list during rewriting.
 * These should never be actually inserted into an AST, and are
 * a subtype of Node only to make the type of the rewrite methods
 * a little cleaner.
 */
public class RewriteHackList extends AbstractNode {

    transient private List<AbstractNode> nodes;

    public RewriteHackList() {
        super(new Span());
        nodes = java.util.Collections.<AbstractNode>emptyList();
    }

    public RewriteHackList(List<AbstractNode> n) {
        super(new Span());
        nodes = n;
    }

    public List<AbstractNode> getNodes() {
        return nodes;
    }

    public <RetType> RetType accept(NodeVisitor<RetType> visitor) {
        return NI.<RetType>na("Instances of this helper class should never be spliced into an AST");
    }
    public void accept(NodeVisitor_void visitor) {}
    public void output(java.io.Writer writer) {}
    public void outputHelp(TabPrintWriter writer, boolean lossless) {}
    public int generateHashCode() { return hashCode(); }
    
    /** Generate a human-readable representation that can be deserialized. */
    public String serialize() { 
        return NI.<String>na("Instances of this helper class should never be serialized");
    }
    /** Generate a human-readable representation that can be deserialized. */
    public void serialize(java.io.Writer writer) {
        NI.<Voidoid>na("Instances of this helper class should never be serialized");
    }
    
    
}
