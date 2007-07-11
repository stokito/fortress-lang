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
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public Contract(Span in_span, List<Expr> in_requires, List<EnsuresClause> in_ensures, List<Expr> in_invariants) {
    super(in_span);

    if (in_requires == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'requires' to the Contract constructor was null");
    }
    _requires = in_requires;

    if (in_ensures == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'ensures' to the Contract constructor was null");
    }
    _ensures = in_ensures;

    if (in_invariants == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'invariants' to the Contract constructor was null");
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
   * {@link #output} to generate a nicely tabbed tree.
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
    outputHelp(new TabPrintWriter(writer, 2), false);
  }

  public void outputHelp(TabPrintWriter writer, boolean lossless) {
    writer.print("Contract:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<Expr> temp_requires = getRequires();
    writer.startLine();
    writer.print("requires = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_requires = true;
    for (Expr elt_temp_requires : temp_requires) {
      isempty_temp_requires = false;
      writer.startLine("* ");
      if (elt_temp_requires == null) {
        writer.print("null");
      } else {
        elt_temp_requires.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_requires) writer.print(" }");
    else writer.startLine("}");

    List<EnsuresClause> temp_ensures = getEnsures();
    writer.startLine();
    writer.print("ensures = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_ensures = true;
    for (EnsuresClause elt_temp_ensures : temp_ensures) {
      isempty_temp_ensures = false;
      writer.startLine("* ");
      if (elt_temp_ensures == null) {
        writer.print("null");
      } else {
        elt_temp_ensures.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_ensures) writer.print(" }");
    else writer.startLine("}");

    List<Expr> temp_invariants = getInvariants();
    writer.startLine();
    writer.print("invariants = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_invariants = true;
    for (Expr elt_temp_invariants : temp_invariants) {
      isempty_temp_invariants = false;
      writer.startLine("* ");
      if (elt_temp_invariants == null) {
        writer.print("null");
      } else {
        elt_temp_invariants.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_invariants) writer.print(" }");
    else writer.startLine("}");
    writer.unindent();
  }

  /**
   * Implementation of equals that is based on the values of the fields of the
   * object. Thus, two objects created with identical parameters will be equal.
   */
  public boolean equals(java.lang.Object obj) {
    if (obj == null) return false;
    if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {
      return false;
    } else {
      Contract casted = (Contract) obj;
      List<Expr> temp_requires = getRequires();
      List<Expr> casted_requires = casted.getRequires();
      if (!(temp_requires == casted_requires || temp_requires.equals(casted_requires))) return false;
      List<EnsuresClause> temp_ensures = getEnsures();
      List<EnsuresClause> casted_ensures = casted.getEnsures();
      if (!(temp_ensures == casted_ensures || temp_ensures.equals(casted_ensures))) return false;
      List<Expr> temp_invariants = getInvariants();
      List<Expr> casted_invariants = casted.getInvariants();
      if (!(temp_invariants == casted_invariants || temp_invariants.equals(casted_invariants))) return false;
      return true;
    }
  }

  /**
   * Implementation of hashCode that is consistent with equals.  The value of
   * the hashCode is formed by XORing the hashcode of the class object with
   * the hashcodes of all the fields of the object.
   */
  public int generateHashCode() {
    int code = getClass().hashCode();
    List<Expr> temp_requires = getRequires();
    code ^= temp_requires.hashCode();
    List<EnsuresClause> temp_ensures = getEnsures();
    code ^= temp_ensures.hashCode();
    List<Expr> temp_invariants = getInvariants();
    code ^= temp_invariants.hashCode();
    return code;
  }
}
