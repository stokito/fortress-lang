/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.interpreter.nodes;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.*;

/** An interface for visitors over Node that do not return a value. */
public interface NodeVisitor_void {

  /** Process an instance of AbstractNode. */
  public void forAbstractNode(AbstractNode that);

  /** Process an instance of ArrayComprehensionClause. */
  public void forArrayComprehensionClause(ArrayComprehensionClause that);

  /** Process an instance of Binding. */
  public void forBinding(Binding that);

  /** Process an instance of CaseClause. */
  public void forCaseClause(CaseClause that);

  /** Process an instance of Contract. */
  public void forContract(Contract that);

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

  /** Process an instance of Component. */
  public void forComponent(Component that);

  /** Process an instance of Api. */
  public void forApi(Api that);

  /** Process an instance of DoFront. */
  public void forDoFront(DoFront that);

  /** Process an instance of EnsuresClause. */
  public void forEnsuresClause(EnsuresClause that);

  /** Process an instance of Entry. */
  public void forEntry(Entry that);

  /** Process an instance of Export. */
  public void forExport(Export that);

  /** Process an instance of Expr. */
  public void forExpr(Expr that);

  /** Process an instance of AsExpr. */
  public void forAsExpr(AsExpr that);

  /** Process an instance of AsIfExpr. */
  public void forAsIfExpr(AsIfExpr that);

  /** Process an instance of Block. */
  public void forBlock(Block that);

  /** Process an instance of CaseExpr. */
  public void forCaseExpr(CaseExpr that);

  /** Process an instance of Do. */
  public void forDo(Do that);

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

  /** Process an instance of ArrayComprehension. */
  public void forArrayComprehension(ArrayComprehension that);

  /** Process an instance of SetComprehension. */
  public void forSetComprehension(SetComprehension that);

  /** Process an instance of MapComprehension. */
  public void forMapComprehension(MapComprehension that);

  /** Process an instance of ListComprehension. */
  public void forListComprehension(ListComprehension that);

  /** Process an instance of Generator. */
  public void forGenerator(Generator that);

  /** Process an instance of Id. */
  public void forId(Id that);

  /** Process an instance of DottedId. */
  public void forDottedId(DottedId that);

  /** Process an instance of Fun. */
  public void forFun(Fun that);

  /** Process an instance of Name. */
  public void forName(Name that);

  /** Process an instance of Enclosing. */
  public void forEnclosing(Enclosing that);

  /** Process an instance of Opr. */
  public void forOpr(Opr that);

  /** Process an instance of PostFix. */
  public void forPostFix(PostFix that);

  /** Process an instance of SubscriptAssign. */
  public void forSubscriptAssign(SubscriptAssign that);

  /** Process an instance of SubscriptOp. */
  public void forSubscriptOp(SubscriptOp that);

  /** Process an instance of IfClause. */
  public void forIfClause(IfClause that);

  /** Process an instance of Op. */
  public void forOp(Op that);

  /** Process an instance of BoolParam. */
  public void forBoolParam(BoolParam that);

  /** Process an instance of DimensionParam. */
  public void forDimensionParam(DimensionParam that);

  /** Process an instance of IntParam. */
  public void forIntParam(IntParam that);

  /** Process an instance of NatParam. */
  public void forNatParam(NatParam that);

  /** Process an instance of OperatorParam. */
  public void forOperatorParam(OperatorParam that);

  /** Process an instance of SimpleTypeParam. */
  public void forSimpleTypeParam(SimpleTypeParam that);

  /** Process an instance of TypeCaseClause. */
  public void forTypeCaseClause(TypeCaseClause that);
}
