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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.EvaluatorBase;
import com.sun.fortress.interpreter.evaluator.InstantiationLock;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.FGenericFunction.GenericComparer;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.GenericWithParams;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes_util.NodeComparator;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.useful.Factory1P;
import com.sun.fortress.useful.Factory2P;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Memo1P;
import com.sun.fortress.useful.Memo1PCL;
import com.sun.fortress.useful.Memo2PCL;
import com.sun.fortress.useful.Useful;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

public class GenericConstructor
extends SingleFcn
implements Factory1P<List<FType>, Simple_fcn, HasAt>, GenericFunctionOrMethod {
    private class Factory implements Factory1P<List<FType>,  Constructor, HasAt> {

        public Constructor make(List<FType> args, HasAt within) {
            // Use the generic type to make the specific type
            String name = odefOrDecl.stringName();
            FTypeGeneric gt = (FTypeGeneric) env.getType(name);

            /*
             * Necessary to fake an instantiation expression.
             */
            //QualifiedIdName qin = NodeFactory.makeQualifiedIdName(odefOrDecl.getSpan(), name);
            //InstantiatedType inst_type = new InstantiatedType(qin, statics);
            FTypeObject ft = (FTypeObject) gt.make(args, odefOrDecl);

            // Use the augmented environment from the specific type.
            BetterEnv clenv = ft.getEnv();

            // Build the constructor
//            Option<List<Param>> params = odefOrDecl.getParams();
//            List<Parameter> fparams =
//                EvalType.paramsToParameters(clenv, Option.unwrap(params));

            Constructor cl = makeAConstructor(clenv, ft,  odefOrDecl.getParams());
            return cl;
        }


    }

     Memo1PCL<List<FType>,  Constructor, HasAt> memo =
         new Memo1PCL<List<FType>,  Constructor, HasAt>(new Factory(), FType.listComparer, InstantiationLock.L);

     public Constructor make(List<FType> l,  HasAt within) {
        return memo.make(l,  within);
    }

    public GenericConstructor(BetterEnv env, GenericWithParams odefOrDecl, SimpleName cfn) {
        super(env);
        this.env = env;
        this.odefOrDecl = odefOrDecl;
        this.cfn = cfn;
    }

  Environment env;
  GenericWithParams odefOrDecl;
  SimpleName cfn;
  volatile Simple_fcn symbolicInstantiation;


  public GenericWithParams getDefOrDecl() {
      return odefOrDecl;
  }

  public String getString() {
      return s(odefOrDecl);
  }

  protected Constructor constructAConstructor(BetterEnv clenv,
                                              FTypeObject objectType,
                                              Option<List<Param>> objectParams) {
    return new Constructor(clenv, objectType, odefOrDecl, objectParams);
  }

  private Constructor makeAConstructor(BetterEnv clenv, FTypeObject objectType, Option<List<Param>> objectParams) {
      Constructor cl = constructAConstructor(clenv, objectType, objectParams);
      cl.finishInitializing();
      FTypeGeneric.flushPendingTraitFMs();
      return cl;
  }

  public Simple_fcn typeApply(HasAt location, List<FType> argValues) throws ProgramError {
      return make(argValues, location);
  }

  public Simple_fcn typeApply(List<StaticArg> args, BetterEnv e, HasAt x) {
    List<StaticParam> params = odefOrDecl.getStaticParams();

    ArrayList<FType> argValues = argsToTypes(args, e, x, params);
    return make(argValues, x);
}

public static ArrayList<FType> argsToTypes(List<StaticArg> args, BetterEnv e, HasAt x,
        List<StaticParam> params) {
    // Evaluate each of the args in e, inject into clenv.
    if (args.size() != params.size() ) {
        error(x, e,
              errorMsg("Generic instantiation (size) mismatch, expected ",
                       Useful.listInParens(params), " got ",
                       Useful.listInParens(args)));
    }
    EvalType et = new EvalType(e);
    ArrayList<FType> argValues = et.forStaticArgList(args);
    return argValues;
}

@Override
public String at() {
    // TODO Auto-generated method stub
    return odefOrDecl.at();
}

@Override
public FValue applyInner(List<FValue> args, HasAt loc, BetterEnv envForInference) {
   // TODO Auto-generated method stub
    Simple_fcn foo = EvaluatorBase.inferAndInstantiateGenericFunction(args, this, loc, envForInference);
    return foo.apply(args, loc, envForInference);
}

@Override
public SimpleName getFnName() {
    // TODO Auto-generated method stub
    return cfn;
}

public String stringName() {
    // TODO Auto-generated method stub
    return cfn.stringName();
}

/* (non-Javadoc)
 * @see com.sun.fortress.interpreter.evaluator.values.SingleFcn#getDomain()
 *
 * Cut and paste from FGenericFunction
 */
@Override
public List<FType> getDomain() {
    if (symbolicInstantiation == null) {
        synchronized (this) {
            if (symbolicInstantiation == null) {
                List<FType> symbolic_static_args = FGenericFunction.symbolicStaticsByPartition.get(this);
                if (symbolic_static_args == null) {
                    /* TODO This is not quite right, because we risk
                     * identifying two functions whose where clauses are
                     * interpreted differently in two different environments.
                     */
                    symbolic_static_args =
                        FGenericFunction.symbolicStaticsByPartition.syncPutIfMissing(this,
                                createSymbolicInstantiation(getWithin(),
                                        odefOrDecl.getStaticParams(),
                                        FortressUtil.emptyWhereClause(),
                                        odefOrDecl));
                }
                symbolicInstantiation = make(symbolic_static_args, odefOrDecl);
            }
        }
    }
    return symbolicInstantiation.getDomain();

}

public SimpleName getName() {
    // TODO Auto-generated method stub
    return cfn;
}

public List<StaticParam> getStaticParams() {
    // TODO Auto-generated method stub
    return odefOrDecl.getStaticParams();
}

public List<Param> getParams() {
    // TODO Auto-generated method stub
    return Option.unwrap(odefOrDecl.getParams());
}

public Option<Type> getReturnType() {
    // TODO Auto-generated method stub
    // TODO this will probably not be good enough.
    return Option.<Type>none();
}

}
