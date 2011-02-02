/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nodes_util;

import java.io.IOException;

import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.tools.FortressAstToConcrete;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

public class UIDObject implements HasAt {

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     *
     * This is mostly for testing purposes, but it also allows
     * a more deterministic replay than Object.hashCode().
     */
    @Override
    public int hashCode() {
        return (int) uid ^ (int) (uid >>> 32);
    }

    /**
     * If none of the nodes override, this provides a somewhat terser toString
     * than the generated formatting.
     */
    @Override
    public String toString() {
        if (this instanceof AbstractNode)
            return ErrorMsgMaker.makeErrorMsg((AbstractNode) this);
        return super.toString();
    }

    public String at() {
        if (this instanceof AbstractNode)
            return NodeUtil.getSpan((AbstractNode) this).toString();
        return bug(this, "Class " + this.getClass().toString() + " needs to a case in UIDObject.at()");
    }

    public String stringName() {
        return NodeUtil.stringName((Node)this);
    }

    public String toStringVerbose() {
        Printer p = new Printer();
        StringBuilder sb = new StringBuilder();
        try {
            p.dump(this, sb);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return sb.toString();
    }

    public String toStringReadable() {
        if (this instanceof Node) {
            FortressAstToConcrete fatc = new FortressAstToConcrete(false, false);
            return this.toString()+"\n"+((Node)this).accept( fatc );
        } else {
            return this.toStringVerbose();
        }
    }

    static private Object lock = new Object();
    static private long seedUID = 0x7b546b0e12fd2559L;
    static private long prevUID = seedUID;
    private transient final long uid;

    public UIDObject() {
        uid = next();
    }

    public final long getUID() {
        return uid;
    }

    /* LFSR generating 63-bit residues */
    private long next() {
        synchronized (lock)
        {
        long x = prevUID;
        x = x + x;
        if (x < 0)
            x = x ^ 0xb1463923a7c109cdL;
        prevUID = x;
        return x;
        }
    }
}
