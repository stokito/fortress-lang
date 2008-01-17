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
import java.util.Set;

import com.sun.fortress.compiler.disambiguator.NameEnv;
import com.sun.fortress.compiler.disambiguator.ProductionEnv;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.ProductionDef;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.syntax_abstractions.phases.ItemDisambiguator;
import com.sun.fortress.useful.HasAt;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

// TODO: add whitespace in between symbols.
public class ProductionDisambiguator extends NodeUpdateVisitor {

	private NameEnv _env;
	private List<StaticError> _errors;
	private ProductionEnv _currentEnv;

	public ProductionDisambiguator() {
		_errors = new LinkedList<StaticError>();
	}
	
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
		return super.forProductionDef(that);
	}
	
	@Override
	public Node forProductionDefOnly(ProductionDef that,
									 Option<? extends Modifier> modifier_result,
									 QualifiedIdName name_result, TraitType type_result,
									 Option<QualifiedIdName> extends_result,
									 List<SyntaxDef> syntaxDefs_result) {
		ProductionNameDisambiguator pnd = new ProductionNameDisambiguator();
		Option<QualifiedIdName> extended = Option.none();
		if (that.getExtends().isSome()) {
			extended = Option.wrap(pnd.handleProductionName(_currentEnv, Option.unwrap(that.getExtends())));
		}
		QualifiedIdName name = pnd.handleProductionName(_currentEnv, that.getName());
		
		return new ProductionDef(that.getModifier(),name,that.getType(), extended, syntaxDefs_result);
	}

	@Override
	public Node forSyntaxDef(SyntaxDef that) {
		List<SyntaxSymbol> ls = new LinkedList<SyntaxSymbol>();
		for (SyntaxSymbol symbol: that.getSyntaxSymbols()) {
			ItemDisambiguator id = new ItemDisambiguator(_currentEnv);
			SyntaxSymbol n = (SyntaxSymbol) symbol.accept(id);
			ls.add(n);
		}
		return new SyntaxDef(that.getSpan(),ls, that.getTransformationExpression());
	}

	public class ProductionNameDisambiguator {
		
		public QualifiedIdName handleProductionName(ProductionEnv currentEnv, QualifiedIdName name) {
			// If it is already fully qualified
			if (name.getApi().isSome()) {
				APIName originalApiGrammar = Option.unwrap(name.getApi());
				Option<APIName> realApiGrammarOpt = currentEnv.grammarName(originalApiGrammar);
				// Check that the qualifying part is a real grammar 
				if (realApiGrammarOpt.isNone()) {
					error("Undefined grammar: " + NodeUtil.nameString(originalApiGrammar) +" obtained from "+name, originalApiGrammar);
					return name;
				}
				APIName realApiGrammar = Option.unwrap(realApiGrammarOpt);
				QualifiedIdName newN;
				if (originalApiGrammar == realApiGrammar) { newN = name; }
				else { newN = NodeFactory.makeQualifiedIdName(realApiGrammar, name.getName()); }

				if (!currentEnv.hasQualifiedProduction(newN)) {
					error("Undefined production: " + NodeUtil.nameString(newN), newN);
					return name;
				}
				return newN;
			}
			else { // Unqualified name
				// Is it defined in the current grammar?
				if (currentEnv.hasProduction(name)) {
					Set<QualifiedIdName> productions = currentEnv.declaredProductionNames(name);
					if (productions.size() > 1) {
						error("Production name may refer to: " + NodeUtil.namesString(productions), name);
						return name;
					}
					if (productions.isEmpty()) {
						error("Internal error know the production is there but can't see it: " + name, name);
						return name;
					}
					QualifiedIdName qname = IterUtil.first(productions);
					return qname;
				}
				else {
					Set<QualifiedIdName> productions = currentEnv.declaredProductionNames(name);
					// If the production is not defined in the current grammar then look
					// among the inherited production names
					if (productions.isEmpty()) {
						productions = currentEnv.inheritedProductionNames(name);
					}

					// if not there it is undefined
					if (productions.isEmpty()) {
						error("Undefined production: " + NodeUtil.nameString(name), name);
						return name;
					}
					
					// If too many are found we are not sure which one is the right...
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
}
