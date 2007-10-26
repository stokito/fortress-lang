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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.useful.MagicNumbers;
import com.sun.fortress.useful.NI;


public class Span {
    public SourceLoc begin;

    public SourceLoc end;

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
    }

    public Span(SourceLoc b, SourceLoc e) {
        begin = b;
        end = e;
    }

    /** Span which includes both the given spans.  Assumption: they're
     * from the same file.  If this is not true, the results will be
     * unpredictable. */
    public Span(Span a, Span b) {
        if (a.getBegin().getLine() < b.getBegin().getLine()) {
            begin = a.getBegin();
        } else {
            begin = b.getBegin();
        }
        if (a.getEnd().getLine() < b.getEnd().getLine()) {
            end = b.getEnd();
        } else {
            end = a.getEnd();
        }
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

    @Override
    public String toString() {
        try {
            return appendTo(new StringBuffer(), true).toString();
        } catch (IOException ex) {
            return NI.np();
        }
    }

    public Appendable appendTo(Appendable w, boolean do_files)
            throws IOException {
        return appendTo(w,do_files,false);
    }

    public Appendable appendTo(Appendable w, boolean do_files, boolean printer)
            throws IOException {
        int left_col = begin.column();
        int right_col = end.column();
        boolean file_names_differ = !(begin.getFileName().equals(end
                .getFileName()));
        do_files |= file_names_differ;

        if (printer) w.append(" @");
        if (do_files) {
            if (printer) w.append("\"");
            // Need to add escapes to the file name
            w.append(begin.getFileName());
            if (printer) w.append("\"");
            w.append(":");
        }
        w.append(String.valueOf(begin.getLine()));
        w.append(":");
        w.append(String.valueOf(left_col));
        if (file_names_differ || begin.getLine() != end.getLine()
                || left_col != right_col) {
            w.append(Printer.tilde);
            if (file_names_differ) {
                if (printer) w.append("\"");
                // Need to add escapes to the file name
                w.append(end.getFileName());
                if (printer) w.append("\"");
                w.append(":");
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
