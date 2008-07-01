/************************************************************
Copyright 2008, NaturalBridge, Inc.
All rights reserved.

Redistribution and use in source and binary forms, with
or without modification, are permitted provided that
the following conditions are met:

* Redistributions of source code must retain the above
copyright notice, this list of conditions and the
following disclaimer.
* Redistributions in binary form must reproduce the
above copyright notice, this list of conditions and the
following  disclaimer in the documentation and/or other
materials provided with the distribution.
* Neither the name of the NaturalBridge, Inc. nor
the names of its contributors may be used to endorse or
promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
************************************************************/

package com.sun.fortress.numerics;

/**

 The <code>Unsigned</code> class provides static methods useful for
 interpreting Java <code>int</code> values as if they were unsigned
 numbers.

 <p>
 Note that many of the operations are already compatible
 because Java ints are stored in twos-complement representation;
 thus, there is no need for
 <ul>
 <li> unsigned addition
 <li> unsigned subtraction
 <li> unsigned type-times-type-into-same-type multiplication
 </ul>
 However, methods are provided so that it is not necessary
 to remember this.

 <p>
 Note the importance of this class not having any initialization,
 either in itself or its superclasses, so that a compiler might (in
 some better future world) replace calls to these routines with the
 obvious inline code.

 <p>

 @author David Chase chase@naturalbridge.com
 @version 0.96, 28 Jul 1999

 <p>

 Note that the version incorporated into fortress.Numerics has been
 given a package label to match (as this was stripped from the
 BSD-licensed source).  Thanks go to NaturalBridge for agreeing to
 re-license this code under BSD.

 */

public final class Unsigned {

 private Unsigned() {}

 /**
   Two's complement integer representation
   makes this method unnecessary, but there's
   no need to force people to remember special
   cases.
   */
 public static int add(int x, int y) {
   return x+y;
 }
 /**
   Two's complement integer representation
   makes this method unnecessary, but there's
   no need to force people to remember special
   cases.
   */
 public static int subtract(int x, int y) {
   return x-y;
 }

 /**
   Two's complement integer representation
   makes this method unnecessary, but there's
   no need to force people to remember special
   cases.
   */
 public static int multiplyToInt(int x, int y) {
   return x*y;
 }

 public static int multiplyToInt(short x, short y) {
   return ((int)x&0xffff)*((int)y&0xffff);
 }

 public static int multiplyToInt(byte x, byte y) {
   return ((int)x&0xff)*((int)y&0xff);
 }

 public static int fromByte(byte x) {
   return (int) x & 0xFF;
 }

 public static int fromShort(short x) {
   return (int) x & 0xFFFF;
 }

 /**
   Two's complement integer representation
   makes this method unnecessary, but there's
   no need to force people to remember special
   cases.
   */
 public static int fromChar(char x) {
   return (int) x;
 }

 public static long fromInt(int x) {
   return toLong(x);
 }

 /**
   Two's complement integer representation
   makes this method unnecessary, but there's
   no need to force people to remember special
   cases.
   */
 public static long add(long x, long y) {
   return x+y;
 }
 /**
   Two's complement integer representation
   makes this method unnecessary, but there's
   no need to force people to remember special
   cases.
   */
 public static long subtract(long x, long y) {
   return x-y;
 }

 /**
   Two's complement integer representation
   makes this method unnecessary, but there's
   no need to force people to remember special
   cases.
   */

 public static long multiplyToLong(long x, long y) {
   return x*y;
 }


 private static final class Constants {

   /* When converting, ensure that the result stays
      in range.  For each radix r, these are the
      largest values v[r] such that r*v[r] does not
      overflow. */

   static int intMultMinima[] = { 0, 0, 2147483647, 1431655765, 1073741823,
				   858993459, 715827882, 613566756, 536870911,
				   477218588, 429496729, 390451572, 357913941, 330382099, 306783378, 286331153, 268435455,
				   252645135, 238609294, 226050910, 214748364, 204522252, 195225786,
				   186737708, 178956970, 171798691, 165191049, 159072862, 153391689,
				   148102320, 143165576, 138547332, 134217727, 130150524, 126322567,
				   122713351, 119304647};

