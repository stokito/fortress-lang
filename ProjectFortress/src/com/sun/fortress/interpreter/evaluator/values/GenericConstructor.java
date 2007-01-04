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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.nodes.GenericDefWithParams;
import com.sun.fortress.interpreter.nodes.Option;
import com.sun.fortress.interpreter.nodes.Param;
import com.sun.fortress.interpreter.nodes.StaticArg;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.useful.Factory1P;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.Memo1P;


public class GenericConstructor extends FValue implements Factory1P<List<FType>, Simple_fcn, HasAt> {
    private class Factory implements Factory1P<List<FType>, Constructor, HasAt> {

        public Constructor make(List<FType> args, HasAt within) {
            // Use the generic type to make the specific type
            FTypeGeneric gt = (FTypeGeneric) env.getType(odef.stringName());
            FTypeObject ft = (FTypeObject) gt.make(args, within);

            // Use the augmented environment from the specific type.
            BetterEnv clenv = ft.getEnv();

            // Build the constructor
            Option<List<Param>> params = odef.getParams();
            List<Parameter> fparams =
                EvalType.paramsToParameters(clenv, params.getVal());

            Constructor cl = makeAConstructor(clenv, ft, fparams);
            return cl;
        }


    }

     Memo1P<List<FType>, Constructor, HasAt> memo =
         new Memo1P<List<FType>, Constructor, HasAt>(new Factory());

     public Constructor make(List<FType> l, HasAt within) {
        return memo.make(l, within);
    }

    public GenericConstructor(Environment env, GenericDefWithParams odef) {
        this.env = env;
        this.odef = odef;

    }

  Environment env;
  GenericDefWithParams odef;

  public GenericDefWithParams getDef() {
      return odef;
  }

  public String getString() {
      return odef.toString();
  }

  /**
   * @param clenv
   * @param objectType
   * @param objectParams
   * @return
   */
  protected Constructor makeAConstructor(BetterEnv clenv, FTypeObject objectType, List<Parameter> objectParams) {
      Constructor cl = new Constructor(clenv, objectType, odef);
      cl.setParams(objectParams);
      cl.finishInitializing();
      return cl;
  }

  public FValue typeApply(List<StaticArg> args, BetterEnv e, HasAt x) {
    List<StaticParam> params = odef.getStaticParams().getVal();

    // Evaluate each of the args in e, inject into clenv.
    if (args.size() != params.size() ) {
        throw new ProgramError(x, e,
                "Generic instantiation (size) mismatch, expected " + params + " got " + args);
    }
    EvalType et = new EvalType(e);
    ArrayList<FType> argValues = et.forStaticArgList(args);
    return make(argValues, x);
}

}
