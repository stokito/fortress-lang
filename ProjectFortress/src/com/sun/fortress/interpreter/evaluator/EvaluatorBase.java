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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.BottomType;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.TypeLatticeOps;
import com.sun.fortress.interpreter.evaluator.values.FGenericFunction;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;

import com.sun.fortress.nodes.FnDefOrDecl;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.NodeVisitor;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.SimpleTypeParam;
import com.sun.fortress.useful.Option;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TypeRef;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.ABoundingMap;
import com.sun.fortress.useful.BoundingMap;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.LatticeIntervalMap;
import com.sun.fortress.useful.StringComparer;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;

public class EvaluatorBase<T> extends NodeAbstractVisitor<T>  {

    protected static final boolean DUMP_INFERENCE = false;
    
    final public BetterEnv e;

    protected EvaluatorBase(BetterEnv e) {
        this.e = e;
    }

    protected FValue functionInvocation(List<FValue> args, FValue foo,
                                        AbstractNode loc) {
        if (foo instanceof Fcn) {
            return functionInvocation(args, (Fcn) foo, loc);
        } else {
            throw new ProgramError(loc, errorMsg("Not a Fcn: ", foo));
        }
    }

    protected FValue functionInvocation(List<FValue> args, Fcn foo, AbstractNode loc) {
        if (foo instanceof FGenericFunction) {
            FGenericFunction gen = (FGenericFunction) foo;

// This caching did not seem to help performance.
//
//            Simple_fcn sfcn = gen.cache.get(args);
//
//            if (sfcn == null) {
//                sfcn = inferAndInstantiateGenericFunction(args, gen, loc);
//                gen.cache.syncPut(args, sfcn);
//            }
//
//            foo = sfcn;
            try {
                foo = inferAndInstantiateGenericFunction(args, gen, loc, e);
            } catch (ProgramError ex) {
                throw ex;
            }
            // System.out.println("Generic invoke "+foo+"\n  On arguments "+args);
        }
        return foo.apply(args, loc, e);
    }

    /**
     * Given args, infers the appropriate instantiation of a generic function.
     * @throws ProgramError
     */
    public  static Simple_fcn inferAndInstantiateGenericFunction(List<FValue> args,
            FGenericFunction appliedThing, HasAt loc, BetterEnv e) throws ProgramError {
        
        if (DUMP_INFERENCE)
            System.err.println("IAIGF " + appliedThing + " with " + args);
        
        FGenericFunction bar = (FGenericFunction) appliedThing;
        FnDefOrDecl fndod =  bar.getFnDefOrDecl();
        Option<List<StaticParam>> otparams = fndod.getStaticParams();
        List<StaticParam> tparams = otparams.getVal();
        List<Param> params = bar.getFnDefOrDecl().getParams();
        EvalType et = new EvalType(e);
        // The types of the actual parameters ought to unify with the
        // types of the formal parameters.
        // TODO WE MUST MOVE TO THE FULL LATTICE INTERVAL MAP
        BoundingMap<String, FType, TypeLatticeOps> abm = new
          //ABoundingMap
          LatticeIntervalMap
          <String, FType, TypeLatticeOps>(TypeLatticeOps.V, StringComparer.V);
        Iterator<Param> pit = params.iterator();
        Param p = null;
        Set<StaticParam> tp_set = new HashSet<StaticParam>(tparams);
        for (StaticParam sp : tparams) {
            if (sp instanceof SimpleTypeParam) {
                SimpleTypeParam stp = (SimpleTypeParam) sp;
                Option<List<TypeRef>> ec = stp.getExtendsClause();
                if (ec.isPresent()) {
                    String stp_name = stp.getId().getName();
                    for (TypeRef tr : ec.getVal()) {
                        // Preinstall bounds in the boundingmap
                        abm.meetPut(stp_name, et.evalType(tr));
                    }
                }
            }
        }
        for (FValue a : args) {
            p = pit.hasNext() ? pit.next() : p;
            Option<TypeRef> t = p.getType();
            // why can't we just skip if missing?
            if (!t.isPresent())
                throw new ProgramError(loc,
                        errorMsg("Parameter needs type for generic resolution"));
            FType at = a.type();
            try {
                at.unify(e, tp_set, abm, t.getVal());
            } catch (ProgramError ex) {
                /* Give decent feedback when unification fails. */
                ex.setWithin(e);
                ex.setWhere(loc);
                throw ex;
            }
        }

        if (DUMP_INFERENCE)
            System.err.println("ABM 0={" + abm + "}");
        
       /*
        * Filter the inference through the result type, making it more
        * specific (which is less-specific, for arrow domain types).
        */
        
        MakeInferenceSpecific mis = new MakeInferenceSpecific(abm);
        
        // TODO: There is still a lurking error in inference, probably in arrow types.
        
//        for (Param param : params) {
//            Option<TypeRef> t = param.getType();
//            t.getVal().accept(mis);
//        }
//        if (DUMP_INFERENCE) 
//            System.err.println("ABM 1={" + abm + "}");
           
        Option<TypeRef> opt_rt = fndod.getReturnType();
       
        if (opt_rt.isPresent())
           opt_rt.getVal().accept(mis);

        if (DUMP_INFERENCE)
            System.err.println("ABM 2={" + abm + "}");
        
        /*
         * Iterate over static parameters, choosing least-general binding
         * for each one.
         */
        ArrayList<FType> tl = new ArrayList<FType>(tparams.size());
        for (StaticParam tp : tparams) {
            FType t = abm.get(NodeUtil.getName(tp));
            if (t == null)
                t = BottomType.ONLY;
            tl.add(t);
        }
        Simple_fcn sfcn = bar.make(tl, loc);
        if (DUMP_INFERENCE)
            System.err.println("Result " + sfcn);
        return sfcn;
    }


}
