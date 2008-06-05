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

package com.sun.fortress.compiler.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.fortress.compiler.typechecker.StaticTypeReplacer;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;

public class DeclaredMethod extends Method {

    private final FnAbsDeclOrDecl _ast;
    private final Id _declaringTrait;

    public DeclaredMethod(FnAbsDeclOrDecl ast, Id declaringTrait) {
        _ast = ast;
        _declaringTrait = declaringTrait;
    }

    public FnAbsDeclOrDecl ast() { return _ast; }

	@Override
	public Type instantiatedType(Type... staticArgs) {
		
		List<StaticParam> static_params = _ast.getStaticParams();
		
		Iterable<StaticArg> args =
			IterUtil.map(Arrays.asList(staticArgs), new Lambda<Type, StaticArg>() {
				public StaticArg value(Type arg0) {
					return new TypeArg(arg0);
				}});
		List<StaticArg> static_args = new ArrayList<StaticArg>();
		for( StaticArg arg : args ) {
			static_args.add(arg);
		}
		
		StaticTypeReplacer replacer = new StaticTypeReplacer(static_params, static_args);
		
		if( _ast.getReturnType().isSome() ) {
			Type ret_type = (Type)_ast.getReturnType().unwrap().accept(replacer);
			
			List<Param> new_params = replacer.recurOnListOfParam(_ast.getParams());
	//		new_params.get(0)
		}
		else {
			return NI.nyi();
		}
		
		
		return super.instantiatedType(staticArgs);
	}
	
}
