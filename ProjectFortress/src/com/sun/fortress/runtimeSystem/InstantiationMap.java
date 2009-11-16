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

import java.util.Map;

public class InstantiationMap  {
    
    Map<String, String> p;

    public InstantiationMap(Map<String, String> p) {
        this.p = p;
    }
    
    
    public String getCompletely(String s) {
        // TODO will need to rewrite into type, desc, and method variants.
        if (s == null)
            return s;
        s = Naming.demangleFortressIdentifier(s);
        s = oxfordSensitiveSubstitution(s);
        return s;
    }

    public String getDesc(String s) {           
        if (s == null)
            return s;
        s = Naming.demangleFortressDescriptor(s);
        s = oxfordSensitiveSubstitution(s);
        return s;
        }


    /**
     * @param s
     * @return
     * @throws Error
     */
    public String oxfordSensitiveSubstitution(String s) throws Error {
        int l = s.length();
        
        int oxLevel = 0;
        
        for (int i = 0; i < l; i++) {
            char ch = s.charAt(i);
            
            if (Naming.GENERIC_TAGS.indexOf(ch) != -1) {
                // Found a variable
                StringBuffer b = new StringBuffer();
                int j = 0;
                while (j < l) {
                     ch = s.charAt(j);
                     j++;
                     if (ch == Naming.LEFT_OXFORD_CHAR) {
                         oxLevel++;
                         b.append(ch);
                     } else if (ch == Naming.RIGHT_OXFORD_CHAR)  {
                         oxLevel--;
                         b.append(ch);
                     } else if (Naming.GENERIC_TAGS.indexOf(ch) != -1) {
                         StringBuffer v = new StringBuffer(8);
                         v.append(ch);
                         while (j < l) {
                             ch = s.charAt(j);
                             if ('0' <= ch && ch <= '9') {
                                 v.append(ch);
                                 j++;
                             } else {
                                 break;
                             }
                         }
                         // j addresses next character or the end.
                         // v contains the variable.
                         String vs = v.toString();
                         String t = p.get(vs);
                         if (t == null)
                             throw new Error("Missing generic binding for " + vs + " map = " + p);
                         if (oxLevel == 0) {
                             b.append(t.substring(1));
                         } else {
                             b.append(t);
                         }
                         
                     } else {
                         b.append(ch);
                     }
                }
                s = b.toString();
                break;
            }
        }
        return s;
    }


    /**
     * 
     * 
     * 
     * @param name
     * @param left_oxford
     * @param right_oxford
     * @param xlation
     * @return
     * @throws Error
     */
    public static String canonicalizeStaticParameters(String name, int left_oxford,
            int right_oxford, Map<String, String> xlation) throws Error {
        String template_start = name.substring(0,left_oxford+1);
        String template_end = name.substring(right_oxford);
        // Note include trailing oxford to simplify loop termination.
        String generics = name.substring(left_oxford+1, right_oxford);
        String template_middle = "";
        int i = 1;
        while (generics.length() > 0) {
            int end = generics.indexOf(';');
            if (end == -1)
                end = generics.length();
            String tok =
                generics.substring(0, end);
            char ch = tok.charAt(0);
            String tag;
            if (ch == Naming.FOREIGN_TAG_CHAR ||
                ch == Naming.NORMAL_TAG_CHAR ||
                ch == Naming.INTERNAL_TAG_CHAR) {
                tag = Naming.YINYANG;
            } else if (ch == Naming.MUSIC_SHARP_CHAR) {
                tag = Naming.MUSIC_SHARP;
            } else if (ch == Naming.HAMMER_AND_PICK_CHAR) {
                tag = Naming.MUSIC_SHARP;
            } else {
                throw new Error("Unimplemented generic kind " + ch + " seen in instantiating classloader");
            }
            template_middle += tag+i;
            xlation.put(tag+i, tok);
    
    
            if (end == generics.length())
                break;
            template_middle += ";";
            generics = generics.substring(end+1);
            i++;
        }
        String s = template_start + template_middle + template_end;
        return s;
    }
    
    
}