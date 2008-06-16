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

package com.sun.fortress.syntax_abstractions.phases;

import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.nodes.Api;

/*
 * FIXME: !!!!
 * A dummy implementation of TemplateChecker until the real class can be checked in.
 */
public class TemplateChecker extends NodeUpdateVisitor {

	public static class Result extends StaticPhaseResult {
            private Api api;
            public Result( Api api ){
                this.api = api;
            }

            public Api api(){ return api; }
	}
            
        public static Result checkTemplates( Api api ){
            return new Result(api);
        }
}
