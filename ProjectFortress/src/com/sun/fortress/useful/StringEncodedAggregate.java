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

package com.sun.fortress.useful;

import java.util.Map;
import java.util.Set;

public class StringEncodedAggregate {
    public static Set<String> stringToSet(String s, char d, Set<String> set) {
        int a = 0;
        int l = s.length();
        while (a != -1 && a != l) {
            int b = s.indexOf(d,a);
            if (b == -1)
                throw new Error("Improperly formatted string representation of aggregate; no ending delimiter");
            set.add(s.substring(a,b));
            a = b+1;
        }
        return set;
    }

    public static StringBuffer setToString(Set<String> set, char d, StringBuffer sb) {
        for (String s : set) {
            if (s.indexOf(d) != -1)
                    throw new Error("String " + s + " in set contains delimiter " + d);
            sb.append(s);
            sb.append(d);
        }
        return sb;
    }
    public static StringBuffer setToFormattedString(Set<String> set, char d, StringBuffer sb) {
        boolean first = true;
        sb.append("\n\t\"");
        for (String s : set) {
            if (first) {
                first = false;
            } else {
                sb.append("\"+\n\t\"");
            }
            if (s.indexOf(d) != -1)
                    throw new Error("String " + s + " in set contains delimiter " + d);
            sb.append(s);
            sb.append(d);
        }
        sb.append("\"");
        return sb;
    }

    public static Map<String, String> stringToMap(String s, char d, Map<String, String> map) {
        int a = 0;
        int l = s.length();
        while (a != -1 && a != l) {
            int b = s.indexOf(d,a);
            if (b == -1)
                throw new Error("Improperly formatted string representation of aggregate; no ending delimiter");
            String key = s.substring(a,b);
            a = b+1;

            b = s.indexOf(d,a);
            if (b == -1)
                throw new Error("Improperly formatted string representation of aggregate; no ending delimiter");
            String value = s.substring(a,b);
            a = b+1;
            map.put(key, value);
        }
        return map;
    }

    public static StringBuffer mapToFormattedString(Map<String, String> map, char d, StringBuffer sb) {
        boolean first = true;
        sb.append("\n\t\"");
        for (String key : map.keySet()) {
            String value = map.get(key);
            if (first) {
                first = false;
            } else {
                sb.append("\"+\n\t\"");
            }
            if (key.indexOf(d) != -1)
                throw new Error("Key " + key + " in map contains delimiter " + d);
            if (key.indexOf(d) != -1)
                throw new Error("Value " + value + " in map contains delimiter " + d);
            sb.append(key);
            sb.append(d);
            sb.append("\"+\"");
            sb.append(value);
            sb.append(d);
        }
        sb.append("\"");
        return sb;
    }




}
