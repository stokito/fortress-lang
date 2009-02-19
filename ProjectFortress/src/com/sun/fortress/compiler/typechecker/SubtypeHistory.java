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

import com.sun.fortress.compiler.typechecker.constraints.ConstraintFormula;
import com.sun.fortress.nodes.Type;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import static com.sun.fortress.compiler.typechecker.constraints.ConstraintUtil.*;

/** An immutable record of all subtyping invocations in the call stack. */
// Package private -- accessed by ConstraintFormula
public class SubtypeHistory {
    private final TypeAnalyzer _analyzer;
    private final Relation<Type, Type> _entries;
    private final int _expansions;

    public SubtypeHistory(TypeAnalyzer analyzer) {
        _analyzer = analyzer;
        _entries = CollectUtil.emptyRelation();
        _expansions = 0;
    }

    private SubtypeHistory(TypeAnalyzer analyzer, Relation<Type, Type> entries, int expansions) {
        _analyzer = analyzer;
        _entries = entries;
        _expansions = expansions;
    }

    public int size() { return _entries.size(); }

    public int expansions() { return _expansions; }

    public boolean contains(Type s, Type t) {
        InferenceVarTranslator trans = new InferenceVarTranslator();
        return _entries.contains(trans.canonicalizeVars(s), trans.canonicalizeVars(t));
    }

    public SubtypeHistory extend(Type s, Type t) {
        InferenceVarTranslator trans = new InferenceVarTranslator();
        Relation<Type, Type> newEntries =
            CollectUtil.union(_entries, trans.canonicalizeVars(s), trans.canonicalizeVars(t));
        newEntries = CollectUtil.conditionalSnapshot(newEntries, 8);
        return new SubtypeHistory(_analyzer, newEntries, _expansions);
    }

    public SubtypeHistory expand() {
        return new SubtypeHistory(_analyzer, _entries, _expansions + 1);
    }

    public Type normalize(Type t) {
        return _analyzer.normalize(t);
    }

    public ConstraintFormula subtypeNormal(Type s, Type t) {
        return _analyzer.sub(s, t, this);
    }

    public Type meetNormal(Type... ts) {
        return _analyzer.mt(IterUtil.asIterable(ts), this);
    }

    public Type joinNormal(Type... ts) {
        return _analyzer.jn(IterUtil.asIterable(ts), this);
    }

    public String toString() {
        return IterUtil.multilineToString(_entries) + "\n" + _expansions + " expansions";
    }
}