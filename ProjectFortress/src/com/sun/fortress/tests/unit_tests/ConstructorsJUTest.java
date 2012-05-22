/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.tests.unit_tests;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.useful.Useful;
import junit.framework.Assert;

import java.math.BigInteger;
import java.util.Collections;

public class ConstructorsJUTest extends com.sun.fortress.useful.TestCaseWrapper {
    public void testSourceLoc() {
        SourceLoc sl1 = new SourceLocRats("cat", 1, 2, 0);
        SourceLoc sl2 = new SourceLocRats("cat", 1, 2, 0);
        SourceLoc sl3 = new SourceLocRats("dog", 1, 2, 0);
        SourceLoc sl4 = new SourceLocRats("cat", 1, 3, 0);
        SourceLoc sl5 = new SourceLocRats("cat", 2, 2, 0);
        Assert.assertEquals("cat", sl1.getFileName());
        Assert.assertEquals(1, sl1.getLine());
        Assert.assertEquals(2, sl1.column());
        Assert.assertEquals(sl1, sl2);
        Assert.assertEquals(sl1.hashCode(), sl2.hashCode());

        Assert.assertFalse(sl1.equals(sl3));
        Assert.assertFalse(sl1.equals(sl4));
        Assert.assertFalse(sl1.equals(sl5));
    }

    public void testSpan() {
        SourceLoc sl1a = new SourceLocRats("cat", 1, 2, 0);
        SourceLoc sl2a = new SourceLocRats("cat", 3, 4, 0);
        SourceLoc sl1b = new SourceLocRats("cat", 1, 2, 0);
        SourceLoc sl2b = new SourceLocRats("cat", 3, 4, 0);
        Span span1 = new Span(sl1a, sl2a);
        Span span2 = new Span(sl1b, sl2b);
        Span span3 = new Span(sl2a, sl1a);
        Assert.assertEquals(span1, span2);
        Assert.assertFalse(span1.equals(span3));
    }

    Span newSpan(String f, int l, int c1, int c2) {
        return new Span(new SourceLocRats(f, l, c1, 0), new SourceLocRats(f, l, c2, 0));
    }


    public void testId() {
        Id id1 = NodeFactory.makeId(newSpan("cat", 1, 2, 3), "TheIdString");
        Id id2 = NodeFactory.makeId(newSpan("cat", 1, 2, 3), "TheIdString");
        Id id3 = NodeFactory.makeId(newSpan("cat", 1, 2, 999), "TheIdString");
        Id id4 = NodeFactory.makeId(newSpan("cat", 1, 2, 3), "AnotherString");
        Assert.assertEquals(id1, id2);
        Assert.assertTrue(id1.equals(id3)); // We ignore Sourceloc for equality
        Assert.assertFalse(id1.equals(id4));
        Assert.assertEquals(id1.getText(), "TheIdString");
    }

    Id newId(String id, int l, int c1, int c2) {
        return NodeFactory.makeId(newSpan("somefile", l, c1, c2), id);
    }

    Id newId(String id) {
        return NodeFactory.makeId(newSpan("somefile", 1, 2, 3), id);
    }

    public void testAPIName() {
        Span span11 = newSpan("cat", 1, 2, 3);
        Span span12 = newSpan("cat", 1, 2, 3);
        newSpan("cat", 2, 4, 5);
        APIName di1 = NodeFactory.makeAPIName(span11, newId("snert"));
        APIName di2 = NodeFactory.makeAPIName(span11, newId("snert"));
        APIName di3 = NodeFactory.makeAPIName(span11, newId("snort"));
        Assert.assertEquals(di1, di2);
        Assert.assertEquals(di1.hashCode(), di2.hashCode());
        Assert.assertFalse(di1.equals(di3));
        di1.hashCode();
        di3.hashCode();
        Assert.assertFalse(di1.hashCode() == di3.hashCode());

        APIName di4 = NodeFactory.makeAPIName(span12, Useful.list(newId("foo"), newId("bar"), newId("baz")));
        APIName di5 = NodeFactory.makeAPIName(span12, Useful.list(newId("foo"), newId("bar"), newId("bar")));
        APIName di6 = NodeFactory.makeAPIName(span12, Useful.list(newId("foo"), newId("baz"), newId("baz")));
        APIName di7 = NodeFactory.makeAPIName(span12, Useful.list(newId("bar"), newId("bar"), newId("baz")));

        Assert.assertEquals("foo.bar.baz", NodeUtil.nameString(di4));
        Assert.assertEquals(di4.hashCode(), di4.hashCode());

        Assert.assertFalse(di4.equals(di5));
        Assert.assertFalse(di4.equals(di6));
        Assert.assertFalse(di4.equals(di7));
    }

    APIName newAPIName(String a, String b, String c) {
        Span span1 = newSpan("cat", 1, 2, 3);
        return NodeFactory.makeAPIName(span1, Useful.list(newId(a), newId(b), newId(c)));
    }