   static long longMultMinima[] = { 0L, 0L, 9223372036854775807L,
				     6148914691236517205L, 4611686018427387903L, 3689348814741910323L,
				     3074457345618258602L, 2635249153387078802L, 2305843009213693951L,
				     2049638230412172401L, 1844674407370955161L, 1676976733973595601L,
				     1537228672809129301L, 1418980313362273201L, 1317624576693539401L,
				     1229782938247303441L, 1152921504606846975L, 1085102592571150095L,
				     1024819115206086200L, 970881267037344821L, 922337203685477580L,
				     878416384462359600L, 838488366986797800L, 802032351030850070L,
				     768614336404564650L, 737869762948382064L, 709490156681136600L,
				     683212743470724133L, 658812288346769700L, 636094623231363848L,
				     614891469123651720L, 595056260442243600L, 576460752303423487L,
				     558992244657865200L, 542551296285575047L, 527049830677415760L,
				     512409557603043100L};

   static int intLastLegalDigit[] = { 0, 0, 1, 0, 3, 0, 3, 3, 7, 3, 5, 3, 3, 8,
				       3, 0, 15, 0, 3, 5, 15, 3, 3, 11, 15, 20, 21, 21, 3, 15, 15, 3, 31, 3,
				       17, 10, 3 };

   static int longLastLegalDigit[] = { 0, 0, 1, 0, 3, 0, 3, 1, 7, 6, 5, 4, 3, 2,
					1, 0, 15, 0, 15, 16, 15, 15, 15, 5, 15, 15, 15, 24, 15, 23, 15, 15,
					31, 15, 17, 15, 15 };

   /* The length in bits of a leading digit. */
   static final byte BITLEN[] = {
     0, 1,
     2, 2,
     3, 3, 3, 3,
     4, 4, 4, 4, 4, 4, 4, 4,
     5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 };


   static final char digits[] = {
     '0','1','2','3','4','5','6','7','8','9','a','b',
     'c','d','e','f','g','h','i','j','k','l','m','n',
     'o','p','q','r','s','t','u','v','w','x','y','z'
   };

   static final char digits64[] = {
     'A','B','C','D','E','F','G','H',
     'I','J','K','L','M','N','O','P',
     'Q','R','S','T','U','V','W','X',
     'Y','Z','a','b','c','d','e','f',
     'g','h','i','j','k','l','m','n',
     'o','p','q','r','s','t','u','v',
     'w','x','y','z','0','1','2','3',
     '4','5','6','7','8','9','+','/'
   };

 }

 /**
   This method provides an unsigned (overflow to negative allowed)
   version of Integer.decode(String s).
  */
 public static int decodeInt(String s) {
   return decodeInt(s, 10, true);
 }

 /**
   This method provides an unsigned variant
   version of Integer.decode(String s), that
   takes a default radix, but does NOT treat
   a leading zero as signalling an octal input,
   because that causes confusion more often
   than it helps.
  */
 public static int decodeInt(String s, int radix) {
   return decodeInt(s, radix, false);
 }

 public static int decodeInt(String s, int radix, boolean leadingZeroMeansOctal) {
   int l = s.length();
   if (l == 0) throw new NumberFormatException("Decode of empty string");
   char c = s.charAt(0);
   boolean isNeg = false;
   int at = 0;

   if (c == '-') {
     l--;
     if (l == 0) throw new NumberFormatException("Decode of -empty string");
     isNeg = true;
     at ++;
     c = s.charAt(at);
   }

   if (c == '0') {
     l--;
     if (l == 0) return 0;
     at++;
     c = s.charAt(at);
     if (c == 'x') {
	l--;
	if (l == 0) throw new NumberFormatException("Decode of '0x' string");
	at++;
	radix = 16;
     } else if (leadingZeroMeansOctal) {
	radix = 8;
     }
   } else if (c == '#') {
     l--;
     if (l == 0) throw new NumberFormatException("Decode of '#' string");
     at++;
     radix = 16;
   }

   int result = parseInt(s.substring(at), radix);
   if (isNeg) result = -result;
   return result;
 }

