/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.evaluator.values;
import com.sun.fortress.interpreter.evaluator.types.FBuiltinType;
import com.sun.fortress.interpreter.evaluator.types.FTypeInt;
import com.sun.fortress.useful.MagicNumbers;

public class FInt extends FBuiltinValue implements HasIntValue {
  private final int val;
  public FBuiltinType type() {return FTypeInt.ONLY;}
  public int getInt() {return val;}
  public long getLong() {return (long)val;}
  public double getFloat() { return (double) val;}
  public String getString() {return Integer.toString(val);}
  public String toString() {
      return "ZZ32 " + val;
  }
  private FInt(int x) {
    val = x;
  }

  public static final FInt ZERO = new FInt(0);

  private static final int negCached=128;

  private static FInt[] cached = {
      new FInt(-128),   new FInt(-127),   new FInt(-126),   new FInt(-125),
      new FInt(-124),   new FInt(-123),   new FInt(-122),   new FInt(-121),
      new FInt(-120),   new FInt(-119),   new FInt(-118),   new FInt(-117),
      new FInt(-116),   new FInt(-115),   new FInt(-114),   new FInt(-113),
      new FInt(-112),   new FInt(-111),   new FInt(-110),   new FInt(-109),
      new FInt(-108),   new FInt(-107),   new FInt(-106),   new FInt(-105),
      new FInt(-104),   new FInt(-103),   new FInt(-102),   new FInt(-101),
      new FInt(-100),   new FInt(-99),   new FInt(-98),   new FInt(-97),
      new FInt(-96),   new FInt(-95),   new FInt(-94),   new FInt(-93),
      new FInt(-92),   new FInt(-91),   new FInt(-90),   new FInt(-89),
      new FInt(-88),   new FInt(-87),   new FInt(-86),   new FInt(-85),
      new FInt(-84),   new FInt(-83),   new FInt(-82),   new FInt(-81),
      new FInt(-80),   new FInt(-79),   new FInt(-78),   new FInt(-77),
      new FInt(-76),   new FInt(-75),   new FInt(-74),   new FInt(-73),
      new FInt(-72),   new FInt(-71),   new FInt(-70),   new FInt(-69),
      new FInt(-68),   new FInt(-67),   new FInt(-66),   new FInt(-65),
      new FInt(-64),   new FInt(-63),   new FInt(-62),   new FInt(-61),
      new FInt(-60),   new FInt(-59),   new FInt(-58),   new FInt(-57),
      new FInt(-56),   new FInt(-55),   new FInt(-54),   new FInt(-53),
      new FInt(-52),   new FInt(-51),   new FInt(-50),   new FInt(-49),
      new FInt(-48),   new FInt(-47),   new FInt(-46),   new FInt(-45),
      new FInt(-44),   new FInt(-43),   new FInt(-42),   new FInt(-41),
      new FInt(-40),   new FInt(-39),   new FInt(-38),   new FInt(-37),
      new FInt(-36),   new FInt(-35),   new FInt(-34),   new FInt(-33),
      new FInt(-32),   new FInt(-31),   new FInt(-30),   new FInt(-29),
      new FInt(-28),   new FInt(-27),   new FInt(-26),   new FInt(-25),
      new FInt(-24),   new FInt(-23),   new FInt(-22),   new FInt(-21),
      new FInt(-20),   new FInt(-19),   new FInt(-18),   new FInt(-17),
      new FInt(-16),   new FInt(-15),   new FInt(-14),   new FInt(-13),
      new FInt(-12),   new FInt(-11),   new FInt(-10),   new FInt(-9),
      new FInt(-8),   new FInt(-7),   new FInt(-6),   new FInt(-5),
      new FInt(-4),   new FInt(-3),   new FInt(-2),   new FInt(-1),
      ZERO,   new FInt(1),   new FInt(2),   new FInt(3),
      new FInt(4),   new FInt(5),   new FInt(6),   new FInt(7),
      new FInt(8),   new FInt(9),   new FInt(10),   new FInt(11),
      new FInt(12),   new FInt(13),   new FInt(14),   new FInt(15),
      new FInt(16),   new FInt(17),   new FInt(18),   new FInt(19),
      new FInt(20),   new FInt(21),   new FInt(22),   new FInt(23),
      new FInt(24),   new FInt(25),   new FInt(26),   new FInt(27),
      new FInt(28),   new FInt(29),   new FInt(30),   new FInt(31),
      new FInt(32),   new FInt(33),   new FInt(34),   new FInt(35),
      new FInt(36),   new FInt(37),   new FInt(38),   new FInt(39),
      new FInt(40),   new FInt(41),   new FInt(42),   new FInt(43),
      new FInt(44),   new FInt(45),   new FInt(46),   new FInt(47),
      new FInt(48),   new FInt(49),   new FInt(50),   new FInt(51),
      new FInt(52),   new FInt(53),   new FInt(54),   new FInt(55),
      new FInt(56),   new FInt(57),   new FInt(58),   new FInt(59),
      new FInt(60),   new FInt(61),   new FInt(62),   new FInt(63),
      new FInt(64),   new FInt(65),   new FInt(66),   new FInt(67),
      new FInt(68),   new FInt(69),   new FInt(70),   new FInt(71),
      new FInt(72),   new FInt(73),   new FInt(74),   new FInt(75),
      new FInt(76),   new FInt(77),   new FInt(78),   new FInt(79),
      new FInt(80),   new FInt(81),   new FInt(82),   new FInt(83),
      new FInt(84),   new FInt(85),   new FInt(86),   new FInt(87),
      new FInt(88),   new FInt(89),   new FInt(90),   new FInt(91),
      new FInt(92),   new FInt(93),   new FInt(94),   new FInt(95),
      new FInt(96),   new FInt(97),   new FInt(98),   new FInt(99),
      new FInt(100),   new FInt(101),   new FInt(102),   new FInt(103),
      new FInt(104),   new FInt(105),   new FInt(106),   new FInt(107),
      new FInt(108),   new FInt(109),   new FInt(110),   new FInt(111),
      new FInt(112),   new FInt(113),   new FInt(114),   new FInt(115),
      new FInt(116),   new FInt(117),   new FInt(118),   new FInt(119),
      new FInt(120),   new FInt(121),   new FInt(122),   new FInt(123),
      new FInt(124),   new FInt(125),   new FInt(126),   new FInt(127),
      new FInt(128),   new FInt(129),   new FInt(130),   new FInt(131),
      new FInt(132),   new FInt(133),   new FInt(134),   new FInt(135),
      new FInt(136),   new FInt(137),   new FInt(138),   new FInt(139),
      new FInt(140),   new FInt(141),   new FInt(142),   new FInt(143),
      new FInt(144),   new FInt(145),   new FInt(146),   new FInt(147),
      new FInt(148),   new FInt(149),   new FInt(150),   new FInt(151),
      new FInt(152),   new FInt(153),   new FInt(154),   new FInt(155),
      new FInt(156),   new FInt(157),   new FInt(158),   new FInt(159),
      new FInt(160),   new FInt(161),   new FInt(162),   new FInt(163),
      new FInt(164),   new FInt(165),   new FInt(166),   new FInt(167),
      new FInt(168),   new FInt(169),   new FInt(170),   new FInt(171),
      new FInt(172),   new FInt(173),   new FInt(174),   new FInt(175),
      new FInt(176),   new FInt(177),   new FInt(178),   new FInt(179),
      new FInt(180),   new FInt(181),   new FInt(182),   new FInt(183),
      new FInt(184),   new FInt(185),   new FInt(186),   new FInt(187),
      new FInt(188),   new FInt(189),   new FInt(190),   new FInt(191),
      new FInt(192),   new FInt(193),   new FInt(194),   new FInt(195),
      new FInt(196),   new FInt(197),   new FInt(198),   new FInt(199),
      new FInt(200),   new FInt(201),   new FInt(202),   new FInt(203),
      new FInt(204),   new FInt(205),   new FInt(206),   new FInt(207),
      new FInt(208),   new FInt(209),   new FInt(210),   new FInt(211),
      new FInt(212),   new FInt(213),   new FInt(214),   new FInt(215),
      new FInt(216),   new FInt(217),   new FInt(218),   new FInt(219),
      new FInt(220),   new FInt(221),   new FInt(222),   new FInt(223),
      new FInt(224),   new FInt(225),   new FInt(226),   new FInt(227),
      new FInt(228),   new FInt(229),   new FInt(230),   new FInt(231),
      new FInt(232),   new FInt(233),   new FInt(234),   new FInt(235),
      new FInt(236),   new FInt(237),   new FInt(238),   new FInt(239),
      new FInt(240),   new FInt(241),   new FInt(242),   new FInt(243),
      new FInt(244),   new FInt(245),   new FInt(246),   new FInt(247),
      new FInt(248),   new FInt(249),   new FInt(250),   new FInt(251),
      new FInt(252),   new FInt(253),   new FInt(254),   new FInt(255)
  };

  public static FInt make(int i) {
      int co = i + negCached;
      if (co >= 0 && co < cached.length) {
          return cached[co];
      }
      return new FInt(i);
  }

}
