package com.sun.fortress.interpreter.nodes;
public class NullNodeVisitor<T> extends NodeVisitor<T> {
    /**
     * Make the default behavior return null, no throw an exception.
     */
    public T NI(com.sun.fortress.interpreter.useful.HasAt x, String s) {
        return null;
    }
    
}
