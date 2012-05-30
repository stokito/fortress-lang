(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

(**
  \chapter{Negated Relational Operators}
  \chaplabel{lib:negated-relational-operators}
 **)

(** \section{Negated Equivalence Operators} **)

                                              (** \Function { **)
opr NSEQV(x: Any, y: Any): Boolean
                                              (** } \seclabel{operator-NSEQV} **)

(**
  The infix operator \EXP{\not\sequiv} applies \EXP{\neg} to the
  result of \EXP{\sequiv} on the same operands.
 **)

                                              (** \Function { **)
opr NEQV[\T extends BinaryPredicate[\T,EQV\]\](x: T, y: T): Boolean
                                              (** } \seclabel{operator-NEQV} **)

                                              (** \Function* { **)
opr NEQV[\T extends BinaryIntervalPredicate[\T,EQV\]\](x: T, y: T): BooleanInterval
                                              (** } \seclabel{interval-operator-NEQV} **)

(**
  The infix operator \EXP{\not\equiv} applies \EXP{\neg} to the
  result of \EXP{\equiv} on the same operands.
 **)

(** \Function { **)
opr NE[\T extends BinaryPredicate[\T,=\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NE} **)

(** \Function* { **)
opr NE[\T extends BinaryIntervalPredicate[\T,=\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NE} **)

(**
  The infix operator \EXP{\neq} applies \EXP{\neg} to the result of
  \EXP{=} on the same operands.
 **)

(** \Function { **)
opr NSIMEQ[\T extends BinaryPredicate[\T,SIMEQ\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NSIMEQ} **)

(** \Function* { **)
opr NSIMEQ[\T extends BinaryIntervalPredicate[\T,SIMEQ\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NSIMEQ} **)

(**
  The infix operator \EXP{\not\simeq} applies \EXP{\neg} to the
  result of \EXP{\simeq} on the same operands.
 **)

(** \Function { **)
opr NAPPROX[\T extends BinaryPredicate[\T,APPROX\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NAPPROX} **)

(**
  The infix operator \EXP{\not\approx} applies \EXP{\neg} to the
  result of \EXP{\approx} on the same operands.
 **)

(** \Function { **)
opr NAPPROX[\T extends BinaryIntervalPredicate[\T,APPROX\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NAPPROX} **)


(** \section{Negated Plain Comparison Operators} **)

(** \Function { **)
opr NLT[\T extends BinaryPredicate[\T,LT\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NLT} **)

(** \Function* { **)
opr NLT[\T extends BinaryIntervalPredicate[\T,LT\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NLT} **)

(**
  The infix operator \EXP{\nless} applies \EXP{\neg} to the result of
  \EXP{<} on the same operands.
 **)

(** \Function { **)
opr NLE[\T extends BinaryPredicate[\T,LE\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NLE} **)

(** \Function* { **)
opr NLE[\T extends BinaryIntervalPredicate[\T,LE\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NLE} **)

(**
  The infix operator \EXP{\nleq} applies \EXP{\neg} to the result of
  \EXP{\leq} on the same operands.
 **)

(** \Function { **)
opr NGE[\T extends BinaryPredicate[\T,GE\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NGE} **)

(** \Function* { **)
opr NGE[\T extends BinaryIntervalPredicate[\T,GE\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NGE} **)

(**
  The infix operator \EXP{\ngeq} applies \EXP{\neg} to the result of
  \EXP{\geq} on the same operands.
 **)

(** \Function { **)
opr NGT[\T extends BinaryPredicate[\T,GT\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NGT} **)

(** \Function* { **)
opr NGT[\T extends BinaryIntervalPredicate[\T,GT\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NGT} **)

(**
  The infix operator \EXP{\ngtr} applies \EXP{\neg} to the result of
  \EXP{>} on the same operands.
 **)


(** \section{Negated Set Comparison Operators} **)

(** \Function { **)
opr NSUBSET[\T extends BinaryPredicate[\T,SUBSET\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NSUBSET} **)

(** \Function* { **)
opr NSUBSET[\T extends BinaryIntervalPredicate[\T,SUBSET\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NSUBSET} **)

(**
  The infix operator \EXP{\not\subset} applies \EXP{\neg} to the
  result of \EXP{\subset} on the same operands.
 **)

(** \Function { **)
opr NSUBSETEQ[\T extends BinaryPredicate[\T,SUBSETEQ\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NSUBSETEQ} **)

(** \Function* { **)
opr NSUBSETEQ[\T extends BinaryIntervalPredicate[\T,SUBSETEQ\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NSUBSETEQ} **)

(**
  The infix operator \EXP{\nsubseteq} applies \EXP{\neg} to the
  result of \EXP{\subseteq} on the same operands.
 **)

(** \Function { **)
opr NSUPSETEQ[\T extends BinaryPredicate[\T,SUPSETEQ\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NSUPSETEQ} **)

(** \Function* { **)
opr NSUPSETEQ[\T extends BinaryIntervalPredicate[\T,SUPSETEQ\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NSUPSETEQ} **)

(**
  The infix operator \EXP{\nsupseteq} applies \EXP{\neg} to the
  result of \EXP{\supseteq} on the same operands.
 **)

(** \Function { **)
opr NSUPSET[\T extends BinaryPredicate[\T,SUPSET\]\](x: T, y: T): Boolean
(** }  \seclabel{operator-NSUPSET} **)

(** \Function* { **)
opr NSUPSET[\T extends BinaryIntervalPredicate[\T,SUPSET\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NSUPSET} **)

(**
  The infix operator \EXP{\not\supset} applies \EXP{\neg} to the
  result of \EXP{\supset} on the same operands.
 **)

(**
  \section{Negated Square Comparison Operators}
 **)

(** \Function { **)
opr NSQSUBSET[\T extends BinaryPredicate[\T,SQSUBSET\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NSQSUBSET} **)

(** \Function* { **)
opr NSQSUBSET[\T extends BinaryIntervalPredicate[\T,SQSUBSET\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NSQSUBSET} **)

(**
  The infix operator \EXP{\not\sqsubset} applies \EXP{\neg} to the
  result of \EXP{\sqsubset} on the same operands.
 **)

(** \Function { **)
opr NSQSUBSETEQ[\T extends BinaryPredicate[\T,SQSUBSETEQ\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NSQSUBSETEQ} **)

(** \Function* { **)
opr NSQSUBSETEQ[\T extends BinaryIntervalPredicate[\T,SQSUBSETEQ\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NSQSUBSETEQ} **)

(**
  The infix operator \EXP{\not\sqsubseteq} applies \EXP{\neg} to the
  result of \EXP{\sqsubseteq} on the same operands.
 **)

(** \Function { **)
opr NSQSUPSETEQ[\T extends BinaryPredicate[\T,SQSUPSETEQ\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NSQSUPSETEQ} **)

(** \Function* { **)
opr NSQSUPSETEQ[\T extends BinaryIntervalPredicate[\T,SQSUPSETEQ\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NSQSUPSETEQ} **)

(**
  The infix operator \EXP{\not\sqsupseteq} applies \EXP{\neg} to the
  result of \EXP{\sqsupseteq} on the same operands.
 **)

(** \Function { **)
opr NSQSUPSET[\T extends BinaryPredicate[\T,SQSUPSET\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NSQSUPSET} **)

(** \Function* { **)
opr NSQSUPSET[\T extends BinaryIntervalPredicate[\T,SQSUPSET\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NSQSUPSET} **)

(**
  The infix operator \EXP{\not\sqsupset} applies \EXP{\neg} to the
  result of \EXP{\sqsupset} on the same operands
 **)

(** \section{Negated Curly Comparison Operators} **)

(** \Function { **)
opr NPREC[\T extends BinaryPredicate[\T,PREC\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NPREC} **)

(** \Function* { **)
opr NPREC[\T extends BinaryIntervalPredicate[\T,PREC\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NPREC} **)

(**
  The infix operator \EXP{\nprec} applies \EXP{\neg} to the result of
  \EXP{\prec} on the same operands.
 **)

(** \Function { **)
opr NPRECEQ[\T extends BinaryPredicate[\T,PRECEQ\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NPRECEQ} **)

(** \Function* { **)
opr NPRECEQ[\T extends BinaryIntervalPredicate[\T,PRECEQ\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NPRECEQ} **)

(**
  The infix operator \EXP{\not\preceq} applies \EXP{\neg} to the
  result of \EXP{\preceq} on the same operands.
 **)

(** \Function { **)
opr NSUCCEQ[\T extends BinaryPredicate[\T,SUCCEQ\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NSUCCEQ} **)

(** \Function* { **)
opr NSUCCEQ[\T extends BinaryIntervalPredicate[\T,SUCCEQ\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NSUCCEQ} **)

(**
  The infix operator \EXP{\not\succeq} applies \EXP{\neg} to the
  result of \EXP{\succeq} on the same operands.
 **)

(** \Function { **)
opr NSUCC[\T extends BinaryPredicate[\T,SUCC\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NSUCC} **)

(** \Function* { **)
opr NSUCC[\T extends BinaryIntervalPredicate[\T,SUCC\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NSUCC} **)

(**
  The infix operator \EXP{\nsucc} applies \EXP{\neg} to the result of
  \EXP{\succ} on the same operands.
 **)

(** \section{Negated Miscellaneous Relational Operators} **)

(** \Function { **)
opr NOTIN[\T extends BinaryPredicate[\T,IN\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NOTIN} **)

(** \Function* { **)
opr NOTIN[\T extends BinaryIntervalPredicate[\T,IN\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NOTIN} **)

(**
  The infix operator \EXP{\not\in} applies \EXP{\neg} to the result
  of \EXP{\in} on the same operands.
 **)

(** \Function { **)
opr NOTNI[\T extends BinaryPredicate[\T,CONTAINS\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NOTNI} **)

(** \Function* { **)
opr NOTNI[\T extends BinaryIntervalPredicate[\T,CONTAINS\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NOTNI} **)

(**
  The infix operator \EXP{\not\ni} applies \EXP{\neg} to the result
  of \EXP{\ni} on the same operands.
 **)

(** \section{Other Negated Operators} **)

(** \Function { **)
opr NPARALLEL[\T extends BinaryPredicate[\T,PARALLEL\]\](x: T, y: T): Boolean
(** } \seclabel{operator-NPARALLEL} **)

(** \Function* { **)
opr NPARALLEL[\T extends BinaryIntervalPredicate[\T,PARALLEL\]\](x: T, y: T): BooleanInterval
(** } \seclabel{interval-operator-NPARALLEL} **)

(**
  The infix operator \EXP{\nparallel} applies \EXP{\neg} to the
  result of \EXP{\parallel} on the same operands.
 **)
