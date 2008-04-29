package com.sun.fortress.compiler.typechecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sun.fortress.nodes.AndType;
import com.sun.fortress.nodes.ArgType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.OrType;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarargsType;
import com.sun.fortress.nodes._RewriteGenericArrowType;
import com.sun.fortress.nodes_util.NodeFactory;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

import static com.sun.fortress.nodes_util.NodeFactory.makeId;
import static com.sun.fortress.nodes_util.NodeFactory.makeQualifiedIdName;
import static edu.rice.cs.plt.tuple.Option.*;

/**
 * Contains static utility methods for creating and interacting with types.
 */
public class TypesUtil {

    /**
     * Figure out the static type of a non-generic function application.
     * @param checker the SubtypeChecker to use for any type comparisons
     * @param fn the type of the function, which can be some AbstractArrowType,
     *           or an intersection of such (in the case of an overloaded
     *           function)
     * @param args the arguments to apply to this function
     * @return the return type of the most applicable arrow type in {@code fn},
     *         or {@code Option.none()} if no arrow type matched the args
     */
    public static Option<Type> applicationType(final SubtypeChecker checker,
                                               final Type fn,
                                               final Iterable<Type> args) {
        return applicationType(checker, fn, NodeFactory.makeArgType(IterUtil.asList(args)));
    }

    public static Option<Type> applicationType(final SubtypeChecker checker,
                                               final Type fn,
                                               final Type arg) {
    
        // Make sure domain is an ArgType
        final ArgType domain;
        if (arg instanceof ArgType) {
            domain = (ArgType) arg;
        } else if (arg instanceof TupleType) {
            domain = NodeFactory.makeArgType(((TupleType)arg).getElements());
        } else {
            domain = NodeFactory.makeArgType(Collections.singletonList(arg));
        }
    
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
                    return checker.subtype(domain, that.getDomain())
                        ? some(that)
                        : Option.<ArrowType>none();
                }
                @Override public Option<ArrowType> for_RewriteGenericArrowType(_RewriteGenericArrowType that) {
                    return none(); // TODO - implement
                }
                @Override public Option<ArrowType> defaultCase(Node that) {
                    return none();
                }
            });
            if (newArrow.isSome()) {
                matchingArrows.add(unwrap(newArrow));
            }
        }
        if (matchingArrows.isEmpty()) {
            return none();
        }
    
        // Find the most applicable arrow type
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
                                               Type arrow,
                                               Iterable<Type> args,
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

    public static final Type fromVarargsType(VarargsType varargsType) {
        return NodeFactory.makeInstantiatedType(varargsType.getSpan(),
                                                false,
                                                makeQualifiedIdName(Arrays.asList(makeId("FortressBuiltin")),
                                                                    makeId("ImmutableHeapSequence")));
    }
    
    

}
