/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.naturalbridge.misc.Unsigned;


public class FNN64 extends FNativeObject implements HasIntValue {
    private final long val;
    private static volatile NativeConstructor con;

    private FNN64(long x) {
        super(null);
        val = x;
    }

    public int getInt() {
        // TODO Throw an error on out-of-range conversion?
        return (int) val;
    }

    public int getNN32() {
        // TODO Throw an error on out-of-range conversion?
        return (int) val;
    }

    public long getLong() {
        // TODO Throw an error on out-of-range conversion?
        return (long) val;
    }

    public long getNN64() {
        return val;
    }

    public double getFloat() {
        return Unsigned.toDouble(val);
    }

    public String getString() {
        return Unsigned.toString(val);
    }

    public String toString() {
        return getString() + ": NN64";
    }

    public boolean seqv(FValue v) {
        if (!(v instanceof FNativeObject)) return false;
        if (v instanceof FNN32 || v instanceof FNN64) {
            return (getNN64() == v.getNN64());
        }
        return false;
    }

    public static final FNN64 ZERO = new FNN64(0);


    private static FNN64[] cached = {
            ZERO, new FNN64(1), new FNN64(2), new FNN64(3), new FNN64(4), new FNN64(5), new FNN64(6), new FNN64(7),
            new FNN64(8), new FNN64(9), new FNN64(10), new FNN64(11), new FNN64(12), new FNN64(13), new FNN64(14),
            new FNN64(15), new FNN64(16), new FNN64(17), new FNN64(18), new FNN64(19), new FNN64(20), new FNN64(21),
            new FNN64(22), new FNN64(23), new FNN64(24), new FNN64(25), new FNN64(26), new FNN64(27), new FNN64(28),
            new FNN64(29), new FNN64(30), new FNN64(31), new FNN64(32), new FNN64(33), new FNN64(34), new FNN64(35),
            new FNN64(36), new FNN64(37), new FNN64(38), new FNN64(39), new FNN64(40), new FNN64(41), new FNN64(42),
            new FNN64(43), new FNN64(44), new FNN64(45), new FNN64(46), new FNN64(47), new FNN64(48), new FNN64(49),
            new FNN64(50), new FNN64(51), new FNN64(52), new FNN64(53), new FNN64(54), new FNN64(55), new FNN64(56),
            new FNN64(57), new FNN64(58), new FNN64(59), new FNN64(60), new FNN64(61), new FNN64(62), new FNN64(63),
            new FNN64(64), new FNN64(65), new FNN64(66), new FNN64(67), new FNN64(68), new FNN64(69), new FNN64(70),
            new FNN64(71), new FNN64(72), new FNN64(73), new FNN64(74), new FNN64(75), new FNN64(76), new FNN64(77),
            new FNN64(78), new FNN64(79), new FNN64(80), new FNN64(81), new FNN64(82), new FNN64(83), new FNN64(84),
            new FNN64(85), new FNN64(86), new FNN64(87), new FNN64(88), new FNN64(89), new FNN64(90), new FNN64(91),
            new FNN64(92), new FNN64(93), new FNN64(94), new FNN64(95), new FNN64(96), new FNN64(97), new FNN64(98),
            new FNN64(99), new FNN64(100), new FNN64(101), new FNN64(102), new FNN64(103), new FNN64(104),
            new FNN64(105), new FNN64(106), new FNN64(107), new FNN64(108), new FNN64(109), new FNN64(110), new FNN64(
                    111), new FNN64(112), new FNN64(113), new FNN64(114), new FNN64(115), new FNN64(116),
            new FNN64(117), new FNN64(118), new FNN64(119), new FNN64(120), new FNN64(121), new FNN64(122), new FNN64(
                    123), new FNN64(124), new FNN64(125), new FNN64(126), new FNN64(127), new FNN64(128),
            new FNN64(129), new FNN64(130), new FNN64(131), new FNN64(132), new FNN64(133), new FNN64(134), new FNN64(
                    135), new FNN64(136), new FNN64(137), new FNN64(138), new FNN64(139), new FNN64(140),
            new FNN64(141), new FNN64(142), new FNN64(143), new FNN64(144), new FNN64(145), new FNN64(146), new FNN64(
                    147), new FNN64(148), new FNN64(149), new FNN64(150), new FNN64(151), new FNN64(152),
            new FNN64(153), new FNN64(154), new FNN64(155), new FNN64(156), new FNN64(157), new FNN64(158), new FNN64(
                    159), new FNN64(160), new FNN64(161), new FNN64(162), new FNN64(163), new FNN64(164),
            new FNN64(165), new FNN64(166), new FNN64(167), new FNN64(168), new FNN64(169), new FNN64(170), new FNN64(
                    171), new FNN64(172), new FNN64(173), new FNN64(174), new FNN64(175), new FNN64(176),
            new FNN64(177), new FNN64(178), new FNN64(179), new FNN64(180), new FNN64(181), new FNN64(182), new FNN64(
                    183), new FNN64(184), new FNN64(185), new FNN64(186), new FNN64(187), new FNN64(188),
            new FNN64(189), new FNN64(190), new FNN64(191), new FNN64(192), new FNN64(193), new FNN64(194), new FNN64(
                    195), new FNN64(196), new FNN64(197), new FNN64(198), new FNN64(199), new FNN64(200),
            new FNN64(201), new FNN64(202), new FNN64(203), new FNN64(204), new FNN64(205), new FNN64(206), new FNN64(
                    207), new FNN64(208), new FNN64(209), new FNN64(210), new FNN64(211), new FNN64(212),
            new FNN64(213), new FNN64(214), new FNN64(215), new FNN64(216), new FNN64(217), new FNN64(218), new FNN64(
                    219), new FNN64(220), new FNN64(221), new FNN64(222), new FNN64(223), new FNN64(224),
            new FNN64(225), new FNN64(226), new FNN64(227), new FNN64(228), new FNN64(229), new FNN64(230), new FNN64(
                    231), new FNN64(232), new FNN64(233), new FNN64(234), new FNN64(235), new FNN64(236),
            new FNN64(237), new FNN64(238), new FNN64(239), new FNN64(240), new FNN64(241), new FNN64(242), new FNN64(
                    243), new FNN64(244), new FNN64(245), new FNN64(246), new FNN64(247), new FNN64(248),
            new FNN64(249), new FNN64(250), new FNN64(251), new FNN64(252), new FNN64(253), new FNN64(254), new FNN64(
                    255)
    };

    public static FNN64 make(long i) {
        if (i >= 0 && i < cached.length) {
            return cached[(int) i];
        }
        return new FNN64(i);
    }

    public static void setConstructor(NativeConstructor con) {
        // WARNING!  In order to run the tests we must reset con for
        // each new test, so it's not OK to ignore setConstructor
        // attempts after the first one.
        if (con == null) return;
        FNN64.con = con;
    }

    public NativeConstructor getConstructor() {
        return FNN64.con;
    }

    public static void resetConstructor() {
        FNN64.con = null;

    }
}
