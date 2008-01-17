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

package com.sun.fortress.compiler.index;

import java.util.List;

import com.sun.fortress.nodes.ProductionDef;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.TraitType;

import edu.rice.cs.plt.tuple.Option;

public class ProductionIndex {

	private Option<ProductionDef> ast;
	
	public ProductionIndex(Option<ProductionDef> ast) {
		this.ast = ast;
	}

	public Option<ProductionDef> ast() {
		return this.ast;
	}

	public String getName() {
		if (this.ast().isSome()) {
			return Option.unwrap(this.ast()).getName().toString();
		}
		throw new RuntimeException("Production index without ast and thus no name");
	}

	public Option<QualifiedIdName> getExtends() {
		if (this.ast().isSome()) {
			return Option.unwrap(this.ast()).getExtends();
		}
		return Option.none();
	}

	public List<SyntaxDef> getSyntaxDefs() {
		if (this.ast().isSome()) {
			return Option.unwrap(this.ast()).getSyntaxDefs();
		}
		throw new RuntimeException("Production index without ast and thus no syntax definitions");
	}

	public TraitType getType() {
		if (this.ast().isSome()) {
			return Option.unwrap(this.ast()).getType();
		}
		throw new RuntimeException("Production index without ast and thus no type");
	}
}
