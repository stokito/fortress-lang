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
/*
 * Created on May 1, 2007
 *
 */
package com.sun.fortress.interpreter.evaluator.values;

import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.FortressException;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.interpreter.evaluator.types.SymbolicInstantiatedType;
import com.sun.fortress.interpreter.evaluator.types.SymbolicBool;
import com.sun.fortress.interpreter.evaluator.types.SymbolicNat;
import com.sun.fortress.interpreter.evaluator.types.SymbolicOprType;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.DimParam;
import com.sun.fortress.nodes.BoolParam;
import com.sun.fortress.nodes.IntParam;
import com.sun.fortress.nodes.NatParam;
import com.sun.fortress.nodes.OpParam;
import com.sun.fortress.nodes.TypeParam;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TypeAlias;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes.WhereConstraint;
import com.sun.fortress.nodes.WhereExtends;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Hasher;
import com.sun.fortress.useful.MagicNumbers;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Useful;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;
import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;

public abstract class SingleFcn extends Fcn implements HasAt {

    public SingleFcn(BetterEnv within) {
        super(within);
    }

    abstract public String  at();
    abstract public List<FType> getDomain();
    abstract public FType getRange();
    public boolean isOverride() { return false; }

    /**
     * For now, prefer to unwrap tuples because that avoid creating
     * new memo entries for tuple types.
     * @return
     */
    public List<FType> getNormalizedDomain() {
        List<FType> d = getDomain();
        if (d.size() == 1) {
            FType t = d.get(0);
            if (t  instanceof FTypeTuple) {
              d = ((FTypeTuple) t).getTypes();
            }
        }
        return d;

    }

    /**
     * This is just like getNormalizedDomain, except that the method version
     * of functional methods appends a "nat" Type indicating the position of
     * the self parameter.
     * @return
     */
    public List<FType> getNormalizedDomainForTables() {
        return getNormalizedDomain();
    }

    public List<FValue> fixupArgCount(List<FValue> args) {
        System.out.println("Naive fixupArgCount "+this+
                           "("+this.getClass()+")"+
                           " of "+Useful.listInParens(args));
        int dsz = getDomain().size();
        if (args.size() == dsz) return args;
        return null;
    }

     // NOTE: I believe it is ok for functions to use object identity for
    // equals and hashCode().

    static class SignatureEquivalence extends Hasher<SingleFcn> {
        @Override
        public long hash(SingleFcn x) {
            long a = (long) x.asMethodName().hashCode() * MagicNumbers.s;
            long b =  (long) x.getNormalizedDomainForTables().hashCode() * MagicNumbers.l;
            // System.err.println("Hash of " + x + " yields " + a + " and " + b);

            return a + b;
        }

        @Override
        public boolean equiv(SingleFcn x, SingleFcn y) {
            List<FType> dx = x.getNormalizedDomainForTables();
            List<FType> dy = y.getNormalizedDomainForTables();
            if (dx.size() != dy.size())
                return false;
            if (! x.asMethodName().equals(y.asMethodName()))
                return false;
            for (int i = 0; i < dx.size(); i++) {
                if (! dx.get(i).equals(dy.get(i)))
                    return false;
            }
            return true;
        }

    }


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
     * @throws Error
     */
    static public List<FType> createSymbolicInstantiation(BetterEnv bte, Applicable ap, HasAt location) throws Error {
        List<StaticParam> tpl = ap.getStaticParams();
        WhereClause wcl = ap.getWhere();

        // The (possibly multiple and interrelated) symbolic
        // types must be created in an environment, but we don't
        // want to contaminate a "real" environment with these names.
        // We also don't want "crosstalk" between type parameters
        // to different overloaded things.
        BetterEnv ge = bte.extendAt(location);

        // Note that we must arrange for the symbolic things
        // to meet the constraints required by the object.
        List<FType> instantiationTypes;
        try {
            instantiationTypes = createSymbolicInstantiation(tpl, wcl, ge);
        } catch (FortressException e) {
            e.setContext(location,ge);
            throw e;
        }
        return instantiationTypes;
    }

