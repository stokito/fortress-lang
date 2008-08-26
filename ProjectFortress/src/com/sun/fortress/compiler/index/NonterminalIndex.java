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

import java.util.List;

import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.SyntaxDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.Pair;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionUnwrapException;

public abstract class NonterminalIndex {

    public abstract NonterminalDecl ast();

    public Id getName() {
        return this.ast().getHeader().getName();
    }

    public BaseType getAstType() {
        Option<BaseType> type = this.ast().getAstType();
        if (type.isSome()) {
            return type.unwrap();
        } else {
            throw new RuntimeException("Production index without type");
        }
    }

    public List<SyntaxDecl> getSyntaxDecls() {
        return this.ast().getSyntaxDecls();
    }

    public boolean isPrivate() {
        return ast().getHeader().getModifier().isSome();
    }

    public String toString(){
        return getName().toString();
    }
}
