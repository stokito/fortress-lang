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

package com.sun.fortress.interpreter.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeArray;
import com.sun.fortress.interpreter.evaluator.types.FTypeDynamic;
import com.sun.fortress.interpreter.evaluator.types.FTypeMatrix;
import com.sun.fortress.interpreter.evaluator.types.FTypeVector;
import com.sun.fortress.interpreter.evaluator.values.FArray;
import com.sun.fortress.interpreter.evaluator.values.FMatrix;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FTuple;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVector;
import com.sun.fortress.interpreter.evaluator.values.IUOTuple;
import com.sun.fortress.interpreter.evaluator.values.Indexed;
import com.sun.fortress.interpreter.evaluator.values.Method;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.interpreter.glue.IndexedArrayWrapper;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.ExtentRange;
import com.sun.fortress.nodes.MemberSelection;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.useful.Option;
import com.sun.fortress.nodes.SubscriptExpr;
import com.sun.fortress.nodes.TupleExpr;
import com.sun.fortress.nodes.TypeRef;
import com.sun.fortress.nodes.Unpasting;
import com.sun.fortress.nodes.UnpastingBind;
import com.sun.fortress.nodes.UnpastingSplit;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Voidoid;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;

public class LHSEvaluator extends NodeAbstractVisitor<Voidoid>  {
    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forSubscriptExpr(com.sun.fortress.interpreter.nodes.SubscriptExpr)
     */
    @Override
    public Voidoid forSubscriptExpr(SubscriptExpr x) {
        Expr obj = x.getObj();
        List<Expr> subs = x.getSubs();
        // Should evaluate obj.[](subs, value)
        FObject array = (FObject) obj.accept(evaluator);
        Method cl = (Method) array.getSelfEnv().getValue("[]=");
        ArrayList<FValue> subscriptsAndValue = new ArrayList<FValue>(1+subs.size());
        subscriptsAndValue.add(value);
        evaluator.evalExprList(subs, subscriptsAndValue);
        // TODO verify that 'e' is proper inference environment
        cl.applyMethod(subscriptsAndValue, array, x, evaluator.e);
        return null;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forMemberSelection(com.sun.fortress.interpreter.nodes.MemberSelection)
     */
    @Override
    public Voidoid forMemberSelection(MemberSelection x) {
        Expr from = x.getObj();
        Id what = x.getId();
        // TODO need to generalize to dotted names.
        FValue val = from.accept(evaluator);
        if (val instanceof FObject) {
            FObject obj = (FObject) val;
            obj.getSelfEnv().assignValue(x, what.getName(), value);
            return null;
        } else {
            return NI.nyi("DottedId expression, not object.field");
        }

    }


    LHSEvaluator(Evaluator evaluator, FValue value) {
        this.evaluator = evaluator; this.value = value;
    }
    Evaluator evaluator;
    FValue value;

    boolean debug = false;
    public void debugPrint(String debugString) {if (debug) System.out.println(debugString);}

    public Voidoid forVarRef(VarRef x) {
        Id var = x.getVar();
        String s = var.getName();
        Environment e = evaluator.e;
        FType ft = e.getVarTypeNull(s);
        if (ft != null) {
            // Check that variable can receive type
            if (!ft.typeMatch(value)) {
                throw new ProgramError(x, e,
                        errorMsg("TypeRef mismatch assigning ", value,
                                       " (type ", value.type(), ") to ", s, " (type ", ft, ")"));
            }
        }
        e.assignValue(x, s, value);
        return null;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forUnpastingBind(com.sun.fortress.interpreter.nodes.UnpastingBind)
     */
    @Override
    public Voidoid forUnpastingBind(UnpastingBind x) {
        Id id = x.getId();
        List<ExtentRange> dim = x.getDim();
        return super.forUnpastingBind(x);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forUnpastingSplit(com.sun.fortress.interpreter.nodes.UnpastingSplit)
     */
    @Override
    public Voidoid forUnpastingSplit(UnpastingSplit x) {
        int dim = x.getDim();
        List<Unpasting> elems = x.getElems();
        return super.forUnpastingSplit(x);
    }

    public Voidoid forLValueBind(LValueBind x) {
        Id name = x.getId();
        Option<TypeRef> type = x.getType();
        String s = name.getName();
        boolean mutable = x.isMutable();

        // Here we have an LHS context
        if (value instanceof IUOTuple) {
            IUOTuple iuo_tuple = (IUOTuple) value;
            FType bestGuess = FTypeDynamic.ONLY;
            FType outerType = null;
            // Perhaps the LHS has a type?
            if (type.isPresent()) {
                TypeRef t = type.getVal();
                outerType = EvalType.getFType(t, evaluator.e);

//                if (outerType instanceof FAggregateType) {
//                    bestGuess = ((FAggregateType) outerType).getElementType();
//                } else {
//                    throw new ProgramError(x, com.sun.fortress.interpreter.evaluator.e, "Assigning matrix/vector/array to non-aggregate type " + outerType);
//                }
            } else {
                // Take the (urk!) JOIN of the types of the array elements.
                // Do we require that they all lie on the same chain in
                // the type system?  Not sure.

                throw new ProgramError(x, evaluator.e,
                            errorMsg("Can't infer element type for array construction"));
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
                    Simple_fcn f = Glue.instantiateGenericConstructor(evaluator.e, genericName, bestGuess, natParams, x);
                    // Now invoke f.
                    FValue theArray = f.apply(Collections.<FValue>emptyList(), x, evaluator.e);
                    // Do the copy.
                    iuo_tuple.copyTo(new IndexedArrayWrapper(theArray, x));
                    evaluator.e.putValue(s, theArray);

                } else if (outerType == null) {
                    Indexed it = new Indexed(iuo_tuple, bestGuess);

                    iuo_tuple.copyTo(it);

                    evaluator.e.putValue(s, new FArray(it, evaluator.e, x));

                } else if (outerType instanceof FTypeArray) {
                    Indexed it = new Indexed(iuo_tuple, bestGuess);
                    iuo_tuple.copyTo(it);
                    evaluator.e.putValue(s, new FArray(it, (FTypeArray) outerType));

                } else if (outerType instanceof FTypeMatrix){
                    Indexed it = new Indexed(iuo_tuple, bestGuess);
                    iuo_tuple.copyTo(it);
                    evaluator.e.putValue(s, new FMatrix(it, (FTypeMatrix) outerType));

                } else if (outerType instanceof FTypeVector){
                    Indexed it = new Indexed(iuo_tuple, bestGuess);
                    iuo_tuple.copyTo(it);
                    evaluator.e.putValue(s, new FVector(it));
                }



            } catch (ProgramError ex) {
                throw ex.setWhere(x).setWithin(evaluator.e);
            }

        } else {
            // TODO need to check the type first.
            FType outerType = null;
            // Perhaps the LHS has a type?
            try {
                if (type.isPresent()) {
                    TypeRef t = type.getVal();
                    outerType = EvalType.getFType(t, evaluator.e);
                    if (value.type().subtypeOf(outerType))
                        evaluator.e.putVariable(s, value, outerType);
                    else {
                        throw new ProgramError(x, evaluator.e,
                                errorMsg("RHS expression type ", value.type(),
                                               " is not assignable to LHS type ", outerType));
                    }
                } else {
                    if (mutable)
                        putOrAssignVariable(x, s);
                    else
                        evaluator.e.putValue(s, value);
                }
            } catch (ProgramError ex) {
                throw ex.setWhere(x).setWithin(evaluator.e);
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
            throw new ProgramError(x, evaluator.e,
                                   errorMsg("RHS yields non-tuple ", value));
        }
        FTuple t = (FTuple)value;
        Iterator<FValue> rhsIterator = t.getVals().iterator();
        for (Expr lhs : x.getExprs()) {
            // TODO: arity matching and exotic tuple types.
            lhs.accept(new LHSEvaluator(evaluator, rhsIterator.next()));
        }

        return null;
    }
}
