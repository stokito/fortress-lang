/*
 * Created on Jul 12, 2007
 *
 */
package com.sun.fortress.interpreter.nodes_util;

public class UIDObject {

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     * 
     * This is mostly for testing purposes, but it also allows
     * a more deterministic replay than Object.hashCode().
     */
    @Override
    public int hashCode() {
        return (int) uid ^ (int) (uid >>> 32);
    }

    static private Object lock = new Object();
    static private long seedUID = 0x7b546b0e12fd2559L;
    static private long prevUID = seedUID;
    private transient final long uid;
    
    public UIDObject() {
        uid = next();
    }
    
    public final long getUID() {
        return uid;
    }
    
    /* LFSR generating 63-bit residues */
    private long next() {
        synchronized (lock)
        {
        long x = prevUID;
        x = x + x;
        if (x < 0)
            x = x ^ 0xb1463923a7c109cdL;
        prevUID = x;
        return x;
        }
    }

}
