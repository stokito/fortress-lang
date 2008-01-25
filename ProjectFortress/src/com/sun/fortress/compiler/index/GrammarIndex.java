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

package com.sun.fortress.compiler.index;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import com.sun.fortress.compiler.disambiguator.ProductionEnv;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.QualifiedIdName;

import edu.rice.cs.plt.tuple.Option;

public class GrammarIndex {

	private Option<GrammarDef> ast;
	
	private Map<QualifiedIdName, ProductionIndex<? extends NonterminalDecl>> productions;

	private Collection<GrammarIndex> gs;

	private ProductionEnv env;
	
	public GrammarIndex(Option<GrammarDef> ast, Map<QualifiedIdName, ProductionIndex<? extends NonterminalDecl>> productions) {
		this.ast = ast;
		this.productions = productions;
		this.gs = new LinkedList<GrammarIndex>();
	}

	public Option<GrammarDef> ast() {
		return this.ast;
	}
	
	public Map<QualifiedIdName, ProductionIndex<? extends NonterminalDecl>> productions() {
		return this.productions;
	}

	public void setAst(GrammarDef g) {
		this.ast = Option.wrap(g);		
	}

	public void setExtendedGrammars(Collection<GrammarIndex> gs) {
		this.gs = gs;
	}
	
	public Collection<GrammarIndex> getExtendedGrammars() {
		return this.gs;
	}

	public void setEnv(ProductionEnv env) {
		this.env = env;
	}

	public ProductionEnv env() {
		return this.env;
	}

	public QualifiedIdName getName() {
		if (this.ast().isSome()) {
			return Option.unwrap(this.ast()).getName();
		}
		throw new RuntimeException("No name for grammar: "+this.hashCode());
	}

}
