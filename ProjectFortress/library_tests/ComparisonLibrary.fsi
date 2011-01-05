(*******************************************************************************
    Copyright 2011 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************)

component ComparisonLibrary

trait Equality[\Self\] comprises Self
    opr =(self, other:Self): Boolean
end

trait Comparison
        extends { StandardPartialOrder[\Comparison\] }
        comprises { Unordered, TotalComparison }
    getter asString(): String
    (*) ??? The following two should not be needed, becauise Object provides concrete definitions.
    getter asExprString(): String
    getter asDebugString(): String
    (*) Default opr
    (** Lexicographic ordering.  First non-equal comparison dictates result. *)
    opr LEXICO(self, other:Comparison): Comparison
    (** Symmetric comparison.  All comparisons
        must be compatible, or Unordered results.  Compatible means
        "all the same sense or EqualTo" in which case the sense is the
        result (EqualTo if everything's EqualTo). *)
    opr SQCAP(self, other:Comparison): Comparison
    opr SQCAP(self, other:()->Comparison): Comparison
    opr CONVERSE(self): Comparison
end

(** Unordered is the outcome of a CMP b when a and b are partially
    ordered and no ordering relationship exists between them. **)
object Unordered extends Comparison end

trait TotalComparison
(*)     extends { Comparison, StandardTotalOrder[\TotalComparison\] }
        extends { Comparison }
        comprises { LessThan, EqualTo, GreaterThan }
end

object LessThan extends TotalComparison end

object GreaterThan extends TotalComparison end

object EqualTo extends TotalComparison end

(** StandardPartialOrder is partial ordering using the operators
    <, <=, >=, >, =, and CMP.
    This is primarily for floating-point values.
    Minimal complete definition: CMP or { <,
trait StandardPartialOrder[\Self\]
        extends { Equality[\Self\] }
        comprises Self
    opr CMP(self, other:Self): Comparison
    opr <(self, other:Self): Boolean
    opr >(self, other:Self): Boolean
    opr <=(self, other:Self): Boolean
    opr >=(self, other:Self): Boolean
end

(** SquarePartialOrder is partial ordering using the operators
    SQSUBSET, SQSUBSETEQ, SQSUPSETEQ, SQSUPSET, =, and SQCMP.
    Minimal complete definition: SQCMP or { SQSUBSET,
trait SquarePartialOrder[\Self\]
        extends { Equality[\Self\] }
        comprises Self
    opr SQCMP(self, other:Self): Comparison
    opr SQSUBSET(self, other:Self): Boolean
    opr SQSUPSET(self, other:Self): Boolean
    opr SQSUBSETEQ(self, other:Self): Boolean
    opr SQSUPSETEQ(self, other:Self): Boolean
end

(** SetPartialOrder is partial ordering using the operators
    SUBSET, SUBSETEQ, SUPSETEQ, SUPSET, =, and SETCMP.
    Minimal complete definition: SETCMP or { SUBSET,
trait SetPartialOrder[\Self\]
        extends { Equality[\Self\] }
        comprises Self
    opr SETCMP(self, other:Self): Comparison
    opr SUBSET(self, other:Self): Boolean
    opr SUPSET(self, other:Self): Boolean
    opr SUBSETEQ(self, other:Self): Boolean
    opr SUPSETEQ(self, other:Self): Boolean
end

(** PrecPartialOrder is partial ordering using the operators
    PREC, PRECEQ, SUCCEQ, SUCC, =, and PRECCMP.
    Minimal complete definition: PRECCMP or { PREC,
trait PrecPartialOrder[\Self\]
        extends { Equality[\Self\] }
        comprises Self
    opr PRECCMP(self, other:Self): Comparison
    opr PREC(self, other:Self): Boolean
    opr SUCC(self, other:Self): Boolean
    opr PRECEQ(self, other:Self): Boolean
    opr SUCCEQ(self, other:Self): Boolean
end

(** %StandardMin% is a MIN operator; most types that implement %MIN%
    will implement a corresponding total order.  It's a separate type
    to account for the existence of floating point numbers, for which
    NaN counts as a bottom that is less than anything else but doesn't
    actually participate in the standard total ordering.  It is
    otherwise the case that %a MIN b = a% when %a <= b% and that
    %a MIN b = b MIN a%. **)
trait StandardMin[\T extends StandardMin[\T\]\]
    opr MIN(self, other:T): T
end

(** %StandardMax% is a MAX operator; most types that implement %MAX%
    will implement a corresponding total order.  It's a separate type
    to account for the existence of floating point numbers, for which
    NaN counts as a bottom that is less than anything else but doesn't
    actually participate in the standard total ordering.  It is
    otherwise the case that %a MAX b = a% when %a <= b% and that
    %a MAX b = b MAX a%. **)
trait StandardMax[\T extends StandardMax[\T\]\]
    opr MAX(self, other:T): T
end

(** %StandardMinMax% combines MIN and MAX operators, and provides a
    combined MINMAX operator.  This operator returns both its
    arguments; if equality is possible, self should be the leftmost
    result.  This effectively means that %(a MINMAX b)% stably sorts
    %a% and %b%.  In addition, %a MINMAX b = (a MIN b, a MAX b)% must
    always hold. **)

trait StandardMinMax[\Self\]
        extends { StandardMin[\Self\], StandardMax[\Self\] }
        comprises Self
    opr MINMAX(self, other:Self): (Self,Self)
end

(** StandardTotalOrder is the usual total order using the operators
    <, <=, >=, >, =, and CMP.
    Most values that define a comparison should do so using this.
    Minimal complete definition: either CMP or < (and it is advisable
    to define = in the latter case).  MIN and MAX respect the total
    order and are defined in the obvious way. **)
trait StandardTotalOrder[\Self\]
        extends { StandardPartialOrder[\Self\], StandardMinMax[\Self\] }
        comprises Self
    opr CMP(self, other:Self): TotalComparison
end

end
