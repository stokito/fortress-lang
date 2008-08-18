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

import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.Pair;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionUnwrapException;

public abstract class NonterminalIndex<T extends GrammarMemberDecl> {

    private Option<T> ast;

    public NonterminalIndex(Option<T> ast) {
        this.ast = ast;
    }

    public Option<T> ast() {
        return this.ast;
    }

    public Id getName() {
        if (this.ast().isSome()) {
            return this.ast().unwrap().getHeader().getName();
        }
        throw new RuntimeException("Production index without ast and thus no name");
    }

    public BaseType getAstType() {
        if (this.ast().isSome()) {
            Option<BaseType> type = this.ast().unwrap().getAstType();
            if (type.isSome()) {
                return type.unwrap();
            }
            throw new RuntimeException("Production index without type, type inference is not implemented yet!");
        }
        throw new RuntimeException("Production index without ast and thus no type");
    }

    public T getAst() {
        try { return this.ast.unwrap(); }
        catch (OptionUnwrapException e) { throw new RuntimeException("Ast not found."); }
    }

    public boolean isPrivate() {
        return getAst().getHeader().getModifier().isSome();
    }

    public String toString(){
        return getName().toString();
    }

}
