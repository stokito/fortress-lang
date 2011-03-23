/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Prints out an ISO 8601 formatted date, including milliseconds and timezone.
 * Useful for generating time-stamped log file names in scripts.
 */

public class ISO8601 {

    static final String localMilliIso8601Format = "yyyy-MM-dd'T'HH:mm:ss.SSSzzz";
    static public final DateFormat localMilliDateFormat = new SimpleDateFormat(localMilliIso8601Format);

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
