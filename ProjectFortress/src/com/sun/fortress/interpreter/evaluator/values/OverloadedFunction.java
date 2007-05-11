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
import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeOverloadedArrow;
import com.sun.fortress.interpreter.evaluator.types.FTypeRest;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.interpreter.nodes.FnName;
import com.sun.fortress.interpreter.useful.BATreeEC;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.Ordinal;
import com.sun.fortress.interpreter.useful.Useful;


public class OverloadedFunction extends Fcn {

    protected List<Overload> overloads = new ArrayList<Overload>();
    protected boolean finishedFirst = false;
    protected boolean finishedSecond = false;
    protected FnName fnName;

    BATreeEC<List<FValue>, List<FType>, SingleFcn> cache =
        new BATreeEC<List<FValue>, List<FType>, SingleFcn>(FValue.asTypesList);

    public String getString() {

        return Useful.listInCurlies(overloads);
    }

    public boolean getFinished() {
        return finishedFirst;
    }

    public boolean getFinishedSecond() {
        return finishedSecond;
    }

    public FnName getFnName() {
        return fnName;
    }

    public boolean hasSelfDotMethodInvocation() {
        return false;
    }

    public void finishInitializing() {
        finishInitializingFirstPart();
        finishInitializingSecondPart();
    }

    /**
     * The first part of finishing makes sure that all the
     * Closures in the overload have their types assigned.
     * It is not possible to Determine the goodness or badness
     * of an overloading until actual types are known.
     */
    public void finishInitializingFirstPart() {
        if (finishedFirst)
            return;

        for (Overload ol : getOverloads()) {
            SingleFcn sfcn = ol.getFn();
            if (sfcn instanceof Closure)  {
                Closure cl = (Closure) sfcn;
                if (! cl.getFinished())
                    cl.finishInitializing();

            } else if (sfcn instanceof Dummy_fcn) {
                // Primitives are all ready

            } else {
                throw new InterpreterError(
                            "Expected a closure or primitive, instead got "
                                    + sfcn);
             }
        }
        finishedFirst = true;
    }
    /**
     * The second part of finishing ensures that the overloadings
     * are consistent, and assigns a type to the value.
     */
    @SuppressWarnings("unchecked")
    public void finishInitializingSecondPart() {

        if (finishedSecond)
            return;

        // Put shorter parameter lists first (it's a funny sort order).
        // TODO I don't understand what's "unchecked" about the next line.
        java.util.Collections.<Overload>sort(overloads);
        ArrayList<FType> ftalist = new ArrayList<FType> (overloads.size());

        for (int i = 0; i< overloads.size(); i++) {
            ftalist.add(overloads.get(i).getFn().type());

            for (int j = i-1; j >= 0 ; j--) {
                Overload o1 = overloads.get(i);
                Overload o2 = overloads.get(j);

                List<FType> pl1 = o1.getParams();
                List<FType> pl2 = o2.getParams();

                int l1 = pl1.size();
                boolean rest1 = (l1 > 0 && pl1.get(l1-1) instanceof FTypeRest);

                int l2 = pl2.size();
                boolean rest2 = (l2 > 0 && pl2.get(l2-1) instanceof FTypeRest);

                // by construction, l1 is bigger.
                // (see sort order above)
                // possibilities
                // rest1 l2 can be no smaller than l1-1; iterate to l2
                // rest2 test out to l1
                // both  test out to l1
                // neither = required

                int min;
                if (rest2) {
                    // both, rest2
                    min = l1;
                } else if (rest1) {
                    // rest1
                    if (l2 < l1-1) continue;
                    min = l2;
                } else {
                    // neither
                    if (l1 != l2)
                        continue;
                    min = l1;
                }

                int p1better = -1; // set to index where p1 is subtype
                int p2better = -1; // set to index where p2 is subtype

                boolean distinct = false; // known to exclude
                int unrelated = -1; // neither subtype nor exclude nor identical
                boolean unequal = false;
                boolean sawSymbolic1 = false;
                boolean sawSymbolic2 = false;

                for (int k = 0; k < min; k++) {
                    FType p1 = pl1.get(k);
                    FType p2 = k < l2 ? pl2.get(k) : pl2.get(l2-1);

                    p1 = deRest(p1);
                    p2 = deRest(p2);

                    if (p1.equals(p2)) continue;

                    if (p1.isSymbolic() )
                        sawSymbolic1 = true;

                    if (p2.isSymbolic() )
                        sawSymbolic2 = true;

                    unequal = true;

                    if (p1.excludesOther(p2)) {
                        distinct = true;
                    } else {
                        boolean local_unrelated = true;
                        boolean p1subp2 = p1.subtypeOf(p2);
                        boolean p2subp1 = p2.subtypeOf(p1);
                        if (p1subp2 && !p2subp1) {
                            p1better = k;
                            local_unrelated = false;
                         }
                        if (p2subp1 && !p1subp2) {
                            p2better = k;
                            local_unrelated = false;
                        }
                        if (local_unrelated && unrelated == -1)
                            unrelated = k;
                    }
                }

                if (!distinct && (sawSymbolic1 || sawSymbolic2)) {
                    String explanation;
                    if (sawSymbolic1 && sawSymbolic2)
                        explanation = "\nBecause " + o1 + " and " + o2 + " have parameters\n";
                    else if (sawSymbolic1)
                        explanation = "\nBecause " + o1 + " has a parameter\n";
                    else
                        explanation = "\nBecause " + o2 + " has a parameter\n";
                    explanation = explanation + "with generic type, at least one pair of parameters must have excluding types";
                    throw new ProgramError(o1, o2, within, explanation);
                }

                if (!distinct && unrelated != -1) {
                    String s1 = parameterName(unrelated, o1);
                    String s2 = parameterName(unrelated, o2);

                    String explanation = Ordinal.ordinal(unrelated+1) + " parameters " +s1 + ":" + pl1 + " and " + s2 + ":" + pl2 + " are unrelated (neither subtype, excludes, nor equal) and no excluding pair is present";
                    throw new ProgramError(o1, o2, within, explanation);
                }

                if (!distinct && p1better >= 0 && p2better >= 0 &&  !meetExistsIn(o1, o2, overloads)) {
                    throw new ProgramError(o1, o2, within,
                            "Overloading of\n\t(first) " + o1 + " and\n\t(second) " + o2 + " fails because\n\t" +
                            formatParameterComparison(p1better, o1, o2, "more") + " but\n\t" +
                            formatParameterComparison(p2better, o1, o2, "less"));
                }
                if (!distinct && p1better < 0 && p2better < 0 ) {
                    String explanation = null;
                    if (l1 == l2 && rest1 == rest2) {
                        if (unequal)
                        explanation = "Overloading of " + o1 + " and " + o2 +
                        " fails because their parameter lists have potentially overlapping (non-excluding) types";
                        else
                            explanation = "Overloading of " + o1 + " and " + o2 +
                        " fails because their parameter lists have the same types";
                    } else
                        explanation = "Overloading of " + o1 + " and " + o2 +
                        " fails because of ambiguity in overlapping rest (...) parameters";
                    throw new ProgramError(o1, o2, within, explanation);
                }
            }
        }
        this.setFtypeUnconditionally(FTypeOverloadedArrow.make(ftalist));
        finishedSecond = true;
    }

