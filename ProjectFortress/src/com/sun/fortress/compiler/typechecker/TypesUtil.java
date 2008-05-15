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

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.AndType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.Domain;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.OrType;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes._RewriteGenericArrowType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.compiler.Types;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

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
     * Figure out the static type of a non-generic function application.
     * @param checker the SubtypeChecker to use for any type comparisons
     * @param fn the type of the function, which can be some AbstractArrowType,
     *           or an intersection of such (in the case of an overloaded
     *           function)
     * @param arg the argument to apply to this function
     * @return the return type of the most applicable arrow type in {@code fn},
     *         or {@code Option.none()} if no arrow type matched the args
     */
    public static Option<Type> applicationType(final SubtypeChecker checker,
                                               final Type fn,
                                               final ArgList args) {
        // Turn fn into a list of types (i.e. flatten if an intersection)
        final Iterable<Type> arrows =
            (fn instanceof AndType) ? conjuncts((AndType)fn)
                                    : IterUtil.make(fn);

        // Get a list of the arrow types that match these arguments
        List<ArrowType> matchingArrows = new ArrayList<ArrowType>();
        for (Type arrow : arrows) {

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
                    return none(); // TODO - implement
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

    public static Option<Type> applicationType(SubtypeChecker checker,
                                               Type fn,
                                               ArgList args,
                                               Iterable<StaticArg> staticArgs) {
        return Option.<Type>none(); // TODO implement
    }

    /** Get all the conjunct types from a nested AndType. */
    public static Iterable<Type> conjuncts(AndType types) {
        Type left = types.getFirst();
        Type right = types.getSecond();
        return IterUtil.compose(
                (left instanceof AndType) ? conjuncts((AndType)left) : IterUtil.make(left),
                (right instanceof AndType) ? conjuncts((AndType)right) : IterUtil.make(right));
    }

    /** Get all the disjunct types from a nested OrType. */
    public static Iterable<Type> disjuncts(OrType types) {
        Type left = types.getFirst();
        Type right = types.getSecond();
        return IterUtil.compose(
                (left instanceof OrType) ? disjuncts((OrType)left) : IterUtil.make(left),
                (right instanceof OrType) ? disjuncts((OrType)right) : IterUtil.make(right));
    }
    
}
