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

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.TopSort;
import com.sun.fortress.useful.TopSortItem;
import com.sun.fortress.useful.TopSortItemImpl;

public class Graph<GNode extends GraphNode>{

	private HashMap<GNode,List<GNode>> edges;
	public Graph(){
		edges = new HashMap<GNode, List<GNode>>();
	}

	public Graph( Graph<GNode> copy, Fn<GNode,Boolean> keep ){
		this();
		for ( GNode node : copy.nodes() ){
			if ( keep.apply(node) ){
				addNode(node);
			}
		}
		for ( GNode node : copy.nodes() ){
			for ( GNode next : copy.successors(node) ){
				if ( contains(next) ){
					addEdge(node, next);
				}
			}
		}
	}

	public void dump(){
		for ( Map.Entry<GNode,List<GNode>> entry : edges.entrySet() ){
			System.out.println( entry.getKey() + " -> " + entry.getValue() );
		}
	}

	/* helper method for dumpSorted */
	private TopSortItemImpl<GNode> findNode( GNode node, List<TopSortItem> items ){
		for ( TopSortItem item : items ){
			TopSortItemImpl<GNode> ix = (TopSortItemImpl<GNode>) item;
			if ( ix.x == node || ix.x.equals( node ) ){
				return ix;
			}
		}
		return null;
	}

	public List<GNode> sorted(){
		List<TopSortItem> unsorted = new ArrayList<TopSortItem>();
		for ( GNode key : edges.keySet() ){
			unsorted.add( new TopSortItemImpl<GNode>( key ) );
		}
		for ( Map.Entry<GNode,List<GNode>> entry : edges.entrySet() ){
			TopSortItemImpl<GNode> from = findNode( entry.getKey(), unsorted );
			for ( GNode to : entry.getValue() ){
				from.edgeTo( findNode( to, unsorted ) );
			}
		}
		List<TopSortItem> sorted = TopSort.breadthFirst( unsorted );
		List<GNode> real = new ArrayList<GNode>();
		for ( TopSortItem t : sorted ){
			real.add( 0, ((TopSortItemImpl<GNode>) t).x );
		}
		return real;
	}

	/* do a topological sort and print it out */
	public void dumpSorted(){
		System.out.println( "Sorted: " + sorted() );
	}

	public void addEdge( GNode node1, GNode node2 ){
		if ( ! contains(node1) ){
			addNode(node1);
		} else {
			node1 = find(node1);
		}
		if ( ! edges.get(node1).contains(node2) ){
			edges.get(node1).add(node2);
		}
	}

	public List<GNode> filter( Fn<GNode,Boolean> func ){
		List<GNode> all = new ArrayList<GNode>();
		for ( GNode key : edges.keySet() ){
			if ( func.apply(key) ){
				all.add(key);
			}
		}
		return all;
	}

	private List<GNode> dependancies( GNode start, List<GNode> seen ){
		if ( contains( start ) ){
			List<GNode> depends = new ArrayList<GNode>();
			for ( GNode node : edges.get( start ) ){
				if ( ! seen.contains( node ) ){
					seen.add( node );
					depends.add( node );
					depends.addAll( dependancies( node, seen ) );
				}
			}
			return depends;
		}

		return new ArrayList<GNode>();
	}

	public List<GNode> dependancies( GNode start ){
		return dependancies( start, new ArrayList<GNode>() );
	}

	public boolean contains( GNode node ){
		return find( node ) != null;
	}

	public void addNode( GNode node ){
		edges.put( node, new ArrayList<GNode>() );
	}

	public List<GNode> successors( GNode node ){
		return edges.get( node );
	}

	public Set<GNode> nodes(){
		return edges.keySet();
	}

	public GNode find( GNode node ){
		for ( GNode key : edges.keySet() ){
			if ( key.equals( node ) ){
				return key;
			}
		}
		return null;
	}
}
