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

package com.sun.fortress.interpreter.drivers;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Prints out an ISO 8601 formatted date, including milliseconds and timezone.
 * Useful for generating time-stamped log file names in scripts.
 * 
 */

public class ISO8601 {

    static String localMilliIso8601Format = "yyyy-MM-dd'T'HH:mm:ss.SSSzzz";
    static public DateFormat localMilliDateFormat =
            new SimpleDateFormat(localMilliIso8601Format);
    public static String localThen(java.util.Date d) {
        return localMilliDateFormat.format(d);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println(localThen(new Date()));
    }

}
