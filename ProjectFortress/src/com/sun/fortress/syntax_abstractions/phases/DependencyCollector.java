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

package com.sun.fortress.syntax_abstractions.phases;

import java.util.HashSet;
import java.util.Set;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.NonterminalHeader;
import com.sun.fortress.nodes.NonterminalParameter;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.tuple.Option;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

/*
 * Collect the names of nonterminals referred from the alternatives of any
 * nonterminal declaration it is applied to.
 */
public class DependencyCollector extends NodeDepthFirstVisitor_void {

    private Set<Id> result;

    public DependencyCollector() {
        this.result = new HashSet<Id>();
    }

    @Override
    public void forNonterminalSymbolOnly(NonterminalSymbol that) {
        addModule(that.getNonterminal());
    }

    private void addModule(Id id){
        // We know that fortress modules are on the form FortressSyntax.moduleName.nonterminal
        if (ModuleInfo.isFortressModule(id)) {
            if (id.getApi().isNone())
                bug(id, "Missing an API name...");
            Id name = id.getApi().unwrap().getIds().get(1);
            result.add(name);
        }
        else {
            result.add(id);
        }
    }

    public Set<Id> getResult() {
        return this.result;
    }

}
