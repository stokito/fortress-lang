/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
package com.sun.fortress.runtimeSystem;

import java.util.HashMap;
import java.util.Map;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.nodes_util.NodeFactory;


public class Naming {

    public final static String FOREIGN_TAG = "\u2615"; // hot beverage == JAVA
    public final static String NORMAL_TAG = "\u263a"; // smiley face == normal case.
    public final static String INTERNAL_TAG = "\u26a0"; // warning sign -- internal use only
    
    public final static String ENVELOPE = "\u2709";
    public final static String SNOWMAN = "\u2603"; // for empty tuple, sigh.
    
    public final static char FOREIGN_TAG_CHAR = FOREIGN_TAG.charAt(0);
    public final static char NORMAL_TAG_CHAR = NORMAL_TAG.charAt(0);
    public final static char INTERNAL_TAG_CHAR = INTERNAL_TAG.charAt(0);
  
    public final static char LEFT_OXFORD_CHAR = '\u27e6';
    public final static char RIGHT_OXFORD_CHAR = '\u27e7';
    public final static String LEFT_OXFORD = "\u27e6";
    public final static String RIGHT_OXFORD = "\u27e7";
    
    public final static String COMPILER_BUILTIN = "CompilerBuiltin";
    
    public static final String runtimeValues = "com/sun/fortress/compiler/runtimeValues/";
    
    /**
     * Java descriptors for (boxed) Fortress types, INCLUDING leading L and trailing ;
     */
    static Map<String, String> specialFortressDescriptors = new HashMap<String, String>();
    /**
     * Java descriptors for (boxed) Fortress types, WITHOUT leading L and trailing ;
     */
    static Map<String, String> specialFortressTypes = new HashMap<String, String>();

    
    static void bl(String lib, String ft, String cl) {
        cl = runtimeValues + cl;
        specialFortressDescriptors.put(lib+ft, "L" + cl + ";");
        specialFortressTypes.put(lib+ft, cl );
    }

    static {
        /*
         * This code is duplicated, mostly, in runtime Naming.java,
         * except that it deals only in strings.
         */
        bl(COMPILER_BUILTIN, ".Boolean", "FBoolean");
        bl(COMPILER_BUILTIN, ".Char", "FChar");
        bl(COMPILER_BUILTIN, ".RR32", "FRR32");
        bl(COMPILER_BUILTIN, ".RR64", "FRR64");
        bl(COMPILER_BUILTIN, ".ZZ32", "FZZ32");
        bl(COMPILER_BUILTIN, ".ZZ64", "FZZ64");
        bl(COMPILER_BUILTIN, ".String", "FString");
        bl("", SNOWMAN, "FVoid");
    }

    
    
    /**
     * (Symbolic Freedom) Dangerous characters should not appear in JVM identifiers
     */
    private static final String SF_DANGEROUS = "/.;$<>][:";
    /**
     * (Symbolic Freedom) Escape characters have a translation if they appear following
     * a backslash.
     * Note omitted special case -- leading \= is empty string.
     * Note note \ by itself is not escaped unless it is followed by
     * one of the escape characters.
     */
    private static final String    SF_ESCAPES = "|,?%^_}{!-";
    /**
     * (Symbolic Freedom) Translations of escapes, in corresponding order.
     */
    private static final String SF_TRANSLATES = "/.;$<>][:\\";

    private static final String SF_FIRST_ESCAPES = SF_ESCAPES + "=";


    public static String javaDescForTaggedFortressType(String ft) {
        char ch = ft.charAt(0);
        ft = ft.substring(1);
        String tx = specialFortressDescriptors.get(ft);
        if (tx != null)
            return tx;
        if (ch == NORMAL_TAG_CHAR) {
            return "L" + deDot(ft) + ";";
        } else if (ch == INTERNAL_TAG_CHAR) {
            return "Lfortress/" + deDot(ft) + ";";
        } else if (ch == FOREIGN_TAG_CHAR) {
            throw new Error("Haven't figured out JVM xlation of foreign type " + ft);
        }
        throw new Error("Bad fortress naming scheme tag (unicode " + Integer.toHexString(ch) +
                    ") on fortress type " + ft);
    }
    
