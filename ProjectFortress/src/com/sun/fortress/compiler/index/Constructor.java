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
import java.util.Collections;
import java.util.List;

import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;

public class Constructor extends Function {

    private final Id _declaringTrait;
    private final List<StaticParam> _staticParams;
    private final Option<List<Param>> _params;
    private final Option<List<BaseType>> _throwsClause;
    private final Option<WhereClause> _where;

    public Constructor(Id declaringTrait,
                       List<StaticParam> staticParams,
                       Option<List<Param>> params,
                       Option<List<BaseType>> throwsClause,
                       Option<WhereClause> where)
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
    public Option<WhereClause> where() { return _where; }

	@Override
	public Option<Expr> body() {
		return Option.none();
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
	public Functional instantiate(List<StaticParam> params, List<StaticArg> args) {
		// TODO Auto-generated method stub
		return NI.nyi();
	}

	@Override
	public Type getReturnType() {
		// TODO Auto-generated method stub
		return NI.nyi();
	}

	@Override
	public Functional acceptNodeUpdateVisitor(final NodeUpdateVisitor v) {

		Option<List<Param>> new_params;
		if( _params.isSome() ) {
			List<Param> new_params_ = new ArrayList<Param>(_params.unwrap().size());
			for( Param p : _params.unwrap() ) {
				new_params_.add((Param)p.accept(v));
			}
			new_params = Option.some(new_params_);
		}
		else {
			new_params = Option.none();
		}

		Option<List<BaseType>> new_throws;
		if( _throwsClause.isSome() ) {
			List<BaseType> new_throws_ = new ArrayList<BaseType>(_throwsClause.unwrap().size());
			for( BaseType p : _throwsClause.unwrap() ) {
				new_throws_.add((BaseType)p.accept(v));
			}
			new_throws = Option.some(new_throws_);
		}
		else {
			new_throws = Option.none();
		}

		List<StaticParam> new_static_params =
			CollectUtil.makeList(IterUtil.map(_staticParams, new Lambda<StaticParam,StaticParam>(){
				public StaticParam value(StaticParam arg0) {
					return (StaticParam)arg0.accept(v);
				}}));

                Option<WhereClause> new_where;
                if ( _where.isSome() ) {
                    new_where = Option.some((WhereClause)_where.unwrap().accept(v));
                } else {
                    new_where = Option.<WhereClause>none();
                }

		return
		  new Constructor(_declaringTrait,
                                  new_static_params,
                                  new_params,
                                  new_throws,
                                  new_where);
	}


}
