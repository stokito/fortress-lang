/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Random;

public class Pickup52 {

    static String[] cards = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U",
            "V", "W", "X", "Y", "Z", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p",
            "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
    };

    /**
     * @param args
     */
    public static void main(String[] args) {
        int l = cards.length;
        for (int i = 0; i < args.length; i++) {
            Random r = new Random(args[i].hashCode());
            for (int j = 1; j < l - 1; j++) {
                int k = r.nextInt(l - j);
                // Swap l-j with k.
                String t = cards[k];
                cards[k] = cards[l - j];
                cards[l - j] = t;
            }
            for (int j = 0; j < l; j++) {
                if ((j + 1) % 13 == 0) System.out.println(cards[j] + ",");
                else System.out.print(cards[j] + ", ");

            }
        }


    }

}
