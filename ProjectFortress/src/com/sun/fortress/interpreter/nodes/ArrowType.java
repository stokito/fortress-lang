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

    List<TypeRef> domain;

    TypeRef range;

    List<TypeRef> throws_;

    public ArrowType(Span span, TypeRef domain, TypeRef range,
		     List<TypeRef> throws_) {
        super(span);
	if (domain instanceof TupleType) {
	    this.domain   = ((TupleType)domain).getElements();
	} else {
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
        return Useful.listInParens(getDomain())
                + "->"
                + getRange()
                + (getThrows_().size() > 0 ? (" throws " +
                        Useful.listInCurlies(getThrows_())) : "");
    }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forArrowType(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forArrowType(this); }
  /**
   * Prints this object out as a nicely tabbed tree.
   */
    public void output(java.io.Writer writer) {}

    public void outputHelp(TabPrintWriter writer, boolean lossless) {}
  /**
   * Implementation of hashCode that is consistent with equals.  The value of
   * the hashCode is formed by XORing the hashcode of the class object with
   * the hashcodes of all the fields of the object.
   */
    public int generateHashCode() { return hashCode(); }
}
