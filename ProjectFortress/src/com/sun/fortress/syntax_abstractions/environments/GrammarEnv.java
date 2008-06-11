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
import java.util.Set;
import java.util.Map.Entry;

import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.syntax_abstractions.phases.GrammarAnalyzer;
import com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil;

/**
 * Contains a map from fully qualified nonterminal or terminal
 * names to their corresponding environment. 
 */
public class GrammarEnv {

	private static Map<Id, MemberEnv> members = new HashMap<Id, MemberEnv>();

	/**
	 * Add name of terminal or nonterminal and its corresponding member environment
	 * @param id
	 * @param memberEnv
	 */
    public static void add(Id id, MemberEnv memberEnv) {
        GrammarEnv.members.put(id, memberEnv);
    }
	
    /**
     * Returns true if the given name is contained in this environment
     * @param name
     * @return
     */
	public static boolean contains(Id name) {
		return GrammarEnv.members.containsKey(name);
	}

	/**
	 * Returns the member environment associated with the given name 
	 * @param name
	 * @return
	 */
	public static MemberEnv getMemberEnv(Id name) {
		return GrammarEnv.members.get(name);
	}

	/**
	 * Dump the content of this environment as a string
	 * @return
	 */
	public static String getDump() {
		String s = "GrammarEnv: \n";
		for (Id id: GrammarEnv.members.keySet()) {
		    s += " - "+id.toString()+"\n";
		    s += " - "+GrammarEnv.members.get(id).toString()+"\n";
		}
		return s;
	}

	/**
	 * Empty this environment
	 */
    public static void clear() {
        GrammarEnv.members = new HashMap<Id, MemberEnv>();
    }
    
    public static void add(GrammarIndex g) {
        GrammarAnalyzer<GrammarIndex> ga = new GrammarAnalyzer<GrammarIndex>();
        Collection<NonterminalIndex<? extends GrammarMemberDecl>> ls = ga.getContainedSet(g);
        for (NonterminalIndex/*<? extends GrammarMemberDecl> Commented out due to a bug in Sun Java 1.5.0_04 on Fedora Linux */ nt: ls) {
            APIName currentApi = g.getName().getApi().unwrap();
            String currentName = g.getName().getText();
            Id name = SyntaxAbstractionUtil.qualifyMemberName(currentApi, currentName, nt.getName().getText());
            GrammarEnv.add(nt.getName(), new MemberEnv(nt));
        }
    }

    public static boolean isEmpty() {
        return GrammarEnv.members.isEmpty();
    }
}
