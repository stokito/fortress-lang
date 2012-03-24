/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.io.PrintStream;
import java.util.List;


public final class Debug {

    public static final int MAX_LEVEL = 11;
    private static int defaultLevel = MAX_LEVEL;
    private static final PrintStream debugPS = System.out;

    private Debug() {
        /* prevent anyone from instantiating this class */
    }

    /* A list of possible debugging type. */
    public enum Type {
        FORTRESS("fortress"), ASTGEN("astgen"), CODEGEN("codegen"),
        ENVGEN("envgen"), COMPILER("compiler"), INTERPRETER("interpreter"),
        PARSER("parser"), REPOSITORY("repository"), STACKTRACE("stacktrace"), SYNTAX("syntax");

        private final String name;
        private int level;

        Type(String name) {
            this.name = name;
            level = 0;
        }

        public void setOn(int level) {
            this.level = level;
        }

        public String toString() {
            return name;
        }

        public boolean isOn(int level) {
            return this.level >= level;
        }

        public boolean matchAndSet(String s, int level) {
            if (s.equalsIgnoreCase(name)) {
                this.level = level;
                return true;
            }
            return false;
        }
        
    }

    /** Return a string specifying the debugging types suppored */
    public static String typeStrings() {
        StringBuilder buf = new StringBuilder();
        for (Type type : Type.values()) {
            buf.append(type.toString() + " ");
        }
        return buf.toString();
    }

    /**
     * Check an option against all the options available, setting the level
     * of the first one that matches.  Returns true iff there is a match.
     */
    public static boolean matchOption(String s, int level) {
        for (Type type : Type.values()) {
            if (type.matchAndSet(s, level))
                return true;
        }
        return false;
    }
    
    /** Takes in a list of options, parse the ones relevant to the Debug facility,
     * and return the rest of the options that are not used.
     */
    public static List<String> parseOptions(List<String> options) {
        int tokenConsumed = 0;

        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            int eq_at = option.indexOf('=');
            int opt_default_level = defaultLevel;
            if (eq_at != -1) {
                String num = option.substring(eq_at+1);
                try {
                    opt_default_level = Integer.valueOf(num);
                    option = option.substring(0, eq_at);
                } catch (NumberFormatException ex) {
                    // Do nothing, it could be a valid input, not a debugging option.
                }
            }
            if (matchOption(option, opt_default_level))
                tokenConsumed++;
            else {
                try {
                    int l = Integer.valueOf(option);
                    defaultLevel = l;
                    tokenConsumed++;
                }
                catch (NumberFormatException e) {
                    /* Doesn't recognize this option; we are done.
                    */
                    break;
                }
            }
        }

        return options.subList(tokenConsumed, options.size());
    }

    /** Print the debugging string iff the debugging type is on
     * and if the level argument is smaller than the debugging level
     * set when fortress is run.
     */
    public static void debug(Type type, int level, Object... msgs) {
        if (type.isOn(level)) {
            StringBuilder buf = new StringBuilder();
            buf.append("[" + type.toString() + "] " + java.text.DateFormat.getTimeInstance().format(new java.util.Date()) + " ");
	    //            buf.append("[" + type.toString() + "] ");
            for (Object s : msgs) {
                buf.append(s.toString());
            }
            debugPS.println(buf.toString());
        }
    }

    /** Checks whether a stack trace is desired (in general).
     */
    public static boolean stackTraceOn() {
        return Debug.isOnFor(1, Debug.Type.STACKTRACE);
    }


    /** Checks whether debugging is on for the level and
     * type specified by the arguments.
     */
    public static boolean isOnFor(int l, Type type) {
        if (type.isOn(l) ) return true;
        return false;
    }


}
