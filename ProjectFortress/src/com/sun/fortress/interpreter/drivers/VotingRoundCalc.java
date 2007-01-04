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

package com.sun.fortress.interpreter.drivers;

public class VotingRoundCalc {

    public static void usage() {
        System.err.println("java VotingRoundCalc #candidates #finalists #rounds");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
        double cands = Double.parseDouble(args[0]);
        double finals = Double.parseDouble(args[1]);
        double rounds = Double.parseDouble(args[2]);

        double keepratio = Math.exp( (Math.log(cands) - Math.log(finals))/rounds);

        for (int i = 1; i <= rounds; i++) {
            double next = cands/keepratio;
            double rnext = Math.round(next);
            double elim = cands - rnext;
            System.out.println("Vote " + i + " eliminates " + elim + " leaving " + rnext);
            cands = rnext;
        }

        } catch (Throwable th) {
            th.printStackTrace();
            usage();
        }

    }

}
