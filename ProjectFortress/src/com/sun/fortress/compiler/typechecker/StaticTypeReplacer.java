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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.fortress.nodes.ArgType;
import com.sun.fortress.nodes.BoolRef;
import com.sun.fortress.nodes.DimRef;
import com.sun.fortress.nodes.UnitRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdStaticParam;
import com.sun.fortress.nodes.IdType;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes.IntRef;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.OprParam;
import com.sun.fortress.nodes.OprArg;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes._RewriteGenericSingletonType;
import com.sun.fortress.nodes_util.NodeFactory;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;

import static com.sun.fortress.nodes_util.NodeFactory.*;
import static edu.rice.cs.plt.tuple.Option.*;

/**
 * This class will replace all occurrences of a static parameter in a given
 * type with its instantiated static argument. A StaticTypeReplacer is created with
 * some static parameters and corresponding static arguments. Each type P_i in
 * the parameters will be replaced with the corresponding type A_i in the
 * arguments, for every occurrence of P_i in some outer type T.
 *
 * For example, for static parameters [\U, V\], static arguments
 * [\ZZ32, String\], and outer type Pair<V,U>, the type replacer will return
 * type Pair<String, ZZ32>.
 */
public class StaticTypeReplacer extends NodeUpdateVisitor {

    /** Map parameter name to the static argument bound to it. */
    private final Map<IdOrOpOrAnonymousName, StaticArg> parameterMap;

    /** Assume params.size() == args.size() */
    public StaticTypeReplacer(List<StaticParam> params, List<StaticArg> args) {
        assert(params.size() == args.size());
        int n = params.size();
        parameterMap = new HashMap<IdOrOpOrAnonymousName, StaticArg>(n);
        for (int i=0; i<n; ++i) {
            IdOrOpOrAnonymousName name;
            StaticParam p = params.get(i);
            if (p instanceof OprParam) {
                name = ((OprParam)p).getName();
                //                System.err.printf("put op: %s\n", name);
            } else {
                name = ((IdStaticParam)p).getName();
            }
            parameterMap.put(name, args.get(i));
        }
    }

    public Type replaceIn(Type t) {
        return (Type)t.accept(this); // TODO safe?
    }

    private Node updateNode(Node that, QualifiedIdName name) {
        if (name.getApi().isSome()) { return that; }
        StaticArg arg = parameterMap.get(name.getName());
        return arg instanceof TypeArg ? ((TypeArg) arg).getType() : that;
    }

    private Node updateNode(Node that, Id name) {
        if (name.getApi().isSome()) { return that; }
        StaticArg arg = parameterMap.get(name);
        return arg == null ? that : arg;
    }

    // ----------- VISITOR METHODS ---------------

    @Override
    public Node forIdType(IdType that) {
        return updateNode(that, that.getName());
    }

    @Override
    public Node forOprArg(OprArg that) {
        StaticArg arg = parameterMap.get(that.getName());
        //        System.err.printf("forOprArg lookup: %s\n", arg);
        return arg == null ? that : arg;
    }

    @Override
    public Node forIntRef(IntRef that) {
        return updateNode(that, that.getName());
    }

    @Override
    public Node forBoolRef(BoolRef that) {
        return updateNode(that, that.getName());
    }

    @Override
    public Node forDimRef(DimRef that) {
        return updateNode(that, that.getName());
    }

    @Override
    public Node forUnitRef(UnitRef that) {
        return updateNode(that, that.getName());
    }

    @Override
    public Node forVarRef(VarRef that) {
        return updateNode(that, that.getVar());
    }
}
