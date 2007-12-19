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

package com.sun.fortress.compiler.index;

import java.util.*;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.StaticParam;

import com.sun.fortress.useful.NI;

/**
 * Represents a possibly-parameterized type declaration.  Comprises
 * {@link TraitIndex} and {@link TypeAliasIndex}.
 */
public abstract class TypeConsIndex {
    private List<StaticParam> _staticParams;
    
    public TypeConsIndex(List<StaticParam> staticParams) {
        _staticParams = staticParams;
    }

    public List<StaticParam> staticParameters() {
        return _staticParams;
    }
    
}
