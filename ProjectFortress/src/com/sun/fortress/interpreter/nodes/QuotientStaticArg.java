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

public class QuotientStaticArg extends CompoundStaticArg {
  private final TypeRef _numerator;
  private final TypeRef _denominator;

  /**
   * Constructs a QuotientStaticArg.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public QuotientStaticArg(Span in_span, TypeRef in_numerator, TypeRef in_denominator) {
    super(in_span);

    if (in_numerator == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'numerator' to the QuotientStaticArg constructor was null");
    }
    _numerator = in_numerator;

    if (in_denominator == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'denominator' to the QuotientStaticArg constructor was null");
    }
    _denominator = in_denominator;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forQuotientStaticArg(this);
    }

    QuotientStaticArg(Span span) {
        super(span);
        _numerator = null;
        _denominator = null;
    }

  final public TypeRef getNumerator() { return _numerator; }
  final public TypeRef getDenominator() { return _denominator; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forQuotientStaticArg(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forQuotientStaticArg(this); }

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

  protected void outputHelp(TabPrintWriter writer, boolean lossless) {
    writer.print("QuotientStaticArg:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    TypeRef temp_numerator = getNumerator();
    writer.startLine();
    writer.print("numerator = ");
    temp_numerator.outputHelp(writer, lossless);

    TypeRef temp_denominator = getDenominator();
    writer.startLine();
    writer.print("denominator = ");
    temp_denominator.outputHelp(writer, lossless);
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
      QuotientStaticArg casted = (QuotientStaticArg) obj;
      TypeRef temp_numerator = getNumerator();
      TypeRef casted_numerator = casted.getNumerator();
      if (!(temp_numerator == casted_numerator || temp_numerator.equals(casted_numerator))) return false;
      TypeRef temp_denominator = getDenominator();
      TypeRef casted_denominator = casted.getDenominator();
      if (!(temp_denominator == casted_denominator || temp_denominator.equals(casted_denominator))) return false;
      return true;
    }
  }

  /**
   * Implementation of hashCode that is consistent with equals.  The value of
   * the hashCode is formed by XORing the hashcode of the class object with
   * the hashcodes of all the fields of the object.
   */
  protected int generateHashCode() {
    int code = getClass().hashCode();
    TypeRef temp_numerator = getNumerator();
    code ^= temp_numerator.hashCode();
    TypeRef temp_denominator = getDenominator();
    code ^= temp_denominator.hashCode();
    return code;
  }
}
