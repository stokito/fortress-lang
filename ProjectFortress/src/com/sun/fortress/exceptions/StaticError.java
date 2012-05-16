/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions;

import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.Serializable;

import com.sun.fortress.nodes_util.ErrorMsgMaker;
import com.sun.fortress.useful.HasAt;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

public class StaticError extends RuntimeException implements HasAt, Comparable<StaticError> {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = -4401235748242748034L;

    public static String errorMsg(Object... messages) {
        return ErrorMsgMaker.errorMsg(messages);
    }

    protected String description;
    public String description() {
        return description;
    }

    protected Option<HasAt> location;
    public Option<HasAt> location() {
        return location;
    }

    protected StaticError() {
        this("StaticError");
    }

    protected StaticError(String description) {
        this.description = description;
        this.location = Option.<HasAt>none();
    }

    protected StaticError(String description, HasAt location) {
        this.description = description;
        this.location = Option.some(location);
    }

    public String at() {
        return location.isSome() ? location.unwrap().at() : "";
    }

    public String stringName() { return toString(); }

    public String getMessage() { return toString(); }

    public String toString() {
        // TODO: Use the indented description and remove ALL (!) explicit indentations from error messages.
        return String.format("%s:\n    %s", at(), description());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null) return false;
        if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {
            return false;
        }
        else {
            return super.equals(obj);
        }
    }

    public int compareTo(StaticError that) {
        Pattern numMatcher = Pattern.compile("(.+\\.fs[si]:)(\\d+)(?::(\\d+)(?:-(\\d+))?)?");
        Matcher m1 = numMatcher.matcher(this.toString());
        Matcher m2 = numMatcher.matcher(that.toString());

        // If either does not have the span, just do string comparison.
        if (m1.lookingAt() && m2.lookingAt()) {

            // Check that they both have the same prefix before numbers.
            int cmp = m1.group(1).compareTo(m2.group(1));
            if (cmp != 0) return cmp;

            // Compare each number.
            for (int i=2; i<=4; ++i) {
                Integer num1 = null;
                Integer num2 = null;
                try { num1 = Integer.parseInt(m1.group(i)); }
                catch (Exception e) {}
                try { num2 = Integer.parseInt(m2.group(i)); }
                catch (Exception e) {}

                if (num1 == null) {
                    if (num2 == null) continue;
                    else return -1;
                } else {
                    if (num2 == null) return 1;
                    else if (num1.equals(num2)) continue;
                    else return num1.compareTo(num2);
                }
            }
        }
        return this.toString().compareTo(that.toString());
    }

    private static class StaticErrorComparator implements Comparator<StaticError>, Serializable {
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
        return new StaticError(description, location);
    }

    public static StaticError make(String description, final File f) {
        return new StaticError(description) {
            @Override public String at() {
                return f.toString();
            }
        };
    }

    /**
     * Make a simple static error with type description "Error" and the given
     * location.
     */
    public static StaticError make(final String description, final String loc) {
        return new StaticError(description) {
            @Override public String at() { return loc; }
        };
    }

    public static StaticError make(final String description) {
        return new StaticError(description) {
            @Override public String at() { return ""; }
            @Override public String toString() { return description(); }
        };
    }

    protected static String indentAllLines(String s, String prefix) {
        return Pattern.compile("^", Pattern.MULTILINE).matcher(s).replaceAll(prefix);
    }

    protected static String indentLines(String s, String prefix) {
        return s.replace("\n", "\n" + prefix);
    }

    /** Indents every line after the first line with the `prefix` string. */
    public String toStringIndented(String prefix) {
        return indentLines(this.toString(), prefix);
    }

    /** Indents every line in the toString with the `prefix` string. */
    public String toStringIndentedAll(String prefix) {
        return indentAllLines(this.toString(), prefix);
    }

    /**
     * Indents every line after the first line in the description with the
     * `prefix` string.
     */
    public String descriptionIndented(String prefix) {
        return indentLines(this.description(), prefix);
    }

    /** Indents every line in the description with the `prefix` string. */
    public String descriptionIndentedAll(String prefix) {
        return indentAllLines(this.description(), prefix);
    }
}
