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

public class ObjectDecl extends ObjectDefOrDecl implements GenericDefOrDeclWithParams, AbsDecl {
  private final List<? extends DefOrDecl> _defOrDecls;

  /**
   * Constructs a ObjectDecl.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public ObjectDecl(Span in_span, List<Modifier> in_mods, Id in_name, Option<List<StaticParam>> in_staticParams, Option<List<Param>> in_params, Option<List<TypeRef>> in_traits, List<TypeRef> in_throwsClause, List<WhereClause> in_where, Contract in_contract, List<? extends DefOrDecl> in_defOrDecls) {
    super(in_span, in_mods, in_name, in_staticParams, in_params, in_traits, in_throwsClause, in_where, in_contract);

    if (in_defOrDecls == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'defOrDecls' to the ObjectDecl constructor was null");
    }
    _defOrDecls = in_defOrDecls;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forObjectDecl(this);
    }

    ObjectDecl(Span span) {
        super(span);
        _defOrDecls = null;
    }

  final public List<? extends DefOrDecl> getDefOrDecls() { return _defOrDecls; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forObjectDecl(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forObjectDecl(this); }

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
    writer.print("ObjectDecl:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

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

    Id temp_name = getName();
    writer.startLine();
    writer.print("name = ");
    temp_name.outputHelp(writer, lossless);

    Option<List<StaticParam>> temp_staticParams = getStaticParams();
    writer.startLine();
    writer.print("staticParams = ");
    if (lossless) {
      writer.printSerialized(temp_staticParams);
      writer.print(" ");
      writer.printEscaped(temp_staticParams);
    } else { writer.print(temp_staticParams); }

    Option<List<Param>> temp_params = getParams();
    writer.startLine();
    writer.print("params = ");
    if (lossless) {
      writer.printSerialized(temp_params);
      writer.print(" ");
      writer.printEscaped(temp_params);
    } else { writer.print(temp_params); }

    Option<List<TypeRef>> temp_traits = getTraits();
    writer.startLine();
    writer.print("traits = ");
    if (lossless) {
      writer.printSerialized(temp_traits);
      writer.print(" ");
      writer.printEscaped(temp_traits);
    } else { writer.print(temp_traits); }

    List<TypeRef> temp_throwsClause = getThrowsClause();
    writer.startLine();
    writer.print("throwsClause = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_throwsClause = true;
    for (TypeRef elt_temp_throwsClause : temp_throwsClause) {
      isempty_temp_throwsClause = false;
      writer.startLine("* ");
      if (elt_temp_throwsClause == null) {
        writer.print("null");
      } else {
        elt_temp_throwsClause.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_throwsClause) writer.print(" }");
    else writer.startLine("}");

    List<WhereClause> temp_where = getWhere();
    writer.startLine();
    writer.print("where = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_where = true;
    for (WhereClause elt_temp_where : temp_where) {
      isempty_temp_where = false;
      writer.startLine("* ");
      if (elt_temp_where == null) {
        writer.print("null");
      } else {
        elt_temp_where.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_where) writer.print(" }");
    else writer.startLine("}");

    Contract temp_contract = getContract();
    writer.startLine();
    writer.print("contract = ");
    temp_contract.outputHelp(writer, lossless);

    List<? extends DefOrDecl> temp_defOrDecls = getDefOrDecls();
    writer.startLine();
    writer.print("defOrDecls = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_defOrDecls = true;
    for (DefOrDecl elt_temp_defOrDecls : temp_defOrDecls) {
      isempty_temp_defOrDecls = false;
      writer.startLine("* ");
      if (elt_temp_defOrDecls == null) {
        writer.print("null");
      } else {
        elt_temp_defOrDecls.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_defOrDecls) writer.print(" }");
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
      ObjectDecl casted = (ObjectDecl) obj;
      List<Modifier> temp_mods = getMods();
      List<Modifier> casted_mods = casted.getMods();
      if (!(temp_mods == casted_mods || temp_mods.equals(casted_mods))) return false;
      Id temp_name = getName();
      Id casted_name = casted.getName();
      if (!(temp_name == casted_name || temp_name.equals(casted_name))) return false;
      Option<List<StaticParam>> temp_staticParams = getStaticParams();
      Option<List<StaticParam>> casted_staticParams = casted.getStaticParams();
      if (!(temp_staticParams == casted_staticParams || temp_staticParams.equals(casted_staticParams))) return false;
      Option<List<Param>> temp_params = getParams();
      Option<List<Param>> casted_params = casted.getParams();
      if (!(temp_params == casted_params || temp_params.equals(casted_params))) return false;
      Option<List<TypeRef>> temp_traits = getTraits();
      Option<List<TypeRef>> casted_traits = casted.getTraits();
      if (!(temp_traits == casted_traits || temp_traits.equals(casted_traits))) return false;
      List<TypeRef> temp_throwsClause = getThrowsClause();
      List<TypeRef> casted_throwsClause = casted.getThrowsClause();
      if (!(temp_throwsClause == casted_throwsClause || temp_throwsClause.equals(casted_throwsClause))) return false;
      List<WhereClause> temp_where = getWhere();
      List<WhereClause> casted_where = casted.getWhere();
      if (!(temp_where == casted_where || temp_where.equals(casted_where))) return false;
      Contract temp_contract = getContract();
      Contract casted_contract = casted.getContract();
      if (!(temp_contract == casted_contract || temp_contract.equals(casted_contract))) return false;
      List<? extends DefOrDecl> temp_defOrDecls = getDefOrDecls();
      List<? extends DefOrDecl> casted_defOrDecls = casted.getDefOrDecls();
      if (!(temp_defOrDecls == casted_defOrDecls || temp_defOrDecls.equals(casted_defOrDecls))) return false;
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
    List<Modifier> temp_mods = getMods();
    code ^= temp_mods.hashCode();
    Id temp_name = getName();
    code ^= temp_name.hashCode();
    Option<List<StaticParam>> temp_staticParams = getStaticParams();
    code ^= temp_staticParams.hashCode();
    Option<List<Param>> temp_params = getParams();
    code ^= temp_params.hashCode();
    Option<List<TypeRef>> temp_traits = getTraits();
    code ^= temp_traits.hashCode();
    List<TypeRef> temp_throwsClause = getThrowsClause();
    code ^= temp_throwsClause.hashCode();
    List<WhereClause> temp_where = getWhere();
    code ^= temp_where.hashCode();
    Contract temp_contract = getContract();
    code ^= temp_contract.hashCode();
    List<? extends DefOrDecl> temp_defOrDecls = getDefOrDecls();
    code ^= temp_defOrDecls.hashCode();
    return code;
  }
}