 /**
   This method provides an unsigned (overflow to negative allowed)
   version of Integer.decode(String s).
  */

 public static long decodeLong(String s) {
   return decodeLong(s, 10, true);
 }

 /**
   This method provides an unsigned variant
   version of Long.decode(String s), that
   takes a default radix, but does NOT treat
   a leading zero as signalling an octal input,
   because that causes confusion more often
   than it helps.
  */
 public static long decodeLong(String s, int radix) {
   return decodeLong(s, radix, false);
 }
 public static long decodeLong(String s, int radix, boolean leadingZeroMeansOctal) {
   int l = s.length();
   if (l == 0) throw new NumberFormatException("Decode of empty string");
   char c = s.charAt(0);
   boolean isNeg = false;
   int at = 0;

   if (c == '-') {
     l--;
     if (l == 0) throw new NumberFormatException("Decode of -empty string");
     isNeg = true;
     at ++;
     c = s.charAt(at);
   }

   if (c == '0') {
     l--;
     if (l == 0) return 0;
     at++;
     c = s.charAt(at);
     if (c == 'x') {
	l--;
	if (l == 0) throw new NumberFormatException("Decode of '0x' string");
	at++;
	radix = 16;
     } else if (leadingZeroMeansOctal) {
	radix = 8;
     }
   } else if (c == '#') {
     l--;
     if (l == 0) throw new NumberFormatException("Decode of '#' string");
     at++;
     radix = 16;
   }

   long result = parseLong(s.substring(at), radix);
   if (isNeg) result = -result;
   return result;
 }

 public static int parseInt(String s, int radix) {
   if (s == null) throw new NumberFormatException("null");

   int strLen = s.length();
   if (strLen == 0) throw new NumberFormatException(s);

   if ((radix < Character.MIN_RADIX) || (radix > Character.MAX_RADIX)) {
     throw new NumberFormatException("invalid radix " + radix);
   }

   switch (radix) {
   case 2:
     return parseIntP2(s, radix, 1);
   case 4:
     return parseIntP2(s, radix, 2);
   case 8:
     return parseIntP2(s, radix, 3);
   case 16:
     return parseIntP2(s, radix, 4);
   case 32:
     return parseIntP2(s, radix, 5);
   }

   int i = 0;
   int result = 0;
   int digit;
   int multMin = Constants.intMultMinima[radix];

   while (i < strLen) {
     digit = Character.digit(s.charAt(i++),radix);
     if (digit < 0) throw new NumberFormatException(s);
     if (result < 0) throw new NumberFormatException(s);
     if (result >= multMin) {
	if (result > multMin ||
	    result == multMin &&
	    digit > Constants.intLastLegalDigit[radix]) throw new NumberFormatException(s);
     }
     result = result * radix + digit;
   }

   return result;
 }

 public static long parseLong(String s, int radix) {
   if (s == null) throw new NumberFormatException("null");

   int strLen = s.length();
   if (strLen == 0) throw new NumberFormatException(s);

   if ((radix < Character.MIN_RADIX) || (radix > Character.MAX_RADIX)) {
     throw new NumberFormatException("invalid radix " + radix);
   }

   switch (radix) {
   case 2:
     return parseLongP2(s, radix, 1);
   case 4:
     return parseLongP2(s, radix, 2);
   case 8:
     return parseLongP2(s, radix, 3);
   case 16:
     return parseLongP2(s, radix, 4);
   case 32:
     return parseLongP2(s, radix, 5);
   }

   int i = 0;
   long result = 0;
   int digit;
   long multMin = Constants.longMultMinima[radix];

   while (i < strLen) {
     digit = Character.digit(s.charAt(i++),radix);
     if (digit < 0) throw new NumberFormatException(s);
     if (result < 0) throw new NumberFormatException(s);
     if (result >= multMin) {
	if (result > multMin ||
	    result == multMin &&
	    digit > Constants.longLastLegalDigit[radix]) throw new NumberFormatException(s);
     }
     result = result * radix + digit;
   }

   return result;
 }

 public static int parseInt(String s) {
   return parseInt(s,10);
 }

 public static long parseLong(String s) {
   return parseLong(s,10);
 }

