/*******************************************************************************
    Copyright 2008,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_resolver;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.parser_util.precedence_opexpr.*;
import com.sun.fortress.useful.MagicNumbers;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.VarArgs;

import java.util.*;

/* The following file is the automatically-generated list of operator
 * sets and equivalences.  This file is used as the primary data
 * source for the PrecedenceMap. */

/**
 * A precedence map maps pairs of operator names (expressed as
 * strings) to a precedence relationship (a RealPrecedence).  It does
 * this by mapping each operator into an equivalence class of
 * operators with identical precedence, then keeping a map of the
 * relationships between equivalence classes.  Most of the
 * complications have to do with setting up the relationships in the
 * first place; having done that we'll simply be looking things up.
 */
public class PrecedenceMap {

    public static final PrecedenceMap ONLY = new PrecedenceMap();

    private static final class IPair implements Comparable<IPair> {
        private final CanonOp a;
        private final CanonOp b;

        public IPair(CanonOp ia, CanonOp ib) {
            super();
            this.a = ia;
            this.b = ib;
        }

        public boolean equals(Object o) {
            if (o instanceof IPair) {
                IPair p = (IPair) o;
                return p.a == a && p.b == b;
            }
            return false;
        }

        public int compareTo(IPair p) {
            int c = a.compareTo(p.a);
            if (c != 0) return c;
            return b.compareTo(p.b);
        }

        public int hashCode() {
            return (MagicNumbers.P + a.intValue()) * (MagicNumbers.Q + b.intValue());
        }

        public String toString() {
            return a + "<" + b;
        }
    }

    private static final class CanonOp implements Comparable<CanonOp> {
        private final int n;
        private final String s;

        public CanonOp(int n, String s) {
            super();
            this.n = n;
            this.s = s.intern();
        }

        public int intValue() {
            return n;
        }

        public String toString() {
            return s;
        }

        public int compareTo(CanonOp op) {
            if (n == op.n) return 0;
            if (n < op.n) return -1;
            return 1;
        }

        public boolean equals(Object o) {
            if (o instanceof CanonOp) {
                return (n == ((CanonOp) o).intValue());
            }
            return false;
        }

        public int hashCode() {
            return intValue();
        }
    }

    /* Grr!  These types ought to be singletons. */
    private static final Equal E = new Equal();
    private static final Lower L = new Lower();
    private static final Higher H = new Higher();
    private static final None N = new None();

    int nextRep = 1;         // Equivalence among short & long operator names.

    HashMap<String, CanonOp> rep; // Equivalence classes of com.sun.fortress.interpreter.unicode things.

    HashMap<CanonOp, CanonOp> bracket;
    // The right bracket class for each left bracket.  Every left bracket
    // must have an entry (except bracketmania brackets, which will
    // need to be programatically controlled).

    HashSet<CanonOp> rbracket;
    // Lookup table for right brackets.

    HashSet<CanonOp> nonAssociative;
    // Lookup table for non-associative operators.

    HashMap<CanonOp, CanonOp> eq;
    // Operators with equal precedence have equal values.

    HashSet<IPair> lt;
    // Set of pairs of classes (a,b) where a has lower precedence than b.

    HashMap<CanonOp, Set<CanonOp>> chain;
    // Map from classes of chaining operators to the set of classes
    // with which they may be chained (must be symmetrically closed).
    // Every chaining operator must have an entry.
    //
    // We could use a Useful.MultiMap here, but we end up having to
    // effectively re-roll mm.putItems in order to capture sharing of
    // rhs sets (ie we choose to copy only on mutation, and assume
    // arguments won't change).

    private void clear() {
        rep = new HashMap<String, CanonOp>();
        bracket = new HashMap<CanonOp, CanonOp>();
        rbracket = new HashSet<CanonOp>();
        nonAssociative = new HashSet<CanonOp>();
        eq = new HashMap<CanonOp, CanonOp>();
        lt = new HashSet<IPair>();
        chain = new HashMap<CanonOp, Set<CanonOp>>();
        insertEquivs();
        buildPrecedences();
        buildChainSets();
        buildBrackets();
        buildNonAssociatives();
    }

