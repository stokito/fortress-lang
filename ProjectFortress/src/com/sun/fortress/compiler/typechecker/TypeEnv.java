/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.compiler.index.Variable;
import com.sun.fortress.compiler.index.ParamVariable;
import com.sun.fortress.compiler.index.SingletonVariable;
import com.sun.fortress.compiler.index.DeclaredVariable;
import edu.rice.cs.plt.tuple.Option;
import java.util.*;

import static com.sun.fortress.nodes_util.NodeFactory.*;

/**
 * This class is used by the type checker to represent static type environments,
 * mapping bound variables to their types.
 */
public abstract class TypeEnv {
    public static TypeEnv make(LValueBind... entries) {
        return EmptyTypeEnv.ONLY.extend(entries);
    }

    public abstract Option<LValueBind> binding(Id var);
    public abstract Option<Type> type(Id var);
    public abstract Option<List<Modifier>> mods(Id var);
    public abstract Option<Boolean> mutable(Id var);

    public Option<Type> type(String var) { return type(makeId(var)); }
    public Option<List<Modifier>> mods(String var) { return mods(makeId(var)); }
    public Option<Boolean> mutable(String var) { return mutable(makeId(var)); }

    /**
     * Produce a new type environment extending this with the given variable bindings.
     */
    public TypeEnv extend(LValueBind... entries) {
        if (entries.length == 0) { return EmptyTypeEnv.ONLY; }
        else { return new NonEmptyTypeEnv(entries, this); }
    }

    public TypeEnv extend(Map<Id, Variable> vars) {
        ArrayList<LValueBind> lvals = new ArrayList<LValueBind>();

        for (Variable var: vars.values()) {
            if (var instanceof ParamVariable) {
                Param ast = ((ParamVariable)var).ast();
                if (ast instanceof NormalParam) {
                    lvals.add(NodeFactory.makeLValue((NormalParam)ast));
                } else { // ast instanceof VarargsParam
                    lvals.add(NodeFactory.makeLValue(ast.getName(),
                        makeInstantiatedType
                            (ast.getSpan(),
                             false,
                             makeQualifiedIdName
                                 (Arrays.asList
                                      (makeId("FortressBuiltin")),
                                  makeId("ImmutableHeapSequence")),
                             new TypeArg(((VarargsParam)ast).
                                             getVarargsType().getType()))));
                }
            } else if (var instanceof SingletonVariable) {
                // Singleton objects declare both a value and a type with the same name.
                Id nameAndType = ((SingletonVariable)var).declaringTrait();
                lvals.add(NodeFactory.makeLValue(nameAndType, nameAndType));
            } else { // entry instanceof DeclaredVariable
                lvals.add(((DeclaredVariable)var).ast());
            }
        }
        LValueBind[] result = new LValueBind[lvals.size()];
        return this.extend(lvals.toArray(result));
    }

    // I think this is the wrong approach. We should instead create various
    // subtypes of TypeEnvs corresponding to function environments, var environments,
    // etc.
//    public TypeEnv extend(Relation<SimpleName, Function> functions) {
//        // First build up map of functions -> names in a HashMap.
//        HashMap<SimpleName, Function> fnMap = new HashMap<SimpleName, Function>();
//        ArrayList<LValueBind> lvals = new ArrayList<LValueBind>();
//
//        for (SimpleName name: functions.firstSet()) {
//            List<Type> elements = new ArrayList<Type>();
//            for (Function fn: functions.getSeconds(name)) {
//                elements.add(fn.instantiatedType
//            if (fnMap.containsKey(pair.first())
//
//
//    }

}
