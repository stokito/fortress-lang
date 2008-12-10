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

package com.sun.fortress.repository.graph;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.compiler.index.ApiIndex;

import edu.rice.cs.plt.tuple.Option;

public class ApiGraphNode extends GraphNode{
	private Option<ApiIndex> api;
	private final String k;
	
	public ApiGraphNode( APIName name, long sourceDate ){
	    super(name, sourceDate);
	    k = key(name);
	    this.api = Option.none();
	}

	public boolean equals( Object o ){
		if ( o instanceof ApiGraphNode ){
			ApiGraphNode a = (ApiGraphNode) o;
			return a.getName().equals( getName() );
		}
		return false;
	}

	public Option<ApiIndex> getApi(){
		return this.api;
	}

	public void setApi(ApiIndex api){
		this.api = Option.wrap(api);
	}

	public String toString(){
		return "Api " + getName().toString();
	}
        
        public <T,F extends Throwable> T accept( GraphVisitor<T,F> g ) throws F{
            return g.visit(this);
        }
        
        public String key() {
            return k;
        }
        
        public static String key(APIName k) {
            return "api " + k.getText();
        }
}
