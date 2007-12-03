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

package com.sun.fortress.syntaxabstractions.util;

import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.QualifiedName;

import edu.rice.cs.plt.tuple.Option;

/**
 * Representation of syntax parameters
 * A syntax parameter is either a keyword or an Id colon Type. Where 
 * production names are used as types. 
 * 
 */
public class SyntaxParam {

	private IdName identifier;
	private Option<QualifiedName> type;
	
	public SyntaxParam(IdName identifier, Option<QualifiedName> type) {
		super();
		this.identifier = identifier;
		this.type = type;
	}

	public IdName getIdName() {
		return identifier;
	}

	public String getIdentifier() {
		return identifier.stringName();
	}

	public void setIdName(IdName identifier) {
		this.identifier = identifier;
	}

	public QualifiedName getType() {
		return Option.unwrap(type);
	}

	public void setType(QualifiedName type) {
		this.type = Option.wrap(type);
	}
	
	public boolean isKeyword() {
		return this.type.isNone() || Option.unwrap(this.type).getName().stringName().equals("Keyword");
	}
}
