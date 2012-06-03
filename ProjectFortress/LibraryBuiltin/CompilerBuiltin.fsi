(*******************************************************************************
    Copyright 2008,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api CompilerBuiltin
import CompilerAlgebra.{ Equality, StandardTotalOrder, Comparison }
import AnyType.{Any}

trait Object extends Any
    getter asString(): String
    getter asExprString(): String
    getter asDebugString(): String
end Object

nanoTime(): RR64

trait String extends StandardTotalOrder[\String\]
    abstract getter asJavaString(): JavaString
    abstract getter asString(): String
    abstract getter isEmpty(): Boolean
    opr <(self, b: String): Boolean
    opr =(self, b: String): Boolean
    opr >(self, b: String): Boolean 
    opr <=(self, b: String): Boolean 
    opr >=(self, b: String): Boolean 
    opr CMP(self, other:String): Comparison    
    abstract opr |self| : ZZ32
    abstract opr || (self, b:Object): String
    abstract opr juxtaposition(self, b:Object): String
    abstract opr[i:ZZ32] : Character
    abstract substring(lo:ZZ32, hi:ZZ32): String
    abstract opr ^(self, n: ZZ32): String
end

trait JavaString extends String
end JavaString

print(x:Object):()
(*) print(c:Character):()
(*) print(x:ZZ32):()
(*) print(x:ZZ64):()
(*) print(x:RR64):()

println(x:Object):()
(*) println(s:String):()
(*) println(c:Character):()
(*) println():()
(*) println(x:Any):()
(*) println(x:ZZ32):()
(*) println(x:ZZ64):()
(*) println(x:RR32):()
(*) println(x:RR64):()
println(a: Any, b: Any): ()
println(a: Any, b: Any, c: Any): ()
println(a: Any, b: Any, c: Any, d: Any): ()
(*) println[\A,B,C,D,E\](x: (A,B,C,D,E)):()
(*) println[\A,B,C,D,E,F\](x: (A,B,C,D,E,F)):()
(*) println[\A,B,C,D,E,F,G\](x: (A,B,C,D,E,F,G)):()

errorPrintln(x:Object):()
(*) errorPrintln(s:String):()
(*) errorPrintln(c:Character):()
(*) errorPrintln(x:Object): ()
(*) errorPrintln():()
(*) errorPrintln(x:Any):()
(*) errorPrintln(x:ZZ32):()
(*) errorPrintln(x:ZZ64):()
(*) errorPrintln(x:RR32):()
(*) errorPrintln(x:RR64):()
(*) errorPrintln[\A,B\](x: (A,B)):()
(*) errorPrintln[\A,B,C\](x: (A,B,C)):()
(*) errorPrintln[\A,B,C,D\](x: (A,B,C,D)):()
(*) errorPrintln[\A,B,C,D,E\](x: (A,B,C,D,E)):()
(*) errorPrintln[\A,B,C,D,E,F\](x: (A,B,C,D,E,F)):()
(*) errorPrintln[\A,B,C,D,E,F,G\](x: (A,B,C,D,E,F,G)):()

strToInt(s:String):ZZ32

trait Number excludes { String }
end

value object Infinity extends Number end
value object NegativeInfinity extends Number end
value object IndefiniteNumber extends Number end

trait ZZ extends { Number, Equality[\ZZ\] } excludes { RR64, ZZ64, ZZ32, NN32, NN64, IntLiteral } 
    coerce(x: IntLiteral)
    opr |self| : ZZ
    opr -(self): ZZ
    opr BOXMINUS(self): ZZ
    opr DOTMINUS(self): ZZ
    opr +(self, other:ZZ): ZZ
    opr BOXPLUS(self, other:ZZ): ZZ
    opr DOTPLUS(self, other:ZZ): ZZ
    opr -(self, other:ZZ): ZZ 
    opr BOXMINUS(self, other:ZZ): ZZ 
    opr DOTMINUS(self, other:ZZ): ZZ 
    opr <(self, other:ZZ): Boolean 
    opr <=(self, other:ZZ): Boolean 
    opr >(self, other:ZZ): Boolean 
    opr >=(self, other:ZZ): Boolean 
    opr =(self, other:ZZ): Boolean 
    opr =/=(self, other:ZZ): Boolean 
    opr juxtaposition(self, other:ZZ): ZZ
    opr DOT(self, other:ZZ): ZZ 
    opr BOXDOT(self, other:ZZ): ZZ 
    opr CROSS(self, other:ZZ): ZZ 
    opr BOXCROSS(self, other:ZZ): ZZ 
    opr DOTCROSS(self, other:ZZ): ZZ 
    opr DIV(self, other:ZZ): ZZ
    opr BITNOT(self): ZZ 
    opr BITAND(self, other:ZZ): ZZ 
    opr BITOR(self, other:ZZ): ZZ 
    opr BITXOR(self, other:ZZ): ZZ
    opr <<<(self, other:ZZ32): ZZ
    opr MIN(self, other:ZZ): ZZ 
    opr MAX(self, other:ZZ): ZZ 
    opr MINMAX(self, other:ZZ): (ZZ, ZZ) 
    even(self): Boolean
    odd(self): Boolean
    floorAverage(self, other: ZZ): ZZ
    ceilingAverage(self, other: ZZ): ZZ
end

trait ZZ64 extends { Number, Equality[\ZZ64\] } excludes { RR64 , ZZ }
    coerce(x: IntLiteral)
    coerce(x: ZZ32) 
    getter asZZ32(): ZZ32 
    getter bitsAsNN64(): NN64
    opr |self| : ZZ64
    opr -(self): ZZ64
    opr BOXMINUS(self): ZZ64
    opr DOTMINUS(self): ZZ64
    opr +(self, other:ZZ64): ZZ64
    opr BOXPLUS(self, other:ZZ64): ZZ64
    opr DOTPLUS(self, other:ZZ64): ZZ64
    opr -(self, other:ZZ64): ZZ64 
    opr BOXMINUS(self, other:ZZ64): ZZ64 
    opr DOTMINUS(self, other:ZZ64): ZZ64 
    opr <(self, other:ZZ64): Boolean 
    opr <=(self, other:ZZ64): Boolean 
    opr >(self, other:ZZ64): Boolean 
    opr >=(self, other:ZZ64): Boolean 
    opr =(self, other:ZZ64): Boolean 
    opr =/=(self, other:ZZ64): Boolean 
    opr juxtaposition(self, other:ZZ64): ZZ64
    opr DOT(self, other:ZZ64): ZZ64 
    opr BOXDOT(self, other:ZZ64): ZZ64 
    opr CROSS(self, other:ZZ64): ZZ64 
    opr BOXCROSS(self, other:ZZ64): ZZ64 
    opr DOTCROSS(self, other:ZZ64): ZZ64 
    opr DIV(self, other:ZZ64): ZZ64
    opr BITNOT(self): ZZ64 
    opr BITAND(self, other:ZZ64): ZZ64 
    opr BITOR(self, other:ZZ64): ZZ64 
    opr BITXOR(self, other:ZZ64): ZZ64
    opr <<(self, other:ZZ32): ZZ64
    opr <<(self, other:ZZ64): ZZ64
    opr <<(self, other:NN32): ZZ64
    opr <<(self, other:NN64): ZZ64
    opr >>(self, other:ZZ32): ZZ64
    opr >>(self, other:ZZ64): ZZ64
    opr >>(self, other:NN32): ZZ64
    opr >>(self, other:NN64): ZZ64
    opr <<<(self, other:ZZ32): ZZ64
    opr <<<(self, other:ZZ64): ZZ64
    opr <<<(self, other:NN32): ZZ64
    opr <<<(self, other:NN64): ZZ64
    opr MIN(self, other:ZZ64): ZZ64 
    opr MAX(self, other:ZZ64): ZZ64 
    opr MINMAX(self, other:ZZ64): (ZZ64, ZZ64) 
    opr CHOOSE(self, other:ZZ64): ZZ64
    even(self): Boolean
    odd(self): Boolean
    floorAverage(self, other: ZZ64): ZZ64
    ceilingAverage(self, other: ZZ64): ZZ64
end

trait ZZ32 extends { Number, Equality[\ZZ32\], StandardTotalOrder[\ZZ32\] } excludes { ZZ64, RR32, RR64 }
    coerce(x: IntLiteral)
    getter asZZ32(): ZZ32
    getter bitsAsNN32(): NN32
    opr |self| : ZZ32
    opr -(self): ZZ32
    opr BOXMINUS(self): ZZ32
    opr DOTMINUS(self): ZZ32
    opr +(self, other:ZZ32): ZZ32
    opr BOXPLUS(self, other:ZZ32): ZZ32
    opr DOTPLUS(self, other:ZZ32): ZZ32
    opr -(self, other:ZZ32): ZZ32
    opr BOXMINUS(self, other:ZZ32): ZZ32
    opr DOTMINUS(self, other:ZZ32): ZZ32
    opr <(self, other:ZZ32) : Boolean
    opr <=(self, other:ZZ32): Boolean
    opr >(self, other:ZZ32): Boolean
    opr >=(self, other:ZZ32): Boolean
    opr =(self, other:ZZ32): Boolean
    opr =/=(self, other:ZZ32): Boolean
    opr juxtaposition(self, other:ZZ32): ZZ32
    opr DOT(self, other:ZZ32): ZZ32
    opr BOXDOT(self, other:ZZ32): ZZ32
    opr CROSS(self, other:ZZ32): ZZ32
    opr BOXCROSS(self, other:ZZ32): ZZ32
    opr DOTCROSS(self, other:ZZ32): ZZ32
    opr DIV(self, other:ZZ32): ZZ32
    opr BITNOT(self): ZZ32 
    opr BITAND(self, other:ZZ32): ZZ32 
    opr BITOR(self, other:ZZ32): ZZ32 
    opr BITXOR(self, other:ZZ32): ZZ32
    opr <<(self, other:ZZ32): ZZ32
    opr <<(self, other:ZZ64): ZZ32
    opr <<(self, other:NN32): ZZ32
    opr <<(self, other:NN64): ZZ32
    opr >>(self, other:ZZ32): ZZ32
    opr >>(self, other:ZZ64): ZZ32
    opr >>(self, other:NN32): ZZ32
    opr >>(self, other:NN64): ZZ32
    opr <<<(self, other:ZZ32): ZZ32
    opr <<<(self, other:ZZ64): ZZ32
    opr <<<(self, other:NN32): ZZ32
    opr <<<(self, other:NN64): ZZ32
    opr MIN(self, other:ZZ32): ZZ32 
    opr MAX(self, other:ZZ32): ZZ32 
    opr MINMAX(self, other:ZZ32): (ZZ32, ZZ32)
    opr CHOOSE(self, other:ZZ32): ZZ32 
    even(self): Boolean
    odd(self): Boolean
    opr ^(self, other:ZZ32): ZZ32
    asRR64(): RR64
end

trait NN32 extends { Number, Equality[\NN32\] } excludes { ZZ32, ZZ64, RR32, RR64 }
    coerce(x: IntLiteral)
    getter asNN32(): NN32
    getter bitsAsZZ32(): ZZ32
    getter asString(): String
    opr |self| : NN32
    opr -(self): NN32 
    opr BOXMINUS(self): NN32
    opr DOTMINUS(self): NN32
    opr +(self, other:NN32): NN32
    opr BOXPLUS(self, other:NN32): NN32
    opr DOTPLUS(self, other:NN32): NN32
    opr -(self, other:NN32): NN32
    opr BOXMINUS(self, other:NN32): NN32
    opr DOTMINUS(self, other:NN32): NN32
    opr DOT(self, other:NN32): NN32
    opr BOXDOT(self, other:NN32): NN32
    opr CROSS(self, other:NN32): NN32
    opr BOXCROSS(self, other:NN32): NN32
    opr DOTCROSS(self, other:NN32): NN32
    opr DIV(self, other:NN32): NN32
    opr <(self, other:NN32): Boolean
    opr <=(self, other:NN32): Boolean
    opr >(self, other:NN32): Boolean
    opr >=(self, other:NN32): Boolean
    opr =(self, other:NN32): Boolean
    opr =/=(self, other:NN32): Boolean
    opr juxtaposition(self, other:NN32): NN32
    opr BITNOT(self): NN32
    opr BITAND(self, other:NN32): NN32
    opr BITOR(self, other:NN32): NN32
    opr BITXOR(self, other:NN32): NN32
    opr <<(self, other:ZZ32): NN32
    opr <<(self, other:ZZ64): NN32
    opr <<(self, other:NN32): NN32
    opr <<(self, other:NN64): NN32
    opr >>(self, other:ZZ32): NN32
    opr >>(self, other:ZZ64): NN32
    opr >>(self, other:NN32): NN32
    opr >>(self, other:NN64): NN32
    opr <<<(self, other:ZZ32): NN32
    opr <<<(self, other:ZZ64): NN32
    opr <<<(self, other:NN32): NN32
    opr <<<(self, other:NN64): NN32
    opr MIN(self, other:NN32): NN32
    opr MAX(self, other:NN32): NN32
    opr MINMAX(self, other:NN32): (NN32, NN32)
    opr CHOOSE(self, other:NN32): NN32
    even(self): Boolean
    odd(self): Boolean
    floorAverage(self, other: NN32): NN32
    ceilingAverage(self, other: NN32): NN32
    opr ^(self, other:NN32):NN32
    asRR64(): RR64
end

trait NN64 extends { Number, Equality[\NN64\] } excludes { ZZ32, ZZ64, RR32, RR64, NN32, ZZ, IntLiteral }
    coerce(x: IntLiteral)
    coerce(x: NN32)
    getter asNN64(): NN64
    getter bitsAsZZ64(): ZZ64
    getter asString(): String
    opr |self| : NN64
    opr -(self): NN64 
    opr BOXMINUS(self): NN64
    opr DOTMINUS(self): NN64
    opr +(self, other:NN64): NN64
    opr BOXPLUS(self, other:NN64): NN64
    opr DOTPLUS(self, other:NN64): NN64
    opr -(self, other:NN64): NN64
    opr BOXMINUS(self, other:NN64): NN64
    opr DOTMINUS(self, other:NN64): NN64
    opr DOT(self, other:NN64): NN64
    opr BOXDOT(self, other:NN64): NN64
    opr CROSS(self, other:NN64): NN64
    opr BOXCROSS(self, other:NN64): NN64
    opr DOTCROSS(self, other:NN64): NN64
    opr DIV(self, other:NN64): NN64
    opr <(self, other:NN64): Boolean
    opr <=(self, other:NN64): Boolean
    opr >(self, other:NN64): Boolean
    opr >=(self, other:NN64): Boolean
    opr =(self, other:NN64): Boolean
    opr =/=(self, other:NN64): Boolean
    opr juxtaposition(self, other:NN64): NN64
    opr BITNOT(self): NN64
    opr BITAND(self, other:NN64): NN64
    opr BITOR(self, other:NN64): NN64
    opr BITXOR(self, other:NN64): NN64
    opr <<(self, other:ZZ32): NN64
    opr <<(self, other:ZZ64): NN64
    opr <<(self, other:NN32): NN64
    opr <<(self, other:NN64): NN64
    opr >>(self, other:ZZ32): NN64
    opr >>(self, other:ZZ64): NN64
    opr >>(self, other:NN32): NN64
    opr >>(self, other:NN64): NN64
    opr <<<(self, other:ZZ32): NN64
    opr <<<(self, other:ZZ64): NN64
    opr <<<(self, other:NN32): NN64
    opr <<<(self, other:NN64): NN64
    opr MIN(self, other:NN64): NN64
    opr MAX(self, other:NN64): NN64
    opr MINMAX(self, other:NN64): (NN64, NN64)
    even(self): Boolean
    odd(self): Boolean
    floorAverage(self, other: NN64): NN64
    ceilingAverage(self, other: NN64): NN64
    opr ^(self, other:NN64):NN64
end

trait IntLiteral extends { Number, Equality[\IntLiteral\] } excludes {ZZ32, ZZ64, NN32, RR64, RR32, Character, Boolean, String, NN64, ZZ}
    abstract getter asZZ32(): ZZ32
    abstract getter asZZ64(): ZZ64
    abstract getter asNN32(): NN32
    abstract getter asZZ(): ZZ
    abstract getter asNN64(): NN64
    abstract getter asRR64(): RR64

    opr |self| : IntLiteral
    opr -(self): IntLiteral
    opr BOXMINUS(self): IntLiteral
    opr DOTMINUS(self): IntLiteral
    opr +(self, other:IntLiteral): IntLiteral
    opr BOXPLUS(self, other:IntLiteral): IntLiteral
    opr DOTPLUS(self, other:IntLiteral): IntLiteral
    opr -(self, other:IntLiteral): IntLiteral 
    opr BOXMINUS(self, other:IntLiteral): IntLiteral 
    opr DOTMINUS(self, other:IntLiteral): IntLiteral 
    opr <(self, other:IntLiteral): Boolean 
    opr <=(self, other:IntLiteral): Boolean 
    opr >(self, other:IntLiteral): Boolean 
    opr >=(self, other:IntLiteral): Boolean 
    opr =(self, other:IntLiteral): Boolean 
    opr =/=(self, other:IntLiteral): Boolean 
    opr juxtaposition(self, other:IntLiteral): IntLiteral
    opr DOT(self, other:IntLiteral): IntLiteral 
    opr BOXDOT(self, other:IntLiteral): IntLiteral 
    opr CROSS(self, other:IntLiteral): IntLiteral 
    opr BOXCROSS(self, other:IntLiteral): IntLiteral 
    opr DOTCROSS(self, other:IntLiteral): IntLiteral 
    opr DIV(self, other:IntLiteral): IntLiteral
    opr BITNOT(self): IntLiteral 
    opr BITAND(self, other:IntLiteral): IntLiteral 
    opr BITOR(self, other:IntLiteral): IntLiteral 
    opr BITXOR(self, other:IntLiteral): IntLiteral
    opr MIN(self, other:IntLiteral): IntLiteral 
    opr MAX(self, other:IntLiteral): IntLiteral 
    opr MINMAX(self, other:IntLiteral): (IntLiteral, IntLiteral) 
    opr CHOOSE(self, other:IntLiteral): IntLiteral
    even(self): Boolean
    odd(self): Boolean
end

trait RR64 extends { Number, Equality[\RR64\] } excludes ZZ64
    coerce(x: FloatLiteral)
    coerce(x: RR32)
    getter isNaN(): Boolean 
    opr |self| : RR64
    opr -(self): RR64
    opr +(self, other:RR64): RR64
    opr -(self, other:RR64): RR64
    opr <(self, other:RR64): Boolean
    opr <=(self, other:RR64): Boolean
    opr >(self, other:RR64): Boolean
    opr >=(self, other:RR64): Boolean
    opr =(self, other:RR64): Boolean
    opr juxtaposition(self, other:RR64): RR64
    opr DOT(self, other:RR64): RR64
    opr /(self, other:RR64): RR64
    opr MIN(self, other:RR64): RR64 
    opr MAX(self, other:RR64): RR64 
    opr MINNUM(self, other:RR64): RR64 
    opr MAXNUM(self, other:RR64): RR64 
    opr MINNUMMAX(self, other:RR64): (RR64, RR64) 
    opr MINMAXNUM(self, other:RR64): (RR64, RR64) 
    opr ^(self, other:RR64): RR64
    opr ^(self, other:ZZ32): RR64
    opr |\self/|:RR64
    opr |/self\|:RR64
(*    ceiling(self):RR64
    floor(self):RR64 *)
    opr SQRT(self):RR64
end

trait RR32 extends { Number, Equality[\RR32\] } excludes { ZZ64, ZZ32, RR64 }
    coerce(x: FloatLiteral)
end

trait FloatLiteral excludes {RR32, RR64}
    abstract getter asRR32(): RR32
    abstract getter asRR64(): RR64
end


trait Boolean   extends { Equality[\Boolean\] }
    excludes { String, Number } 
  getter holds(): Boolean
  getter get(): ()
  getter size(): ZZ32
  opr |self| : ZZ32

  opr NOT(self):Boolean
  opr AND(self, other:Boolean):Boolean
  opr AND(self, other:()->Boolean):Boolean
  opr OR(self, other:Boolean):Boolean
  opr OR(self, other:()->Boolean):Boolean
  opr XOR(self, other:Boolean):Boolean
  opr OPLUS(self, other:Boolean):Boolean
  opr NEQV(self, other:Boolean):Boolean
  opr EQV(self, other:Boolean):Boolean
  opr <->(self, other:Boolean):Boolean
  opr ->(self, other:Boolean):Boolean
  opr NAND(self, other:Boolean):Boolean
  opr NOR(self, other:Boolean):Boolean

  opr =(self, other:Boolean): Boolean
end

(*)true: Boolean
(*)false: Boolean

makeCharacter(n: ZZ32): Character

trait Character extends Equality[\Character\] excludes { String, Number, Boolean } 
    getter codePoint(): ZZ32

    opr <(self, other:Character): Boolean
    opr <=(self, other:Character): Boolean
    opr >(self, other:Character): Boolean
    opr >=(self, other:Character): Boolean
    opr =(self, other:Character): Boolean
    opr =/=(self, other:Character): Boolean

    opr LNSIM(self, other:Character): Boolean
    opr LESSSIM(self, other:Character): Boolean
    opr GNSIM(self, other:Character): Boolean
    opr GTRSIM(self, other:Character): Boolean
    opr SIMEQ(self, other:Character): Boolean
    opr NSIMEQ(self, other:Character): Boolean

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
    isSurrogatePair(self, low: Character): Boolean
    isTitleCase(self): Boolean
    isUnicodeIdentifierPart(self): Boolean
    isUnicodeIdentifierStart(self): Boolean
    isUpperCase(self): Boolean
    isValidCodePoint(self): Boolean
    isWhitespace(self): Boolean
    javaDigit(self, radix: ZZ32): ZZ32
    toLowerCase(self): Character
    toTitleCase(self): Character
    toUpperCase(self): Character
end

trait ZZ32Vector excludes { String, Number, Boolean, Character }
  getter shape(): (ZZ32, ZZ32)
  getter nrows(): ZZ32
  getter ncols(): ZZ32
  getter copy(): ZZ32Vector
  getValue(i:ZZ32): ZZ32
  getValue(i:ZZ32, j:ZZ32): ZZ32
  putValue(i:ZZ32, v:ZZ32): ()
  putValue(i:ZZ32, j:ZZ32, v:ZZ32): ()
  opr[i:ZZ32]: ZZ32
  opr[i:ZZ32, j:ZZ32]: ZZ32
  opr[i:ZZ32] := (v: ZZ32): ()
  opr[i:ZZ32, j:ZZ32] := (v: ZZ32): ()
  opr |self| : ZZ32
  fill(v: ZZ32): ZZ32Vector
  fill(f: ZZ32 -> ZZ32): ZZ32Vector
  fill2(f: (ZZ32, ZZ32) -> ZZ32): ZZ32Vector
end

trait StringVector
  getValue(i:ZZ32):String
  putValue(i:ZZ32, v:String):()
  opr[i:ZZ32] : String
  opr |self| : ZZ32
end

__makeZZ32Vector(l1: ZZ32, d1: ZZ32, l2: ZZ32, d2: ZZ32): ZZ32Vector
makeStringVector(i:ZZ32) : StringVector

trait JavaBufferedReader excludes { String, Number, Boolean, Character }
  read(): Character throws IOException
  readLine(): String throws IOException
  readk(k: ZZ32): String throws IOException
  eof(): Boolean
  ready(): Boolean throws IOException
  close(): () throws IOException
  whenUnconsumed(): () throws IOException
  consume(): () throws IOException
end

makeJavaBufferedReader(s: String): JavaBufferedReader throws FileNotFoundException

trait JavaBufferedWriter excludes { String, Number, Boolean, Character, JavaBufferedReader }
  write(c: Character): () throws IOException
  write(s: String): () throws IOException
  newLine(): () throws IOException
  flush(): () throws IOException
  close(): () throws IOException
end

makeJavaBufferedWriter(s: String): JavaBufferedWriter throws FileNotFoundException


(************************************************************
 * Generators and Reducers
 ************************************************************)

trait AllGenerators end

trait Generator[\E1 extends Any\] extends { AllGenerators } excludes { Number, Character }
    abstract getter reverse(): Generator[\E1\]
    abstract generate[\R extends Any\](r: Reduction[\R\], body: E1->R): R
    abstract map[\Gyy extends Any\](f: E1->Gyy): Generator[\Gyy\]
(*)    abstract seq(self): SequentialGenerator[\E1\]
    abstract seq(): SequentialGenerator[\E1\]
    abstract nest[\G1 extends Any\](f: E1 -> Generator[\G1\]): Generator[\G1\]
    abstract filter(f: E1 -> Condition[\()\]): Generator[\E1\]
    abstract cross[\G2 extends Any\](g: Generator[\G2\]): Generator[\(E1,G2)\]
    abstract mapReduce[\R extends Any\](body: E1->R, join: (R,R)->R, id: R): R
    abstract reduce(r: Reduction[\E1\]): E1
    abstract reduce(join: (E1,E1)->E1, id: E1): E1
    abstract loop(body :E1->()): ()
end Generator

trait SequentialGenerator[\E2 extends Any\] extends { Generator[\E2\] }
    abstract getter reverse(): SequentialGenerator[\E2\]
    abstract seq(): SequentialGenerator[\E2\]
    abstract map[\G3 extends Any\](f: E2->G3): SequentialGenerator[\G3\]
    abstract nest[\G4 extends Any\](f: E2 -> SequentialGenerator[\G4\]): SequentialGenerator[\G4\]
    abstract filter(f: E2 -> Condition[\()\]): SequentialGenerator[\E2\]
    abstract cross[\F1 extends Any\](g: SequentialGenerator[\F1\]): SequentialGenerator[\(E2,F1)\]
end SequentialGenerator

trait Reduction[\R extends Any\]
    getter reverse(): Reduction[\R\]
    abstract getter id(): R
    abstract join(a: R, b: R): R
end

trait Condition[\E18 extends Any\] extends { SequentialGenerator[\E18\] }
(*)        excludes { String, ZZ, ZZ32, ZZ64, NN32, NN64, IntLiteral, RR32, RR64, ZZ32Vector, StringVector }
    abstract getter isEmpty(): Boolean 
    abstract getter nonEmpty(): Boolean
    abstract getter holds(): Boolean
    abstract getter size(): ZZ32
    abstract getter get(): E18 throws NotFound
    abstract getter reverse(): Condition[\E18\]
    abstract getDefault(defaultValue: E18): E18
(*)    abstract opr |self| : ZZ32
    abstract generate[\G11 extends Any\](r: Reduction[\G11\], body: E18 -> G11): G11
    abstract map[\G12 extends Any\](f: E18->G12): Condition[\G12\]
    seq(): Condition[\E18\]
    abstract nest[\G13 extends Any\](f: E18 -> Generator[\G13\]): Generator[\G13\]
    abstract nest[\G18 extends Any\](f: E18 -> SequentialGenerator[\G18\]): SequentialGenerator[\G18\]
    abstract cross[\G14 extends Any\](g: Generator[\G14\]): Generator[\(E18,G14)\]
    abstract cross[\G17 extends Any\](g: SequentialGenerator[\G17\]): SequentialGenerator[\(E18,G17)\]
    abstract mapReduce[\R extends Any\](body: E18->R, _:(R,R)->R, id:R): R
    abstract reduce(_:(E18,E18)->E18, id:E18): E18
    abstract reduce(r: Reduction[\E18\]): E18
    abstract loop(f:E18->()): ()
    abstract cond[\G10 extends Any\](t: E18 -> G10, e: () -> G10): G10
(*)    abstract opr OR(self, other: () -> E18): E18
end Condition


(* Option type *)

value trait Option[\E19 extends Any\] extends { Condition[\E19\] }
        comprises { NoneObject[\E19\], Some[\E19\] }
  coerce(_: None)
(*)  opr SQCAP(self, other: Option[\E19\]): Option[\E19\]
  seq(): Option[\E19\]
  abstract filter(f: E19 -> Condition[\()\]): Option[\E19\]
end

value object Some[\E20 extends Any\](x: E20) extends Option[\E20\] end

value object NoneObject[\E21 extends Any\] extends Option[\E21\] end

value object None end


(************************************************************
* Comparison values
************************************************************)

(*) trait Comparison
(*) (*)        extends { StandardPartialOrder[\Comparison\] }
(*) (*)           extends { SnerdEquality[\Comparison\] }
(*)         extends { Equality[\Comparison\] }
(*)         comprises { Unordered, TotalComparison }
(*)         excludes { String, Number, Boolean, Character, JavaBufferedReader, JavaBufferedWriter }
(*)     opr LEXICO(self, other:Comparison): Comparison
(*)     opr LEXICO(self, other:()->Comparison): Comparison
(*)     opr SQCAP(self, other:Comparison): Comparison
(*)     opr SQCAP(self, other:()->Comparison): Comparison
(*)     opr CONVERSE(self): Comparison
(*)     (*) This stuff ought to be provided by Equality[\Comparison\].
(*)     opr =(self, other:Comparison): Boolean
(*)     (*) This stuff ought to be provided by StandardPartialOrder.
(*)     opr CMP(self, other:Comparison): Comparison
(*)     opr <(self, other:Comparison): Boolean
(*)     opr >(self, other:Comparison): Boolean
(*)     opr <=(self, other:Comparison): Boolean
(*)     opr >=(self, other:Comparison): Boolean
(*) end

(*) object Unordered extends Comparison end

(*) trait TotalComparison
(*) (*)     extends { Comparison, StandardTotalOrder[\TotalComparison\] }
(*)         extends { Comparison }
(*)         comprises { LessThan, EqualTo, GreaterThan }
(*)     opr LEXICO(self, other:TotalComparison): TotalComparison
(*)     opr LEXICO(self, other:()->TotalComparison): TotalComparison
(*)     opr CMP(self, other:TotalComparison): TotalComparison
(*) end

(*) object LessThan extends TotalComparison end

(*) object GreaterThan extends TotalComparison end

(*) object EqualTo extends TotalComparison end

(************************************************************
* Exception hierarchy
************************************************************)

trait Exception comprises { UncheckedException, CheckedException }
end

trait UncheckedException extends Exception excludes CheckedException
end

object NegativeLength extends UncheckedException end

object DivisionByZero extends UncheckedException end

object IntegerOverflow extends UncheckedException end

trait CheckedException extends Exception excludes UncheckedException
end

trait IOException extends CheckedException
end

object CompilerFailureDetectedAtRunTime extends UncheckedException
end

object IOFailure(s: String) extends IOException
    getter asString(): String
end

object FileNotFoundException(s: String) extends IOException
    getter asString(): String
end

(************************************************************
* Random numbers
************************************************************)

random(i:RR64): RR64
randomZZ32(x:ZZ32): ZZ32

(************************************************************
* Character properties
************************************************************)

characterMinCodePoint: ZZ32
characterMaxCodePoint: ZZ32

end
