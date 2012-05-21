/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Map;
import java.util.Set;

public class StringEncodedAggregate {
    public static Set<String> stringToSet(String s, char d, Set<String> set) {
        int a = 0;
        int l = s.length();
        while (a != -1 && a != l) {
            int b = s.indexOf(d, a);
            if (b == -1)
                throw new Error("Improperly formatted string representation of aggregate; no ending delimiter");
            set.add(s.substring(a, b));
            a = b + 1;
        }
        return set;
    }

    public static StringBuilder setToString(Set<String> set, char d, StringBuilder sb) {
        for (String s : set) {
            if (s.indexOf(d) != -1) throw new Error("String " + s + " in set contains delimiter " + d);
            sb.append(s);
            sb.append(d);
        }
        return sb;
    }

    public static StringBuilder setToFormattedString(Set<String> set, char d, StringBuilder sb) {
        boolean first = true;
        sb.append("\n\t\"");
        for (String s : set) {
            if (first) {
                first = false;
            } else {
                sb.append("\"+\n\t\"");
            }
            if (s.indexOf(d) != -1) throw new Error("String " + s + " in set contains delimiter " + d);
            if (s.startsWith("\\\\u")) s = "\\u" + s.substring(3);
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
            int b = s.indexOf(d, a);
            if (b == -1)
                throw new Error("Improperly formatted string representation of aggregate; no ending delimiter");
            String key = s.substring(a, b);
            a = b + 1;

            b = s.indexOf(d, a);
            if (b == -1)
                throw new Error("Improperly formatted string representation of aggregate; no ending delimiter");
            String value = s.substring(a, b);
            a = b + 1;
            map.put(key, value);
        }
        return map;
    }

    /**
     * For small, simple encoded maps that are only used once, use this to bypass the whole map-creation song-and-dance.
     * Returns the value matching key, from the map encoded in s, assuming delimiter d.
     *
     * @param s
     * @param d
     * @param key
     * @return
     */
    public static String getFromEncodedMap(String s, char d, String key) {
        int a = 0;
        int l = s.length();
        while (a != -1 && a != l) {
            int b = s.indexOf(d, a);
            if (b == -1)
                throw new Error("Improperly formatted string representation of aggregate; no ending delimiter");
            String k = s.substring(a, b);
            a = b + 1;

            b = s.indexOf(d, a);
            if (b == -1)
                throw new Error("Improperly formatted string representation of aggregate; no ending delimiter");
            String value = s.substring(a, b);
            a = b + 1;
            if (k.equals(key)) return value;
        }
        return null;
    }

    public static StringBuilder mapToFormattedString(Map<String, String> map, char d, StringBuilder sb) {
        boolean first = true;
        sb.append("\n\t\"");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (first) {
                first = false;
            } else {
                sb.append("\"+\n\t\"");
            }
            if (key.indexOf(d) != -1) throw new Error("Key " + key + " in map contains delimiter " + d);
            if (value.indexOf(d) != -1) throw new Error("Value " + value + " in map contains delimiter " + d);
            if (key.startsWith("\\\\u")) key = "\\u" + key.substring(3);
            sb.append(key);
            sb.append(d);
            sb.append("\"+\"");
            if (value.startsWith("\\\\u")) value = "\\u" + value.substring(3);
            sb.append(value);
            sb.append(d);
        }
        sb.append("\"");
        return sb;
    }

    public static StringBuilder mapPairToString(String from1, String to1, String from2, String to2, char d) {
        return mapPairToString(from1, to1, from2, to2, d, new StringBuilder());
    }

    public static StringBuilder mapPairToString(String from1,
                                               String to1,
                                               String from2,
                                               String to2,
                                               char d,
                                               StringBuilder sb) {


        if (from1.indexOf(d) != -1) throw new Error("Key " + from1 + " in map contains delimiter " + d);
        if (to1.indexOf(d) != -1) throw new Error("Value " + to1 + " in map contains delimiter " + d);
        sb.append(from1);
        sb.append(d);

        sb.append(to1);
        sb.append(d);


        if (from2.indexOf(d) != -1) throw new Error("Key " + from2 + " in map contains delimiter " + d);
        if (to2.indexOf(d) != -1) throw new Error("Value " + to2 + " in map contains delimiter " + d);
        sb.append(from2);
        sb.append(d);

        sb.append(to2);
        sb.append(d);


        return sb;
    }

}
