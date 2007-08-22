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
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.FnName;
import com.sun.fortress.nodes.DottedName;

public abstract class Environment {
    public abstract boolean hasVar(IdName name);
    public abstract boolean hasFn(FnName name);
    public abstract boolean hasTrait(IdName name);
    public abstract boolean hasTypeVar(IdName name);
    
    public abstract Option<DottedName> apiForVar(IdName name);
    public abstract Set<DottedName> apisForFn(FnName name);
    public abstract Option<DottedName> apiForTrait(IdName name);
}
