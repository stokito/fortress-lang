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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.QualifiedIdName;

public class GrammarEnv {

	Map<Id, MemberEnv> members;
	
	public GrammarEnv() {
		this.members = new HashMap<Id, MemberEnv>();
	}
	
	public GrammarEnv(Collection<NonterminalIndex<? extends GrammarMemberDecl>> members) {
		for (NonterminalIndex<? extends GrammarMemberDecl> member: members) {
			MemberEnv menv = new MemberEnv(member);
			this.members.put(member.getName(), menv);
		}
	}

	public boolean contains(Id name) {
		return this.members.containsKey(name);
	}
	
	public MemberEnv getMemberEnv(Id name) {
		return this.members.get(name);
	}

}
