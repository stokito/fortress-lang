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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InstantiationMap  {
    
    private final static InstantiationMap EMPTY = new InstantiationMap(new HashMap<String, String>());
    
    Map<String, String> p;
    Map<String, String> q;

    public InstantiationMap(Map<String, String> p) {
        this.p = p;
        // Copy tags to front, makes it easier to do tag replacement.
        q = new HashMap<String,String>();
        for (Map.Entry<String, String> e : p.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            q.put(v.substring(0,1)+k, v);
            
        }
    }
    
    
    public String getName(String s) {
        // TODO will need to rewrite into type, desc, and method variants.
        if (s == null)
            return s;
        s = Naming.demangleFortressIdentifier(s);
        StringBuffer b = new StringBuffer();
        maybeBareVar(s, 0, b, false);
        
        s = b.toString();
       
        return s;
    }
    
    public String getTypeName(String s) {
        // TODO will need to rewrite into type, desc, and method variants.
        String t = s;
        if (s == null)
            return s;
        s = Naming.demangleFortressIdentifier(s);
        StringBuffer b = new StringBuffer();
        maybeBareVar(s, 0, b, false);
        
        s =  b.toString();

        return s;
    }

    public String getUnmangledTypeDesc(String s) {
        // TODO will need to rewrite into type, desc, and method variants.
        String t = s;
        if (s == null)
            return s;
        StringBuffer b = new StringBuffer();
        maybeVarInTypeDesc(s, 0, b);
        
        s =  b.toString();

        return s;
    }

    public String getFieldDesc(String s) {           
        if (s == null)
            return s;        
        s = Naming.demangleFortressDescriptor(s);

        StringBuffer b = new StringBuffer();
        maybeVarInTypeDesc(s, 0, b);

        s = b.toString();
       
        return s;
        }

    public String getMethodDesc(String s) {           
        if (s == null)
            return s;
        s = Naming.demangleFortressDescriptor(s);
        
        StringBuffer b = new StringBuffer();
        maybeVarInMethodSig(s, 0, b);

        s = b.toString();
       
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

    
    // Looking for  Lvariable; in method signatures,
    // Looking for  LOXvariable;variableROX as part of types
    
    /* states:
     *
     *  1) scanning potential variable
     *    LOX -> push suffix; 
     *    / -> 
     *    (a) in Oxfords
     *      ; -> check; (1a)
     *      ROX -> check; pop suffix;
     *    (b) in L;
     *      ; -> check; (3)
     *    (c) bare
     *    
     *    
     *  2) scanning known non-variable
     *    (a) in Oxfords
     *    (b) in L;
     *    (c) bare
     *    
     *  3) scanning in Parens
     *     BCDIJSZV -> (3)
     *     L -> 1, suffix=b
     *     
     *  4) scanning after Parens
     *     BCDIJSZV -> (3)
     *     L -> 1, suffix=b
     *     
     *  Outer context is bare/var/L;
     *  Possibly in ()
     *  Then some number of nested Oxfords.
     *     
     */
    
    /**
     * Come here after seeing a left oxford.  Process characters until an
     * unnested right Oxford is seen.  At semicolons, check to see if the
     * previous string is a variable, if it has not been disqualified.
     */
    int maybeVarInOxfords(String input, int begin, StringBuffer accum) {
         int at = maybeBareVar(input, begin, accum, true);
         char ch = input.charAt(at++);
         
         if (ch == ';' || ch == '=') {
             accum.append(ch);
             /* This recursion will never be very deep, so it does not
              * need a tail-call, though golly, that would be nice.
              */
             return maybeVarInOxfords(input, at, accum);
         } else if (ch == Naming.RIGHT_OXFORD_CHAR) {
             accum.append(ch);
             return at;
         } else {
             throw new Error();
         }
     }
    
    /**
     * Come here after seeing L where a type signature is expected.
     * Process characters until an unnested ';' is seen.  Consume the ';' ,
     * return the index of the next character.
     * 
     * If none of the processed characters is disqualifying, check 
     * for replacement.
     * 
     * @param input
     * @param at
     * @param accum
     */
     int maybeVarInLSemi(String input, int begin, StringBuffer accum) {
        int at = maybeBareVar(input, begin, accum, false);
        char ch = input.charAt(at++);
        if (ch != ';')
            throw new Error("Expected semicolon, saw " + ch +
                    " instead at index " + (at-1) + " in " + input) ;
        
        accum.append(';');
        
        return at;
    }
    
     /**
      * 
      * @param ch
      * @return
      */
     private static boolean nonVar(char ch) {
        return ch == '/' || ch == '$' || ch == ';' || 
               ch == Naming.LEFT_OXFORD_CHAR || ch == Naming.RIGHT_OXFORD_CHAR;
    }

    /**
     * 
     * @param input
     * @param begin
     * @param accum
     * @param inOxfords
     * @return
     */
    int maybeBareVar(String input, int begin, StringBuffer accum, boolean inOxfords) {
        int at = begin;
        char ch = input.charAt(at++);
        boolean maybeVar = true;
        boolean eol = false;
        boolean disabled = false;
        
        while (ch != ';' && ch != Naming.RIGHT_OXFORD_CHAR) {
            if (ch == Naming.HEAVY_X_CHAR)
                disabled = true;
            
            if (ch == '=') {
                if (maybeVar)
                    accum.append(input.substring(begin, at-1));
                maybeVar = false;
                break;
            } else if (!maybeVar) {
                accum.append(ch);
            } else if (nonVar(ch)) {
                maybeVar = false;
                accum.append(input.substring(begin, at));
            } 
            
            if (ch == Naming.LEFT_OXFORD_CHAR) {
                at = (disabled ? EMPTY: this).maybeVarInOxfords(input, at, accum);
            }
            
            if (at >= input.length()) {
                eol = true;
                break;
            }
            ch = input.charAt(at++);
        }
        
        at = eol ? at : at - 1;
        
        if (maybeVar) {
            String s = input.substring(begin, at);
            if (ch != '=') {
                String t = p.get(s); // (inOxfords ? q : p).get(s);
                if (t != null) {
                    accum.append(t);
                    //accum.append(inOxfords ? t : t.substring(1));
                } else {
                    accum.append(s);
                }
            } else {
                accum.append(s);
            }
        }
        return at;
    }
    
     int maybeVarInMethodSig(String input, int begin, StringBuffer accum) {
         int at = begin;
         char ch = input.charAt(at++);
         // Begin with "("
         // Expect signature letters, followed by ")"
         // Expect one more signature letter
         if (ch != '(') {
             throw new Error("Signature does not begin with '(', sig="+input);
         }
         accum.append(ch);
         ch = input.charAt(at++);
         while (ch != ')') {
             accum.append(ch);
             if (ch == 'L') {
                 at = maybeVarInLSemi(input, at, accum);
             }
             ch = input.charAt(at++);
         }
         accum.append(ch);
         
         return maybeVarInTypeDesc(input, at, accum);
     }


    /**
     * @param input
     * @param accum
     * @param at
     * @return
     */
    private int maybeVarInTypeDesc(String input, int at, StringBuffer accum) {
        char ch;
        ch = input.charAt(at++);
         accum.append(ch);
         if (ch == 'L') {
             at = maybeVarInLSemi(input, at, accum);
         }
         return at;
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
            int right_oxford, Map<String, String> xlation, ArrayList<String> sargs) throws Error {
        String template_start = name.substring(0,left_oxford+1);
        String template_end = name.substring(right_oxford);
        // Note include trailing oxford to simplify loop termination.
        String generics = name.substring(left_oxford+1, right_oxford);
        //String template_middle = "";
        int i = 1;
        while (generics.length() > 0) {
            int end = generics.indexOf(';');
            if (end == -1)
                end = generics.length();
            String tok =
                generics.substring(0, end);
//            char ch = tok.charAt(0);
//            String tag;
//            if (ch == Naming.FOREIGN_TAG_CHAR ||
//                ch == Naming.NORMAL_TAG_CHAR ||
//                ch == Naming.INTERNAL_TAG_CHAR) {
//                tag = Naming.YINYANG;
//            } else if (ch == Naming.MUSIC_SHARP_CHAR) {
//                tag = Naming.MUSIC_SHARP;
//            } else if (ch == Naming.HAMMER_AND_PICK_CHAR) {
//                tag = Naming.MUSIC_SHARP;
//            } else {
//                throw new Error("Unimplemented generic kind " + ch + " seen in instantiating classloader");
//            }
//            template_middle += tag+i;
//            xlation.put(tag+i, tok);
            sargs.add(tok);
    
    
            if (end == generics.length())
                break;
//            template_middle += ";";
            generics = generics.substring(end+1);
            i++;
        }
        String s = template_start +
                   // template_middle +
                   template_end;
        return s;
    }
    
    
}