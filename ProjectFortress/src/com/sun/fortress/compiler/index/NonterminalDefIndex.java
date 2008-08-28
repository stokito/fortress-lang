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

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.BaseType;
import edu.rice.cs.plt.tuple.Option;

public class NonterminalDefIndex extends NonterminalIndex {

    private NonterminalDef ast;

    public NonterminalDefIndex(NonterminalDef ast) {
        this.ast = ast;
    }

    public NonterminalDef ast() {
        return ast;
    }

    public boolean isPrivate() {
        return ast().getHeader().getModifier().isSome();
    }

    public BaseType getAstType() {
        Option<BaseType> type = this.ast().getAstType();
        if (type.isSome()) {
            return type.unwrap();
        } else {
            throw new RuntimeException("Production index without type");
        }
    }
}