    public static String deDot(String s) {
        return s.replace(".", "/");
    }
    
    public static String deMangle(String s) {
        if (s.length() < 2 || s.charAt(0) != '\\')
            return s;
        if (s.charAt(1) == '=')
            s = s.substring(2);
        int l = s.length();
        if (l == 0)
            return s;
        StringBuffer sb = new StringBuffer();
        boolean sawback = false;
        for (int i = 0; i < l; i++) {
            char ch = s.charAt(i);
            if (sawback) {
                int j = SF_ESCAPES.indexOf(ch);
                if (j != -1) {
                    sb.append(SF_TRANSLATES.charAt(j));
                    sawback = false;
                    continue;
                }
                sb.append('\\');
            }
            if (ch == '\\') {
                sawback = true;
            } else {
                sawback = false;
                sb.append(ch);
            }
        }
        if (sawback)
            sb.append('\\');

        return sb.toString();
    }



    public static boolean likelyMangled(String s) {
        if (s.length() < 2) return false;
        if (s.charAt(0) != '\\') return false;
        // if (-1 == NF_FIRST_ESCAPES.indexOf(s.charAt(1))) return false;
        return true;
    }



    public static boolean isMangled(String s) {
        int l = s.length();
        if (l < 2) return false;
        if (s.charAt(0) != '\\') return false;
        if (s.charAt(1) == '=') return true;
        return isSubstringMangled(s, l);
    }



    /**
     * @param s
     * @param l
     * @return
     */
    private static boolean isSubstringMangled(String s, int l) {
        for (int i = 0; i < l-1; i++) {
            char ch = s.charAt(i);
            if (ch == '\\' && -1 != SF_ESCAPES.indexOf(s.charAt(i+1)))
                return true;
        }
        return false;
    }



    /**
     * Concatenates s1 and s2, preserving valid-mangling property.
     * 
     * @param s1 Validly mangled ("naming freedom") JVM identifier
     * @param s2 Validly mangled ("naming freedom") JVM identifier
     * @return concatenation of s1 and s2, validly mangled if s1 and s2 were validly mangled.
     */
    public static String catMangled(String s1, String s2) {
        int l1 = s1.length();
        int l2 = s2.length();
        
        // Strictly speaking, empty strings are illegal inputs.
        if (l1 == 0) return s2;
        if (l2 == 0) return s1;
        
        boolean ms1 = likelyMangled(s1);
        boolean ms2 = likelyMangled(s2);
        
        if (ms1) {
            // Fancy way to encode the empty string.
            if (l1 == 2 && s1.charAt(1) == '=')
                return s2;
            
            if (ms2) {
                // ms2 begins with \, hence no accidental escapes
                char ch1 = s2.charAt(1);
                if (ch1 == '=') {
                    // remove embedded \=
                    return Naming.catMangledCheckingJoint(s1, s2.substring(2));
                } else if (ch1 == '-' && l2 > 2 && s2.charAt(2) == '=') {
                    // replace non-first \-= with \=
                    return s1 + "\\" + s2.substring(2);
                } else{
                  return s1 + s2;
                }
            } else {
                return Naming.catMangledCheckingJoint(s1, s2);
            }
        } else if (ms2) {
            char ch1 = s2.charAt(1);
 
            // If s2 is truly mangled, then prepend \= to concatenation.
                if (ch1 == '=') {
                    if (l2 == 2) {
                        // Fancy way to encode the empty string.
                        return s1;
                    }
                    // definitely mangled, but embedded \=
                    return "\\=" + Naming.catMangledCheckingJoint(s1, s2.substring(2));
                } else if (ch1 == '-' && l2 > 2 && s2.charAt(2) == '=') {
                    // Embedded \-= goes away.
                    String s2sub = s2.substring(3);
                    if (isSubstringMangled(s2sub, l2-3)) {
                        // Joints ok
                        return "\\=" + s1 + "\\=" + s2sub;
                    } else {
                        // Joints ok
                        return s1 + "\\=" + s2sub;
                    }
                } else if (isMangled(s2)) {
                    // mangled for some other reason.
                    return "\\=" + s1 + s2;
                } else {
                    // Joints ok (s2 begins with \\)
                    return s1 + s2;
                }
        } else {
            return Naming.catMangledCheckingJoint(s1, s2);
        }
    }



