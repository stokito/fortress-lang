(*******************************************************************************
    Copyright 2008,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api CompilerBuiltin
import CompilerAlgebra.{ Equality }
import AnyType.{Any}


trait Object extends Any
    getter asString(): String
    getter asExprString(): String
    getter asDebugString(): String
end Object

nanoTime(): RR64

trait String
(*)    coerce(n: ZZ32) 
(*)    coerce(n: ZZ64) 
    getter isEmpty(): Boolean
    opr <(self, b:String): Boolean
    opr =(self, b: String): Boolean
    opr |self| : ZZ32
    opr || (self, b:String):String
    opr juxtaposition(self, b:String): String
    opr[i:ZZ32] : ZZ32
    substring(lo:ZZ32, hi:ZZ32):String
    opr ^(self, n: ZZ32): String
end

object FlatString extends String
end FlatString

print(s:String):()
print(c:Character):()
print(x:ZZ32):()
print(x:ZZ64):()
print(x:RR64):()

println(s:String):()
println(c:Character):()
(*) println(x:Object): ()
(*) println():()
(*) println(x:Any):()
println(x:ZZ32):()
println(x:ZZ64):()
(*) println(x:RR32):()
println(x:RR64):()
(*) println(x: (Any, Any)):()
(*) println(x: (Any, Any, Any)):()
(*) println(x: (Any, Any, Any, Any)):()
(*) println[\A,B,C,D,E\](x: (A,B,C,D,E)):()
(*) println[\A,B,C,D,E,F\](x: (A,B,C,D,E,F)):()
(*) println[\A,B,C,D,E,F,G\](x: (A,B,C,D,E,F,G)):()

errorPrintln(s:String):()
errorPrintln(c:Character):()
(*) errorPrintln(x:Object): ()
(*) errorPrintln():()
(*) errorPrintln(x:Any):()
errorPrintln(x:ZZ32):()
errorPrintln(x:ZZ64):()
(*) errorPrintln(x:RR32):()
errorPrintln(x:RR64):()
(*) errorPrintln[\A,B\](x: (A,B)):()
(*) errorPrintln[\A,B,C\](x: (A,B,C)):()
(*) errorPrintln[\A,B,C,D\](x: (A,B,C,D)):()
(*) errorPrintln[\A,B,C,D,E\](x: (A,B,C,D,E)):()
(*) errorPrintln[\A,B,C,D,E,F\](x: (A,B,C,D,E,F)):()
(*) errorPrintln[\A,B,C,D,E,F,G\](x: (A,B,C,D,E,F,G)):()

strToInt(s:String):ZZ32

trait Number excludes { String }
end

trait ZZ64 extends Number excludes RR64
    coerce(x: IntLiteral)
    coerce(x: ZZ32) 
    getter asZZ32(): ZZ32 
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
    opr MIN(self, other:ZZ64): ZZ64 
    opr MAX(self, other:ZZ64): ZZ64 
    opr MINMAX(self, other:ZZ64): (ZZ64, ZZ64) 
    opr CHOOSE(self, other:ZZ64): ZZ64
    even(self): Boolean
    odd(self): Boolean
end

trait ZZ32 extends Number excludes { ZZ64, RR32, RR64 }
    coerce(x: IntLiteral)
    getter asZZ32(): ZZ32
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
    opr MIN(self, other:ZZ32): ZZ32 
    opr MAX(self, other:ZZ32): ZZ32 
    opr MINMAX(self, other:ZZ32): (ZZ32, ZZ32)
    opr CHOOSE(self, other:ZZ32): ZZ32 
    even(self): Boolean
    odd(self): Boolean
    opr ^(self, other:ZZ32): ZZ32
    asRR64(): RR64

end

trait IntLiteral excludes {ZZ32, ZZ64}
    abstract getter asZZ32(): ZZ32
    abstract getter asZZ64(): ZZ64
(*
    abstract getter asNN32(): NN32
    abstract getter asZZ(): ZZ
    abstract getter asRR32(): RR32
*)
    abstract getter asRR64(): RR64
end

trait RR64 extends Number excludes ZZ64
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
end

trait RR32 extends Number excludes { ZZ64, ZZ32, RR64 }
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

true: Boolean
false: Boolean

makeCharacter(n: ZZ32): Character

trait Character excludes { String, Number, Boolean } 
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

trait ZZ32Vector 
      getValue(i:ZZ32):ZZ32
      getValue(i:ZZ32, j:ZZ32):ZZ32
      putValue(i:ZZ32, v:ZZ32):()
      putValue(i:ZZ32, j:ZZ32, v:ZZ32):()
      opr[i:ZZ32] : ZZ32
      opr[i:ZZ32, j:ZZ32]: ZZ32
      opr |self| : ZZ32
      nrows():ZZ32
      ncols():ZZ32
end

trait StringVector
      getValue(i:ZZ32):String
      putValue(i:ZZ32, v:String):()
      opr[i:ZZ32] : String
      opr |self| : ZZ32
end

makeZZ32Vector(i:ZZ32) : ZZ32Vector
makeZZ32Vector(i:ZZ32, j:ZZ32) : ZZ32Vector
makeStringVector(i:ZZ32) : StringVector

trait JavaBufferedReader excludes { String, Number, Boolean, Character }
  getter asString(): String
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
  getter asString(): String
  write(c: Character): () throws IOException
  write(s: String): () throws IOException
  newLine(): () throws IOException
  flush(): () throws IOException
  close(): () throws IOException
end

makeJavaBufferedWriter(s: String): JavaBufferedWriter throws FileNotFoundException

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
