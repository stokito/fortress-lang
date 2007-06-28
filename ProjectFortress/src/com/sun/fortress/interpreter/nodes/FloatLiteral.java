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

public class FloatLiteral extends NumberLiteral {
  private final BigInteger _intPart;
  private final BigInteger _numerator;
  private final int _denomBase;
  private final int _denomPower;

  /**
   * Constructs a FloatLiteral.
   * @throw java.lang.IllegalArgumentException if any parameter to the constructor is null.
   */
  public FloatLiteral(Span in_span, String in_text, BigInteger in_intPart, BigInteger in_numerator, int in_denomBase, int in_denomPower) {
    super(in_span, in_text);

    if (in_intPart == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'intPart' to the FloatLiteral constructor was null. This class may not have null field values.");
    }
    _intPart = in_intPart;

    if (in_numerator == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'numerator' to the FloatLiteral constructor was null. This class may not have null field values.");
    }
    _numerator = in_numerator;
    _denomBase = in_denomBase;
    _denomPower = in_denomPower;
  }

    public FloatLiteral(Span in_span) {
        super(in_span);
        _intPart = null;
        _numerator = null;
        _denomBase = 0;
        _denomPower = 0;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forFloatLiteral(this);
    }

  final public BigInteger getIntPart() { return _intPart; }
  final public BigInteger getNumerator() { return _numerator; }
  final public int getDenomBase() { return _denomBase; }
  final public int getDenomPower() { return _denomPower; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forFloatLiteral(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forFloatLiteral(this); }

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
    writer.print("FloatLiteral" + ":");
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
    writer.print("text = ");
    String temp_text = getText();
    if (temp_text == null) {
      writer.print("null");
    } else {
      writer.print(temp_text);
    }

    writer.startLine("");
    writer.print("intPart = ");
    BigInteger temp_intPart = getIntPart();
    if (temp_intPart == null) {
      writer.print("null");
    } else {
      writer.print(temp_intPart);
    }

    writer.startLine("");
    writer.print("numerator = ");
    BigInteger temp_numerator = getNumerator();
    if (temp_numerator == null) {
      writer.print("null");
    } else {
      writer.print(temp_numerator);
    }

    writer.startLine("");
    writer.print("denomBase = ");
    int temp_denomBase = getDenomBase();
    writer.print(temp_denomBase);

    writer.startLine("");
    writer.print("denomPower = ");
    int temp_denomPower = getDenomPower();
    writer.print(temp_denomPower);
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
      FloatLiteral casted = (FloatLiteral) obj;
      if (! (getIntPart().equals(casted.getIntPart()))) return false;
      if (! (getNumerator().equals(casted.getNumerator()))) return false;
      if (! (getDenomBase() == casted.getDenomBase())) return false;
      if (! (getDenomPower() == casted.getDenomPower())) return false;
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
    code ^= getIntPart().hashCode();
    code ^= getNumerator().hashCode();
    code ^= getDenomBase();
    code ^= getDenomPower();
    return code;
  }
}
