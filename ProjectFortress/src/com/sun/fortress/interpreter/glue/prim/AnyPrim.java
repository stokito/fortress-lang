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

package com.sun.fortress.interpreter.glue.prim;

import java.math.BigInteger;
import java.util.List;
import java.util.Iterator;

import com.sun.fortress.interpreter.glue.NativeFn2;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.evaluator.types.*;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

/**
 * Functions on Any type.
 */
public class AnyPrim {

public static boolean sequiv(FValue x, FValue y) {
    if (x==y) return true;
    return x.seqv(y);
    // if (x instanceof FBuiltinValue) {
    //     // Special treatment of builtin numbers.
    //     if (!(y instanceof FBuiltinValue)) return false;
    //     if (x instanceof HasIntValue) {
    //         if (y instanceof FFloatLiteral || y instanceof FFloat) {
    //             return x.getFloat()==y.getFloat();
    //         }
    //         if (x instanceof FIntLiteral) {
    //             return (y instanceof FIntLiteral) &&
    //                 ((FIntLiteral)x).getLit().equals(((FIntLiteral)y).getLit());
    //         }
    //         if (y instanceof FIntLiteral) return false;
    //         return (y instanceof HasIntValue) && x.getLong()==y.getLong();
    //     }
    //     if (x instanceof FFloatLiteral || x instanceof FFloat) {
    //         if (y instanceof FFloatLiteral || y instanceof FFloat ||
    //             y instanceof HasIntValue) {
    //             return x.getFloat() == y.getFloat();
    //         }
    //         return false;
    //     }
    //     if (x instanceof FBool) {
    //         return (y instanceof FBool) &&
    //             ((FBool)x).getBool()==((FBool)y).getBool();
    //     }
    //     if (x instanceof FString) {
    //         return (y instanceof FString) && x.getString().equals(y.getString());
    //     }
    //     if (x instanceof FChar) {
    //         return (y instanceof FChar) && x.getChar()==y.getChar();
    //     }
    // }
    // if (x.getClass() != y.getClass()) return false;
    // // **** At this point x and y have the same class. ****
    // if (x instanceof FTuple) {
    //     if (x instanceof FVoid) return true;
    //     List<FValue> xt = ((FTuple)x).getVals();
    //     List<FValue> yt = ((FTuple)y).getVals();
    //     if (xt.size()!=yt.size()) return false;
    //     Iterator<FValue> xi = xt.iterator();
    //     Iterator<FValue> yi = yt.iterator();
    //     while (xi.hasNext()) {
    //         if (!sequiv(xi.next(), yi.next())) return false;
    //     }
    //     return true;
    // }
    // FType ty = x.type();
    // if (ty != y.type()) return false;
    // if (x instanceof FObject) {
    //     if (!(((FTypeObject)ty).isValueType())) return false;
    //     return x.sequiv(y);
    // }
    // if (x instanceof Fcn) {
    //     // TODO: Check closures if closed-over stuff is the same.
    //     return false;
    // }
    // bug(errorMsg("SEQUIV: unhandled type (should not happen)",x));
    // return false;
}

public static final class SEquiv extends NativeFn2 {
    protected FValue act(FValue x, FValue y) {
        return FBool.make(AnyPrim.sequiv(x,y));
    }
}

}
