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

package com.sun.fortress.compiler;

import java.util.List;
import java.util.Set;

import com.sun.fortress.compiler.disambiguator.NameEnv;
import com.sun.fortress.compiler.disambiguator.ProductionEnv;
import com.sun.fortress.nodes.DottedName;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.ProductionDef;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

public class ProductionDisambiguator extends NodeUpdateVisitor {

	private NameEnv _env;
	private List<StaticError> _errors;
	private ProductionEnv _currentEnv;

	public ProductionDisambiguator(NameEnv env, List<StaticError> newErrs) {
		this._env = env;
		this._errors = newErrs;
	}
	
	private void error(String msg, HasAt loc) {
		this._errors.add(StaticError.make(msg, loc));
	}
	
	@Override
	public Node forGrammarDefOnly(GrammarDef that, QualifiedIdName name_result, List<QualifiedIdName> extends_result, List<ProductionDef> productions_result) {
		return new GrammarDef(name_result, extends_result, productions_result);
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
	public Node forProductionDef(ProductionDef that) {
		Option<QualifiedIdName> extended = Option.none();
		if (that.getExtends().isSome()) {
			extended = Option.wrap(handleProductionName(Option.unwrap(that.getExtends())));
		}
		QualifiedIdName name = handleProductionName(that.getName());
		return new ProductionDef(that.getModifier(),name,that.getType(), extended, that.getSyntaxDefs());
	}

	private QualifiedIdName handleProductionName(QualifiedIdName name) {
		if (name.getApi().isSome()) {
			DottedName originalApiGrammar = Option.unwrap(name.getApi());
			Option<DottedName> realApiGrammarOpt = _currentEnv.grammarName(originalApiGrammar);
			if (realApiGrammarOpt.isNone()) {
				error("Undefined grammar: " + NodeUtil.nameString(originalApiGrammar), originalApiGrammar);
				return name;
			}
			DottedName realApiGrammar = Option.unwrap(realApiGrammarOpt);
			QualifiedIdName newN;
			if (originalApiGrammar == realApiGrammar) { newN = name; }
			else { newN = NodeFactory.makeQualifiedIdName(realApiGrammar, name.getName()); }

			if (!_currentEnv.hasQualifiedProduction(newN)) {
				error("Undefined production: " + NodeUtil.nameString(newN), newN);
				return name;
			}
			return newN;
		}
		else {
			if (_currentEnv.hasProduction(name)) { 
				Set<QualifiedIdName> productions = _currentEnv.explicitProductionNames(name);
				if (productions.size() > 1) {
					error("Production name may refer to: " + NodeUtil.namesString(productions), name);
					return name;
				}
				QualifiedIdName qname = IterUtil.first(productions);
				return qname;
			}
			else {
				Set<QualifiedIdName> productions = _currentEnv.explicitProductionNames(name);
				if (productions.isEmpty()) {
					productions = _currentEnv.inheritedProductionNames(name);
				}

				if (productions.isEmpty()) {
					error("Undefined production: " + NodeUtil.nameString(name), name);
					return name;
				}
				if (productions.size() > 1) {
					error("Production name may refer to: " + NodeUtil.namesString(productions), name);
					return name;
				}
				QualifiedIdName qname = IterUtil.first(productions);
				return qname;
			}
		}
	}	


}
