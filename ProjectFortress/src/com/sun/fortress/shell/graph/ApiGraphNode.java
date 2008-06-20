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

package com.sun.fortress.shell.graph;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.compiler.index.ApiIndex;

import edu.rice.cs.plt.tuple.Option;

public class ApiGraphNode extends GraphNode{
	private APIName name;
	private Option<ApiIndex> api;
	public ApiGraphNode( APIName name ){
		this.name = name;
		this.api = Option.none();
	}

	public boolean equals( Object o ){
		if ( o instanceof ApiGraphNode ){
			ApiGraphNode a = (ApiGraphNode) o;
			return a.getName().equals( name );
		}
		return false;
	}

	public APIName getName(){
		return name;
	}

	public Option<ApiIndex> getApi(){
		return this.api;
	}

	public void setApi(ApiIndex api){
		this.api = Option.wrap(api);
	}

	public String toString(){
		return "Api " + name.toString();
	}
        
        public <T,F extends Throwable> T accept( GraphVisitor<T,F> g ) throws F{
            return g.visit(this);
        }
}
