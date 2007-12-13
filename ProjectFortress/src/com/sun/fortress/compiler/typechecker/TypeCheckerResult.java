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

package com.sun.fortress.compiler.typechecker;

import java.util.*;
import com.sun.fortress.compiler.*;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.DottedName;
import com.sun.fortress.nodes.Node;

import edu.rice.cs.plt.iter.IterUtil;

public class TypeCheckerResult extends StaticPhaseResult {
    private final Node _ast;
    public TypeCheckerResult(Node ast, 
                             Iterable<? extends StaticError> errors) {
        super(errors);
        _ast = ast;
    }
    public Node ast() { return _ast; }
}