    /* For unit testing only. */
    public static void reset() {
        ONLY.clear();
    }

    private PrecedenceMap() {
        super();
        clear();
    }

    /**
     * *********************************************************
     * Public API
     */

    /* Is the given non-CAPS operator a valid operator? */
    public boolean isOperator(String a) {
        CanonOp ra = rep.get(a);
        return (ra != null && !bracket.containsKey(ra) && !rbracket.contains(ra));
    }

    /* Find the canonical name for the given operator or bracket.
     * In the common case that is the operator itself.
     */
    public String canon(String a) {
        CanonOp ra = rep.get(a);
        if (ra == null) {
            return a.intern();
        } else {
            return ra.toString();
        }
    }

    /**
     * Get the RealPrecedence between operator a and operator b.
     *
     * @return null if no precedence relationship exists.
     *         <p/>
     *         Use precedence() if you want a Precedence back.  Because the
     *         set of operators is open-ended, we indicate that no precedence
     *         relationship exists (by returning null) if a
     *         previously-unknown operator is involved.
     */
    public RealPrecedence get(String a, String b) {
        CanonOp ra = rep.get(a);
        CanonOp rb = rep.get(b);
        if (ra == null || rb == null) {
            if (a.equals(b)) {
                return E;
            } else {
                return null;
            }
        }
        if (ra.equals(rb)) return E;
        CanonOp e = eq.get(ra);
        if (e != null && e.equals(eq.get(rb))) return E;
        if (lt.contains(new IPair(ra, rb))) return L;
        if (lt.contains(new IPair(rb, ra))) return H;
        return null;
    }

    public boolean isNonAssociative(String s) {
        CanonOp op = rep.get(s);
        if (op != null) return nonAssociative.contains(op);
        return false;
    }

    public Precedence precedence(String a, String b) {
        RealPrecedence r = get(a, b);
        return (r == null) ? N : r;
    }

    /**
     * Is the operator a valid chaining operator?
     */
    public boolean isChain(String a) {
        CanonOp ra = rep.get(a);
        if (ra == null) return false;
        return chain.containsKey(ra);
    }

    /**
     * Can this collection of chaining operators be chained together
     * meaningfully?
     *
     * @throws NullPointerException if !isChain(op) for any input op.
     */
    public boolean isValidChaining(Collection<String> ops) {
        Set<CanonOp> reps = getSimilarSet(ops);
        for (CanonOp c_rep : reps) {
            if (!chain.get(c_rep).containsAll(reps)) return false;
        }
        return true;
    }

    /**
     * Remove '*' and '.' characters from a bracketmania bracket
     * cf spec 5.14.1 case 1.
     */
    private static String case5(String op) {
        /* First handle case 5 by casting out '.' and '*'. */
        if (op.length() < 3) return op;
        if (op.charAt(0) == '(' || op.charAt(op.length() - 1) == ')') {
            return op;
        }
        StringBuilder reduced = new StringBuilder(op.length());
        boolean canIg = false;
        for (int i = 0; i < op.length(); i++) {
            char c = op.charAt(i);
            if (canIg && (c == '.' || c == '*')) {
                canIg = false;
                continue;
            }
            reduced.append(c);
            canIg = true;
        }
        if (canIg) {
            return reduced.toString();
        } else {
            /* Trailing . or *; this is wrong. */
            return "";
        }
    }

    /**
     * Is the operator an encloser?
     */
    public boolean isEncloser(String op) {
        CanonOp r = rep.get(op);
        if (r != null) return bracket.containsKey(r) && rbracket.contains(r);

        /* Bracketmania!  Just need to recognize "|" sequences. */
        op = case5(op);
        int len = op.length();
        if (len < 2) return false;
        for (int i = 0; i < len; i++) {
            if (op.charAt(i) != '|') return false;
        }
        return true;
    }

