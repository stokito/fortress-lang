/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.numerics;

/**
 * @author Guy L. Steele, Jr.
 *         <p/>
 *         Directed rounding for Java float and double, implemented
 *         in pure Java.
 */
public final class DirectedRounding {

    private DirectedRounding() {
    }

    public static float nextUp(float x) {
        if (Float.isNaN(x)) return x;
        else {
            int a = Float.floatToRawIntBits(x);
            if (a < 0) {
                if (a == 0x80000000) return (float) 0.0;
                else return Float.intBitsToFloat(a - 1);
            } else {
                if (a == 0x7F800000) return x;
                else return Float.intBitsToFloat(a + 1);
            }
        }
    }

    public static float nextDown(float x) {
        return -nextUp(-x);
    }

    public static float addUp(float x, float y) {
        float z = x + y;
        if (Float.isInfinite(z)) {
            if (z == Float.POSITIVE_INFINITY || x == Float.NEGATIVE_INFINITY || y == Float.NEGATIVE_INFINITY) return z;
            else return -Float.MAX_VALUE;
        } else if (Float.isNaN(z)) return z;
        else {
            float w = (Math.abs(x) >= Math.abs(y)) ? (z - x) - y : (z - y) - x;
            return (w >= 0) ? z : nextUp(z);
        }
    }

    public static float subtractUp(float x, float y) {
        return addUp(x, -y);
    }

    public static float multiplyUp(float x, float y) {
        float z = x * y;
        if (x == 0 || y == 0 || Float.isInfinite(x) || Float.isInfinite(y) || Float.isNaN(z)) return z;
        else {
            double w = ((double) z) - ((double) x) * ((double) y);
            return (w >= 0) ? z : nextUp(z);
        }
    }

    public static float divideUp(float x, float y) {
        float z = x / y;
        if (x == 0 || y == 0 || Float.isInfinite(x) || Float.isInfinite(y) || Float.isNaN(z)) return z;
        else {
            double w = ((double) z) - ((double) x) / ((double) y);
            return (w >= 0) ? z : nextUp(z);
        }
    }

    public static float sqrtUp(float x) {
        float z = (float) Math.sqrt(x);
        if (x == 0 || Float.isInfinite(x) || Float.isNaN(z)) return z;
        else {
            double w = ((double) z) - Math.sqrt((double) x);
            return (w >= 0) ? z : nextUp(z);
        }
    }

    public static float addDown(float x, float y) {
        return -addUp(-x, -y);
    }

    public static float subtractDown(float x, float y) {
        return -subtractUp(y, x);
    }

    public static float multiplyDown(float x, float y) {
        return -multiplyUp(-x, y);
    }

    public static float divideDown(float x, float y) {
        return -divideUp(-x, y);
    }

    public static float sqrtDown(float x) {
        float z = (float) Math.sqrt(x);
        if (x == 0 || Float.isInfinite(x) || Float.isNaN(z)) return z;
        else {
            double w = ((double) z) - Math.sqrt((double) x);
            return (w > 0) ? nextDown(z) : z;
        }
    }

    public static double nextUp(double x) {
        if (Double.isNaN(x)) return x;
        else {
            long a = Double.doubleToRawLongBits(x);
            if (a < 0) {
                if (a == 0x8000000000000000L) return 0.0;
                else return Double.longBitsToDouble(a - 1);
            } else {
                if (a == 0x7FF0000000000000L) return x;
                else return Double.longBitsToDouble(a + 1);
            }
        }
    }

    public static double nextDown(double x) {
        return -nextUp(-x);
    }

    public static double addUp(double x, double y) {
        double z = x + y;
        if (Double.isInfinite(z)) {
            if (z == Double.POSITIVE_INFINITY || x == Double.NEGATIVE_INFINITY || y == Double.NEGATIVE_INFINITY)
                return z;
            else return -Double.MAX_VALUE;
        } else if (Double.isNaN(z)) return z;
        else {
            double w = (Math.abs(x) >= Math.abs(y)) ? (z - x) - y : (z - y) - x;
            return (w >= 0) ? z : nextUp(z);
        }
    }

    public static double subtractUp(double x, double y) {
        return addUp(x, -y);
    }


