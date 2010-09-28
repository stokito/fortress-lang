/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.codegen;

import java.util.HashSet;
import java.util.List;
import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.Debug;

public class ParallelismAnalyzer extends NodeDepthFirstVisitor_void {
    private static final int ARG_THRESHOLD = 2;
    private final HashSet<ASTNode> worthy = new HashSet<ASTNode>();

    private boolean isComputeIntensiveArg(Expr e) {
        // A FnRef should not be parallelized itself. But, as an argument,
        // it supports the case for parallelizing the enclosing application.

        // We are parallelizing the evaluation of 2 and 3 in 2 + 3.  This needs
        // some serious refinement.  Turning parallelism way down for now.
        //        return (worthParallelizing(e) || (e instanceof _RewriteFnApp));
        return worthy.contains(e);
    }

    private void debug(ASTNode x) {
        Debug.debug(Debug.Type.CODEGEN,1, "ParallelismAnalyzer: node =" + x);
    }

    private void debug(ASTNode x, String message){
        Debug.debug(Debug.Type.CODEGEN,1, "ParallelismAnalyzer: node =" + x + "::" + message);
    }

    private void debug(String message) {
        Debug.debug(Debug.Type.CODEGEN,1, "ParallelismAnalyzer: " + "::" + message);
    }

    private boolean tallyArgs(List<Expr> args) {
        int count = 0;

        for (Expr e : args) {
            if (isComputeIntensiveArg(e)) count++;
        }
        return count >= ARG_THRESHOLD;
    }

    public boolean worthParallelizing(OpExpr x) {
        if (tallyArgs(x.getArgs())) return true; else return false;
    }

    public boolean worthParallelizing(ASTNode n) { 
        if (n instanceof OpExpr) {
            OpExpr x = (OpExpr) n;
            if (tallyArgs(x.getArgs())) {
                return true;
            }
        }
        return false;
    }

    public void printTable() {
        for (ASTNode node : worthy)
            System.out.println("Parallelizable table has entry " + node);
    }

    // Line 51 is the only OpExpr we want to parallelize
    public void forOpExprOnly(OpExpr x) {
        debug(x,"forOpExpr");
        if (tallyArgs(x.getArgs())) worthy.add(x);
    }

    public void forTupleExprOnly(TupleExpr x) {
        debug(x,"forTupleExpr");
        if (tallyArgs(x.getExprs())) worthy.add(x);
    }

    public void for_RewriteFnAppOnly(_RewriteFnApp x) {
        debug(x,"for_RewriteFnApp");
        worthy.add(x);
    }
}