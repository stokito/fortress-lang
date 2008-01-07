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

package com.sun.fortress.compiler.typechecker;


import com.sun.fortress.compiler.*;
import com.sun.fortress.nodes.*;
import edu.rice.cs.plt.iter.IterUtil;
import java.util.*;

public class TypeChecker extends NodeDepthFirstVisitor<TypeCheckerResult> {
    private GlobalEnvironment globals;
    private StaticParamEnv staticParams;
    private TypeEnv params; 
    
    public TypeChecker(GlobalEnvironment _globals, 
                       TypeEnv _params,
                       StaticParamEnv _staticParams) 
    {
        globals = _globals;
        params = _params;
        staticParams = _staticParams;
    }
    
    /** Ignore unsupported nodes for now. */
    public TypeCheckerResult defaultCase(Node that) {
        return new TypeCheckerResult(that, IterUtil.<StaticError>empty());
    }
    
    public TypeCheckerResult forFnDecl(FnDecl that) {
        TypeEnv localEnv = params.extend(that.getParams());
        return null;
    }
}