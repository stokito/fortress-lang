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

package com.sun.fortress.compiler.typechecker;

import com.sun.fortress.compiler.*;
import com.sun.fortress.compiler.index.*;
import com.sun.fortress.compiler.typechecker.TypeEnv.BindingLookup;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Option;
import java.util.*;

import static com.sun.fortress.nodes_util.NodeFactory.*;
import static edu.rice.cs.plt.tuple.Option.*;

/** 
 * A type environment whose outermost lexical scope consists of a map from IDs
 * to Variables.
 */
class MethodTypeEnv extends TypeEnv {
    private Relation<IdOrOpOrAnonymousName, Method> entries;
    private TypeEnv parent;
    
    MethodTypeEnv(Relation<IdOrOpOrAnonymousName, Method> _entries, TypeEnv _parent) {
        entries = _entries;
        parent = _parent;
    }
    
    /**
     * Return a BindingLookup that binds the given IdOrOpOrAnonymousName to a type
     * (if the given IdOrOpOrAnonymousName is in this type environment).
     */
    public Option<BindingLookup> binding(IdOrOpOrAnonymousName var) {
        Set<Method> methods = entries.getSeconds(var);
        if (methods.isEmpty()) { return parent.binding(var); }
        
        Type type = Types.ANY;
        for (Method method : methods) {
            if (method instanceof DeclaredMethod) {
                DeclaredMethod _method = (DeclaredMethod)method;
                FnAbsDeclOrDecl decl = _method.ast();
                type = new AndType(type,
                                   makeGenericArrowType(decl.getSpan(),
                                                        decl.getStaticParams(),
                                                        typeFromParams(decl.getParams()),
                                                        unwrap(decl.getReturnType()), // all types have been filled in at this point
                                                        decl.getThrowsClause(),
                                                        decl.getWhere()));
            } else if (method instanceof FieldGetterMethod) {
                FieldGetterMethod _method = (FieldGetterMethod)method;
                LValueBind binding = _method.ast();
                type = makeArrowType(binding.getSpan(),
                                     Types.VOID,
                                     unwrap(binding.getType())); // all types have been filled in at this point
                
            } else { // method instanceof FieldSetterMethod
                final FieldSetterMethod _method = (FieldSetterMethod)method;
                LValueBind binding = _method.ast();
                
                type = makeArrowType(binding.getSpan(),
                                     unwrap(binding.getType()), // all types have been filled in at this point
                                     Types.VOID);
            }
        }
        return some(new BindingLookup(var, type));
    }

    @Override
    public List<BindingLookup> contents() {
        List<BindingLookup> result = new ArrayList<BindingLookup>();
        for (IdOrOpOrAnonymousName name : entries.firstSet()) {
            Option<BindingLookup> element = binding(name);
            if (element.isSome()) {
                result.add(unwrap(element));
            }
        }
        result.addAll(parent.contents());
        return result;
    }
}