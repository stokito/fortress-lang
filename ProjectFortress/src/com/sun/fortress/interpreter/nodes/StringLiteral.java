/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.nodes;

import com.sun.fortress.interpreter.useful.MagicNumbers;

// / type literal = string node
public class StringLiteral extends Literal {
    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forStringLiteral(this);
    }

    StringLiteral(Span span) {
        super(span);
    }

    public StringLiteral(Span span, String s) {
        super(span, s);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof StringLiteral) {
            StringLiteral sl = (StringLiteral) o;
            return text.equals(sl.getText());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return MagicNumbers.S * text.hashCode();
    }

    /**
     * @return Returns the name.
     */
    public String value() {
        return getText();
    }
}