    /**
     * Is the operator a left bracket?
     */
    public boolean isLeft(String op) {
        CanonOp r = rep.get(op);
        /* Special cases: (.< and ((.> */
        if (op.equals("(.<") || op.equals("((.>")) return true;
        /* Special cases: Oxford bracket and anything comment-like */
        if (op.equals("[\\")) return false;
        if (op.startsWith("(*")) return false;
        if (r != null) return bracket.containsKey(r); // Common case.

        /* Bracketmania!  See Spec 5.14.1.
         * lbracket = '|'+                                 (case 1)
         *          | ( "(." | '{' | '[' ) ( '/'+ | '\'+ ) (case 2)
         *          | '<'+ '|'+                            (case 3)
         *          | ( '<'+ | '|'+ ) ( '/'+ | '\'+ )      (case 4)
         *  Case 5: interleave '*' or '.' non-contiguously in above, ex "(".
         *
         * Simplifying a bit:
         *          | '<'* '|'+ (case 1/3)
         *          | ( "(." | '{' | '[' | '<'+ | '|'+ ) ('/'+ | '\'+)
         */
        op = case5(op);
        int len = op.length();
        if (len < 2) return false;
        int i = 0;
        char c = op.charAt(i++);
        switch (c) {
            case '<':
                while (c == '<' && i < len) {
                    c = op.charAt(i++);
                }
                if (c != '|') break;  // case 4
                /* Case 3 */
                while (c == '|' && i < len) {
                    c = op.charAt(i++);
                }
                return (c == '|' && i == len);
            case '|':
                while (c == '|' && i < len) {
                    c = op.charAt(i++);
                }
                if (c == '|' && i == len) return true; // case 1
                break; // case 4
            case '(':
                if (op.charAt(i++) != '.') return false;
                //$FALL-THROUGH$
            case '[':  
            case '{':
                c = op.charAt(i++);
                break;
            default:
                return false;
        }
        /* Case 2/4; look for sequences of slashes. */
        if (c != '/' && c != '\\') return false;
        if (i == len) return true;
        while (i < len && c == op.charAt(i++)) {
            if (i == len) return true;
        }
        return false;
    }

    /**
     * Is the operator a right bracket?
     */
    public boolean isRight(String op) {
        CanonOp r = rep.get(op);
        /* Special cases: >.) and <.)) */
        if (op.equals(">.)") || op.equals("<.))")) return true;
        /* Special cases: Oxford bracket and anything comment-like. */
        if (op.equals("\\]")) return false;
        if (op.endsWith("*)")) return false;
        if (r != null) return rbracket.contains(r);

        /* Bracketmania!  The reverse of the above code. */
        op = case5(op);
        int i = op.length();
        if (i < 2) return false;
        char c = op.charAt(--i);
        switch (c) {
            case '>':
                while (c == '>' && i > 0) {
                    c = op.charAt(--i);
                }
                if (c != '|') break;  // case 4
                /* Case 3 */
                while (c == '|' && i > 0) {
                    c = op.charAt(--i);
                }
                return (c == '|' && i == 0);
            case '|':
                while (c == '|' && i > 0) {
                    c = op.charAt(--i);
                }
                if (c == '|' && i == 0) return true; // case 1
                break;
            case ')':
                if (op.charAt(--i) != '.') return false;
              //$FALL-THROUGH$
            case ']':
            case '}':
                c = op.charAt(--i);
                break;
            default:
                return false;
        }
        /* Case 2/4; look for sequences of slashes. */
        if (c != '/' && c != '\\') return false;
        if (i == 0) return true;
        while (i > 0 && c == op.charAt(--i)) {
            if (i == 0) return true;
        }
        return false;
    }

