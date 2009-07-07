/*******************************************************************************
  Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.typechecker;

import java.util.HashMap;

import com.sun.fortress.compiler.typechecker.constraints.ConstraintFormula;
import com.sun.fortress.nodes.Type;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

public class ChildSubtypeCache extends SubtypeCache {

  private final SubtypeCache _parent;
  private final HashMap<Pair<Type,Type>, ConstraintFormula> _results;

  public ChildSubtypeCache(SubtypeCache parent) {
      _parent = parent;
      _results = new HashMap<Pair<Type,Type>, ConstraintFormula>();
  }

  public void put(Type s, Type t, SubtypeHistory h, ConstraintFormula c) {
      InferenceVarTranslator trans = new InferenceVarTranslator();
      _results.put(Pair.make(trans.canonicalizeVars(s), trans.canonicalizeVars(t)),
                   c.applySubstitution(trans.canonicalSubstitution()));
  }

  public Option<ConstraintFormula> get(Type s, Type t, SubtypeHistory h) {
      // we currently ignore the history, leading to incorrect results in some cases
      InferenceVarTranslator trans = new InferenceVarTranslator();
      ConstraintFormula result = _results.get(Pair.make(trans.canonicalizeVars(s),
                                                        trans.canonicalizeVars(t)));
      if (result == null) { return _parent.get(s, t, h); }
      else { return Option.some(result.applySubstitution(trans.revertingSubstitution())); }
  }

  public int size() { return _results.size() + _parent.size(); }

  public String toString() {
      return IterUtil.multilineToString(_results.entrySet()) + "\n=====\n" + _parent.toString();
  }
}
