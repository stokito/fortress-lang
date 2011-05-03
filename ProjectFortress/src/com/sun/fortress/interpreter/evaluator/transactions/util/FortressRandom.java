/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.transactions.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;

/**
 * Lightweight random number generator.  <I>Not thread-safe.</I> Synchronization in the
 * <CODE>java.util.Randome</CODE> can distort the performance of multithreaded benchmarks.
 */
public class FortressRandom extends java.util.Random {

    /**
     * use serialVersionUID from JDK 1.1 for interoperability
     */
    static final long serialVersionUID = 3905348978240129619L;

    private long seed;

    private final static long multiplier = 0x5DEECE66DL;
    private final static long addend = 0xBL;
    private final static long mask = (1L << 48) - 1;

    /**
     * Creates a new random number generator. This constructor sets
     * the seed of the random number generator to a value very likely
     * to be distinct from any other invocation of this constructor.
     */
    public FortressRandom() {
        this(++seedUniquifier + System.nanoTime());
    }

    private static volatile long seedUniquifier = 8682522807148012L;

    /**
     * Creates a new random number generator using a single
     * <code>long</code> seed:
     * <blockquote><pre>
     * public FortressRandom(long seed) { setSeed(seed); }</pre></blockquote>
     * Used by method <tt>next</tt> to hold
     * the state of the pseudorandom number generator.
     *
     * @param seed the initial seed.
     * @see java.util.Random#setSeed(long)
     */
    public FortressRandom(long seed) {
        this.seed = 0L;
        setSeed(seed);
    }

    /**
     * Sets the seed of this random number generator using a single
     * <code>long</code> seed. The general contract of <tt>setSeed</tt>
     * is that it alters the state of this random number generator
     * object so as to be in exactly the same state as if it had just
     * been created with the argument <tt>seed</tt> as a seed. The method
     * <tt>setSeed</tt> is implemented by class Random as follows:
     * <blockquote><pre>
     * synchronized public void setSeed(long seed) {
     *       this.seed = (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1);
     *       haveNextNextGaussian = false;
     * }</pre></blockquote>
     * The implementation of <tt>setSeed</tt> by class <tt>Random</tt>
     * happens to use only 48 bits of the given seed. In general, however,
     * an overriding method may use all 64 bits of the long argument
     * as a seed value.
     * <p/>
     * Note: Although the seed value is an AtomicLong, this method
     * must still be synchronized to ensure correct semantics
     * of haveNextNextGaussian.
     *
     * @param seed the initial seed.
     */
    public void setSeed(long seed) {
        seed = (seed ^ multiplier) & mask;
        this.seed = seed;
        haveNextNextGaussian = false;
    }

    /**
     * Generates the next pseudorandom number. Subclass should
     * override this, as this is used by all other methods.<p>
     * The general contract of <tt>next</tt> is that it returns an
     * <tt>int</tt> value and if the argument bits is between <tt>1</tt>
     * and <tt>32</tt> (inclusive), then that many low-order bits of the
     * returned value will be (approximately) independently chosen bit
     * values, each of which is (approximately) equally likely to be
     * <tt>0</tt> or <tt>1</tt>. The method <tt>next</tt> is implemented
     * by class <tt>Random</tt> as follows:
     * <blockquote><pre>
     * synchronized protected int next(int bits) {
     *       seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
     *       return (int)(seed >>> (48 - bits));
     * }</pre></blockquote>
     * This is a linear congruential pseudorandom number generator, as
     * defined by D. H. Lehmer and described by Donald E. Knuth in <i>The
     * Art of Computer Programming,</i> Volume 2: <i>Seminumerical
     * Algorithms</i>, section 3.2.1.
     *
     * @param bits random bits
     * @return the next pseudorandom value from this random number generator's sequence.
     * @since JDK1.1
     */
    protected int next(int bits) {
        seed = (seed * multiplier + addend) & mask;
        return (int) (seed >>> (48 - bits));
    }

    private static final int BITS_PER_BYTE = 8;
    private static final int BYTES_PER_INT = 4;

    private double nextNextGaussian;
    private boolean haveNextNextGaussian = false;

