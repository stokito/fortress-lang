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

    private static final int MAX_LEVEL = 11;
    private static int level = 0;
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
        private boolean isOn;

        Type(String name) {
            this.name = name;
            isOn = false;
        }

        public void setOn(boolean isOn) {
            this.isOn = isOn;
        }

        public String toString() {
            return name;
        }

        public boolean isOn() {
            return isOn;
        }

        public static void setAllOn() {
            for (Type type : Type.values()) {
                type.setOn(true);
            }
        }

    }

    /* Return a string specifying the debugging types suppored */
    public static String typeStrings() {
        StringBuilder buf = new StringBuilder();
        for (Type type : Type.values()) {
            buf.append(type.toString() + " ");
        }
        return buf.toString();
    }

    /* Takes in a list of options, parse the ones relavent to the Debug facility,
     * and return the rest of the options that are not used.
     */
    public static List<String> parseOptions(List<String> options) {
        Debug.level = MAX_LEVEL;
        boolean somethingIsOn = false;
        int tokenConsumed = 0;

        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);

            if (option.equalsIgnoreCase(Type.FORTRESS.toString())) {
                Type.FORTRESS.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else if (option.equalsIgnoreCase(Type.ASTGEN.toString())) {
                Type.ASTGEN.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else if (option.equalsIgnoreCase(Type.ENVGEN.toString())) {
                Type.ENVGEN.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else if (option.equalsIgnoreCase(Type.CODEGEN.toString())) {
                Type.CODEGEN.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else if (option.equalsIgnoreCase(Type.COMPILER.toString())) {
                Type.COMPILER.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else if (option.equalsIgnoreCase(Type.INTERPRETER.toString())) {
                Type.INTERPRETER.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else if (option.equalsIgnoreCase(Type.PARSER.toString())) {
                Type.PARSER.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else if (option.equalsIgnoreCase(Type.REPOSITORY.toString())) {
                Type.REPOSITORY.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else if (option.equalsIgnoreCase(Type.STACKTRACE.toString())) {
                Type.STACKTRACE.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else if (option.equalsIgnoreCase(Type.SYNTAX.toString())) {
                Type.SYNTAX.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else {
                try {
                    int l = Integer.valueOf(option);
                    Debug.level = l;
                    tokenConsumed++;
                }
                catch (NumberFormatException e) {
                    /* Doesn't recognize this option; we are done.
                    */
                    break;
                }
            }
        }

        if (somethingIsOn == false) {
            /* -debug flag is set but no debugging type specified */
            Type.setAllOn();
        }

        return options.subList(tokenConsumed, options.size());
    }

    /* Print the debugging string iff the debugging type is on
     * and no specific debugging level is set when fortress is run.
     */
    public static void debug(Type type, Object... msgs) {
        if (type.isOn() && Debug.level == MAX_LEVEL) {
            StringBuilder buf = new StringBuilder();
            buf.append("[" + type.toString() + "] ");
            for (Object s : msgs) {
                buf.append(s.toString());
            }
            debugPS.println(buf.toString());
        }
    }

    /* Print the debugging string iff the debugging type is on
     * and if the level argument is smaller than the debugging level
     * set when fortress is run.
     */
    public static void debug(Type type, int level, Object... msgs) {
        if (type.isOn() && level <= Debug.level) {
            StringBuilder buf = new StringBuilder();
            buf.append("[" + type.toString() + "] ");
            for (Object s : msgs) {
                buf.append(s.toString());
            }
            debugPS.println(buf.toString());
        }
    }

    public static boolean isOnMax() {
        return (Debug.level == MAX_LEVEL);
    }

    /* Checking whether debugging is on in the most general sense: that the debug
     * level is on at all..
     */
    public static boolean isOn() {
        return (Debug.level > 0);
    }

    /* Checking whether debugging is on for the type specified by the
     * arguments and that the debug level is either not specified when fortress
     * is run or is set to the MAX_LEVEL.
     */
    public static boolean isOnMaxFor(Type type) {
        if (type.isOn() && Debug.level == MAX_LEVEL) return true;
        return false;
    }

    /* Checking whether debugging is on for the level and
     * type specified by the arguments.
     */
    public static boolean isOnFor(int l, Type type) {
        if (type.isOn() && l <= Debug.level) return true;
        return false;
    }

    /* Doesn't seem like anyone is using it
       public static void debugArray(Type type, int level, Object[] o ) {
       if( type.isOn() && level <= Debug.level ) {
       debugPS.println( String.format("Array %s", o.toString()) );
       for (Object n : o) {
       debugPS.println( n.toString() );
       }
       debugPS.println( String.format("End of Array %s", o.toString()) );
       }
       }*/


}
