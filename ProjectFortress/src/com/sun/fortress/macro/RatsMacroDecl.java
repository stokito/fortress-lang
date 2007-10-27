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

/*
 * Class containing a collection of Rats! AST nodes which corresponds to a 
 * given macro declaration.
 * 
 */


package com.sun.fortress.macro;

import java.util.List;

import xtc.parser.Sequence;

public class RatsMacroDecl {

	private ModuleEnum module;
	private ProductionEnum production;
	private List<Sequence> sequence;

	public RatsMacroDecl(ModuleEnum module, ProductionEnum production,
			List<Sequence> sequence) {
		this.module = module;
		this.production = production;
		this.sequence = sequence;
	}

	public ModuleEnum getModule() {
		return this.module;
	}

	public ProductionEnum getProduction() {
		return this.production;
	}

	public List<Sequence> getSequence() {
		return this.sequence;
	}

}
