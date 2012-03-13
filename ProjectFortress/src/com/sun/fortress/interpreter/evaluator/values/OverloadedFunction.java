/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.exceptions.FortressException;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.exceptions.ProgramError;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.EvaluatorBase;
import com.sun.fortress.interpreter.evaluator.InstantiationLock;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.*;

import java.io.IOException;
import java.util.*;

public class OverloadedFunction extends Fcn implements Factory1P<List<FType>, Fcn, HasAt> {

    private final static boolean debug = false;
    private final static boolean debugMatch = false;
    /**
     * Disables ALL consistency checking of overloaded functions.
     */
    private final static boolean noCheck = false;

    protected volatile List<Overload> overloads = new ArrayList<Overload>();
    protected List<Overload> pendingOverloads = new ArrayList<Overload>();
    private Map<Overload, Overload> allOverloadsEver = new Hashtable<Overload, Overload>();
    private volatile boolean needsInference;

    protected volatile boolean finishedFirst = true; // an empty overload is consistent
    protected volatile boolean finishedSecond = true;
    protected IdOrOpOrAnonymousName fnName;

    static final boolean DUMP_EXCLUSION = false;
    static int excl_skip = 100000;

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

    public static void exclDumpSkip() {
        if (DUMP_EXCLUSION && excl_skip > 0) {
            System.out.print("excl_skip = " + excl_skip + "; ");
        }
    }

    BATreeEC<List<FValue>, List<FType>, SingleFcn> cache =
            new BATreeEC<List<FValue>, List<FType>, SingleFcn>(FValue.asTypesList);

    @Override
    public boolean needsInference() {
        return needsInference;
    }

    public String getString() {
        if (pendingOverloads.size() > 0) {
            return Useful.listInDelimiters("{\n\t", overloads, " PENDING BELOW", "\n\t") + Useful.listInDelimiters(
                    "\n*\t",
                    pendingOverloads,
                    "}",
                    "\n*\t");
        }
        return Useful.listInDelimiters("{\n\t", overloads, "}", "\n\t");
    }

    public boolean getFinished() {
        return finishedFirst;
    }

    public boolean getFinishedSecond() {
        return finishedSecond;
    }

    public IdOrOpOrAnonymousName getFnName() {
        return fnName;
    }

    public boolean seqv(FValue v) {
        return false;
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
        if (finishedFirst) return;

        Overload ol;
        for (int i = 0; i < pendingOverloads.size(); i++) {
            // Cannot be an iterator -- will get comodification exception
            // iteration to the growing end is perfectly ok.
            ol = pendingOverloads.get(i);
            SingleFcn sfcn = ol.getFn();

            String ps = ol.ps != null ? String.valueOf(ol.ps) + " " : "";

            if (sfcn instanceof FunctionClosure) {
                FunctionClosure cl = (FunctionClosure) sfcn;
                if (!cl.getFinished()) cl.finishInitializing();

                if (debug) {
                    System.err.println("Overload " + ps + cl);
                }

            } else if (sfcn instanceof Constructor) {
                Constructor cl = (Constructor) sfcn;
                if (!cl.getFinished()) cl.finishInitializing();

                if (debug) {
                    System.err.println("Overload " + ps + cl);
                }

            } else if (sfcn instanceof GenericConstructor) {
                if (debug) {
                    System.err.println("Overload " + ps + sfcn);
                }

            } else if (sfcn instanceof Dummy_fcn) {
                if (debug) System.err.println("Overload primitive " + ps + sfcn);

            } else if (sfcn instanceof FGenericFunction) {
                if (debug) System.err.println("Overload generic " + ps + sfcn);

            } else {
                bug(errorMsg("Expected a closure or primitive, instead got ", sfcn));
            }

            if (ol.ps != null) ol.ps.close();
        }
        finishedFirst = true;
    }