    // Given the bits of two normalized floats, return the high 56 bits of the
    // product of their significands, with guard and sticky bits.
    private static long high56productbits(long a, long b) {
        // We first strip the sign and exponent bits, split each 53-bit fraction
        // into 28 high bits and 25 low bits, and make the leading hidden bits explicit.
        long ahi = ((a << 12) >>> 37) | 0x08000000L, alo = a & 0x01ffffffL;
        long bhi = ((b << 12) >>> 37) | 0x08000000L, blo = b & 0x01ffffffL;
        long h = alo * blo;
        long g = ahi * blo + alo * bhi + (h >> 25);
        long f = (ahi * bhi + (g >> 25)) | ((((g | h) & 0x01ffffffL) != 0) ? 1 : 0);
        // Now f has the high 56 bits of the 106-bit product, except that its
        // low bit is a "sticky bit" that is 1 iff that bit or any lower bit of the
        // product is non-zero.  This value may need to be left-shifted 1 place.
        return f;
    }

    // Given the bits of two normalized floats, return the high 55 bits of the
    // quotient of their significands, with guard and sticky bits.
    private static long high55quotientbits(long a, long b) {
        // We first strip the sign and exponent bits and make the leading hidden bits explicit.
        long n = ((a << 12) >>> 12) | 0x0010000000000000L;
        long d = ((b << 12) >>> 12) | 0x0010000000000000L;
        long q = 0;
        // Now six iterations, getting 10 bits per iteration
        n <<= 10;
        q <<= 10;
        q |= n / d;
        n %= d;
        n <<= 10;
        q <<= 10;
        q |= n / d;
        n %= d;
        n <<= 10;
        q <<= 10;
        q |= n / d;
        n %= d;
        n <<= 10;
        q <<= 10;
        q |= n / d;
        n %= d;
        n <<= 10;
        q <<= 10;
        q |= n / d;
        n %= d;
        n <<= 10;
        q <<= 10;
        q |= n / d;
        n %= d;
        // After six iterations, q has 60 bits of quotient.
        long f = (q >>> 5) | (((q & 0x1fL) | n) != 0 ? 1 : 0);
        // Now f has the high 55 bits of the quotient, except that its
        // low bit is a "sticky bit" that is 1 iff that bit or any lower bit of the
        // quotient is non-zero.  This value may need to be left-shifted 1 place.
        return f;
    }

    // Multiplying by 2**60 is way more than enough to make a denormalized value normalized.
    private static double double_2_pow_30 = 1024.0 * 1024.0 * 1024.0;
    private static double double_2_pow_60 = double_2_pow_30 * double_2_pow_30;

