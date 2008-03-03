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

package com.sun.fortress.interpreter.evaluator.values;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.EvaluatorBase;
import com.sun.fortress.interpreter.evaluator.FortressError;
import com.sun.fortress.interpreter.evaluator.InstantiationLock;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObjectOrGeneric;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeNat;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.types.FTypeOverloadedArrow;
import com.sun.fortress.interpreter.evaluator.types.FTypeRest;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes_util.ErrorMsgMaker;
import com.sun.fortress.nodes.SimpleTypeParam;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.useful.BATreeEC;
import com.sun.fortress.useful.DebugletPrintStream;
import com.sun.fortress.useful.Factory1P;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Memo1P;
import com.sun.fortress.useful.Ordinal;
import com.sun.fortress.useful.Useful;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class  OverloadedFunction extends Fcn
    implements Factory1P<List<FType>, Fcn, HasAt>, HasFinishInitializing {

    private final boolean debug = false;
    private final boolean debugMatch = false;
    /**
     * Disables ALL consistency checking of overloaded functions.
     */
    private final boolean noCheck = false;

    protected volatile List<Overload> overloads = new ArrayList<Overload>();
    protected List<Overload> pendingOverloads = new ArrayList<Overload>();
    private Map<Overload, Overload> allOverloadsEver = new Hashtable<Overload, Overload>();

    protected volatile boolean finishedFirst = true; // an empty overload is consistent
    protected volatile boolean finishedSecond = true;
    protected SimpleName fnName;

    static final boolean DUMP_EXCLUSION = false;
    static int excl_skip = 65536*8+16384+8192+4096+512;

    public static void exclDump(Object... os) {
        if (DUMP_EXCLUSION && excl_skip <= 0) {
            for (Object o : os) {
                System.out.print(o);
            }
        }
    }

    public static void exclDumpln(Object... os) {
        if (DUMP_EXCLUSION && excl_skip <= 0) {
            for (Object o : os) {
                System.out.print(o);
            }
            System.out.println();
        } else {
            excl_skip--;
        }

    }

    BATreeEC<List<FValue>, List<FType>, SingleFcn> cache =
        new BATreeEC<List<FValue>, List<FType>, SingleFcn>(FValue.asTypesList);

    public String getString() {
        if (pendingOverloads.size() > 0) {
            return Useful.listInDelimiters("{\n\t",overloads,
                                           " PENDING BELOW", "\n\t")
              + Useful.listInDelimiters("\n*\t",pendingOverloads, "}", "\n*\t");
        }
        return Useful.listInDelimiters("{\n\t",overloads, "}", "\n\t");
    }

    public boolean getFinished() {
        return finishedFirst;
    }

    public boolean getFinishedSecond() {
        return finishedSecond;
    }

    public SimpleName getFnName() {
        return fnName;
    }

    public boolean hasSelfDotMethodInvocation() {
        return false;
    }

    public void finishInitializing() {
        finishInitializingFirstPart();
        finishInitializingSecondPart();
        return;
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

        Overload ol;
        for (int i = 0; i < pendingOverloads.size(); i++) {
            // Cannot be an iterator -- will get comodification exception
            // iteration to the growing end is perfectly ok.
            ol = pendingOverloads.get(i);
            SingleFcn sfcn = ol.getFn();

            String ps = ol.ps != null ? String.valueOf(ol.ps) + " " : "";

            if (sfcn instanceof Closure)  {
                Closure cl = (Closure) sfcn;
                if (! cl.getFinished())
                    cl.finishInitializing();

                if (debug) {
                    System.err.println("Overload " + ps  + cl);
                }

            } else if (sfcn instanceof Constructor)  {
                Constructor cl = (Constructor) sfcn;
                if (! cl.getFinished())
                    cl.finishInitializing();

                if (debug) {
                    System.err.println("Overload " + ps  + cl);
                }

            } else if (sfcn instanceof GenericConstructor)  {
                if (debug) {
                    System.err.println("Overload " + ps  + sfcn);
                }

            } else if (sfcn instanceof Dummy_fcn) {
                if (debug)
                    System.err.println("Overload primitive " + ps  + sfcn);

            } else if (sfcn instanceof FGenericFunction) {
                if (debug)
                    System.err.println("Overload generic " + ps  + sfcn);

            } else {
                bug(errorMsg("Expected a closure or primitive, instead got ",
                             sfcn));
             }

            if (ol.ps != null)
                ol.ps.close();
        }
        finishedFirst = true;
    }
    /**
     * The second part of finishing ensures that the overloadings
     * are consistent, and assigns a type to the value.
     */
    @SuppressWarnings("unchecked")
    public synchronized boolean finishInitializingSecondPart() {

        boolean change = false;

        if (finishedSecond)
            return change;

        while (true) {

        List<Overload> old_pendingOverloads = pendingOverloads;
        pendingOverloads = new ArrayList<Overload>();
        List<Overload> new_overloads = new ArrayList<Overload>();

        for (Overload overload : old_pendingOverloads) {
            // Duplicate detector
            if (allOverloadsEver.containsKey(overload)) {
                SingleFcn thisFn = overload.getFn();
                SingleFcn otherFn = allOverloadsEver.get(overload).getFn();
                // Debugging output
                if (debug) {
                    String ps = overload.ps != null ? String.valueOf(overload.ps) + " " : "";
                    System.err.println("Not putting " + ps + overload + "\n   equal to " + otherFn);
                }
                continue;
            } else {
                change = true;
                new_overloads.add(overload);
                allOverloadsEver.put(overload, overload);
            }
        }

        if (new_overloads.size() == 0) {
            bless();
            return change;
        }

        new_overloads.addAll(overloads);

        // Put shorter parameter lists first (it's a funny sort order).
        // TODO I don't understand what's "unchecked" about the next line.
        java.util.Collections.<Overload>sort(new_overloads);
        ArrayList<FType> ftalist = new ArrayList<FType> (new_overloads.size());

        for (int i = 0; i< new_overloads.size(); i++) {
            Overload o1 = new_overloads.get(i);
            ftalist.add(o1.getFn().type());

            if (!noCheck && !o1.guaranteedOK) {

            for (int j = i-1; j >= 0 ; j--) {

                Overload o2 = new_overloads.get(j);
                if (o2.guaranteedOK)
                    continue;
                SingleFcn f1 = o1.getFn();
                SingleFcn f2 = o2.getFn();

                if (genericFMAndInstance(f1, f2) || genericFMAndInstance(f2, f1))
                    continue;

                boolean samePartition = false;

                // Spot the case where two generics have equal static parameter lists
                if (f1 instanceof ClosureInstance && f2 instanceof ClosureInstance) {
                    GenericFunctionOrMethod g1 = ((ClosureInstance) f1).getGenerator();
                    GenericFunctionOrMethod g2 = ((ClosureInstance) f2).getGenerator();
                    if (FGenericFunction.genComparer.compare(g1, g2) == 0) {
                        samePartition = true;
                    }
                }
                // Yutch, we need to fix that type hierarchy
                if (f1 instanceof FunctionalMethodInstance && f2 instanceof FunctionalMethodInstance) {
                    GenericFunctionOrMethod g1 = ((FunctionalMethodInstance) f1).getGenerator();
                    GenericFunctionOrMethod g2 = ((FunctionalMethodInstance) f2).getGenerator();
                    if (FGenericFunction.genComparer.compare(g1, g2) == 0) {
                        samePartition = true;
                    }
                }

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
                int selfIndex = -1;

                /* This is a hack for dealing with cases like
                 *
                   trait Bar[\A,nat n\]
                       get():ZZ32 = n
                   end

                   object Baz[\A,nat n\]() extends Bar[\A,n\]
                   end

                   f[\A\](x:Bar[\A,17\]) = 20
                   g[\A\](x:Baz[\A,17\]) = 21
                   h[\A\](x:Bar[\A,17\]) = 22
                   h[\A\](x:Baz[\A,17\]) = 23



                 */
                boolean allObjInstance1 = true;
                boolean allObjInstance2 = true;

                exclDumpln("Checking exclusion of ",pl1," and ",pl2,":");
                for (int k = 0; k < min; k++) {
                    FType p1 = pl1.get(k);
                    FType p2 = k < l2 ? pl2.get(k) : pl2.get(l2-1);
                    exclDump(k,": ",p1," and ",p2,", ",p1.getExtends()," and ",p2.getExtends(),", ");

                    p1 = deRest(p1);
                    p2 = deRest(p2);

                    if (p1==p2  && !p1.isSymbolic() && !p2.isSymbolic()) {
                        exclDumpln("equal.");
                        continue;
                    }

                    allObjInstance1 &= p1 instanceof FTypeObject;
                    allObjInstance2 &= p2 instanceof FTypeObject;

                    unequal = true;

                    if (f1 instanceof HasSelfParameter &&
                            f2 instanceof HasSelfParameter &&
                            ((HasSelfParameter)f1).getSelfParameterIndex() == ((HasSelfParameter)f2).getSelfParameterIndex() &&
                            ((HasSelfParameter)f1).getSelfParameterIndex() == k) {
                            exclDumpln("self params.");
                            // ONLY set this when the self indices coincide -- otherwise, they obey the same rules.
                            selfIndex = k;
                            /*
                             * Somebody Else's Problem -- if self parameters are different,
                             * any problems will be flagged at the object level.
                             */
                            if (! p1.equals(p2))
                                distinct = true; // This seems wrong/unnecessary
                    }

                    if (p1.excludesOther(p2)) {
                        exclDumpln("distinct.");
                        distinct = true;
                    } else {

                        boolean local_unrelated = true;
                        // Check for subtype constraint.
                        boolean p1subp2 = p1.subtypeOf(p2);
                        boolean p2subp1 = p2.subtypeOf(p1);
                        if (p1subp2 && !p2subp1) {
                            p1better = k;
                            local_unrelated = false;
                            exclDumpln(" left better.");
                        } else if (p2subp1 && !p1subp2) {
                            p2better = k;
                            local_unrelated = false;
                            exclDumpln(" right better.");
                        } else if (selfIndex != k) {
                            if (p1.isSymbolic() )
                                sawSymbolic1 = true;

                            if (p2.isSymbolic() )
                                sawSymbolic2 = true;
                        }
                        if (local_unrelated && unrelated == -1) {
                            // Here we check for self parameters!
                            if (selfIndex != k) {
                                unrelated = k;
                                exclDumpln("Unrelated.");
                            }
                        }
                    }
                }

                distinct |= unequal && (allObjInstance1 || allObjInstance2);

                if (!distinct && (sawSymbolic1 || sawSymbolic2)) {
                    String explanation;
                    if (sawSymbolic1 && sawSymbolic2)
                        explanation = errorMsg("\nBecause ", o1, " and ", o2, " have parameters\n");
                    else if (sawSymbolic1)
                        explanation = errorMsg("\nBecause ", o1, " has a parameter\n");
                    else
                        explanation = errorMsg("\nBecause ", o2, " has a parameter\n");
                    explanation = explanation + "with generic type, at least one pair of parameters must have excluding types";
                    error(o1, o2, within, explanation);
                }

                if (!distinct && unrelated != -1) {
                    String s1 = parameterName(unrelated, o1);
                    String s2 = parameterName(unrelated, o2);

                    String explanation = errorMsg(Ordinal.ordinal(unrelated+1), " parameters ", s1, ":", pl1, " and ", s2, ":", pl2, " are unrelated (neither subtype, excludes, nor equal) and no excluding pair is present");
                    error(o1, o2, within, explanation);
                }

                if (!distinct && p1better >= 0 && p2better >= 0 &&  !meetExistsIn(o1, o2, new_overloads)) {
                    error(o1, o2, within,
                            errorMsg("Overloading of\n\t(first) ", o1, " and\n\t(second) ", o2, " fails because\n\t",
                            formatParameterComparison(p1better, o1, o2, "more"), " but\n\t",
                            formatParameterComparison(p2better, o1, o2, "less")));
                }
                if (!distinct && p1better < 0 && p2better < 0 && selfIndex < 0) {
                    String explanation = null;
                    if (l1 == l2 && rest1 == rest2) {
                        if (unequal)
                        explanation = errorMsg("Overloading of ", o1, " and ", o2,
                        " fails because their parameter lists have potentially overlapping (non-excluding) types");
                        else
                            explanation = errorMsg("Overloading of ", o1, " and ", o2,
                        " fails because their parameter lists have the same types");
                    } else
                        explanation = errorMsg("Overloading of ", o1, " and ", o2,
                        " fails because of ambiguity in overlapping rest (...) parameters");
                    error(o1, o2, within, explanation);
                }
            }
            }
        }
        FType ftoa = FTypeOverloadedArrow.make(ftalist);
        this.setFtypeUnconditionally(ftoa);
        //String ftoas = ftoa.toString();
        //System.err.println(ftoas);
        this.overloads = new_overloads;

        if (!finishedFirst) {
            // Come here if we generated MORE overloads as a side-effect.
            finishInitializingFirstPart();
        }

        }
    }

    private boolean genericFMAndInstance(SingleFcn f1, SingleFcn f2) {
        if (f1 instanceof GenericFunctionalMethod && f2 instanceof FunctionalMethod) {
            GenericFunctionalMethod gfm = (GenericFunctionalMethod) f1;
            FunctionalMethod fm = (FunctionalMethod) f2;
            FTraitOrObjectOrGeneric tgfm = gfm.getSelfParameterType();
            FTraitOrObjectOrGeneric tfm = fm.getSelfParameterType();
            return tgfm.getDecl() == tfm.getDecl();
        }
        return false;
    }

    private String formatParameterComparison(int i, Overload o1, Overload o2,
            String how) {
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
            s1 = np.getParameters().get(i).getName();
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
    public OverloadedFunction(SimpleName fnName, BetterEnv within) {
        super(within);
        this.fnName = fnName;
    }

    public OverloadedFunction(SimpleName fnName, Set<? extends Simple_fcn> ssf, BetterEnv within) {
        this(fnName, within);
        for (Simple_fcn sf : ssf) {
            addOverload(sf);
        }
        finishInitializingSecondPart();
    }

    public void addOverload(SingleFcn fn) {
//      if (finishedFirst && !fn.getFinished())
//          throw new IllegalStateException("Any functions added after finishedFirst must have types assigned.");
      addOverload(new Overload(fn, this));
  }

    public void addOverload(SingleFcn fn, boolean guaranteedOK) {
//      if (finishedFirst && !fn.getFinished())
//          throw new IllegalStateException("Any functions added after finishedFirst must have types assigned.");
      addOverload(new Overload(fn, this, guaranteedOK));
  }

    public void addOverloads(OverloadedFunction cls) {
        List<Overload> clso = cls.overloads;
        for (Overload cl : clso) {
            addOverload(cl);
        }
        clso = cls.pendingOverloads;
        try {
            for (Overload cl : clso) {

                addOverload(cl);
            }
        } catch (ConcurrentModificationException ex) {
            bug("Whoops, concurrent modification");
        }

    }

    /**
     * Add an overload to the list of overloads. Not Allowed after the
     * overloaded function has been (completely) finished.
     * 
     * @param overload
     */
    public void addOverload(Overload overload) {

         if (!finishedSecond) {
            // Reset finishedFirst -- new overloads can appear as side-effect
            // of finishing first overloads.
            finishedFirst = false;
            pendingOverloads.add(overload);
            // InstantiationLock.lastOverload = this;
            // InstantiationLock.lastOverloadThrowable = Useful.backtrace(0, 1000);
        } else {
            // InstantiationLock.L.lock();
            if (debug)
                System.err.println("Lock " + fnName.stringName());
            finishedFirst = false;
            finishedSecond = false;
            pendingOverloads.add(overload);
            // InstantiationLock.lastOverload = this;
            // InstantiationLock.lastOverloadThrowable = Useful.backtrace(0, 1000);
        }

        if (debug) {
            DebugletPrintStream ps = DebugletPrintStream.make("OVERLOADS");
            overload.ps = ps;
            System.err.println("add " + ps + " " + overload);
            ps.backtrace().flush();
        }

    }

    @Override
    public FValue applyInner(List<FValue> args, HasAt loc, BetterEnv envForInference) {

        SingleFcn best_f = cache.get(args);

        if (best_f == null) {
//            if (getFnName().toString().equals("seq")) {
//                System.err.println("Resolving seq from " + this );
//            }

            List<Overload>  someOverloads = overloads;
            // System.err.println("Overloaded " + fnName + " cache.size = " + cache.size());

            int best = bestMatchIndex(args, loc, envForInference, someOverloads);

            best_f = someOverloads.get(best).getFn();
            if (debugMatch)
                System.err.println("Choosing " + best_f + " for args " + args);
            cache.syncPut(args, best_f);
        }

        return best_f.apply(args, loc, envForInference);
    }

     /**
     * Returns index of best match for args among the overloaded functions.
     * @throws Error
     */
     public int bestMatchIndex(List<FValue> args, HasAt loc, BetterEnv envForInference, List<Overload> someOverloads) throws Error {

        if (!finishedSecond && InstantiationLock.L.isHeldByCurrentThread())
            bug(loc, "Cannot call before 'setFinished()'");

        int best = bestMatchIndexInternal(args, loc, envForInference,
                someOverloads);
        if (best == -1) {
            // TODO add checks for COERCE, right here.
            // Replay the test for debugging
            best = bestMatchIndexInternal(args, loc, envForInference,
                    someOverloads);
            error(loc,errorMsg("Failed to find any matching overload, args = ",
                    Useful.listInParens(args), ", overload = ", this));
        }
        return best;
    }

    private int bestMatchIndexInternal(List<FValue> args, HasAt loc,
            BetterEnv envForInference, List<Overload> someOverloads) {
        int best = -1;
        SingleFcn best_sfn = null;

        for (int i = 0; i < someOverloads.size(); i++) {
            Overload o = someOverloads.get(i);
            if (o.getParams() == null) {
                bug(loc, errorMsg("Unfinished overloaded function ", this));
            }
            SingleFcn sfn = o.getFn();

            List<FValue> oargs = args;

            if (sfn instanceof GenericFunctionOrMethod) {
                GenericFunctionOrMethod gsfn = (GenericFunctionOrMethod) sfn;
                try {
                    sfn = EvaluatorBase.inferAndInstantiateGenericFunction(oargs, gsfn, loc, envForInference);
                    if (debugMatch)
                        System.err.println("Inferred from " + gsfn + " to " + sfn);
                } catch (FortressError pe) {
                    continue; // No match, means no dice.
                }

            } else if (debugMatch) {
                System.err.println("Trying w/o instantiation: "+ sfn);
            }

            oargs = sfn.fixupArgCount(args);

            // Non-generic, old code.
            if (oargs != null &&
                argsMatchTypes(oargs,  sfn.getDomain()) &&
                        (best == -1 ||
                         best == i ||
                         FTypeTuple.moreSpecificThan(sfn.getDomain(), best_sfn.getDomain()))) {
                best = i;
                best_sfn = sfn;
            }

        }
        return best;
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

    public static boolean argsMatchTypes(List<FValue> args, List<FType> l) {
        for (int j = 0; j < args.size(); j++) {
            FValue a = args.get(j);
            FType t = Useful.clampedGet(l,j).deRest();
            try {
                if (! t.typeMatch(a))
                    return false;
            } catch (FortressError e) {
                return false;
            }
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
        if (finishedSecond)
            return;
        finishedSecond = true;
        finishedFirst = true;
        if (debug)
            System.err.println("Unlock " + fnName.stringName());
    }

    /* This code gives overloaded functions the interface of a set of generic
     * functions.
     *
     */

    private class Factory implements Factory1P<List<FType>, Fcn, HasAt> {

        public Fcn make(List<FType> args, HasAt location) {
            // TODO finish this.

            SingleFcn   f = null;
            OverloadedFunction of = null;

            for (Overload ol : getOverloads()) {
                SingleFcn sfcn = ol.getFn();
                if (sfcn instanceof GenericFunctionOrMethod) {
                    GenericFunctionOrMethod gf = (GenericFunctionOrMethod) sfcn;

                    // Check that args matches the static parameters of the generic function
                    // TODO -- can a generic instantiation result in an unfulfillable overloading?

                    if (compatible(args, gf.getStaticParams())) {

                        SingleFcn tf = gf.typeApply(location, args);
                        if (f == null) {
                            f = tf;
                        } else if (of == null) {
                            of = new OverloadedFunction(getFnName(),
                                    getWithin());
                            of.addOverload(f);
                            of.addOverload(tf);
                        } else {
                            of.addOverload(tf);
                        }
                    }

                }
            }
           if (of != null) {
               of.finishInitializing();
               return of;
           }
           if (f != null) return f;
           return error(location,
                   errorMsg("No matches for instantiation of overloaded ",
                   OverloadedFunction.this, " with ", Useful.listInParens(args)));
        }
    }

     Memo1P<List<FType>, Fcn, HasAt> memo = new Memo1P<List<FType>, Fcn, HasAt>(new Factory());

    public Fcn make(List<FType> l, HasAt location) {
        return memo.make(l, location);
    }


    public static boolean compatible(List<FType> args, List<StaticParam> val) {
       if (args.size() != val.size()) return false;
       for (int i = 0; i < args.size(); i++) {
           // TODO need to make this check more comprehensive and detailed.
           FType a = args.get(i);
           StaticParam p = val.get(i);
           if (p instanceof SimpleTypeParam) {
               if (a instanceof FTypeNat) return false;
           }
       }
       return true;
    }

    public Fcn typeApply(List<StaticArg> args, BetterEnv e, HasAt location) {
        EvalType et = new EvalType(e);
        // TODO Can combine these two functions if we enhance the memo and factory
        // to pass two parameters instead of one.
        ArrayList<FType> argValues = et.forStaticArgList(args);

        return typeApply(e, location, argValues);
    }

    /**
     * Same as typeApply, but with the types evaluated already.
     *
     * @param args
     * @param e
     * @param within
     * @param argValues
     * @return
     * @throws ProgramError
     */
    Fcn typeApply(BetterEnv e, HasAt location, List<FType> argValues) throws ProgramError {
        // Need to filter for matching generics in the overloaded type.
        return make(argValues, location);
    }


}
