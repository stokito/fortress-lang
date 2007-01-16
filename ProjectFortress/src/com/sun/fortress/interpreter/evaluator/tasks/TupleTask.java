/********************************************************************************
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
********************************************************************************/

package com.sun.fortress.interpreter.evaluator.tasks;

import EDU.oswego.cs.dl.util.concurrent.FJTask;
import EDU.oswego.cs.dl.util.concurrent.FJTaskRunnerGroup;

import com.sun.fortress.interpreter.nodes.CompilationUnit;
import java.util.List;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.nodes.Expr;
import com.sun.fortress.interpreter.evaluator.values.FValue;

public class TupleTask extends BaseTask {
        Evaluator eval;

        Expr expr;

        FValue res;

        public TupleTask(Expr ex, Evaluator ev) {
            super();
            expr = ex;
            eval = ev;
        }

        public void run() {
            res = expr.accept(new Evaluator(eval, expr));
        }

       public FValue getRes() { return res;}
}