    /**
     * @param s1
     * @param s2
     * @return
     */
    public static String catMangledCheckingJoint(String s1, String s2) {
        int l1 = s1.length();
        int l2 = s2.length();
        
        if (l2 == 0)
            return s1;
        
        if (s1.endsWith("\\") &&
                -1 != (l1 == 1 ? SF_FIRST_ESCAPES : SF_ESCAPES).indexOf(s2.charAt(0))) {
            // must escape trailing \
            return s1.charAt(0) != '\\'
                ? "\\=" + s1 + "-" + s2
                : s1 + "-" + s2;
            
        } else {
            return s1 + s2;
        }
    }



    public static String catMangled(String s1, String s2, String s3) {
        return catMangled(catMangled(s1,s2), s3);
    }



    /**
         * Convert a string identifier into something that will be legal in a
         * JVM.
         *
         * http://blogs.sun.com/jrose/entry/symbolic_freedom_in_the_vm
         * Dangerous characters are the union of all characters forbidden
         * or otherwise restricted by the JVM specification, plus their mates,
         * if they are brackets.
    
         * @param identifier
         * @return
         */
        public static String mangleIdentifier(String identifier) {
    
            /* This is not quite right; accidental escapes are those
             * where the backslash is followed by one of |,?%^_{}!
             */
            
            // 1. In each accidental escape, replace the backslash with an escape sequence (\-)
            StringBuffer mangledStringBuffer = null;
            String mangledString = identifier;
            
            int l = identifier.length();
            if (l == 0)
                return "\\=";
            
            for (int j = 0; j < l-1; j++) {
                char ch = identifier.charAt(j);
                if (ch == '\\') {
                    ch = identifier.charAt(j+1);
                    if (-1 != SF_ESCAPES.indexOf(ch) || j == 0 && ch == '=') {
                        // found one, do the translation.
                        mangledStringBuffer = new StringBuffer(mangledString.substring(0, j+1));
                        mangledStringBuffer.append('-');
                        mangledStringBuffer.append(ch);
                         for (int i = j+2; i < l-1; i++) {
                            ch = identifier.charAt(i);
                            mangledStringBuffer.append(ch);
                            if (ch == '\\') {
                                ch = identifier.charAt(i+1);
                                if (-1 !=  SF_ESCAPES.indexOf(ch)) {
                                    // found one, do the translation.
                                    mangledStringBuffer.append('-');
                                }
                            }
                            
                        }
                         if (j + 2 < l)
                             mangledStringBuffer.append(identifier.charAt(l-1));
                        mangledString = mangledStringBuffer.toString();
                        break;
                    }
                }
            }
            
    //        if (mangledString.startsWith("\\=")) {
    //            mangledString = "\\-=" + mangledString.substring(2);
    //        }
    
            // 2. Replace each dangerous character with an escape sequence (\| for /, etc.)
    
            mangledString = mangledString.replaceAll("/", "\\\\|");
            mangledString = mangledString.replaceAll("\\.", "\\\\,");
            mangledString = mangledString.replaceAll(";", "\\\\?");
            mangledString = mangledString.replaceAll("\\$", "\\\\%");
            mangledString = mangledString.replaceAll("<", "\\\\^");
            mangledString = mangledString.replaceAll(">", "\\\\_");
            mangledString = mangledString.replaceAll("\\[", "\\\\{");
            mangledString = mangledString.replaceAll("\\]", "\\\\}");
            mangledString = mangledString.replaceAll(":", "\\\\!");
    
            // Actually, this is NOT ALLOWED.
    //        // Non-standard name-mangling convention.  Michael Spiegel 6/16/2008
    //        mangledString = mangledString.replaceAll("\\ ", "\\\\~");
    
            // 3. If the first two steps introduced any change, <em>and</em> if the
            // string does not already begin with a backslash, prepend a null prefix (\=)
            if (!mangledString.equals(identifier))
                if (!(mangledString.charAt(0) == '\\')) 
                mangledString = "\\=" + mangledString;
            
            return mangledString;
        }
}
