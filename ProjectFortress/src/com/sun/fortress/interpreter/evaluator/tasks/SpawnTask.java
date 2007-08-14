
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
import com.sun.fortress.nodes.Expr;

public class SpawnTask extends BaseTask {
    
    Expr expr;
    
    Evaluator eval;
    
    FValue val;
    
    public void compute() {
        initTask();
        try {
            val = new Evaluator(eval, expr).eval(expr);
        } 
        catch (Throwable e) {
            causedException = true;
            err = e;
        }
        finalizeTask();
    }
    
    public SpawnTask(Expr b, Evaluator e, BaseTask task) {
        super(task);
        expr = b;
        eval = e;
    }
    
    public FValue val() {
        st_wait();
        return val;
    }
    
    public void print() {
        System.out.println("Spawn Task: Expr = " + expr +
                           " eval = " + eval +
                           " val = " + val);
    }
    
    public void st_wait() {}
    public Boolean ready() { return false;}
    public void stop() { }
    
}
