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

public class ProductUnitType extends DimType {
  private final TypeRef _multiplier;
  private final UnitRef _multiplicand;

  /**
   * Constructs a ProductUnitType.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public ProductUnitType(Span in_span, TypeRef in_multiplier, UnitRef in_multiplicand) {
    super(in_span);

    if (in_multiplier == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'multiplier' to the ProductUnitType constructor was null");
    }
    _multiplier = in_multiplier;

    if (in_multiplicand == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'multiplicand' to the ProductUnitType constructor was null");
    }
    _multiplicand = in_multiplicand;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forProductUnitType(this);
    }

    ProductUnitType(Span span) {
        super(span);
        _multiplier = null;
        _multiplicand = null;
    }

  final public TypeRef getMultiplier() { return _multiplier; }
  final public UnitRef getMultiplicand() { return _multiplicand; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forProductUnitType(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forProductUnitType(this); }

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
    writer.print("ProductUnitType:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    TypeRef temp_multiplier = getMultiplier();
    writer.startLine();
    writer.print("multiplier = ");
    temp_multiplier.outputHelp(writer, lossless);

    UnitRef temp_multiplicand = getMultiplicand();
    writer.startLine();
    writer.print("multiplicand = ");
    temp_multiplicand.outputHelp(writer, lossless);
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
      ProductUnitType casted = (ProductUnitType) obj;
      TypeRef temp_multiplier = getMultiplier();
      TypeRef casted_multiplier = casted.getMultiplier();
      if (!(temp_multiplier == casted_multiplier || temp_multiplier.equals(casted_multiplier))) return false;
      UnitRef temp_multiplicand = getMultiplicand();
      UnitRef casted_multiplicand = casted.getMultiplicand();
      if (!(temp_multiplicand == casted_multiplicand || temp_multiplicand.equals(casted_multiplicand))) return false;
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
    TypeRef temp_multiplier = getMultiplier();
    code ^= temp_multiplier.hashCode();
    UnitRef temp_multiplicand = getMultiplicand();
    code ^= temp_multiplicand.hashCode();
    return code;
  }
}
