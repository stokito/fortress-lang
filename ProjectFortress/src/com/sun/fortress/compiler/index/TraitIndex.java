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
import com.sun.fortress.nodes.TraitObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TypeRef;

import com.sun.fortress.useful.NI;

/**
 * Wraps a trait or object declaration.  Comprises {@link ProperTraitIndex} and
 * {@link ObjectTraitIndex}.
 */
public abstract class TraitIndex {
  
  private final TraitObjectAbsDeclOrDecl _ast;
  private final Map<String, Method> _getters;
  private final Map<String, Method> _setters;
  private final Set<Function> _coercions;
  private final Relation<String, Method> _dottedMethods;
  private final Relation<String, FunctionalMethod> _functionalMethods;
  
  public TraitIndex(TraitObjectAbsDeclOrDecl ast,
                    Map<String, Method> getters,
                    Map<String, Method> setters,
                    Set<Function> coercions,
                    Relation<String, Method> dottedMethods,
                    Relation<String, FunctionalMethod> functionalMethods) {
    _ast = ast;
    _getters = getters;
    _setters = setters;
    _coercions = coercions;
    _dottedMethods = dottedMethods;
    _functionalMethods = functionalMethods;
  }

  
  public Map<String, StaticParam> staticParameters() {
    return NI.nyi();
  }
  
  public Set<TypeRef> extendsTypes() {
    return NI.nyi();
  }
  
  public Map<String, Method> getters() {
    return NI.nyi();
  }
  
  public Map<String, Method> setters() {
    return NI.nyi();
  }
  
  public Set<Function> coercions() {
    return NI.nyi();
  }
  
  public Relation<String, Method> dottedMethods() {
    return NI.nyi();
  }
  
  public Relation<String, FunctionalMethod> functionalMethods() {
    return NI.nyi();
  }
  
}
