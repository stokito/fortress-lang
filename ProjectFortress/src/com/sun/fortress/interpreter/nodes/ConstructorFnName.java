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
import com.sun.fortress.interpreter.glue.WellKnownNames;

public class ConstructorFnName extends OprName {
  private final DefOrDecl _def;

  /**
   * Constructs a ConstructorFnName.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public ConstructorFnName(Span in_span, DefOrDecl in_def) {
    super(in_span);

    if (in_def == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'def' to the ConstructorFnName constructor was null");
    }
    _def = in_def;
  }

    ConstructorFnName(Span span) {
        super(span);
        _def = null;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return NI.<T> na();
    }

  final public DefOrDecl getDef() { return _def; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forConstructorFnName(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forConstructorFnName(this); }

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
    writer.print("ConstructorFnName:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    DefOrDecl temp_def = getDef();
    writer.startLine();
    writer.print("def = ");
    temp_def.outputHelp(writer, lossless);
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
      ConstructorFnName casted = (ConstructorFnName) obj;
      DefOrDecl temp_def = getDef();
      DefOrDecl casted_def = casted.getDef();
      if (!(temp_def == casted_def || temp_def.equals(casted_def))) return false;
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
    DefOrDecl temp_def = getDef();
    code ^= temp_def.hashCode();
    return code;
  }
}
