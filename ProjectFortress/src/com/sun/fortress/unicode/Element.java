/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.unicode;

import java.io.*;
import java.util.*;

public class Element {
    // 0028;LEFT PARENTHESIS;Ps;0;ON;;;;;Y;OPENING PARENTHESIS;;;;
    //     1                2  3 4  56789 a                   bcde

    static final Element collision = new Element("0000;SOME COLLISION;;;;;;;;;;;;;");
    static ArrayList<XForm> xforms = new ArrayList<XForm>();

    String[] fields;

    HashSet<String> aliases = new HashSet<String>();
    HashSet<String> spaceFreeAliases = new HashSet<String>();

    String name() {
        return fields[1];
    }

    String shortName() {
        String shortest = null;
        int u = Integer.parseInt(unicode(), 16);
        if (u <= 127) {
            char[] ca = new char[1];
            ca[0] = (char) u;
            return new String(ca);
        }
        for (String s : spaceFreeAliases) {
            if (shortest == null) shortest = s;
                // Note the need for a well-defined "shortest"
            else if (s.length() < shortest.length() || s.length() == shortest.length() && s.compareTo(shortest) < 0)
                shortest = s;
        }
        return shortest;
    }

    String quotedShortNameOrOXUnicode() {
        int u = Integer.parseInt(unicode(), 16);
        if (u <= 127 && u != '\\' && u != '\"') {
            char[] ca = new char[3];
            ca[0] = '\"';
            ca[1] = (char) u;
            ca[2] = '\"';
            return new String(ca);
        }
        return "0x" + unicode();
    }

    String escapedShortName() {
        String s = shortName();
        s = s.replace("\\", "\\\\");
        s = s.replace("\"", "\\\"");
        return s;
    }

    String otherName() {
        return fields.length > 10 ? fields[10] : "";
    }

    boolean isCollision() {
        return this == collision;
    }

    public String toString() {
        String s = otherName();
        return unicode() + " " + (s.length() > 0 ? name() + "(" + otherName() + ")" : name());
    }

    public boolean simpleEquals(Element that) {
        return this == that || this.name().equals(that.otherName()) || that.name().equals(this.otherName());
    }

    Element(String line) {
        fields = line.split(";");
    }

    void addUnderXForm(Map<String, Element> m, XForm x) {
        if ("<control>".equals(name())) return;
        addUnderXForm(name(), m, x);
        addUnderXForm(otherName(), m, x);
    }

    void addUnderXForm(String s, Map<String, Element> m, XForm x) {

        if (s.length() == 0) return;
        String t = x.translate(s);
        if (t != null) addAlias(s, m, x, t, false);
    }

    protected final static String[] keyswordsTriggeringErrorIgnore =
            {"YOGH", "CYRILLIC", "GEORGIAN", "HANGUL", "GUJARATI"};

    static void Systemerrprintln(String s) {
        for (String k : keyswordsTriggeringErrorIgnore) {
            if (s.indexOf(k) != -1) return;
        }
        System.err.println(s);
    }

    public void addAlias(String original,
                         Map<String, Element> m,
                         NamedXForm translatorToBlame,
                         String translated,
                         boolean becauseISaySo) {
        Element o = (Element) m.get(translated);
        String[] synonyms = new String[]{
                "MINUS SIGN", "ASTERISK OPERATOR", "DOT OPERATOR", "VECTOR OR CROSS PRODUCT"
        };
        List<String> synonymOperators = new LinkedList<String>(java.util.Arrays.asList(synonyms));

        if (o == null) {
            m.put(translated, this);
            this.addName(translated);
        } else if (synonymOperators.contains(o.name())) {
            m.put(translated, this);
            this.addName(translated);
        } else if (o == this) {
            // No-op transform, re-add of same.
        } else if (o.name().equals(translated)) {
            // Existing entry owns it.
            Systemerrprintln("Name collision of " + this + " and owner " + o + " on name " + translated);
            collision.addName(translated);
            translatorToBlame.addName(translated, original);
        } else if (this.name().equals(translated)) {
            // Unusual, but just in case.
            this.addName(translated);
        } else if (o.otherName().equals(translated)) {
            // Existing entry owns it.
            Systemerrprintln("Name collision of " + this + " and owner (other) " + o + " on name " + translated);
            collision.addName(translated);
            translatorToBlame.addName(translated, original);
        } else if (this.otherName().equals(translated) && !o.otherName().equals(translated)) {
            m.put(translated, this);
            this.addName(translated);
            o.removeName(translated);
            collision.addName(translated);
            translatorToBlame.addName(translated, original);
            Systemerrprintln("Name collision of owner (other) " + this + " and " + o + " on name " + translated);
        } else {
            if (becauseISaySo) {
                m.put(translated, this);
                this.addName(translated);
                Systemerrprintln("Name collision of (defined owner) " + this + " and " + o + " on name " + translated);
            } else {
                m.put(translated, collision);
                Systemerrprintln("Name collision of " + this + " and " + o + " on name " + translated);
            }
            collision.addName(translated);
            translatorToBlame.addName(translated, original);
            o.removeName(translated);
        }
    }

