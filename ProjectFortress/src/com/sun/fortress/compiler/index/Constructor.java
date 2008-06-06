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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

public class Constructor extends Function {

    private final Id _declaringTrait;
    private final List<StaticParam> _staticParams;
    private final Option<List<Param>> _params;
    private final Option<List<BaseType>> _throwsClause;
    private final WhereClause _where;

    public Constructor(Id declaringTrait,
                       List<StaticParam> staticParams,
                       Option<List<Param>> params,
                       Option<List<BaseType>> throwsClause,
                       WhereClause where)
    {
        _declaringTrait = declaringTrait;
        _staticParams = staticParams;
        _params = params;
        _throwsClause = throwsClause;
        _where = where;
    }

    public Id declaringTrait() { return _declaringTrait; }
//    public List<StaticParam> staticParams() { return _staticParams; }
//    public Option<List<Param>> params() { return _params; }
//    public Option<List<BaseType>> throwsClause() { return _throwsClause; }
    public WhereClause where() { return _where; }

	@Override
	public Option<Expr> body() {
		return Option.none();
	}

	@Override
	public ArrowType instantiatedType(List<StaticArg> args) {
		return NI.nyi();
	}

	@Override
	public ArrowType asArrowType() {
		return NI.nyi();
	}
	
	@Override
	public List<Param> parameters() {
		if( _params.isNone() )
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(_params.unwrap());
	}

	@Override
	public List<StaticParam> staticParameters() {
		return Collections.unmodifiableList(_staticParams);
	}

	@Override
	public Iterable<BaseType> thrownTypes() {
		if( _throwsClause.isNone() )
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(_throwsClause.unwrap());
	}

	@Override
	public Functional instantiate(List<StaticArg> args) {
		// TODO Auto-generated method stub
		return NI.nyi();
	}

	@Override
	public Type getReturnType() {
		// TODO Auto-generated method stub
		return NI.nyi();
	}
	
	
}
