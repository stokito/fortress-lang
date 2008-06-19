/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

import java.io.IOException;
import java.io.Serializable;

import com.sun.fortress.useful.MagicNumbers;
import com.sun.fortress.useful.NI;


public class Span implements Serializable {
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

    private static boolean beginsEarlierThan(Span a, Span b) {
    	return (a.getBegin().getLine() < b.getBegin().getLine() ||
    			 (a.getBegin().getLine() == b.getBegin().getLine() && 
    			  a.getBegin().column() < b.getBegin().column()));
    }
    
    private static boolean endsLaterThan(Span a, Span b) {
    	return (a.getEnd().getLine() > b.getEnd().getLine() ||
    			(a.getEnd().getLine() == b.getEnd().getLine() &&
    			 a.getEnd().column() > b.getEnd().column())); 
    }
    
    /** Span which includes both the given spans.  Assumption: they're
     * from the same file.  If this is not true, the results will be
     * unpredictable. */
    public Span(Span a, Span b) {
        if ( beginsEarlierThan(a,b) ) {
            begin = a.getBegin();
        } else {
            begin = b.getBegin();
        }
        if ( endsLaterThan(a,b) ) {
            end = a.getEnd();
        } else {
            end = b.getEnd();
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
            String beginFileName;
            if (printer) beginFileName = Unprinter.enQuote(begin.getFileName());
            else beginFileName = begin.getFileName();
            w.append(beginFileName);
            if (printer) w.append("\"");
            w.append(":");
        }
        w.append(String.valueOf(begin.getLine()));
        w.append(":");
        w.append(String.valueOf(left_col));
        if (file_names_differ || begin.getLine() != end.getLine()
                || left_col != right_col) {
            w.append(printer ? Printer.tilde : "-");
            if (file_names_differ) {
                if (printer) w.append("\"");
                // Need to add escapes to the file name
                String endFileName;
                if (printer) endFileName = Unprinter.enQuote(end.getFileName());
                else endFileName = end.getFileName();                
                w.append(endFileName);
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
