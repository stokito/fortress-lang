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
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.*;

public abstract class TraitDefOrDecl extends AbstractNode implements Generic, HasWhere {
  private final List<Modifier> _mods;
  private final Id _name;
  private final Option<List<StaticParam>> _staticParams;
  private final Option<List<TypeRef>> _extendsClause;
  private final List<TypeRef> _excludes;
  private final Option<List<TypeRef>> _bounds;
  private final List<WhereClause> _where;
  private final List<? extends DefOrDecl> _fns;

  /**
   * Constructs a TraitDefOrDecl.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public TraitDefOrDecl(Span in_span, List<Modifier> in_mods, Id in_name, Option<List<StaticParam>> in_staticParams, Option<List<TypeRef>> in_extendsClause, List<TypeRef> in_excludes, Option<List<TypeRef>> in_bounds, List<WhereClause> in_where, List<? extends DefOrDecl> in_fns) {
    super(in_span);

    if (in_mods == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'mods' to the TraitDefOrDecl constructor was null");
    }
    _mods = in_mods;

    if (in_name == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'name' to the TraitDefOrDecl constructor was null");
    }
    _name = in_name;

    if (in_staticParams == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'staticParams' to the TraitDefOrDecl constructor was null");
    }
    _staticParams = in_staticParams;

    if (in_extendsClause == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'extendsClause' to the TraitDefOrDecl constructor was null");
    }
    _extendsClause = in_extendsClause;

    if (in_excludes == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'excludes' to the TraitDefOrDecl constructor was null");
    }
    _excludes = in_excludes;

    if (in_bounds == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'bounds' to the TraitDefOrDecl constructor was null");
    }
    _bounds = in_bounds;

    if (in_where == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'where' to the TraitDefOrDecl constructor was null");
    }
    _where = in_where;

    if (in_fns == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'fns' to the TraitDefOrDecl constructor was null");
    }
    _fns = in_fns;
  }

  /**
   * Empty constructor, for reflective access.  Clients are
   * responsible for manually instantiating each field.
   */
  protected TraitDefOrDecl() {
    _mods = null;
    _name = null;
    _staticParams = null;
    _extendsClause = null;
    _excludes = null;
    _bounds = null;
    _where = null;
    _fns = null;
  }

  protected TraitDefOrDecl(Span span) {
      super(span);
    _mods = null;
    _name = null;
    _staticParams = null;
    _extendsClause = null;
    _excludes = null;
    _bounds = null;
    _where = null;
    _fns = null;
  }

  public List<Modifier> getMods() { return _mods; }
  public Id getName() { return _name; }
  public Option<List<StaticParam>> getStaticParams() { return _staticParams; }
  public Option<List<TypeRef>> getExtendsClause() { return _extendsClause; }
  public List<TypeRef> getExcludes() { return _excludes; }
  public Option<List<TypeRef>> getBounds() { return _bounds; }
  public List<WhereClause> getWhere() { return _where; }
  public List<? extends DefOrDecl> getFns() { return _fns; }

  public abstract <RetType> RetType visit(NodeVisitor<RetType> visitor);
  public abstract void visit(NodeVisitor_void visitor);
  public abstract void output(java.io.Writer writer);
  public abstract void outputHelp(TabPrintWriter writer, boolean lossless);
  public abstract int generateHashCode();
}
