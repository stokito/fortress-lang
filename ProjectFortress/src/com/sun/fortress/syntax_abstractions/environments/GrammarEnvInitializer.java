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

package com.sun.fortress.syntax_abstractions.environments;

import java.util.Collection;

import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.syntax_abstractions.intermediate.Module;

public class GrammarEnvInitializer {

    public static void init(Collection<Module> modules) {
        GrammarEnv.clear();
        
        for (Module module: modules) {
                for (NonterminalIndex<? extends GrammarMemberDecl> nt: module.getDeclaredNonterminals()) {
                    GrammarEnv.add(nt.getName(), new MemberEnv(nt));
            }
        }
    }

}
