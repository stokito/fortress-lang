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

import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.ListComparer;
import com.sun.fortress.interpreter.useful.Useful;

// / and keyword_type = keyword_type_rec node
// / and keyword_type_rec =
// / {
// / keyword_name : id;
// / keyword_type : type_ref;
// / }
// /
public class KeywordType extends AbstractNode implements Comparable<KeywordType> {
    public KeywordType(Span s, Id name, TypeRef type) {
        super(s);
        this.name = name;
        this.type = type;
    }

    Id name;

    TypeRef type;

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forKeywordType(this);
    }

    KeywordType(Span span) {
        super(span);
    }

    /**
     * @return Returns the name.
     */
    public Id getName() {
        return name;
    }

    /**
     * @return Returns the type.
     */
    public TypeRef getType() {
        return type;
    }

    @Override
    public String toString() {
        return "" + getName() + ":" + getType();
    }

    public int compareTo(KeywordType o) {
        return NodeComparator.compare(name, o.name, type, o.type);
    }

}
