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

package com.sun.fortress.syntax_abstractions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.sun.fortress.compiler.index.GrammarIndex;

public class GrammarEnv {

	private Map<GrammarIndex, Boolean> grammars;

	public GrammarEnv() {
		this.grammars = new HashMap<GrammarIndex, Boolean>();
	}

	public GrammarEnv(Collection<GrammarIndex> gs, boolean isTopLevel) {
		this();
		for (GrammarIndex g: gs) {
			this.addGrammar(g, isTopLevel);
		}
	}

	public void addGrammar(GrammarIndex grammar, boolean isTopLevel) {
		this.grammars.put(grammar, isTopLevel);
	}

	public Collection<GrammarIndex> getGrammars() {
		return this.grammars.keySet();
	}

	public boolean isToplevel(GrammarIndex g) {
		if (this.grammars.containsKey(g)) {
			return this.grammars.get(g);
		}
		return false;
	}
	
//	@SuppressWarnings("serial")
//	public class Error extends StaticError {
//
//		private String description;
//		private Span position;
//
//		public Error(String description, Span position) {
//			super();
//			this.description = description;
//			this.position = position;
//		}
//
//		@Override
//		public String at() {
//			return position.toString();
//		}
//
//		@Override
//		public String description() {
//			return description;
//		}
//
//	}
//
//	private Map<String, GrammarIndex> grammars;
//	private List<StaticError> errors;
//
//    public GrammarEnv() {
//    	this.grammars = new HashMap<String, GrammarIndex>();
//    	this.errors = new LinkedList<StaticError>();
//	}
//
//    public GrammarEnv(Collection<GrammarIndex> grammars) {
//		this();
//    	for (GrammarIndex grammar: grammars) {
//			this.addGrammar(grammar);
//		}
//	}
//
//    public void addGrammar(GrammarIndex grammar) {
//    	GrammarIndex grammarIndex = this.grammars.get(grammar.getQualifiedName());
//System.err.println(grammarIndex+" "+grammar.getQualifiedName()+" "+this.grammars);
//    	if (grammarIndex == null) {
//			this.grammars.put(grammar.getQualifiedName(), grammar);
//		}
////		else if (!grammarIndex.isInitialized()) {
////			grammarIndex.setGrammar(grammar, isTopLevel);
////		}
//		else {
//			this.errors.add(new Error("Duplicate grammar imported", grammarIndex.getSpan()));
//		}
//
//		for (QualifiedName name: grammar.ast().getExtends()) {
//			if (!this.grammars.containsKey(name.getName().toString())) {
//				this.grammars.put(name.getName().toString(), new GrammarIndex());
//			}
//			grammarIndex.addExtendingGrammar(this.grammars.get(name.getName().toString()));
//		}
//    }
//
//	public GrammarIndex getGrammarIndex(Id name) {
//        GrammarIndex result = this.grammars.get(name);
//        if (result == null) {
//            throw new IllegalArgumentException("Undefined Grammar: " +
//                                               NodeUtil.nameString(name));
//        }
//        else { return result; }
//    }
//
//	public Map<String, GrammarIndex> grammars() {
//		return this.grammars;
//	}
//
//    public boolean definesGrammar(Id name) {
//    	return this.grammars.containsKey(name.getId().getText());
//    }
//
//    public List<StaticError> errors() {
//    	return this.errors;
//    }
//
//	public Collection<GrammarIndex> getGrammars() {
//		return this.grammars.values();
//	}
//
//    public void print() {
//        for (String name : this.grammars().keySet()) {
//            System.out.println(name);
//        }
//    }

}
