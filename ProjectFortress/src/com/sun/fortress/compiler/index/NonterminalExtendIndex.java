/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.NonterminalExtensionDef;

public class NonterminalExtendIndex extends NonterminalIndex {

    private NonterminalExtensionDef ast;

    public NonterminalExtendIndex(NonterminalExtensionDef ast) {
        this.ast = ast;
    }

    public NonterminalExtensionDef ast() {
        return ast;
    }
}
