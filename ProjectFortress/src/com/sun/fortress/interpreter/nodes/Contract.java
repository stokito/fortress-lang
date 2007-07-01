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

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.*;

public class Contract extends AbstractNode {
  private final List<Expr> _requires;
  private final List<EnsuresClause> _ensures;
  private final List<Expr> _invariants;

  /**
   * Constructs a Contract.
   * @throw java.lang.IllegalArgumentException if any parameter to the constructor is null.
   */
  public Contract(Span in_span, List<Expr> in_requires, List<EnsuresClause> in_ensures, List<Expr> in_invariants) {
    super(in_span);

    if (in_requires == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'requires' to the Contract constructor was null. This class may not have null field values.");
    }
    _requires = in_requires;

    if (in_ensures == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'ensures' to the Contract constructor was null. This class may not have null field values.");
    }
    _ensures = in_ensures;

    if (in_invariants == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'invariants' to the Contract constructor was null. This class may not have null field values.");
    }
    _invariants = in_invariants;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forContract(this);
    }

    Contract(Span span) {
        super(span);
        _requires = null;
        _ensures = null;
        _invariants = null;
    }

  final public List<Expr> getRequires() { return _requires; }
  final public List<EnsuresClause> getEnsures() { return _ensures; }
  final public List<Expr> getInvariants() { return _invariants; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forContract(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forContract(this); }

  /**
   * Implementation of toString that uses
   * {@see #output} to generated nicely tabbed tree.
   */
  public java.lang.String toString() {
    java.io.StringWriter w = new java.io.StringWriter();
    output(w);
    return w.toString();
  }

  /**
   * Prints this object out as a nicely tabbed tree.
   */
  public void output(java.io.Writer writer) {
    outputHelp(new TabPrintWriter(writer, 2));
  }

  public void outputHelp(TabPrintWriter writer) {
    writer.print("Contract" + ":");
    writer.indent();

    writer.startLine("");
    writer.print("span = ");
    Span temp_span = getSpan();
    if (temp_span == null) {
      writer.print("null");
    } else {
      writer.print(temp_span);
    }

    writer.startLine("");
    writer.print("requires = ");
    List<Expr> temp_requires = getRequires();
    if (temp_requires == null) {
      writer.print("null");
    } else {
      writer.print(temp_requires);
    }

    writer.startLine("");
    writer.print("ensures = ");
    List<EnsuresClause> temp_ensures = getEnsures();
    if (temp_ensures == null) {
      writer.print("null");
    } else {
      writer.print(temp_ensures);
    }

    writer.startLine("");
    writer.print("invariants = ");
    List<Expr> temp_invariants = getInvariants();
    if (temp_invariants == null) {
      writer.print("null");
    } else {
      writer.print(temp_invariants);
    }
    writer.unindent();
  }

  /**
   * Implementation of equals that is based on the values
   * of the fields of the object. Thus, two objects
   * created with identical parameters will be equal.
   */
  public boolean equals(java.lang.Object obj) {
    if (obj == null) return false;
    if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {
      return false;
    } else {
      Contract casted = (Contract) obj;
      if (! (getRequires().equals(casted.getRequires()))) return false;
      if (! (getEnsures().equals(casted.getEnsures()))) return false;
      if (! (getInvariants().equals(casted.getInvariants()))) return false;
      return true;
    }
  }

  /**
   * Implementation of hashCode that is consistent with
   * equals. The value of the hashCode is formed by
   * XORing the hashcode of the class object with
   * the hashcodes of all the fields of the object.
   */
  protected int generateHashCode() {
    int code = getClass().hashCode();
    code ^= 0;
    code ^= getRequires().hashCode();
    code ^= getEnsures().hashCode();
    code ^= getInvariants().hashCode();
    return code;
  }
}