 private static long parseLongP2(String s, int radix, int logRadix)
      throws NumberFormatException
 {
   int strLen = s.length();

   long result = 0;
   int i = 0;
   int bitLen = 0;

   while((i < strLen) && (s.charAt(i) == '0')) {i++;};

   if (i < strLen) {
     int digit = Character.digit(s.charAt(i++),radix);
     if (digit < 0) throw new NumberFormatException(s);
     result = digit;
     bitLen = Constants.BITLEN[digit];
   }

   while (i < strLen) {
     int digit = Character.digit(s.charAt(i++),radix);
     if (digit < 0) throw new NumberFormatException(s);
     result = (result << logRadix) + digit;
     bitLen += logRadix;
     if (bitLen > 64) throw new NumberFormatException(s);
   }

   return result;
 }

 private static int parseIntP2(String s, int radix, int logRadix)
      throws NumberFormatException
 {
   int strLen = s.length();

   int result = 0;
   int i = 0;
   int bitLen = 0;

   while((i < strLen) && (s.charAt(i) == '0')) {i++;};

   if (i < strLen) {
     int digit = Character.digit(s.charAt(i++),radix);
     if (digit < 0) throw new NumberFormatException(s);
     result = digit;
     bitLen = Constants.BITLEN[digit];
   }

   while (i < strLen) {
     int digit = Character.digit(s.charAt(i++),radix);
     if (digit < 0) throw new NumberFormatException(s);
     result = (result << logRadix) + digit;
     bitLen += logRadix;
     if (bitLen > 32) throw new NumberFormatException(s);
   }

   return result;
 }

 private static String toMumbleString(int u, int digitLen, char[] digits, int width) {
   if (width < 32) u &= (1 << width) - 1;
   int sblen = (width + digitLen - 1) / digitLen;
   int start = (sblen - 1) * digitLen;
   int radix = 1 << digitLen;
   int mask = radix - 1;

   StringBuffer b = new StringBuffer(sblen);
   for (int i = start; i >= 0; i -= digitLen) {
     int digit_index = (u >> i) & mask;
     b.append(digits[digit_index]);
   }
   return b.toString();
 }

 private static String toMumbleString(long u, int digitLen, char[] digits, int width) {
   if (width < 64) u &= (1L << width) - 1;
   int sblen = (width + digitLen - 1) / digitLen;
   int start = (sblen - 1) * digitLen;
   int radix = 1 << digitLen;
   int mask = radix - 1;

   StringBuffer b = new StringBuffer(sblen);
   for (int i = start; i >= 0; i -= digitLen) {
     int digit_index = (int) (u >>> i) & mask;
     b.append(digits[digit_index]);
   }
   return b.toString();
 }

 public static String toHexString(int u) {
   return toMumbleString(u,4, Constants.digits, 32);
 }
 public static String toOctalString(int u) {
   return toMumbleString(u,3, Constants.digits, 32);
 }
 public static String toBinaryString(int u) {
   return toMumbleString(u,1, Constants.digits, 32);
 }

 public static String toHexString(long u) {
   return toMumbleString(u,4,Constants.digits, 64);
 }
 public static String toOctalString(long u) {
   return toMumbleString(u,3,Constants.digits, 64);
 }
 public static String toBinaryString(long u) {
   return toMumbleString(u,1,Constants.digits, 64);
 }

 public static String toHexString(int u, int width) {
   return toMumbleString(u,4, Constants.digits, width);
 }
 public static String toOctalString(int u, int width) {
   return toMumbleString(u,3, Constants.digits, width);
 }
 public static String toBinaryString(int u, int width) {
   return toMumbleString(u,1, Constants.digits, width);
 }

 public static String toHexString(long u, int width) {
   return toMumbleString(u,4,Constants.digits, width);
 }
 public static String toOctalString(long u, int width) {
   return toMumbleString(u,3,Constants.digits, width);
 }
 public static String toBinaryString(long u, int width) {
   return toMumbleString(u,1,Constants.digits, width);
 }


 public static String toString(int u) {
   if (u >= 0) return Integer.toString(u);
   else return Long.toString( (long) u + 0x100000000L);
 };

