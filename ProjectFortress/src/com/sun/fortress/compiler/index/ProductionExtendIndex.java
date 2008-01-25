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

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import edu.rice.cs.plt.tuple.Option;

public class ProductionExtendIndex extends ProductionIndex<NonterminalExtensionDef> {

	private Option<ProductionIndex<? extends NonterminalDecl>> extend;
	
	public ProductionExtendIndex(Option<NonterminalExtensionDef> ast) {
		super(ast);
	}

	public ProductionIndex<? extends NonterminalDecl> getExtends() {
		if (this.extend.isNone()) {
			throw new RuntimeException("Nonterminal "+this.getName()+" is expected to extend another nonterminal but didn't.");
		}
		return Option.unwrap(this.extend);
	}

	public void setExtends(ProductionIndex<? extends NonterminalDecl> ext) {
		this.extend = Option.<ProductionIndex<? extends NonterminalDecl>>some(ext);
	}
}
