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

import static com.sun.fortress.exceptions.InterpreterBug.bug;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

import java.util.*;

import static com.sun.fortress.nodes_util.NodeFactory.*;

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
    	IdOrOpOrAnonymousName no_api_var = removeApi(var);
    	
    	Set<Method> methods = entries.matchFirst(no_api_var);
        if (methods.isEmpty()) {
            return parent.binding(var);
        }

        List<Type> overloads = new ArrayList<Type>();
        for (Method method : methods) {
            if (method instanceof DeclaredMethod) {
                DeclaredMethod _method = (DeclaredMethod)method;
                overloads.add(genericArrowFromDecl(_method.ast()));
            } else if (method instanceof FieldGetterMethod) {
                FieldGetterMethod _method = (FieldGetterMethod)method;
                LValueBind binding = _method.ast();
                overloads.add(makeArrowType(binding.getSpan(),
                                            Types.VOID,
                                             // all types have been filled in at this point
                                            binding.getType().unwrap()));

            } else { // method instanceof FieldSetterMethod
                final FieldSetterMethod _method = (FieldSetterMethod)method;
                LValueBind binding = _method.ast();

                overloads.add(makeArrowType(binding.getSpan(),
                                            binding.getType().unwrap(),
                                            // all types have been filled in at this point
                                            Types.VOID));
            }
        }
        return Option.some(new BindingLookup(var, new IntersectionType(overloads)));
    }

    @Override
    public List<BindingLookup> contents() {
        List<BindingLookup> result = new ArrayList<BindingLookup>();
        for (IdOrOpOrAnonymousName name : entries.firstSet()) {
            Option<BindingLookup> element = binding(name);
            if (element.isSome()) {
                result.add(element.unwrap());
            }
        }
        result.addAll(parent.contents());
        return result;
    }

	@Override
	public Option<Node> declarationSite(IdOrOpOrAnonymousName var) {
   	IdOrOpOrAnonymousName no_api_var = removeApi(var);
    	
    	Set<Method> methods = entries.matchFirst(no_api_var);
        if (methods.isEmpty()) {
            return parent.declarationSite(var);
        }
		
		throw new IllegalArgumentException("The declarationSite method should not be called on any functions, but was called on " + var);
	}

	@Override
	public TypeEnv replaceAllIVars(Map<_InferenceVarType, Type> ivars) {

		Set<Pair<IdOrOpOrAnonymousName, Method>> new_entries = new HashSet<Pair<IdOrOpOrAnonymousName, Method>>();
		Iterator<Pair<IdOrOpOrAnonymousName, Method>> iter = entries.iterator();
		InferenceVarReplacer rep = new InferenceVarReplacer(ivars);
		
		while(iter.hasNext()) {
			Pair<IdOrOpOrAnonymousName, Method> p = iter.next();
			
			Method m = p.second();
			Method new_m = (Method)m.acceptNodeUpdateVisitor(rep);
			
			new_entries.add(Pair.make(p.first(), new_m));
		}
		
		return new MethodTypeEnv(CollectUtil.makeRelation(new_entries),
				parent.replaceAllIVars(ivars));
	}
}
