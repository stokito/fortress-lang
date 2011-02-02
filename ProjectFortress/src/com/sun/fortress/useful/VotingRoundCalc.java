/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

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

            double keepratio = Math.exp((Math.log(cands) - Math.log(finals)) / rounds);

            for (int i = 1; i <= rounds; i++) {
                double next = cands / keepratio;
                double rnext = Math.round(next);
                double elim = cands - rnext;
                System.out.println("Vote " + i + " eliminates " + elim + " leaving " + rnext);
                cands = rnext;
            }

        }
        catch (Throwable th) {
            th.printStackTrace();
            usage();
        }

    }

}