    /**
     * Do the given brackets form a matched pair?  Assumes isLeft(l)
     * and isRight(r), though it is technically only necessary to
     * check one of these two conditions.
     */
    public boolean matchedBrackets(String l, String r) {
        /* "(.<" is paired with ">.)" */
        if (l.equals("(.<") && r.equals(">.)")) return true;
        /* "((.>" is paired with "<.))" */
        if (l.equals("((.>") && r.equals("<.))")) return true;
        if (l.startsWith("BIG ") && r.startsWith("BIG ")) return matchedBrackets(l.substring(4), r.substring(4));
        /* "{|->" is paired with "}" */
        if (l.endsWith("|->")) return matchedBrackets(l.substring(0, l.length() - 3), r);

        CanonOp le = rep.get(l);
        CanonOp re = rep.get(r);

        /* [// may be paired with /] and [/ may be paired with //] */
        if (le != null && re != null) {
            if ((l.equals("[//") && r.equals("/]")) || (l.equals("[/") && r.equals("//]"))) return true;
            else return bracket.get(le).equals(re);
        }
        /* Check bracketmania.  Don't parse, just look for matched pairs. */
        int len = r.length();
        if (l.length() != len) return false;
        boolean isOpposite = false;
        if (l.charAt(0) == '<' || l.charAt(0) == '|') isOpposite = true;
        for (int i = 0; i < len;) {
            char c = l.charAt(i++);
            char m = c;
            switch (c) {
                case '(':
                    m = ')';
                    break;
                case '[':
                    m = ']';
                    break;
                case '{':
                    m = '}';
                    break;
                case '<':
                    m = '>';
                    break;
                case '/':
                    if (isOpposite) m = '\\';
                    break;
                case '\\':
                    if (isOpposite) m = '/';
                    break;
                case '|':
                case '.':
                case '*':
                    break;
                default:
                    return false;
            }
            if (r.charAt(len - i) != m) return false;
        }
        /* Cache these brackets; if we define them we will likely use
         * them at least once! */
        makeBrackets(l, r);
        return true;
    }

    /**
     * *********************************************************
     * Equivalence class creation/lookup
     */

    private CanonOp getEquiv(String op) {
        CanonOp res = rep.get(op);
        if (res != null) return res;
        res = new CanonOp(nextRep++, op);
        rep.put(op, res);
        return res;
    }

    private void makeEquiv(String op1, String op2) {
        CanonOp i = getEquiv(op1);
        if (rep.put(op2, i) != null && !op1.equals(op2)) {
            bug("Duplicate equivalence of operator " + op2);
        }
    }

    private void insertEquivs() {
        for (Map.Entry<String, String> e : Operators.aliases.entrySet()) {
            makeEquiv(e.getValue(), e.getKey());
        }
    }

    /**
     * *********************************************************
     * Sets copied from precedence.ml.  c_n_m names refer to Unicode
     * section numbers, so David tells me.
     */

    static Set<String> c_2_1() {
        return Useful.union(Operators.p_c_2_1_multiplication, Operators.p_c_2_1_division);
    }

    static Set<String> c_2_2() {
        return Useful.union(VarArgs.make(Operators.p_multiplication_and_division,
                                         Operators.p_addition_and_subtraction,
                                         Operators.p_dot_above_multiplication_and_division,
                                         Operators.p_dot_above_addition_and_subtraction,
                                         Operators.p_tri_multiplication_and_division,
                                         Operators.p_tri_addition_and_subtraction,
                                         Operators.p_circled_multiplication_and_division,
                                         Operators.p_circled_addition_and_subtraction,
                                         Operators.p_squared_multiplication_and_division,
                                         Operators.p_squared_addition_and_subtraction,
                                         Operators.p_misc_addition));
    }

    static Set<String> c_2_3() {
        return Useful.set(VarArgs.make("MAX", "MIN", "REM", "MOD", "GCD", "LCM", "CHOOSE", "per"));
    }

    static Set<String> c_2_4() {
        return Useful.union(Operators.p_set_intersection, Operators.p_set_union);
    }

    static Set<String> c_2_5() {
        return Useful.union(Operators.p_square_intersection, Operators.p_square_union, Operators.p_misc_set);
    }

    static Set<String> c_2_6() {
        return Useful.union(Operators.p_curly_and, Operators.p_curly_or);
    }

    static Set<String> c_2_123() {
        return Useful.union(c_2_1(), c_2_2(), c_2_3());
    }

    static Set<String> c_2() {
        return Useful.union(c_2_123(), c_2_4(), c_2_5(), c_2_6());
    }

