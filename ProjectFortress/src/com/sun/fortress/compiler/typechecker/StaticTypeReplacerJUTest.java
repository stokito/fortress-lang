/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.typechecker;

import junit.framework.TestCase;
import java.util.*;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.*;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.FALSE;
import static com.sun.fortress.compiler.Types.*;
import static com.sun.fortress.nodes_util.NodeFactory.*;

public class StaticTypeReplacerJUTest extends TypeCheckerTestCase {

    private StaticTypeReplacer replacer;

    public void testLeafNodes() {
        List<StaticParam> params = makeSparams(makeStaticParam("K"),
                                               makeStaticParam("int n"),
                                               makeStaticParam("nat m"),
                                               makeStaticParam("bool b"),
                                               makeStaticParam("opr +"),
                                               makeStaticParam("dim d"),
                                               makeStaticParam("unit u"));
        Span span = NodeFactory.testSpan;
        List<StaticArg> args = makeSargs(makeTypeArg(span, "ZZ32"),
                                         makeIntArg(span, "-5"),
                                         makeIntArg(span, "m"),
                                         makeBoolArg(span, "true"),
                                         makeOpArg(span, "-"),
                                         makeDimArg(makeDimRef(span, "Length")),
                                         makeUnitArg(makeUnitRef(span, "ft_")));

        replacer = new StaticTypeReplacer(params, args);

        assertEqualTypes("ZZ32 -> ZZ32", "K -> K");
        assertEqualTypes("(ZZ32, String, ZZ32)", "(K, String, ZZ32)");
    }

    private void assertEqualTypes(String s, String t) {
        assertEquals(parseType(s).toString(), replacer.replaceIn(parseType(t)).toString());
    }

    // private void assertEqualTypes(String s, String t, Lambda<Type, Type> subst) {
    //     assertEquals(parseType(s).toString(), subst.value(parseType(t)).toString());
    // }

}
