/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.SyntaxDecl;

import java.util.List;

public abstract class NonterminalIndex {

    public abstract NonterminalDecl ast();

    public Id getName() {
        return this.ast().getName();
    }

    public List<SyntaxDecl> getSyntaxDecls() {
        return this.ast().getSyntaxDecls();
    }

    public String toString() {
        return getName().toString();
    }
}
