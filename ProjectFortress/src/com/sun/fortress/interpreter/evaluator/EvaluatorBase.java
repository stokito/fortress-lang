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

package com.sun.fortress.interpreter.evaluator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.BottomType;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.TypeLatticeOps;
import com.sun.fortress.interpreter.evaluator.values.FGenericFunction;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;
import com.sun.fortress.interpreter.nodes.BaseNodeVisitor;
import com.sun.fortress.interpreter.nodes.NodeVisitor;
import com.sun.fortress.interpreter.nodes.Option;
import com.sun.fortress.interpreter.nodes.Param;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.useful.ABoundingMap;
import com.sun.fortress.interpreter.useful.HasAt;


public class EvaluatorBase<T> extends BaseNodeVisitor<T> implements
        NodeVisitor<T> {

    final public BetterEnv e;

    protected EvaluatorBase(BetterEnv e) {
        this.e = e;
    }

    protected FValue functionInvocation(List<FValue> args, FValue foo,
            com.sun.fortress.interpreter.nodes.Node loc) {
        if (foo instanceof Fcn) {
            return functionInvocation(args, (Fcn) foo, loc);
        } else {
            throw new ProgramError(loc, "Not a Fcn: " + foo);
        }
    }

    protected FValue functionInvocation(List<FValue> args, Fcn foo, com.sun.fortress.interpreter.nodes.Node loc) {
        if (foo instanceof FGenericFunction) {
            FGenericFunction gen = (FGenericFunction) foo;
            foo = inferAndInstantiateGenericFunction(args, gen, loc);
            // System.out.println("Generic invoke "+foo+"\n  On arguments "+args);
        }
        return foo.apply(args, loc, e);
    }

    /**
     * @param args
     * @param appliedThing
     * @param loc
     * @return
     * @throws ProgramError
     */
    public Simple_fcn inferAndInstantiateGenericFunction(List<FValue> args,
            FGenericFunction appliedThing, HasAt loc) throws ProgramError {
        FGenericFunction bar = (FGenericFunction) appliedThing;
        Option<List<StaticParam>> otparams = bar.getFnDefOrDecl()
                .getStaticParams();
        List<StaticParam> tparams = otparams.getVal();
        List<Param> params = bar.getFnDefOrDecl().getParams();
        // The types of the actual parameters ought to unify with the
        // types of the formal parameters.
        ABoundingMap<String, FType, TypeLatticeOps> abm = new ABoundingMap<String, FType, TypeLatticeOps>(
                TypeLatticeOps.V, com.sun.fortress.interpreter.useful.StringComparer.V);
        Iterator<Param> pit = params.iterator();
        Param p = null;
        Set<StaticParam> tp_set = new HashSet<StaticParam>(tparams);
        for (FValue a : args) {
            p = pit.hasNext() ? pit.next() : p;
            Option<TypeRef> t = p.getType();
            if (!t.isPresent())
                throw new ProgramError(loc,
                        "Parameter needs type for generic resolution");
            FType at = a.type();
            try {
                at.unify(e, tp_set, abm, t.getVal());
            } catch (ProgramError ex) {
                /* Give decent feedback when unification fails. */
                ex.setWithin(e);
                ex.setWhere(loc);
                throw ex;
            }
        }

        ArrayList<FType> tl = new ArrayList<FType>(tparams.size());
        for (StaticParam tp : tparams) {
            FType t = abm.get(tp.getName());
            if (t == null)
                t = BottomType.T;
            tl.add(t);
        }
        Simple_fcn sfcn = bar.make(tl, loc);
        return sfcn;
    }


}
