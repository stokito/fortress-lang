/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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
import com.sun.fortress.scala_src.useful.Lists;

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