    private void addName(String s) {
        aliases.add(s);
        if (s.indexOf(' ') == -1) {
            if (s.indexOf('-') == -1) spaceFreeAliases.add(s);
            else if (!s.matches(".*[a-zA-Z].*")) spaceFreeAliases.add(s);
        }
    }

    private void removeName(String s) {
        aliases.remove(s);
        spaceFreeAliases.remove(s);
    }

    static void forAll(ArrayList<Element> l, XForm x, Map<String, Element> m) {
        for (int i = 0; i < l.size(); i++) {
            l.get(i).addUnderXForm(m, x);
        }
    }

    public static void main(String[] args) throws IOException {
        String unicodeFile = args[0];
        ArrayList<Element> chars = readUnicodeFile(unicodeFile);

        HashMap<String, Element> h = generateAbbreviated(chars);

        //        HashSet tokens = new HashSet();
        //
        //        {
        //            for (int i = 0; i < chars.size(); i++) {
        //                Element e = (Element) chars.get(i);
        //                String s = e.name();
        //                StringTokenizer st = new StringTokenizer(s);
        //                while (st.hasMoreTokens()) {
        //                    String tok = st.nextToken();
        //                    tokens.add(tok);
        //                }
        //            }
        //        }
        Set<String> keys = h.keySet();

        System.err.println(collision.aliases.toString());
        for (XForm x : xforms) {
            System.err.println("Transform " + x);
            for (String y : x.aliases) {
                System.err.println(y);
            }
            System.err.println();
        }

        for (String s : keys) {
            checkNonHex(s);
        }
    }

    /**
     * @param chars
     * @return
     */
    static HashMap<String, Element> generateAbbreviated(ArrayList<Element> chars) {
        HashMap<String, Element> h = new HashMap<String, Element>();

        forAll(chars, new XForm("") {
            String translate(String x) {
                return x;
            }
        }, h);

        final String[] abbrevs = {"LETTER ", "DIGIT ", "NUMERAL ", "RADICAL ", "WITH ", "SIGN "};

        for (int i = 0; i < abbrevs.length; i++) {
            final String iLoveSuperfluousTemporaries = abbrevs[i];
            XForm xform = new XForm("Remove " + iLoveSuperfluousTemporaries) {
                String translate(String x) {
                    String t = x.replace(iLoveSuperfluousTemporaries, "");
                    if (t.equals(x)) return null;
                    return t;
                }
            };
            xforms.add(xform);
            forAll(chars, xform, h);
        }

        XForm xform = new XForm("Remove OF") {
            String translate(String x) {
                String t = x.replace(" OF", "");
                if (t.equals(x)) return null;
                return t;
            }
        };
        xforms.add(xform);
        forAll(chars, xform, h);

        return h;
    }

    /**
     * @param unicodeFile
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    static ArrayList<Element> readUnicodeFile(String unicodeFile) throws FileNotFoundException, IOException {
        BufferedReader is = filenameToBufferedReader(unicodeFile);
        ArrayList<Element> chars = new ArrayList<Element>();
        String l = is.readLine();
        while (l != null) {
            chars.add(new Element(l));
            l = is.readLine();
        }
        return chars;
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

    public String unicode() {
        return fields[0];
    }

    // Coped from Useful to simplify tool management.
    public static BufferedReader filenameToBufferedReader(String filename) throws FileNotFoundException {
        return new BufferedReader(new FileReader(filename));
    }

    public static BufferedWriter filenameToBufferedWriter(String filename) throws IOException {
        return new BufferedWriter(new FileWriter(filename));
    }
}
