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

package com.sun.fortress.syntax_abstractions.environments;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.TerminalDecl;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.useful.Pair;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

public class MemberEnv {

    private Type returnType;
    private Id name;
    private Map<SyntaxDef, SyntaxDeclEnv> syntaxDefToEnv;
    
    
    private Map<Id, Type> varToTypeMap;
	private Id[] params;

	private MemberEnv() {
		this.varToTypeMap = new HashMap<Id, Type>();
		this.syntaxDefToEnv = new HashMap<SyntaxDef, SyntaxDeclEnv>();
	}
	
	public MemberEnv(NonterminalIndex<? extends GrammarMemberDecl> member) {
		this();
		this.name = member.getName();
		this.returnType = member.getType();
		
		if (member.getAst() instanceof NonterminalDecl) {
			NonterminalDecl nd = (NonterminalDecl) member.getAst();
			initEnv(nd.getHeader().getParams(), nd.getSyntaxDefs());
		}
		else if (member.getAst() instanceof TerminalDecl) {
			TerminalDecl nd = (TerminalDecl) member.getAst();
			List<SyntaxDef> ls = new LinkedList<SyntaxDef>();
			ls.add(nd.getSyntaxDef());
			initEnv(nd.getHeader().getParams(), ls);
		}
	}
	
	private void initEnv(List<Pair<Id,Type>> ls, List<SyntaxDef> syntaxDefs) {
		Id[] params = new Id[ls.size()];
		int inx = 0;
		for (Pair<Id, Type> p: ls) {
			Id var = p.getA();
			params[inx] = var; 
			this.addVarType(var, p.getB());
		}
		this.setParamArray(params);
		
		for (SyntaxDef sd: syntaxDefs) {
			SyntaxDeclEnv sdEnv = new SyntaxDeclEnv(sd);
			this.add(sd, sdEnv);
		}
	}

	public void add(SyntaxDef sd, SyntaxDeclEnv sdEnv) {
		this.syntaxDefToEnv.put(sd, sdEnv);		
	}

	private void addVarType(Id var, Type t) {
		this.varToTypeMap.put(var, t);		
	}
	
	private void setParamArray(Id[] params) {
		this.params = params;
	}

	/**
	 * Returns the n'th parameter counting from 0
	 * @param inx
	 * @return
	 */
	public Id getParameter(int inx) {
		if (inx < this.params.length) {
			return this.params[inx];
		}
		throw new IllegalArgumentException("Argument out of range: "+inx);
	}

	public Option<SyntaxDeclEnv> getSyntaxDeclEnv(SyntaxDef syntaxDef) {
		SyntaxDeclEnv sdEnv = null;
		if (null != (sdEnv= this.syntaxDefToEnv.get(syntaxDef))) {
			return Option.some(sdEnv);
		}
		return Option.none();
	}

	public String toString() {
		return this.name+", type: " + this.getType() + " var types: "+this.varToTypeMap+", Params: "+Arrays.toString(params)+", "+this.syntaxDefToEnv;
	}

    public Type getType() {
        return this.returnType;
    }
}
