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

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Collections;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Pair;
import edu.rice.cs.plt.iter.IterUtil;
import com.sun.fortress.nodes.TraitObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.SimpleName;

import com.sun.fortress.useful.NI;

/**
 * Wraps a trait or object declaration.  Comprises {@link ProperTraitIndex} and
 * {@link ObjectTraitIndex}.
 */
public abstract class TraitIndex extends TypeConsIndex {
    
    private final TraitObjectAbsDeclOrDecl _ast;
    private final Map<Id, Method> _getters;
    private final Map<Id, Method> _setters;
    private final Set<Function> _coercions;
    private final Relation<SimpleName, Method> _dottedMethods;
    private final Relation<SimpleName, FunctionalMethod> _functionalMethods;
    
    public TraitIndex(TraitObjectAbsDeclOrDecl ast,
                      Map<Id, Method> getters,
                      Map<Id, Method> setters,
                      Set<Function> coercions,
                      Relation<SimpleName, Method> dottedMethods,
                      Relation<SimpleName, FunctionalMethod> functionalMethods) {
        _ast = ast;
        _getters = getters;
        _setters = setters;
        _coercions = coercions;
        _dottedMethods = dottedMethods;
        _functionalMethods = functionalMethods;
    }
    
    public TraitObjectAbsDeclOrDecl ast() { return _ast; }
    
    public List<StaticParam> staticParameters() { return _ast.getStaticParams(); }
    
    public List<Id> hiddenParameters() { return Collections.emptyList(); }
    
    /**
     * Return all subtype relationships described by parameter bounds and where clauses.
     * Each pair {@code p} in the list is an assertion that {@code p.first()} is a subtype
     * of {@code p.second()}.
     */
    public Iterable<Pair<Type, Type>> typeConstraints() { return IterUtil.empty(); }
    
    public List<TraitTypeWhere> extendsTypes() {
        return _ast.getExtendsClause();
    }
    
    public Map<Id, Method> getters() {
        return _getters;
    }
    
    public Map<Id, Method> setters() {
        return _setters;
    }
    
    public Set<Function> coercions() {
        return _coercions;
    }
    
    public Relation<SimpleName, Method> dottedMethods() {
        return _dottedMethods;
    }
    
    public Relation<SimpleName, FunctionalMethod> functionalMethods() {
        return _functionalMethods;
    }
    
}
