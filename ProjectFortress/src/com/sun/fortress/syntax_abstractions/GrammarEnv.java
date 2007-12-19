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
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.QualifiedName;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.compiler.index.GrammarIndex;

public class GrammarEnv {

	public GrammarEnv() {
		// TODO Auto-generated constructor stub
	}

	public GrammarEnv(Collection<GrammarIndex> gs, boolean isTopLevel) {
		// TODO Auto-generated constructor stub
	}

	public void addGrammar(GrammarIndex grammar, boolean isTopLevel) {
		// TODO Auto-generated method stub

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
