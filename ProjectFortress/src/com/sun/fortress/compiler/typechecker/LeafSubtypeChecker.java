/*******************************************************************************
  Copyright 2008 Sun Microsystems, Inc.,
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

import edu.rice.cs.plt.tuple.Option;
import com.sun.fortress.nodes.*;

public class LeafSubtypeChecker extends SubtypeChecker {

    public LeafSubtypeChecker(TraitTable table) {
        super(table, StaticParamEnv.make());
    }

    protected Option<Boolean> cacheContains(Type s, Type t) {
        return _cache.contains(s, t);
    }

    protected void cachePut(Type s, Type t, Boolean r) {
        _cache.put(s, t, r);
    }

}