    /**
     * Returns the next pseudorandom, Gaussian ("normally") distributed
     * <code>double</code> value with mean <code>0.0</code> and standard
     * deviation <code>1.0</code> from this random number generator's sequence.
     * <p/>
     * The general contract of <tt>nextGaussian</tt> is that one
     * <tt>double</tt> value, chosen from (approximately) the usual
     * normal distribution with mean <tt>0.0</tt> and standard deviation
     * <tt>1.0</tt>, is pseudorandomly generated and returned. The method
     * <tt>nextGaussian</tt> is implemented by class <tt>Random</tt> as follows:
     * <blockquote><pre>
     * synchronized public double nextGaussian() {
     *    if (haveNextNextGaussian) {
     *            haveNextNextGaussian = false;
     *            return nextNextGaussian;
     *    } else {
     *            double v1, v2, s;
     *            do {
     *                    v1 = 2 * nextDouble() - 1;   // between -1.0 and 1.0
     *                    v2 = 2 * nextDouble() - 1;   // between -1.0 and 1.0
     *                    s = v1 * v1 + v2 * v2;
     *            } while (s >= 1 || s == 0);
     *            double multiplier = Math.sqrt(-2 * Math.log(s)/s);
     *            nextNextGaussian = v2 * multiplier;
     *            haveNextNextGaussian = true;
     *            return v1 * multiplier;
     *    }
     * }</pre></blockquote>
     * This uses the <i>polar method</i> of G. E. P. Box, M. E. Muller, and
     * G. Marsaglia, as described by Donald E. Knuth in <i>The Art of
     * Computer Programming</i>, Volume 2: <i>Seminumerical Algorithms</i>,
     * section 3.4.1, subsection C, algorithm P. Note that it generates two
     * independent values at the cost of only one call to <tt>Math.log</tt>
     * and one call to <tt>Math.sqrt</tt>.
     *
     * @return the next pseudorandom, Gaussian ("normally") distributed
     *         <code>double</code> value with mean <code>0.0</code> and
     *         standard deviation <code>1.0</code> from this random number
     *         generator's sequence.
     */
    public double nextGaussian() {
        // See Knuth, ACP, Section 3.4.1 Algorithm C.
        if (haveNextNextGaussian) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        } else {
            double v1, v2, s;
            do {
                v1 = 2 * nextDouble() - 1; // between -1 and 1
                v2 = 2 * nextDouble() - 1; // between -1 and 1
                s = v1 * v1 + v2 * v2;
            } while (s >= 1 || s == 0);
            double m = Math.sqrt(-2 * Math.log(s) / s);
            nextNextGaussian = v2 * m;
            haveNextNextGaussian = true;
            return v1 * m;
        }
    }

    /**
     * Serializable fields for Random.
     *
     * @serialField seed long;
     * seed for random computations
     * @serialField nextNextGaussian double;
     * next Gaussian to be returned
     * @serialField haveNextNextGaussian boolean
     * nextNextGaussian is valid
     */
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("seed", Long.TYPE), new ObjectStreamField("nextNextGaussian", Double.TYPE),
            new ObjectStreamField("haveNextNextGaussian", Boolean.TYPE)
    };

    /**
     * Reconstitute the <tt>Random</tt> instance from a stream (that is,
     * deserialize it). The seed is read in as long for
     * historical reasons, but it is converted to an AtomicLong.
     */
    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {

        ObjectInputStream.GetField fields = s.readFields();
        long seedVal;

        seedVal = (long) fields.get("seed", -1L);
        if (seedVal < 0) throw new java.io.StreamCorruptedException("FortressRandom: invalid seed");
        seed = seedVal;
        nextNextGaussian = fields.get("nextNextGaussian", 0.0);
        haveNextNextGaussian = fields.get("haveNextNextGaussian", false);
    }


    /**
     * Save the <tt>FortressRandom</tt> instance to a stream.
     * The seed of a FortressRandom is serialized as a long for
     * historical reasons.
     */
    synchronized private void writeObject(ObjectOutputStream s) throws IOException {
        // set the values of the Serializable fields
        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("seed", seed);
        fields.put("nextNextGaussian", nextNextGaussian);
        fields.put("haveNextNextGaussian", haveNextNextGaussian);

        // save them
        s.writeFields();

    }

}
