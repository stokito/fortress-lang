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

/*
 * Enumeration of all Rats! modules which can be meaningfully extended 
 * in the Fortress grammar.
 * 
 */

package com.sun.fortress.syntax_abstractions.rats.util;

public enum ModuleEnum {
  ABSFIELD, COMPILATION, DECLARATION, 
  DELIMITEDEXPR, EXPRESSION, FIELD, FORTRESS,  
  FUNCTION, IDENTIFIER, KEYWORD, LITERAL, 
  LOCALDECL, MAYNEWLINEHEADER, METHOD, METHODPARAM,
  NONEWLINEEXPR, NONEWLINEHEADER, NONEWLINETYPE,  
  NOSPACEEXPR, OTHERDECL, PARAMETER, SPACING, 
  SYMBOL, SYNTAX, TRAITOBJECT, TYPE, UNICODE,  
  VARIABLE   
}

