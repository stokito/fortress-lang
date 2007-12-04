/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.syntax_abstractions;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.QualifiedName;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;

public class GrammarEnvironment {

	@SuppressWarnings("serial")
	public class Error extends StaticError {

		private String description;
		private Span position;

		public Error(String description, Span position) {
			super();
			this.description = description;
			this.position = position;
		}

		@Override
		public String at() {
			return position.toString();
		}

		@Override
		public String description() {
			return description;
		}

	}

	private Map<String, GrammarIndex> grammars;
	private List<StaticError> errors;
       
    public GrammarEnvironment() {
    	this.grammars = new HashMap<String, GrammarIndex>();
    	this.errors = new LinkedList<StaticError>();
	}

    public GrammarEnvironment(Map<GrammarDef, Boolean> grammars) {
		this();
    	for (GrammarDef grammar: grammars.keySet()) {
			this.addGrammar(grammar, grammars.get(grammar));
		}
	}

	public Map<String, GrammarIndex> grammars() { return this.grammars; }
    
    public boolean definesGrammar(IdName name) { return this.grammars.containsKey(name.getId().getText()); }

	public GrammarIndex grammar(IdName name) {
        GrammarIndex result = this.grammars.get(name);
        if (result == null) {
            throw new IllegalArgumentException("Undefined Grammar: " +
                                               NodeUtil.nameString(name));
        }
        else { return result; }
    }
    
    public void print() {
        for (String name : this.grammars().keySet()) {
            System.out.println(name);
        }
    }
    
    public void addGrammar(GrammarDef grammar, Boolean isTopLevel) {
    	GrammarIndex grammarIndex = this.grammars.get(grammar.getName().getId().getText());
		if (grammarIndex == null) {
			grammarIndex = new GrammarIndex(grammar, isTopLevel);
			this.grammars.put(grammar.getName().getId().getText(), grammarIndex);
		}
		else if (!grammarIndex.isInitialized()) {
			grammarIndex.setGrammar(grammar, isTopLevel);
		}
		else {
			this.errors.add(new Error("Duplicate grammar imported", grammar.getSpan()));			
		}
		
		for (QualifiedName name: grammar.getExtends()) {
			List<QualifiedName> seen = new LinkedList<QualifiedName>();
			if (seen.contains(name) || name.getName().equals(grammar.getName().getId().getText())) {
				this.errors.add(new Error("A grammar can not extend it self", name.getSpan()));
			}
			else {
				if (!this.grammars.containsKey(name.getName().toString())) {
					this.grammars.put(name.getName().toString(), new GrammarIndex());
				}
				grammarIndex.addExtendingGrammar(this.grammars.get(name.getName().toString()));				
			}			
		}
    }
    
    public List<StaticError> errors() {
    	return this.errors;
    }

	public Collection<GrammarIndex> getGrammars() {
		return this.grammars.values();
	}

}
