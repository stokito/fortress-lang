(*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api TestComparisonLibrary

trait TestComparison
(*)        extends { StandardPartialOrder[\TestComparison\] }
        comprises { TestUnordered, TestTotalComparison }
    getter asString(): String
    (** Lexicographic ordering.  An associative operator.
        Leftmost non-equal comparison dictates result. *)
    opr LEXICO(self, other:TestComparison): TestComparison
    opr LEXICO(self, other:()->TestComparison): TestComparison
    (** Symmetric comparison (product ordering).  A commutative
        and associative operator. *)
    opr SQCAP(self, other:TestComparison): TestComparison
    opr SQCAP(self, other:()->TestComparison): TestComparison
    opr CONVERSE(self): TestComparison
    (*) This stuff ought to be provided by Equality[\TestComparison\].
    opr =(self, other:TestComparison): Boolean
    (*) This stuff ought to be provided by StandardPartialOrder.
    opr CMP(self, other:TestComparison): TestComparison
    abstract opr <(self, other:TestComparison): Boolean
    opr >(self, other:TestComparison): Boolean
    opr <=(self, other:TestComparison): Boolean
    opr >=(self, other:TestComparison): Boolean
end

(** TestUnordered is the outcome of a CMP b when a and b are partially
    ordered and no ordering relationship exists between them. **)
object TestUnordered extends TestComparison end

trait TestTotalComparison
(*)     extends { TestComparison, StandardTotalOrder[\TestTotalComparison\] }
        extends { TestComparison }
        comprises { TestLessThan, TestEqualTo, TestGreaterThan }
    getter asString(): String
    opr LEXICO(self, other:TestTotalComparison): TestTotalComparison
    opr LEXICO(self, other:()->TestTotalComparison): TestTotalComparison
    opr CMP(self, other:TestTotalComparison): TestTotalComparison
end

object TestLessThan extends TestTotalComparison end

object TestGreaterThan extends TestTotalComparison end

object TestEqualTo extends TestTotalComparison end

end TestComparisonLibrary
