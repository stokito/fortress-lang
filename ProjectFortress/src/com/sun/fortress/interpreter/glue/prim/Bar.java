/*
 * Created on Aug 22, 2007
 *
 */
package com.sun.fortress.interpreter.glue.prim;

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Method;
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.useful.HasAt;

public class Bar extends NativeApp implements Method {

    @Override
    public FValue applyToArgs(List<FValue> args) {
        // TODO Auto-generated method stub
        throw new Error();
    }

    @Override
    public int getArity() {
        // TODO Auto-generated method stub
        return 2;
    }

    public FValue applyMethod(List<FValue> args, FObject selfValue, HasAt loc,
            BetterEnv envForInference) {
        FValue x = selfValue.getSelfEnv().getValue("x");
        FValue s = args.get(0);
        return FString.make(s.getString() + x.getString());
    }

}