    private String formatParameterComparison(int i, Overload o1, Overload o2, String how) {
        String s1 = parameterName(i, o1);
        String s2 = parameterName(i, o2);
        return "(first) " + s1 + " is " + how + " specific than (second) " + s2;
    }

    /**
     * @param i
     * @param f1
     */
    private String parameterName(int i, Overload o) {
        SingleFcn f1 = o.getFn();
        String s1;
        if (f1 instanceof NonPrimitive) {
            NonPrimitive np = (NonPrimitive) f1;
            s1 = np.getParams().get(i).getName();
        } else {
            s1 = "parameter " + (i+1);
        }
        return s1;
    }

    /**
     * Computes a conservative meet of o1 and o2, and checks for its existence.
     *
     * @param o1
     * @param o2
     * @param overloads2
     * @return
     */
    private boolean meetExistsIn(Overload o1, Overload o2, List<Overload> overloads2) {
        List<FType> pl1 = o1.getParams();
        List<FType> pl2 = o2.getParams();

        Set<List<FType>> meet_set = FTypeTuple.meet(pl1, pl2);

        if (meet_set.size() != 1) return false;

        List<FType> meet = meet_set.iterator().next();

        for (Overload o : overloads2) {
            if (meet.equals(o.getParams()))
                return true;
        }
        // TODO finish this.

        return false;
    }

    private FType deRest(FType p1) {
        if (p1 instanceof FTypeRest) p1 = ((FTypeRest)p1).getType();
        return p1;
    }