    public static double multiplyUp(double x, double y) {
        double z = x * y;
        if (x == 0 || y == 0 || Double.isInfinite(x) || Double.isInfinite(y) || Double.isNaN(z)) return z;
        else if (Double.isInfinite(z)) return (z > 0) ? z : -Double.MAX_VALUE;
        else if (z == 0) return (Double.doubleToRawLongBits(z) >= 0) ? Double.MIN_VALUE : -0.0;
        else {
            long a = Double.doubleToRawLongBits(x);
            long b = Double.doubleToRawLongBits(y);
            // Cheap test: if the low 53 bits of a*b are 0,
            // then the floating-point product was exact.
            if (((a * b) << 11) == 0) return z;
            else {
                long c = Double.doubleToRawLongBits(z);
                double xs = x, ys = y, zs = z;
                // Now a decision tree for scaling denormalized values
                if (((a << 1) >>> 53) == 0) {
                    // If x and y are both denormalized, then z is zero; we already handled this.
                    if (((c << 1) >>> 53) == 0) {
                        // x and z are denormalized, so scale x and z by 2**60
                        xs = x * double_2_pow_60;
                        a = Double.doubleToRawLongBits(xs);
                        zs = z * double_2_pow_60;
                        c = Double.doubleToRawLongBits(zs);
                    } else {
                        // Only x is denormalized, so y > 1; we can scale x up and y down
                        xs = x * double_2_pow_60;
                        a = Double.doubleToRawLongBits(xs);
                        ys = y / double_2_pow_60;
                        b = Double.doubleToRawLongBits(ys);
                    }
                } else {
                    if (((b << 1) >>> 53) == 0) {
                        if (((c << 1) >>> 53) == 0) {
                            // y and z are denormalized, so scale y and z by 2**60
                            ys = y * double_2_pow_60;
                            b = Double.doubleToRawLongBits(ys);
                            zs = z * double_2_pow_60;
                            c = Double.doubleToRawLongBits(zs);
                        } else {
                            // Only y is denormalized, so x > 1; we can scale y up and x down
                            xs = x / double_2_pow_60;
                            a = Double.doubleToRawLongBits(xs);
                            ys = y * double_2_pow_60;
                            b = Double.doubleToRawLongBits(ys);
                        }
                    } else {
                        if (((c << 1) >>> 53) == 0) {
                            // Only z is denormalized, so at least one of x and y has headroom;
                            // scale both z and whichever of x and y has smaller magnitude by 2**60
                            if ((a & 0x7fffffffffffffffL) < (b & 0x7fffffffffffffffL)) {
                                xs = x * double_2_pow_60;
                                a = Double.doubleToRawLongBits(xs);
                            } else {
                                ys = y * double_2_pow_60;
                                b = Double.doubleToRawLongBits(ys);
                            }
                            zs = z * double_2_pow_60;
                            c = Double.doubleToRawLongBits(zs);
                        }
                    }
                }
                long e = ((a << 12) >>> 9) | 0x0080000000000000L;
                long delta = ((c << 1) >>> 53) - ((b << 1) >>> 53) - ((a << 1) >>> 53) + 1023;
                long f = high55quotientbits(c, b) << delta;
                // For a negative result, the round-to-even result is wrong for our purposes
                // if f (the quotient computed from the product) is larger than e.
                // For a positive result, the round-to-even result is wrong for our purposes
                // if f (the quotient computed from the product) is smaller than e.
                return (((a ^ b) < 0) ? (f > e) : (f < e)) ? nextUp(z) : z;
            }
        }
    }

    public static double divideUp(double x, double y) {
        double z = x / y;
        if (x == 0 || y == 0 || Double.isInfinite(x) || Double.isInfinite(y) || Double.isNaN(z)) return z;
        else if (Double.isInfinite(z)) return (z > 0) ? z : -Double.MAX_VALUE;
        else if (z == 0) return (Double.doubleToRawLongBits(z) >= 0) ? Double.MIN_VALUE : -0.0;
        else {
            long b = Double.doubleToRawLongBits(y);
            long c = Double.doubleToRawLongBits(z);
            // Cheap test: if x==y*z and the low 53 bits of b*c are 0,
            // then the floating-point quotient was exact.
            if ((x == y * z) && (((b * c) << 11) == 0)) return z;
            else {
                long a = Double.doubleToRawLongBits(x);
                double xs = x, ys = y, zs = z;
                // Now a decision tree for scaling denormalized values
                if (((a << 1) >>> 53) == 0) {
                    if (((b << 1) >>> 53) == 0) {
                        // x and y are denormalized (so z is fairly close to 1); scale x and y by 2**60
                        xs = x * double_2_pow_60;
                        a = Double.doubleToRawLongBits(xs);
                        ys = y * double_2_pow_60;
                        b = Double.doubleToRawLongBits(ys);
                    } else {
                        if (((c << 1) >>> 53) == 0) {
                            // x and z are denormalized (so y is fairly close to 1); scale x and z by 2**60
                            xs = x * double_2_pow_60;
                            a = Double.doubleToRawLongBits(xs);
                            zs = z * double_2_pow_60;
                            c = Double.doubleToRawLongBits(zs);
                        } else {
                            // Only x is denormalized, so y has headroom; scale x and y by 2**60
                            xs = x * double_2_pow_60;
                            a = Double.doubleToRawLongBits(xs);
                            ys = y * double_2_pow_60;
                            b = Double.doubleToRawLongBits(ys);
                        }
                    }
                } else {
                    if (((b << 1) >>> 53) == 0) {
                        // y is denormalized and x is not, therefore z is not; try scaling x and y by 2**60,
                        // but if x becomes infinite, then z must be an infinity and that's the answer.
                        xs = x * double_2_pow_60;
                        a = Double.doubleToRawLongBits(xs);
                        ys = y * double_2_pow_60;
                        b = Double.doubleToRawLongBits(ys);
                        if (Double.isInfinite(xs)) return z;
                    } else {
                        if (((c << 1) >>> 53) == 0) {
                            // Only z is denormalized, so x has headroom; scale x and z by 2**60
                            xs = x * double_2_pow_60;
                            a = Double.doubleToRawLongBits(xs);
                            zs = z * double_2_pow_60;
                            c = Double.doubleToRawLongBits(zs);
                        }
                    }
                }
                long e = ((a << 12) >>> 9) | 0x0080000000000000L;
                long delta = ((a << 1) >>> 53) - ((b << 1) >>> 53) - ((c << 1) >>> 53) + 1023;
                long f = high56productbits(b, c) << (1 - delta);
                // For a negative result, the round-to-even result is wrong for our purposes
                // if f (the product computed from the quotient) is larger than e.
                // For a positive result, the round-to-even result is wrong for our purposes
                // if f (the product computed from the quotient) is smaller than e.
                return (((a ^ b) < 0) ? (f > e) : (f < e)) ? nextUp(z) : z;
            }
        }
    }

