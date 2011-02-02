/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.naturalbridge.misc.Unsigned;


public class FNN32 extends FNativeObject implements HasIntValue {
    private final int val;
    private static volatile NativeConstructor con;

    private FNN32(int x) {
        super(null);
        val = x;
    }

    public int getInt() {
        // TODO Throw an error on out-of-range conversion?
        return (int) val;
    }

    public long getLong() {
        return Unsigned.toLong(val);
    }

    public int getNN32() {
        return val;
    }

    public long getNN64() {
        return Unsigned.toLong(val);
    }

    public double getFloat() {
        return Unsigned.toDouble(val);
    }

    public String getString() {
        return Unsigned.toString(val);
    }

    public String toString() {
        return getString() + ": NN32";
    }

    public boolean seqv(FValue v) {
        if (!(v instanceof FNativeObject)) return false;
        if (v instanceof FNN32) {
            return (getNN32() == v.getNN32());
        }
        if (v instanceof FNN64) {
            return (getNN64() == v.getNN64());
        }
        return false;
    }

    public static final FNN32 ZERO = new FNN32(0);


    private static FNN32[] cached = {
            ZERO, new FNN32(1), new FNN32(2), new FNN32(3), new FNN32(4), new FNN32(5), new FNN32(6), new FNN32(7),
            new FNN32(8), new FNN32(9), new FNN32(10), new FNN32(11), new FNN32(12), new FNN32(13), new FNN32(14),
            new FNN32(15), new FNN32(16), new FNN32(17), new FNN32(18), new FNN32(19), new FNN32(20), new FNN32(21),
            new FNN32(22), new FNN32(23), new FNN32(24), new FNN32(25), new FNN32(26), new FNN32(27), new FNN32(28),
            new FNN32(29), new FNN32(30), new FNN32(31), new FNN32(32), new FNN32(33), new FNN32(34), new FNN32(35),
            new FNN32(36), new FNN32(37), new FNN32(38), new FNN32(39), new FNN32(40), new FNN32(41), new FNN32(42),
            new FNN32(43), new FNN32(44), new FNN32(45), new FNN32(46), new FNN32(47), new FNN32(48), new FNN32(49),
            new FNN32(50), new FNN32(51), new FNN32(52), new FNN32(53), new FNN32(54), new FNN32(55), new FNN32(56),
            new FNN32(57), new FNN32(58), new FNN32(59), new FNN32(60), new FNN32(61), new FNN32(62), new FNN32(63),
            new FNN32(64), new FNN32(65), new FNN32(66), new FNN32(67), new FNN32(68), new FNN32(69), new FNN32(70),
            new FNN32(71), new FNN32(72), new FNN32(73), new FNN32(74), new FNN32(75), new FNN32(76), new FNN32(77),
            new FNN32(78), new FNN32(79), new FNN32(80), new FNN32(81), new FNN32(82), new FNN32(83), new FNN32(84),
            new FNN32(85), new FNN32(86), new FNN32(87), new FNN32(88), new FNN32(89), new FNN32(90), new FNN32(91),
            new FNN32(92), new FNN32(93), new FNN32(94), new FNN32(95), new FNN32(96), new FNN32(97), new FNN32(98),
            new FNN32(99), new FNN32(100), new FNN32(101), new FNN32(102), new FNN32(103), new FNN32(104),
            new FNN32(105), new FNN32(106), new FNN32(107), new FNN32(108), new FNN32(109), new FNN32(110), new FNN32(
                    111), new FNN32(112), new FNN32(113), new FNN32(114), new FNN32(115), new FNN32(116),
            new FNN32(117), new FNN32(118), new FNN32(119), new FNN32(120), new FNN32(121), new FNN32(122), new FNN32(
                    123), new FNN32(124), new FNN32(125), new FNN32(126), new FNN32(127), new FNN32(128),
            new FNN32(129), new FNN32(130), new FNN32(131), new FNN32(132), new FNN32(133), new FNN32(134), new FNN32(
                    135), new FNN32(136), new FNN32(137), new FNN32(138), new FNN32(139), new FNN32(140),
            new FNN32(141), new FNN32(142), new FNN32(143), new FNN32(144), new FNN32(145), new FNN32(146), new FNN32(
                    147), new FNN32(148), new FNN32(149), new FNN32(150), new FNN32(151), new FNN32(152),
            new FNN32(153), new FNN32(154), new FNN32(155), new FNN32(156), new FNN32(157), new FNN32(158), new FNN32(
                    159), new FNN32(160), new FNN32(161), new FNN32(162), new FNN32(163), new FNN32(164),
            new FNN32(165), new FNN32(166), new FNN32(167), new FNN32(168), new FNN32(169), new FNN32(170), new FNN32(
                    171), new FNN32(172), new FNN32(173), new FNN32(174), new FNN32(175), new FNN32(176),
            new FNN32(177), new FNN32(178), new FNN32(179), new FNN32(180), new FNN32(181), new FNN32(182), new FNN32(
                    183), new FNN32(184), new FNN32(185), new FNN32(186), new FNN32(187), new FNN32(188),
            new FNN32(189), new FNN32(190), new FNN32(191), new FNN32(192), new FNN32(193), new FNN32(194), new FNN32(
                    195), new FNN32(196), new FNN32(197), new FNN32(198), new FNN32(199), new FNN32(200),
            new FNN32(201), new FNN32(202), new FNN32(203), new FNN32(204), new FNN32(205), new FNN32(206), new FNN32(
                    207), new FNN32(208), new FNN32(209), new FNN32(210), new FNN32(211), new FNN32(212),
            new FNN32(213), new FNN32(214), new FNN32(215), new FNN32(216), new FNN32(217), new FNN32(218), new FNN32(
                    219), new FNN32(220), new FNN32(221), new FNN32(222), new FNN32(223), new FNN32(224),
            new FNN32(225), new FNN32(226), new FNN32(227), new FNN32(228), new FNN32(229), new FNN32(230), new FNN32(
                    231), new FNN32(232), new FNN32(233), new FNN32(234), new FNN32(235), new FNN32(236),
            new FNN32(237), new FNN32(238), new FNN32(239), new FNN32(240), new FNN32(241), new FNN32(242), new FNN32(
                    243), new FNN32(244), new FNN32(245), new FNN32(246), new FNN32(247), new FNN32(248),
            new FNN32(249), new FNN32(250), new FNN32(251), new FNN32(252), new FNN32(253), new FNN32(254), new FNN32(
                    255)
    };

    public static FNN32 make(int i) {
        if (i >= 0 && i < cached.length) {
            return cached[i];
        }
        return new FNN32(i);
    }

    public static void setConstructor(NativeConstructor con) {
        // WARNING!  In order to run the tests we must reset con for
        // each new test, so it's not OK to ignore setConstructor
        // attempts after the first one.
        if (con == null) return;
        FNN32.con = con;
    }

    public NativeConstructor getConstructor() {
        return FNN32.con;
    }

    public static void resetConstructor() {
        FNN32.con = null;

    }
}
