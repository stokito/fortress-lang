/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_resolver;

import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.parser_util.precedence_opexpr.*;
import com.sun.fortress.useful.Useful;

import java.util.List;
import java.util.Set;


public class PrecedenceMapJUTest extends com.sun.fortress.useful.TestCaseWrapper {
    final PrecedenceMap pm = PrecedenceMap.ONLY;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PrecedenceMapJUTest.class);
    }

    public PrecedenceMapJUTest() {
        super();
    }

    public void usual() {
        PrecedenceMap.reset();
    }

    Equal equal_precedence = new Equal();
    Higher higher_precedence = new Higher();
    Lower lower_precedence = new Lower();
    None no_precedence = new None();

    /* Unit test for missed operator. */
    public void testImplies() {
        assertEquals(true, pm.isOperator("=>"));
    }

    public void testIsOperator() {
        for (String op : Operators.ops) {
            if (NodeUtil.validOp(op)) continue;
            assertEquals(op, true, pm.isOperator(op));
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
        assertEquals(equal_precedence, pm.precedence("BOGUS", "BOGUS"));
        assertEquals(no_precedence, pm.precedence("BOGUS", "BOGUS2"));
        for (int i = 0; i < ops.length; i++) {
            String preOp = ops[i];
            assertEquals(no_precedence, pm.precedence("BOGUS", preOp));
            assertEquals(no_precedence, pm.precedence(preOp, "BOGUS"));
            assertEquals(equal_precedence, pm.precedence(preOp, preOp));
            for (int j = i; j < ops.length; j++) {
                String preOp1 = ops[j];
                Precedence f = pm.precedence(preOp, preOp1);
                Precedence r = pm.precedence(preOp1, preOp);
                if (f == equal_precedence) assertEquals(equal_precedence, r);
                if (f == higher_precedence) assertEquals(lower_precedence, r);
                if (f == lower_precedence) assertEquals(higher_precedence, r);
                if (f == no_precedence) assertEquals(no_precedence, r);
            }
        }
    }

    public void testEqPrecedences() {
        /* Just spot test. */
        for (String i : Operators.p_addition_and_subtraction) {
            for (String op : Operators.ops) {
                Precedence p = pm.precedence(i, op);
                assertEquals(i + " and " + op + " is " + p,
                             Operators.p_addition_and_subtraction.contains(op),
                             equal_precedence.equals(p));
            }
        }
    }

    public void testOrdPrecedences() {
        /* Another spot test. */
        Set[][] spot = {
                {
                        Operators.p_multiplication_and_division, Operators.p_addition_and_subtraction
                }, {
                        Operators.p_boolean_conjunction, Operators.p_boolean_disjunction
                }, {PrecedenceMap.c_2_123(), PrecedenceMap.c_3_2()}
        };
        for (int i = 0; i < spot.length; i++) {
            Set[] s = spot[i];
            String cl0 = (String) s[0].iterator().next();
            String cl1 = (String) s[1].iterator().next();
            for (String op0 : Operators.ops) {
                if (!equal_precedence.equals(pm.precedence(cl0, op0))) continue;
                for (String op1 : Operators.ops) {
                    Precedence ee = pm.precedence(cl1, op1);
                    Precedence hh = pm.precedence(op0, op1);
                    if (equal_precedence.equals(ee)) assertEquals(true, higher_precedence.equals(hh));
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
                assertEquals(true, pm.isChain(op));
            }
        }
        for (String eqv : Operators.p_equivalence_operators) {
            for (String op : Operators.ops) {
                if (pm.isChain(op)) assertEquals(true, pm.isValidChaining(Useful.list(op, eqv)));
            }
        }
    }

    public void testLBrak() {
        for (String brak : Operators.left) {
            assertEquals(true, pm.isLeft(brak));
        }
    }

    public void testRBrak() {
        for (String brak : Operators.right) {
            assertEquals(true, pm.isRight(brak));
        }
    }

    public void testEBrak() {
        for (String brak : Operators.enclosing) {
            assertEquals(true, pm.isLeft(brak));
            assertEquals(true, pm.isRight(brak));
        }
    }

    public void testBraks() {
        for (String lbrak : Operators.left) {
            String rbrak = Operators.l2r.get(lbrak);
            assertEquals("" + lbrak, false, rbrak == null);
            assertEquals(true, pm.matchedBrackets(lbrak, rbrak));
        }
        for (String encl : Operators.enclosing) {
            String rbrak = Operators.l2r.get(encl);
            assertEquals(encl, rbrak);
            assertEquals(true, pm.matchedBrackets(encl, rbrak));
        }
    }

    public void testManiaLeft() {
        assertEquals(false, pm.isLeft(""));
        assertEquals(false, pm.isLeft("<"));
        assertEquals(false, pm.isLeft(".|"));
        assertEquals(false, pm.isLeft("|*"));
        assertEquals(false, pm.isLeft("|("));
        assertEquals(false, pm.isLeft("||."));
        assertEquals(false, pm.isLeft("*||"));
        assertEquals(false, pm.isLeft("<<<<<"));
        assertEquals(false, pm.isLeft("<|/"));
        assertEquals(false, pm.isLeft("(./\\"));
        assertEquals(false, pm.isLeft("(|"));
    }

    public void testManiaRight() {
        assertEquals(false, pm.isRight(""));
        assertEquals(false, pm.isRight(">"));
        assertEquals(false, pm.isRight("|."));
        assertEquals(false, pm.isRight("*|"));
        assertEquals(false, pm.isRight(")|"));
        assertEquals(false, pm.isRight("||."));
        assertEquals(false, pm.isRight("*||"));
        assertEquals(false, pm.isRight(">>>>>"));
        assertEquals(false, pm.isRight("/|>"));
        assertEquals(false, pm.isRight("/\\.)"));
        assertEquals(false, pm.isRight("|)"));
    }

    private void maniaPair(String l, String r) {
        assertEquals(true, pm.isLeft(l));
        assertEquals(true, pm.isRight(r));
        assertEquals(true, pm.matchedBrackets(l, r));
    }

    public void testManiaPair() {
        maniaPair("|.|||*|.||", "||.|*|||.|"); // case 1
        maniaPair("(.//////", "//////.)"); // case 2
        maniaPair("{.///*/.//", "//./*///.}"); // case 2
        maniaPair("[.\\\\\\*\\.\\\\", "\\\\.\\*\\\\\\.]"); // case 2
        maniaPair("<.<<|*|.||", "||.|*|>>.>"); // case 3
        maniaPair("<.<</*/.//", "\\\\.\\*\\>>.>"); // case 4
        maniaPair("|.||\\*\\.\\\\", "//./*/||.|"); // case 4
    }

    public void testEnclNotOp() {
        for (String lbrak : Operators.left) {
            assertEquals(lbrak, false, pm.isOperator(lbrak));
        }
        for (String rbrak : Operators.right) {
            assertEquals(rbrak, false, pm.isOperator(rbrak));
        }
        for (String encl : Operators.enclosing) {
            assertEquals(encl, false, pm.isOperator(encl));
        }
    }
}
