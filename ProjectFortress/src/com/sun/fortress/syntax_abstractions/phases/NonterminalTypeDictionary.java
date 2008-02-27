package com.sun.fortress.syntax_abstractions.phases;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.sun.fortress.compiler.index.ProductionIndex;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.IdType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.intermediate.Module;
import com.sun.fortress.syntax_abstractions.util.ActionCreater;
import com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil;

import edu.rice.cs.plt.tuple.Option;

public class NonterminalTypeDictionary {

	private static Collection<ProductionIndex<? extends GrammarMemberDecl>> members = new LinkedList<ProductionIndex<? extends GrammarMemberDecl>>();
	private static Map<String, Type> cache = new HashMap<String, Type>();

	public static void addAll(Collection<Module> modules) {
		for (Module module: modules) {
			NonterminalTypeDictionary.members.addAll(module.getDeclaredNonterminals());
		}
	}

	/**
	 * Given a name of a nonterminal or terminal definition, the type of the
	 * corresponding member is returned.
	 * The algorithm works by traversing the collection of members.
	 * The type of the first member found is returned.
	 * If the name is the empty String we return
	 * FortressLibrary.String return.
	 * This is kind of a hack see {@link ActionCreater#getType} for use.   
	 * If a member is not found with the given name then a none value is returned
	 * @param name
	 * @return
	 */
	public static Option<Type> getType(String name) {
		if (cache.containsKey(name)) {
			return Option.<Type>some(cache.get(name));
		}

		for (ProductionIndex<? extends GrammarMemberDecl> n: members) {
			if (n.getName().getName().getText().equals(name)) {
				cache.put(name, n.getType());
				return Option.<Type>some(n.getType());
			}
		}
		return Option.none();
	}

}
