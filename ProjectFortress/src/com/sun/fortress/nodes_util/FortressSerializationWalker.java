/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.nodes_util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.math.BigInteger;
import java.util.HashMap;

import com.sun.fortress.nodes.LosslessStringWalker;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

public class FortressSerializationWalker extends LosslessStringWalker {

    private HashMap<Object,Integer> seen = new HashMap();
    private int seenId = -1;

    public FortressSerializationWalker(Writer writer, int tabSize) {
        super(writer, tabSize);
    }

    // Memoize the location of object o for future backward reference.
    // If object already seen, output an id record and return true;
    // otherwise record object as seen and return false.
    // NOTE: NO OVERFLOW PROTECTION on seenId; assumption
    // is the entities indexed eat so much storage that we run out of
    // memory before overflow occurs.
    protected boolean printSeen(Object o) {
        Integer b = seen.get(o);
        if (b!=null) {
            return false;
        }
        if (b!=null) {
            _out.append('^');
            _out.append(b.toString());
            return true;
        } else {
            seenId++;
            seen.put(o,Integer.valueOf(seenId));
            return false;
        }
    }

    void printSpan(Span s) {
        try {
            s.appendTo(_out,true,true);
        } catch (IOException e) {
            bug("IOException while printing span "+s, e);
        }
    }

    void printModifiers(Modifiers m) {
        _out.append('M');
        try {
            m.encodeTo(_out);
        } catch (IOException e) {
            bug("IOException while encoding modifiers "+m,e);
        }
    }

    @Override
    public void visitUnknownObject(Object o) {
        if (printSeen(o)) return;
        if (o instanceof Span) {
            printSpan((Span) o);
        } else if (o instanceof Modifiers) {
            printModifiers((Modifiers)o);
        } else if (o instanceof BigInteger) {
            _out.append('I');
            _out.append(o.toString());
        } else {
            super.visitUnknownObject(o);
        }
    }

    @Override
    public boolean visitNodeField(String name, Object v) {
        if (ASTIO.useFieldLabels) {
            return super.visitNodeField(name,v);
        } else {
            _out.startLine();
            return true;
        }
    }

}
