opr NSEQV(x: Any, y: Any): Boolean
opr NEQV[\T extends BinaryPredicate[\T,EQV\]\](x: T, y: T): Boolean
opr NEQV[\T extends BinaryIntervalPredicate[\T,EQV\]\](x: T, y: T): BooleanInterval
opr NE[\T extends BinaryPredicate[\T,=\]\](x: T, y: T): Boolean
opr NE[\T extends BinaryIntervalPredicate[\T,=\]\](x: T, y: T): BooleanInterval
opr NSIMEQ[\T extends BinaryPredicate[\T,SIMEQ\]\](x: T, y: T): Boolean
opr NSIMEQ[\T extends BinaryIntervalPredicate[\T,SIMEQ\]\](x: T, y: T): BooleanInterval
opr NAPPROX[\T extends BinaryPredicate[\T,APPROX\]\](x: T, y: T): Boolean
opr NAPPROX[\T extends BinaryIntervalPredicate[\T,APPROX\]\](x: T, y: T): BooleanInterval

opr NLT[\T extends BinaryPredicate[\T,LT\]\](x: T, y: T): Boolean
opr NLT[\T extends BinaryIntervalPredicate[\T,LT\]\](x: T, y: T): BooleanInterval
opr NLE[\T extends BinaryPredicate[\T,LE\]\](x: T, y: T): Boolean
opr NLE[\T extends BinaryIntervalPredicate[\T,LE\]\](x: T, y: T): BooleanInterval
opr NGE[\T extends BinaryPredicate[\T,GE\]\](x: T, y: T): Boolean
opr NGE[\T extends BinaryIntervalPredicate[\T,GE\]\](x: T, y: T): BooleanInterval
opr NGT[\T extends BinaryPredicate[\T,GT\]\](x: T, y: T): Boolean
opr NGT[\T extends BinaryIntervalPredicate[\T,GT\]\](x: T, y: T): BooleanInterval

opr NSUBSET[\T extends BinaryPredicate[\T,SUBSET\]\](x: T, y: T): Boolean
opr NSUBSET[\T extends BinaryIntervalPredicate[\T,SUBSET\]\](x: T, y: T): BooleanInterval
opr NSUBSETEQ[\T extends BinaryPredicate[\T,SUBSETEQ\]\](x: T, y: T): Boolean
opr NSUBSETEQ[\T extends BinaryIntervalPredicate[\T,SUBSETEQ\]\](x: T, y: T): BooleanInterval
opr NSUPSETEQ[\T extends BinaryPredicate[\T,SUPSETEQ\]\](x: T, y: T): Boolean
opr NSUPSETEQ[\T extends BinaryIntervalPredicate[\T,SUPSETEQ\]\](x: T, y: T): BooleanInterval
opr NSUPSET[\T extends BinaryPredicate[\T,SUPSET\]\](x: T, y: T): Boolean
opr NSUPSET[\T extends BinaryIntervalPredicate[\T,SUPSET\]\](x: T, y: T): BooleanInterval

opr NSQSUBSET[\T extends BinaryPredicate[\T,SQSUBSET\]\](x: T, y: T): Boolean
opr NSQSUBSET[\T extends BinaryIntervalPredicate[\T,SQSUBSET\]\](x: T, y: T): BooleanInterval
opr NSQSUBSETEQ[\T extends BinaryPredicate[\T,SQSUBSETEQ\]\](x: T, y: T): Boolean
opr NSQSUBSETEQ[\T extends BinaryIntervalPredicate[\T,SQSUBSETEQ\]\](x: T, y: T): BooleanInterval
opr NSQSUPSETEQ[\T extends BinaryPredicate[\T,SQSUPSETEQ\]\](x: T, y: T): Boolean
opr NSQSUPSETEQ[\T extends BinaryIntervalPredicate[\T,SQSUPSETEQ\]\](x: T, y: T): BooleanInterval
opr NSQSUPSET[\T extends BinaryPredicate[\T,SQSUPSET\]\](x: T, y: T): Boolean
opr NSQSUPSET[\T extends BinaryIntervalPredicate[\T,SQSUPSET\]\](x: T, y: T): BooleanInterval

opr NPREC[\T extends BinaryPredicate[\T,PREC\]\](x: T, y: T): Boolean
opr NPREC[\T extends BinaryIntervalPredicate[\T,PREC\]\](x: T, y: T): BooleanInterval
opr NPRECEQ[\T extends BinaryPredicate[\T,PRECEQ\]\](x: T, y: T): Boolean
opr NPRECEQ[\T extends BinaryIntervalPredicate[\T,PRECEQ\]\](x: T, y: T): BooleanInterval
opr NSUCCEQ[\T extends BinaryPredicate[\T,SUCCEQ\]\](x: T, y: T): Boolean
opr NSUCCEQ[\T extends BinaryIntervalPredicate[\T,SUCCEQ\]\](x: T, y: T): BooleanInterval
opr NSUCC[\T extends BinaryPredicate[\T,SUCC\]\](x: T, y: T): Boolean
opr NSUCC[\T extends BinaryIntervalPredicate[\T,SUCC\]\](x: T, y: T): BooleanInterval

opr NOTIN[\T extends BinaryPredicate[\T,IN\]\](x: T, y: T): Boolean
opr NOTIN[\T extends BinaryIntervalPredicate[\T,IN\]\](x: T, y: T): BooleanInterval
opr NOTNI[\T extends BinaryPredicate[\T,CONTAINS\]\](x: T, y: T): Boolean
opr NOTNI[\T extends BinaryIntervalPredicate[\T,CONTAINS\]\](x: T, y: T): BooleanInterval

opr NPARALLEL[\T extends BinaryPredicate[\T,PARALLEL\]\](x: T, y: T): Boolean
opr NPARALLEL[\T extends BinaryIntervalPredicate[\T,PARALLEL\]\](x: T, y: T): BooleanInterval