    static Set<String> c_3_1() {
        return Useful.union(Operators.p_equivalence_operators, Operators.p_inequivalence_operators);
    }

    static Set<String> c_3_2() {
        return Useful.union(Operators.p_less_than_operators,
                            Operators.p_greater_than_operators,
                            Operators.p_plain_comparison,
                            Useful.set(Operators.p_range_opr));
    }

    static Set<String> c_3_3() {
        return Useful.union(Operators.p_subset_comparison_operators,
                            Operators.p_superset_comparison_operators,
                            Operators.p_misc_set_comparison);
    }

    static Set<String> c_3_4() {
        return Useful.union(Operators.p_square_original_of, Operators.p_square_image_of, Operators.p_square_misc);
    }

    static Set<String> c_3_5() {
        return Useful.union(Operators.p_curly_precedes, Operators.p_curly_succeeds, Operators.p_curly_misc);
    }

    static Set<String> c_3_6() {
        return Useful.union(Operators.p_tri_contains, Operators.p_tri_subgroup, Operators.p_tri_misc);
    }

    static Set<String> c_3_7() {
        return Useful.union(Operators.p_chickenfoot_greater, Operators.p_chickenfoot_smaller);
    }

    static Set<String> c_3_8() {
        return Operators.p_relational_misc;
    }

    @SuppressWarnings ("unchecked")
    // varargs of generic
    static Set<String> c_3() {
        return Useful.union(VarArgs.make(c_3_1(),
                                         c_3_2(),
                                         Operators.p_range_opr,
                                         c_3_3(),
                                         c_3_4(),
                                         c_3_5(),
                                         c_3_6(),
                                         c_3_7(),
                                         c_3_8()));
    }

    static Set<String> boolean_misc() {
        return Useful.union(Operators.p_boolean_misc_nonAssociative, Operators.p_boolean_misc_leftAssociative);
    }

    static Set<String> c_4() {
        return Useful.union(Operators.p_boolean_conjunction, Operators.p_boolean_disjunction, boolean_misc());
    }

    /**
     * *********************************************************
     * Setup of precedence
     */

    private void eqPrec(Set<String> eqv) {
        CanonOp r = null;
        for (String op : eqv) {
            CanonOp e = getEquiv(op);
            if (r == null) r = e;
            if (eq.put(e, r) != null) {
                bug("Operator already in precedence table " + op);
            }
        }
    }

    private void ordPrec(Set<String> eqvHi, Set<String> eqvLo) {
        for (String opHi : eqvHi) {
            CanonOp pHi = getEquiv(opHi);
            for (String opLo : eqvLo) {
                CanonOp pLo = getEquiv(opLo);
                lt.add(new IPair(pLo, pHi));
            }
        }
    }

    private void buildPrecedences() {
        /* Operator sets whose precedences are all equal. */
        eqPrec(Operators.p_addition_and_subtraction);
        eqPrec(Operators.p_dot_above_addition_and_subtraction);
        eqPrec(Operators.p_dot_below_addition_and_subtraction);
        eqPrec(Operators.p_tri_addition_and_subtraction);
        eqPrec(Operators.p_circled_addition_and_subtraction);
        eqPrec(Operators.p_squared_addition_and_subtraction);
        eqPrec(Operators.p_range_opr);

        /* Each list is ordered from left to right, and is transitive
           There is no ordering between the lists. */
        ordPrec(Operators.p_multiplication_and_division, Operators.p_addition_and_subtraction);
        ordPrec(Operators.p_dot_above_multiplication_and_division, Operators.p_dot_above_addition_and_subtraction);
        ordPrec(Operators.p_tri_multiplication_and_division, Operators.p_tri_addition_and_subtraction);
        ordPrec(Operators.p_circled_multiplication_and_division, Operators.p_circled_addition_and_subtraction);
        ordPrec(Operators.p_squared_multiplication_and_division, Operators.p_squared_addition_and_subtraction);
        ordPrec(Operators.p_set_intersection, Operators.p_set_union);
        ordPrec(Operators.p_square_intersection, Operators.p_square_union);
        ordPrec(Operators.p_curly_and, Operators.p_curly_or);
        ordPrec(Operators.p_boolean_conjunction, Operators.p_boolean_disjunction);
        ordPrec(c_2(), c_3_1());
        ordPrec(c_2_123(), c_3_2());
        ordPrec(c_2_4(), c_3_3());
        ordPrec(c_2_5(), c_3_4());
        ordPrec(c_2_6(), c_3_5());
        ordPrec(c_3(), c_4());
    }

