/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.iter.IterUtil;

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
import com.sun.fortress.nodes.AbstractFieldRef;
import com.sun.fortress.nodes.FieldRefForSure;
import com.sun.fortress.nodes.Name;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.ExtentRange;
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.SubscriptExpr;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.ArgExpr;
import com.sun.fortress.nodes.TupleExpr;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.Unpasting;
import com.sun.fortress.nodes.UnpastingBind;
import com.sun.fortress.nodes.UnpastingSplit;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes._RewriteFieldRef;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Voidoid;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeUtil;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class LHSEvaluator extends NodeAbstractVisitor<Voidoid>  {
    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forSubscriptExpr(com.sun.fortress.interpreter.nodes.SubscriptExpr)
     */
    @Override
    public Voidoid forSubscriptExpr(SubscriptExpr x) {
        Expr obj = x.getObj();
        List<Expr> subs = x.getSubs();
        Option<Enclosing> op = x.getOp();
        // Should evaluate obj.[](subs, value)
        FObject array = (FObject) obj.accept(evaluator);
        String opString;
        if (op.isSome()) {
            opString = NodeUtil.nameString(Option.unwrap(op));
        } else {
            opString = "[]=";
        }
        Method cl = (Method) array.getSelfEnv().getValue(opString);
        ArrayList<FValue> subscriptsAndValue = new ArrayList<FValue>(1+subs.size());
        subscriptsAndValue.add(value);
        evaluator.evalExprList(subs, subscriptsAndValue);
        // TODO verify that 'e' is proper inference environment
        cl.applyMethod(subscriptsAndValue, array, x, evaluator.e);
        return null;
    }

    public Voidoid forFieldRefCommon(AbstractFieldRef x, Name what) {
        Expr from = x.getObj();
        // TODO need to generalize to dotted names.
        FValue val = from.accept(evaluator);
        if (val instanceof FObject) {
            FObject obj = (FObject) val;
            if (what instanceof Id) {
                obj.getSelfEnv().assignValue(x, ((Id)what).getText(), value);
            } else {
                bug("'what' not instanceof IDName, instead is " + what );
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

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor#for_RewriteFieldRef(com.sun.fortress.nodes._RewriteFieldRef)
     */
    @Override
    public Voidoid for_RewriteFieldRef(_RewriteFieldRef x) {
        return forFieldRefCommon(x, x.getField());
    }

    @Override
    public Voidoid forFieldRefForSure(FieldRefForSure x) {
        return forFieldRefCommon(x, x.getField());
    }



    LHSEvaluator(Evaluator evaluator, FValue value) {
        this.evaluator = evaluator; this.value = value;
    }
    Evaluator evaluator;
    FValue value;

    public Voidoid forVarRef(VarRef x) {
        Iterable<Id> names = NodeUtil.getIds(x.getVar());
        Environment e = evaluator.e;
        if (IterUtil.sizeOf(names) == 1) {
            String s = IterUtil.first(names).getText();
            FType ft = e.getVarTypeNull(s);
            if (ft != null) {
                // Check that variable can receive type
                if (!ft.typeMatch(value)) {
                    String m = errorMsg("Type mismatch assigning ", value, " (type ",
                                        value.type(), ") to ", s, " (type ", ft, ")");
                    return error(x, e, m);
                }
            }
            e.assignValue(x, s, value);
            return null;
        } else {
            return NI.na("wrong processing of QID var-ref");
            // return forFieldRef(ExprFactory.makeFieldRef(x));
        }
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forUnpastingBind(com.sun.fortress.interpreter.nodes.UnpastingBind)
     */
    @Override
    public Voidoid forUnpastingBind(UnpastingBind x) {
        Id name = x.getName();
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
        Id name = x.getName();
        Option<Type> type = x.getType();
        String s = NodeUtil.nameString(name);
        boolean mutable = x.isMutable();

        // Here we have an LHS context
        if (value instanceof IUOTuple) {
            IUOTuple iuo_tuple = (IUOTuple) value;
            FType bestGuess = FTypeDynamic.ONLY;
            FType outerType = null;
            // Perhaps the LHS has a type?
            if (type.isSome()) {
                Type t = Option.unwrap(type);
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

                outerType = error(x, evaluator.e,
                            "Can't infer element type for array construction");
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



            } catch (FortressError ex) {
                throw ex.setContext(x,evaluator.e);
            }

        } else {
            // TODO need to check the type first.
            FType outerType = null;
            // Perhaps the LHS has a type?
            try {
                if (type.isSome()) {
                    Type t = Option.unwrap(type);
                    outerType = EvalType.getFType(t, evaluator.e);
                    if (value.type().subtypeOf(outerType))
                        evaluator.e.putVariable(s, value, outerType);
                    else {
                        error(x, evaluator.e,
                         errorMsg("RHS expression type ", value.type(),
                                  " is not assignable to LHS type ", outerType));
                    }
                } else {
                    if (mutable)
                        putOrAssignVariable(x, s);
                    else
                        evaluator.e.putValue(s, value);
                }
            } catch (FortressError ex) {
                throw ex.setContext(x,evaluator.e);
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
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forArgExpr(com.sun.fortress.interpreter.nodes.ArgExpr)
     */
    @Override
    public Voidoid forArgExpr(ArgExpr x) {
        if (!(value instanceof FTuple)) {
            error(x, evaluator.e, errorMsg("RHS yields non-tuple ", value));
        }
        FTuple t = (FTuple)value;
        Iterator<FValue> rhsIterator = t.getVals().iterator();
        for (Expr lhs : x.getExprs()) {
            // TODO: arity matching and exotic tuple types.
            lhs.accept(new LHSEvaluator(evaluator, rhsIterator.next()));
        }

        return null;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forTupleExpr(com.sun.fortress.interpreter.nodes.TupleExpr)
     */
    @Override
    public Voidoid forTupleExpr(TupleExpr x) {
        if (!(value instanceof FTuple)) {
            error(x, evaluator.e, errorMsg("RHS yields non-tuple ", value));
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
