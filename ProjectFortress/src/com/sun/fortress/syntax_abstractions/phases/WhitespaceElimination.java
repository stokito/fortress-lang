/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.Debug;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class WhitespaceElimination extends NodeUpdateVisitor {

    @Override
    public Node forSyntaxDef(SyntaxDef that) {
        List<SyntaxSymbol> ls = new LinkedList<SyntaxSymbol>();
        boolean ignoreWhitespace = false;
        for (SyntaxSymbol symbol : that.getSyntaxSymbols()) {
            if (!ignoreWhitespace || !(symbol instanceof WhitespaceSymbol)) {
                if (symbol instanceof NoWhitespaceSymbol) {
                    symbol = ((NoWhitespaceSymbol) symbol).getSymbol();
                    ignoreWhitespace = true;
                } else {
                    ignoreWhitespace = false;
                }
                ls.add(symbol);
            } else {
                Debug.debug(Debug.Type.SYNTAX, 1, "[whitespace] Throwing out symbol ", symbol);
            }
        }
        return new SyntaxDef(that.getInfo(), that.getModifier(), ls, that.getTransformer());
    }
}
