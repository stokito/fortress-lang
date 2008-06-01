/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.evaluator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeTop;
import com.sun.fortress.interpreter.evaluator.values.DummyValue;
import com.sun.fortress.interpreter.evaluator.values.Dummy_fcn;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Overload;
import com.sun.fortress.interpreter.evaluator.values.OverloadedFunction;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.nodes_util.NodeFactory;

public class OverloadJUTest extends com.sun.fortress.useful.TestCaseWrapper  {

    public OverloadJUTest() {
        super("OverloadJUTest");
    }
    private Overload simple_overload(List<FType> types, OverloadedFunction olf) {
        return new Overload(new Dummy_fcn(types));
    }

    private void makeDispatchTest(List<FType> dynamic_types, List<List<FType>> clauses, List<FType> result) {
        try {
            int idx = overloadDispatch(dynamic_types, clauses);
            assertTrue(idx >= 0);
            assertEquals(result, clauses.get(idx));
        }
        catch (FortressException pe) {
            assertFalse("CompilationUnit error related to overloading: " +
                        pe.getMessage(), pe.getMessage().contains("verload"));
            fail();
        }

    }

    private void makeDispatchFailTest(List<FType> dynamic_types, List<List<FType>> clauses) {
        try {
            int idx = overloadDispatch(dynamic_types, clauses);
            assertTrue("Dispatch found candidate "+idx+ " when it shouldn't",
                       idx == -1);
        }
        catch (FortressException pe) {
            assertTrue("CompilationUnit error was not related to overloading: "+
                       pe.getMessage(), pe.getMessage().contains("verload"));
        }
    }

    private int overloadDispatch(List<FType> dynamic_types, List<List<FType>> clauses) {
        OverloadedFunction fcn = new OverloadedFunction(
              NodeFactory.makeId("dummyOverloadName"), BetterEnv.primitive("Test overload dispatch"));
        for(List<FType> cl: clauses) {
            fcn.addOverload(simple_overload(cl, fcn));
        }
        fcn.finishInitializing();
        List<FValue> vals = new ArrayList<FValue>();
        for(FType t: dynamic_types)
            vals.add(new DummyValue(t));
        return fcn.bestMatchIndex(vals, null, null, fcn.getOverloads());
    }

    private static <T> List<T> l(T... args) { return Arrays.asList(args); }

    // some convenience bindings; need to invent some superclass relationships
    // since they're not baked in anymore.
    private static final class TInt extends FType {
        private TInt() { super("TInt"); cannotBeExtended = true; }
        protected List<FType> computeTransitiveExtends() {
            return l(this, Integral, Float, Number);
        }
        public boolean subtypeOf(FType other) {
            return (this==other || Integral.subtypeOf(other));
        }
    }
    private static final class TIntegral extends FType {
        private TIntegral() { super("TIntegral"); }
        protected List<FType> computeTransitiveExtends() {
            return l(this, Float, Number);
        }
        public boolean subtypeOf(FType other) {
            return (this==other || Float.subtypeOf(other));
        }
    }
    private static final class TFloat extends FType {
        private TFloat() { super("TFloat"); cannotBeExtended = true; }
        protected List<FType> computeTransitiveExtends() {
            return l(this, Number);
        }
        public boolean subtypeOf(FType other) {
            return (this==other || Number.subtypeOf(other));
        }
    }
    private static final class TNumber extends FType {
        private TNumber() { super("TNumber"); }
        protected List<FType> computeTransitiveExtends() {
            return l((FType)this);
        }
        public boolean subtypeOf(FType other) {
            return (this==other || other==FTypeTop.ONLY);
        }
    }
    static final FType Int = new TInt();
    static final FType Integral = new TIntegral();
    static final FType Float = new TFloat();
    static final FType Number = new TNumber();

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        Init.initializeEverything();
    }

    @SuppressWarnings("unchecked")
    public void testOverloadSucceed() {
        // All the tests
        makeDispatchTest(l(Int), l(l(Int), l(Float)), l(Int));
        makeDispatchTest(l(Int), l(l(Integral),l(Float)), l(Integral));
        makeDispatchTest(l(Int), l(l(Number),l(Int)), l(Int));
        makeDispatchTest(l(Int,Float),
                         l(l(Number,Number),l(Float,Float), l(Int,Int)),
                         l(Float,Float));
        makeDispatchTest(l(Int,Int),
                         l(l(Integral,Integral),l(Int,Float), l(Float,Int)),
                         l(Integral,Integral));
        makeDispatchTest(l(Int,Int),
                         l(l(Number,Number),l(Int,Number), l(Float,Int)),
                         l(Int,Number));
        makeDispatchTest(l(Int,Int),
                         l(l(Number,Number),l(Int,Number), l(Number,Int), l(Int,Int)),
                         l(Int,Int));

    }

    @SuppressWarnings("unchecked")
    public void testOverloadFail() {
        makeDispatchFailTest(l(Int,Int),l(l(Number,Number),l(Number,Int),l(Int,Number)));
        // makeDispatchFailTest(l(Int,Int),l(l(Float,Float)));
        makeDispatchFailTest(l(Int,Int),l(l(Number,Float),l(Float,Number)));
        makeDispatchFailTest(l(Number),l(l(Int),l(Float)));
        makeDispatchFailTest(l(Number),new ArrayList<List<FType>>());

    }

}
