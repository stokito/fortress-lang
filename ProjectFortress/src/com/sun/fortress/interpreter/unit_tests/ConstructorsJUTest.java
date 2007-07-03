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

package com.sun.fortress.interpreter.unit_tests;

import java.math.BigInteger;
import java.util.Collections;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.sun.fortress.interpreter.nodes.DottedId;
import com.sun.fortress.interpreter.nodes.Expr;
import com.sun.fortress.interpreter.nodes.FloatLiteral;
import com.sun.fortress.interpreter.nodes.Id;
import com.sun.fortress.interpreter.nodes.ImportStar;
import com.sun.fortress.interpreter.nodes.IntLiteral;
import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.nodes.StringLiteral;
import com.sun.fortress.interpreter.nodes.TupleExpr;
import com.sun.fortress.interpreter.nodes.VarRefExpr;
import com.sun.fortress.interpreter.nodes.VoidLiteral;
import com.sun.fortress.interpreter.nodes_util.NodeFactory;
import com.sun.fortress.interpreter.parser.FortressUtil;
import com.sun.fortress.interpreter.useful.Useful;

public class ConstructorsJUTest extends com.sun.fortress.interpreter.useful.TcWrapper  {
    public void testSourceLoc() {
        SourceLoc sl1 = new SourceLocRats("cat", 1, 2);
        SourceLoc sl2 = new SourceLocRats("cat", 1, 2);
        SourceLoc sl3 = new SourceLocRats("dog", 1, 2);
        SourceLoc sl4 = new SourceLocRats("cat", 1, 3);
        SourceLoc sl5 = new SourceLocRats("cat", 2, 2);
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
        SourceLoc sl1a = new SourceLocRats("cat", 1, 2);
        SourceLoc sl2a = new SourceLocRats("cat", 3, 4);
        SourceLoc sl1b = new SourceLocRats("cat", 1, 2);
        SourceLoc sl2b = new SourceLocRats("cat", 3, 4);
        Span span1 = new Span(sl1a, sl2a);
        Span span2 = new Span(sl1b, sl2b);
        Span span3 = new Span(sl2a, sl1a);
        Assert.assertEquals(span1, span2);
        Assert.assertFalse(span1.equals(span3));
    }

    Span newSpan(String f, int l, int c1, int c2) {
        return new Span(new SourceLocRats(f,l,c1), new SourceLocRats(f,l,c2));
    }


    public void testId() {
        Id id1 = new Id(newSpan("cat", 1, 2, 3), "TheIdString");
        Id id2 = new Id(newSpan("cat", 1, 2, 3), "TheIdString");
        Id id3 = new Id(newSpan("cat", 1, 2, 999), "TheIdString");
        Id id4 = new Id(newSpan("cat", 1, 2, 3), "AnotherString");
        Assert.assertEquals(id1, id2);
        Assert.assertTrue(id1.equals(id3)); // We ignore Sourceloc for equality
        Assert.assertFalse(id1.equals(id4));
        Assert.assertEquals(id1.getName(), "TheIdString");
    }

    Id newId(String id, int l, int c1, int c2) {
        return new Id(newSpan("somefile", l, c1, c2), id);
    }
    Id newId(String id) {
        return new Id(newSpan("somefile", 1, 2, 3), id);
    }
    public void testDottedId() {
        Span span11 = newSpan("cat", 1, 2, 3);
        Span span12 = newSpan("cat", 1, 2, 3);
        Span span13 = newSpan("cat", 2, 4, 5);
        DottedId di1 = new DottedId(span11, newId("snert"));
        DottedId di2 = new DottedId(span11, newId("snert"));
        DottedId di3 = new DottedId(span11, newId("snort"));
        Assert.assertEquals(di1, di2);
        Assert.assertEquals(di1.hashCode(), di2.hashCode());
        Assert.assertFalse(di1.equals(di3));
        Assert.assertFalse(di1.hashCode() == di3.hashCode());

        DottedId di4 = new DottedId(span12, newId("foo"), Useful.list(newId("bar"), newId("baz")));
        DottedId di5 = new DottedId(span12, newId("foo"), Useful.list(newId("bar"), newId("bar")));
        DottedId di6 = new DottedId(span12, newId("foo"), Useful.list(newId("baz"), newId("baz")));
        DottedId di7 = new DottedId(span12, newId("bar"), Useful.list(newId("bar"), newId("baz")));

        Assert.assertEquals("foo.bar.baz", di4.toString());
        Assert.assertEquals(di4.hashCode(), di4.hashCode());

        Assert.assertFalse(di4.equals(di5));
        Assert.assertFalse(di4.equals(di6));
        Assert.assertFalse(di4.equals(di7));
    }

