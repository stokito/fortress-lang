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

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;

import java.util.List;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.FortressException;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeTrait;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.useful.HasAt;

/**
 * A DottedMethodApplication is produced when a FieldRef occurs in
 * a loosely juxtaposed context.  It simply packages up self along with
 * with the method closure, permitting them to be applied to arguments
 * as if they were an ordinary function closure.
 */
public final class DottedMethodApplication extends Fcn {

    private final Method cl;
    private final FObject self;

    private DottedMethodApplication(FObject self, Method cl, Environment selfEnv) {
        super(selfEnv);
        this.cl = cl;
        this.self = self;
    }

    /** This factory does the work necessary to deal with AsIf
     *  receivers, and the DottedMethodApplication the results
     *  packages up the required information that results (most
     *  importantly the receiver, self, and the method closure cl).
     *  It has been written in a fairly general way in order to permit
     *  it to represent arbitrary method applications encountered
     *  during Evaluation.
     */
    public static DottedMethodApplication make(FValue receiver,
                                               String prettyName, String mname,
                                               HasAt x) {
        // Step 1: determine self (modulo AsIf), make sure it's an object,
        //    and simultaneously determine the selfEnv for errors.
        // TODO Need to distinguish between public/private
        // methods/fields
        //  (actually this is properly the duty of static analysis).
        Environment selfEnv;
        FObject self;
        FValue cl;
        if (receiver instanceof FObject) {
            self = (FObject) receiver;
            selfEnv = self.getSelfEnv();
            cl = selfEnv.getValueNull(mname);
        } else {
            // fobj instanceof FAsIf, nontrivial type().  Since
            // getMembers() on traits only returns the immediately
            // defined methods and fields, we need to walk the
            // transitive extends hierarchy in order to find the
            // method we're looking for.  Open question: Is this right
            // or sufficient?  What happens if multiple overloadings
            // of given method are obtained from different
            // supertraits---will we actually get an overloaded method
            // closure, or will the world simply break?

            FValue selfVal = receiver.getValue();
            if (!(selfVal instanceof FObject)) {
                return error(x, errorMsg("Non-object receiver ",receiver,
                                         " trying to invoke method ",prettyName));
            }
            self = (FObject) selfVal;
            FType rtype = receiver.type();
            if (rtype instanceof FTypeTrait) {
                cl = null;
                selfEnv = null;
                FTypeTrait tr = (FTypeTrait) rtype;
                for (FType t : tr.getTransitiveExtends()) {
                    if (!(t instanceof FTypeTrait)) continue;
                    selfEnv = ((FTypeTrait)t).getMembers();
                    cl = selfEnv.getValueNull(mname);
                    if (cl != null) break;
                }
                if (cl == null) {
                    // We're going to fail; set selfEnv to root of search.
                    selfEnv = tr.getMembers();
                }
            } else {
                selfEnv = self.getSelfEnv();
                cl = selfEnv.getValueNull(mname);
            }
        }
        // Step 2: validate the closure we obtained.
        if (cl==null) {
            return error(x, selfEnv,
                         errorMsg("Cannot find definition for method ",prettyName,
                                  " given receiver ",receiver));
        } else if (!(cl instanceof Method && cl instanceof Fcn)) {
            return error(x, selfEnv,
                         errorMsg("Unexpected method value ",cl.toString(),
                                  " when invoking method ",prettyName,
                                  " given receiver ",receiver));
        }
        // Step 4: package it all up into a DottedMethodApplication.
        return new DottedMethodApplication(self,((Method)cl),selfEnv);
    }

    /** Perform a full method invocation. */
    public static FValue invokeMethod(FValue receiver, String prettyName,
                                      String mname, List<FValue> args,
                                      HasAt x, Environment envForInference) {
        DottedMethodApplication app =
            DottedMethodApplication.make(receiver,prettyName,mname,x);
        return app.apply(args,x,envForInference);
    }

    public DottedMethodApplication applyToStaticArgs(List<StaticArg> sargs, HasAt x,
            Environment envForInference) {
        Method cl = getMethod();
        FObject self = getSelf();
        Environment selfEnv = getWithin();

        if (cl instanceof OverloadedMethod) {
            return bug(x, selfEnv,
                       "Don't actually resolve overloading of generic methods yet.");
        } else if (cl instanceof MethodInstance) {
            // What gets retrieved is the symbolic instantiation of
            // the generic method.
            // This is ever-so-slightly wrong -- we need to not
            // create an "instance"
            // if the parameters are non-symbolic.
            GenericMethod gm = ((MethodInstance) cl).getGenerator();
            MethodClosure actual = gm.typeApply(sargs,envForInference,x);
            return new DottedMethodApplication(self,actual,selfEnv);
        } else {
            return error(x, selfEnv,
                         errorMsg("Unexpected Selection result in Juxt of FnRef of Selection, ",
                                  cl));
        }
    }

    public Method getMethod() {
        return cl;
    }

    public FObject getSelf() {
        return self;
    }

    public FType type() {
        return ((Fcn)cl).type();
    }

    public void setFtype(FType type) {
        error(errorMsg("Cannot set type of ",this,";\n  tried to set to ",type));
    }

    public void setFtypeUnconditionally(FType ftype) {
        // Do nothing.
    }

    public FValue applyInner(List<FValue> args, HasAt loc, Environment envForInference) {
        try {
            return cl.applyMethod(args, self, loc, envForInference);
        } catch (FortressException ex) {
            throw ex.setContext(loc, getWithin());
        } catch (StackOverflowError soe) {
            return error(loc, getWithin(), errorMsg("Stack overflow on ",loc));
        }
    }

    public IdOrOpOrAnonymousName getFnName() {
        return ((Fcn)cl).getFnName();
    }

    /**
     * Returns the name if this "function" is regarded as a method.
     * For functions, just returns the name.
     */
    public String asMethodName() {
        return ((Fcn)cl).asMethodName();
    }

    public String toString() {
        return ("partial method application "+self+"."+((Fcn)cl));
    }

    public boolean seqv(FValue other) {
        return false;
    }
}
