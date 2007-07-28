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
 * Fortress arrow type tail part
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser;

import java.util.List;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.NonArrowType;

public class ArrowTypeTail {

    NonArrowType range;
    List<TraitType> throwsClause;

    ArrowTypeTail(NonArrowType in_range, List<TraitType> in_throws) {
        this.range = in_range;
        this.throwsClause = in_throws;
    }

    public NonArrowType getRange() { return range; }
    public List<TraitType> getThrows() { return throwsClause; }
}
