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
import java.util.Collections;
import java.util.List;

import com.sun.fortress.interpreter.useful.Useful;

// / and arrow_type = arrow_type_rec node
// / and arrow_type_rec =
// / {
// / arrow_type_keywords : keyword_type list;
// / arrow_type_domain : type_ref list;
// / arrow_type_range : type_ref;
// / arrow_type_throws : type_ref list;
// / }
// /
public class TupleType extends TypeRef {

    List<TypeRef> elements;

    List<KeywordType> keywords;

    public TupleType(Span s, List<TypeRef> elements, List<KeywordType> keywords) {
        super(s);
        this.elements = elements;
        this.keywords = keywords;
    }

    public TupleType(Span s, List<TypeRef> elements) {
        this(s, elements, Collections.<KeywordType> emptyList());
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forTupleType(this);
    }

    TupleType(Span span) {
        super(span);
    }

    @Override
    public String toString() {
        return Useful.listInParens(getElements());
    }

    /**
     * @return Returns the elements.
     */
    public List<TypeRef> getElements() {
        return elements;
    }

    /**
     * @return Returns the keywords.
     */
    public List<KeywordType> getKeywords() {
        return keywords;
    }
}
