/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.disambiguator;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.Op;
import edu.rice.cs.plt.tuple.Pair;

import java.util.List;
import java.util.Set;

public abstract class NameEnv extends TypeNameEnv {

    /**
     * Produce the set of unaliased qualified names corresponding to the given
     * variable name; on-demand imports are ignored.  An undefined reference
     * produces an empty set, and an ambiguous reference produces a set of size greater
     * than 1.
     */
    public abstract Set<Id> explicitVariableNames(Id name);

    /**
     * Produce the set of all unaliased expilcit variable names in this environment.
     */
    public abstract List<Id> explicitVariableNames();

    /**
     * Produce the set of unaliased qualified names corresponding to the given
     * function name.  Imported names are included.  An undefined reference
     * produces an empty set.
     */
    public abstract Set<IdOrOp> explicitFunctionNames(IdOrOp name);

    /**
     * Produce the set of qualified, unambiguous names corresponding to the
     * given, possibly qualified function name.  
     */
    public abstract Set<IdOrOp> unambiguousFunctionNames(IdOrOp name);
    
    /**
     * Produces the set of tuples of qualified and unambiguous names for parametric operators
     */
    public abstract Set<Pair<Op, Op>> getParametricOperators();

    /**
     * Produce the set of unaliased qualified names available via on-demand imports
     * that correspond to the given variable name.  An undefined reference
     * produces an empty set, and an ambiguous reference produces a set of size
     * greater than 1.
     */
    public abstract Set<Id> onDemandVariableNames(Id name);

    /**
     * Produce the set of unaliased qualified names available via on-demand imports
     * that correspond to the given function name.  An undefined reference
     * produces an empty set.
     */
    public abstract Set<Id> onDemandFunctionNames(Id name);

    /**
     * Produce the set of unaliased qualified names available via on-demand imports
     * that correspond to the given operator name.  An undefined reference
     * produces an empty set.
     */
    public abstract Set<Op> onDemandFunctionNames(Op name);

    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * determine whether a variable exists.  Assumes {@code name.getApi().isSome()}.
     */
    public abstract boolean hasQualifiedVariable(Id name);

    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * determine whether a function exists.  Assumes {@code name.getApi().isSome()}.
     */
    public abstract boolean hasQualifiedFunction(Id name);

}
