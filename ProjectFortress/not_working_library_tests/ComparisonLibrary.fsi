(*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api ComparisonLibrary
import CompilerAlgebra.{ Equality, opr = }

trait Comparison
(*)        extends { StandardPartialOrder[\Comparison\] }
(*)           extends { SnerdEquality[\Comparison\] }
        extends { Equality[\Comparison\] }
        comprises { Unordered, TotalComparison }
        excludes { Number, Boolean, Character, String }
    opr LEXICO(self, other:Comparison): Comparison
    opr LEXICO(self, other:()->Comparison): Comparison
    opr SQCAP(self, other:Comparison): Comparison
    opr SQCAP(self, other:()->Comparison): Comparison
    opr CONVERSE(self): Comparison
    (*) This stuff ought to be provided by Equality[\Comparison\].
    opr =(self, other:Comparison): Boolean
    (*) This stuff ought to be provided by StandardPartialOrder.
    opr CMP(self, other:Comparison): Comparison
    opr <(self, other:Comparison): Boolean
    opr >(self, other:Comparison): Boolean
    opr <=(self, other:Comparison): Boolean
    opr >=(self, other:Comparison): Boolean
end

object Unordered extends Comparison end

trait TotalComparison
(*)     extends { Comparison, StandardTotalOrder[\TotalComparison\] }
        extends { Comparison }
        comprises { LessThan, EqualTo, GreaterThan }
    opr LEXICO(self, other:TotalComparison): TotalComparison
    opr LEXICO(self, other:()->TotalComparison): TotalComparison
    opr CMP(self, other:TotalComparison): TotalComparison
end

object LessThan extends TotalComparison end

object GreaterThan extends TotalComparison end

object EqualTo extends TotalComparison end

end ComparisonLibrary

