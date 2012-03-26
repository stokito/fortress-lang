/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

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