 public static String toString(long u) {
   long original_u = u;
   if (u >= 0) return Long.toString(u);

   String d1="";
   String d2=d1;

   /* Otherwise u is an unsigned with magnitude
      larger than or equal to 9223372036854775808
                              9000000000000000000
      The easiest way to print this is to subtract
      off either 10000000000000000000 or
      ____________9000000000000000000,
      _____________123456789012345678 (18 digits)
      explicitly stuff in the leading digit, and
      convert the rest as a long (with leading
      zeros, of course). */

   if (u >=  -8446744073709551616L) {
     u = u - -8446744073709551616L;
     /* Will print 1,something */
     d1 = "1";
     //                       123456789012345678
     if (u >=                1000000000000000000L) {
	int quot = (int) (u / 1000000000000000000L);
	u = u - quot *        1000000000000000000L;
	d2 = "0123456789".substring(quot,quot+1);
     } else {
	d2 = "0";
     }

   } else {
     u = u - 9000000000000000000L;
     d2 = "9";
   }

   String trailing = Long.toString(u);
   try {
     return d1 + d2 + "000000000000000000".substring(0,18-trailing.length()) + trailing;
   } catch (StringIndexOutOfBoundsException e) {
     System.err.println("Trouble printing " + trailing + " d1 = " + d1 + ", d2 = " + d2 + ", orig_u = " + original_u);
     throw e;
   }
 };

 public static boolean lessThan(int x, int y) {
   return ((x^y) >= 0) ? (x < y) : (y < x);
 };

 public static boolean lessThan(long x, long y) {
   return ((x^y) >= 0) ? (x < y) : (y < x);
 };

 public static boolean lessThanOrEqual(int x, int y) {
   return ((x^y) >= 0) ? (x <= y) : (y <= x);
 };

 public static boolean lessThanOrEqual(long x, long y) {
   return ((x^y) >= 0) ? (x <= y) : (y <= x);
 };

 public static boolean greaterThan(int x, int y) {
   return ((x^y) >= 0) ? (x > y) : (y > x);
 };

 public static boolean greaterThan(long x, long y) {
   return ((x^y) >= 0) ? (x > y) : (y > x);
 };

 public static boolean greaterThanOrEqual(int x, int y) {
   return ((x^y) >= 0) ? (x >= y) : (y >= x);
 };

 public static boolean greaterThanOrEqual(long x, long y) {
   return ((x^y) >= 0) ? (x >= y) : (y >= x);
 };

 public static int min(int x, int y) {
   if ((x^y) >= 0)
     return x < y ? x : y;
   else
     return x > y ? x : y;
 }

 public static long min(long x, long y) {
   if ((x^y) >= 0)
     return x < y ? x : y;
   else
     return x > y ? x : y;
 }

 public static int max(int x, int y) {
   if ((x^y) < 0)
     return x < y ? x : y;
   else
     return x > y ? x : y;
 }

 public static long max(long x, long y) {
   if ((x^y) < 0)
     return x < y ? x : y;
   else
     return x > y ? x : y;
 }

 public static int divide(int x, int y) {
   if ((x|y) >= 0) return x/y;
   long lx = toLong(x);
   long ly = toLong(y);
   return (int) (lx/ly);
 };

 public static int remainder(int x, int y) {
   if ((x|y) >= 0) return x%y;
   long lx = toLong(x);
   long ly = toLong(y);
   return (int) (lx%ly);
 };

