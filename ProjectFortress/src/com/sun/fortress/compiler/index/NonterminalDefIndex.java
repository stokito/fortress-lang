/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.NonterminalDef;
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
        return ast().getHeader().getMods().isPrivate();
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
