/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.compiler.codegen;

import java.util.HashSet;
import java.util.List;
import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.tuple.Option;

/* There is a complication here: there is a distinction between things
 * that should be parallelized, and things that are compute-intensive.
 *
 * For example:
 *
 *  (foo(2,3) + bar(baz(4),5)) + (2 + 3);
 *
 * The outermost expression should not be parallelized, because only
 * the LHS has something compute-intensive.  The addition in the LHS
 * should be parallelized, because it is comprised of two function
 * calls.  However, the function calls themselves should *not* be
 * parallized, since their arguments are not compute-intensive enough.
 *
 * Things should be added to the worthy hash table iff they themselves
 * should be run in parallel.  So, after traversing this expression,
 * only the plus in the LHS of the expression would be "worthy".
 */

// TODO: I don't think this handles mutual recursion.
// TODO: It would be nice to easily turn on/off this analysis

public class ParallelismAnalyzer extends NodeDepthFirstVisitor_void {
    private static final int ARG_THRESHOLD = 2;
    private static final boolean ANALYZATION_ON = true;

    // It's kind of a waste to have two hashsets with almost the same entries
    // Is worthy \subset computeIntensive ?
    private final HashSet<ASTNode> worthy = new HashSet<ASTNode>();
    private final HashSet<ASTNode> computeIntensive = new HashSet<ASTNode>();

    private void addToWorthy(ASTNode x) {
        if (ANALYZATION_ON) { 
            debug(x, "adding to worthy");
            worthy.add(x);
        }
    }

    private void addToComputeIntensive(ASTNode x) {
        if (ANALYZATION_ON) {
            debug(x, "adding to computeIntensive");
            computeIntensive.add(x);
        }
    }

    private void removeFromComputeIntensive(ASTNode x) {
        if (ANALYZATION_ON) {
            debug(x, "removing from computeIntensive");
            computeIntensive.remove(x);
        }
    }

    private void debug(ASTNode x) {
        Debug.debug(Debug.Type.CODEGEN,1, "ParallelismAnalyzer: node = " + x);
    }

    private void debug(ASTNode x, String message){
        Debug.debug(Debug.Type.CODEGEN,1, "ParallelismAnalyzer: node = " + x + "::" + message);
    }

    private void debug(String message) {
        Debug.debug(Debug.Type.CODEGEN,1, "ParallelismAnalyzer: " + "::" + message);
    }

    public void printTable() {
        for (ASTNode node : worthy)
            debug("Parallelizable table has entry " + node);
    }

    public void printComputeIntensive() {
        for (ASTNode node : computeIntensive)
            debug("Compute-intensive table has entry " + node);
    }

    public boolean worthParallelizing(ASTNode n) {
        if (ANALYZATION_ON) {
            if (worthy.contains(n)) {
                debug(n, "worthy of parallizing");
                return true;
            } else {
                debug(n, "not worthy of parallizing");
                return false;
            }
        }
        return false;
    }

    /** Determining whether things are compute intensive **/
    
    private boolean isComputeIntensive(ASTNode n) {
        if (ANALYZATION_ON) {
            debug("checking compute intensity in the following table:");
            printComputeIntensive();
            if (computeIntensive.contains(n)) {
                debug(n, "is compute intensive");
                return true;
            } else {
                debug(n, "is not compute intensive");
                return false;
            }
        }
        return false;
    }

    private boolean isComputeIntensive(List<? extends Expr> exprs) { // Why can't this be List<ASTNode>?
        int count = 0;
        for (Expr e : exprs) 
            if (isComputeIntensive(e)) count++;
        return (count >= ARG_THRESHOLD);
    }

    private boolean anyComputeIntensive(List<? extends Expr> exprs) {
        for (Expr e : exprs)
            if (isComputeIntensive(e)) return true;
        return false;
    }

    /** Visitor Pattern methods **/

    public void forDoOnly(Do x) {
        debug(x, "forDo " + x);
        if(anyComputeIntensive(x.getFronts()))
            addToComputeIntensive(x);
    }
    
    public void forBlockOnly(Block x) {
        debug(x, "forBlock " + x);
        if(anyComputeIntensive(x.getExprs()))
            addToComputeIntensive(x);
    }
    
    public void forJuxtOnly(Juxt x) {
        debug(x, "forJuxtOnly " + x);
        if(anyComputeIntensive(x.getExprs()))
            addToComputeIntensive(x);
    }
    
    public void forOpExprOnly(OpExpr x) {
        debug(x, "forOpExpr " + x);
        if (isComputeIntensive(x.getArgs())) {
            addToComputeIntensive(x);
            addToWorthy(x);
        }
    }
    
    public void forIfOnly(If x) {
        debug(x, "forIfOnly " + x);
        List<IfClause> clauses = x.getClauses();
        for(IfClause clause : clauses) {
            if (isComputeIntensive(clause.getTestClause()) ||
                isComputeIntensive(clause.getBody())) {
                addToComputeIntensive(x);
                return;
            }
            else {
                Option<Block> e = x.getElseClause();
                if (e.isSome() &&
                    isComputeIntensive(e.unwrap())) {
                    addToComputeIntensive(x);
                    return;
                }
            }
        }
    }

    public void forTupleExprOnly(TupleExpr x) {
        debug(x, "forTupleExpr " + x);
        if (isComputeIntensive(x.getExprs())) {
            addToComputeIntensive(x);
            addToWorthy(x);
        }
    }

    // Is this right?  The builtins could be intensive too, no?
    public void for_RewriteFnAppOnly(_RewriteFnApp x) {
        debug(x, "for_RewriteFnApp " + x);
        String node = x.toString();
        String fcn = x.getFunction().toString();
        if (node.contains("CompilerLibrary") ||
            node.contains("CompilerBuiltin") ||
            fcn.contains("CompilerBuiltin") ||
            fcn.contains("println"))
            return;
        Expr fn = x.getFunction();
        if (fn instanceof FnRef) {
            FnRef fnref = (FnRef) fn;
            if(isComputeIntensive(fnref.getOriginalName())) // do we always want the original name?
                addToComputeIntensive(x); 
        }
        if (isComputeIntensive(x.getArgument()))
            addToWorthy(x);
    }

    public void forFnDeclDoFirst(FnDecl x) {
        debug(x, "forFnDeclDoFirst " + x);
        IdOrOpOrAnonymousName fnName = x.getHeader().getName();
        if (fnName instanceof Id) 
            addToComputeIntensive(fnName);
    }

    public void forFnDeclOnly(FnDecl x) {
        debug(x, "forFnDeclOnly " + x);
        Option<Expr> body = x.getBody();
        if (body.isNone() || (! isComputeIntensive(body.unwrap())))
            removeFromComputeIntensive(x);
    }

}
