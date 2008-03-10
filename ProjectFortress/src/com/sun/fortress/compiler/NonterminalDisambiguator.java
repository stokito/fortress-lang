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

package com.sun.fortress.compiler;

import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.compiler.disambiguator.NameEnv;
import com.sun.fortress.compiler.disambiguator.NonterminalNameDisambiguator;
import com.sun.fortress.compiler.disambiguator.NonterminalEnv;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;

import edu.rice.cs.plt.tuple.Option;

public class NonterminalDisambiguator extends NodeUpdateVisitor {

	private NameEnv _env;
	private List<StaticError> _errors;
	private NonterminalEnv _currentEnv;
	private GlobalEnvironment _globalEnv;

	public NonterminalDisambiguator(NameEnv env, GlobalEnvironment globalEnv, List<StaticError> newErrs) {
		this._env = env;
		this._errors = newErrs;
		this._globalEnv = globalEnv;
	}

	private void error(String msg, HasAt loc) {
		this._errors.add(StaticError.make(msg, loc));
	}

	@Override
	public Node forGrammarDef(GrammarDef that) {
		if (this._env.grammarIndex(that.getName()).isSome()) {
			this._currentEnv = Option.unwrap(_env.grammarIndex(that.getName())).env();
		}
		else {
			error("Undefined grammar: " + NodeUtil.nameString(that.getName()), that.getName());
		}
		return super.forGrammarDef(that);
	}

	@Override
	public Node forGrammarDefOnly(GrammarDef that, QualifiedIdName name_result, List<QualifiedIdName> extends_result, List<GrammarMemberDecl> nonterminal_result) {
		return new GrammarDef(name_result, extends_result, nonterminal_result);
	}

	@Override
	public Node forNonterminalDefOnly(NonterminalDef that, QualifiedIdName name_result, Option<TraitType> type_result, Option<? extends Modifier> modifier_result, List<SyntaxDef> syntaxDefs_result) {
		if (type_result.isNone()) {
			throw new RuntimeException("Type inference is not supported yet!");
		}
		NonterminalNameDisambiguator nnd = new NonterminalNameDisambiguator(this._globalEnv);
		Option<QualifiedIdName> oname = nnd.handleNonterminalName(_currentEnv, that.getName());
		this._errors.addAll(nnd.errors());
		if (oname.isSome()) {
			QualifiedIdName name = Option.unwrap(oname);
			return new NonterminalDef(name, that.getType(), that.getModifier(), syntaxDefs_result);
		}
		return new NonterminalDef(that.getName(), that.getType(), that.getModifier(), syntaxDefs_result);
	}
	
	
	@Override
	public Node forNonterminalExtensionDefOnly(NonterminalExtensionDef that,
			QualifiedIdName name_result, Option<TraitType> type_result,
			Option<? extends Modifier> modifier_result,
			List<SyntaxDef> syntaxDefs_result) {
		NonterminalNameDisambiguator pnd = new NonterminalNameDisambiguator(this._globalEnv);
		Option<QualifiedIdName> oname = pnd.handleNonterminalName(_currentEnv, that.getName());
		this._errors.addAll(pnd.errors());
		if (oname.isSome()) {
			QualifiedIdName name = Option.unwrap(oname);
			return new NonterminalExtensionDef(name, that.getType(), that.getModifier(), syntaxDefs_result);
		}
		return new NonterminalExtensionDef(that.getName(), that.getType(), that.getModifier(), syntaxDefs_result);
	}
}
