/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.tests.unit_tests;

import com.sun.fortress.exceptions.FortressException;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.interpreter.reader.Lex;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes_util.Printer;
import com.sun.fortress.nodes_util.Unprinter;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.Useful;
import edu.rice.cs.plt.tuple.Option;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * JUnit based test
 */

public class ASTJUTest extends com.sun.fortress.useful.TestCaseWrapper {

    public ASTJUTest(String testName) {
        super(testName);
    }

    public ASTJUTest() {
        super("ASTJUTest");
    }

    static PrintWriter out = new PrintWriter(System.out);
    static PrintWriter err = new PrintWriter(System.err);

    BufferedReader bs(String s) {
        return new BufferedReader(new StringReader(s));
    }

    protected void setUp() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(ASTJUTest.class);

        return suite;
    }

    abstract static class FN {
        void run(Lex a) throws IOException {
            bug("unimplemented");
        }

        void run(Lex a, String eq) throws IOException {
            bug("unimplemented");
        }

        void run(Lex a, int eq) throws IOException {
            bug("unimplemented");
        }

        void run(Lex a, String s1, String s2) throws IOException {
            bug("unimplemented");
        }
    }


    private void shouldFail(String s, FN f) {
        try {
            Lex a = new Lex(bs(s));
            f.run(a);
            fail("Missing exception");
        }
        catch (IOException ex) {
            out.println("OK, " + s + ", " + ex);
        }
    }

    private void shouldError(String s, FN f) {
        try {
            Lex a = new Lex(bs(s));
            f.run(a);
            fail("Missing error");
        }
        catch (FortressException ex) {
            out.println("OK, " + s + ", " + ex);
        }
        catch (IOException ex) {
            fail("Unexpected IO exception " + ex);
        }
    }

    private void shouldWork(String s, FN f, int line, int column) {
        try {
            Lex a = new Lex(bs(s));
            f.run(a);
            out.println("OK, l=" + a.line() + ", c=" + a.column());
            Assert.assertEquals(a.line(), line);
            Assert.assertEquals(a.column(), column);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            fail(ex.toString());
        }
    }

    private void shouldWork(String s, FN f, int line, int column, String eq) {
        try {
            Lex a = new Lex(bs(s));
            f.run(a, eq);
            out.println("OK, l=" + a.line() + ", c=" + a.column());
            Assert.assertEquals(a.line(), line);
            Assert.assertEquals(a.column(), column);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            fail(ex.toString());
        }
    }

    private void shouldWork(String s, FN f, int line, int column, int eq) {
        try {
            Lex a = new Lex(bs(s));
            f.run(a, eq);
            out.println("OK, l=" + a.line() + ", c=" + a.column());
            Assert.assertEquals(a.line(), line);
            Assert.assertEquals(a.column(), column);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            fail(ex.toString());
        }
    }

    private void shouldWork(String s, FN f, int line, int column, String s1, String s2) {
        try {
            Lex a = new Lex(bs(s));
            f.run(a, s1, s2);
            out.println("OK, l=" + a.line() + ", c=" + a.column());
            Assert.assertEquals(a.line(), line);
            Assert.assertEquals(a.column(), column);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            fail(ex.toString());
        }
    }

    // public void testDecodeIntLiteralExpr() {
    //     Assert.assertEquals(IntLiteralExpr.decode("10_16").intValue(), 16);
    //     Assert.assertEquals(IntLiteralExpr.decode("10_SIXTEEN").intValue(), 16);
    //     Assert.assertEquals(IntLiteralExpr.decode("FF_SIXTEEN").intValue(), 255);
    // }

    //    public void testDecodeFloatLiteralExpr() {
    //        Assert.assertEquals(FloatLiteralExpr.decode("10_16"), 16.0);
    //        Assert.assertEquals(FloatLiteralExpr.decode("10_SIXTEEN"), 16.0);
    //        Assert.assertEquals(FloatLiteralExpr.decode("10.0_SIXTEEN"), 16.0);
    //        Assert.assertEquals(FloatLiteralExpr.decode("10.00_SIXTEEN"), 16.0);
    //        Assert.assertEquals(FloatLiteralExpr.decode("10._SIXTEEN"), 16.0);
    //        Assert.assertEquals(FloatLiteralExpr.decode("10.0000000000000000000000000000_10"), 10.0);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.0000000000000000000000000000_10"), 0.0);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.0000000000000000000000000000_16"), 0.0);
    //
    //        Assert.assertEquals(FloatLiteralExpr.decode("X_12"), 10.0);
    //        Assert.assertEquals(FloatLiteralExpr.decode("E_12"), 11.0);
    //
    //        Assert.assertEquals(FloatLiteralExpr.decode("XX_12"), 130.0);
    //        Assert.assertEquals(FloatLiteralExpr.decode("EE_12"), 143.0);
    //
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.1_2"), 1.0/2);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.1_3"), 1.0/3);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.1_4"), 1.0/4);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.1_5"), 1.0/5);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.1_6"), 1.0/6);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.1_7"), 1.0/7);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.1_8"), 1.0/8);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.1_9"), 1.0/9);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.1_10"), 1.0/10);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.1_11"), 1.0/11);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.1_12"), 1.0/12);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.1_13"), 1.0/13);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.1_14"), 1.0/14);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.1_15"), 1.0/15);
    //        Assert.assertEquals(FloatLiteralExpr.decode("0.1_16"), 1.0/16);
    //
    //        Assert.assertEquals(FloatLiteralExpr.decode("1.1_2"), 1+1.0/2);
    //        Assert.assertEquals(FloatLiteralExpr.decode("1.1_3"), 1+1.0/3);
    //        Assert.assertEquals(FloatLiteralExpr.decode("1.1_4"), 1+1.0/4);
    //        Assert.assertEquals(FloatLiteralExpr.decode("1.1_5"), 1+1.0/5);
    //        Assert.assertEquals(FloatLiteralExpr.decode("1.1_6"), 1+1.0/6);
    //        Assert.assertEquals(FloatLiteralExpr.decode("1.1_7"), 1+1.0/7);
    //        Assert.assertEquals(FloatLiteralExpr.decode("1.1_8"), 1+1.0/8);
    //        Assert.assertEquals(FloatLiteralExpr.decode("1.1_9"), 1+1.0/9);
    //        Assert.assertEquals(FloatLiteralExpr.decode("1.1_10"), 1+1.0/10);
    //        Assert.assertEquals(FloatLiteralExpr.decode("1.1_11"), 1+1.0/11);
    //        Assert.assertEquals(FloatLiteralExpr.decode("1.1_12"), 1+1.0/12);
    //        Assert.assertEquals(FloatLiteralExpr.decode("1.1_13"), 1+1.0/13);
    //        Assert.assertEquals(FloatLiteralExpr.decode("1.1_14"), 1+1.0/14);
    //        Assert.assertEquals(FloatLiteralExpr.decode("1.1_15"), 1+1.0/15);
    //        Assert.assertEquals(FloatLiteralExpr.decode("1.1_16"), 1+1.0/16);
    //
    //    }

    public void testReadSpan() throws IOException {
        Lex l = new Lex(bs("@\"f1\":1:2~\"f2\":3:4 " + "@\"f3\":5:6~7:8 " + "@\"f4\":9:10~11 " + "@\"f5\":12:13 " +
                           "@14:15~16:17 " + "@18:19~20 " + "@21:22 @"));
        Unprinter up = new Unprinter(l);
        Assert.assertEquals("@", l.name());
        String s = up.readSpan();
        Assert.assertEquals("@", s);
        checkSpan(up, "f1", 1, 2, "f2", 3, 4);

        s = up.readSpan();
        Assert.assertEquals("@", s);
        checkSpan(up, "f3", 5, 6, "f3", 7, 8);

        s = up.readSpan();
        Assert.assertEquals("@", s);
        checkSpan(up, "f4", 9, 10, "f4", 9, 11);

        s = up.readSpan();
        Assert.assertEquals("@", s);
        checkSpan(up, "f5", 12, 13, "f5", 12, 13);

        s = up.readSpan();
        Assert.assertEquals("@", s);
        checkSpan(up, "f5", 14, 15, "f5", 16, 17);

        s = up.readSpan();
        Assert.assertEquals("@", s);
        checkSpan(up, "f5", 18, 19, "f5", 18, 20);

        s = up.readSpan();
        Assert.assertEquals("@", s);
        checkSpan(up, "f5", 21, 22, "f5", 21, 22);

    }

    private void checkSpan(Unprinter up, String string, int i, int j, String string2, int k, int l) {
        Assert.assertEquals(string, up.lastSpan.begin.getFileName());
        Assert.assertEquals(string2, up.lastSpan.end.getFileName());
        Assert.assertEquals(i, up.lastSpan.begin.getLine());
        Assert.assertEquals(k, up.lastSpan.end.getLine());
        Assert.assertEquals(j, up.lastSpan.begin.column());
        Assert.assertEquals(l, up.lastSpan.end.column());

    }

    // Gone, because we changed the structure of Assignment com.sun.fortress.interpreter.nodes.
    //    public void testMakeAssignment() throws IOException {
    //        String s =
    //            "(Assignment \n"+
    //             "       lhs=(VarRef  var=(Id name=\"x\"))\n"+
    //             "       rhs=(IntLiteralExpr text=\"3\" val=3))\n";
    //        Lex l = new Lex(bs(s));
    //        Unprinter up = new Unprinter(l);
    //        l.name(); // Reading "("
    //        Node x = up.readNode(l.name()); // Reading name of class
    //
    //        assertTrue (x instanceof com.sun.fortress.interpreter.nodes.Assignment);
    //        (new com.sun.fortress.interpreter.nodes.Printer(true, true, true)).dump(x, out, 0);
    //        out.println();
    //
    //
    //    }

    public void testMakeEmptyContract() throws IOException {
        String s = "(Contract @24:41~43)\n" + "name=(Fun @24:0~39\n" + "name_=(Id @24:0~17 name=\"builtinPrimitive\"))";

        Lex l = new Lex(bs(s));
        Unprinter up = new Unprinter(l);
        l.name(); // Reading "("
        Node x = up.readNode(l.name()); // Reading name of class

        assertTrue(x instanceof com.sun.fortress.nodes.Contract);
        (new com.sun.fortress.nodes_util.Printer(true, true, true)).dump(x, out, 0);
        out.println();
    }

    public void testWriteList() throws IOException {
        StringBuilder sb = new StringBuilder();
        List<String> l = new ArrayList<String>();
        l.add("cat");
        l.add("dog");

        Printer p = new Printer(false, true, true);
        p.dump(l, sb, 0);
        String s = sb.toString();
        out.println(s);

        Lex lex = new Lex(bs(s));
        Unprinter up = new Unprinter(lex);
        up.expectPrefix("[");
        List<Object> l2 = up.readList();
        assertEquals(2, l2.size());
        assertEquals(l.get(0), l2.get(0));
        assertEquals(l.get(1), l2.get(1));
    }

    public void testWriteOptionList() throws IOException {
        StringBuilder sb = new StringBuilder();
        List<Option<String>> l = new ArrayList<Option<String>>();
        Option<String> oc = Option.some("cat");
        Option<String> none = Option.<String>none();
        Option<String> od = Option.some("cat");

        l.add(oc);
        l.add(none);
        l.add(od);

        Printer p = new Printer(false, true, true);
        p.dump(l, sb, 0);
        String s = sb.toString();
        out.println(s);

        Lex lex = new Lex(bs(s));
        Unprinter up = new Unprinter(lex);
        up.expectPrefix("[");
        List<Object> l2 = up.readList();
        assertEquals(3, l2.size());
        assertEquals(l.get(0), l2.get(0));
        assertEquals(l.get(1), l2.get(1));
        assertEquals(l.get(2), l2.get(2));
    }

    public void testWriteMess() throws IOException {
        StringBuilder sb = new StringBuilder();
        List<Option<Pair<Option<String>, Option<String>>>> l =
                new ArrayList<Option<Pair<Option<String>, Option<String>>>>();
        Option<Pair<Option<String>, Option<String>>> oc =
                Option.some(new Pair<Option<String>, Option<String>>(Option.some("cat"), Option.some("cat")));
        Option<Pair<Option<String>, Option<String>>> none = Option.<Pair<Option<String>, Option<String>>>none();
        Option<Pair<Option<String>, Option<String>>> od =
                Option.some(new Pair<Option<String>, Option<String>>(Option.some("dog"), Option.<String>none()));

        l.add(oc);
        l.add(none);
        l.add(od);

        Printer p = new Printer(false, true, true);
        p.dump(l, sb, 0);
        String s = sb.toString();
        out.println(s);

        Lex lex = new Lex(bs(s));
        Unprinter up = new Unprinter(lex);
        up.expectPrefix("[");
        List<Object> l2 = up.readList();
        assertEquals(3, l2.size());
        assertEquals(l.get(0), l2.get(0));
        assertEquals(l.get(1), l2.get(1));
        assertEquals(l.get(2), l2.get(2));
    }

    public void testWriteBigMess() throws IOException {
        StringBuilder sb = new StringBuilder();
        List<Option<Pair<Option<String>, List<String>>>> l =
                new ArrayList<Option<Pair<Option<String>, List<String>>>>();
        Option<Pair<Option<String>, List<String>>> oc = Option.some(new Pair<Option<String>, List<String>>(Option.some(
                "cat"), Useful.list("cat")));
        Option<Pair<Option<String>, List<String>>> none = Option.<Pair<Option<String>, List<String>>>none();
        Option<Pair<Option<String>, List<String>>> od =
                Option.some(new Pair<Option<String>, List<String>>(Option.<String>none(),
                                                                   Collections.<String>emptyList()));

        l.add(oc);
        l.add(none);
        l.add(od);

        Printer p = new Printer(false, true, true);
        p.dump(l, sb, 0);
        String s = sb.toString();
        out.println(s);

        Lex lex = new Lex(bs(s));
        Unprinter up = new Unprinter(lex);
        up.expectPrefix("[");
        List<Object> l2 = up.readList();
        assertEquals(3, l2.size());
        assertEquals(l.get(0), l2.get(0));
        assertEquals(l.get(1), l2.get(1));
        assertEquals(l.get(2), l2.get(2));
    }


    public void testEnquote() {
        out.println("testEnquote");
        Assert.assertEquals("cat", Unprinter.enQuote("cat"));
        Assert.assertEquals("\\\\", Unprinter.enQuote("\\"));
        Assert.assertEquals("\\b", Unprinter.enQuote("\b"));
        Assert.assertEquals("\\f", Unprinter.enQuote("\f"));
        Assert.assertEquals("\\n", Unprinter.enQuote("\n"));
        Assert.assertEquals("\\r", Unprinter.enQuote("\r"));
        Assert.assertEquals("\\t", Unprinter.enQuote("\t"));
        Assert.assertEquals("\\\"", Unprinter.enQuote("\""));
        Assert.assertEquals(" ", Unprinter.enQuote(" "));
        Assert.assertEquals("'", Unprinter.enQuote("'"));
        Assert.assertEquals("\\'555'", Unprinter.enQuote("\u0555"));
        Assert.assertEquals("\\'05'", Unprinter.enQuote("\u0005"));
        Assert.assertEquals("\\'0aaa'", Unprinter.enQuote("\u0aaa"));
    }

    public void testDequote() {
        out.println("testdeQuote");//"\" =\""
        Assert.assertEquals("\" =\"", Unprinter.deQuote("\"\\\" =\\\"\""));
        Assert.assertEquals("cat", Unprinter.deQuote("\"cat\""));
        Assert.assertEquals("\\", Unprinter.deQuote("\"\\\\\""));
        Assert.assertEquals("\b", Unprinter.deQuote("\"\\b\""));
        Assert.assertEquals("\f", Unprinter.deQuote("\"\\f\""));
        Assert.assertEquals("\n", Unprinter.deQuote("\"\\n\""));
        Assert.assertEquals("\r", Unprinter.deQuote("\"\\r\""));
        Assert.assertEquals("\t", Unprinter.deQuote("\"\\t\""));
        Assert.assertEquals("\"", Unprinter.deQuote("\"\\\"\""));
        Assert.assertEquals(" ", Unprinter.deQuote("\" \""));
        Assert.assertEquals("'", Unprinter.deQuote("\"'\""));
        out.println(Unprinter.deQuote("\"\\'Psi&iota&rho&alpha'\""));
        Assert.assertEquals("\u03a8\u03b1\u03c1\u03b9", Unprinter.deQuote("\"\\'Psi&alpha&rho&iota'\""));
    }

    public void testWhite() {
        out.println("testWhite");
        FN whitefn = new FN() {
            void run(Lex a) throws IOException {
                a.white();
            }
        };

        shouldFail("", whitefn);
        shouldFail("  ", whitefn);
        shouldFail("\r\n", whitefn);

        shouldWork("\r\na", whitefn, 2, 1);
        shouldWork(" a", whitefn, 1, 2);
        shouldWork("\r a", whitefn, 2, 2);
        shouldWork("a", whitefn, 1, 1);

    }

    /**
     * Test of expect method, of class AST.
     */
    public void testExpect() {
        out.println("testExpect");
        FN catfn = new FN() {
            void run(Lex a) throws IOException {
                a.expect("cat");
            }
        };

        FN catdogfn = new FN() {
            void run(Lex a) throws IOException {
                a.expect("cat");
                a.expect("dog");
            }
        };

        FN ssfn = new FN() {
            void run(Lex a, String s1, String s2) throws IOException {
                a.expect(s1);
                a.expect(s2);
            }
        };

        shouldFail("dog", catfn);
        shouldFail("catt", catfn);
        shouldFail("ca ", catfn);
        shouldFail("ca", catfn);
        shouldWork("cat", catfn, 1, 4);
        shouldWork("cat ", catfn, 1, 4);
        shouldWork("cat\r", catfn, 2, 0);
        shouldWork("cat\n", catfn, 2, 0);
        shouldWork(" cat", catfn, 1, 5);
        shouldWork(" cat ", catfn, 1, 5);

        shouldFail("catdog", catdogfn);
        shouldWork("cat dog", catdogfn, 1, 8);
        shouldWork("cat dog ", catdogfn, 1, 8);
        shouldWork("cat\rdog", catdogfn, 2, 4);
        shouldWork("cat\r\ndog", catdogfn, 2, 4);
        shouldWork("cat\ndog", catdogfn, 2, 4);
        shouldWork("cat\r \rdog", catdogfn, 3, 4);

        shouldWork("cat) dog", ssfn, 1, 5, "cat", ")");
        shouldWork("cat)) dog", ssfn, 1, 5, "cat", ")");
        shouldWork("cat) dog", ssfn, 1, 9, "cat)", "dog");
        shouldWork("cat))dog", ssfn, 1, 9, "cat)", ")dog");

    }

    public void testExpectPrefix() {
        out.println("testExpectPrefix");
        FN catfn = new FN() {
            void run(Lex a) throws IOException {
                a.expectPrefix("cat");
            }
        };

        FN catdogfn = new FN() {
            void run(Lex a) throws IOException {
                a.expectPrefix("cat");
                a.expectPrefix("dog");
            }
        };

        FN ssfn = new FN() {
            void run(Lex a, String s1, String s2) throws IOException {
                a.expect(s1);
                a.expect(s2);
            }
        };

        shouldFail("dog", catfn);
        shouldWork("catt", catfn, 1, 4);
        shouldFail("ca ", catfn);
        shouldFail("ca", catfn);
        shouldWork("cat", catfn, 1, 4);
        shouldWork("cat ", catfn, 1, 4);
        shouldWork("cat\r", catfn, 2, 0);
        shouldWork("cat\n", catfn, 2, 0);
        shouldWork(" cat", catfn, 1, 5);
        shouldWork(" cat ", catfn, 1, 5);

        shouldWork("catdog", catdogfn, 1, 7);

        shouldWork("cat dog", catdogfn, 1, 8);
        shouldWork("cat dog ", catdogfn, 1, 8);
        shouldWork("cat\rdog", catdogfn, 2, 4);
        shouldWork("cat\r\ndog", catdogfn, 2, 4);
        shouldWork("cat\ndog", catdogfn, 2, 4);
        shouldWork("cat\r \rdog", catdogfn, 3, 4);

        shouldWork("cat) dog", ssfn, 1, 5, "cat", ")");
        shouldWork("cat) dog", ssfn, 1, 9, "cat)", "dog");

    }

    public void testString() {
        out.println("testString");
        FN catfn = new FN() {
            void run(Lex a, String s) throws IOException {
                String cat = a.string();
                Assert.assertEquals(cat, s);
            }
        };

        FN catdogfn = new FN() {
            void run(Lex a, String s1, String s2) throws IOException {
                String cat = a.string();
                String dog = a.string();
                Assert.assertEquals(cat, s1);
                Assert.assertEquals(dog, s2);
            }
        };

        shouldWork("dog", catfn, 1, 4, "dog");
        shouldWork("\"dog\"", catfn, 1, 6, "\"dog\"");
        shouldWork("catt", catfn, 1, 5, "catt");
        shouldWork("ca ", catfn, 1, 3, "ca");
        shouldWork("ca", catfn, 1, 3, "ca");
        shouldWork("cat", catfn, 1, 4, "cat");
        shouldWork("cat ", catfn, 1, 4, "cat");
        shouldWork("cat\r", catfn, 2, 0, "cat");
        shouldWork("cat\n", catfn, 2, 0, "cat");
        shouldWork(" cat", catfn, 1, 5, "cat");
        shouldWork(" cat ", catfn, 1, 5, "cat");

        shouldWork("catdog", catfn, 1, 7, "catdog");
        shouldWork("cat dog", catdogfn, 1, 8, "cat", "dog");
        shouldWork("cat dog ", catdogfn, 1, 8, "cat", "dog");
        shouldWork("cat\rdog", catdogfn, 2, 4, "cat", "dog");
        shouldWork("cat\r\ndog", catdogfn, 2, 4, "cat", "dog");
        shouldWork("cat\ndog", catdogfn, 2, 4, "cat", "dog");
        shouldWork("cat\r \rdog", catdogfn, 3, 4, "cat", "dog");

        shouldWork("(cat", catdogfn, 1, 5, "(", "cat");
        shouldWork("( cat", catdogfn, 1, 6, "(", "cat");
        shouldWork(" (cat", catdogfn, 1, 6, "(", "cat");
        shouldWork(" ( cat", catdogfn, 1, 7, "(", "cat");

        shouldWork("cat)", catdogfn, 1, 5, "cat", ")");
        shouldWork("cat )", catdogfn, 1, 6, "cat", ")");
        shouldWork(" cat)", catdogfn, 1, 6, "cat", ")");
        shouldWork("cat) ", catdogfn, 1, 5, "cat", ")");
        shouldWork(" cat ) ", catdogfn, 1, 7, "cat", ")");

        shouldWork("()", catdogfn, 1, 3, "(", ")");
        shouldWork("( )", catdogfn, 1, 4, "(", ")");
        shouldWork(" ()", catdogfn, 1, 4, "(", ")");
        shouldWork("((", catdogfn, 1, 3, "(", "(");
        shouldWork("( (", catdogfn, 1, 4, "(", "(");
        shouldWork(" ((", catdogfn, 1, 4, "(", "(");


    }

    public void testInt() {
        out.println("testInt");
        FN catfn = new FN() {
            void run(Lex a, int s) throws IOException {
                int cat = a.integer();
                Assert.assertEquals(cat, s);
            }
        };

        shouldWork("1", catfn, 1, 2, 1);
        shouldWork(" 1", catfn, 1, 3, 1);
        shouldWork("1 ", catfn, 1, 2, 1);
        shouldWork("2", catfn, 1, 2, 2);

        shouldWork("21", catfn, 1, 3, 21);

        shouldError("cat", catfn);

    }


    //    public void testDotted() {
    //        out.println("testDotted");
    //        Lex l = new Lex(bs("node "+
    //    "((pos samples/import_component.fss 1 0 26) "+
    //    "(pos samples/import_component.fss 1 0 26)) "+
    //    "(dotted a b c))"));
    //        try {
    //           com.sun.fortress.interpreter.nodes.DottedId dot = new com.sun.fortress.interpreter.nodes.DottedId(l);
    //           Assert.assertEquals(dot.names.size(), 3);
    //           Assert.assertEquals("a", dot.names.get(0));
    //           Assert.assertEquals("b", dot.names.get(1));
    //           Assert.assertEquals("c", dot.names.get(2));
    //        } catch (IOException ex) {
    //            ex.printStackTrace();
    //           fail("Oops, " + ex);
    //        }
    //
    //    }

    // The file is missing too often
    //    public void testFile() throws IOException {
    //        out.println("testFile");
    //        Lex l = new Lex(new BufferedReader(new FileReader("test.sexp")));
    //        try {
    //           try {
    //             CompilationUnit.program(l); // discard program
    //           } finally {
    //             out.println("Final line = " + l.getLine()() + ", final column = " + l.column());
    //           }
    //        } catch (IOException ex) {
    //            ex.printStackTrace();
    //           fail("Oops, " + ex);
    //        }
    //

    //    }
    protected void tearDown() throws Exception {
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
}
