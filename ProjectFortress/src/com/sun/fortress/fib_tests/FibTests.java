/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.fib_tests;

import jsr166y.*;

/**
 * The goal of this file is to explore the design space of possible
 * FJTask implementations of the fibonacci function.  The idea is to
 * see what effects different code generation strategies have on
 * performance.  It is thus a NON-GOAL to actually optimize the fib
 * function itself (eg by depth bounding, which is the obvious way to
 * make recursive work-stealing fib run fast).  We structure this as a
 * series of inner classes, each with a long fib(long n) function.
 * What the classes do internally is completely open-ended.
 *
 * Stuff that appears to really matter: Anything that avoids allocation!
 * - Depth bounding task creation
 * - Performing one of the tasks inline without a spawn
 * - Performing an inline call when work isn't stolen
 * - Unboxing data into task objects where possible
 *
 * Things that might or might not make any difference:
 * - Using a static method for stateless portions of computation
 * - Factoring out (or not) various task creation boilerplate
 * - Making inputs final
 * - Nulling out inputs
 * - Performing trivial computations before task creation
 *
 * Results so far (based on 20 runs on a MacBook Pro2.5GHz Core 2 Duo):
 * Test                 time(s) Sigma   vs Seq  vs Par
 * Sequential            0.147  0.00042  1.000   1.070  Par is 7% faster
 * SeqBoxed              0.334  0.00457  2.281   2.441
 * UnboxedDepth          0.137  0.01025  0.935   1.000
 * UnboxedDDepth         0.138  0.00361  0.944   1.010
 * BoxedDepth            0.282  0.00474  1.924   2.059  Boxing costs less than 2x on 2 procs
 * UnboxedDDepthFlag     0.414  0.03020  2.827   3.025
 * UnboxedDUnforkNull    0.827  0.03191  5.644   6.038
 * UnboxedDUnforkMeth    0.828  0.04316  5.648   6.043
 * UnboxedDUnforkFactory 0.843  0.05084  5.757   6.159
 * UnboxedInline         0.844  0.04691  5.759   6.161
 * UnboxedDeepUnfork     0.857  0.05781  5.851   6.260
 * UnboxedDUnforkCompute 0.880  0.08042  6.007   6.427
 * UnboxedUnforkWork     0.941  0.05899  6.421   6.870
 * BoxedInline           1.239  0.06601  8.459   9.051
 * UnboxedDeepHonHelp    1.472  0.06311 10.050  10.753
 * UnboxedHonHelp        1.486  0.07632 10.146  10.855
 * UnboxedDeeperInline   1.563  0.09581 10.669  11.415
 * UnboxedInlineDotted   1.570  0.08488 10.716  11.466
 * UnboxedDeepHonest     1.578  0.04502 10.770  11.523
 * UnboxedLastInline     1.590  0.08216 10.854  11.613
 * UnboxedHonest         1.690  0.08880 11.536  12.343
 * Unboxed               1.733  0.06544 11.829  12.656
 * UnboxedMutual         1.782  0.08159 12.163  13.013
 * RT                    1.931  0.07362 13.180  14.101
 * RTNonFinal            1.960  0.12861 13.379  14.315
 * RTNull                2.203  0.08994 15.038  16.089
 * Boxed                 2.205  0.05915 15.050  16.102  naive code is costly!
 * BoxedBoxed            2.298  0.06212 15.686  16.782
 *
 **/

public final class FibTests {
    static int trials = 3;
    static long init_n = 36;
    static long expected = 0;
    static int trial = 0;
    static int procs = Runtime.getRuntime().availableProcessors();
    static boolean skipSeq = false;

    static ForkJoinPool pool;

    private static interface Fib {
        abstract long fib(long n);
        abstract boolean isSeq();
    }

    private static final class SequentialFib implements Fib {
        public String toString() { return "Sequential"; }
        public boolean isSeq() { return true; }
        public long fib(long n) {
            if (n <= 1) return n;
            return fib(n-1) + fib(n-2);
        }
    }

    private static final class SequentialBoxed implements Fib {
        public String toString() { return "SeqBoxed"; }
        public boolean isSeq() { return true; }
        public long fib(long n) {
            if (skipSeq) return expected;
            return bfib(n);
        }
        public Long bfib(Long n) {
            if (n <= 1) return n;
            return bfib(n-1) + bfib(n-2);
        }
    }

