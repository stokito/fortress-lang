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

import java.util.Map;
import java.util.Set;
import edu.rice.cs.plt.collect.Relation;
import com.sun.fortress.nodes.CompilationUnit;

import com.sun.fortress.useful.NI;

/** Comprises {@link ApiIndex} and {@link CompilationUnit}. */
public class CompilationUnitIndex {
  
  private final CompilationUnit _ast;
  private final Map<String, Variable> _variables;
  private final Relation<String, Function> _functions;
  private final Map<String, TraitIndex> _traits;
  
  public CompilationUnitIndex(CompilationUnit ast, Map<String, Variable> variables,
                              Relation<String, Function> functions,
                              Map<String, TraitIndex> traits) {
    _ast = ast;
    _variables = variables;
    _functions = functions;
    _traits = traits;
  }
  
  public Set<String> exports() {
    return NI.nyi();
  }

  public Set<String> imports() {
    return NI.nyi();
  }
  
  public Map<String, Variable> variables() {
    return NI.nyi();
  }
  
  public Relation<String, Function> functions() {
    return NI.nyi();
  }

  public Map<String, TraitIndex> traits() {
    return NI.nyi();
  }
  
}
