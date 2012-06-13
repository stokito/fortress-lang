/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler;

import java.util.Arrays;
import java.util.LinkedList;

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

    private static String _Infinity = "Infinity";
    private static String _NegativeInfinity = "NegativeInfinity";
    private static String _UndefinedNumber = "UndefinedNumber";
    
    private static String _compilerLibrary = "CompilerLibrary";
    private static String _compilerAlgebra = "CompilerAlgebra";
    private static String _compilerBuiltin = com.sun.fortress.runtimeSystem.Naming.COMPILER_BUILTIN;
    private static String _fortressLibrary = "FortressLibrary";
    private static String _fortressBuiltin = "FortressBuiltin";
    private static String _anyTypeLibrary = "AnyType";
    private static final String _executableApi = "Executable";
    private static final String _simpleExecutableApi = "SimpleExecutable";

    private static String[] _defaultLibrary =
        { anyTypeLibrary(), fortressLibrary(), fortressBuiltin() };

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
    public final static String outcome = "outcome";

    public final static String containsTypeName = "Contains";
    public final static String containsMatchName = "MATCH";

    public final static String operatorNameJuxtaposition = "juxtaposition";
    public final static String operatorNameEQUALS = "=";
    public final static String operatorNameIN = "IN";
    public final static String operatorNameAND = "AND";

    public final static String forbiddenException = "ForbiddenException";
    public final static String tryatomicFailureException = "TryAtomicFailure";
    public final static String matchFailureException = "MatchFailure";
    public final static String callerViolationException = "CallerViolation";
    public final static String calleeViolationException = "CalleeViolation";
    public final static String labelException = "LabelException";

    public static String Infinity() { return _Infinity; }	
    public static String NegativeInfinity() { return _NegativeInfinity; }	
    public static String UndefinedNumber() { return _UndefinedNumber; }	
    
    public static String compilerAlgebra() { return _compilerAlgebra; }
    public static String fortressLibrary() { return _fortressLibrary; }
    public static String fortressBuiltin() { return _fortressBuiltin; }
    public static String anyTypeLibrary() { return _anyTypeLibrary; }
    public static String executableApi() { return _executableApi; }
    public static String simpleExecutableApi() { return _simpleExecutableApi; }
    public static String[] defaultLibrary() { return _defaultLibrary; }
    public static String compilerBuiltin() { return _compilerBuiltin; }
    
    public static boolean exportsMain(String apiName) {
        return ( apiName.equals(_executableApi) ||
                 apiName.equals(_simpleExecutableApi) );
    }

    public static boolean exportsDefaultLibrary(String apiName) {
        return new LinkedList<String>(Arrays.asList(defaultLibrary())).contains( apiName );
    }

    /**
     * Re-points fortressLibrary etc. to special compiler (rather than interpreter) versions
     * of the code.  Hopefully temporary hack as we work on importing java objects cleanly.
     */
    public static void useCompilerLibraries() {
        _fortressLibrary = "CompilerLibrary";
        _fortressBuiltin = "CompilerBuiltin";

        // Note that fortressLibrary and fortressBuiltin depend on anyTypeLibrary,
        // and fortressLibrary depends on fortressBuiltin.
        // These libraries are processed in order by the GraphRepository, so keep
        // them listed in the order they are.
        _defaultLibrary =
            new String[] { fortressLibrary(),
			   fortressBuiltin(),
			   //			   compilerAlgebra(),
			   anyTypeLibrary() };
    }

    public static void useFortressLibraries() {
        _fortressLibrary = "FortressLibrary";
        _fortressBuiltin = "FortressBuiltin";

        // Note that fortressLibrary and fortressBuiltin depend on anyTypeLibrary,
        // and fortressLibrary depends on fortressBuiltin.
        // These libraries are processed in order by the GraphRepository, so keep
        // them listed in the order they are.
        _defaultLibrary =
            new String[] { fortressLibrary(), fortressBuiltin(), anyTypeLibrary() };
    }

    public static boolean areCompilerLibraries() {
        return _fortressLibrary.equals("CompilerLibrary");
    }

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
