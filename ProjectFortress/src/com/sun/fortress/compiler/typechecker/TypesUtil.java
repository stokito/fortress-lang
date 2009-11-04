/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

import static edu.rice.cs.plt.tuple.Option.none;
import static edu.rice.cs.plt.tuple.Option.some;
import static com.sun.fortress.nodes_util.NodeFactory.typeSpan;
import static com.sun.fortress.scala_src.useful.Lists.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.typechecker.constraints.ConstraintFormula;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes._InferenceVarType;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.scala_src.typechecker.staticenv.*;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Box;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;
import static com.sun.fortress.compiler.typechecker.constraints.ConstraintUtil.*;

/**
 * Contains static utility methods for type checking.
 */
public class TypesUtil {
  
    /** The empty type environment as defined in the Scala code. */
    public static STypeEnv EMPTY_TYPE_ENV = STypeEnv$.MODULE$.EMPTY();
    
    /** The empty kind environment as defined in the Scala code. */
    public static KindEnv makeFreshKindEnv() {
      return KindEnv$.MODULE$.makeFresh();
    }

    /** Currently exists in TypeEnv class too. */
    public static List<StaticArg> staticParamsToArgs(List<StaticParam> params) {
        List<StaticArg> result = new ArrayList<StaticArg>();

        for (StaticParam param: params) {
            final IdOrOp name = param.getName();
            result.add(param.getKind().accept(new NodeAbstractVisitor<StaticArg>() {
                        public StaticArg forKindBool(KindBool k) {
                            return NodeFactory.makeBoolArg(typeSpan,
                                                           NodeFactory.makeBoolRef(typeSpan, (Id)name));
                        }
                        public StaticArg forKindDim(KindDim k) {
                            return NodeFactory.makeDimArg(typeSpan,
                                                          NodeFactory.makeDimRef(typeSpan, (Id)name));
                        }
                        public StaticArg forKindInt(KindInt k) {
                            return NodeFactory.makeIntArg(typeSpan,
                                                          NodeFactory.makeIntRef(typeSpan, (Id)name));
                        }
                        public StaticArg forKindNat(KindNat k) {
                            return NodeFactory.makeIntArg(typeSpan,
                                                          NodeFactory.makeIntRef(typeSpan, (Id)name));
                        }
                        public StaticArg forKindType(KindType k) {
                            return NodeFactory.makeTypeArg(typeSpan,
                                                           NodeFactory.makeVarType(typeSpan, (Id)name));
                        }
                        public StaticArg forKindUnit(KindUnit k) {
                            return NodeFactory.makeUnitArg(typeSpan, NodeFactory.makeUnitRef(typeSpan, false, (Id)name));
                        }
                        public StaticArg forKindOp(KindOp that) {
                            return NodeFactory.makeOpArg(typeSpan,
                                                         ExprFactory.makeOpRef((Op)name));
                        }
                    }));
        }
        return result;
    }
    
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
                default: return NodeFactory.makeTupleType(NodeFactory.makeSpan("impossible", _args), _args);
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
     * @param existingConstraint Any existing constraints that should be taken into account when
     *        choosing an overloading.
     * @return
     */
    public static Option<Pair<Type,ConstraintFormula>> applicationType(final TypeAnalyzer checker,
            final Type fn_type,
            final ArgList args,
            final List<StaticArg> staticArgs,
            final ConstraintFormula existingConstraint) {
        // List of arrow types that statically match
        List<ArrowType> matching_types = new ArrayList<ArrowType>();
        // The constraint formed from all matching arrows
        ConstraintFormula result_constraint = trueFormula();
        for( Type arrow : conjuncts(fn_type) ) {
            // create instantiated arrow types using visitor
            Pair<Option<ArrowType>,ConstraintFormula> pair =
                arrow.accept(new NodeDepthFirstVisitor<Pair<Option<ArrowType>,ConstraintFormula>>() {
                    @Override
                    public Pair<Option<ArrowType>, ConstraintFormula> defaultCase(
                            Node that) {
                        return Pair.make(Option.<ArrowType>none(), falseFormula());
                    }

                    // apply (inferring if necessary) static arguments and checking sub-typing
                    private Pair<Option<ArrowType>,ConstraintFormula>
                    arrowTypeHelper(ArrowType that, List<StaticParam> static_params) {
                        int num_static_params = static_params.size();
                        int num_static_args = staticArgs.size();

                        List<StaticArg> static_args_to_apply = staticArgs;
                        if( num_static_params > 0 && num_static_args == 0 ) {
                            // inference must be done

                            // TODO if parameters are anything but TypeParam, we don't know
                            // how to infer it yet.
                            for( StaticParam p : static_params )
                                if( !(NodeUtil.isTypeParam(p)) )
                                    return Pair.make(Option.<ArrowType>none(), falseFormula());

                            static_args_to_apply =
                                CollectUtil.makeList(IterUtil.map(static_params,
                                        new Lambda<StaticParam,StaticArg>() {
                                    public StaticArg value(StaticParam arg0) {
                                        // This is only legal if StaticParam is a TypeParam!!!
                                        Type t = NodeFactory.make_InferenceVarType(NodeUtil.getSpan(arg0));
                                        return NodeFactory.makeTypeArg(NodeFactory.makeSpan(t), t);
                                    }}));
                        }
                        else if( num_static_params != num_static_args ) {
                            // just not the right method
                            return Pair.make(Option.<ArrowType>none(), falseFormula());
                        }
                        // now apply the static arguments,
                        that = (ArrowType)
                        that.accept(new StaticTypeReplacer(static_params,static_args_to_apply));
                        // and then check parameter sub-typing
                        ConstraintFormula valid = checker.subtype(args.argType(), Types.stripKeywords(that.getDomain()));
                        return Pair.make(Option.some(that), valid);
                    }
                    @Override
                    public Pair<Option<ArrowType>, ConstraintFormula> forArrowType(
                            ArrowType that) {
                        return this.arrowTypeHelper(that, NodeUtil.getStaticParams(that));
                    }
                });
            ConstraintFormula temp =  pair.second().and(existingConstraint, new SubtypeHistory(checker));
            if(temp.isSatisfiable() ) {
                matching_types.add(pair.first().unwrap());
                result_constraint = result_constraint.and(pair.second(), new SubtypeHistory(checker));
            }
        }

        // For better error messages, since we could always return bottom
        if( matching_types.isEmpty() ) {
            return Option.none();
        }
        else {
            // Now, take all the matching ones and join their ranges to be the result range type.
            Iterable<Type> ranges =
                IterUtil.map(matching_types, new Lambda<ArrowType, Type>(){
                    public Type value(ArrowType arg0) {
                        return arg0.getRange();
                    }});
            Type range_type = checker.meet(ranges);
            range_type = checker.normalize(range_type);
            return Option.some(Pair.make(range_type, result_constraint));
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
     * @param checker the TypeAnalyzer to use for any type comparisons
     * @param fn the type of the function, which can be some ArrowType,
     *           or an intersection of such (in the case of an overloaded
     *           function)
     * @param existingConstraint Any additional constraints that should be taken into
     *        account when choosing an overloading.
     * @param arg the argument to apply to this function
     * @return the return type of the most applicable arrow type in {@code fn},
     *         or {@code Option.none()} if no arrow type matched the args
     */
    public static Option<Pair<Type,ConstraintFormula>> applicationType(final TypeAnalyzer checker,
            final Type fn,
            final ArgList args,
            final ConstraintFormula existingConstraint) {
        // Just a convenience method
        return applicationType(checker,fn,args,
                               Collections.<StaticArg>emptyList(), existingConstraint);
    }

    /** Treat the given type as an intersection and get its elements. */
    public static Iterable<Type> conjuncts(Type intersection_type) {
        return intersection_type.accept(new NodeAbstractVisitor<Iterable<Type>>() {
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
    public static Iterable<Type> disjuncts(Type union_type) {
        return union_type.accept(new NodeAbstractVisitor<Iterable<Type>>() {
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
     * Does the given ast contain any of the given InferenceVarTypes?
     * @param ast The Node tree in which to check.
     * @param ivars A set of inference variables to check for. If any of these are found in the ast,
     *     this method returns true. If ivars is null, then this is as if the set contained
     *     everything.
     * @return true if any of ivars are found recursively in ast, false otherwise
     */
    public static boolean containsInferenceVarTypes(Node ast, final Set<_InferenceVarType> ivars) {
        final Box<Boolean> result_ = new Box<Boolean>() {
            Boolean b = Boolean.FALSE;
            public void set(Boolean arg0) { b = arg0; }
            public Boolean value() { return b; }
        };

        ast.accept(new NodeDepthFirstVisitor_void() {
            @Override
            public void for_InferenceVarType(_InferenceVarType that) {
                if (ivars == null || ivars.contains(that)) {
                    result_.set(Boolean.TRUE);
                }
            }
        });

        return result_.value();
    }

    /**
     * Does the given ast contain any InferenceVarTypes?
     * @param ast The Node tree in which to check for inference variables.
     * @return true if any inference variables are found in the ast, false otherwise
     */
    public static boolean containsInferenceVarTypes(Node ast) {
        return containsInferenceVarTypes(ast, null);
    }

    /**
     * Does the given ast contain any AST nodes
     * that should be removed after type checking?
     *
     * After type checking, the following nodes should be removed:
     *     ArrayType
     *     MatrixType
     *     _InferenceVarType
     */
    public static boolean assertAfterTypeChecking(Node ast) {
        final Set<_InferenceVarType> ivars;
        final Box<Boolean> result_ = new Box<Boolean>() {
            Boolean b = Boolean.FALSE;
            public void set(Boolean arg0) { b = arg0; }
            public Boolean value() { return b; }
        };

        ast.accept(new NodeDepthFirstVisitor_void() {
            @Override
            public void forOutAfterTypeCheckingOnly(OutAfterTypeChecking that) {
                result_.set(Boolean.TRUE);
            }
        });

        return result_.value();
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
    public static Option<Pair<Type,ConstraintFormula>> applyStaticArgsIfPossible(Type type, final List<StaticArg> static_args, final TypeAnalyzer subtype_checker) {
    	if( static_args.size() == 0 ) {
    		return Option.some(Pair.make(type, trueFormula()));
    	}
    	else {
    		return type.accept(new NodeDepthFirstVisitor<Option<Pair<Type,ConstraintFormula>>>() {
    			@Override
    			public Option<Pair<Type,ConstraintFormula>> defaultCase(Node that) {
    				return Option.none();
    			}

    			@Override
    			public Option<Pair<Type,ConstraintFormula>> forIntersectionType(IntersectionType that) {
    				List<Option<Pair<Type,ConstraintFormula>>> results = this.recurOnListOfType(that.getElements());
    				List<Type> conjuncts = new ArrayList<Type>(results.size());
    				ConstraintFormula accumulated_constraint=trueFormula();
    				for( Option<Pair<Type,ConstraintFormula>> t : results ) {
    					if( t.isNone() ) {
    						return Option.none();
    					}
    					else {
    						conjuncts.add(t.unwrap().first());
    						accumulated_constraint.and(t.unwrap().second(), new SubtypeHistory(subtype_checker));
    					}
    				}
    				return Option.some(Pair.make((Type) NodeFactory.makeIntersectionType(NodeUtil.getSpan(that),
                                                                                                     NodeUtil.isParenthesized(that),
                                                                                                     conjuncts),accumulated_constraint));
    			}

    			@Override
    			public Option<Pair<Type,ConstraintFormula>> forArrowType(ArrowType that) {

    				Option<ConstraintFormula> constraints = StaticTypeReplacer.argsMatchParams(static_args,
                                                                                                           NodeUtil.getStaticParams(that),
                                                                                                           subtype_checker);

    				if(constraints.isSome()) {
    					ArrowType temp = (ArrowType) that.accept(new StaticTypeReplacer(NodeUtil.getStaticParams(that),static_args));
    					Type new_type = NodeFactory.makeArrowType(NodeUtil.getSpan(temp),NodeUtil.isParenthesized(temp),
                                                                      temp.getDomain(),temp.getRange(), temp.getEffect(),
                                                                      Collections.<StaticParam>emptyList(),
                                                                      Option.<WhereClause>none());
    					return Option.some(Pair.make(new_type,constraints.unwrap()));
    				}
    				else {
    					return Option.none();
    				}
    			}
    		});
    	}
    }

    /**
     * Take a node with _InferenceVarType, and TypeCheckerResults, which containt
     * constraints on those inference vars, solve, and replace the inference vars
     * in the node.
     */
    public static Pair<Boolean,Node> closeConstraints(Node node, TypeCheckerResult result) {
        result.getIVarResults();
        InferenceVarReplacer rep = new InferenceVarReplacer(result.getIVarResults());
        Node new_node = node.accept(rep);
        return Pair.make(result.getNodeConstraints().isSatisfiable(), new_node);
    }

    /**
     * Take a node with _InferenceVarType, and TypeCheckerResults, which containt
     * constraints on those inference vars, solve, and replace the inference vars
     * in the node.
     */
    public static Pair<Boolean,Node> closeConstraints(Node node,
            TypeAnalyzer subtypeChecker, TypeCheckerResult... results) {
        TypeCheckerResult result = TypeCheckerResult.compose(node, subtypeChecker, results);
        return closeConstraints(node, result);
    }

    /** Given a list of Types, produce a list of static arguments, each one a TypeArg. */
    public static List<StaticArg> staticArgsFromTypes(List<Type> types) {
        if( types.isEmpty() ) return Collections.emptyList();

        List<StaticArg> result = new ArrayList<StaticArg>(types.size());
        for( Type ty : types ) {
            result.add(NodeFactory.makeTypeArg(ty));
        }
        return result;
    }

    public static boolean overloadingRequiresStaticArgs(List<Type> overloaded_types) {
        for(Type overloaded_type : overloaded_types ) {
            for( Type conj : conjuncts(overloaded_type) ) {
                Boolean b = conj.accept(new TypeAbstractVisitor<Boolean>(){
                    @Override public Boolean forArrowType(ArrowType that) { return !NodeUtil.getStaticParams(that).isEmpty(); }
                    @Override public Boolean forType(Type that) { return Boolean.FALSE; }
                });
                if( b ) return true;
            }
        }
        return false;
    }
}
