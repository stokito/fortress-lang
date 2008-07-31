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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.sun.fortress.nodes.AbstractArrowType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.Domain;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.ObjectExpr;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.AnyType;
import com.sun.fortress.nodes.BottomType;
import com.sun.fortress.nodes.IntersectionType;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.TypeParam;
import com.sun.fortress.nodes.UnionType;
import com.sun.fortress.nodes._RewriteGenericArrowType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.TTarrowSetT;
import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer.SubtypeHistory;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

import static com.sun.fortress.nodes_util.NodeFactory.makeId;
import static edu.rice.cs.plt.tuple.Option.*;

/**
 * Contains static utility methods for type checking.
 */
public class TypesUtil {

    public static class ArgList {

        private final List<Type> _args;
        // _keywords is null if there are none (avoiding needless
        // allocation in typical use cases)
        private Map<Id, Type> _keywords;

        public ArgList(Type... args) {
            if (args.length == 0) {
                // more elements will probably be added
                _args = new ArrayList<Type>();
            }
            else {
                // probably won't be more elements
                _args = new ArrayList<Type>(args.length);
            }
            _keywords = null;
            for (Type t : args) { _args.add(t); }
        }

        @Override
        public String toString() {
         return _args.toString();
        }

        /** All add() invocations should occur before calling getters. */
        public void add(Type arg) { _args.add(arg); }

        /** All add() invocations should occur before calling getters. */
        public void add(Id name, Type type) {
            if (_keywords == null) {_keywords = new HashMap<Id, Type>(8); }
            _keywords.put(name, type);
        }

        /**
         * Extract the type represented by non-keywords args.  May be (),
         * a TupleType, or the singleton member of the list of args.
         */
        public Type argType() {
            switch (_args.size()) {
                case 0: return Types.VOID;
                case 1: return _args.get(0);
                default: return new TupleType(_args);
            }
        }

        public Map<Id, Type> keywordTypes() {
            if (_keywords == null) { return Collections.emptyMap(); }
            else { return Collections.unmodifiableMap(_keywords); }
        }

    }

    /**
     * Given the type of a regular function, which is presumed to be an
     * arrow type, its arguments and its static arguments, return the
     * resulting type of the function application.
     * @param checker Needed to check compatibility of arguments.
     * @param fn_type The type of the function.
     * @param args Type of arguments provided by the programmer.
     * @param staticArgs Static arguments provided by the programmer.
     * @return
     */
    public static Option<Pair<Type,ConstraintFormula>> applicationType(final TypeAnalyzer checker,
    		final Type fn_type,
    		final ArgList args,
    		final List<StaticArg> staticArgs) {
    	// List of arrow types that statically match
    	List<AbstractArrowType> matching_types = new ArrayList<AbstractArrowType>();
    	// The constraint formed from all matching arrows
    	ConstraintFormula result_constraint = ConstraintFormula.TRUE;
    	for( Type arrow : conjuncts(fn_type) ) {
    		// create instantiated arrow types using visitor
    		Pair<Option<AbstractArrowType>,ConstraintFormula> pair =
    			arrow.accept(new NodeDepthFirstVisitor<Pair<Option<AbstractArrowType>,ConstraintFormula>>() {
    				@Override
    				public Pair<Option<AbstractArrowType>, ConstraintFormula> defaultCase(
    						Node that) {
    					return Pair.make(Option.<AbstractArrowType>none(), ConstraintFormula.FALSE);
    				}

    				// apply (inferring if necessary) static arguments and checking sub-typing
    				private Pair<Option<AbstractArrowType>,ConstraintFormula>
    				arrowTypeHelper(AbstractArrowType that, List<StaticParam> static_params) {
    					int num_static_params = static_params.size();
    					int num_static_args = staticArgs.size();

    					List<StaticArg> static_args_to_apply = staticArgs;
    					if( num_static_params > 0 && num_static_args == 0 ) {
    						// inference must be done

    						// TODO if parameters are anything but TypeParam, we don't know
    						// how to infer it yet.
    						for( StaticParam p : static_params )
    							if( !(p instanceof TypeParam) ) return Pair.make(Option.<AbstractArrowType>none(), ConstraintFormula.FALSE);

    						static_args_to_apply =
    							CollectUtil.makeList(IterUtil.map(static_params,
    									new Lambda<StaticParam,StaticArg>() {
    								public StaticArg value(StaticParam arg0) {
    									// This is only legal if StaticParam is a TypeParam!!!
    									Type t = NodeFactory.make_InferenceVarType(arg0.getSpan());
    									return new TypeArg(t);
    								}}));
    					}
    					else if( num_static_params != num_static_args ) {
    						// just not the right method
    						return Pair.make(Option.<AbstractArrowType>none(), ConstraintFormula.FALSE);
    					}
    					// now apply the static arguments,
    					that = (AbstractArrowType)
    					that.accept(new StaticTypeReplacer(static_params,static_args_to_apply));
    					// and then check parameter sub-typing
    					ConstraintFormula valid = checker.subtype(args.argType(), Types.stripKeywords(that.getDomain()));
    					return Pair.make(Option.some(that), valid);
    				}
    				@Override
    				public Pair<Option<AbstractArrowType>, ConstraintFormula> for_RewriteGenericArrowType(
    						_RewriteGenericArrowType that) {
    					return this.arrowTypeHelper(that, that.getStaticParams());
    				}
    				@Override
    				public Pair<Option<AbstractArrowType>, ConstraintFormula> forArrowType(
    						ArrowType that) {
    					return this.arrowTypeHelper(that, Collections.<StaticParam>emptyList());
    				}
    			});
    		if( pair.second().isSatisfiable() ) {
    			matching_types.add(pair.first().unwrap());
    			result_constraint = result_constraint.and(pair.second(), checker.new SubtypeHistory());
    		}
    	}

    	// For better error messages, since we could always return bottom
    	if( matching_types.isEmpty() ) {
    		return Option.none();
    	}
    	else {
    		// Now, take all the matching ones and join their ranges to be the result range type.
    		Iterable<Type> ranges =
    			IterUtil.map(matching_types, new Lambda<AbstractArrowType, Type>(){
    				public Type value(AbstractArrowType arg0) {
    					return arg0.getRange();
    				}});
    		return Option.some(Pair.make(checker.meet(ranges), result_constraint));
    	}
    }

