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

public abstract class Debug{
    private static int level = 0;

    public static void setDebug( int level ){
        Debug.level = level;
    }

    public static int getDebug(){
        return Debug.level;
    }

    public static void debug( int level, String s ){
        if ( level <= Debug.level ){
            System.out.println( s );
        }
    }

    public static void debugArray( int level, Object[] o ){
        debug( level, String.format( "Array %s", o.toString() ) );
        for ( Object n : o ){
            debug( level, n.toString() );
        }
        debug( level, String.format( "End of Array %s", o.toString() ) );
    }
}
