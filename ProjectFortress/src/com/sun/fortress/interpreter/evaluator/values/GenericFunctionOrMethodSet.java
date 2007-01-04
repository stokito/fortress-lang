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
import com.sun.fortress.interpreter.evaluator.types.SymbolicType;
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
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.NI;


abstract public class GenericFunctionOrMethodSet<What extends GenericFunctionOrMethod> extends Fcn {

   // Extending Fcn is semi-bogus.  Generic methods have a name and an environment.

    public GenericFunctionOrMethodSet(FnName name, BetterEnv within) {
        this(name, within, new HashSet<What>());
    }


    public GenericFunctionOrMethodSet(FnName name, BetterEnv within, Set<What> gs) {
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

    public void addOverload(What cl) {
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
    }

    public void addOverloads(GenericFunctionOrMethodSet<What> cls) {
        for (What cl : cls.gmset) {
            addOverload(cl);
        }
    }

    public Set<What> getMethods() { return gmset; }

    /**
     * Given a (generic) applicable, an environment for type
     * evaluation. and a location to which problems can be
     * attached, create the ordered list of symbolic instantiation
     * arguments consistent with the type parameters and where
     * clauses.  The symbolic types so created will be tied to
     * a newly generated environment so that they do not contaminate
     * any "real" environments.
     *
     * The instantiated generics can be used to allow checks on
     * overloading.
     *
     * @param bte
     * @param ap
     * @return
     * @throws Error
     */
    static public List<FType> createSymbolicInstantiation(BetterEnv bte, Applicable ap, HasAt location) throws Error {
        List<StaticParam> tpl = ap.getStaticParams().getVal();
        List<WhereClause> wcl = ap.getWhere();

        // The (possibly multiple and interrelated) symbolic
        // types must be created in an environment, but we don't
        // want to contaminate a "real" environment with these names.
        // We also don't want "crosstalk" between type parameters
        // to different overloaded things.
        BetterEnv ge = new BetterEnv(bte, location);

        // Note that we must arrange for the symbolic things
        // to meet the constraints required by the object.
        List<FType> instantiationTypes = createSymbolicInstantiation(tpl, wcl, ge);
        return instantiationTypes;
    }

    /**
     * @param tpl
     * @param wcl
     * @param ge The generic environment that is being populated by this instantiation.
     * @throws Error
     */
    static private List<FType> createSymbolicInstantiation(List<StaticParam> tpl, List<WhereClause> wcl, BetterEnv ge) throws Error {
        ArrayList<FType> a = new ArrayList<FType>();
        for (StaticParam tp: tpl) {
            if (tp instanceof DimensionParam) {
                DimensionParam dp = (DimensionParam) tp;
                NI.nyi();
            } else if (tp instanceof NatParam) {
                NatParam np = (NatParam) tp;
                String np_name = np.getName();
                SymbolicNat sn = new SymbolicNat(np_name);
                ge.putType(np_name, sn);
                a.add(sn);
            } else if (tp instanceof IntParam) {
                IntParam np = (IntParam) tp;
                String np_name = np.getName();
                SymbolicNat sn = new SymbolicNat(np_name);
                ge.putType(np_name, sn);
                a.add(sn);
            } else if (tp instanceof OperatorParam) {
                OperatorParam op = (OperatorParam) tp;
                NI.nyi();
            } else if (tp instanceof SimpleTypeParam) {
                SimpleTypeParam stp = (SimpleTypeParam) tp;
                String stp_name = stp.getName();
                SymbolicInstantiatedType st = new SymbolicInstantiatedType(stp_name, ge);
                ge.putType(stp_name, st);
                a.add(st);
            } else {
                throw new InterpreterError("Unimplemented symbolic StaticParam " + tp);
            }
        }

        // Expect that where clauses will add names and constraints.9
        for (WhereClause wc : wcl) {
            if (wc instanceof TypeAlias) {
                TypeAlias ta = (TypeAlias) wc;
                NI.nyi("Where clauses - type alias");
            } else if (wc instanceof WhereExtends) {
                WhereExtends we = (WhereExtends) wc;
                String we_name = we.getName().getName();
                // List<TypeRef> we_supers = we.getSupers();
                if (ge.getTypeNull(we_name) == null) {
                    // Add name
                    SymbolicInstantiatedType st = new SymbolicInstantiatedType(we_name, ge);
                    ge.putType(we_name, st);
                }
            }
        }

        EvalType eval_type = new EvalType(ge);

        // Process constraints
        for (StaticParam tp: tpl) {
            if (tp instanceof DimensionParam) {
                DimensionParam dp = (DimensionParam) tp;
                NI.nyi();
            } else if (tp instanceof NatParam) {
                NatParam np = (NatParam) tp;
                String np_name = np.getName();

            } else if (tp instanceof IntParam) {
                IntParam np = (IntParam) tp;
                String np_name = np.getName();

            } else if (tp instanceof OperatorParam) {
                OperatorParam op = (OperatorParam) tp;
                NI.nyi();
            } else if (tp instanceof SimpleTypeParam) {
                SimpleTypeParam stp = (SimpleTypeParam) tp;
                String stp_name = stp.getName();
                SymbolicInstantiatedType st = (SymbolicInstantiatedType) ge.getType(stp_name);
                Option<List<TypeRef>> oext = stp.getExtends_();
                // pass null, no excludes here.
                // Note no need to replace environment, these
                // are precreated in a fresh environment.
                st.setExtendsAndExcludes(eval_type.getFTypeListFromOptionList(oext), null);
            } else {
                throw new InterpreterError("Unexpected StaticParam " + tp);
            }
        }

        // Expect that where clauses will add names and constraints.
        for (WhereClause wc : wcl) {
            if (wc instanceof TypeAlias) {
                NI.nyi("Where clauses - type alias");
                TypeAlias ta = (TypeAlias) wc;
            } else if (wc instanceof WhereExtends) {
                WhereExtends we = (WhereExtends) wc;
                String we_name = we.getName().getName();
                List<TypeRef> we_supers = we.getSupers();
                SymbolicInstantiatedType st = (SymbolicInstantiatedType) ge.getType(we_name);
                st.addExtends(eval_type.getFTypeListFromList(we_supers));
            }
        }

        return a;
    }

    Set<What> gmset;

}
