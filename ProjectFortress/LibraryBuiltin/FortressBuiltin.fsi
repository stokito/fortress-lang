(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api FortressBuiltin

(** The %builtinPrimitive% function is actually recognized as a special piece
    of built-in magic by the Fortress interpreter.  The %javaClass%
    argument names a Java Class which is a subclass of
    \texttt{com.sun.fortress.interpreter.glue.NativeApp}, which provides code
    for the closure which is used in place of the call to
    %builtinPrimitive%.  Meanwhile all the necessary type information,
    argument names, etc. must be declared here in Fortress-land.  For
    examples, see the end of this file.

    In practice, if you are extending the interpreter you will probably
    want to extend \texttt{com.sun.fortress.interpreter.glue.NativeFn0,1,2,3,4}
    or one of their subclasses defined in
    \texttt{com.sun.fortress.interpreter.glue.primitive}.  These types are
    generally easier to work with, and the boilerplate packing and
    unpacking of values is done for you.
**)
builtinPrimitive[\T\](javaClass:String):T

trait Object extends Any
    getter ilkName(): String
    getter asString(): String                (* for normal use *)
    getter asDebugString(): String    (* for debugging; may contain more information *)
    getter asExprString(): String       (* when considered as Fortress expression, will equal self *)

    getter toString(): String                (* deprecated *)
end

value object Float extends RR64
end

object FloatLiteral extends RR64
end

value object RR32 extends RR64
    (** returns true if the value is an IEEE NaN **)
    getter isNaN(): Boolean
    (** returns true if the value is an IEEE infinity **)
    getter isInfinite(): Boolean
    (** returns true if the value is a valid number (not NaN) **)
    getter isNumber(): Boolean
    (** returns true if the value is finite **)
    getter isFinite(): Boolean
    (** %check% returns %Just(its argument)% if it is finite, otherwise %Nothing%. **)
    getter check(): Maybe[\RR32\]
    (** %check_star% returns %Just(its argument)% if it is non-NaN, otherwise %Nothing%. **)
    getter check_star(): Maybe[\RR32\]
    (** obtain the raw bits of the IEEE floating-point representation of this value. **)
    getter rawBits():ZZ32
    (** obtain the sign bit of the IEEE floating-point representation of this value. **)
    getter signBit():ZZ32
    (** next higher IEEE float **)
    getter nextUp():RR32
    (** next lower IEEE float **)
    getter nextDown():RR32
    opr ^(self, b:RR32):RR32
    (** %MINNUM% and %MAXNUM% return a numeric result where possible (avoiding NaN).
        Note that %MINNUM% and %MAX% form a lattice with NaN at the top, and
        that %MAXNUM% and %MIN% form a lattice with NaN at the bottom.  **)
    opr MINNUM(self, b:RR32):RR32
    opr MAXNUM(self, b:RR32):RR32
end

value object Int extends ZZ32
end

value object Long extends ZZ64
end

value object NN32 extends { StandardTotalOrder[\NN32\], NN64 }
    opr |self| : NN32
    opr =(self, b:NN32):Boolean
    opr <(self, b:NN32):Boolean
    opr -(self):NN32
    opr +(self,b:NN32):NN32
    opr -(self,b:NN32):NN32
    opr DOT(self,b:NN32):NN32
    opr TIMES(self,b:NN32):NN32
    opr juxtaposition(self,b:NN32):NN32
    opr DIV(self,b:NN32):NN32
    opr REM(self,b:NN32):NN32
    opr MOD(self,b:NN32):NN32
    opr GCD(self,b:NN32):NN32
    opr LCM(self,b:NN32):NN32
    opr CHOOSE(self,b:NN32):NN32
    opr BITAND(self,b:NN32):NN32
    opr BITOR(self,b:NN32):NN32
    opr BITXOR(self,b:NN32):NN32
    opr LSHIFT(self,b:AnyIntegral):NN32
    opr RSHIFT(self,b:AnyIntegral):NN32
    opr BITNOT(self):NN32
    opr ^(self, b:AnyIntegral):RR64
    widen(self):NN64
    partitionL(self):NN32
    signed(self):ZZ32
end

value object UnsignedLong extends NN64
end

object IntLiteral extends { ZZ32 }
    opr =(self, b: IntLiteral):Boolean
    opr <(self, other:IntLiteral): Boolean
    opr <=(self, other:IntLiteral): Boolean
    opr >(self, other:IntLiteral): Boolean
    opr >=(self, other:IntLiteral): Boolean
    opr CMP(self, other:IntLiteral): TotalComparison

(*
Do not enable these until coercion is implemented; doing so will
cause all our arithmetic to occur on IntLiterals.
    opr -(self): IntLiteral
    opr +(self, b: IntLiteral): IntLiteral
    opr -(self, b: IntLiteral): IntLiteral
    opr DOT(self, b: IntLiteral): IntLiteral
    opr juxtaposition(self, b: IntLiteral): IntLiteral
    opr DIV(self, b: IntLiteral): IntLiteral
    opr REM(self, b: IntLiteral): IntLiteral
    opr MOD(self, b: IntLiteral): IntLiteral
    opr GCD(self, b: IntLiteral): IntLiteral
    opr LCM(self, b: IntLiteral): IntLiteral
    opr CHOOSE(self, b: IntLiteral): IntLiteral
    opr BITAND(self, b: IntLiteral): IntLiteral
    opr BITOR(self, b: IntLiteral): IntLiteral
    opr BITXOR(self, b: IntLiteral): IntLiteral
    opr LSHIFT(self, b:AnyIntegral): IntLiteral
    opr RSHIFT(self, b:AnyIntegral): IntLiteral
    opr BITNOT(self): IntLiteral
    opr ^(self, b:AnyIntegral):RR64
*)
end

object BigNum extends ZZ end

value object Boolean
    extends { Condition[\()\], StandardTotalOrder[\Boolean\] }
end

value object Char extends { StandardTotalOrder[\Char\] }
    (** %char.codePoint% converts %char% to the equivalent integer code point.
        It is always the case that %c = char(c.codePoint())% for %c : Char%. **)
    getter codePoint(): ZZ32

    (** |c| means the same as %c.ord()%; it's unclear if this is
        actually a good idea, and we solicit feedback on the subject. **)
    opr |self| : ZZ32

    (** Ordering respects %codePoint%. **)
    opr =(self, other:Char): Boolean
    opr <(self, other:Char): Boolean
    opr SIMEQ(self, other:Char): Boolean
    opr NSIMEQ(self, other:Char): Boolean
    opr LNSIM(self, other:Char): Boolean
    opr LESSSIM(self, other:Char): Boolean
    opr GNSIM(self, other:Char): Boolean
    opr GTRSIM(self, other:Char): Boolean

    (* The following methods have the same behavior as the methods in Java
       Character class, except for methods digit and forDigit.  These two
       particular methods deviate from Java Character class when it gets
       argument radix = 12.  For radix 12, the digits are "0123456789xe"
       instead of "0123456789ab". *)
    (* javaDigit(self, radix:ZZ32): ZZ32 *)
    digit(self): Maybe[\ZZ32\] (* radix 10 *)
    digit(self, radix:ZZ32): Maybe[\ZZ32\]
    getDirectionality(self): ZZ32
    getNumericValue(self): ZZ32
    getType(self): ZZ32
    isDefined(self): Boolean
    isDigit(self): Boolean
    isFortressIdentifierPart(self): Boolean
    isFortressIdentifierStart(self): Boolean
    isHighSurrogate(self): Boolean
    isIdentifierIgnorable(self): Boolean
    isISOControl(self): Boolean
    isJavaIdentifierPart(self): Boolean
    isJavaIdentifierStart(self): Boolean
    isLetter(self): Boolean
    isLetterOrDigit(self): Boolean
    isLowerCase(self): Boolean
    isLowSurrogate(self): Boolean
    isMirrored(self): Boolean
    isSpaceChar(self): Boolean
    isSupplementaryCodePoint(self): Boolean
    isSurrogatePair(self, low: Char): Boolean
    isTitleCase(self): Boolean
    isUnicodeIdentifierPart(self): Boolean
    isUnicodeIdentifierStart(self): Boolean
    isUpperCase(self): Boolean
    isValidCodePoint(self): Boolean
    isWhitespace(self): Boolean
    toLowerCase(self): Char
    toTitleCase(self): Char
    toUpperCase(self): Char
end

object Thread[\T\](fcn:()->T)
    getter val():T
    getter ready():Boolean
    wait():()
    stop():()
end

abort():()

end