    /**
     *
     * Checks whether a type is an arrow or a conjunct of arrows
     *
     */
    public static Boolean isArrows(Type type){
    	boolean valid=true;
    	for(Type t: conjuncts(type)){
    		valid&=t.accept(new NodeDepthFirstVisitor<Boolean>(){
    			@Override public Boolean defaultCase(Node that) {return false;    }
    			@Override public Boolean for_RewriteGenericArrowType(_RewriteGenericArrowType that) {return true;}
    			@Override public Boolean forArrowType(ArrowType that) {return true;}
    		});
    	}
    	return valid;
    }

    /**
     * Figure out the static type of a non-generic function application. This
     * method is a rewrite of the old method with the same name but using a
     * {@code TypeAnalyzer} rather than a Subtype checker. Accordingly,
     * we may have to (in the future)
     * return a ConstraintFormula instead of a type.
     * @param checker the SubtypeChecker to use for any type comparisons
     * @param fn the type of the function, which can be some AbstractArrowType,
     *           or an intersection of such (in the case of an overloaded
     *           function)
     * @param arg the argument to apply to this function
     * @return the return type of the most applicable arrow type in {@code fn},
     *         or {@code Option.none()} if no arrow type matched the args
     */
    public static Option<Pair<Type,ConstraintFormula>> applicationType(final TypeAnalyzer checker,
                                         final Type fn,
                                         final ArgList args) {
     // Just a convenience method
     return applicationType(checker,fn,args,Collections.<StaticArg>emptyList());

     // NEB: I've kept this old code around because it used to handle keyword args and we may want
     // to use this later when we put keyword args back in.

//     ConstraintFormula valid = checker.subtype(args.argType(), Types.stripKeywords(that.getDomain()));
//
//      Map<Id, Type> argMap = args.keywordTypes();
//      Map<Id, Type> paramMap = Types.extractKeywords(that.getDomain());
//      if (paramMap.keySet().containsAll(argMap.keySet())) {
//       for (Map.Entry<Id, Type> entry : argMap.entrySet()) {
//        Type sup = paramMap.get(entry.getKey());
//        //valid &= checker.subtype(entry.getValue(), sup);  creating a new history here is weird
//        valid = valid.and(checker.subtype(entry.getValue(), sup), checker.new SubtypeHistory());
//        //if (!valid) { break; }
    }

    /**
     * Figure out the static type of a non-generic function application.
     * @param checker the SubtypeChecker to use for any type comparisons
     * @param fn the type of the function, which can be some AbstractArrowType,
     *           or an intersection of such (in the case of an overloaded
     *           function)
     * @param arg the argument to apply to this function
     * @return the return type of the most applicable arrow type in {@code fn},
     *         or {@code Option.none()} if no arrow type matched the args
     * @deprecated Deprecated along with the entire SubtypeChecker class.
     */
    @Deprecated
    public static Option<Type> applicationType(final SubtypeChecker checker,
                                               final Type fn,
                                               final ArgList args) {
        // Get a list of the arrow types that match these arguments
        List<ArrowType> matchingArrows = new ArrayList<ArrowType>();
        for (Type arrow : conjuncts(fn)) {

            // Try to form a non-generic ArrowType from this arrow, if it matches the args
            Option<ArrowType> newArrow = arrow.accept(new NodeAbstractVisitor<Option<ArrowType>>() {
                @Override public Option<ArrowType> forArrowType(ArrowType that) {
                    boolean valid = false;
                    if (checker.subtype(args.argType(), Types.stripKeywords(that.getDomain()))) {
                        Map<Id, Type> argMap = args.keywordTypes();
                        Map<Id, Type> paramMap = Types.extractKeywords(that.getDomain());
                        if (paramMap.keySet().containsAll(argMap.keySet())) {
                            valid = true;
                            for (Map.Entry<Id, Type> entry : argMap.entrySet()) {
                                Type sup = paramMap.get(entry.getKey());
                                valid &= checker.subtype(entry.getValue(), sup);
                                if (!valid) { break; }
                            }
                        }
                    }
                    return valid ? some(that) : Option.<ArrowType>none();
                }
                @Override public Option<ArrowType> for_RewriteGenericArrowType(_RewriteGenericArrowType that) {
                    return NI.nyi();
                }
                @Override public Option<ArrowType> defaultCase(Node that) {
                    return none();
                }
            });
            if (newArrow.isSome()) {
                matchingArrows.add(newArrow.unwrap());
            }
        }
        if (matchingArrows.isEmpty()) {
            return none();
        }

        // Find the most applicable arrow type
        // TODO: there's not always a single minimum -- the meet rule may have
        // allowed a declaration that has a minimum at run time, but that doesn't
        // statically (when the runtime type of the argument is not precisely known).
        ArrowType minType = matchingArrows.get(0);
        for (int i=1; i<matchingArrows.size(); ++i) {
            ArrowType t = matchingArrows.get(i);
            if (checker.subtype(t, minType)) {
                minType = t;
            }
        }
        return some(minType.getRange());
    }

