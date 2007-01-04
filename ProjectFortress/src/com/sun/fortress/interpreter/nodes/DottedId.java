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

import java.util.Iterator;
import java.util.List;

import com.sun.fortress.interpreter.useful.Fn;
import com.sun.fortress.interpreter.useful.MagicNumbers;
import com.sun.fortress.interpreter.useful.Useful;


// /
public class DottedId extends FnName {
    List<String> names;

    private String cachedToString;

    // For reflective creation
    DottedId(Span span) {
        super(span);
    }

    // One way to get a DottedID
    public DottedId(Span span, String string) {
        super(span);
        names = Useful.list(string);
    }

    // One way to get a DottedID
    public DottedId(Span span, String string, List<String> tail) {
        super(span);
        names = Useful.prepend(string, tail);
    }

    // One way to get a DottedID
    public DottedId(Span span, Id s) {
        super(span);
        names = Useful.list(s.getName());
    }

    // One way to get a DottedID
    public DottedId(Span span, Id s, List<Id> ls) {
        super(span);
        names = Useful.prependMapped(s, ls,
        // fn(x) => x.getName()
                new Fn<Id, String>() {
                    @Override
                    public String apply(Id x) {
                        return x.getName();
                    }
                });
    }

    // for Visitor pattern
    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forDottedId(this);
    }

    /**
     * @return Returns the names.
     */
    public List<String> getNames() {
        return names;
    }

    /**
     * @return Returns the name.
     */
    @Override
    public String name() {
        String name;
        if (names.size() == 0) {
            throw new Error("Non-empty string is expected.");
        } else {
            name = names.get(0);
        }
        ;
        for (Iterator<String> ns = names.subList(1, names.size() - 1)
                .iterator(); ns.hasNext();) {
            name += "." + ns.next();
        }
        return name;
    }

    @Override
    public String toString() {
        if (cachedToString == null) {
            cachedToString = Useful.dottedList(names);
        }
        return cachedToString;
    }

    public int compareTo(DottedId other) {
        return com.sun.fortress.interpreter.useful.ListComparer.stringListComparer.compare(names,
                other.names);
    }

    @Override
    public boolean mandatoryEquals(Object other) {
        if (other instanceof DottedId) {
            DottedId di = (DottedId) other;
            return names.equals(di.names);
        }
        return false;
    }

    @Override
    public int mandatoryHashCode() {
        return MagicNumbers.hashList(names, MagicNumbers.D);
    }
}
