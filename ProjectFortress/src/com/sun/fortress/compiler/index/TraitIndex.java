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
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.SimpleName;

import com.sun.fortress.useful.NI;

/**
 * Wraps a trait or object declaration.  Comprises {@link ProperTraitIndex} and
 * {@link ObjectTraitIndex}.
 */
public abstract class TraitIndex extends TypeConsIndex {
    
    private final TraitObjectAbsDeclOrDecl _ast;
    private final Map<IdName, Method> _getters;
    private final Map<IdName, Method> _setters;
    private final Set<Function> _coercions;
    private final Relation<SimpleName, Method> _dottedMethods;
    private final Relation<SimpleName, FunctionalMethod> _functionalMethods;
    
    public TraitIndex(TraitObjectAbsDeclOrDecl ast,
                      Map<IdName, Method> getters,
                      Map<IdName, Method> setters,
                      Set<Function> coercions,
                      Relation<SimpleName, Method> dottedMethods,
                      Relation<SimpleName, FunctionalMethod> functionalMethods) {
        super(ast.getStaticParams());
        _ast = ast;
        _getters = getters;
        _setters = setters;
        _coercions = coercions;
        _dottedMethods = dottedMethods;
        _functionalMethods = functionalMethods;
    }
    
    
    public Set<Type> extendsTypes() {
        return NI.nyi();
    }
    
    public Map<IdName, Method> getters() {
        return NI.nyi();
    }
    
    public Map<IdName, Method> setters() {
        return NI.nyi();
    }
    
    public Set<Function> coercions() {
        return NI.nyi();
    }
    
    public Relation<SimpleName, Method> dottedMethods() {
        return NI.nyi();
    }
    
    public Relation<SimpleName, FunctionalMethod> functionalMethods() {
        return NI.nyi();
    }
    
}
