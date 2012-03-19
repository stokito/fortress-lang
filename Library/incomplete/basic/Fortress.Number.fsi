(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

trait QQ
    extends { RR, QQ_star,
              Field[\QQ,QQ_NE,+,-,DOT,/\],
              Field[\QQ,QQ_NE,+,-,TIMES,/\],
              Field[\QQ,QQ_NE,+,-,juxtaposition,/\],
              TotalOrderOperators[\QQ,<,<=,>=,>,CMP\],
              PartialOrderAndLattice[\QQ,<=,MIN,MAX\] }
  coerce(x: Identity[\+\])
  coerce(x: Identity[\DOT\])
  coerce(x: Identity[\TIMES\])
  coerce(x: Identity[\juxtaposition\])
  coerce(x: Zero[\DOT\])
  coerce(x: Zero[\TIMES\])
  coerce(x: Zero[\juxtaposition\])
  opr juxtaposition(self, other: QQ): QQ
  opr +(self): QQ
  opr +(self, other: QQ): QQ
  opr -(self): QQ
  opr -(self, other: QQ): QQ
  opr DOT(self, other: QQ): QQ
  opr TIMES(self, other: QQ): QQ
  opr /(self): QQ_star
  opr /(self, other: QQ): QQ_splat
  opr ^(self, power: ZZ): QQ_splat
  opr <(self, other: QQ): Boolean
  opr <=(self, other: QQ): Boolean
  opr =(self, other: QQ): Boolean
  opr >=(self, other: QQ): Boolean
  opr >(self, other: QQ): Boolean
  opr CMP(self, other: QQ_star): TotalComparison
  opr CMP(self, other: QQ_splat): Comparison
  opr MAX(self, other: QQ): QQ
  opr MIN(self, other: QQ): QQ
  opr MAXNUM(self, other: QQ): QQ
  opr MINNUM(self, other: QQ): QQ
  opr |self| : QQ_GE
  signum(self): ZZ
  numerator(self): ZZ
  denominator(self): ZZ
  floor(self): ZZ
  opr |\ self /| : ZZ
  ceiling(self): ZZ
  opr |/ self \| : ZZ
  round(self): ZZ
  truncate(self): ZZ
  opr ||\ self /|| : NN
  opr ||/ self \|| : NN
  opr |||\ self /||| : NN
  opr |||/ self \||| : NN
  realpart(self): QQ
  imagpart(self): QQ
  check(self): QQ throws CastException
  check_star(self): QQ_star throws CastException
  check_LT(self): QQ_LT throws CastException
  check_LE(self): QQ_LE throws CastException
  check_GE(self): QQ_GE throws CastException
  check_GT(self): QQ_GT throws CastException
  check_NE(self): QQ_NE throws CastException
  check_star_LT(self): QQ_star_LT throws CastException
  check_star_LE(self): QQ_star_LE throws CastException
  check_star_GE(self): QQ_star_GE throws CastException
  check_star_GT(self): QQ_star_GT throws CastException
  check_star_NE(self): QQ_star_NE throws CastException
  check_splat_LT(self): QQ_splat_LT throws CastException
  check_splat_LE(self): QQ_splat_LE throws CastException
  check_splat_GE(self): QQ_splat_GE throws CastException
  check_splat_GT(self): QQ_splat_GT throws CastException
  check_splat_NE(self): QQ_splat_NE throws CastException
end

trait ZZ
    extends { QQ, ZZ_star, IntegerLike[\ZZ\],
              CommutativeRing[\ZZ,+,-,juxtaposition\],
              CommutativeRing[\ZZ,+,-,DOT\],
              CommutativeRing[\ZZ,+,-,TIMES\],
              CommutativeRing[\ZZ,BOXPLUS,BOXMINUS,BOXDOT\],
              CommutativeRing[\ZZ,BOXPLUS,BOXMINUS,BOXTIMES\],
              CommutativeRing[\ZZ,DOTPLUS,DOTMINUS,DOTTIMES\],
              TotalOrderOperators[\ZZ,<,<=,>=,>,CMP\],
              PartialOrderAndLattice[\ZZ,<=,MIN,MAX\],
              BooleanAlgebra[\ZZ,BITAND,BITOR,BITNOT,BITXOR\] }
  coerce(x: Identity[\+\])
  coerce(x: Identity[\BOXPLUS\])
  coerce(x: Identity[\DOTPLUS\])
  coerce(x: Identity[\juxtaposition\])
  coerce(x: Identity[\DOT\])
  coerce(x: Identity[\TIMES\])
  coerce(x: Identity[\BOXDOT\])
  coerce(x: Identity[\BOXTIMES\])
  coerce(x: Identity[\DOTTIMES\])
  coerce(x: Zero[\juxtaposition\])
  coerce(x: Zero[\DOT\])
  coerce(x: Zero[\TIMES\])
  coerce(x: Zero[\BOXDOT\])
  coerce(x: Zero[\BOXTIMES\])
  coerce(x: Zero[\DOTTIMES\])
  opr juxtaposition(self, other: ZZ): ZZ
  opr +(self): ZZ
  opr BOXPLUS(self): ZZ
  opr DOTPLUS(self): ZZ
  opr +(self, other: ZZ): ZZ
  opr BOXPLUS(self, other: ZZ): ZZ
  opr DOTPLUS(self, other: ZZ): ZZ
  opr -(self): ZZ
  opr BOXMINUS(self): ZZ
  opr DOTMINUS(self): ZZ
  opr -(self, other: ZZ): ZZ
  opr BOXMINUS(self, other: ZZ): ZZ
  opr DOTMINUS(self, other: ZZ): ZZ
  opr DOT(self, other: ZZ): ZZ
  opr TIMES(self, other: ZZ): ZZ
  opr BOXDOT(self, other: ZZ): ZZ
  opr BOXTIMES(self, other: ZZ): ZZ
  opr DOTTIMES(self, other: ZZ): ZZ
  opr /(self, other: ZZ): QQ_splat
  opr DIV(self, other: ZZ): ZZ throws IntegerDivideByZeroException
  opr REM(self, other: ZZ): ZZ throws IntegerDivideByZeroException
  opr MOD(self, other: ZZ): ZZ throws IntegerDivideByZeroException
  opr DIVREM(self, other: ZZ): (ZZ,ZZ) throws IntegerDivideByZeroException
  opr DIVMOD(self, other: ZZ): (ZZ,ZZ) throws IntegerDivideByZeroException
  opr | (self, other: ZZ): Boolean
  opr ^(self, power: NN): ZZ
  opr GCD(self, other: ZZ): ZZ
  opr LCM(self, other: ZZ): ZZ
  opr (self)! : NN
  opr CHOOSE(self, other: ZZ): NN
  opr <(self, other: ZZ): Boolean
  opr <=(self, other: ZZ): Boolean
  opr =(self, other: ZZ): Boolean
  opr >=(self, other: ZZ): Boolean
  opr >(self, other: ZZ): Boolean
  opr CMP(self, other: ZZ_star): TotalComparison
  opr MAX(self, other: ZZ): ZZ
  opr MIN(self, other: ZZ): ZZ
  opr MAXNUM(self, other: ZZ): ZZ
  opr MINNUM(self, other: ZZ): ZZ
  opr |self| : ZZ_GE
  signum(self): ZZ
  numerator(self): ZZ
  denominator(self): ZZ
  floor(self): ZZ
  opr |\ self /| : ZZ
  ceiling(self): ZZ
  opr |/ self \| : ZZ
  round(self): ZZ
  truncate(self): ZZ
  opr ||\ self /|| : NN
  opr ||/ self \|| : NN
  opr |||\ self /||| : NN
  opr |||/ self \||| : NN
  shift(self, k: IndexInt): ZZ
  bit(self, k: IndexInt): Bit
  opr BITNOT(self): ZZ
  opr BITAND(self, other: ZZ): ZZ
  opr BITOR(self, other: ZZ): ZZ
  opr BITXOR(self, other: ZZ): ZZ
  countBits(self): IndexInt
  countFactorsOfTwo(self): IndexInt
  integerLength(self): IndexInt
  lowBits(self, k: IndexInt): ZZ
  even(self): Boolean
  odd(self): Boolean
  prime(self): Boolean
  realpart(self): ZZ
  imagpart(self): ZZ
  check(self): ZZ throws CastException
  check_star(self): ZZ_star throws CastException
  check_LT(self): ZZ_LT throws CastException
  check_LE(self): ZZ_LE throws CastException
  check_GE(self): ZZ_GE throws CastException
  check_GT(self): ZZ_GT throws CastException
  check_NE(self): ZZ_NE throws CastException
  check_star_LT(self): ZZ_star_LT throws CastException
  check_star_LE(self): ZZ_star_LE throws CastException
  check_star_GE(self): ZZ_star_GE throws CastException
  check_star_GT(self): ZZ_star_GT throws CastException
  check_star_NE(self): ZZ_star_NE throws CastException
end
