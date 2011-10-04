/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.exceptions.FortressException;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.Driver;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.interpreter.glue.IndexedArrayWrapper;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Voidoid;
import edu.rice.cs.plt.tuple.Option;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class LHSEvaluator extends NodeAbstractVisitor<Voidoid> {
    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forSubscriptExpr(com.sun.fortress.interpreter.nodes.SubscriptExpr)
     */
    @Override
    public Voidoid forSubscriptExpr(SubscriptExpr x) {
        Expr obj = x.getObj();
        FValue arr = obj.accept(evaluator);
        List<FValue> indices = evaluator.evalExprListParallel(x.getSubs());
        List<FValue> args = new ArrayList<FValue>(1 + indices.size());
        args.add(value);
        args.addAll(indices);
        Option<Op> op = x.getOp();
        // Should evaluate obj._[_]:=(subs, value)
        String opString = "_[_]:=";
        if (op.isSome()) {
            opString = NodeUtil.nameString(op.unwrap()) + ":=";
        }
        evaluator.invokeMethod(arr, opString, args, x);
        return null;
    }

    public Voidoid forFieldRefCommon(FieldRef x, Name what) {
        Expr from = x.getObj();
        // TODO need to generalize to dotted names.
        FValue val = from.accept(evaluator);
        if (val instanceof FObject) {
            FObject obj = (FObject) val;
            if (what instanceof Id) {
                obj.getSelfEnv().assignValue(x, ((Id) what).getText(), value);
            } else {
                bug("'what' not instanceof IDName, instead is " + what);
            }
            return null;
        } else {
            return NI.nyi("FieldRef expression, not object.field");
        }
    }

    @Override
    public Voidoid forFieldRef(FieldRef x) {
        return forFieldRefCommon(x, x.getField());
    }

    LHSEvaluator(Evaluator evaluator, FValue value) {
        this.evaluator = evaluator;
        this.value = value;
    }

    Evaluator evaluator;
    FValue value;

    public Voidoid forVarRef(VarRef x) {
        String s = x.getVarId().getText();

        Environment e = evaluator.e.getHomeEnvironment(x.getVarId());
        e = BaseEnv.toContainingObjectEnv(e, x.getLexicalDepth());

        FType ft = e.getVarTypeNull(s);
        if (ft != null) {
            // Check that variable can receive type
            if (!ft.typeMatch(value)) {
                String m = errorMsg("Type mismatch assigning ",
                                    value,
                                    " (type ",
                                    value.type(),
                                    ") to ",
                                    s,
                                    " (type ",
                                    ft,
                                    ")");
                return error(x, e, m);
            }
        }
        e.assignValue(x, s, value);
        return null;
    }

    public Voidoid forLValue(LValue x) {
        Id name = x.getName();
        Option<TypeOrPattern> type = x.getIdType();
        String s = NodeUtil.nameString(name);
        boolean mutable = x.isMutable();

        // Here we have an LHS context
        if (value instanceof IUOTuple) {
            IUOTuple iuo_tuple = (IUOTuple) value;
            FType bestGuess = FTypeTop.ONLY;
            FType outerType = null;
            // Perhaps the LHS has a type?
            if (type.isSome()) {
                Type t = NodeUtil.optTypeOrPatternToType(type).unwrap();
                outerType = EvalType.getFType(t, evaluator.e);

                //                if (outerType instanceof FAggregateType) {
                //                    bestGuess = ((FAggregateType) outerType).getElementType();
                //                } else {
                //                    bestGuess = error(x, com.sun.fortress.interpreter.evaluator.e, "Assigning matrix/vector/array to non-aggregate type " + outerType);
                //                }
            } else {
                // Take the (urk!) JOIN of the types of the array elements.
                // Do we require that they all lie on the same chain in
                // the type system?  Not sure.

                outerType = error(x, evaluator.e, "Can't infer element type for array construction");
            }

            /*
             * Next step is to copy the pieces of the paste into a newly
             * constructed array/matrix/vector.
             */
            try {
                int rank = iuo_tuple.resultRank();
                String aname = WellKnownNames.arrayTrait(rank);
                if (Glue.extendsGenericTrait(outerType, aname)) {
                    bestGuess = Glue.typeFromGeneric(outerType, aname, WellKnownNames.arrayElementTypeName);

                    // Find and invoke the generic factory arrayK[\ T, size1, ..., sizeK \] ()
                    String genericName = WellKnownNames.arrayMaker(rank);
                    int[] natParams = (int[]) iuo_tuple.resultExtents().clone();
                    // Transpose first two indices because of row/column peculiarity
                    if (natParams.length > 1) {
                        int t = natParams[0];
                        natParams[0] = natParams[1];
                        natParams[1] = t;
                    }
                    Environment wknInstantiationEnv = Driver.getFortressLibrary();

                    Simple_fcn f = Glue.instantiateGenericConstructor(wknInstantiationEnv,
                                                                      genericName,
                                                                      bestGuess,
                                                                      natParams);
                    // Now invoke f.
                    FValue theArray = f.functionInvocation(Collections.<FValue>emptyList(), x);
                    // Do the copy.
                    iuo_tuple.copyTo(new IndexedArrayWrapper(theArray));
                    evaluator.e.putValue(s, theArray);

                } else {
                    // bug("Thought this was not executed anymore");
                    // yes, it is, for matrix/vector/array pasting
                    if (outerType == null) {
                        Indexed it = new Indexed(iuo_tuple, bestGuess);

                        iuo_tuple.copyTo(it);

                        evaluator.e.putValue(s, new FArray(it, evaluator.e, x));

                    } else if (outerType instanceof FTypeArray) {
                        Indexed it = new Indexed(iuo_tuple, bestGuess);
                        iuo_tuple.copyTo(it);
                        evaluator.e.putValue(s, new FArray(it, (FTypeArray) outerType));

                    } else if (outerType instanceof FTypeMatrix) {
                        Indexed it = new Indexed(iuo_tuple, bestGuess);
                        iuo_tuple.copyTo(it);
                        evaluator.e.putValue(s, new FMatrix(it, (FTypeMatrix) outerType));

                    } else if (outerType instanceof FTypeVector) {
                        Indexed it = new Indexed(iuo_tuple, bestGuess);
                        iuo_tuple.copyTo(it);
                        evaluator.e.putValue(s, new FVector(it, (FTypeVector) outerType));
                    }
                }


            }
            catch (FortressException ex) {
                throw ex.setContext(x, evaluator.e);
            }

        } else {
            // TODO need to check the type first.
            FType outerType = null;
            // Perhaps the LHS has a type?
            try {
                if (type.isSome()) {
                    Type t = NodeUtil.optTypeOrPatternToType(type).unwrap();
                    outerType = EvalType.getFType(t, evaluator.e);
                    if (value.type().subtypeOf(outerType)) evaluator.e.putVariable(s, value, outerType);
                    else {
                        error(x, evaluator.e, errorMsg("RHS expression type ",
                                                       value.type(),
                                                       " is not assignable to LHS type ",
                                                       outerType));
                    }
                } else {
                    if (mutable) putOrAssignVariable(x, s);
                    else evaluator.e.putValue(s, value);
                }
            }
            catch (FortressException ex) {
                throw ex.setContext(x, evaluator.e);
            }
        }

        return null;
    }

    /**
     * @param s
     */
    protected void putOrAssignVariable(HasAt x, String s) {
        evaluator.e.putVariable(s, value);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forTupleExpr(com.sun.fortress.interpreter.nodes.TupleExpr)
     */
    @Override
    public Voidoid forTupleExpr(TupleExpr x) {
        if (!(value instanceof FTuple)) {
            error(x, evaluator.e, errorMsg("RHS yields non-tuple ", value));
        }
        FTuple t = (FTuple) value;
        Iterator<FValue> rhsIterator = t.getVals().iterator();
        for (Expr lhs : x.getExprs()) {
            // TODO: arity matching and exotic tuple types.
            lhs.accept(new LHSEvaluator(evaluator, rhsIterator.next()));
        }

        return null;
    }
}