    public static double sqrtUp(double x) {
        double z = Math.sqrt(x);
        if (x == 0 || Double.isInfinite(x) || Double.isNaN(z)) return z;
        else {
            long c = Double.doubleToRawLongBits(z);
            // Cheap test: if x==z*z and the low 53 bits of c*c are 0,
            // then the floating-point square root was exact.
            if ((x == z * z) && (((c * c) << 11) == 0)) return z;
            else {
                long a = Double.doubleToRawLongBits(x);
                // If x is denormalized, multiply by 2**60 to get normalized significand.
                // Multiply z by 2**30 to compensate.  (Note that z cannot be denormalized.)
                if (((a << 1) >>> 53) == 0) {
                    a = Double.doubleToRawLongBits(x * double_2_pow_60);
                    c = Double.doubleToRawLongBits(z * double_2_pow_30);
                }
                long e = ((a << 12) >>> 9) | 0x0080000000000000L;
                long delta = ((a << 1) >>> 53) - (((c << 1) >>> 53) << 1) + 1023;
                long f = high56productbits(c, c) << (1 - delta);
                // The round-to-even result is wrong for our purposes
                // if f (the product computed from the square root) is smaller than e.
                return (f < e) ? nextUp(z) : z;
            }
        }
    }

    public static double addDown(double x, double y) {
        return -addUp(-x, -y);
    }

    public static double subtractDown(double x, double y) {
        return -subtractUp(y, x);
    }

    public static double multiplyDown(double x, double y) {
        return -multiplyUp(-x, y);
    }

    public static double divideDown(double x, double y) {
        return -divideUp(-x, y);
    }

    public static double sqrtDown(double x) {
        double z = Math.sqrt(x);
        if (x == 0 || Double.isInfinite(x) || Double.isNaN(z)) return z;
        else {
            long c = Double.doubleToRawLongBits(z);
            // Cheap test: if x==z*z and the low 53 bits of c*c are 0,
            // then the floating-point square root was exact.
            if ((x == z * z) && (((c * c) << 11) == 0)) return z;
            else {
                long a = Double.doubleToRawLongBits(x);
                // If x is denormalized, multiply by 2**60 to get normalized significand.
                // Multiply z by 2**30 to compensate.  (Note that z cannot be denormalized.)
                if (((a << 1) >>> 53) == 0) {
                    a = Double.doubleToRawLongBits(x * double_2_pow_60);
                    c = Double.doubleToRawLongBits(z * double_2_pow_30);
                }
                long e = ((a << 12) >>> 9) | 0x0080000000000000L;
                long delta = ((a << 1) >>> 53) - (((c << 1) >>> 53) << 1) + 1023;
                long f = high56productbits(c, c) << (1 - delta);
                // The round-to-even result is wrong for our purposes
                // if f (the product computed from the square root) is larger than e.
                return (f > e) ? nextDown(z) : z;
            }
        }
    }

