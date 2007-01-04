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

import com.sun.fortress.interpreter.useful.MagicNumbers;


public class Span {
    public SourceLoc begin;

    public SourceLoc end;

    List<String> props;

    @Override
    public int hashCode() {
        return begin.hashCode() * MagicNumbers.p + end.hashCode()
                * MagicNumbers.a;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Span) {
            Span sp = (Span) o;
            return begin.equals(sp.begin) && end.equals(sp.end);
        }
        return false;
    }

    public Span() {
        begin = new SourceLocRats();
        end = new SourceLocRats();
        props = new ArrayList<String>();
    }

    public Span(SourceLoc b, SourceLoc e) {
        begin = b;
        end = e;
        props = new ArrayList<String>();
    }

    /**
     * @return Returns the begin.
     */
    public SourceLoc getBegin() {
        return begin;
    }

    /**
     * @return Returns the end.
     */
    public SourceLoc getEnd() {
        return end;
    }

    /**
     * @return Returns the props.
     */
    public List<String> getProps() {
        return props;
    }

    @Override
    public String toString() {
        try {
            return appendTo(new StringBuffer(), true).toString();
        } catch (IOException ex) {
            return com.sun.fortress.interpreter.useful.NI.np();
        }
    }

    public Appendable appendTo(Appendable w, boolean do_files)
            throws IOException {
        int left_col = begin.column();
        int right_col = end.column();
        boolean file_names_differ = !(begin.getFileName().equals(end
                .getFileName()));
        do_files |= file_names_differ;

        w.append(" @");
        if (do_files) {
            w.append("\"");
            // Need to add escapes to the file name
            w.append(begin.getFileName());
            w.append("\"");
            w.append(",");
        }
        w.append(String.valueOf(begin.getLine()));
        w.append(":");
        w.append(String.valueOf(left_col));
        if (file_names_differ || begin.getLine() != end.getLine()
                || left_col != right_col) {
            w.append(Printer.tilde);
            if (file_names_differ) {
                w.append(end.getFileName());
                w.append(",");
            }
            if (begin.getLine() != end.getLine()) {
                w.append(String.valueOf(end.getLine()));
                w.append(":");
            }
            w.append(String.valueOf(right_col));
        }
        return w;
    }
}
