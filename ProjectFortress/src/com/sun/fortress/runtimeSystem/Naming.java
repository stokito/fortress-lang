/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.runtimeSystem;

/* NOTE: this should NOT depend on any part of the main compiler, because this
 * is used as part of the runtime system.  There's some refactoring of single
 * sources for important names, that still has to be moved into here.
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;

import com.sun.fortress.compiler.nativeInterface.SignatureParser;
import com.sun.fortress.useful.CheapSerializer;
import com.sun.fortress.useful.F;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.ProjectedList;
import com.sun.fortress.useful.Triple;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.VersionMismatch;

public class Naming {
    
    public static final String ABSTRACT_ARROW = "AbstractArrow";
    public static final String CONCRETE_TUPLE = "ConcreteTuple";

    private final static
    
    CheapSerializer.PAIR<
        String,
        List<Triple<String, String, Integer>>> xlationSerializer = 
            
            new CheapSerializer.PAIR<String,List<Triple<String, String, Integer>>>(
                    CheapSerializer.STRING,
                    new CheapSerializer.LIST<Triple<String, String, Integer>>(
                            new CheapSerializer.TRIPLE<String, String, Integer>(
                                    CheapSerializer.STRING,
                                    CheapSerializer.STRING,
                                    CheapSerializer.INTEGER)
                            )
                    )
        ;
    
    public final static class XlationData {
        Pair<String, List<Triple<String, String, Integer>>> data;
        
        public XlationData(String tag) {
            data = new Pair<String, List<Triple<String, String, Integer>>>(tag,
                    new ArrayList<Triple<String, String, Integer>>());
        }
        
        private XlationData(byte[] bytes)  throws VersionMismatch {
            data = xlationSerializer.fromBytes(bytes);
        }
        
        private XlationData(String tag, XlationData other) {
            data =  new Pair<String, List<Triple<String, String, Integer>>>(tag,
                    other.data.getB());
        }
        
        static public XlationData fromBytes(byte[] bytes) throws VersionMismatch {
            return new XlationData(bytes);
        }
        
        public byte[] toBytes() {
            return Naming.xlationSerializer.toBytes(data);
        }
        
        public List<String> staticParameterNames() {
            List<String> xl =
                new ProjectedList<Triple<String, String, Integer>, String>(
                        data.getB(),
                        new Triple.GetB<String, String, Integer>());
            return xl;
        }
        public List<String> staticParameterKinds() {
            List<String> xl =
                new ProjectedList<Triple<String, String, Integer>, String>(
                        data.getB(),
                        new Triple.GetA<String, String, Integer>());
            return xl;
        }
        
        public final static F<String, Boolean> isOpr = new F<String, Boolean>() {
            @Override
            public Boolean apply(String x) {
                return x.equals(Naming.XL_OPR);
            }
            
        };
        
        public List<Boolean> isOprKind() {
            List<String> xl =
                new ProjectedList<Triple<String, String, Integer>, String>(
                        data.getB(),
                        new Triple.GetA<String, String, Integer>());
            return Useful.applyToAll(xl, isOpr);
        }
        public List<Pair<String,String>> staticParameterKindNamePairs() {
            List<Pair<String,String>> xl =
                new ProjectedList<Triple<String, String, Integer>, Pair<String,String>>(
                        data.getB(),
                        new Triple.GetAB<String, String, Integer>());
            return xl;
        }
        public XlationData setTraitObjectTag(String tag) {
            return new XlationData(tag, this);
        }
        public String first() {
            return data.first();
        }
        
        public void addKindAndNameToStaticParams(Triple<String, String, Integer> p) {
            data.getB().add(p);
        }
        public void addKindAndNameToStaticParams(String sort, String value) {
            data.getB().add(new Triple<String, String, Integer>(sort, value, 0));
        }
    }

    /* 
     * Refactoring note, there may be other defs of these names, that should
     * refer to here instead.
     */
    public final static String RT_VALUES_PKG = "com/sun/fortress/compiler/runtimeValues/";
    public final static String ANY_TYPE_CLASS = "fortress/AnyType$Any";
    
    public final static int TUPLE_ORIGIN = 1;
    public final static int STATIC_PARAMETER_ORIGIN = 1;
    public final static String RTTI_FIELD = "RTTI";
    public final static String RTTI_GETTER = "getRTTI";
    public final static String RTTI_GETTER_CLASS = ANY_TYPE_CLASS;
    public final static String RTTI_CONTAINER_TYPE = RT_VALUES_PKG + "RTTI";
    public final static String RTTI_SINGLETON = "ONLY";
    public final static String RTTI_FACTORY = "factory";
    public final static String TUPLE_RTTI_CONTAINER_TYPE = RT_VALUES_PKG + "TupleRTTI";
    public final static String ARROW_RTTI_CONTAINER_TYPE = RT_VALUES_PKG + "ArrowRTTI";
    public final static String JAVA_RTTI_CONTAINER_TYPE = RT_VALUES_PKG + "JavaRTTI";
    public final static String RTTI_CONTAINER_DESC = "L" + RTTI_CONTAINER_TYPE + ";";
    public final static String RTTI_CONTAINER_ARRAY_DESC = "[L" + RTTI_CONTAINER_TYPE + ";";
    public final static String STATIC_PARAMETER_GETTER_SIG = "()" + RTTI_CONTAINER_DESC;
    public final static String VOID_RTTI_CONTAINER_TYPE = RT_VALUES_PKG + "VoidRTTI";
    public final static String RTTI_SUBTYPE_METHOD_SIG = "(" + RTTI_CONTAINER_DESC + ")Z";
    public final static String RTTI_SUBTYPE_METHOD_NAME = "runtimeSupertypeOf";

    
    /*
     * Special characters for tagging in generate names.
     * These should not be mathematical operators, nor should they be "letters",
     * including in foreign languages.  Best places to go looking for these are
     * in the "Miscellaneous symbols" (2600) and "Dingbats" (2700) code blocks.
     */
    
    // Used to indicate translation convention to apply to type parameter.
    public final static String FOREIGN_TAG = "\u2615"; // hot beverage == JAVA
    public final static String NORMAL_TAG = "\u263a"; // smiley face == normal case.
    // public final static String INTERNAL_TAG = "\u26a0"; // warning sign -- internal use only (fortress.)

    public final static String ENVELOPE = "\u2709"; // Signals necessary closure
    public final static String CLOSURE_FIELD_NAME = ENVELOPE; // Name of closure field
    public final static String SNOWMAN = "\u2603"; // for empty tuple, sigh.
    public final static String INDEX = "\u261e";  // "__"; // "\u261e"; // white right point index (for dotted of functional methods)
    public final static String BOX = "\u2610"; // ballot box, used to indicate prefix or postfix.
    
    public final static String HEAVY_X = "\u2716"; // heavy X -- stop rewriting in an instantiation, for method_schema tags
    public final static String HEAVY_CROSS = "\u271A"; // heavy CROSS -- stop rewriting in an instantiation, for closure schema tags

    public static final String UP_INDEX = "\u261d"; // Special static parameter for static type of self in generic method invocation
    
    public static final String BOTTOM = "\u2620"; // Bottom --  skull and crossbones
    
    /* Enter is used in calculated references for nat args appearing
     * within uninstantiated generics.  Calculations are converted to RPN,
     * to be simplified at instantiation.
     */
    public static final String ENTER = "\u2386"; // used
    public static final String GENERATED = "\u270e"; // lower right pencil == Desugarer gensyms

    public static final String INTERNAL_SNOWMAN = SNOWMAN;

    public final static char FOREIGN_TAG_CHAR = FOREIGN_TAG.charAt(0);
    public final static char NORMAL_TAG_CHAR = NORMAL_TAG.charAt(0);
    
    public static final char HEAVY_X_CHAR = HEAVY_X.charAt(0);
    public static final char HEAVY_CROSS_CHAR = HEAVY_CROSS.charAt(0);

    public final static String GEAR = "\u2699";

    public final static char LEFT_OXFORD_CHAR = '\u27e6'; // generic
    public final static char RIGHT_OXFORD_CHAR = '\u27e7'; // generic
    public final static char LEFT_PAREN_ORNAMENT_CHAR = '\u2768'; // sig disambig tag
    public final static char RIGHT_PAREN_ORNAMENT_CHAR = '\u2769'; // sig disambig tag
    public final static String LEFT_OXFORD = "\u27e6";
    public final static String RIGHT_OXFORD = "\u27e7";
    
    public final static String LEFT_HEAVY_ANGLE = "\u276e"; // marks Opr parameters in RTTI types
    public final static String RIGHT_HEAVY_ANGLE = "\u276f"; // marks Opr parameters in RTTI types
    public final static char LEFT_HEAVY_ANGLE_CHAR = LEFT_HEAVY_ANGLE.charAt(0); // marks Opr parameters in RTTI types
    public final static char RIGHT_HEAVY_ANGLE_CHAR = RIGHT_HEAVY_ANGLE.charAt(0); // marks Opr parameters in RTTI types

    public final static String XL_BOOL = "bool";
    public final static String XL_INTNAT = "intnat";
    public final static String XL_OPR = "opr";
    public final static String XL_TYPE = "type";
    public final static String XL_UNIT = "unit";
    public final static String XL_DIM = "dim";
    
    /**
     * Name for Arrow-interface generic.
     */
    public final static String ARROW_TAG="Arrow";
    public final static String ARROW_RTTI_TAG = "Arrow*";
    public static String arrowRTTIclass(int n) {
    	return ARROW_RTTI_TAG + n;
    }
    
    /**
     * Name for Tuple-interface generic.
     */
    public final static String TUPLE_TAG="Tuple";
    public final static String TUPLE_RTTI_TAG="Tuple*";
    public static String tupleRTTIclass(int n) {
    	return TUPLE_RTTI_TAG + n;
    }

    public final static String COMPILER_BUILTIN = "CompilerBuiltin";
    public final static String NATIVE_PREFIX_DOT = "native.";

    public final static String APPLY_METHOD = "apply";
    public final static String APPLIED_METHOD = "the_function";
    
    // By default, the static and instance methods would have the same name, which does not work.
    public final static String GENERIC_METHOD_FINDER_SUFFIX_IN_TRAIT = "_static";

    public static final String runtimeValues = "com/sun/fortress/compiler/runtimeValues/";

    // java.lang.Object correctly formatted for asm generation
    public static final String javaObject = "java/lang/Object";
    
    //BAlongTrees used to keep track of instantiated instances of generic closures
    public static final String CACHE_TABLE_TYPE = "com/sun/fortress/runtimeSystem/BAlongTree";
    public static final String CACHE_TABLE_DESC = internalToDesc(CACHE_TABLE_TYPE);
    public static String cacheTableName(String definitionID) {
        return definitionID + HEAVY_X + "table";
    }
    
    /* Sometimes a generated constant or string depends on a type parameter.
     * In order to efficiently insert the substituted value, it is encoded
     * into a (bogus) method invocation returning the appropriate type.
     * For example, INTERP.hash"this is some string constant"() (returns long).
     * When the Instantiating classloader encounters such a method invocation,
     * it performs the appropriate calculation and replacement.
     * Plan, likely, is to encode strings with a count and characters.
     */
    public static final String magicInterpClass = "CONST";
    public static final String hashMethod = "hash";
    public static final String stringMethod = "String";

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
        bl(COMPILER_BUILTIN, "$Boolean", "FBoolean");
        bl(COMPILER_BUILTIN, "$Character", "FCharacter");
        bl(COMPILER_BUILTIN, "$JavaBufferedReader", "FJavaBufferedReader");
        bl(COMPILER_BUILTIN, "$JavaBufferedWriter", "FJavaBufferedWriter");
        bl(COMPILER_BUILTIN, "$RR32", "FRR32");
        bl(COMPILER_BUILTIN, "$RR64", "FRR64");
        bl(COMPILER_BUILTIN, "$ZZ32", "FZZ32");
        bl(COMPILER_BUILTIN, "$ZZ64", "FZZ64");
        bl(COMPILER_BUILTIN, "$ZZ", "FZZ");        
        bl(COMPILER_BUILTIN, "$JavaString", "FJavaString");
        bl("", SNOWMAN, "FVoid");
        bl("", INTERNAL_SNOWMAN, "FVoid");
        bl(COMPILER_BUILTIN, "$Vector", "FVector");
    }

    public static String opForString(String op, String s) {
        return op + "." + s;
    }

    
    private final static String magicDot = magicInterpClass + "." ;
    public static boolean isEncodedConst(String s) {
        return s.startsWith(magicDot);
    }

    public static String encodedOp(String s) {
        int first = s.indexOf('.');
        return s.substring(0, first);
    }
    
    public static String encodedConst(String s) {
        int first = s.indexOf('.');
        return s.substring(first+1);      
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

    public static final String FUNCTION_GENERIC_TAG = "function";
    public static final String TRAIT_GENERIC_TAG = "trait";
    public static final String OBJECT_GENERIC_TAG = "object";
    public static final String RTTI_GENERIC_TAG = "rtti";

    public static String javaDescForTaggedFortressType(String ft) {

        //char ch = ft.charAt(0);
        String tx = specialFortressDescriptors.get(ft);
        if (tx != null) {
            return tx; // Should be correct by construction
        }
        return internalToDesc(ft);

//        else if (ch == NORMAL_TAG_CHAR) {
//            //return "L" + mangleFortressIdentifier(ft) + ";";
//            return "L" + (ft) + ";";
//        } else if (ch == INTERNAL_TAG_CHAR) {
//            //return "Lfortress/" + mangleFortressIdentifier(ft) + ";";
//            return "Lfortress/" + (ft) + ";";
//        } else if (ch == FOREIGN_TAG_CHAR) {
//            throw new Error("Haven't figured out JVM xlation of foreign type " + ft);
//        }
//        throw new Error("Bad fortress naming scheme tag (unicode " + Integer.toHexString(ch) +
//                    ") on fortress type " + ft);
    }

    public static String deDot(String s) {
        int lox = s.indexOf(LEFT_OXFORD_CHAR);
        if (lox == -1)
            return s.replace(".", "/");
        // don't de-dot inside of oxford brackets.
        //int rox = s.indexOf(RIGHT_OXFORD_CHAR);
        return s.substring(0, lox).replace(".", "/") +
               s.substring(lox);
    }

    public static String deDollar(String s) {
        return s.replace(".", "$");
    }

    public static String deMangle(String s) {
        if (s.length() < 2 || s.charAt(0) != '\\')
            return s;
        if (s.charAt(1) == '=')
            s = s.substring(2);
        int l = s.length();
        if (l == 0)
            return s;
        StringBuilder sb = new StringBuilder();
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
    public static String catMangled(String s1, String s2, String s3, String s4) {
        return catMangled(catMangled(s1,s2), catMangled(s3,s4));
    }

    public static String mangleMethodSignature(String s) {
        StringBuilder sb = new StringBuilder();
        int l = s.length();
        int i = 0;
        while (i < l) {
            char ch = s.charAt(i);
            switch (ch) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
            case 'V': // should only appear in return if well-formed

            case '[': // eat array indicator
            case '(': // eat intro and outro, assume well-formed
            case ')':
                sb.append(ch);
                i++;
                break;
            case 'L':
                sb.append(ch);
                i = mangleFortressIdentifier(s, i+1, sb, null, false);
                break;
            default:
                Error e = new Error("Was not expecting to see character " + ch + " in " + s);
                throw e;
            }
        }
        return sb.toString();
    }
    
    public static String mangleMethodSignature(String s, StringBuilder erasedContent, boolean erase_UI) {
        // need to work on the erasedContent mangling
        StringBuilder sb = new StringBuilder();
        int l = s.length();
        int i = 0;
        while (i < l) {
            char ch = s.charAt(i);
            switch (ch) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
            case 'V': // should only appear in return if well-formed

            case '[': // eat array indicator
            case ')':
                if (erase_UI && erasedContent.length() > 0) {
                    erasedContent.append("->");
                }
            case '(': // eat intro and outro, assume well-formed
                sb.append(ch);
                i++;
                break;
            case 'L':
                sb.append(ch);
                i = mangleFortressIdentifier(s, i+1, sb, erasedContent, erase_UI);
                break;
            default:
                Error e = new Error("Was not expecting to see character " + ch + " in " + s);
                throw e;
            }
        }
        return sb.toString();
    }

    public static String demangleMethodSignature(String s) {
        StringBuilder sb = new StringBuilder();
        int l = s.length();
        int i = 0;
        while (i < l) {
            char ch = s.charAt(i);
            switch (ch) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
            case 'V': // should only appear in return if well-formed

            case '[': // eat array indicator
            case '(': // eat intro and outro, assume well-formed
            case ')':
                sb.append(ch);
                i++;
                break;
            case 'L':
                sb.append(ch);
                i = demangleFortressIdentifier(s, i+1, sb);
                break;
            default:
                throw new Error("Was not expecting to see character " + ch);
            }
        }
        return sb.toString();
    }

    /**
     * Mangles the chunks of a fortress identifier, where the chunks are
     * delimited by $, /, and ; appearing outside of Oxford brackets.
     *
     * Returns either when the string is exhausted,
     * or after a semicolon is processed.
     *
     * @param s  the string to mangle
     * @param i  the index to begin at
     * @param sb the stringbuffer to which the transformed string is appended.
     * @return the index of the next character to process (if any).
     */
    private static int mangleFortressIdentifier(String s, int start, StringBuilder sb, StringBuilder erasedContent, boolean erase_UI) {
        return mangleOrNotFortressIdentifier(s, start, sb, true, erasedContent, erase_UI);
    }

    public static String mangleFortressIdentifier(String s) {
        if (s == null)
            return null;
        //int l = s.length();
        // Special case of <init> and <clinit>
        if (pointyDelimitedInitMethod(s))
            return s;
        StringBuilder sb = new StringBuilder();
         mangleOrNotFortressIdentifier(s,0, sb, true, null, false);
         String t = sb.toString();
         if (t.startsWith("\\-=Arrow"))
             throw new Error("AHA!");
         return t;
    }

    public static String mangleFortressFileName(String s) {
        if (s == null)
            return null;
       
         StringBuilder sb = new StringBuilder();
         mangleOrNotFortressFileName(s,0, sb,true);
         String t = sb.toString();
         
         return t;
    }

    /**
     * Expects a type, surrounded by L;, or one of the descriptor type characters.
     * @param s
     * @return
     */
    public static String mangleFortressDescriptor(String s) {
        // This is a degenerate case of "signature"; if that is made pickier, this will not work.
        String t =  mangleMethodSignature(s);
        if (t.startsWith("\\-=Arrow"))
            throw new Error("AHA!");
        return t;
    }
    public static String mangleFortressDescriptor(String s, StringBuilder erasedContent, boolean erase_UI) {
        // This is a degenerate case of "signature"; if that is made pickier, this will not work.
        String t =  mangleMethodSignature(s, erasedContent, erase_UI);
        if (t.startsWith("\\-=Arrow"))
            throw new Error("AHA!");
        return t;
    }
    public static String demangleFortressDescriptor(String s) {
        // This is a degenerate case of "signature"; if that is made pickier, this will not work.
        return demangleMethodSignature(s);
    }

    /**
     * Mangles names of methods (and fields?).
     * Mangling includes / and $.
     * Names beginning and end with less-than and greater-than are left along
     * (init, clinit)
     *
     * @param s
     * @return
     */
    public static String mangleMemberName(String s) {
        if (s == null)
            return null;
        //int l = s.length();
        // Special case of <init> and <clinit>
        if (pointyDelimitedInitMethod(s))
            return s;
        return mangleIdentifier(s);
    }

    /**
     * DE-mangles the chunks of a fortress identifier, where the chunks are
     * delimited by $, /, and ; appearing outside of Oxford brackets.
     *
     * Returns either when the string is exhausted,
     * or after a semicolon is processed.
     *
     * @param s  the string to mangle
     * @param i  the index to begin at
     * @param sb the stringbuffer to which the transformed string is appended.
     * @return the index of the next character to process (if any).
     */
    private static int demangleFortressIdentifier(String s, int start, StringBuilder sb) {
        return mangleOrNotFortressIdentifier(s,start, sb, false, null, false);
    }

    public static String demangleFortressIdentifier(String s) {
        if (s == null)
            return null;
        //int l = s.length();

        // Special case of <init> and <clinit>
        if (pointyDelimitedInitMethod(s))
            return s;

        StringBuilder sb = new StringBuilder();
         mangleOrNotFortressIdentifier(s,0, sb, false, null, false);
         return sb.toString();
    }

    /**
     * @param s
     * @param l
     * @return
     */
    public static boolean pointyDelimitedInitMethod(String s) {
        //int l = s.length();
        return s.length() > 0 && s.charAt(0) == '<' && s.endsWith("init>");
    }


    private static int mangleOrNotFortressIdentifier(String s, int start,
            StringBuilder sb, boolean mangleOrNot, StringBuilder erasedContent, boolean erase_UI) {
        // specials = $/;
        return mangleOrNotFortressIdentifier(s, start, sb, mangleOrNot,"$/;", erasedContent, erase_UI);
    }

    private static int mangleOrNotFortressFileName(String s, int start,
            StringBuilder sb, boolean mangleOrNot) {
        // specials = $/;
        return mangleOrNotFortressIdentifier(s, start, sb, mangleOrNot,"$/;.", null, false);
    }

    private static int mangleOrNotFortressIdentifier(String s, int start,
            StringBuilder sb, boolean mangleOrNot,
            String specials, StringBuilder erasedContent, boolean erase_UI) {
        int l = s.length();
        int nesting = 0;

        for (int i = start; i < l; i++) {
            char ch = s.charAt(i);
            if (ch == LEFT_OXFORD_CHAR) {
                nesting++;
            } else if (ch == RIGHT_OXFORD_CHAR) {
                nesting--;
            } else if (nesting == 0 && (-1 != specials.indexOf(ch))) {
                appendNonEmptyMangledSubstring(sb, s, start, i, mangleOrNot, erasedContent, erase_UI);
                sb.append(ch);
                if (ch == ';')
                    return i+1;
                start = i+1;
            }
        }
        appendNonEmptyMangledSubstring(sb, s, start, l, mangleOrNot, erasedContent, erase_UI);
        return l;
    }


    /**
     * @param sb
     * @param s
     * @param start
     * @param i
     */
    private static void appendNonEmptyMangledSubstring(StringBuilder sb,
            String s, int start, int i, boolean mangleOrNot, StringBuilder erasedContent, boolean erase_UI) {
        if (i - start > 0) {
            s = s.substring(start, i);
            if (erase_UI && s.startsWith(UNION_OX)) {
                    erasedContent.append(s);
                    s = ERASED_UNION_TYPE;
                    sb.append(s);
            } else {
                sb.append(mangleOrNot ? mangleIdentifier(s) : deMangle(s));
            }
        }
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
            StringBuilder mangledStringBuilder = null;
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
                        mangledStringBuilder = new StringBuilder(mangledString.substring(0, j+1));
                        mangledStringBuilder.append('-');
                        mangledStringBuilder.append(ch);
                         for (int i = j+2; i < l-1; i++) {
                            ch = identifier.charAt(i);
                            mangledStringBuilder.append(ch);
                            if (ch == '\\') {
                                ch = identifier.charAt(i+1);
                                if (-1 !=  SF_ESCAPES.indexOf(ch)) {
                                    // found one, do the translation.
                                    mangledStringBuilder.append('-');
                                }
                            }

                        }
                         if (j + 2 < l)
                             mangledStringBuilder.append(identifier.charAt(l-1));
                        mangledString = mangledStringBuilder.toString();
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

            // debugging check for double-mangling
            if (mangledString.startsWith("\\-")) {}

            return mangledString;
        }

        /**
         * Need to generalize to include BCDFIJS, too.
         * @param sig
         * @param selfIndex
         * @return
         */
    public static String removeNthSigParameter(String sig, int selfIndex) {
        if (selfIndex == NO_SELF)
            return sig;
        // start, end, are inclusive bounds of nth parameter in sig.
        // This probably needs to parse Oxford brackets, else we will be sorry.
        SignatureParser parsedSig = new SignatureParser(sig);
        return parsedSig.removeNthParameter(selfIndex);
    }

    /**
     * Need to generalize to include BCDFIJS, too.
     * @param sig
     * @param selfIndex
     * @return
     */
    public static String replaceNthSigParameter(String sig, int selfIndex, String newParamDesc) {
        if (selfIndex == NO_SELF)
            return sig;
        // start, end, are inclusive bounds of nth parameter in sig.
        // This probably needs to parse Oxford brackets, else we will be sorry.
        SignatureParser parsedSig = new SignatureParser(sig);
        return parsedSig.replaceNthParameter(selfIndex, newParamDesc);
    }

    // This seems wrong if applied to non-mangled generics.
    public static String nthSigParameter(String sig, int selfIndex) {
        SignatureParser parsedSig = new SignatureParser(sig);
        return parsedSig.getJVMArguments().get(selfIndex);
    }
    
    public static String sigRet(String sig) {
        SignatureParser parsedSig = new SignatureParser(sig);
        String ret = parsedSig.getJVMResult();
        // lose the L;
        return ret.substring(1,ret.length()-1);
    }

    public static String dotToSep(String name) {
        name = name.replace('.', '/');
        return name;
    }

    public static String sepToDot(String name) {
        name = name.replace('/', '.');
        return name;
    }

    /**
     * Returns the package+class name for the class generated for the closure
     * implementing a generic method.  Includes GEAR  (generic function),
     * ENVELOPE (closure), static parameters, and schema.
     * 
     * @param simple_name
     * @param static_parameters
     * @param generic_arrow_schema
     * @return
     */
    public static String genericFunctionPkgClass(String component_pkg_class, String simple_name,
            String static_parameters, String generic_arrow_schema) {
        return component_pkg_class + GEAR +"$" +
        simple_name + static_parameters + ENVELOPE + "$" + HEAVY_CROSS + generic_arrow_schema;
    }


    //Asm requires you to call visitMaxs for every method
    // but ignores the arguments.
    public static final int ignoredMaxsParameter = 1;

    public static final String RTTI_CLASS_SUFFIX = "$RTTIc";

    public static final String RTTI_INTERFACE_SUFFIX = "$RTTIi";
    
    public static final String RTTI_MAP_TYPE =
        "com/sun/fortress/runtimeSystem/RttiTupleMap";
    public static final String RTTI_MAP_DESC = internalToDesc(RTTI_MAP_TYPE);


    /**
     * Returns the name of the RTTI class for a Fortress type,
     * given the (Java) name of the stem of the type.
     * The "stem" is the type name, minus static parameters (in Oxford
     * brackets) if there are any.
     * 
     * For Arrows and Tuples, the stem should already be in RTTI form:
     *  Tuple,# - where # is the length of the tuple
     *  Arrow,# - where # is the number of arguments plus 1 for the return type
     *      (note that a single tuple argument would be un-tupled while a tupled
     *       return type will not)
     * 
     * This is used to allocate the type names, and in certain instances of
     * type queries (e.g., for Object types).
     * 
     * @param stemClassName
     * @return
     */
    public static String stemClassToRTTIclass(String stemClassName) {
//        if (stemClassName.startsWith("ConcreteTuple")) {
//            java.lang.System.err.println("stemClassToRTTIclass called with ConcreteTuple - FIX");
//            //concrete tuples n-ary use the RTTI class Tuple,<n>$RTTIc
//        	int n = InstantiatingClassloader.extractStringParameters(stemClassName).size();
//        	return TUPLE_RTTI_TAG + n + RTTI_CLASS_SUFFIX;
//        }
    	return stemClassName + RTTI_CLASS_SUFFIX;
    }

    public static String oprArgAnnotatedRTTI(String stemClassName,
            List<String> opr_args) {
        if (opr_args.size() == 0)
            return stemClassName;
        StringBuffer sb = new StringBuffer(stemClassName);
        String sep = LEFT_HEAVY_ANGLE;
        for (String op : opr_args) {
            sb.append(sep);
            sep = ",";
            sb.append(op);
        }
        sb.append(RIGHT_HEAVY_ANGLE);
        return sb.toString();
    }

    public static String fileForOprTaggedGeneric(String stemClassName) {
        return stemClassName + LEFT_HEAVY_ANGLE + RIGHT_HEAVY_ANGLE;
    }


    
    /**
     * Returns the name of the RTTI interface for a Fortress type,
     * given the (Java) name of the stem of the type.
     * The "stem" is the type name, minus static parameters (in Oxford
     * brackets) if there are any.
     * 
     * For Arrows and Tuples, the stem should already be in RTTI form:
     *  Tuple,# - where # is the length of the tuple
     *  Arrow,# - where # is the number of arguments plus 1 for the return type
     *      (note that a single tuple argument would be un-tupled while a tupled
     *       return type will not)
     * Since we don't have interfaces for these RTTI types, we just use the class
     * 
     * In general, this is what is tested against and cast to in type queries.
     * 
     * @param stemClassName
     * @return
     */
    public static String stemInterfaceToRTTIinterface(String stemClassName) {
        if (stemClassName.startsWith(TUPLE_RTTI_TAG) || stemClassName.startsWith(ARROW_RTTI_TAG) )
            return stemClassName + RTTI_CLASS_SUFFIX;
        
        return stemClassName + RTTI_INTERFACE_SUFFIX;
    }

    /**
     * @param owner_and_result_class The java class implementing a (generic) fortress class's RTTI.
     * @param n_static_params
     * @return
     */
    public static String rttiFactorySig(final int n_static_params) {
        return InstantiatingClassloader.jvmSignatureForNTypes(
                n_static_params, RTTI_CONTAINER_TYPE, internalToDesc(RTTI_CONTAINER_TYPE));
    }
    
    public static String combineStemAndSparams(String stem, String sparams_in_oxfords) {
        return stem + sparams_in_oxfords;
    }
    
    public static String rttiClassToBaseClass(String rttiClass) {
        if (rttiClass.startsWith(ARROW_RTTI_TAG)) {
            return ABSTRACT_ARROW;
        } else if (rttiClass.startsWith(TUPLE_RTTI_TAG)) {
            return CONCRETE_TUPLE;
        } else {
            return rttiClass.substring(0,rttiClass.length() - Naming.RTTI_CLASS_SUFFIX.length());
        }
        
    }
    
    /**
     * Convert an ASM internal form to a Java descriptor form.
     * That is, surround a class type with L and ;
     * Special case for snowman - use FVoid type
     */
    // Widely used
    public static String internalToDesc(String type) {
        if (type.equals(INTERNAL_SNOWMAN)) type = specialFortressTypes.get(type);
    	return "L" + type + ";";
    }

    public static String internalToType(String type) {
        if (type.equals(INTERNAL_SNOWMAN)) type = specialFortressTypes.get(type);
        return  type ;
    }

    /**
         * Returns "(" + param + ")" + result ; converts
         * to JVM method descriptor form.
         *
         * @param param
         * @param result
         * @return
         */
        // Widely used internally, not much outside.
        public static String makeMethodDesc(String param, String result) {
            return "(" + param + ")" + result;
        }

        /**
         * Returns the mangling of the static parameter list appropriate for
         * use in a template file name.
         */
        
    public static String makeTemplateSParams(String sparamsPart) {
        if (sparamsPart.length() == 0)
            return "";
        else
            return LEFT_OXFORD + RIGHT_OXFORD;
    }

        static public String staticParameterGetterName(String stem_class_name, int index) {
            // using __ for separator to ease native/primitive type story.
            return stem_class_name + "__" + index;
        }

        public static final String descVoid          = org.objectweb.asm.Type.getDescriptor(void.class);

        public static final String voidToVoid        = makeMethodDesc("", Naming.descVoid);


        // belongs in Naming perhaps
        public static String fmDottedName(String name, int selfIndex) {
            if (selfIndex != Naming.NO_SELF)
                name = name + INDEX + selfIndex;
            return name;
        }


        public static final int NO_SELF = -1;
        public static final String RT_HELPERS = "com/sun/fortress/runtimeSystem/RTHelpers";
        public static final String ERASED_UNION_TYPE = "java/lang/Object";
        public static final String ERASED_UNION_DESC = "L" + ERASED_UNION_TYPE + ";" ;
        public static final String UNION = "Union";
        public final static String UNION_OX = Naming.UNION + Naming.LEFT_OXFORD;
        public static final String INTERSECTION = "Intersection";
        public final static String INTERSECTION_OX = Naming.INTERSECTION + Naming.LEFT_OXFORD;
        public static final String ARROW_OX = ARROW_TAG + "\u27e6";
        public static final String TUPLE_OX = TUPLE_TAG + "\u27e6";
        
        public static final String GENERIC_SEPARATOR = ",";
        public static final char GENERIC_SEPARATOR_CHAR = GENERIC_SEPARATOR.charAt(0);
        public static final String NON_OVERLOADED_TAG = "\u2659"; // Pawn
        public static final String METHOD_SPECIALS =
            NON_OVERLOADED_TAG + INDEX + GENERIC_SEPARATOR +
            LEFT_HEAVY_ANGLE + RIGHT_HEAVY_ANGLE + HEAVY_X + HEAVY_CROSS;
        

    
}
