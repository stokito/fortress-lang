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
import java.util.ArrayList;
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
public class ArrowType extends TypeRef {

    // This field should go away after replacing the OCaml com.sun.fortress.interpreter.parser with the
    // Rats! com.sun.fortress.interpreter.parser.
    List<KeywordType> keywords;

    List<TypeRef> domain;

    TypeRef range;

    List<TypeRef> throws_;

    public ArrowType(Span span, TypeRef domain, TypeRef range,
		     List<TypeRef> throws_) {
        super(span);
	if (domain instanceof TupleType) {
	    this.domain   = ((TupleType)domain).getElements();
	    this.keywords = ((TupleType)domain).getKeywords();
	} else {
	    this.keywords = Collections.<KeywordType> emptyList();
	    List<TypeRef> domains = new ArrayList<TypeRef>();
	    domains.add(domain);
	    this.domain = domains;
	}
        this.range = range;
        this.throws_ = throws_;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forArrowType(this);
    }

    ArrowType(Span span) {
        super(span);
    }

    /**
     * @return Returns the domain.
     */
    public List<TypeRef> getDomain() {
        return domain;
    }

    /**
     * @return Returns the keywords.
     */
    public List<KeywordType> getKeywords() {
        return keywords;
    }

    /**
     * @return Returns the range.
     */
    public TypeRef getRange() {
        return range;
    }

    /**
     * @return Returns the throws_.
     */
    public List<TypeRef> getThrows_() {
        return throws_;
    }

    @Override
    public String toString() {
        return Useful.listsInParens(getDomain(), getKeywords())
                + "->"
                + getRange()
                + (getThrows_().size() > 0 ? (" throws " +
                        Useful.listInCurlies(getThrows_())) : "");
    }

    @Override
    int subtypeCompareTo(TypeRef o) {
        ArrowType a = (ArrowType) o;
        int x = NodeComparator.compare(range, a.range);
        if (x != 0) {
            return x;
        }
        x = NodeComparator.typeRefListComparer.compare(domain, a.domain);
        if (x != 0) {
            return x;
        }
        x = NodeComparator.keywordTypeListComparer.compare(keywords, a.keywords);
        if (x != 0) {
            return x;
        }
        x = NodeComparator.typeRefListComparer.compare(throws_, a.throws_);
        return x;
    }
}
