package com.sun.fortress.interpreter.nodes;

/** An interface for visitors over Node that do not return a value. */
public interface NodeVisitor_void {

  /** Process an instance of AbstractNode. */
  public void forAbstractNode(AbstractNode that);

  /** Process an instance of ArrayComprehensionClause. */
  public void forArrayComprehensionClause(ArrayComprehensionClause that);

  /** Process an instance of Expr. */
  public void forExpr(Expr that);

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

  /** Process an instance of Op. */
  public void forOp(Op that);
}
