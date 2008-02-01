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
import java.util.Set;

import com.sun.fortress.compiler.disambiguator.ProductionEnv;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.syntax_abstractions.phases.Analyzable;

import edu.rice.cs.plt.tuple.Option;

public class GrammarIndex implements Analyzable<GrammarIndex> {

	private Option<GrammarDef> ast;
	
	private Collection<ProductionIndex<? extends NonterminalDecl>> productions;

	private Collection<GrammarIndex> extendedGrammars;

	private ProductionEnv env;
	
	public GrammarIndex(Option<GrammarDef> ast, Set<ProductionIndex<? extends NonterminalDecl>> productions) {
		this.ast = ast;
		this.extendedGrammars = new LinkedList<GrammarIndex>();
		this.productions = productions;
		this.env = new ProductionEnv(this);
	}

	public Option<GrammarDef> ast() {
		return this.ast;
	}
	
	public Collection<ProductionIndex<? extends NonterminalDecl>> getDeclaredNonterminals() {
		return this.productions;
	}
	
	public void setAst(GrammarDef g) {
		this.ast = Option.wrap(g);		
	}

	public void setExtended(Collection<GrammarIndex> gs) {
		this.extendedGrammars = gs;
	}
	
	public Collection<GrammarIndex> getExtended() {
		return this.extendedGrammars;
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
