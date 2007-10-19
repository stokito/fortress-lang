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
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.InstantiationLock;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.nodes.GenericWithParams;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Factory1P;
import com.sun.fortress.useful.Factory2P;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Memo1P;
import com.sun.fortress.useful.Memo1PCL;
import com.sun.fortress.useful.Memo2PCL;
import com.sun.fortress.useful.Useful;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

public class GenericConstructor extends FConstructedValue implements Factory2P<List<FType>, List<StaticArg>, Simple_fcn, HasAt> {
    private class Factory implements Factory2P<List<FType>, List<StaticArg>, Constructor, HasAt> {

        public Constructor make(List<FType> args, List<StaticArg>statics, HasAt within) {
            // Use the generic type to make the specific type
            String name = odefOrDecl.stringName();
            FTypeGeneric gt = (FTypeGeneric) env.getType(name);
            
            /*
             * Necessary to fake an instantiation expression.
             */
            QualifiedIdName qin = NodeFactory.makeQualifiedIdName(odefOrDecl.getSpan(), name);
            InstantiatedType inst_type = new InstantiatedType(qin, statics);
            FTypeObject ft = (FTypeObject) gt.make(args, inst_type);

            // Use the augmented environment from the specific type.
            BetterEnv clenv = ft.getEnv();

            // Build the constructor
            Option<List<Param>> params = odefOrDecl.getParams();
            List<Parameter> fparams =
                EvalType.paramsToParameters(clenv, Option.unwrap(params));

            Constructor cl = makeAConstructor(clenv, ft, fparams);
            return cl;
        }


    }

     Memo2PCL<List<FType>, List<StaticArg>, Constructor, HasAt> memo =
         new Memo2PCL<List<FType>, List<StaticArg>, Constructor, HasAt>(new Factory(), FType.listComparer, InstantiationLock.L);

     public Constructor make(List<FType> l, List<StaticArg>statics, HasAt within) {
        return memo.make(l, statics, within);
    }

    public GenericConstructor(Environment env, GenericWithParams odefOrDecl) {
        this.env = env;
        this.odefOrDecl = odefOrDecl;

    }

  Environment env;
  GenericWithParams odefOrDecl;

  public GenericWithParams getDefOrDecl() {
      return odefOrDecl;
  }

  public String getString() {
      return s(odefOrDecl);
  }

  protected Constructor constructAConstructor(BetterEnv clenv,
                                              FTypeObject objectType) {
    return new Constructor(clenv, objectType, odefOrDecl);
  }
  
  private Constructor makeAConstructor(BetterEnv clenv, FTypeObject objectType, List<Parameter> objectParams) {
      Constructor cl = constructAConstructor(clenv, objectType);
      cl.setParams(objectParams);
      cl.finishInitializing();
      FTypeGeneric.flushPendingTraitFMs();
      return cl;
  }

  public FValue typeApply(List<StaticArg> args, BetterEnv e, HasAt x) {
    List<StaticParam> params = odefOrDecl.getStaticParams();

    // Evaluate each of the args in e, inject into clenv.
    if (args.size() != params.size() ) {
        error(x, e,
              errorMsg("Generic instantiation (size) mismatch, expected ",
                       Useful.listInParens(params), " got ",
                       Useful.listInParens(args)));
    }
    EvalType et = new EvalType(e);
    ArrayList<FType> argValues = et.forStaticArgList(args);
    return make(argValues, args, x);
}

}
