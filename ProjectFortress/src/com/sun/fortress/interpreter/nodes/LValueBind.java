package com.sun.fortress.interpreter.nodes;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.*;

/**
 * Class LValueBind, a component of the Node composite hierarchy.
 * Note: null is not allowed as a value for any field.
 * @version  Generated automatically by ASTGen at Sun Jul 08 09:49:14 EDT 2007
 */
public class LValueBind extends LValue implements LHS {
  private final Id _name;
  private final Option<TypeRef> _type;
  private final List<Modifier> _mods;
  private final boolean _mutable;

  /**
   * Constructs a LValueBind.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public LValueBind(Span in_span, Id in_name, Option<TypeRef> in_type, List<Modifier> in_mods, boolean in_mutable) {
    super(in_span);

    if (in_name == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'name' to the LValueBind constructor was null");
    }
    _name = in_name;

    if (in_type == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'type' to the LValueBind constructor was null");
    }
    _type = in_type;

    if (in_mods == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'mods' to the LValueBind constructor was null");
    }
    _mods = in_mods;
    _mutable = in_mutable;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        // TODO Auto-generated method stub
        return v.forLValueBind(this);
    }

    public LValueBind(Span span) {
        super(span);
        _name = null;
        _type = null;
        _mods = null;
        _mutable = false;
    }

  final public Id getName() { return _name; }
  final public Option<TypeRef> getType() { return _type; }
  final public List<Modifier> getMods() { return _mods; }
  final public boolean isMutable() { return _mutable; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forLValueBind(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forLValueBind(this); }

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
    writer.print("LValueBind:");
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

    Option<TypeRef> temp_type = getType();
    writer.startLine();
    writer.print("type = ");
    if (lossless) {
      writer.printSerialized(temp_type);
      writer.print(" ");
      writer.printEscaped(temp_type);
    } else { writer.print(temp_type); }

    List<Modifier> temp_mods = getMods();
    writer.startLine();
    writer.print("mods = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_mods = true;
    for (Modifier elt_temp_mods : temp_mods) {
      isempty_temp_mods = false;
      writer.startLine("* ");
      if (elt_temp_mods == null) {
        writer.print("null");
      } else {
        if (lossless) {
          writer.printSerialized(elt_temp_mods);
          writer.print(" ");
          writer.printEscaped(elt_temp_mods);
        } else { writer.print(elt_temp_mods); }
      }
    }
    writer.unindent();
    if (isempty_temp_mods) writer.print(" }");
    else writer.startLine("}");

    boolean temp_mutable = isMutable();
    writer.startLine();
    writer.print("mutable = ");
    writer.print(temp_mutable);
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
      LValueBind casted = (LValueBind) obj;
      Id temp_name = getName();
      Id casted_name = casted.getName();
      if (!(temp_name == casted_name || temp_name.equals(casted_name))) return false;
      Option<TypeRef> temp_type = getType();
      Option<TypeRef> casted_type = casted.getType();
      if (!(temp_type == casted_type || temp_type.equals(casted_type))) return false;
      List<Modifier> temp_mods = getMods();
      List<Modifier> casted_mods = casted.getMods();
      if (!(temp_mods == casted_mods || temp_mods.equals(casted_mods))) return false;
      boolean temp_mutable = isMutable();
      boolean casted_mutable = casted.isMutable();
      if (!(temp_mutable == casted_mutable)) return false;
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
    Id temp_name = getName();
    code ^= temp_name.hashCode();
    Option<TypeRef> temp_type = getType();
    code ^= temp_type.hashCode();
    List<Modifier> temp_mods = getMods();
    code ^= temp_mods.hashCode();
    boolean temp_mutable = isMutable();
    code ^= temp_mutable ? 1231 : 1237;
    return code;
  }
}
