package com.sun.fortress.interpreter.nodes;

/** An interface for visitors over Node that do not return a value. */
public interface NodeVisitor_void {

  /** Process an instance of AbstractNode. */
  public void forAbstractNode(AbstractNode that);

  /** Process an instance of ArrayComprehensionClause. */
  public void forArrayComprehensionClause(ArrayComprehensionClause that);

  /** Process an instance of CaseClause. */
  public void forCaseClause(CaseClause that);

  /** Process an instance of CaseParamExpr. */
  public void forCaseParamExpr(CaseParamExpr that);

  /** Process an instance of CaseParamLargest. */
  public void forCaseParamLargest(CaseParamLargest that);

  /** Process an instance of CaseParamSmallest. */
  public void forCaseParamSmallest(CaseParamSmallest that);

  /** Process an instance of Catch. */
  public void forCatch(Catch that);

  /** Process an instance of CatchClause. */
  public void forCatchClause(CatchClause that);

  /** Process an instance of Expr. */
  public void forExpr(Expr that);

  /** Process an instance of CaseExpr. */
  public void forCaseExpr(CaseExpr that);

  /** Process an instance of Block. */
  public void forBlock(Block that);

  /** Process an instance of For. */
  public void forFor(For that);

  /** Process an instance of If. */
  public void forIf(If that);

  /** Process an instance of Label. */
  public void forLabel(Label that);

  /** Process an instance of Try. */
  public void forTry(Try that);

  /** Process an instance of TypeCase. */
  public void forTypeCase(TypeCase that);

  /** Process an instance of While. */
  public void forWhile(While that);

  /** Process an instance of Accumulator. */
  public void forAccumulator(Accumulator that);

  /** Process an instance of AtomicExpr. */
  public void forAtomicExpr(AtomicExpr that);

  /** Process an instance of Exit. */
  public void forExit(Exit that);

  /** Process an instance of Spawn. */
  public void forSpawn(Spawn that);

  /** Process an instance of Throw. */
  public void forThrow(Throw that);

  /** Process an instance of TryAtomicExpr. */
  public void forTryAtomicExpr(TryAtomicExpr that);

  /** Process an instance of FloatLiteral. */
  public void forFloatLiteral(FloatLiteral that);

  /** Process an instance of IntLiteral. */
  public void forIntLiteral(IntLiteral that);

  /** Process an instance of CharLiteral. */
  public void forCharLiteral(CharLiteral that);

  /** Process an instance of StringLiteral. */
  public void forStringLiteral(StringLiteral that);

  /** Process an instance of VoidLiteral. */
  public void forVoidLiteral(VoidLiteral that);

  /** Process an instance of VarRefExpr. */
  public void forVarRefExpr(VarRefExpr that);

  /** Process an instance of Id. */
  public void forId(Id that);

  /** Process an instance of IfClause. */
  public void forIfClause(IfClause that);

  /** Process an instance of Op. */
  public void forOp(Op that);

  /** Process an instance of TypeCaseClause. */
  public void forTypeCaseClause(TypeCaseClause that);
}
