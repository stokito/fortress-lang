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

package com.sun.fortress.interpreter.nodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.interpreter.useful.HasAt;


public abstract class Node implements HasAt {
    Span span;

    List<String> props;

    private Node originalIfReplaced; // MAY CONTAIN REFERENCES TO CURRENT;
                                        // NOT A TREE.

    Node(Span span) {
        this.span = span;
        this.props = span.props;
    }

    protected Node(Node rewriteFrom) {
        this.span = rewriteFrom.span;
        this.props = rewriteFrom.span.props;
        this.originalIfReplaced = rewriteFrom;
    }

    Node() {
    }

    public void setInParentheses() {
        if (props == null) {
            props = new ArrayList<String>();
        }
        props.add("P");
    }

    /**
     *
     * @return String representation of the location, suitable for error
     *         messages.
     */
    public String at() {
        return span.begin.at(); // + ":(" + getClass().getSimpleName() + ")";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + span.begin.at();
    }

    public String stringName() {
        return getClass().getSimpleName();
    }

    /* Some static methods in scope for all the descendants of Node */

    /*
     * Note: triple-slashed comments are original OCaml code.
     */

    public static <T> T NYI(String s) {
        throw new Error("AST." + s + " NYI");
    }

    static <T> T NI(String s) {
        throw new Error("AST." + s + " intentionally not implemented");
    }

    public String dump() {
        try {
            StringBuffer sb = new StringBuffer();
            dump(sb);
            return sb.toString();
        } catch (Throwable ex) {
            return "Exception " + ex + " during dump";
        }
    }

    /**
     * @param appendable
     * @throws IOException
     */
    public void dump(Appendable appendable) throws IOException {
        Printer p = new Printer(true, true, true);
        p.dump(this, appendable, 0);
    }

    /**
     * The externally accessible "accept" method for the vistor pattern
     * @param <T>
     * @param v
     * @return
     */
    public final <T> T accept(NodeVisitor<T> v) {
        return acceptInner(v);
    }

    /**
     * The internal accept method, that all leaf nodes should implement.
     *
     * @param <T>
     * @param v
     * @return
     */
    abstract <T> T acceptInner(NodeVisitor<T> v);

    /**
     * @return Returns the span.
     */
    public Span getSpan() {
        return span;
    }

    public Node getOriginal() {
        return originalIfReplaced;
    }

    public void setOriginal(Node n) {
        n.originalIfReplaced = n;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.DefOrDecl#isAFunctionalMethod()
     */
    public int selfParameterIndex() {
        return -1;
    }
}
