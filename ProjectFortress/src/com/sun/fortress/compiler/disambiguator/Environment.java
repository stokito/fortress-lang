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

package com.sun.fortress.compiler;

import java.util.Set;
import edu.rice.cs.plt.tuple.Option;

public abstract class Environment {
    public abstract boolean hasVar(String name);
    public abstract boolean hasFn(String name);
    public abstract boolean hasMethod(String name);
    public abstract boolean hasType(String name);
    
    public abstract Option<String> apiForVar(String name);
    public abstract Set<String> apisForFn(String name);
}
