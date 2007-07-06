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

public class Binding extends AbstractNode {
  private final Id _name;
  private final Expr _init;

  /**
   * Constructs a Binding.
   * @throw java.lang.IllegalArgumentException if any parameter to the constructor is null.
   */
  public Binding(Span in_span, Id in_name, Expr in_init) {
    super(in_span);

    if (in_name == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'name' to the Binding constructor was null. This class may not have null field values.");
    }
    _name = in_name;

    if (in_init == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'init' to the Binding constructor was null. This class may not have null field values.");
    }
    _init = in_init;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forBinding(this);
    }

    Binding(Span span) {
        super(span);
        _name = null;
        _init = null;
    }

  final public Id getName() { return _name; }
  final public Expr getInit() { return _init; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forBinding(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forBinding(this); }

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
    outputHelp(new TabPrintWriter(writer, 2), false);
  }

  protected void outputHelp(TabPrintWriter writer, boolean lossless) {
    writer.print("Binding:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Id temp_name = getName();
    writer.startLine();
    writer.print("name = ");
    temp_name.outputHelp(writer, lossless);

    Expr temp_init = getInit();
    writer.startLine();
    writer.print("init = ");
    temp_init.outputHelp(writer, lossless);
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
      Binding casted = (Binding) obj;
      if (! (getName().equals(casted.getName()))) return false;
      if (! (getInit().equals(casted.getInit()))) return false;
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
    code ^= getName().hashCode();
    code ^= getInit().hashCode();
    return code;
  }
}
