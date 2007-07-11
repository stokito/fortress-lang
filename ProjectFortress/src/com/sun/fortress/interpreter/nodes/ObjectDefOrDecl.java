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

public abstract class ObjectDefOrDecl extends AbstractNode implements Generic, DefOrDecl {
  private final List<Modifier> _mods;
  private final Id _name;
  private final Option<List<StaticParam>> _staticParams;
  private final Option<List<Param>> _params;
  private final Option<List<TypeRef>> _traits;
  private final List<TypeRef> _throwsClause;
  private final List<WhereClause> _where;
  private final Contract _contract;

  /**
   * Constructs a ObjectDefOrDecl.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public ObjectDefOrDecl(Span in_span, List<Modifier> in_mods, Id in_name, Option<List<StaticParam>> in_staticParams, Option<List<Param>> in_params, Option<List<TypeRef>> in_traits, List<TypeRef> in_throwsClause, List<WhereClause> in_where, Contract in_contract) {
    super(in_span);

    if (in_mods == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'mods' to the ObjectDefOrDecl constructor was null");
    }
    _mods = in_mods;

    if (in_name == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'name' to the ObjectDefOrDecl constructor was null");
    }
    _name = in_name;

    if (in_staticParams == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'staticParams' to the ObjectDefOrDecl constructor was null");
    }
    _staticParams = in_staticParams;

    if (in_params == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'params' to the ObjectDefOrDecl constructor was null");
    }
    _params = in_params;

    if (in_traits == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'traits' to the ObjectDefOrDecl constructor was null");
    }
    _traits = in_traits;

    if (in_throwsClause == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'throwsClause' to the ObjectDefOrDecl constructor was null");
    }
    _throwsClause = in_throwsClause;

    if (in_where == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'where' to the ObjectDefOrDecl constructor was null");
    }
    _where = in_where;

    if (in_contract == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'contract' to the ObjectDefOrDecl constructor was null");
    }
    _contract = in_contract;
  }

    ObjectDefOrDecl(Span s) {
        super(s);
        _mods = null;
        _name = null;
        _staticParams = null;
        _params = null;
        _traits = null;
        _throwsClause = null;
        _where = null;
        _contract = null;
    }

  public List<Modifier> getMods() { return _mods; }
  public Id getName() { return _name; }
  public Option<List<StaticParam>> getStaticParams() { return _staticParams; }
  public Option<List<Param>> getParams() { return _params; }
  public Option<List<TypeRef>> getTraits() { return _traits; }
  public List<TypeRef> getThrowsClause() { return _throwsClause; }
  public List<WhereClause> getWhere() { return _where; }
  public Contract getContract() { return _contract; }

  public abstract <RetType> RetType visit(NodeVisitor<RetType> visitor);
  public abstract void visit(NodeVisitor_void visitor);
  public abstract void output(java.io.Writer writer);
  public abstract void outputHelp(TabPrintWriter writer, boolean lossless);
  public abstract int generateHashCode();
}