    DottedId newDottedId(String a, String b, String c) {
        Span span1 = newSpan("cat", 1, 2, 3);
        return new DottedId(span1, newId(a), Useful.list(newId(b), newId(c)));
    }

    public void testExport() {
        Span span1 = newSpan("cat", 1, 2, 3);
        com.sun.fortress.interpreter.nodes.Export e1 = new com.sun.fortress.interpreter.nodes.Export(span1, FortressUtil.mkList(newDottedId("some", "exported", "apiname")));
        com.sun.fortress.interpreter.nodes.Export e2 = new com.sun.fortress.interpreter.nodes.Export(span1, FortressUtil.mkList(newDottedId("some", "exported", "apiname")));
        com.sun.fortress.interpreter.nodes.Export e3 = new com.sun.fortress.interpreter.nodes.Export(span1, FortressUtil.mkList(newDottedId("an", "exported", "apiname")));

        een(e1, e2, e3);
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
        StringLiteral sl1 = new StringLiteral(span1, "123");
        StringLiteral sl2 = new StringLiteral(span2, "123");
        StringLiteral sl3 = new StringLiteral(span1, "124");

        een(sl1, sl2, sl3);

        IntLiteral il1 = NodeFactory.makeIntLiteral(span1, "123");
        IntLiteral il2 = NodeFactory.makeIntLiteral(span2, "123");
        IntLiteral il3 = NodeFactory.makeIntLiteral(span1, "124");

        een(il1, il2, il3);

        IntLiteral il4 = NodeFactory.makeIntLiteral(span1, new BigInteger("123"));

        Assert.assertEquals(il1, il4);

        FloatLiteral fl1 = NodeFactory.makeFloatLiteral(span1, "123");
        FloatLiteral fl2 = NodeFactory.makeFloatLiteral(span2, "123");
        FloatLiteral fl3 = NodeFactory.makeFloatLiteral(span1, "124");
        een(fl1, fl2, fl3);

        nnn(sl1, il1, fl1);

        FloatLiteral fl4 = NodeFactory.makeFloatLiteral(span1, "123.0");
        Assert.assertEquals(fl1, fl4);
    }


    StringLiteral newString(String s) {
        Span span1 = newSpan("cat", 1, 2, 3);
        return new StringLiteral(span1, s);
    }
    IntLiteral newInt(String s) {
        Span span1 = newSpan("dog", 1, 2, 3);
        return NodeFactory.makeIntLiteral(span1, s);
    }
    FloatLiteral newFloat(String s) {
        Span span1 = newSpan("emu", 1, 2, 3);
        return NodeFactory.makeFloatLiteral(span1, s);
    }

    public void testVarRef() {
        Span span1 = newSpan("dog", 1, 2, 3);
        Span span2 = newSpan("cat", 1, 2, 3);
        VarRefExpr v1 = NodeFactory.makeVarRefExpr(span1, "cat");
        VarRefExpr v2 = NodeFactory.makeVarRefExpr(span2, "cat");
        VarRefExpr v3 = NodeFactory.makeVarRefExpr(span1, "dog");

        een(v1, v2, v3);
    }

    VarRefExpr newVar(String s) {
        Span span1 = newSpan("dog", 1, 2, 3);
        VarRefExpr v1 = NodeFactory.makeVarRefExpr(span1, "cat");
        return v1;
    }

    public void testTuples() {
        Span span1 = newSpan("dog", 1, 2, 3);
        Span span2 = newSpan("cat", 1, 2, 3);
        TupleExpr t1 = new TupleExpr(span1, Useful.<Expr>list(newString("cat"), newString("dog")));
        TupleExpr t2 = new TupleExpr(span2, Useful.<Expr>list(newString("cat"), newString("dog")));
        TupleExpr t3 = new TupleExpr(span1, Useful.<Expr>list(newString("car"), newString("bog")));
        een(t1, t2, t3);

        try {
            TupleExpr t4 = new TupleExpr(span1, Collections.<Expr>emptyList());
            Assert.fail("Should have thrown exception, empty list not allowed");
        } catch (Error e) {

        }
    }

    public void testVoid() {
        Span span1 = newSpan("dog", 1, 2, 3);
        Span span2 = newSpan("cat", 1, 2, 3);
        VoidLiteral v1 = NodeFactory.makeVoidLiteral(span1);
        VoidLiteral v2 = NodeFactory.makeVoidLiteral(span2);
        Assert.assertEquals(v1, v2);
    }



}
