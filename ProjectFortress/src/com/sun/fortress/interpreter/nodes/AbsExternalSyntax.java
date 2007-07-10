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

public class AbsExternalSyntax extends AbstractNode implements Decl, AbsDecl {
  private final Name _openExpander;
  private final Id _id;
  private final Name _closeExpander;

  /**
   * Constructs a AbsExternalSyntax.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public AbsExternalSyntax(Span in_span, Name in_openExpander, Id in_id, Name in_closeExpander) {
    super(in_span);

    if (in_openExpander == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'openExpander' to the AbsExternalSyntax constructor was null");
    }
    _openExpander = in_openExpander;

    if (in_id == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'id' to the AbsExternalSyntax constructor was null");
    }
    _id = in_id;

    if (in_closeExpander == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'closeExpander' to the AbsExternalSyntax constructor was null");
    }
    _closeExpander = in_closeExpander;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forAbsExternalSyntax(this);
    }

    AbsExternalSyntax(Span span) {
        super(span);
        _openExpander = null;
        _id = null;
        _closeExpander = null;
    }

  final public Name getOpenExpander() { return _openExpander; }
  final public Id getId() { return _id; }
  final public Name getCloseExpander() { return _closeExpander; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forAbsExternalSyntax(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forAbsExternalSyntax(this); }

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
    writer.print("AbsExternalSyntax:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Name temp_openExpander = getOpenExpander();
    writer.startLine();
    writer.print("openExpander = ");
    temp_openExpander.outputHelp(writer, lossless);

    Id temp_id = getId();
    writer.startLine();
    writer.print("id = ");
    temp_id.outputHelp(writer, lossless);

    Name temp_closeExpander = getCloseExpander();
    writer.startLine();
    writer.print("closeExpander = ");
    temp_closeExpander.outputHelp(writer, lossless);
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
      AbsExternalSyntax casted = (AbsExternalSyntax) obj;
      Name temp_openExpander = getOpenExpander();
      Name casted_openExpander = casted.getOpenExpander();
      if (!(temp_openExpander == casted_openExpander || temp_openExpander.equals(casted_openExpander))) return false;
      Id temp_id = getId();
      Id casted_id = casted.getId();
      if (!(temp_id == casted_id || temp_id.equals(casted_id))) return false;
      Name temp_closeExpander = getCloseExpander();
      Name casted_closeExpander = casted.getCloseExpander();
      if (!(temp_closeExpander == casted_closeExpander || temp_closeExpander.equals(casted_closeExpander))) return false;
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
    Name temp_openExpander = getOpenExpander();
    code ^= temp_openExpander.hashCode();
    Id temp_id = getId();
    code ^= temp_id.hashCode();
    Name temp_closeExpander = getCloseExpander();
    code ^= temp_closeExpander.hashCode();
    return code;
  }
}
