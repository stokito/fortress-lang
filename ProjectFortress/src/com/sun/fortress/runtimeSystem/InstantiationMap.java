/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.runtimeSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstantiationMap  {
    private final static InstantiationMap EMPTY = new InstantiationMap(new HashMap<String, String>());
    
    /* Controls expansion of tuple types occurring in */
    private final static boolean DEFAULT_TUPLE_FLATTENING = false;
    
    Map<String, String> p;

    public InstantiationMap(Map<String, String> p) {
        this.p = p;
    }
    
    public String getMethodName(String s) {
        // TODO will need to rewrite into type, desc, and method variants.
        if (s == null)
            return s;
        s = Naming.demangleFortressIdentifier(s);
        StringBuilder b = new StringBuilder();
        maybeBareMethodName(s, b);
        
        String s2 = b.toString();
       
        return s2;
    }
    
    public String getName(String s) {
        // TODO will need to rewrite into type, desc, and method variants.
        if (s == null)
            return s;
        s = Naming.demangleFortressIdentifier(s);
        StringBuilder b = new StringBuilder();
        maybeBareVar(s, 0, b, false, false, true);
        
        s = b.toString();
       
        return s;
    }
    
    public String getTypeName(String s) {
        return getTypeName(s, DEFAULT_TUPLE_FLATTENING);
    }
     
   public String getTypeName(String s,
            boolean unwrap_expanded_tuples_in_arrows) {
        // TODO will need to rewrite into type, desc, and method variants.
        String t = s;
        if (s == null)
            return s;
        s = Naming.demangleFortressIdentifier(s);
        StringBuilder b = new StringBuilder();
        maybeBareVar(s, 0, b, false, false, unwrap_expanded_tuples_in_arrows);
        
        s =  b.toString();

        return s;
    }

    public String getUnmangledTypeDesc(String s) {
        // TODO will need to rewrite into type, desc, and method variants.
        String t = s;
        if (s == null)
            return s;
        StringBuilder b = new StringBuilder();
        maybeVarInTypeDesc(s, 0, b);
        
        s =  b.toString();

        return s;
    }

    public String getFieldDesc(String s) {           
        if (s == null)
            return s;        
        s = Naming.demangleFortressDescriptor(s);

        StringBuilder b = new StringBuilder();
        maybeVarInTypeDesc(s, 0, b);

        s = b.toString();
       
        return s;
        }

    public String getFieldDesc(String s, boolean flattening_tuples) {           
        if (s == null)
            return s;        
        s = Naming.demangleFortressDescriptor(s);

        StringBuilder b = new StringBuilder();
        maybeVarInTypeDesc(s, 0, b, flattening_tuples);

        s = b.toString();
       
        return s;
        }

    public String getMethodDesc(String s) {           
        if (s == null)
            return s;
        s = Naming.demangleFortressDescriptor(s);
        
        StringBuilder b = new StringBuilder();
        maybeVarInMethodSig(s, 0, b);

        s = b.toString();
       
        return s;
        }


//    /**
//     * @param s
//     * @return
//     * @throws Error
//     */
//    public String oxfordSensitiveSubstitution(String s) throws Error {
//        int l = s.length();
//        
//        int oxLevel = 0;
//        
//        
//        for (int i = 0; i < l; i++) {
//            char ch = s.charAt(i);
//            
//            if (Naming.GENERIC_TAGS.indexOf(ch) != -1) {
//                // Found a variable
//                StringBuilder b = new StringBuilder();
//                int j = 0;
//                while (j < l) {
//                     ch = s.charAt(j);
//                     j++;
//                     if (ch == Naming.LEFT_OXFORD_CHAR) {
//                         oxLevel++;
//                         b.append(ch);
//                     } else if (ch == Naming.RIGHT_OXFORD_CHAR)  {
//                         oxLevel--;
//                         b.append(ch);
//                     } else if (Naming.GENERIC_TAGS.indexOf(ch) != -1) {
//                         StringBuilder v = new StringBuilder(8);
//                         v.append(ch);
//                         while (j < l) {
//                             ch = s.charAt(j);
//                             if ('0' <= ch && ch <= '9') {
//                                 v.append(ch);
//                                 j++;
//                             } else {
//                                 break;
//                             }
//                         }
//                         // j addresses next character or the end.
//                         // v contains the variable.
//                         String vs = v.toString();
//                         String t = p.get(vs);
//                         if (t == null)
//                             throw new Error("Missing generic binding for " + vs + " map = " + p);
//                         if (oxLevel == 0) {
//                             b.append(t.substring(1));
//                         } else {
//                             b.append(t);
//                         }
//                         
//                     } else {
//                         b.append(ch);
//                     }
//                }
//                s = b.toString();
//                break;
//            }
//        }
//        return s;
//    }

    
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
    int maybeVarInOxfords(String input, int begin, StringBuilder accum,
            boolean unwrap_expanded_tuples_in_arrows) {
        unwrap_expanded_tuples_in_arrows = false; // Quit flattening tuples!
        return mVIO(input, "", begin, accum,
                unwrap_expanded_tuples_in_arrows, Naming.RIGHT_OXFORD_CHAR);
     }
    /**
     * 
     * @param input
     * @param tag
     * @param begin
     * @param accum
     * @param unwrap_expanded_tuples_in_arrows If Arrow[\T;U\] expands to
     *        Arrow[\Tuple[\X;Y\];U\], flatten out the tuple, to Arrow[\X;Y;U\]
     * @return
     */
    int mVIO(String input, String tag, int begin, StringBuilder accum,
            boolean unwrap_expanded_tuples_in_arrows, char rh_bracket) {
        int original_begin = begin;
        ArrayList<String> params = new ArrayList<String>();
        while (true) {
            StringBuilder one_accum = new StringBuilder();
            int at = maybeBareVar(input, begin, one_accum, true, false, unwrap_expanded_tuples_in_arrows);
            char ch = input.charAt(at++);
        
            if (ch == Naming.GENERIC_SEPARATOR_CHAR) {
                params.add(one_accum.toString());
                                
                begin = at; 
                
            } else if (ch == rh_bracket) {
                params.add(one_accum.toString());

                /* 
                 * Next, process params onto accum, but first check
                 * for the case where tag is Arrow, the number of
                 * params is 2, and the first one is Tuple[ something ].
                 * In that case, normalize to remove the Tuple from the
                 * Arrow.
                 */
                if (tag.equals("Arrow") && params.size() == 2) {
                     String domain = params.get(0);
                     if (domain.startsWith(Naming.TUPLE_OX)) {
                         String in_pat = input.substring(original_begin);
                         if (unwrap_expanded_tuples_in_arrows && // WAS ||, seems dubious.
                             in_pat.startsWith(Naming.TUPLE_OX))
                             params.set(0, domain.substring(6, domain.length()-1));
                     }
                }
                
                int l = params.size(); 
                for (int i = 0; i < l; i++) {
                    accum.append(params.get(i));
                    accum.append( i < (l-1) ? Naming.GENERIC_SEPARATOR_CHAR : rh_bracket);
                }
                
                return at;
            } else {
                throw new Error();
            }
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
     int maybeVarInLSemi(String input, int begin, StringBuilder accum,
             boolean unwrap_expanded_tuples_in_arrows) {
        int at = maybeBareVar(input, begin, accum, false, true, unwrap_expanded_tuples_in_arrows);
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
     private static boolean nonVar(char ch) { // maybe a literal semicolon below?
        return ch == '/' || ch == '$' || ch == Naming.GENERIC_SEPARATOR_CHAR || 
               ch == Naming.LEFT_OXFORD_CHAR || ch == Naming.RIGHT_OXFORD_CHAR || 
               ch == Naming.LEFT_HEAVY_ANGLE_CHAR || ch == Naming.RIGHT_HEAVY_ANGLE_CHAR;
    }

     /**
      * 
      * @param input
      * @param begin
      * @param accum
      * @param inOxfords
      * @param xlate_specials
      * @return
      */
     int maybeBareVar(String input, int begin, StringBuilder accum,
             boolean inOxfords, boolean xlate_specials,
             boolean unwrap_expanded_tuples_in_arrows) {
         unwrap_expanded_tuples_in_arrows = false; // quit flattening tuples in arrows
         int at = begin;
         char ch = input.charAt(at++);
         boolean maybeVar = true;
         boolean eol = false;
         boolean disabled = false;
         
         while (ch != Naming.GENERIC_SEPARATOR_CHAR &&
                 ch != ';' && 
                 ch != Naming.RIGHT_OXFORD_CHAR && 
                 ch != Naming.RIGHT_HEAVY_ANGLE_CHAR) {
             if (ch == Naming.HEAVY_X_CHAR || ch == Naming.HEAVY_CROSS_CHAR)
                 disabled = true;
                 if (!maybeVar) {
                 accum.append(ch);
             } else if (nonVar(ch)) {
                 maybeVar = false;
                 accum.append(input.substring(begin, at));
             } 
             
             if (ch == Naming.LEFT_OXFORD_CHAR) {
                 at = (disabled ? EMPTY: this).mVIO(input,
                         input.substring(begin, at-1), at, accum,
                         unwrap_expanded_tuples_in_arrows, Naming.RIGHT_OXFORD_CHAR);
             } else if (ch == Naming.LEFT_HEAVY_ANGLE_CHAR) {
                 at = (disabled ? EMPTY: this).mVIO(input,
                         input.substring(begin, at-1), at, accum,
                         unwrap_expanded_tuples_in_arrows, Naming.RIGHT_HEAVY_ANGLE_CHAR);
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
                     if (xlate_specials && t.equals(Naming.SNOWMAN)) {
                         t = Naming.specialFortressTypes.get(t);
                     }
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
     /**
      * 
      * @param input
      * @param begin
      * @param accum
      * @param inOxfords
      * @param xlate_specials
      * @return
      */
     void maybeBareMethodName(String input, StringBuilder accum) {
         int at = 0;
         int last = at;
         while (at < input.length()) {
             char ch = input.charAt(at);
             if (Naming.METHOD_SPECIALS.indexOf(ch) != -1) {
                 maybeChunk(input, accum, at, last);
                 accum.append(ch);
                 last=at+1;
                 if (ch == Naming.HEAVY_X_CHAR || ch == Naming.HEAVY_CROSS_CHAR) {
                     accum.append(input.substring(last));
                     return;
                 }
             }
             at++;
         }
         
         maybeChunk(input, accum, at, last);
         return;
     }

    /**
     * @param input
     * @param accum
     * @param at
     * @param last
     */
    private void maybeChunk(String input, StringBuilder accum, int at, int last) {
        String part = input.substring(last, at);
         String repl = p.get(part);
         if (repl != null)
             accum.append(repl);
         else
             accum.append(part);
    }
     
    int maybeVarInMethodSig(String input, int begin, StringBuilder accum) {
        return maybeVarInMethodSig(input, begin, accum, DEFAULT_TUPLE_FLATTENING);
    }
    int maybeVarInMethodSig(String input, int begin, StringBuilder accum, boolean unwrap_expanded_tuples_in_arrows) {
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
                 at = maybeVarInLSemi(input, at, accum, unwrap_expanded_tuples_in_arrows);
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
     private int maybeVarInTypeDesc(String input, int at, StringBuilder accum) {
         return maybeVarInTypeDesc(input, at, accum, DEFAULT_TUPLE_FLATTENING);
     }

     private int maybeVarInTypeDesc(String input, int at, StringBuilder accum,
            boolean unwrap_expanded_tuples_in_arrows) {
        char ch;
        ch = input.charAt(at++);
         accum.append(ch);
         if (ch == 'L') {
             at = maybeVarInLSemi(input, at, accum, unwrap_expanded_tuples_in_arrows);
         }
         return at;
    }

    /**
     * @param s 
     * @return
     */
    public static int templateClosingRightOxford(String s) {
        int heavy_x = s.indexOf(Naming.HEAVY_X);
        int heavy_c = s.indexOf(Naming.HEAVY_CROSS);
        // Assume s can contain cross or X but not both.
        int heavy_max = Math.max(heavy_x, heavy_c);
        int rightBracket = (heavy_max == -1 ? s : s.substring(0, heavy_max)).lastIndexOf(Naming.RIGHT_OXFORD);
        return rightBracket;
    }


    /**
     * @param s
     * @param leftBracket
     * @param rightBracket
     * @return
     */
    public static List<String> extractStringParameters(String s,
                                                             int leftBracket, int rightBracket, List<String> parameters) {
        
        int depth = 1;
        int pbegin = leftBracket+1;
        for (int i = leftBracket+1; i <= rightBracket; i++) {
            char ch = s.charAt(i);
    
            if ((ch == Naming.GENERIC_SEPARATOR_CHAR ||
                    ch == Naming.RIGHT_OXFORD_CHAR ||
                    ch == Naming.RIGHT_HEAVY_ANGLE_CHAR) && depth == 1) {
                String parameter = s.substring(pbegin,i);
                if (parameters != null)
                    parameters.add(parameter);
                pbegin = i+1;
            } else {
                if (ch == Naming.LEFT_OXFORD_CHAR ||
                        ch == Naming.LEFT_HEAVY_ANGLE_CHAR) {
                    depth++;
                } else if (ch == Naming.RIGHT_OXFORD_CHAR ||
                        ch == Naming.RIGHT_HEAVY_ANGLE_CHAR) {
                    depth--;
                } else {
    
                }
            }
        }
        return parameters;
    }


    /**
     * 
     * @param name
     * @param left_oxford
     * @param right_oxford
     * @return
     * @throws Error
     */
    public static String canonicalizeStaticParameters(String name, int left_oxford,
            int right_oxford, ArrayList<String> sargs) throws Error {
        
        String template_start = name.substring(0,left_oxford+1);
        String template_end = name.substring(right_oxford);
        // Note include trailing oxford to simplify loop termination.
        
        extractStringParameters(name, left_oxford, right_oxford, sargs);
        
        String s = template_start +
                   // template_middle +
                   template_end;
        return s;
    }
}