    /** Treat the given type as an intersection and get its elements. */
    public static Iterable<Type> conjuncts(Type t) {
        return t.accept(new NodeAbstractVisitor<Iterable<Type>>() {
            @Override public Iterable<Type> forType(Type t) { return IterUtil.make(t); }
            @Override public Iterable<Type> forAnyType(AnyType t) { return IterUtil.empty(); }
            @Override public Iterable<Type> forIntersectionType(IntersectionType t) {
                Iterable<Type> result = IterUtil.empty();
                for (Type elt : t.getElements()) {
                    result = IterUtil.compose(result, elt.accept(this));
                }
                return result;
            }
        });
    }

    /** Treat the given type as a union and get its elements. */
    public static Iterable<Type> disjuncts(Type t) {
        return t.accept(new NodeAbstractVisitor<Iterable<Type>>() {
            @Override public Iterable<Type> forType(Type t) { return IterUtil.make(t); }
            @Override public Iterable<Type> forBottomType(BottomType t) { return IterUtil.empty(); }
            @Override public Iterable<Type> forUnionType(UnionType t) {
                Iterable<Type> result = IterUtil.empty();
                for (Type elt : t.getElements()) {
                    result = IterUtil.compose(result, elt.accept(this));
                }
                return result;
            }
        });
    }

    /**
     * Attempts to apply the given static args to the given type, checking if the given type is
     * even a arrow type, and if so, if the number of args given is the number expected by the
     * arrow type. This method was written to be useful for FnRef, where the static arguments
     * are provided at a subexpression of where the function itself is actually applied. If no
     * arguments are provided, the original type will be returned no matter what it's type.
     * Intersection types will also be handled correctly.
     * @param type
     * @param static_args
     * @return
     */
    public static Option<Type> applyStaticArgsIfPossible(Type type, final List<StaticArg> static_args) {
    	if( static_args.size() == 0 ) {
    		return Option.some(type);
    	}
    	else {
    		return type.accept(new NodeDepthFirstVisitor<Option<Type>>() {
    			@Override
    			public Option<Type> defaultCase(Node that) {
    				return Option.none();
    			}
    			public Option<Type> forIntersectionType(IntersectionType that) {
    				List<Option<Type>> results = this.recurOnListOfType(that.getElements());
    				List<Type> conjuncts = new ArrayList<Type>(results.size());
    				for( Option<Type> t : results ) {
    					if( t.isNone() ) {
    						return Option.none();
    					}
    					else {
    						conjuncts.add(t.unwrap());
    					}
    				}
    				return Option.<Type>some(new IntersectionType(that.getSpan(),that.isParenthesized(),conjuncts));
    			}

    			@Override
    			public Option<Type> for_RewriteGenericArrowType(
    					_RewriteGenericArrowType that) {
    				if( StaticTypeReplacer.argsMatchParams(static_args,that.getStaticParams()) ) {
    					_RewriteGenericArrowType temp = (_RewriteGenericArrowType) that.accept(new StaticTypeReplacer(that.getStaticParams(),static_args));
    					Type new_type = new ArrowType(temp.getSpan(),temp.isParenthesized(),temp.getDomain(),temp.getRange(), temp.getEffect());
    					return Option.some(new_type);
    				}
    				else {
    					return Option.none();
    				}
    			}
    			@Override
    			public Option<Type> forArrowType(ArrowType that) {
    				return Option.<Type>none();
    			}
    		});
    	}
    }

    /**
     * Given an ObjectExpr, returns the Type of the expression.
     * @return
     */
    public static Type getObjectExprType(ObjectExpr obj) {
    	List<Type> extends_types = CollectUtil.makeList(IterUtil.map(obj.getExtendsClause(),
    			new Lambda<TraitTypeWhere,Type>(){
    		public Type value(TraitTypeWhere arg0) {
    			return arg0.getType();
    		}}));
    	Type self_type =
    		extends_types.isEmpty() ?
    				Types.OBJECT :
    				Types.makeIntersection(extends_types);
    	return self_type;
    }
}