 public static long divide(long x, long y) {
   /* Here, "big" means between 2**63 and 2**64-1, inclusive */
   if (x >= 0) {
     if (y >= 0) {
	return x/y;
     } else {
	/* Small / big */
	return 0;
     }
   } else {
     if (y >= 0) {
	/* big / small */
	/* Divide x by two, multiply result by two, then correct. */
	int x_parity = (int) x & 1;
	long x_over_two = x >>> 1;
	long quotient_over_two = x_over_two / y;
	long remainder_over_two = x_over_two - (y * quotient_over_two);
	if (quotient_over_two == 0) {
	  /* X = 2 * N + P (P = 0 or 1)
            X > Y, Y > N
	     X - Y = N + P + N - Y < N + P
            X - Y <= N + P - 1
            either X - Y <= N < Y
            or     X - Y <= N - 1 < Y
	   */
	  return 1;
	} else {
	  long remainder = remainder_over_two + remainder_over_two + x_parity;
	  /* Proof that remainder cannot overflow (i.e., r_o_2 < 2**62)
            r_o_w < y (so if it overflows, y must be >= 2**62)
            r_o_w <= x_o_2 - y
            But x_over_two < 2**63, and y >= 2**62.  Thus, their
            difference is less than 2**62 */
         long quotient = quotient_over_two + quotient_over_two;
	  return remainder >= y ? quotient + 1 : quotient;
	}

     } else {
	/* big / big */
	if ( x >= y) return 1;
	else return 0;
     }
   }
 };

 public static long remainder(long x, long y) {
   /* Here, "big" means between 2**63 and 2**64-1, inclusive */
   if (x >= 0) {
     if (y >= 0) {
	return x % y;
     } else {
	/* Small / big */
	return x;
     }
   } else {
     if (y >= 0) {
	/* big / small */
	/* Divide x by two, multiply result by two, then correct. */
	int x_parity = (int) x & 1;
	long x_over_two = x >>> 1;
	long quotient_over_two = x_over_two / y;
	long remainder_over_two = x_over_two - (y * quotient_over_two);
	if (quotient_over_two == 0) {
	  /* X = 2 * N + P (P = 0 or 1)
            X > Y, Y > N
	     X - Y = N + P + N - Y < N + P
            X - Y <= N + P - 1
            either X - Y <= N < Y
            or     X - Y <= N - 1 < Y
	   */
	  return x - y;
	} else {
	  long remainder = remainder_over_two + remainder_over_two + x_parity;
	  return remainder >= y ? remainder - y : remainder;
	}
     } else {
	/* big / big */
	if ( x >= y) return x - y;
	else return x;
     }
   }
 };

 public static long multiply(int x, int y) {
   return toLong(x) * toLong(y);
 }

 public static long toLong(int x) {
   return (x >= 0) ? (long) x : (long) x + 0x100000000L;
 };

 public static double toDouble(int x) {
   return (x >= 0) ? (double) x : (double) x + 4294967296.0;
 };

 public static float toFloat(int x) {
   // Note that floats have less than 32 bits of precision, therefore
   // the adjustment (if any) must be applied to a double, then
   // converted to float.
   return (x >= 0) ? (float) x : (float) ((double) x + 4294967296.0);
 };

 public static float toFloat(long x) {
   // There's a really obnoxious double-rounding hazard lurking here.
   if (x >= 0)
     return (float) x;

   // Here, we perform IEEE round-to-nearest-choose-even-if-tie

   int x_mant = (int)(x >>> 40); // 24 bits of mantissa.
   long x_discard = x & 0xffffffffffL; // the lost 40 bits.
   //                     1234567890

   // if the lost bits are less than 1/2, truncate.
   // otherwise, either round up or choose even.
   if (x_discard >= 0x8000000000L) {
   //                 1234567890

     if (x_discard == 0x8000000000L) {
	//               1234567890
	// if mantissa is odd, then round up to get even
	x_mant += (x_mant & 1);
     } else {
	x_mant += 1;
     }
   }

   return (float) x_mant * 1.099511627776E12f;

 };

 public static double toDouble(long x) {
   // There's a really obnoxious double-rounding hazard lurking here.
   if (x >= 0)
     return (double) x;

   // Here, we perform IEEE round-to-nearest-choose-even-if-tie

   long x_mant = (x >>> 11); // 53 bits of mantissa.
   int x_discard = (int) x & 0x7ff; // the lost 11 bits.

   // if the lost bits are less than 1/2, truncate.
   // otherwise, either round up or choose even.
   if (x_discard >= 0x400) {
     if (x_discard == 0x400) {
	// if mantissa is odd, then round up to get even
	x_mant += ((int)x_mant & 1);
     } else {
	x_mant += 1;
     }
   }

   return (double) x_mant * 2048.0;
 };

}