    /** Cribbed from the RecursiveTask documentation. */
    private static final class RecursiveTaskFib extends RecursiveTask<Long> implements Fib {
        final long n;
        RecursiveTaskFib(long n) { this.n = n; }
        RecursiveTaskFib() { this.n = 0; }
        public boolean isSeq() { return false; }
        public String toString() { return "RT"; }
        public long fib(long nn) {
            return pool.invoke(new RecursiveTaskFib(nn));
        }
        public Long compute() {
            if (n <= 1) return n;
            RecursiveTaskFib n1 = new RecursiveTaskFib(n-1);
            n1.fork();
            RecursiveTaskFib n2 = new RecursiveTaskFib(n-2);
            return n2.compute() + n1.join();
        }
    }

    /** As above but with non-final n field. */
    private static final class RTNonFinal extends RecursiveTask<Long> implements Fib {
        long n;
        RTNonFinal(long n) { this.n = n; }
        RTNonFinal() { this.n = 0; }
        public boolean isSeq() { return false; }
        public String toString() { return "RTNonFinal"; }
        public long fib(long nn) {
            return pool.invoke(new RTNonFinal(nn));
        }
        public Long compute() {
            if (n <= 1) return n;
            RTNonFinal n1 = new RTNonFinal(n-1);
            n1.fork();
            RTNonFinal n2 = new RTNonFinal(n-2);
            return n2.compute() + n1.join();
        }
    }

    /** All boxed all the time */
    private static final class Boxed extends RecursiveTask<Long> implements Fib {
        Long n;
        Boxed(Long n) { this.n = n; }
        Boxed() { this.n = null; }
        public boolean isSeq() { return false; }
        public String toString() { return "Boxed"; }
        public long fib(long nn) {
            return pool.invoke(new Boxed(nn));
        }
        public Long compute() {
            if (n <= 1) return n;
            Boxed n1 = new Boxed(n-1);
            n1.fork();
            Boxed n2 = new Boxed(n-2);
            return n2.compute() + n1.join();
        }
    }

    /** All boxed all the time, but nulling out incoming argument. */
    private static final class RTNull extends RecursiveTask<Long> implements Fib {
        Long n0;
        RTNull(Long n) { this.n0 = n; }
        RTNull() { this.n0 = null; }
        public boolean isSeq() { return false; }
        public String toString() { return "RTNull"; }
        public long fib(long n) {
            return pool.invoke(new RTNull(n));
        }
        public Long compute() {
            long n = n0;
            n0 = null;
            if (n <= 1) return n;
            RTNull n1 = new RTNull(n-1);
            n1.fork();
            RTNull n2 = new RTNull(n-2);
            return n2.compute() + n1.join();
        }
    }