    /**
     * @param e1
     * @param e2
     * @param e3
     */
    private void een(Object e1, Object e2, Object e3) {
        Assert.assertEquals(e1, e2);
        Assert.assertEquals(e1.hashCode(), e2.hashCode());
        Assert.assertFalse(e1.equals(e3));
        Assert.assertFalse(e1.hashCode() == e3.hashCode());
    }

    private void nnn(Object e1, Object e2, Object e3) {
        Assert.assertFalse(e1.hashCode() == e2.hashCode());
        Assert.assertFalse(e1.hashCode() == e3.hashCode());
        Assert.assertFalse(e2.hashCode() == e3.hashCode());

        Assert.assertFalse(e1.equals(e2));
        Assert.assertFalse(e1.equals(e3));
        Assert.assertFalse(e2.equals(e3));
    }


    public void testLiterals() {
        Span span1 = newSpan("cat", 1, 2, 3);
        Span span2 = newSpan("cat", 1, 2, 3);
        StringLiteralExpr sl1 = ExprFactory.makeStringLiteralExpr(span1, "123");
        StringLiteralExpr sl2 = ExprFactory.makeStringLiteralExpr(span2, "123");
        StringLiteralExpr sl3 = ExprFactory.makeStringLiteralExpr(span1, "124");

        een(sl1, sl2, sl3);

        IntLiteralExpr il1 = ExprFactory.makeIntLiteralExpr(span1, "123");
        IntLiteralExpr il2 = ExprFactory.makeIntLiteralExpr(span2, "123");
        IntLiteralExpr il3 = ExprFactory.makeIntLiteralExpr(span1, "124");

        een(il1, il2, il3);

        IntLiteralExpr il4 = ExprFactory.makeIntLiteralExpr(span1, new BigInteger("123"));

        Assert.assertEquals(il1, il4);

        FloatLiteralExpr fl1 = ExprFactory.makeFloatLiteralExpr(span1, "123");
        FloatLiteralExpr fl2 = ExprFactory.makeFloatLiteralExpr(span2, "123");
        FloatLiteralExpr fl3 = ExprFactory.makeFloatLiteralExpr(span1, "124");
        een(fl1, fl2, fl3);

        nnn(sl1, il1, fl1);

        FloatLiteralExpr fl4 = ExprFactory.makeFloatLiteralExpr(span1, "123.0");
        Assert.assertEquals(fl1, fl4);
    }


    StringLiteralExpr newString(String s) {
        Span span1 = newSpan("cat", 1, 2, 3);
        return ExprFactory.makeStringLiteralExpr(span1, s);
    }

    IntLiteralExpr newInt(String s) {
        Span span1 = newSpan("dog", 1, 2, 3);
        return ExprFactory.makeIntLiteralExpr(span1, s);
    }

    FloatLiteralExpr newFloat(String s) {
        Span span1 = newSpan("emu", 1, 2, 3);
        return ExprFactory.makeFloatLiteralExpr(span1, s);
    }

    public void testVarRef() {
        Span span1 = newSpan("dog", 1, 2, 3);
        Span span2 = newSpan("cat", 1, 2, 3);
        VarRef v1 = ExprFactory.makeVarRef(span1, "cat");
        VarRef v2 = ExprFactory.makeVarRef(span2, "cat");
        VarRef v3 = ExprFactory.makeVarRef(span1, "dog");

        een(v1, v2, v3);
    }

    VarRef newVar(String s) {
        Span span1 = newSpan("dog", 1, 2, 3);
        VarRef v1 = ExprFactory.makeVarRef(span1, "cat");
        return v1;
    }

    public void testTuples() {
        Span span1 = newSpan("dog", 1, 2, 3);
        Span span2 = newSpan("cat", 1, 2, 3);
        TupleExpr t1 = ExprFactory.makeTupleExpr(span1, Useful.<Expr>list(newString("cat"), newString("dog")));
        TupleExpr t2 = ExprFactory.makeTupleExpr(span2, Useful.<Expr>list(newString("cat"), newString("dog")));
        TupleExpr t3 = ExprFactory.makeTupleExpr(span1, Useful.<Expr>list(newString("car"), newString("bog")));
        een(t1, t2, t3);

        try {
            ExprFactory.makeTupleExpr(span1, Collections.<Expr>emptyList());
            Assert.fail("Should have thrown exception, empty list not allowed");
        }
        catch (Error e) {

        }
    }

    public void testVoid() {
        Span span1 = newSpan("dog", 1, 2, 3);
        Span span2 = newSpan("cat", 1, 2, 3);
        VoidLiteralExpr v1 = ExprFactory.makeVoidLiteralExpr(span1);
        VoidLiteralExpr v2 = ExprFactory.makeVoidLiteralExpr(span2);
        Assert.assertEquals(v1, v2);
    }


}
