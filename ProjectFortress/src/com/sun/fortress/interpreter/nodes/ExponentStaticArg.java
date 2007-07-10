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

public class ExponentStaticArg extends CompoundStaticArg {
  private final TypeRef _base;
  private final TypeRef _power;

  /**
   * Constructs a ExponentStaticArg.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public ExponentStaticArg(Span in_span, TypeRef in_base, TypeRef in_power) {
    super(in_span);

    if (in_base == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'base' to the ExponentStaticArg constructor was null");
    }
    _base = in_base;

    if (in_power == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'power' to the ExponentStaticArg constructor was null");
    }
    _power = in_power;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forExponentStaticArg(this);
    }

    ExponentStaticArg(Span span) {
        super(span);
        _base = null;
        _power = null;
    }

  final public TypeRef getBase() { return _base; }
  final public TypeRef getPower() { return _power; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forExponentStaticArg(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forExponentStaticArg(this); }

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
    writer.print("ExponentStaticArg:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    TypeRef temp_base = getBase();
    writer.startLine();
    writer.print("base = ");
    temp_base.outputHelp(writer, lossless);

    TypeRef temp_power = getPower();
    writer.startLine();
    writer.print("power = ");
    temp_power.outputHelp(writer, lossless);
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
      ExponentStaticArg casted = (ExponentStaticArg) obj;
      TypeRef temp_base = getBase();
      TypeRef casted_base = casted.getBase();
      if (!(temp_base == casted_base || temp_base.equals(casted_base))) return false;
      TypeRef temp_power = getPower();
      TypeRef casted_power = casted.getPower();
      if (!(temp_power == casted_power || temp_power.equals(casted_power))) return false;
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
    TypeRef temp_base = getBase();
    code ^= temp_base.hashCode();
    TypeRef temp_power = getPower();
    code ^= temp_power.hashCode();
    return code;
  }
}