    /** Here we track our result in line. */
    private static final class Unboxed extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        Unboxed(long n) { this.n = n; }
        Unboxed() { this.n = 0; }
        public boolean isSeq() { return false; }
        public String toString() { return "Unboxed"; }
        public long fib(long nn) {
            Unboxed t = new Unboxed(nn);
            pool.invoke(t);
            return t.result;
        }
        public void compute() {
            if (n <= 1) { result=n; return; }
            Unboxed n1 = new Unboxed(n-1);
            n1.fork();
            Unboxed n2 = new Unboxed(n-2);
            n2.compute();
            n1.join();
            result = n1.result + n2.result;
        }
    }

    /** Here we track our result in line. */
    private static final class BoxedBoxed extends RecursiveAction implements Fib {
        Long n;
        Long result = null;
        BoxedBoxed(Long n) { this.n = n; }
        BoxedBoxed() { this.n = null; }
        public boolean isSeq() { return false; }
        public String toString() { return "BoxedBoxed"; }
        public long fib(long nn) {
            BoxedBoxed t = new BoxedBoxed(nn);
            pool.invoke(t);
            return t.result;
        }
        public void compute() {
            if (n <= 1) { result=n; return; }
            BoxedBoxed n1 = new BoxedBoxed(n-1);
            n1.fork();
            BoxedBoxed n2 = new BoxedBoxed(n-2);
            n2.compute();
            n1.join();
            result = n1.result + n2.result;
        }
    }

    /** Here we pull out a separate function and do mutual recursion.
        This separates the "nice" code in fibb that does the fib +
        task creation from the "ugly" code that pulls data out of and
        puts data back in to the task object.  Does this make any difference? */
    private static final class UnboxedMutual extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedMutual(long n) { this.n = n; }
        UnboxedMutual() { this.n = 0; }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedMutual"; }
        public long fib(long nn) {
            UnboxedMutual t = new UnboxedMutual(nn);
            pool.invoke(t);
            return t.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedMutual n1 = new UnboxedMutual(nn-1);
            n1.fork();
            UnboxedMutual n2 = new UnboxedMutual(nn-2);
            n2.compute();
            n1.join();
            return n1.result + n2.result;
        }
        public void compute() {
            result = fibb(n);
        }
    }

    /** Here we do the second task inline without creating a task object at all.  Note
        how the previous transformation makes this easy.  */
    private static final class UnboxedLastInline extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedLastInline(long n) { this.n = n; }
        UnboxedLastInline() { this.n = 0; }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedLastInline"; }
        public long fib(long nn) {
            UnboxedLastInline t = new UnboxedLastInline(nn);
            pool.invoke(t);
            return t.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedLastInline n1 = new UnboxedLastInline(nn-1);
            n1.fork();
            long n2 = fibb(nn-2);
            n1.join();
            return n1.result + n2;
        }
        public void compute() {
            result = fibb(n);
        }
    }

    /** Variant of the above with a static, rather than dotted, method.  */
    private static final class UnboxedInlineDotted extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedInlineDotted(long n) { this.n = n; }
        UnboxedInlineDotted() { this.n = 0; }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedInlineDotted"; }
        public long fib(long nn) {
            UnboxedInlineDotted t = new UnboxedInlineDotted(nn);
            pool.invoke(t);
            return t.result;
        }
        long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedInlineDotted n1 = new UnboxedInlineDotted(nn-1);
            n1.fork();
            long n2 = fibb(nn-2);
            n1.join();
            return n1.result + n2;
        }
        public void compute() {
            result = fibb(n);
        }
    }

    /** Up until now we've been a bit dishonest about what work gets
     * included in the recursive action---we've sneakily done the
     * subtraction of 1 from n up front.  Here's what happens if we do
     * it as part of compute().
     *
     * We're still being a little bit dishonest here about the
     * outermost pool submission, but we're going to pretend that
     * doesn't matter much.
     */
    private static final class UnboxedHonest extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedHonest(long n) { this.n = n; }
        UnboxedHonest() { this.n = 0; }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedHonest"; }
        public long fib(long nn) {
            UnboxedHonest t = new UnboxedHonest(nn+1);
            pool.invoke(t);
            return t.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedHonest n1 = new UnboxedHonest(nn);
            n1.fork();
            long n2 = fibb(nn-2);
            n1.join();
            return n1.result + n2;
        }
        public void compute() {
            result = fibb(n-1);
        }
    }

    /** Here we do the deeper recursion inline and the shallower is
     * spawned.  How do we figure this out in practice?  Instrument, I
     * suppose (spawn the less-spawny one to minimize allocations).
     * But we need a task-aware VM, or a feedback path to HotSpot. */
    private static final class UnboxedDeeperInline extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedDeeperInline(long n) { this.n = n; }
        UnboxedDeeperInline() { this.n = 0; }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedDeeperInline"; }
        public long fib(long nn) {
            UnboxedDeeperInline t = new UnboxedDeeperInline(nn);
            pool.invoke(t);
            return t.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedDeeperInline n2 = new UnboxedDeeperInline(nn-2);
            n2.fork();
            long n1 = fibb(nn-1);
            n2.join();
            return n1 + n2.result;
        }
        public void compute() {
            result = fibb(n);
        }
    }

    /** UnboxedDeep, but again deferring the subtraction until compute time. */
    private static final class UnboxedDeepHonest extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedDeepHonest(long n) { this.n = n; }
        UnboxedDeepHonest() { this.n = 0; }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedDeepHonest"; }
        public long fib(long nn) {
            UnboxedDeepHonest t = new UnboxedDeepHonest(nn+2);
            pool.invoke(t);
            return t.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedDeepHonest n2 = new UnboxedDeepHonest(nn);
            n2.fork();
            long n1 = fibb(nn-1);
            n2.join();
            return n1 + n2.result;
        }
        public void compute() {
            result = fibb(n-2);
        }
    }

    /** UnboxedDeepHonest, using helpJoin at join point. */
    private static final class UnboxedDeepHonHelp extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedDeepHonHelp(long n) { this.n = n; }
        UnboxedDeepHonHelp() { this.n = 0; }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedDeepHonHelp"; }
        public long fib(long nn) {
            UnboxedDeepHonHelp t = new UnboxedDeepHonHelp(nn+2);
            pool.invoke(t);
            return t.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedDeepHonHelp n2 = new UnboxedDeepHonHelp(nn);
            n2.fork();
            long n1 = fibb(nn-1);
            n2.helpJoin();
            return n1 + n2.result;
        }
        public void compute() {
            result = fibb(n-2);
        }
    }

    /** UnboxedHonest, except using helpJoin at join point.
     */
    private static final class UnboxedHonHelp extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedHonHelp(long n) { this.n = n; }
        UnboxedHonHelp() { this.n = 0; }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedHonHelp"; }
        public long fib(long nn) {
            UnboxedHonHelp t = new UnboxedHonHelp(nn+1);
            pool.invoke(t);
            return t.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedHonHelp n1 = new UnboxedHonHelp(nn);
            n1.fork();
            long n2 = fibb(nn-2);
            n1.helpJoin();
            return n1.result + n2;
        }
        public void compute() {
            result = fibb(n-1);
        }
    }

    /** UnboxedDeepHonest, but attempt to unfork work and do inline. */
    private static final class UnboxedDeepUnfork extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedDeepUnfork(long n) { this.n = n; }
        UnboxedDeepUnfork() { this.n = 0; }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedDeepUnfork"; }
        public long fib(long nn) {
            UnboxedDeepUnfork t = new UnboxedDeepUnfork(nn+2);
            pool.invoke(t);
            return t.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedDeepUnfork w = new UnboxedDeepUnfork(nn);
            w.fork();
            long n1 = fibb(nn-1);
            long n2;
            if (w.tryUnfork()) {
                n2 = fibb(nn-2);
            } else {
                w.join();
                n2 = w.result;
            }
            return n1 + n2;
        }
        public void compute() {
            result = fibb(n-2);
        }
    }

    /** UnboxedDeepUnfork, but shifted slow path to getResult method.  This
     * could be shared in a common superclass, thus avoiding a certain
     * amount of boilerplate.
     */
    private static final class UnboxedDUnforkMeth extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedDUnforkMeth(long n) { this.n = n; }
        UnboxedDUnforkMeth() { this.n = 0; }
        public long getResult() { this.join(); return this.result; }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedDUnforkMeth"; }
        public long fib(long nn) {
            UnboxedDUnforkMeth t = new UnboxedDUnforkMeth(nn+2);
            pool.invoke(t);
            return t.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedDUnforkMeth w = new UnboxedDUnforkMeth(nn);
            w.fork();
            long n1 = fibb(nn-1);
            long n2 = w.tryUnfork() ? fibb(nn-2) : w.getResult();
            return n1 + n2;
        }
        public void compute() {
            result = fibb(n-2);
        }
    }

    /** UnboxedDeepUnfork, but just calls through to w.compute().
     */
    private static final class UnboxedDUnforkCompute extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedDUnforkCompute(long n) { this.n = n; }
        UnboxedDUnforkCompute() { this.n = 0; }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedDUnforkCompute"; }
        public long fib(long nn) {
            UnboxedDUnforkCompute t = new UnboxedDUnforkCompute(nn+2);
            pool.invoke(t);
            return t.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedDUnforkCompute w = new UnboxedDUnforkCompute(nn);
            w.fork();
            long n1 = fibb(nn-1);
            if (w.tryUnfork()) w.compute(); else w.join();
            long n2 = w.result;
            return n1 + n2;
        }
        public void compute() {
            result = fibb(n-2);
        }
    }

    /** UnboxedDeepUnfork, but place work in standalone static function.
     * This is something the compiler might reasonably do as a static
     * program transformation, turning the work into a function and
     * then closure-converting / lambda-lifting it. */
    private static final class UnboxedUnforkWork extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedUnforkWork(long n) { this.n = n; }
        UnboxedUnforkWork() { this.n = 0; }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedUnforkWork"; }
        public long fib(long nn) {
            UnboxedUnforkWork t = new UnboxedUnforkWork(nn+2);
            pool.invoke(t);
            return t.result;
        }
        static long work1(long nn) {
            return fibb(nn-2);
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedUnforkWork w = new UnboxedUnforkWork(nn);
            w.fork();
            long n1 = fibb(nn-1);
            long n2;
            if (w.tryUnfork()) {
                n2 = work1(nn);
            } else {
                w.join();
                n2 = w.result;
            }
            return n1 + n2;
        }
        public void compute() {
            result = work1(n);
        }
    }

    /** UnboxedDeepUnfork, but with work creation factory. */
    private static final class UnboxedDUnforkFactory extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedDUnforkFactory(long n) { this.n = n; }
        UnboxedDUnforkFactory() { this.n = 0; }
        static UnboxedDUnforkFactory factory(long n) {
            UnboxedDUnforkFactory r = new UnboxedDUnforkFactory(n);
            r.fork();
            return r;
        }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedDUnforkFactory"; }
        public long fib(long nn) {
            UnboxedDUnforkFactory t = new UnboxedDUnforkFactory(nn+2);
            pool.invoke(t);
            return t.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedDUnforkFactory w = factory(nn);
            long n1 = fibb(nn-1);
            long n2;
            if (w.tryUnfork()) {
                n2 = fibb(nn-2);
            } else {
                w.join();
                n2 = w.result;
            }
            return n1 + n2;
        }
        public void compute() {
            result = fibb(n-2);
        }
    }

    /** UnboxedDUnforkFactory, but with null check */
    private static final class UnboxedDUnforkNull extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedDUnforkNull(long n) { this.n = n; }
        UnboxedDUnforkNull() { this.n = 0; }
        static UnboxedDUnforkNull factory(long n) {
            UnboxedDUnforkNull r = new UnboxedDUnforkNull(n);
            r.fork();
            return r;
        }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedDUnforkNull"; }
        public long fib(long nn) {
            UnboxedDUnforkNull t = new UnboxedDUnforkNull(nn+2);
            pool.invoke(t);
            return t.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedDUnforkNull w = factory(nn);
            long n1 = fibb(nn-1);
            long n2;
            if (w == null || w.tryUnfork()) {
                n2 = fibb(nn-2);
            } else {
                w.join();
                n2 = w.result;
            }
            return n1 + n2;
        }
        public void compute() {
            result = fibb(n-2);
        }
    }

    /** UnboxedUnforkNull, with depth bound check in factory. */
    private static final class UnboxedDDepth extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedDDepth(long n) { this.n = n; }
        UnboxedDDepth() { this.n = 0; }
        static UnboxedDDepth factory(long n) {
            if (ForkJoinTask.getSurplusQueuedTaskCount() > 3) return null;
            UnboxedDDepth r = new UnboxedDDepth(n);
            r.fork();
            return r;
        }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedDDepth"; }
        public long fib(long nn) {
            UnboxedDDepth t = new UnboxedDDepth(nn+2);
            pool.invoke(t);
            return t.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedDDepth w = factory(nn);
            long n1 = fibb(nn-1);
            long n2;
            if (w == null || w.tryUnfork()) {
                n2 = fibb(nn-2);
            } else {
                w.join();
                n2 = w.result;
            }
            return n1 + n2;
        }
        public void compute() {
            result = fibb(n-2);
        }
    }

    /** UnboxedUnforkNull, with depth bound check in factory. */
    private static final class UnboxedDepth extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        UnboxedDepth(long n) { this.n = n; }
        UnboxedDepth() { this.n = 0; }
        static UnboxedDepth factory(long n) {
            if (ForkJoinTask.getSurplusQueuedTaskCount() > 3) return null;
            UnboxedDepth r = new UnboxedDepth(n);
            r.fork();
            return r;
        }
        static boolean runInline(UnboxedDepth w) {
            if (w==null || w.tryUnfork()) return true;
            w.join();
            return false;
        }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedDepth"; }
        public long fib(long nn) {
            UnboxedDepth t = new UnboxedDepth(nn);
            pool.invoke(t);
            return t.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedDepth w = factory(nn-2);
            long n1 = fibb(nn-1);
            long n2 = (runInline(w)) ? fibb(nn-2) : w.result;
            return n1 + n2;
        }
        public void compute() {
            result = fibb(n);
        }
    }

    /** UnboxedDDepth, with flag for work that's unspawned. */
    private static final class UnboxedDDepthFlag extends RecursiveAction implements Fib {
        long n;
        long result = 0;
        boolean spawned = false;
        UnboxedDDepthFlag(long n) { this.n = n; }
        UnboxedDDepthFlag() { this.n = 0; }
        static UnboxedDDepthFlag factory(long n) {
            UnboxedDDepthFlag r = new UnboxedDDepthFlag(n);
            if (ForkJoinTask.getSurplusQueuedTaskCount() > 3) return r;
            r.spawned = true;
            r.fork();
            return r;
        }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedDDepthFlag"; }
        public long fib(long nn) {
            UnboxedDDepthFlag t = new UnboxedDDepthFlag(nn+2);
            pool.invoke(t);
            return t.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedDDepthFlag w = factory(nn);
            long n1 = fibb(nn-1);
            long n2;
            if (!w.spawned || w.tryUnfork()) {
                n2 = fibb(nn-2);
            } else {
                w.join();
                n2 = w.result;
            }
            return n1 + n2;
        }
        public void compute() {
            result = fibb(n-2);
        }
    }

    /** Boxed but with inline calls */
    private static final class BoxedInline extends RecursiveTask<Long> implements Fib {
        Long n;
        BoxedInline(Long n) { this.n = n; }
        BoxedInline() { this.n = null; }
        static BoxedInline factory(Long n) {
            BoxedInline r = new BoxedInline(n);
            r.fork();
            return r;
        }
        static Long runInline(BoxedInline w) {
            if (w==null || w.tryUnfork()) return null;
            return w.join();
        }
        public boolean isSeq() { return false; }
        public String toString() { return "BoxedInline"; }
        public long fib(long nn) {
            return pool.invoke(new BoxedInline(nn));
        }
        static Long fibb(Long nn) {
            if (nn <= 1) { return nn; }
            BoxedInline w = factory(nn-2);
            Long n1 = fibb(nn-1);
            Long n2 = runInline(w);
            if (n2==null) n2 = fibb(nn-2);
            return n1+n2;
        }
        public Long compute() {
            return fibb(n);
        }
    }

    /** Boxed but with depth bounding */
    private static final class BoxedDepth extends RecursiveTask<Long> implements Fib {
        Long n;
        BoxedDepth(Long n) { this.n = n; }
        BoxedDepth() { this.n = null; }
        static BoxedDepth factory(Long n) {
            if (ForkJoinTask.getSurplusQueuedTaskCount() > 3) return null;
            BoxedDepth r = new BoxedDepth(n);
            r.fork();
            return r;
        }
        static Long runInline(BoxedDepth w) {
            if (w==null || w.tryUnfork()) return null;
            return w.join();
        }
        public boolean isSeq() { return false; }
        public String toString() { return "BoxedDepth"; }
        public long fib(long nn) {
            return pool.invoke(new BoxedDepth(nn));
        }
        static Long fibb(Long nn) {
            if (nn <= 1) { return nn; }
            BoxedDepth w = factory(nn-2);
            Long n1 = fibb(nn-1);
            Long n2 = runInline(w);
            if (n2==null) n2 = fibb(nn-2);
            return n1+n2;
        }
        public Long compute() {
            return fibb(n);
        }
    }

    /** Unboxed with inline calls */
    private static final class UnboxedInline extends RecursiveAction implements Fib {
        long n;
        long result;
        UnboxedInline(long n) { this.n = n; }
        UnboxedInline() { this.n = 0; }
        static UnboxedInline factory(long n) {
            UnboxedInline r = new UnboxedInline(n);
            r.fork();
            return r;
        }
        static boolean runInline(UnboxedInline w) {
            if (w==null || w.tryUnfork()) return true;
            w.join();
            return false;
        }
        public boolean isSeq() { return false; }
        public String toString() { return "UnboxedInline"; }
        public long fib(long nn) {
            UnboxedInline w = new UnboxedInline(nn);
            pool.invoke(w);
            return w.result;
        }
        static long fibb(long nn) {
            if (nn <= 1) { return nn; }
            UnboxedInline w = factory(nn-2);
            long n1 = fibb(nn-1);
            long n2 = runInline(w) ? fibb(nn-2) : w.result;
            return n1+n2;
        }
        public void compute() {
            result = fibb(n);
        }
    }

    private static final Fib [] allFibs = {
        new SequentialFib(),
        new SequentialBoxed(),
        new Boxed(),
        new Unboxed(),
        new BoxedInline(),
        new BoxedDepth(),
        new UnboxedInline(),
        new UnboxedDepth(),
        // Below this line are preliminary and less informative experiments.
        // new UnboxedDDepth(),
        // new UnboxedDDepthFlag(),
        // new UnboxedDeepUnfork(),
        // new UnboxedDUnforkNull(),
        // new UnboxedDUnforkFactory(),
        // new UnboxedUnforkWork(),
        // new UnboxedDUnforkMeth(),
        // new UnboxedDUnforkCompute(),
        // new UnboxedDeepHonest(),
        // new UnboxedDeepHonHelp(),
        // new UnboxedHonHelp(),
        // new UnboxedHonest(),
        // new UnboxedInlineDotted(),
        // new UnboxedDeeperInline(),
        // new UnboxedLastInline(),
        // new UnboxedMutual(),
        // new RecursiveTaskFib(),
        // new RTNonFinal(),
        // new RTNull(),
        // new BoxedBoxed(),
    };

    private static void report(Object f, long nano_start, long nano_end) {
        long elapsed = nano_end - nano_start;
        double readable = (double)elapsed * 1e-9;
        System.out.println(f+"\t"+readable);
    }

    private static void initPool() {
        long start = System.nanoTime();
        pool = new ForkJoinPool(procs);
        long end = System.nanoTime();
        report("PoolInit",start,end);
    }

    private static long simpleTrial(Fib f) {
        long start = System.nanoTime();
        long r = f.fib(init_n);
        long end = System.nanoTime();
        if (trial > 0) report(f,start,end);
        return r;
    }

    private static void trial(Fib f) {
        if (skipSeq && f.isSeq()) return;
        long r = simpleTrial(f);
        if (r != expected) {
            System.err.println(f+" yielded "+r+" != "+expected);
        }
    }

    private static void initTrial() {
        expected = simpleTrial(allFibs[0]);
    }

    public static void usage() {
        System.out.println("Usage: fib [-p procs] [-t trials] [-s] [k]");
        System.out.println("  By default, k="+init_n+", trials="+trials+", procs="+procs);
        System.out.println("  The -s flag omits sequential execution.");
        System.exit(1);
    }

    public static void parseArgs(String [] args) {
        int argc = args.length;
        boolean setp = false;
        boolean sett = false;
        for (int argn = 0; argn < argc; argn++) {
            if (args[argn].equals("-p")) {
                setp = true;
                continue;
            } else if (args[argn].equals("-t")) {
                sett = true;
                continue;
            } else if (args[argn].equals("-s")) {
                skipSeq = true;
                continue;
            }
            long i = 0;
            try {
                i = Long.decode(args[argn]);
            } catch (Throwable t) {
                usage();
            }
            if (setp) {
                procs = (int)i;
                setp = false;
            } else if (sett) {
                trials = (int)i;
                sett = false;
            } else if (argn+1 == argc) {
                init_n = i;
            } else {
                usage();
            }
        }
    }

    public static void main(String [] args) {
        parseArgs(args);
        initPool();
        initTrial();
        for (int i = 0; i < allFibs.length; i++) {
            for (trial = 0; trial <= trials; trial++) {
                System.gc();  // Pretend this might make a difference.
                trial(allFibs[i]);
            }
        }
    }
}
