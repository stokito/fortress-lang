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
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.LinkedList;

import com.sun.fortress.nodes.AnyCharacterSymbol;
import com.sun.fortress.nodes.BackspaceSymbol;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.CarriageReturnSymbol;
import com.sun.fortress.nodes.CharacterClassSymbol;
import com.sun.fortress.nodes.FormfeedSymbol;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NewlineSymbol;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.OptionalSymbol;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TabSymbol;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.WhitespaceSymbol;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.intermediate.SyntaxSymbolPrinter;
import com.sun.fortress.syntax_abstractions.util.TypeCollector;


// FIXME: stub; needs completing

public class ComposingSyntaxDeclEnv {

    private SyntaxDef sd;

    public ComposingSyntaxDeclEnv(SyntaxDef sd) {
        this.sd = sd;
    }

    public Id getNonterminalOfVar(Id var) {
        throw new RuntimeException("STUB: getNonterminalName of " + var);
    }




    public boolean contains(Id var) {
        return true;
    }

    public Collection<Id> getVariables() {
        return new LinkedList<Id>();
    }

    public boolean isNonterminal(Id id) {
        return true;
    }

    public boolean isPatternVariable(Id id) {
        return isNonterminal(id) || isOption(id) || 
               isRepeat(id) || isAnyChar(id) || 
               isCharacterClass(id) || isSpecialSymbol(id);
    }

    public boolean isAnyChar(Id id) {
        return true;
    }

    public boolean isCharacterClass(Id id) {
        return true;
    }

    public boolean isOption(Id id) {
        return true;
    }

    public boolean isRepeat(Id id) {
        return true;
    }

    private boolean isSpecialSymbol(Id id) {
        return true;
    }
}
