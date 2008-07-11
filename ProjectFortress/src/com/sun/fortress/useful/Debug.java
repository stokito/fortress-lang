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

package com.sun.fortress.useful;

import java.io.PrintStream;
import java.util.List;

public final class Debug {

    private static final int MAX_LEVEL = 99;	
    private static int level = 0;
    private static final PrintStream debugPS = System.out;

    private Debug() {
        /* prevent anyone from instantiating this class */
    }

    /* A list of possible debugging type. */
    public enum Type {
        FORTRESS("fortress"), ASTGEN("astgen"), COMPILER("compiler"), 
            INTERPRETER("interpreter"), PARSER("parser"), REPOSITORY("repository"), 
            SYNTAX("syntax");

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
            for( Type type : Type.values() ) {
                type.setOn(true);
            }
        }

    }

    /* Return a string specifying the debugging types suppored */
    public static String typeStrings() {
        String retString = "";
        for( Type type : Type.values() ) {
            retString += type.toString() + " ";
        }
        return retString;
    }

    /* Takes in a list of options, parse the ones relavent to the Debug facility, 
     * and return the rest of the options that are not used. 
     */
    public static List<String> parseOptions(List<String> options) {
        Debug.level = MAX_LEVEL;
        boolean somethingIsOn = false;
        int tokenConsumed = 0;

        for(int i=0; i<options.size(); i++) {
            String option = options.get(i);

            if( option.equals(Type.FORTRESS.toString()) ) {
                Type.FORTRESS.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else if( option.equals(Type.ASTGEN.toString()) ) {
                Type.ASTGEN.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else if( option.equals(Type.COMPILER.toString()) ) {
                Type.COMPILER.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else if( option.equals(Type.INTERPRETER.toString()) ) {
                Type.INTERPRETER.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else if( option.equals(Type.PARSER.toString()) ) {
                Type.PARSER.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else if( option.equals(Type.REPOSITORY.toString()) ) {
                Type.REPOSITORY.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else if( option.equals(Type.SYNTAX.toString()) ) {
                Type.SYNTAX.setOn(true);
                somethingIsOn = true;
                tokenConsumed++;
            } else {
                try {
                    int l = Integer.valueOf(option);
                    Debug.level = l;
                    tokenConsumed++;
                } catch(NumberFormatException e) {
                    /* Doesn't recognize this option; we are done.
                    */
                    break;
                }
            }
        }

        if( somethingIsOn == false ) {
            /* -debug flag is set but no debugging type specified */
            Type.setAllOn();
        }

        return options.subList(tokenConsumed, options.size());
    }

    /* Print the debugging string iff the debugging type is on 
     * and no specific debugging level is set when fortress is run. 
     */
    public static void debug(Type type, Object... msgs) {
        if ( type.isOn() && Debug.level == MAX_LEVEL ) {
        	String msgToPrint = "[" + type.toString() + "] ";
        	for(Object s : msgs) {
        		msgToPrint += s.toString();
        	}
            debugPS.println( msgToPrint );
        }
    }

    /* Print the debugging string iff the debugging type is on 
     * and if the level argument is smaller than the debugging level 
     * set when fortress is run.
     */
    public static void debug(Type type, int level, Object... msgs) {
        if ( type.isOn() && level <= Debug.level ) {
           	String msgToPrint = "[" + type.toString() + "] ";
        	for(Object s : msgs) {
        		msgToPrint += s.toString();
        	}
            debugPS.println( msgToPrint );
        }
    }

    /* Checking whether debugging is on in the most general sense: that the debug 
     * level is either not specified when fortress is run or is set to MAX_LEVEL.  
     */
    public static boolean isOnMax() {
        return(Debug.level == MAX_LEVEL);
    }

    /* Checking whether debugging is on for the type specified by the 
     * arguments and that the debug level is either not specified when fortress 
     * is run or is set to the MAX_LEVEL.  
     */
    public static boolean isOnMaxFor(Type type) {
        if(type.isOn() && Debug.level == MAX_LEVEL) return true;
        return false;
    }

    /* Checking whether debugging is on for the level and 
     * type specified by the arguments.  
     */
    public static boolean isOnFor(int l, Type type) {
        if(type.isOn() && l <= Debug.level ) return true;
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