    /**
     * Needs an environment to construct its supertype,
     * but otherwise it is never examined.
     *
     * @param within
     */
    public OverloadedFunction(FnName fnName, BetterEnv within) {
        super(within);
        this.fnName = fnName;
    }

    public OverloadedFunction(FnName fnName, Set<? extends Simple_fcn> ssf, BetterEnv within) {
        this(fnName, within);
        for (Simple_fcn sf : ssf) {
            addOverload(sf);
        }
        finishInitializingSecondPart();
    }

    public void addOverload(SingleFcn fn) {
//        if (finishedFirst && !fn.getFinished())
//            throw new IllegalStateException("Any functions added after finishedFirst must have types assigned.");
        addOverload(new Overload(fn));
    }
    
    public void addOverloads(OverloadedFunction cls) {
        for (Overload cl : cls.overloads) {
            addOverload(cl);
        }
    }

    /**
     * Add an overload to the list of overloads.
     * Not Allowed after the overloaded function has been (completely)
     * finished.
     *
     * @param overload
     */
    public void addOverload(Overload overload) {
//        if (finishedSecond)
//            throw new IllegalStateException("Cannot add overloads after overloaded function is complete");
        overloads.add(overload);
        finishedFirst = false;
        finishedSecond = false;
    }

    @Override
    public FValue applyInner(List<FValue> args, HasAt loc, BetterEnv envForInference) {
       
        SingleFcn best_f = cache.get(args);
        
        if (best_f == null) {
            int best = bestMatchIndex(args);

            if (best == -1) {
                // TODO add checks for COERCE, right here.
                throw new ProgramError(loc,  within,
                             "Failed to find matching overload, args = " +
                             Useful.listInParens(args) + ", overload = " + this);
            }

            best_f = overloads.get(best).getFn();
            cache.syncPut(args, best_f);
        }
        
        return best_f.apply(args, loc, envForInference);
    }

     /**
     * Returns index of best match for args among the overloaded functions.
     *
     * @param args
     * @return
     * @throws Error
     */
    public int bestMatchIndex(List<FValue> args) throws Error {
        if (!finishedSecond) throw new InterpreterError("Cannot call before 'setFinished()'");
        int best = -1;
        for (int i = 0; i < overloads.size(); i++) {
            Overload o = overloads.get(i);
            if (o.getParams() == null) {
                throw new InterpreterError("Unfinished overloaded function " + this);
            }
            List<FValue> oargs = o.getFn().fixupArgCount(args);
            if (oargs != null &&
                argsMatchTypes(oargs,  o) &&
                (best == -1 || moreSpecificThan(i, best))) {
                    best = i;
            }
        }
        if (best == -1) {
            // TODO add checks for COERCE, right here.
            throw new ProgramError("Failed to find matching overload, args = " +
                                   Useful.listInParens(args) + ", overload = " + this);
        }
        return best;
    }

    /**
     * Returns true if overload.get(i).params is more
     * specific than overloads.get(best).params.
     *
     * @param i
     * @param best
     * @return
     */
    private boolean moreSpecificThan(int i, int i_best) {
        if (i == i_best) return false;
        List<FType> candidate = overloads.get(i).getParams();
        List<FType> current = overloads.get(i_best).getParams();

        // TODO This is probably over-engineered; in theory the
        // overloading builder will include a consistency checker
        // to make the guarded-against case be impossible.
        // On the other hand, "trust, but verify".

        return FType.moreSpecificThan(candidate, current);
    }

     /**
     * @param args
     * @param args_len
     * @param i
     * @param o
     * @return
     */
    private boolean argsMatchTypes(List<FValue> args, Overload o) {
        List<FType> l = o.getParams();
        return argsMatchTypes(args, l);
    }

    /**
     * @param args
     * @param l
     * @return
     */
    public static boolean argsMatchTypes(List<FValue> args, List<FType> l) {
        for (int j = 0; j < args.size(); j++) {
            FValue a = args.get(j);
            FType t = Useful.clampedGet(l,j).deRest();
            if (! t.typeMatch(a))
                return false;
        }
        return true;
    }

    /**
     * @return Returns the overloads.
     */
    public List<Overload> getOverloads() {
        return overloads;
    }

    /**
     * To be used for those overloaded functions that are
     * "correct by construction" and do not require the
     * very exciting overload consistency test.
     */
    public void bless() {
        finishedSecond = true;
        finishedFirst = true;

    }

}
