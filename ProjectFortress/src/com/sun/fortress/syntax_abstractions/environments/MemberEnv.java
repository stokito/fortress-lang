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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.NonterminalParameter;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxDecl;
import com.sun.fortress.nodes.TerminalDecl;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.useful.Pair;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

public class MemberEnv {

    private BaseType astType;
    private Id name;
    private Id[] params;
    
    private Map<SyntaxDecl, SyntaxDeclEnv> syntaxDefToEnv;
    private Map<Id, BaseType> parameterToTypeMap;

	private MemberEnv() {
		this.parameterToTypeMap = new HashMap<Id, BaseType>();
		this.syntaxDefToEnv = new HashMap<SyntaxDecl, SyntaxDeclEnv>();
	}
	
	public MemberEnv(NonterminalIndex<? extends GrammarMemberDecl> member) {
		this();
		this.name = member.getName();
		this.astType = member.getAstType();
		
		if (member.getAst() instanceof NonterminalDecl) {
			NonterminalDecl nd = (NonterminalDecl) member.getAst();
			initEnv(nd.getHeader().getParams(), nd.getSyntaxDefs());
		}
		else if (member.getAst() instanceof TerminalDecl) {
			TerminalDecl nd = (TerminalDecl) member.getAst();
			List<SyntaxDecl> ls = new LinkedList<SyntaxDecl>();
			ls.add(nd.getSyntaxDef());
			initEnv(nd.getHeader().getParams(), ls);
		}
	}
	
	private void initEnv(List<NonterminalParameter> ls, List<SyntaxDecl> syntaxDefs) {
		Id[] params = new Id[ls.size()];
		int inx = 0;
		for (NonterminalParameter p: ls) {
			Id var = p.getName();
			params[inx] = var;
            
			this.addArgsNonterminal(var, p.getType());
			inx++;
		}
		this.setParamArray(params);
		
		for (SyntaxDecl sd: syntaxDefs) {
			SyntaxDeclEnv sdEnv = new SyntaxDeclEnv(sd, this);
			this.add(sd, sdEnv);
		}
	}

	public void add(SyntaxDecl sd, SyntaxDeclEnv sdEnv) {
		this.syntaxDefToEnv.put(sd, sdEnv);		
	}
	
    public void rebuildSyntaxDeclEnvs(List<SyntaxDecl> syntaxDefs) {
        this.syntaxDefToEnv.clear();
        for (SyntaxDecl sd: syntaxDefs) {
            this.add(sd, new SyntaxDeclEnv(sd, this));
        }
    }

	private void addArgsNonterminal(Id var, BaseType t) {
		this.parameterToTypeMap.put(var, t);		
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

	public Option<SyntaxDeclEnv> getSyntaxDeclEnv(SyntaxDecl syntaxDef) {
		SyntaxDeclEnv sdEnv = null;
		if (null != (sdEnv= this.syntaxDefToEnv.get(syntaxDef))) {
		    return Option.some(sdEnv);
		}
		return Option.none();
	}

	public String toString() {
	    String s = "params: ";
	    for (Id id: params) {
	        s += id+":"+this.parameterToTypeMap.get(id)+", ";
	    }
		return this.name+", "+s+this.syntaxDefToEnv;
	}

    public BaseType getAstType() {
        return this.astType;
    }

    /**
     * Returns the nonterminal that the given parameter is mapped to
     * @param id
     * @return
     */
    public BaseType getParameterType(Id id) {
        return this.parameterToTypeMap.get(id);
    }

    public boolean isParameter(Id id) {
        return this.parameterToTypeMap.containsKey(id);
    }

    public List<Id> getParameters() {
        return Arrays.asList(this.params);
    }
    
    /**
     * Add all the bindings of the other environment to this environment
     * Assume the parameters among the two environments are the same
     * @param gntMEnv
     */
    public void merge(MemberEnv other) {
        // TODO: Add subtype check
//        if (!this.astType.equals(other.astType)) {
//            throw new RuntimeException("Incompatible member environments, return types mismatch: "+this.astType+", "+other.astType);
//        }
        if (!Arrays.deepEquals(this.params, other.params)) {
            throw new RuntimeException("Incompatible member environments, parameters mismatch");
        }
        this.parameterToTypeMap.putAll(other.parameterToTypeMap);
        this.syntaxDefToEnv.putAll(other.syntaxDefToEnv);
    }

    public Id getName() {
        return this.name;
    }

}
