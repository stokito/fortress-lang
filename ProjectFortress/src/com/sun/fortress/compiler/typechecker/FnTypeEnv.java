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

import static com.sun.fortress.nodes_util.NodeFactory.makeEffect;
import static com.sun.fortress.nodes_util.NodeFactory.makeTraitType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.compiler.index.Constructor;
import com.sun.fortress.compiler.index.DeclaredFunction;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.index.FunctionalMethod;
import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.IntersectionType;
import com.sun.fortress.nodes.NormalParam;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.TypeParam;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes._RewriteGenericArrowType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Predicate;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

/**
 * A type environment whose outermost lexical scope consists of a map from IDs
 * to Variables.
 */
class FnTypeEnv extends TypeEnv {
    private Relation<IdOrOpOrAnonymousName, ? extends Function> entries;
    private TypeEnv parent;

    FnTypeEnv(Relation<IdOrOpOrAnonymousName, ? extends Function> _entries, TypeEnv _parent) {
        parent = _parent;
        entries = _entries;
    }

    // Takes a functional method declaration like: 
    // opr IN(x:E, self:Generator[\E\]):Boolean
    // and returns the type we want it to have,
    // (x:$i, Generator[\$i\]) -> Boolean
    private Type replaceGenericSelfParamsWithInferenceVars(FnAbsDeclOrDecl fn) {
    	// First we need to get the lists of args -> params
    	// To do this we must find self's type
    	
    	Option<Type> self_type_ = Option.none();
    	for( Param param : fn.getParams() ) {
    		if( param.getName().equals(IndexBuilder.SELF_NAME) ) {
    			if( param instanceof NormalParam )
    				self_type_ = ((NormalParam)param).getType();
    			else
    				InterpreterBug.bug("self cannot be a varargs.");
    		}
    	}
    	if( self_type_.isNone() )
    		InterpreterBug.bug("We said this was a functional method (" + fn + ") but maybe it wasn't.");
    	TraitType self_type = (TraitType)self_type_.unwrap();
    	
    	// This may be a seriously misguided attempt to get StaticParams from StaticArgs... NEB
    	Iterable<StaticArg> only_typeargs = IterUtil.filter(self_type.getArgs(), new Predicate<StaticArg>(){
			public boolean contains(StaticArg arg0) { return arg0 instanceof TypeArg; }});
    	Iterable<Pair<StaticParam,StaticArg>> replacement_pairs =
    		IterUtil.map(only_typeargs, new Lambda<StaticArg,Pair<StaticParam,StaticArg>>(){
				public Pair<StaticParam, StaticArg> value(StaticArg arg0) {
					// Ugh..
					TypeArg type_arg = (TypeArg)arg0;
					VarType v = (VarType)type_arg.getType();
					StaticParam p = new TypeParam(v.getName());
					StaticArg a = NodeFactory.makeTypeArg(NodeFactory.make_InferenceVarType());
					return Pair.make(p, a);
				}});
    	
    	// Now replace
    	StaticTypeReplacer st_replacer = 
    		new StaticTypeReplacer(CollectUtil.makeList(IterUtil.pairFirsts(replacement_pairs)),
    				               CollectUtil.makeList(IterUtil.pairSeconds(replacement_pairs)));
    	
    	return (Type)genericArrowFromDecl(fn).accept(st_replacer);
    }
    
    /**
     * Return a BindingLookup that binds the given Id to a type
     * (if the given Id is in this type environment).
     */
    public Option<BindingLookup> binding(IdOrOpOrAnonymousName var) {
    	IdOrOpOrAnonymousName no_api_var = removeApi(var);
    	
    	Set<? extends Function> fns = entries.matchFirst(no_api_var);
        if (fns.isEmpty()) {
            return parent.binding(var);
        }

        LinkedList<Type> overloadedTypes = new LinkedList<Type>();
        for (Function fn: fns) {
            if (fn instanceof DeclaredFunction) {
                DeclaredFunction _fn = (DeclaredFunction)fn;
                overloadedTypes.add(genericArrowFromDecl(_fn.ast()));
            } else if (fn instanceof FunctionalMethod) {
                FunctionalMethod _fn = (FunctionalMethod)fn;
                FnAbsDeclOrDecl decl = _fn.ast();
                overloadedTypes.add(replaceGenericSelfParamsWithInferenceVars(decl));
            } else { // fn instanceof Constructor
                final Constructor _fn = (Constructor)fn;
                Span loc = _fn.declaringTrait().getSpan();
                // Make trait name fully qualified, since Constructors are not.
                Id qualified_trait_name = NodeFactory.makeId(var.getApi(), _fn.declaringTrait());
                Type selfType = makeTraitType(qualified_trait_name,
                                              staticParamsToArgs(_fn.staticParameters()));
                
                // Invariant: _fn.params().isSome()
                // Otherwise, _fn should not have been in entries.
                overloadedTypes.add(new _RewriteGenericArrowType(loc, _fn.staticParameters(),
                                                                 domainFromParams(_fn.parameters()),
                                                                 selfType,
                                                                 makeEffect(loc.getEnd(), CollectUtil.makeList(_fn.thrownTypes())),
                                                                 _fn.where()));
            }
        }
        return Option.some(new BindingLookup(var, new IntersectionType(overloadedTypes)));
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
}
