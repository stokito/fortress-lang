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

package com.sun.fortress.interpreter.glue;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.ObjectExpr;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.useful.HasAt;

public class WellKnownNames {
    public static final String anyTypeName = "Any";
    public static final String objectTypeName = "Object";
    public static final String defaultSelfName = "self";
    public static final String secretSelfName = "$self"; // The true name of the object
    public static final String secretParentName = "$parent"; // The true name of the object
    public static String arrayTrait(int rank) {
        return "Array"+rank;
    }
    public static String arrayMaker(int rank) {
        return "__builtinFactory"+rank;
    }

    public final static String fortressLibrary = "FortressLibrary";
    public final static String fortressBuiltin = "FortressBuiltin";
    public final static String anyTypeLibrary = "AnyType";
    public final static String executableApi = "Executable";
    
    public static final String[] defaultLibrary =
        { anyTypeLibrary,
          fortressLibrary,
          fortressBuiltin,
          //executableApi
        //  "NatReflect",
        //  "NativeArray"
        };

    public final static String varargsFactoryName = "__immutableFactory1";
    public final static String arrayElementTypeName = "T";
    public final static String arrayGetter = "get";
    public final static String arrayPutter = "init";
    
    public final static String matrix = "Matrix";
    
    public static final String thread = "Thread";

    public final static String loop = "loop";
    public final static String generate = "__generate";
    public final static String nest = "__nest";
    public final static String map = "__map";
    public final static String singleton = "__singleton";
    public final static String cond = "__cond";
    public final static String whileCond = "__whileCond";
    public final static String bigOperator = "__bigOperator";
    public final static String bigOperator2 = "__bigOperator2";
    public final static String filter = "__filter";

    public final static String generatorTypeName = "Generator";
    public final static String generatorMatchName = "MATCH";

    public final static String forbiddenException = "ForbiddenException";
    public final static String tryatomicFailureException = "TryAtomicFailure";
    public final static String matchFailureException = "MatchFailure";
    public final static String callerViolationException = "CallerViolation";
    public final static String calleeViolationException = "CalleeViolation";
    
    public final static String obfuscatedSingletonConstructorName(String fname, HasAt x) {
        // TODO Auto-generated method stub
        return "*1_" + fname;
    }
    public static String objectExprName(ObjectExpr expr) {
        return "*objectexpr_" + expr.toString();
    }
    public static String tempTupleName(VarDecl vd) {
        // TODO Auto-generated method stub
        return "*tuple_"+vd.at();
    }
    public static String tempForUnderscore(Id id) {
        // TODO Auto-generated method stub
        return "*underscore_"+id.at();
    }
 
}
