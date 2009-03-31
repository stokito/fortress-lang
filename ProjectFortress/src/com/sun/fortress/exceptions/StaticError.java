/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.exceptions;

import java.util.Comparator;
import java.util.StringTokenizer;
import com.sun.fortress.nodes_util.ErrorMsgMaker;
import com.sun.fortress.useful.HasAt;

public abstract class StaticError extends RuntimeException implements HasAt, Comparable<StaticError> {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = -4401235748242748034L;

    public static String errorMsg(Object... messages) {
        return ErrorMsgMaker.errorMsg(messages);
    }

    public abstract String description();

    public abstract String at();

    public String stringName() { return toString(); }

    public String getMessage() { return toString(); }

    public String toString() {
        return at() + ":\n    " + description();
    }

    public int compareTo(StaticError that) {
        return this.getMessage().compareTo(that.getMessage());
        /*
        StringTokenizer thisTokenizer = new StringTokenizer(this.getMessage(), ":-\n\t\f\r ");
        StringTokenizer thatTokenizer = new StringTokenizer(that.getMessage(), ":-\n\t\f\r ");

        int fileComparison = thisTokenizer.nextToken().compareTo(thatTokenizer.nextToken());
        if (fileComparison != 0) { return fileComparison; }

        // Unless there is a bug in the compiler, there will be at least one token in each tokenizer,
        // indicating line information.
        String thisLineString = thisTokenizer.nextToken();
        String thatLineString = thatTokenizer.nextToken();

        // System.err.println(thisLineString + " vs " + thatLineString);

        if (thisLineString.equals("no line information")) {
            if (thatLineString.equals("no line information")) { return 0; }
            else { return -1; }
        } else if (thatLineString.equals("no line information")) { return 1; }

        int thisLine = Integer.parseInt(thisLineString);
        int thatLine = Integer.parseInt(thatLineString);


        // If there is line information, there should be column information as well.
        int thisColumn = Integer.parseInt(thisTokenizer.nextToken());
        int thatColumn = Integer.parseInt(thatTokenizer.nextToken());

        // System.err.println(thisLine + ":" + thisColumn + " vs " + thatLine + ":" + thatColumn);

        if (thisLine < thatLine) { return -1; }
        else if (thisLine == thatLine && thisColumn < thatColumn) { return -1; }
        else if (thisLine == thatLine && thisColumn == thatColumn) { return 0; }
        else if (thisLine == thatLine && thisColumn > thatColumn) { return 1; }
        else { return 1; } // thisLine > thatLine
        */
    }

    private static class StaticErrorComparator implements Comparator<StaticError> {
        public int compare(StaticError left, StaticError right) {
            // System.err.println("compare");
            return left.compareTo(right);
        }
    }
    public final static StaticErrorComparator comparator = new StaticErrorComparator();

    /**
     * Make a simple static error with the given location.
     */
    public static StaticError make(String description, HasAt location) {
        return make(description, location.at());
    }

    /**
     * Make a simple static error with type description "Error" and the given
     * location.
     */
    public static StaticError make(final String description, final String location) {
        return new StaticError() {
            /**
             * Make Eclipse happy
             */
            private static final long serialVersionUID = -7329719676960380409L;

            public String description() { return description; }
            public String at() { return location; }
        };
    }

    public static StaticError make(final String description) {
        return new StaticError() {
            /**
             * Make Eclipse happy
             */
            private static final long serialVersionUID = -7329719676960380409L;

            public String description() { return description; }
            public String at() { return ""; }
            public String toString() { return description(); }
        };
    }

}
