/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.nodes_util.Unprinter;

public class FChar extends FNativeObject {
    private static volatile NativeConstructor con;

    // Fortress chars are not equivalent to Java chars.
    // Fortress supports 21-bit Unicode, so we use int to represent char instead of just Java char.
    private final int val;

    public static final FChar ZERO = new FChar(0);

    private static FChar[] cached = {
            ZERO, new FChar(1), new FChar(2), new FChar(3), new FChar(4), new FChar(5), new FChar(6), new FChar(7),
            new FChar(8), new FChar(9), new FChar(10), new FChar(11), new FChar(12), new FChar(13), new FChar(14),
            new FChar(15), new FChar(16), new FChar(17), new FChar(18), new FChar(19), new FChar(20), new FChar(21),
            new FChar(22), new FChar(23), new FChar(24), new FChar(25), new FChar(26), new FChar(27), new FChar(28),
            new FChar(29), new FChar(30), new FChar(31), new FChar(32), new FChar(33), new FChar(34), new FChar(35),
            new FChar(36), new FChar(37), new FChar(38), new FChar(39), new FChar(40), new FChar(41), new FChar(42),
            new FChar(43), new FChar(44), new FChar(45), new FChar(46), new FChar(47), new FChar(48), new FChar(49),
            new FChar(50), new FChar(51), new FChar(52), new FChar(53), new FChar(54), new FChar(55), new FChar(56),
            new FChar(57), new FChar(58), new FChar(59), new FChar(60), new FChar(61), new FChar(62), new FChar(63),
            new FChar(64), new FChar(65), new FChar(66), new FChar(67), new FChar(68), new FChar(69), new FChar(70),
            new FChar(71), new FChar(72), new FChar(73), new FChar(74), new FChar(75), new FChar(76), new FChar(77),
            new FChar(78), new FChar(79), new FChar(80), new FChar(81), new FChar(82), new FChar(83), new FChar(84),
            new FChar(85), new FChar(86), new FChar(87), new FChar(88), new FChar(89), new FChar(90), new FChar(91),
            new FChar(92), new FChar(93), new FChar(94), new FChar(95), new FChar(96), new FChar(97), new FChar(98),
            new FChar(99), new FChar(100), new FChar(101), new FChar(102), new FChar(103), new FChar(104),
            new FChar(105), new FChar(106), new FChar(107), new FChar(108), new FChar(109), new FChar(110), new FChar(
                    111), new FChar(112), new FChar(113), new FChar(114), new FChar(115), new FChar(116),
            new FChar(117), new FChar(118), new FChar(119), new FChar(120), new FChar(121), new FChar(122), new FChar(
                    123), new FChar(124), new FChar(125), new FChar(126), new FChar(127), new FChar(128),
            new FChar(129), new FChar(130), new FChar(131), new FChar(132), new FChar(133), new FChar(134), new FChar(
                    135), new FChar(136), new FChar(137), new FChar(138), new FChar(139), new FChar(140),
            new FChar(141), new FChar(142), new FChar(143), new FChar(144), new FChar(145), new FChar(146), new FChar(
                    147), new FChar(148), new FChar(149), new FChar(150), new FChar(151), new FChar(152),
            new FChar(153), new FChar(154), new FChar(155), new FChar(156), new FChar(157), new FChar(158), new FChar(
                    159), new FChar(160), new FChar(161), new FChar(162), new FChar(163), new FChar(164),
            new FChar(165), new FChar(166), new FChar(167), new FChar(168), new FChar(169), new FChar(170), new FChar(
                    171), new FChar(172), new FChar(173), new FChar(174), new FChar(175), new FChar(176),
            new FChar(177), new FChar(178), new FChar(179), new FChar(180), new FChar(181), new FChar(182), new FChar(
                    183), new FChar(184), new FChar(185), new FChar(186), new FChar(187), new FChar(188),
            new FChar(189), new FChar(190), new FChar(191), new FChar(192), new FChar(193), new FChar(194), new FChar(
                    195), new FChar(196), new FChar(197), new FChar(198), new FChar(199), new FChar(200),
            new FChar(201), new FChar(202), new FChar(203), new FChar(204), new FChar(205), new FChar(206), new FChar(
                    207), new FChar(208), new FChar(209), new FChar(210), new FChar(211), new FChar(212),
            new FChar(213), new FChar(214), new FChar(215), new FChar(216), new FChar(217), new FChar(218), new FChar(
                    219), new FChar(220), new FChar(221), new FChar(222), new FChar(223), new FChar(224),
            new FChar(225), new FChar(226), new FChar(227), new FChar(228), new FChar(229), new FChar(230), new FChar(
                    231), new FChar(232), new FChar(233), new FChar(234), new FChar(235), new FChar(236),
            new FChar(237), new FChar(238), new FChar(239), new FChar(240), new FChar(241), new FChar(242), new FChar(
                    243), new FChar(244), new FChar(245), new FChar(246), new FChar(247), new FChar(248),
            new FChar(249), new FChar(250), new FChar(251), new FChar(252), new FChar(253), new FChar(254), new FChar(
                    255)
    };

    FChar(int x) {
        super(null);
        val = x;
    }

    static public FChar make(int x) {
        if (0 <= x && x < cached.length) {
            return cached[x];
        }
        return new FChar(x);
    }

    public static void setConstructor(NativeConstructor con) {
        // WARNING!  In order to run the tests we must reset con for
        // each new test, so it's not OK to ignore setConstructor
        // attempts after the first one.
        if (con == null) return;
        FChar.con = con;
    }

    public NativeConstructor getConstructor() {
        return FChar.con;
    }

    public int getChar() {
        return val;
    }

    /*
     * This needs to get fixed; right now val is an int type, but the String.valueOf only handles 16-bit chars
     */
    public String getString() {
        return String.valueOf((char) val);
    }

    public String toString() {
        return "'" + Unprinter.enQuote(getString()) + "'";
    }

    public boolean seqv(FValue v) {
        if (!(v instanceof FChar)) return false;
        return getChar() == ((FChar) v).getChar();
    }

    public static void resetConstructor() {
        FChar.con = null;
    }
}