    private Set<CanonOp> getSimilarSet(Collection<String> ops) {
        Set<CanonOp> result = new HashSet<CanonOp>();
        for (String op : ops) {
            result.add(getEquiv(op));
        }
        return result;
    }

    private void chainSet(Set<String> c) {
        Set<CanonOp> reps = getSimilarSet(c);
        for (CanonOp r : reps) {
            Set<CanonOp> rs = chain.get(r);
            /* For compactness, we try to use a single copy of reps
             * for all members of the set.  This requires gratuitous
             * copying for operators that are in more than three
             * chaining classes---but that is basically only equality
             * and its ken. */
            if (rs == null) {
                rs = reps;
            } else {
                rs = Useful.union(rs, reps);
            }
            chain.put(r, Collections.unmodifiableSet(rs));
        }
    }

    private void buildChainSets() {
        chainSet(Useful.union(Operators.p_equivalence_operators, Operators.p_less_than_operators));
        chainSet(Useful.union(Operators.p_equivalence_operators, Operators.p_greater_than_operators));
        chainSet(Useful.union(Operators.p_equivalence_operators, Operators.p_subset_comparison_operators));
        chainSet(Useful.union(Operators.p_equivalence_operators, Operators.p_superset_comparison_operators));
        chainSet(Useful.union(Operators.p_equivalence_operators, Operators.p_square_original_of));
        chainSet(Useful.union(Operators.p_equivalence_operators, Operators.p_square_image_of));
        chainSet(Useful.union(Operators.p_equivalence_operators, Operators.p_curly_precedes));
        chainSet(Useful.union(Operators.p_equivalence_operators, Operators.p_curly_succeeds));
        chainSet(Useful.union(Operators.p_equivalence_operators, Operators.p_chickenfoot_greater));
        chainSet(Useful.union(Operators.p_equivalence_operators, Operators.p_chickenfoot_smaller));
        chainSet(Useful.union(Operators.p_equivalence_operators, Operators.p_tri_subgroup));
        chainSet(Useful.union(Operators.p_equivalence_operators, Operators.p_tri_contains));
    }

    private void makeBrackets(String l, String r) {
        CanonOp le = getEquiv(l);
        CanonOp re = getEquiv(r);
        if (bracket.put(le, re) != null) {
            bug("Second matching bracket for " + l + " is " + r);
        }
        if (!rbracket.add(re)) {
            bug("Right bracket " + r + " already registered with different left bracket");
        }
    }

    private void buildBrackets() {
        for (Map.Entry<String, String> e : Operators.l2r.entrySet()) {
            makeBrackets(e.getKey(), e.getValue());
        }
    }

    static Set<String> nonassociative() {
        return Useful.union(VarArgs.make(Operators.p_c_2_1_division, Useful.set(VarArgs.make("REM",
                                                                                             "MOD",
                                                                                             "CHOOSE",
                                                                                             "per")),
                                         Operators.p_misc_set,
                                         Operators.p_inequivalence_operators,
                                         Operators.p_plain_comparison,
                                         Useful.set(Operators.p_range_opr),
                                         Operators.p_misc_set_comparison,
                                         Operators.p_square_misc,
                                         Operators.p_curly_misc,
                                         Operators.p_tri_misc,
                                         c_3_8(),
                                         Operators.p_boolean_misc_nonAssociative,
                                         Operators.p_other_operators));
    }

    private void buildNonAssociatives() {
        for (String s : nonassociative()) {
            if (!nonAssociative.add(getEquiv(s))) bug("The operator " + s + " already registered.");
        }
    }
}
