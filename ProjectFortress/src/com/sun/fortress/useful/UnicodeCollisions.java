/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class UnicodeCollisions {
    // 0028;LEFT PARENTHESIS;Ps;0;ON;;;;;Y;OPENING PARENTHESIS;;;;
    //     1                2  3 4  56789 a                   bcde

    String[] fields;


    String name() {
        return fields[1];
    }

    String otherName() {
        return fields.length > 10 ? fields[10] : "";
    }

    public String toString() {
        String s = otherName();
        return s.length() > 0 ? name() + "(" + otherName() + ")" : name();
    }

    public boolean simpleEquals(UnicodeCollisions that) {
        return this == that || this.name().equals(that.otherName()) || that.name().equals(this.otherName());
    }

    UnicodeCollisions(String line) {
        fields = line.split(";");
    }

    void addUnderXForm(Map<String, UnicodeCollisions> m, XForm x) {
        if ("<control>".equals(name())) return;
        addUnderXForm(name(), m, x);
        addUnderXForm(otherName(), m, x);
    }

    void addUnderXForm(String s, Map<String, UnicodeCollisions> m, XForm x) {

        if (s.length() == 0) return;
        String t = x.translate(s);
        UnicodeCollisions o = (UnicodeCollisions) m.get(t);
        if (o == null) m.put(t, this);
        else if (!o.simpleEquals(this)) {
            System.err.println("Name collision of " + this + " and " + o + " on name " + t);
        }
    }

    static abstract class XForm {
        abstract String translate(String x);
    }

    static void forAll(ArrayList<UnicodeCollisions> l, XForm x, Map<String, UnicodeCollisions> m) {
        for (int i = 0; i < l.size(); i++) {
            l.get(i).addUnderXForm(m, x);
        }
    }

    static void forAllRemoving(ArrayList<UnicodeCollisions> l, final String s, Map<String, UnicodeCollisions> m) {
        XForm x = new XForm() {
            String translate(String str) {
                return str.replace(s, "");
            }
        };
        for (int i = 0; i < l.size(); i++) {
            l.get(i).addUnderXForm(m, x);
        }
    }

    public static void main(String[] args) throws IOException {
        BufferedReader is = Useful.utf8BufferedFileReader(args[0]);
        ArrayList<UnicodeCollisions> chars = new ArrayList<UnicodeCollisions>();
        HashMap<String, UnicodeCollisions> h = new HashMap<String, UnicodeCollisions>();
        try {
            String l = is.readLine();
            while (l != null) {
                chars.add(new UnicodeCollisions(l));
                l = is.readLine();
            }
        }
        finally {
            is.close();
        }

        forAll(chars, new XForm() {
            String translate(String x) {
                return x;
            }
        }, h);

        forAllRemoving(chars, "LETTER ", h);
        forAllRemoving(chars, "DIGIT ", h);
        forAllRemoving(chars, "RADICAL ", h);
        forAllRemoving(chars, "NUMERAL ", h);
        forAllRemoving(chars, "WITH ", h);
        forAllRemoving(chars, " OPERATOR", h);
        forAllRemoving(chars, " SIGN", h);

        HashSet<String> tokens = new HashSet<String>();

        {
            for (int i = 0; i < chars.size(); i++) {
                UnicodeCollisions e = (UnicodeCollisions) chars.get(i);
                String s = e.name();
                StringTokenizer st = new StringTokenizer(s);
                while (st.hasMoreTokens()) {
                    String tok = st.nextToken();
                    tokens.add(tok);
                }
            }
            //            for (int j = 10; j > 4 ; j--) {
            //                Iterator it = tokens.iterator();
            //                HashMap abbrevs = new HashMap();
            //                while (it.hasNext()) {
            //                    String s = (String) it.next();
            //                    int len = Math.min(j, s.length());
            //                    String ss = s.substring(0,len);
            //                    HashSet hs = (HashSet) abbrevs.get(ss);
            //                    if (hs == null) {
            //                        hs = new HashSet();
            //                        abbrevs.put(ss, hs);
            //                    }
            //                    hs.add(s);
            //                }
            //                it = abbrevs.entrySet().iterator();
            //                while (it.hasNext()) {
            //                    Entry e = (Entry) it.next();
            //                    Set es = (Set) e.getValue();
            //                    if (es.size() > 1) {
            //                        String ek = (String) e.getKey();
            //                        if (es.size() > 5)
            //                            System.err.println("Prefix " + ek + " has " + es.size() + " collisions.");
            //                        else
            //                            System.err.println("Prefix " + ek + " has collisions " + es);
            //                    }
            //                }
            //            }

        }
        Set<String> keys = h.keySet();

        for (String s : keys) {
            checkNonHex(s);
        }
    }

    /**
     * @param s
     */
    private static boolean checkNonHex(String s) {
        boolean nonHexSeen = false;
        for (int j = 0; j < s.length(); j++) {
            char c = s.charAt(j);
            if ("abcdefABCDEF0123456789".indexOf(c) == -1) {
                nonHexSeen = true;
                break;
            }
        }
        if (!nonHexSeen) {
            System.err.println("String '" + s + "' looks like a hex number");
        }
        return nonHexSeen;
    }
}
