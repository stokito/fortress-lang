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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.SymbolicInstantiatedType;
import com.sun.fortress.interpreter.evaluator.types.SymbolicNat;
import com.sun.fortress.interpreter.evaluator.values.GenericFunctionOrMethod.GenericComparer;
import com.sun.fortress.interpreter.nodes.Applicable;
import com.sun.fortress.interpreter.nodes.DimensionParam;
import com.sun.fortress.interpreter.nodes.FnName;
import com.sun.fortress.interpreter.nodes.IntParam;
import com.sun.fortress.interpreter.nodes.NatParam;
import com.sun.fortress.interpreter.nodes.OperatorParam;
import com.sun.fortress.interpreter.nodes.Option;
import com.sun.fortress.interpreter.nodes.SimpleTypeParam;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.TypeAlias;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.nodes.WhereClause;
import com.sun.fortress.interpreter.nodes.WhereExtends;
import com.sun.fortress.interpreter.useful.BASet;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.NI;


abstract public class GenericFunctionOrMethodSet<What extends GenericFunctionOrMethod> extends Fcn {

   // Extending Fcn is semi-bogus.  Generic methods have a name and an environment.

    public GenericFunctionOrMethodSet(FnName name, BetterEnv within, Comparator<What> comparer) {
        this(name, within, new BASet<What>(comparer));
    }

    private GenericFunctionOrMethodSet(FnName name, BetterEnv within, Set<What> gs) {
        super(within);
        this.name = name;
        gmset = gs;
    }

    public String toString() {
        String res = name.name() + " with instances:";
        for (What w : gmset) {
            res += "\n    " + w.toString();
        }
        return res;
    }

    public FValue applyMethod(List<FValue> args, FObject selfValue, HasAt loc) {
        return NI.nyi();
    }

    FnName name;

    @Override
    public FValue applyInner(List<FValue> args, HasAt loc, BetterEnv envForInference) {
        return NI.na();
    }

    @Override
    public FnName getFnName() {
        return name;
    }

    abstract public boolean isMethod();

    public GenericFunctionOrMethodSet addOverload(What cl) {
        if (gmset.size() > 0) {
            // Check for consistent parameter lists.
            What first = gmset.iterator().next();
            int x = What.genComparer.compare(cl, first);
            if (x != 0) {
                throw new ProgramError(cl.getDef(), "Overloaded generic method with differing type parameters, " +
                        first + " vs " + cl);
            }
        }
        gmset.add(cl);
        return this;
    }

    public GenericFunctionOrMethodSet addOverloads(GenericFunctionOrMethodSet<What> cls) {
        for (What cl : cls.gmset) {
            addOverload(cl);
        }
        return this;
    }

    public Set<What> getMethods() { return gmset; }

    Set<What> gmset;

}
