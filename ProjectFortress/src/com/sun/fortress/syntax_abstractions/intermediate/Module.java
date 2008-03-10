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

package com.sun.fortress.syntax_abstractions.intermediate;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xtc.parser.ModuleDependency;
import xtc.parser.ModuleName;

import com.sun.fortress.compiler.disambiguator.NonterminalEnv;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TerminalDecl;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.syntax_abstractions.phases.Analyzable;

import edu.rice.cs.plt.tuple.Option;

public abstract class Module implements Analyzable<Module> {

	protected QualifiedIdName name;
	private boolean isTopLevel;
	
	private Map<String, Set<SyntaxSymbol>> tokenMap;

	protected Set<ModuleName> parameters;
	protected Set<ModuleDependency> dependencies;
	
	protected Collection<NonterminalIndex<? extends GrammarMemberDecl>> declaredMembers;

	protected Collection<? extends Module> extendedModules; 
	
	public Module() {
		this.tokenMap = new HashMap<String, Set<SyntaxSymbol>>();
		this.declaredMembers = new LinkedList<NonterminalIndex<? extends GrammarMemberDecl>>();
		this.extendedModules = new java.util.HashSet<Module>();
		this.parameters = new LinkedHashSet<ModuleName>();
		this.dependencies = new LinkedHashSet<ModuleDependency>();
	}

	public Module(QualifiedIdName name,Collection<NonterminalIndex<? extends GrammarMemberDecl>> declaredMembers) {
		this();
		this.name = name;
		this.declaredMembers.addAll(declaredMembers);
	}

	public void isTopLevel(boolean b) {
		this.isTopLevel = b;		
	}
	
	public boolean isTopLevel() {
		return this.isTopLevel;
	}

	public QualifiedIdName getName() {
		return this.name;
	}
	
	public void setName(QualifiedIdName name) {
		this.name = name;
	}
	
	public Set<ModuleName> getParameters() {
		return this.parameters;
	}

	public void setParameters(Set<ModuleName> ls) {
		this.parameters = ls;		
	}
	
	public Collection<ModuleDependency> getDependencies() {
		return this.dependencies;
	}

	public void setDependencies(Set<ModuleDependency> ls) {
		this.dependencies = ls;
	}
	
	public Collection<NonterminalIndex<? extends GrammarMemberDecl>> getDeclaredNonterminals() {
		return this.declaredMembers;
	}
	
	public Collection<? extends Module> getExtended() {
		return this.extendedModules;
	}

	public void setExtended(Collection<Module> ls) {
		this.extendedModules = ls;		
	}

	/** 
	 * Returns a set of all token symbols from all declared and inherited members
	 * @return
	 */
	public Set<String> getKeywords() {
		final Set<String> keywords = new HashSet<String>();
		for (NonterminalIndex<? extends GrammarMemberDecl> m: this.declaredMembers) {
			if (m.ast().isSome()) {
				Option.unwrap(m.ast()).accept(new NodeDepthFirstVisitor_void(){
					@Override
					public void forKeywordSymbol(KeywordSymbol that) {
						keywords.add(that.getToken());
					}
				});
				}
			}
		return keywords;
	}
	
	/** 
	 * Returns a set of all token symbols from all declared and inherited members
	 * @return
	 */
	public Set<String> getTokens() {
		final Set<String> tokens = new HashSet<String>();
		for (NonterminalIndex<? extends GrammarMemberDecl> m: this.declaredMembers) {
			if (m.ast().isSome()) {
				Option.unwrap(m.ast()).accept(new NodeDepthFirstVisitor_void(){
					@Override
					public void forTokenSymbol(TokenSymbol that) {
						tokens.add(that.getToken());
					}
				});
				}
			}
		return tokens;
	}

	public String toString() {
		String indentation = "  ";
		String indent = indentation;
		String tmpIndent = "";
		String s = "*** "+this.getName()+" ***\n";
		
		s+= indent+"* Toplevel: "+this.isTopLevel+"\n";
		
		s+= indent+"* Parameters\n";
		tmpIndent = indent;
		indent += indentation;
		Iterator<ModuleName> pit = this.parameters.iterator();
		while (pit.hasNext()) {
			s+= indent+"- "+pit.next().toString()+"\n";
		}
		indent = tmpIndent;
		
		s+= indent+"* Dependencies\n";
		tmpIndent = indent;
		indent += indentation;
		Iterator<ModuleDependency> dit = this.dependencies.iterator();
		while (dit.hasNext()) {
			s+= indent+"- "+dit.next().visibleName().toString()+"\n";
		}
		indent = tmpIndent;
	    
		s+= indent+"* declared nonterminals\n";
		tmpIndent = indent;
		indent += indentation;
		Iterator<NonterminalIndex<? extends GrammarMemberDecl>> nit = this.declaredMembers.iterator();
		while (nit.hasNext()) {
			NonterminalIndex<? extends GrammarMemberDecl> member = nit.next();
			s+= indent+"- "+member.getType()+" "+member.getName()+"\n";
			if (member.getAst() instanceof NonterminalDecl) {
				for (SyntaxDef sd: ((NonterminalDecl) member.getAst()).getSyntaxDefs()) {
					s+= indent+indent+" - "+sd.getSyntaxSymbols()+"\n";
				}
			}
		}
		indent = tmpIndent;
		
		s+= indent+"* extended modules\n";
		indent += indentation;
		Iterator<? extends Module> amit = this.extendedModules.iterator();
		while (amit.hasNext()) {
			s+= indent+"- "+amit.next().getName()+"\n";
		}
		indent = tmpIndent;
		return s;
	}

}
