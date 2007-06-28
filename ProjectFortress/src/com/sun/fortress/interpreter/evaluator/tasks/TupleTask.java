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

import java.io.IOException; 

import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.nodes.Expr;

public class TupleTask extends BaseTask {
    Evaluator eval;
    
    Expr expr;
    
    FValue res;
    
    public TupleTask(Expr ex, Evaluator ev, BaseTask parent) {
        super(parent);
        expr = ex;
        eval = ev;
    }
    
    public void run() {
        initTask();
        try {
            res = new Evaluator(eval, expr).eval(expr);
        } 
        catch (Throwable e) {
            causedException = true;
            err = e;
        }
        finalizeTask();
    }
    
    public void print() {
        System.out.println("Tuple Task: eval = " + eval +
                           "\n\t Expr = " + expr +
                           "\n\t Res = " + res +
                           "\n\t Thread = " + Thread.currentThread());
    }
    
    public FValue getRes() { return res;}
}
