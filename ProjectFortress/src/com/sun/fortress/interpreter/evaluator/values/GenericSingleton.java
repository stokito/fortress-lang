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
import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.ObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.Factory1P;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Memo1P;

import edu.rice.cs.plt.tuple.Option;

public class GenericSingleton extends FValue implements Factory1P<List<FType>, FObject, HasAt> {

    ObjectAbsDeclOrDecl odecl;
    FType t;
    GenericConstructor genericConstructor;
    
    public GenericSingleton(ObjectAbsDeclOrDecl odecl, FType t, GenericConstructor gc) {
        super();
        this.odecl = odecl;
        this.t = t;
        this.genericConstructor = gc;
    }

    
    private class Factory implements Factory1P<List<FType>, FObject, HasAt> {

        public FObject make(List<FType> args, HasAt location) {
            return (FObject)
            genericConstructor.typeApply(location, args).
            apply(Collections.<FValue>emptyList(),
                    location,
                    genericConstructor.getWithin());
            
        }


    }

     Memo1P<List<FType>, FObject, HasAt> memo = new Memo1P<List<FType>, FObject, HasAt>(new Factory());
    
    public FObject make(List<FType> l, HasAt location) {
        return memo.make(l, location);
    }

    @Override
    public FType type() {
         return t;
    }

    public SimpleName getName() {
        // TODO Auto-generated method stub
        return odecl.getName();
    }
    
    public String getString() {
        return s(odecl);
    }

    public List<StaticParam> getStaticParams() {
        return odecl.getStaticParams();
    }

    public FObject typeApply(HasAt location, List<FType> argValues) {
        return make(argValues, location);
    }
    
    public FObject typeApply(List<StaticArg> args, BetterEnv e, HasAt x) {
        List<StaticParam> params = odecl.getStaticParams();
        ArrayList<FType> argValues = GenericConstructor.argsToTypes(args, e, x, params);
        return make(argValues, x);
    }

}
