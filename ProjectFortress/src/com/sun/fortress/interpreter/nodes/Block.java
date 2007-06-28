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

package com.sun.fortress.interpreter.nodes;

import com.sun.fortress.interpreter.nodes_util.Span;
import java.util.List;

/**
 * Do expressions are parsed into elements of this class. 
 * 
 * Do ::= (DoFront also )? DoFront end 
 * DoFront ::= ( at Expr)? atomic ? do BlockElems? 
 * BlockElems ::= BlockElem+ 
 * BlockElem ::= LocalVarFnDecl 
 *             | Expr( , GeneratorList)? 
 * LocalVarFnDecl ::= LocalFnDecl+ 
 * | LocalVarDecl 
 * 
 * Simple example:
 * do 
 *   y = x 
 *   z = 2x 
 *   y + z 
 * end 
 */
public class Block extends FlowExpr {
    List<Expr> exprs;

    Block(Span span) {
        super(span);
    }

    public Block(Span span, List<Expr> exprs) {
        super(span);
        this.exprs = exprs;
    }

    /**
     * @return Returns the exprs.
     */
    public List<Expr> getExprs() {
        return exprs;
    }
    
    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forBlock(this);
    }
}