    /**
     * The second part of finishing ensures that the overloadings
     * are consistent, and assigns a type to the value.
     */
    @SuppressWarnings ("unchecked")
    public synchronized boolean finishInitializingSecondPart() {

        boolean change = false;

        if (finishedSecond) return change;

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
            ArrayList<FType> ftalist = new ArrayList<FType>(new_overloads.size());

            OverloadComparisonResult ocr = new OverloadComparisonResult();

            for (int i = 0; i < new_overloads.size(); i++) {
                Overload o1 = new_overloads.get(i);
                Fcn fn = o1.getFn();
                if (fn instanceof GenericFunctionOrConstructor) {
                    needsInference = true;
                } else {
                    FType ty = fn.type();
                    if (ty != null) ftalist.add(ty);
                }

                if (!noCheck && !o1.guaranteedOK) {

                    for (int j = i - 1; j >= 0; j--) {

                        Overload o2 = new_overloads.get(j);
                        if (o2.guaranteedOK) continue;
                        SingleFcn f1 = o1.getFn();
                        SingleFcn f2 = o2.getFn();

                        if (genericFMAndInstance(f1, f2) || genericFMAndInstance(f2, f1)) continue;

                        ocr.reset();
                        ocr.completeOverloadingCheck(o1, o2, new_overloads, within);

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

    // FUTURE REFACTORING -- the static methods below will
    // become methods of this class.  The intent is to allow
    // function-by-function queries from within a trait,
    // to permit correctness/overlap checking of overrides.
    public static class OverloadComparisonResult {
        private int p1better; // set to index where p1 is subtype
        private int p2better; // set to index where p2 is subtype
        private boolean meetOk;

        boolean distinct; // known to exclude
        private int unrelated; // neither subtype nor exclude nor identical
        private boolean unequal;
        private boolean sawSymbolic1;
        private boolean sawSymbolic2;
        private int selfIndex;
        private int min;
        private boolean allObjInstance1;
        private boolean allObjInstance2;
        private boolean result1Subtype2Failure;
        private boolean result2Subtype1Failure;

        private int l1;
        private int l2;
        ;

        private boolean rest1;
        private boolean rest2;

        public OverloadComparisonResult() {
            reset();
        }

        public void reset() {
            p1better = -1; // set to index where p1 is subtype
            p2better = -1; // set to index where p2 is subtype

            distinct = false; // known to exclude
            unrelated = -1; // neither subtype nor exclude nor identical
            unequal = false;
            sawSymbolic1 = false;
            sawSymbolic2 = false;
            selfIndex = -1;
            min = Integer.MAX_VALUE;
            allObjInstance1 = true;
            allObjInstance2 = true;
            result1Subtype2Failure = false;
            result2Subtype1Failure = false;

            l1 = -1;
            l2 = -2;
            rest1 = false;
            rest2 = false;
            meetOk = false;

        }

        public void completeOverloadingCheck(Overload o1,
                                             Overload o2,
                                             Collection<Overload> new_overloads,
                                             Environment within) {

            //OverloadComparisonResult ocr = this;

            List<FType> pl1 = o1.getParams();
            List<FType> pl2 = o2.getParams();

            {
                l1 = pl1.size();
                l2 = pl2.size();

                rest1 = (l1 > 0 && pl1.get(l1 - 1) instanceof FTypeRest);
                rest2 = (l2 > 0 && pl2.get(l2 - 1) instanceof FTypeRest);

                // by construction, l1 is bigger.
                // (see sort order above)
                // possibilities
                // rest1 l2 can be no smaller than l1-1; iterate to l2
                // rest2 test out to l1
                // both  test out to l1
                // neither = required


                if (rest2) {
                    // both, rest2
                    min = l1;
                } else if (rest1) {
                    // rest1
                    if (l2 < l1 - 1) return;
                    min = l2;
                } else {
                    // neither
                    if (l1 != l2) return;
                    min = l1;
                }

                //    if (!do_continue) {


                //                  int p1better = -1; // set to index where p1 is subtype
                //                  int p2better = -1; // set to index where p2 is subtype

                //                  boolean distinct = false; // known to exclude
                //                  int unrelated = -1; // neither subtype nor exclude nor identical
                //                  boolean unequal = false;
                //                  boolean sawSymbolic1 = false;
                //                  boolean sawSymbolic2 = false;
                //                  int selfIndex = -1;

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
                //                  boolean allObjInstance1 = true;
                //                  boolean allObjInstance2 = true;

                exclDumpln("Checking exclusion of ", pl1, " and ", pl2, ":");
                for (int k = 0; k < min; k++) {
                    FType p1 = pl1.get(k);
                    FType p2 = k < l2 ? pl2.get(k) : pl2.get(l2 - 1);
                    exclDump(k, ": ", p1, " and ", p2, ", ", p1.getExtends(), " and ", p2.getExtends(), ", ");

                    p1 = deRest(p1);
                    p2 = deRest(p2);

                    if (p1 == p2 && !p1.isSymbolic() && !p2.isSymbolic()) {
                        exclDumpln("equal.");
                        continue;
                    }

                    allObjInstance1 &= p1 instanceof FTypeObject;
                    allObjInstance2 &= p2 instanceof FTypeObject;

                    unequal = true;

                    if (o1.getSelfParameterIndex() == k && o1.getSelfParameterIndex() == o2.getSelfParameterIndex()) {
                        exclDumpln("self params.");
                        // ONLY set this when the self indices coincide -- otherwise, they obey the same rules.
                        selfIndex = k;
                        /*
                        * Somebody Else's Problem -- if self parameters are different,
                        * any problems will be flagged at the object level.
                        */
                        if (!p1.equals(p2)) distinct = true; // This seems wrong/unnecessary
                    }

                    if ( o1.getFn().getFnName() instanceof Op &&
                         o2.getFn().getFnName() instanceof Op &&
                         ((Op)o1.getFn().getFnName()).getFixity() !=
                         ((Op)o2.getFn().getFnName()).getFixity() )
                        distinct = true;

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
                            if (p1.isSymbolic()) sawSymbolic1 = true;

                            if (p2.isSymbolic()) sawSymbolic2 = true;
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
            }
            //      }

            if (!distinct) {
                if (p1better >= 0 && p2better >= 0) {
                    meetOk = meetExistsIn(o1, o2, new_overloads);
                } else {
                    FType r1 = o1.getFn().getRange();
                    FType r2 = o2.getFn().getRange();
                    if (p1better >= 0) {
                        // subtype rule applies
                        result1Subtype2Failure = !r1.subtypeOf(r2);
                    } else if (p2better >= 0) {
                        // subtype rule applies
                        result2Subtype1Failure = !r2.subtypeOf(r1);
                    }
                }
            }


            describeOverloadingFailure(o1, o2, within, pl1, pl2);

            return;
        }

        public boolean overloadOk() {

            // exclusion is good.
            if (distinct) return true;

            // non-ground types need exclusion
            if (sawSymbolic1 || sawSymbolic2 || unrelated != -1) {
                return false;
            }

            // meet rule
            if (p1better >= 0 && p2better >= 0 && !meetOk) return false;

            // neither is better, not a functional method
            if (p1better < 0 && p2better < 0 && selfIndex < 0) return false;

            if (result1Subtype2Failure || result2Subtype1Failure) return false;

            return true;
        }

        private void describeOverloadingFailure(Overload o1,
                                                Overload o2,
                                                Environment within,
                                                List<FType> pl1,
                                                List<FType> pl2) {
            if (distinct) return;
            //OverloadComparisonResult ocr = this;
            if (sawSymbolic1 || sawSymbolic2) {
                exclDumpSkip();
                String explanation;
                if (sawSymbolic1 && sawSymbolic2) explanation = errorMsg("\n", o1, " and\n", o2, " have parameters\n");
                else if (sawSymbolic1) explanation = errorMsg("\n", o1, " has a parameter\n");
                else explanation = errorMsg("\n", o2, " has a parameter\n");
                explanation =
                        explanation + "with generic type, at least one pair of parameters must have excluding types";
                error(o1, o2, within, explanation);
            }

            if (unrelated != -1) {
                exclDumpSkip();
                String s1 = parameterName(unrelated, o1);
                String s2 = parameterName(unrelated, o2);

                String explanation = errorMsg(Ordinal.ordinal(unrelated + 1),
                                              " parameters ",
                                              s1,
                                              ":",
                                              pl1,
                                              " and ",
                                              s2,
                                              ":",
                                              pl2,
                                              " are unrelated (neither subtype, excludes, nor equal) and no excluding pair is present");
                error(o1, o2, within, explanation);
            }

            if (p1better >= 0 && p2better >= 0 && !meetOk) {
                exclDumpSkip();
                error(o1, o2, within, errorMsg("Overloading of\n\t(first) ",
                                               o1,
                                               " and\n\t(second) ",
                                               o2,
                                               " fails because\n\t",
                                               formatParameterComparison(p1better, o1, o2, "more"),
                                               " but\n\t",
                                               formatParameterComparison(p2better, o1, o2, "less")));
            }
            if (p1better < 0 && p2better < 0 && selfIndex < 0) {
                exclDumpSkip();
                String explanation = null;
                if (l1 == l2 && rest1 == rest2) {
                    if (unequal) explanation = errorMsg("Overloading of ",
                                                        o1,
                                                        " and ",
                                                        o2,
                                                        " fails because their parameter lists have potentially overlapping (non-excluding) types");
                    else explanation = errorMsg("Overloading of ",
                                                o1,
                                                " and ",
                                                o2,
                                                " fails because their parameter lists have the same types");
                } else explanation = errorMsg("Overloading of ",
                                              o1,
                                              " and ",
                                              o2,
                                              " fails because of ambiguity in overlapping rest (...) parameters");
                error(o1, o2, within, explanation);
            }

            if (result1Subtype2Failure) {
                String explanation = errorMsg("Overloading of ",
                                              o1,
                                              " and ",
                                              o2,
                                              " fails because the first parameter list is a subtype of the second, but the first result is not a subtype of the second");
                // System.err.println("FAIL " + explanation);
                error(o1, o2, within, explanation);
            }
            if (result2Subtype1Failure) {
                String explanation = errorMsg("Overloading of ",
                                              o1,
                                              " and ",
                                              o2,
                                              " fails because the second parameter list is a subtype of the first, but the second result is not a subtype of the first");
                // System.err.println("FAIL " + explanation);
                error(o1, o2, within, explanation);
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

    static private String formatParameterComparison(int i, Overload o1, Overload o2, String how) {
        String s1 = parameterName(i, o1);
        String s2 = parameterName(i, o2);
        return "(first) " + s1 + " is " + how + " specific than (second) " + s2;
    }

    /**
     * @param i
     * @param f1
     */
    static private String parameterName(int i, Overload o) {
        SingleFcn f1 = o.getFn();
        String s1;
        if (f1 instanceof NonPrimitive) {
            NonPrimitive np = (NonPrimitive) f1;
            s1 = np.getParameters().get(i).getName();
        } else {
            s1 = "parameter " + (i + 1);
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
    static private boolean meetExistsIn(Overload o1, Overload o2, Collection<Overload> overloads2) {
        List<FType> pl1 = o1.getParams();
        List<FType> pl2 = o2.getParams();

        Set<List<FType>> meet_set = FTypeTuple.meet(pl1, pl2);

        if (meet_set.size() != 1) return false;

        List<FType> meet = meet_set.iterator().next();

        for (Overload o : overloads2) {
            if (meet.equals(o.getParams())) return true;
        }
        // TODO finish this.

        return false;
    }

    static private FType deRest(FType p1) {
        if (p1 instanceof FTypeRest) p1 = ((FTypeRest) p1).getType();
        return p1;
    }

    /**
     * Needs an environment to construct its supertype,
     * but otherwise it is never examined.
     *
     * @param within
     */
    public OverloadedFunction(IdOrOpOrAnonymousName fnName, Environment within) {
        super(within);
        this.fnName = fnName;
    }

    public OverloadedFunction(IdOrOpOrAnonymousName fnName, Set<? extends Simple_fcn> ssf, Environment within) {
        this(fnName, within);
        for (Simple_fcn sf : ssf) {
            addOverload(sf);
        }
        finishInitializingSecondPart();
    }

    public void addOverload(SingleFcn fn) {
        //      if (finishedFirst && !fn.getFinished())
        //          throw new IllegalStateException("Any functions added after finishedFirst must have types assigned.");
        addOverload(new Overload(fn));
    }

    public void addOverload(SingleFcn fn, boolean guaranteedOK) {
        //      if (finishedFirst && !fn.getFinished())
        //          throw new IllegalStateException("Any functions added after finishedFirst must have types assigned.");
        addOverload(new Overload(fn, this, guaranteedOK));
    }

    public void addOverloads(OverloadedFunction cls) {
        if (cls == this) return; // Prevents a comodification exception if
        // we import FortressLibrary a second time.

        List<Overload> clso = cls.overloads;
        for (Overload cl : clso) {
            addOverload(cl);
        }
        clso = cls.pendingOverloads;
        try {
            for (Overload cl : clso) {

                addOverload(cl);
            }
        }
        catch (ConcurrentModificationException ex) {
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
            if (debug) System.err.println("Lock " + fnName.stringName());
            finishedFirst = false;
            finishedSecond = false;
            pendingOverloads.add(overload);
            // InstantiationLock.lastOverload = this;
            // InstantiationLock.lastOverloadThrowable = Useful.backtrace(0, 1000);
        }

        if (debug) {
            try {
                DebugletPrintStream ps = DebugletPrintStream.make("OVERLOADS");
                overload.ps = ps;
                System.err.println("add " + ps + " " + overload);
                ps.backtrace().flush();
            } catch (IOException e) {}
        }

    }

    // TODO continue audit of functions in here.
    @Override
    public FValue applyInnerPossiblyGeneric(List<FValue> args) {

        SingleFcn best_f = cache.get(args);

        if (best_f == null) {

            best_f = bestMatch(args, overloads);

            if (best_f instanceof FunctionalMethod) {
                FunctionalMethod fm = (FunctionalMethod) best_f;
                if (debugMatch) System.err.print("\nRefining functional method " + best_f);
                best_f = fm.getApplicableClosure(args);
            }
            if (best_f instanceof GenericFunctionOrMethod) {
                return bug(errorMsg("overload res yielded generic ", best_f));
            }

            if (debugMatch) System.err.println("Choosing " + best_f + " for args " + args);
            cache.syncPut(args, best_f);
        }

        return best_f.applyInnerPossiblyGeneric(args);
    }

    /**
     * Returns index of best match for args among the overloaded functions.
     *
     * @throws Error
     */
    public SingleFcn bestMatch(List<FValue> args, List<Overload> someOverloads) throws Error {
        if (!finishedSecond && InstantiationLock.L.isHeldByCurrentThread()) bug("Cannot call before 'setFinished()'");

        SingleFcn best = bestMatchInternal(args, someOverloads);
        if (best == null) {
            // TODO add checks for COERCE, right here.
            // Replay the test for debugging
            // best = bestMatchInternal(args, someOverloads);
            error(errorMsg("Failed to find any matching overload, args = ",
                           Useful.listInParens(args),
                           ", overload = ",
                           this));
        }
        return best;
    }

    private SingleFcn bestMatchInternal(List<FValue> args, List<Overload> someOverloads) {
        SingleFcn best_sfn = null;

        if (debugMatch) {
            System.err.println("Seeking best match for " + args);
        }

        for (Overload o : someOverloads) {
            if (o.getParams() == null) {
                bug(errorMsg("Unfinished overloaded function ", this));
            }
            SingleFcn sfn = o.getFn();

            List<FValue> oargs = args;

            if (sfn instanceof GenericFunctionOrMethod) {
                GenericFunctionOrMethod gsfn = (GenericFunctionOrMethod) sfn;
                try {
                    sfn = EvaluatorBase.inferAndInstantiateGenericFunction(oargs, gsfn, gsfn.getWithin());
                    if (debugMatch) System.err.println("Inferred from " + gsfn + " to " + sfn);
                }
                catch (FortressException pe) {
                    if (debugMatch) System.err.println("No match for " + gsfn);
                    continue; // No match, means no dice.
                }

            } else if (debugMatch) {
                System.err.println("Trying w/o instantiation: " + sfn);
            }

            oargs = sfn.fixupArgCount(args);

            // Non-generic, old code.
            if (oargs != null && argsMatchTypes(oargs, sfn.getDomain()) &&
                (best_sfn == null || FTypeTuple.moreSpecificThan(sfn.getDomain(), best_sfn.getDomain()))) {
                best_sfn = sfn;
            }

        }
        return best_sfn;
    }

    /**
     * @param args
     * @param args_len
     * @param i
     * @param o
     * @return
     */
    // private boolean argsMatchTypes(List<FValue> args, Overload o) {
    //     List<FType> l = o.getParams();
    //     return argsMatchTypes(args, l);
    // }

    public static boolean argsMatchTypes(List<FValue> args, List<FType> l) {
        for (int j = 0; j < args.size(); j++) {
            FValue a = args.get(j);
            FType t = Useful.clampedGet(l, j).deRest();
            try {
                if (!t.typeMatch(a)) {
                    return false;
                }
            }
            catch (FortressException e) {
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
        if (finishedSecond) return;
        finishedSecond = true;
        finishedFirst = true;
        if (debug) System.err.println("Unlock " + fnName.stringName());
    }

    /* This code gives overloaded functions the interface of a set of generic
     * functions.
     *
     */

    private class Factory implements Factory1P<List<FType>, Fcn, HasAt> {

        public Fcn make(List<FType> args, HasAt location) {
            // TODO finish this.

            SingleFcn f = null;
            OverloadedFunction of = null;

            for (Overload ol : getOverloads()) {
                SingleFcn sfcn = ol.getFn();
                if (sfcn instanceof GenericFunctionOrMethod) {
                    GenericFunctionOrMethod gf = (GenericFunctionOrMethod) sfcn;

                    // Check that args matches the static parameters of the generic function
                    // TODO -- can a generic instantiation result in an unfulfillable overloading?

                    if (compatible(args, gf.getStaticParams())) {

                        SingleFcn tf = gf.typeApply(args, location);
                        if (f == null) {
                            f = tf;
                        } else if (of == null) {
                            of = new OverloadedFunction(getFnName(), getWithin());
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
            return error(location, errorMsg("No matches for instantiation of overloaded function ",
                                            OverloadedFunction.this,
                                            " with ",
                                            Useful.listInParens(args)));
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
            if (NodeUtil.isTypeParam(p)) {
                if (a instanceof FTypeNat) return false;
            }
        }
        return true;
    }

    public Fcn typeApply(List<StaticArg> args, Environment e, HasAt location) {
        EvalType et = new EvalType(e);
        // TODO Can combine these two functions if we enhance the memo and factory
        // to pass two parameters instead of one.
        ArrayList<FType> argValues = et.forStaticArgList(args);

        return typeApply(argValues, location);
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
    Fcn typeApply(List<FType> argValues, HasAt location) throws ProgramError {
        // Need to filter for matching generics in the overloaded type.
        return make(argValues, location);
    }


}
