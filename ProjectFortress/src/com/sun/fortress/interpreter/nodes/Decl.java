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

package com.sun.fortress.interpreter.nodes;

public interface Decl extends DefOrDecl {

    // public String stringName();

    // public String at(); // See Node.at()

    // / and def =
    // / [
    // / | `TraitDecl of trait_def
    // / | `FnDecl of fn_def
    // / | `ObjectDecl of object_def
    // / | `VarDecl of var_def
    // / | `DefOrDecl of def_or_decl
    // / ] node
    // /

    abstract public <T> T acceptInner(NodeVisitor<T> v);

}