    static public List<FType> createSymbolicInstantiation(BetterEnv bte, List<StaticParam> tpl, WhereClause wcl, HasAt location) throws Error {
        return createSymbolicInstantiation(tpl, wcl, bte.extendAt(location));
    }
    /**
     * @param tpl
     * @param wcl
     * @param ge The generic environment that is being populated by this instantiation.
     * @throws Error
     */
    static private List<FType> createSymbolicInstantiation(List<StaticParam> tpl, WhereClause wcl, BetterEnv ge) throws Error {
        ArrayList<FType> a = new ArrayList<FType>();
        for (StaticParam tp: tpl) {
            String name = NodeUtil.getName(tp);
            FType t;
            if (tp instanceof TypeParam) {
                t = new SymbolicInstantiatedType(name, ge, tp);
            } else if (tp instanceof NatParam || tp instanceof IntParam) {
                t = new SymbolicNat(name);
            } else if (tp instanceof BoolParam) {
                t = new SymbolicBool(name);
            } else if (tp instanceof OpParam) {
                OpParam op = (OpParam) tp;
                t = new SymbolicOprType(name, ge, op);
            } else {
                return bug(tp, errorMsg("Unimplemented symbolic StaticParam ", tp));
            }
            ge.putType(name, t);
            a.add(t);
        }

        // Expect that where clauses will add names and constraints.9
        for (WhereConstraint wc : wcl.getConstraints()) {
            if (wc instanceof TypeAlias) {
                TypeAlias ta = (TypeAlias) wc;
                NI.nyi("Where clauses - type alias");
            } else if (wc instanceof WhereExtends) {
                WhereExtends we = (WhereExtends) wc;
                String we_name = we.getName().getText();
                // List<Type> we_supers = we.getSupers();
                if (ge.getTypeNull(we_name) == null) {
                    // Add name
                    SymbolicInstantiatedType st = new SymbolicInstantiatedType(we_name, ge, we);
                    ge.putType(we_name, st);
                }
            }
        }

        EvalType eval_type = new EvalType(ge);

        // Process constraints
        for (StaticParam tp: tpl) {
            String name = NodeUtil.getName(tp);
            if (tp instanceof TypeParam) {
                TypeParam stp = (TypeParam) tp;
                String stp_name = NodeUtil.getName(stp);
                SymbolicInstantiatedType st = (SymbolicInstantiatedType) ge.getType(stp_name);
                List<BaseType> oext = stp.getExtendsClause();
                // pass null, no excludes here.
                // Note no need to replace environment, these
                // are precreated in a fresh environment.
                st.setExtendsAndExcludes(eval_type.getFTypeListFromList(oext), null);
            } else if (tp instanceof NatParam || tp instanceof IntParam ||
                       tp instanceof OpParam || tp instanceof BoolParam) {
                // No constraint handling right now
            } else {
                return bug(tp, errorMsg("Unexpected StaticParam ", tp));
            }
        }

        // Expect that where clauses will add names and constraints.
        for (WhereConstraint wc : wcl.getConstraints()) {
            if (wc instanceof TypeAlias) {
                NI.nyi("Where clauses - type alias");
                TypeAlias ta = (TypeAlias) wc;
            } else if (wc instanceof WhereExtends) {
                WhereExtends we = (WhereExtends) wc;
                String we_name = we.getName().getText();
                List<BaseType> we_supers = we.getSupers();
                SymbolicInstantiatedType st = (SymbolicInstantiatedType) ge.getType(we_name);
                st.addExtends(eval_type.getFTypeListFromList(we_supers));
            }
        }

        return a;
    }

    public static Hasher<SingleFcn> signatureEquivalence = new SignatureEquivalence();

    static class NameEquivalence extends Hasher<SingleFcn> {
        @Override
        public long hash(SingleFcn x) {
            long a = (long) x.getFnName().hashCode() * MagicNumbers.N;
             return a;
        }

        @Override
        public boolean equiv(SingleFcn x, SingleFcn y) {
            return x.getFnName().equals(y.getFnName());
        }

    }

    public static Hasher<SingleFcn> nameEquivalence = new NameEquivalence();


}
