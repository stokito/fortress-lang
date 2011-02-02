/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

public class FLong extends FNativeObject implements HasIntValue {
    private final long val;
    private static volatile NativeConstructor con;

    private FLong(long x) {
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
        return (long) val;
    }

    public long getNN64() {
        // TODO Throw an error on out-of-range conversion?
        return (long) val;
    }

    public double getFloat() {
        return (double) val;
    }

    public String getString() {
        return Long.toString(val);
    }

    public String toString() {
        return val + ": ZZ64";
    }

    public boolean seqv(FValue v) {
        if (!(v instanceof FNativeObject)) return false;
        if (v instanceof FLong || v instanceof FInt) {
            return (getLong() == v.getLong());
        }
        if (v instanceof FFloat || v instanceof FFloatLiteral) {
            return (getFloat() == v.getFloat());
        }
        return false;
    }

    static final long negCached = 128;

    public static final FLong ZERO = new FLong(0);

    static final FLong[] cached = {
            new FLong(-128), new FLong(-127), new FLong(-126), new FLong(-125), new FLong(-124), new FLong(-123),
            new FLong(-122), new FLong(-121), new FLong(-120), new FLong(-119), new FLong(-118), new FLong(-117),
            new FLong(-116), new FLong(-115), new FLong(-114), new FLong(-113), new FLong(-112), new FLong(-111),
            new FLong(-110), new FLong(-109), new FLong(-108), new FLong(-107), new FLong(-106), new FLong(-105),
            new FLong(-104), new FLong(-103), new FLong(-102), new FLong(-101), new FLong(-100), new FLong(-99),
            new FLong(-98), new FLong(-97), new FLong(-96), new FLong(-95), new FLong(-94), new FLong(-93),
            new FLong(-92), new FLong(-91), new FLong(-90), new FLong(-89), new FLong(-88), new FLong(-87),
            new FLong(-86), new FLong(-85), new FLong(-84), new FLong(-83), new FLong(-82), new FLong(-81),
            new FLong(-80), new FLong(-79), new FLong(-78), new FLong(-77), new FLong(-76), new FLong(-75),
            new FLong(-74), new FLong(-73), new FLong(-72), new FLong(-71), new FLong(-70), new FLong(-69),
            new FLong(-68), new FLong(-67), new FLong(-66), new FLong(-65), new FLong(-64), new FLong(-63),
            new FLong(-62), new FLong(-61), new FLong(-60), new FLong(-59), new FLong(-58), new FLong(-57),
            new FLong(-56), new FLong(-55), new FLong(-54), new FLong(-53), new FLong(-52), new FLong(-51),
            new FLong(-50), new FLong(-49), new FLong(-48), new FLong(-47), new FLong(-46), new FLong(-45),
            new FLong(-44), new FLong(-43), new FLong(-42), new FLong(-41), new FLong(-40), new FLong(-39),
            new FLong(-38), new FLong(-37), new FLong(-36), new FLong(-35), new FLong(-34), new FLong(-33),
            new FLong(-32), new FLong(-31), new FLong(-30), new FLong(-29), new FLong(-28), new FLong(-27),
            new FLong(-26), new FLong(-25), new FLong(-24), new FLong(-23), new FLong(-22), new FLong(-21),
            new FLong(-20), new FLong(-19), new FLong(-18), new FLong(-17), new FLong(-16), new FLong(-15),
            new FLong(-14), new FLong(-13), new FLong(-12), new FLong(-11), new FLong(-10), new FLong(-9),
            new FLong(-8), new FLong(-7), new FLong(-6), new FLong(-5), new FLong(-4), new FLong(-3), new FLong(-2),
            new FLong(-1), ZERO, new FLong(1), new FLong(2), new FLong(3), new FLong(4), new FLong(5), new FLong(6),
            new FLong(7), new FLong(8), new FLong(9), new FLong(10), new FLong(11), new FLong(12), new FLong(13),
            new FLong(14), new FLong(15), new FLong(16), new FLong(17), new FLong(18), new FLong(19), new FLong(20),
            new FLong(21), new FLong(22), new FLong(23), new FLong(24), new FLong(25), new FLong(26), new FLong(27),
            new FLong(28), new FLong(29), new FLong(30), new FLong(31), new FLong(32), new FLong(33), new FLong(34),
            new FLong(35), new FLong(36), new FLong(37), new FLong(38), new FLong(39), new FLong(40), new FLong(41),
            new FLong(42), new FLong(43), new FLong(44), new FLong(45), new FLong(46), new FLong(47), new FLong(48),
            new FLong(49), new FLong(50), new FLong(51), new FLong(52), new FLong(53), new FLong(54), new FLong(55),
            new FLong(56), new FLong(57), new FLong(58), new FLong(59), new FLong(60), new FLong(61), new FLong(62),
            new FLong(63), new FLong(64), new FLong(65), new FLong(66), new FLong(67), new FLong(68), new FLong(69),
            new FLong(70), new FLong(71), new FLong(72), new FLong(73), new FLong(74), new FLong(75), new FLong(76),
            new FLong(77), new FLong(78), new FLong(79), new FLong(80), new FLong(81), new FLong(82), new FLong(83),
            new FLong(84), new FLong(85), new FLong(86), new FLong(87), new FLong(88), new FLong(89), new FLong(90),
            new FLong(91), new FLong(92), new FLong(93), new FLong(94), new FLong(95), new FLong(96), new FLong(97),
            new FLong(98), new FLong(99), new FLong(100), new FLong(101), new FLong(102), new FLong(103),
            new FLong(104), new FLong(105), new FLong(106), new FLong(107), new FLong(108), new FLong(109), new FLong(
                    110), new FLong(111), new FLong(112), new FLong(113), new FLong(114), new FLong(115),
            new FLong(116), new FLong(117), new FLong(118), new FLong(119), new FLong(120), new FLong(121), new FLong(
                    122), new FLong(123), new FLong(124), new FLong(125), new FLong(126), new FLong(127),
            new FLong(128), new FLong(129), new FLong(130), new FLong(131), new FLong(132), new FLong(133), new FLong(
                    134), new FLong(135), new FLong(136), new FLong(137), new FLong(138), new FLong(139),
            new FLong(140), new FLong(141), new FLong(142), new FLong(143), new FLong(144), new FLong(145), new FLong(
                    146), new FLong(147), new FLong(148), new FLong(149), new FLong(150), new FLong(151),
            new FLong(152), new FLong(153), new FLong(154), new FLong(155), new FLong(156), new FLong(157), new FLong(
                    158), new FLong(159), new FLong(160), new FLong(161), new FLong(162), new FLong(163),
            new FLong(164), new FLong(165), new FLong(166), new FLong(167), new FLong(168), new FLong(169), new FLong(
                    170), new FLong(171), new FLong(172), new FLong(173), new FLong(174), new FLong(175),
            new FLong(176), new FLong(177), new FLong(178), new FLong(179), new FLong(180), new FLong(181), new FLong(
                    182), new FLong(183), new FLong(184), new FLong(185), new FLong(186), new FLong(187),
            new FLong(188), new FLong(189), new FLong(190), new FLong(191), new FLong(192), new FLong(193), new FLong(
                    194), new FLong(195), new FLong(196), new FLong(197), new FLong(198), new FLong(199),
            new FLong(200), new FLong(201), new FLong(202), new FLong(203), new FLong(204), new FLong(205), new FLong(
                    206), new FLong(207), new FLong(208), new FLong(209), new FLong(210), new FLong(211),
            new FLong(212), new FLong(213), new FLong(214), new FLong(215), new FLong(216), new FLong(217), new FLong(
                    218), new FLong(219), new FLong(220), new FLong(221), new FLong(222), new FLong(223),
            new FLong(224), new FLong(225), new FLong(226), new FLong(227), new FLong(228), new FLong(229), new FLong(
                    230), new FLong(231), new FLong(232), new FLong(233), new FLong(234), new FLong(235),
            new FLong(236), new FLong(237), new FLong(238), new FLong(239), new FLong(240), new FLong(241), new FLong(
                    242), new FLong(243), new FLong(244), new FLong(245), new FLong(246), new FLong(247),
            new FLong(248), new FLong(249), new FLong(250), new FLong(251), new FLong(252), new FLong(253), new FLong(
                    254), new FLong(255)
    };

    public static FLong make(long i) {
        long co = i + negCached;
        if (co >= 0 && co < cached.length) {
            return cached[(int) co];
        }
        return new FLong(i);
    }

    public static void setConstructor(NativeConstructor con) {
        // WARNING!  In order to run the tests we must reset con for
        // each new test, so it's not OK to ignore setConstructor
        // attempts after the first one.
        if (con == null) return;
        FLong.con = con;
    }

    public NativeConstructor getConstructor() {
        return FLong.con;
    }

    public static void resetConstructor() {
        FLong.con = null;
    }
}
