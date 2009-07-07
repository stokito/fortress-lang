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

/**
 * A mutable collection of subtyping results from previously-completed invocations
 * of subtyping in a specific type context.
 */
public abstract class SubtypeCache {
    public abstract void put(Type s, Type t, SubtypeHistory h, ConstraintFormula c);
    public abstract Option<ConstraintFormula> get(Type s, Type t, SubtypeHistory h);
    public abstract int size();
}
