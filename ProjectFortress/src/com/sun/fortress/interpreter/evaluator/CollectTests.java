/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Debug;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a very simple visitor which just gathers up all of the test functions/data and adds them to the environment.
 * For now we only add test functions.
 */

public class CollectTests extends NodeDepthFirstVisitor<Boolean> {

    private static List<String> tests;

    public Boolean defaultCase(Node that) {
        return false;
    }

    public void visit(CompilationUnit n) {
        n.accept(this);
    }

    public static List<String> getTests() {
        return tests;
    }

    public CollectTests() {
        tests = new ArrayList<String>();
    }

    public Boolean forFnDecl(FnDecl x) {
        Debug.debug(Debug.Type.INTERPRETER, 2, "ForFnDecl ", x);
        //List<StaticParam> optStaticParams = NodeUtil.getStaticParams(x);
        String fname = NodeUtil.nameAsMethod(x);

        if (NodeUtil.getMods(x).isTest()) {
            tests.add(fname);
        }
        return false;
    }
}
