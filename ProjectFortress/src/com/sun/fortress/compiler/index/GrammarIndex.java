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

import com.sun.fortress.compiler.disambiguator.NonterminalEnv;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.syntax_abstractions.phases.Analyzable;

import edu.rice.cs.plt.tuple.Option;

public class GrammarIndex implements Analyzable<GrammarIndex> {

	private Option<GrammarDef> ast;
	
	private Collection<NonterminalIndex<? extends GrammarMemberDecl>> members;

	private Collection<GrammarIndex> extendedGrammars;

	private NonterminalEnv env;
	
	public GrammarIndex(Option<GrammarDef> ast, Set<NonterminalIndex<? extends GrammarMemberDecl>> members) {
		this.ast = ast;
		this.extendedGrammars = new LinkedList<GrammarIndex>();
		this.members = members;
		this.env = new NonterminalEnv(this);
	}

	public Option<GrammarDef> ast() {
		return this.ast;
	}
	
	public Collection<NonterminalIndex<? extends GrammarMemberDecl>> getDeclaredNonterminals() {
//		Collection<ProductionIndex<? extends NonterminalDecl>> nonterminals = new LinkedList<ProductionIndex<? extends NonterminalDecl>>();
//		for (ProductionIndex<? extends GrammarMemberDecl> g: this.members) {
//			if ((g instanceof ProductionDefIndex) || 
//			    (g instanceof ProductionExtendIndex)) {
//				nonterminals.add((ProductionIndex) g); // Raw type used because of bug in javac Java 1.5 on Solaris 
//			}
//		}
		return this.members;
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

	public void setEnv(NonterminalEnv env) {
		this.env = env;
	}

	public NonterminalEnv env() {
		return this.env;
	}

	public QualifiedIdName getName() {
		if (this.ast().isSome()) {
			return Option.unwrap(this.ast()).getName();
		}
		throw new RuntimeException("No name for grammar: "+this.hashCode());
	}

	public Option<GrammarNonterminalIndex<? extends NonterminalDecl>> getNonterminalDecl(Id name) {
		for (NonterminalIndex<? extends GrammarMemberDecl> m: this.getDeclaredNonterminals()) {
			if (name.getText().equals(m.getName().getName().getText())) {
				if (m.ast().isSome()) {
					if (m instanceof GrammarNonterminalIndex) {
						return Option.<GrammarNonterminalIndex<? extends NonterminalDecl>>some((GrammarNonterminalIndex) m);
					}
				}
			}
		}
		return Option.none();
	}

}
