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

package com.sun.fortress.compiler.disambiguator;

import java.util.Set;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.QualifiedOpName;

public abstract class NameEnv extends TypeNameEnv {
    
    /**
     * Produce the set of unaliased qualified names corresponding to the given
     * variable name; on-demand imports are ignored.  An undefined reference
     * produces an empty set, and an ambiguous reference produces a set of size greater
     * than 1.
     */
    public abstract Set<QualifiedIdName> explicitVariableNames(Id name);
    /**
     * Produce the set of unaliased qualified names corresponding to the given
     * function name; on-demand imports are ignored.  An undefined reference
     * produces an empty set.
     */
    public abstract Set<QualifiedIdName> explicitFunctionNames(Id name);
    /**
     * Produce the set of unaliased qualified names corresponding to the given
     * operator name; on-demand imports are ignored.  An undefined reference
     * produces an empty set.
     */
    public abstract Set<QualifiedOpName> explicitFunctionNames(OpName name);
    
    /**
     * Produce the set of unaliased qualified names available via on-demand imports
     * that correspond to the given variable name.  An undefined reference
     * produces an empty set, and an ambiguous reference produces a set of size 
     * greater than 1.
     */
    public abstract Set<QualifiedIdName> onDemandVariableNames(Id name);
    /**
     * Produce the set of unaliased qualified names available via on-demand imports
     * that correspond to the given function name.  An undefined reference
     * produces an empty set.
     */
    public abstract Set<QualifiedIdName> onDemandFunctionNames(Id name);
    /**
     * Produce the set of unaliased qualified names available via on-demand imports
     * that correspond to the given operator name.  An undefined reference
     * produces an empty set.
     */
    public abstract Set<QualifiedOpName> onDemandFunctionNames(OpName name);
    
    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * determine whether a variable exists.  Assumes {@code name.getApi().isSome()}.
     */
    public abstract boolean hasQualifiedVariable(QualifiedIdName name);
    /**
     * Given a disambiguated name (aliases and imports have been resolved),
     * determine whether a function exists.  Assumes {@code name.getApi().isSome()}.
     */
    public abstract boolean hasQualifiedFunction(QualifiedIdName name);

}
