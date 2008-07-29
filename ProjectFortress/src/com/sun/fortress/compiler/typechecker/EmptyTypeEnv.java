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

import java.util.Collections;
import java.util.List;

import com.sun.fortress.nodes.*;

import edu.rice.cs.plt.tuple.Option;

import static edu.rice.cs.plt.tuple.Option.*;

class EmptyTypeEnv extends TypeEnv {
    public static final EmptyTypeEnv ONLY = new EmptyTypeEnv();
    
    private EmptyTypeEnv() {}
    
    private RuntimeException error() { throw new RuntimeException("Attempt to lookup in an EmptyTypeEnv."); }
    
    public Option<BindingLookup> binding(IdOrOpOrAnonymousName var) { return none(); }

    @Override
    public List<BindingLookup> contents() {
        return Collections.emptyList();
    }

	@Override
	public Option<Node> declarationSite(IdOrOpOrAnonymousName var) {
		return none();
	}
}