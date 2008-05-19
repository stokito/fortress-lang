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
import com.sun.fortress.nodes.Component;
import com.sun.fortress.compiler.index.ComponentIndex;

import edu.rice.cs.plt.tuple.Option;

public class ComponentGraphNode extends GraphNode{

	private APIName name;
	private Option<ComponentIndex> component;

	public ComponentGraphNode( APIName name ){
		this.name = name;
		this.component = Option.none();
	}

	public boolean equals( Object o ){
		if ( o instanceof ComponentGraphNode ){
			ComponentGraphNode a = (ComponentGraphNode) o;
			return a.getName().equals( name );
		}
		return false;
	}

	public Option<ComponentIndex> getComponent(){
		return component;
	}

	public void setComponent( ComponentIndex c ){
		this.component = Option.wrap(c);
	}

	public APIName getName(){
		return name;
	}

	public String toString(){
		return "Component " + name.toString();
	}
}