    public static float addUpNoNaN(float x, float y) {
        float r = addUp(x, y);
        return addNoNaNfixup(x, y, r, Float.POSITIVE_INFINITY);
    }

    public static float addDownNoNaN(float x, float y) {
        float r = addDown(x, y);
        return addNoNaNfixup(x, y, r, Float.NEGATIVE_INFINITY);
    }

    static float addNoNaNfixup(float x, float y, float r, float alt) {
        if (Float.isNaN(r)) {
            if (Float.isNaN(x) || Float.isNaN(y)) return r;
            else return alt;
        } else return r;
    }

    public static float subtractUpNoNaN(float x, float y) {
        return addUpNoNaN(x, -y);
    }

    public static float subtractDownNoNaN(float x, float y) {
        return addDownNoNaN(x, -y);
    }

    public static float multiplyUpNoNaN(float x, float y) {
        float r = multiplyUp(x, y);
        return multdivNoNaNfixup(x, y, r, -(float) 0.0, Float.POSITIVE_INFINITY);
    }

    public static float multiplyDownNoNaN(float x, float y) {
        float r = multiplyDown(x, y);
        return multdivNoNaNfixup(x, y, r, Float.NEGATIVE_INFINITY, (float) 0.0);
    }

    public static float divideUpNoNaN(float x, float y) {
        float r = divideUp(x, y);
        return multdivNoNaNfixup(x, y, r, -(float) 0.0, Float.POSITIVE_INFINITY);
    }

    public static float divideDownNoNaN(float x, float y) {
        float r = divideDown(x, y);
        return multdivNoNaNfixup(x, y, r, Float.NEGATIVE_INFINITY, (float) 0.0);
    }

    static float multdivNoNaNfixup(float x, float y, float r, float neg, float pos) {
        if (Float.isNaN(r)) {
            if (Float.isNaN(x) || Float.isNaN(y)) return r;
            int xbits = Float.floatToRawIntBits(x);
            int ybits = Float.floatToRawIntBits(y);
            if ((xbits ^ ybits) < 0) return neg;
            else return pos;
        } else return r;
    }

    public static double addUpNoNaN(double x, double y) {
        double r = addUp(x, y);
        return addNoNaNfixup(x, y, r, Double.POSITIVE_INFINITY);
    }

    public static double addDownNoNaN(double x, double y) {
        double r = addDown(x, y);
        return addNoNaNfixup(x, y, r, Double.NEGATIVE_INFINITY);
    }

    static double addNoNaNfixup(double x, double y, double r, double alt) {
        if (Double.isNaN(r)) {
            if (Double.isNaN(x) || Double.isNaN(y)) return r;
            else return alt;
        } else return r;
    }

    public static double subtractUpNoNaN(double x, double y) {
        return addUpNoNaN(x, -y);
    }

    public static double subtractDownNoNaN(double x, double y) {
        return addDownNoNaN(x, -y);
    }

    public static double multiplyUpNoNaN(double x, double y) {
        double r = multiplyUp(x, y);
        return multdivNoNaNfixup(x, y, r, -0.0, Double.POSITIVE_INFINITY);
    }

    public static double multiplyDownNoNaN(double x, double y) {
        double r = multiplyDown(x, y);
        return multdivNoNaNfixup(x, y, r, Double.NEGATIVE_INFINITY, 0.0);
    }

    public static double divideUpNoNaN(double x, double y) {
        double r = divideUp(x, y);
        return multdivNoNaNfixup(x, y, r, -0.0, Double.POSITIVE_INFINITY);
    }

    public static double divideDownNoNaN(double x, double y) {
        double r = divideDown(x, y);
        return multdivNoNaNfixup(x, y, r, Double.NEGATIVE_INFINITY, 0.0);
    }

    static double multdivNoNaNfixup(double x, double y, double r, double neg, double pos) {
        if (Double.isNaN(r)) {
            if (Double.isNaN(x) || Double.isNaN(y)) return r;
            long xbits = Double.doubleToRawLongBits(x);
            long ybits = Double.doubleToRawLongBits(y);
            if ((xbits ^ ybits) < 0) return neg;
            else return pos;
        } else return r;
    }

}
