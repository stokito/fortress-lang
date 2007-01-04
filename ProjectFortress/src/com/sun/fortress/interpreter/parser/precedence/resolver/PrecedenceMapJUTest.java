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

package com.sun.fortress.interpreter.parser.precedence.resolver;

import junit.framework.TestCase;

import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import com.sun.fortress.interpreter.parser.FortressUtil;
import com.sun.fortress.interpreter.parser.precedence.opexpr.Equal;
import com.sun.fortress.interpreter.parser.precedence.opexpr.Higher;
import com.sun.fortress.interpreter.parser.precedence.opexpr.Lower;
import com.sun.fortress.interpreter.parser.precedence.opexpr.None;
import com.sun.fortress.interpreter.parser.precedence.opexpr.Precedence;
import com.sun.fortress.interpreter.parser.precedence.opexpr.RealPrecedence;
import com.sun.fortress.interpreter.useful.Useful;


public class PrecedenceMapJUTest extends TestCase {
    final PrecedenceMap pm = PrecedenceMap.T;

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(PrecedenceMapJUTest.class);
    }

    public PrecedenceMapJUTest() {
        super();
    }

    public void usual() {
        PrecedenceMap.reset();
    }

    Equal e = new Equal();
    Higher h = new Higher();
    Lower l = new Lower();
    None n = new None();

    public void testIsOperator() {
        for (String op : Operators.ops) {
            if (FortressUtil.validOp(op)) continue;
            assertEquals(op,true,pm.isOperator(op));
        }
    }

    public void testAllPrecedence() {
        /* Ensure that some precedence relationship holds between all
         * operators, and that when we commute it's consistent.
         *
         * Using an array here looks ucky, but there are enough
         * operators that halving the execution time of this O(n^2)
         * test by avoiding redundancy is worthwhile. */
        String ops[] = Operators.ops.toArray(new String[0]);
        assertEquals(e,pm.precedence("BOGUS","BOGUS"));
        assertEquals(n,pm.precedence("BOGUS","BOGUS2"));
        for (int i=0; i < ops.length; i++) {
            String preOp = ops[i];
            assertEquals(n,pm.precedence("BOGUS",preOp));
            assertEquals(n,pm.precedence(preOp,"BOGUS"));
            assertEquals(e,pm.precedence(preOp,preOp));
            for (int j=i; j < ops.length; j++) {
                String preOp1 = ops[j];
                Precedence f = pm.precedence(preOp, preOp1);
                Precedence r = pm.precedence(preOp1, preOp);
                if (f==e) assertEquals(e,r);
                if (f==h) assertEquals(l,r);
                if (f==l) assertEquals(h,r);
                if (f==n) assertEquals(n,r);
            }
        }
    }
    public void testEqPrecedences() {
        /* Just spot test. */
        for (Set<String> s :
                 Useful.list(Operators.p_circled_multiplication_and_division,
                             Operators.p_addition_and_subtraction,
                             Operators.p_boolean_disjunction)) {
            for (String i : s) {
                for (String op : Operators.ops) {
                    Precedence p = pm.precedence(i,op);
                    assertEquals(i+" and "+op+" is "+p, s.contains(op),
                                 e.equals(p));
                }
            }
        }
    }
    public void testOrdPrecedences() {
        /* Another spot test. */
        Set[][] spot =
            {{Operators.p_multiplication_and_division,
              Operators.p_addition_and_subtraction},
             {Operators.p_boolean_conjunction,
              Operators.p_boolean_disjunction},
             {PrecedenceMap.c_2_123(), PrecedenceMap.c_3_2()}};
        for (int i = 0; i < spot.length; i++) {
            Set[] s = spot[i];
            String cl0 = (String)s[0].iterator().next();
            String cl1 = (String)s[1].iterator().next();
            for (String op0 : Operators.ops) {
                if (!e.equals(pm.precedence(cl0,op0))) continue;
                for (String op1 : Operators.ops) {
                    Precedence ee = pm.precedence(cl1,op1);
                    Precedence hh = pm.precedence(op0,op1);
                    if (e.equals(ee)) assertEquals(true, h.equals(hh));
                }
            }
        }
    }
    private List<Set<String>> chainSpots() {
        return Useful.list(Operators.p_less_than_operators,
                           Operators.p_greater_than_operators,
                           Operators.p_superset_comparison_operators,
                           Operators.p_equivalence_operators);
    }
    private Set<String> chainSpotsL() {
        return Useful.union(Operators.p_less_than_operators,
                            Operators.p_greater_than_operators,
                            Operators.p_superset_comparison_operators);
    }
    public void testIsChain() {
        /* Spot test. */
        for (Set<String> s : chainSpots()) {
            for (String op : s) {
                assertEquals(true,pm.isChain(op));
            }
        }
        for (String eqv : Operators.p_equivalence_operators) {
            for (String op : Operators.ops) {
                if (pm.isChain(op))
                    assertEquals(true,
                                 pm.isValidChaining
                                   (Useful.list(op,eqv)));
            }
        }
    }
    public void testLBrak() {
        for (String brak : Operators.left) {
            assertEquals(true,pm.isLeft(brak));
        }
    }
    public void testRBrak() {
        for (String brak : Operators.right) {
            assertEquals(true,pm.isRight(brak));
        }
    }
    public void testEBrak() {
        for (String brak : Operators.enclosing) {
            assertEquals(true,pm.isLeft(brak));
            assertEquals(true,pm.isRight(brak));
        }
    }
    public void testBraks() {
        for (String lbrak : Operators.left) {
            String rbrak = Operators.l2r.get(lbrak);
            assertEquals(""+lbrak, false, rbrak==null);
            assertEquals(true,pm.matchedBrackets(lbrak,rbrak));
        }
        for (String encl : Operators.enclosing) {
            String rbrak = Operators.l2r.get(encl);
            assertEquals(encl, rbrak);
            assertEquals(true,pm.matchedBrackets(encl,rbrak));
        }
    }
    public void testManiaLeft() {
        assertEquals(false,pm.isLeft(""));
        assertEquals(false,pm.isLeft("<"));
        assertEquals(false,pm.isLeft(".|"));
        assertEquals(false,pm.isLeft("|*"));
        assertEquals(false,pm.isLeft("|("));
        assertEquals(false,pm.isLeft("||."));
        assertEquals(false,pm.isLeft("*||"));
        assertEquals(false,pm.isLeft("<<<<<"));
        assertEquals(false,pm.isLeft("<|/"));
        assertEquals(false,pm.isLeft("(/\\"));
        assertEquals(false,pm.isLeft("(|"));
    }
    public void testManiaRight() {
        assertEquals(false,pm.isRight(""));
        assertEquals(false,pm.isRight(">"));
        assertEquals(false,pm.isRight("|."));
        assertEquals(false,pm.isRight("*|"));
        assertEquals(false,pm.isRight(")|"));
        assertEquals(false,pm.isRight("||."));
        assertEquals(false,pm.isRight("*||"));
        assertEquals(false,pm.isRight(">>>>>"));
        assertEquals(false,pm.isRight("/|>"));
        assertEquals(false,pm.isRight("/\\)"));
        assertEquals(false,pm.isRight("|)"));
    }
    private void maniaPair(String l, String r) {
        assertEquals(true,pm.isLeft(l));
        assertEquals(true,pm.isRight(r));
        assertEquals(true,pm.matchedBrackets(l,r));
    }
    public void testManiaPair() {
        maniaPair("|.|||*|.||",       "||.|*|||.|"); // case 1
        maniaPair("(//////",       "\\\\\\\\\\\\)"); // case 2
        maniaPair("{.///*/.//",       "\\\\.\\*\\\\\\.}"); // case 2
        maniaPair("[.\\\\\\*\\.\\\\", "//./*///.]"); // case 2
        maniaPair("<.<<|*|.||",       "||.|*|>>.>"); // case 3
        maniaPair("<.<</*/.//",       "\\\\.\\*\\>>.>"); // case 4
        maniaPair("|.||\\*\\.\\\\",   "//./*/||.|"); // case 4
    }
    public void testEnclNotOp() {
        for (String lbrak : Operators.left) {
            assertEquals(lbrak,false,pm.isOperator(lbrak));
        }
        for (String rbrak : Operators.right) {
            assertEquals(rbrak,false,pm.isOperator(rbrak));
        }
        for (String encl : Operators.enclosing) {
            assertEquals(encl,false,pm.isOperator(encl));
        }
    }
}
