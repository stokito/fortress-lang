/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.nodes.StaticParamKind;
import com.sun.fortress.nodes.KindType;
import com.sun.fortress.nodes.KindInt;
import com.sun.fortress.nodes.KindNat;
import com.sun.fortress.nodes.KindBool;
import com.sun.fortress.nodes.KindDim;
import com.sun.fortress.nodes.KindUnit;
import com.sun.fortress.nodes.KindOp;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes._InferenceVarType;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;


/**
 * An environment for static parameters that are allowed to be used as expressions.
 * Nat params, int params, bool params are the best examples, but op params may be
 * useable too. (Note that op params have not yet been implemented.)
 */
public class StaticParamTypeEnv extends TypeEnv {

    final static Type STATIC_INT_TYPE = Types.INT_LITERAL;
    final static Type STATIC_NAT_TYPE = Types.INT_LITERAL;
    final static Type STATIC_BOOL_TYPE = Types.BOOLEAN;

    final List<StaticParam> entries;
    final private TypeEnv parent;

    public StaticParamTypeEnv(List<StaticParam> _static, TypeEnv _parent) {
        entries=Collections.unmodifiableList(new ArrayList<StaticParam>(_static));
        parent=_parent;
    }

    private Option<Pair<StaticParam,Type>> findParam(Id _var) {
        Id no_api_var = removeApi(_var);
        for (StaticParam param : entries) {
            IdOrOp name = nameFromStaticParam(param);
            if(name.equals(no_api_var) || name.equals(_var) ){
                Option<Type> type_ = typeOfStaticParam(param);

                if( type_.isSome() )
                    return Option.some(Pair.make(param, type_.unwrap()));
            }
        }
        return Option.none();
    }

    @Override
    public Option<BindingLookup> binding(IdOrOpOrAnonymousName var) {
        if (!(var instanceof Id)) { return parent.binding(var); }
        Id _var = (Id)var;

        Option<Pair<StaticParam,Type>> p = findParam(_var);

        if( p.isSome() ) {
            Type type_ = p.unwrap().second();
            return Option.some(new BindingLookup(var, type_));
        }
        else
            return parent.binding(_var);
    }

    @Override
    public Option<StaticParam> staticParam(IdOrOpOrAnonymousName id) {
        if (!(id instanceof Id)) { return parent.staticParam(id); }
        Id _var = (Id)id;

        Id no_api_var = removeApi(_var);
        for (StaticParam param : entries) {
            IdOrOp name = nameFromStaticParam(param);

            if(name.equals(no_api_var) || name.equals(_var) ){
                return Option.some(param);
            }
        }
        return parent.staticParam(id);
    }

    private static IdOrOp nameFromStaticParam(StaticParam param) {
            return param.getName();
    }

    private static Option<Type> typeOfStaticParam(StaticParam param) {
        return param.getKind().accept( new NodeAbstractVisitor<Option<Type>>(){
                @Override
                    public Option<Type> defaultCase(Node node) {
                    return Option.<Type>none();
                }
                @Override
                    public Option<Type> forKindBool(KindBool that) {
                    return Option.<Type>some(STATIC_BOOL_TYPE);
                }
                @Override
                    public Option<Type> forKindInt(KindInt that) {
                    return Option.<Type>some(STATIC_INT_TYPE);
                }
                @Override
                    public Option<Type> forKindNat(KindNat that) {
                    return Option.<Type>some(STATIC_NAT_TYPE);
                }} );
    }

    @Override
    public List<BindingLookup> contents() {
        List<BindingLookup> result = new ArrayList<BindingLookup>();

        for( StaticParam param : entries ) {
            Option<Type> type_ = typeOfStaticParam(param);
            if( type_.isSome() )
                result.add(new BindingLookup(nameFromStaticParam(param), type_.unwrap()));
        }
        return result;
    }

    @Override
    public Option<Node> declarationSite(IdOrOpOrAnonymousName var) {
        if (!(var instanceof Id)) { return parent.declarationSite(var); }
        Id _var = (Id)var;

        Option<Pair<StaticParam,Type>> p = findParam(_var);
        if( p.isSome() )
            return Option.<Node>some(p.unwrap().first());
        else
            return parent.declarationSite(var);
    }

    @Override
    public TypeEnv replaceAllIVars(Map<_InferenceVarType, Type> ivars) {
        List<StaticParam> new_entries = new ArrayList<StaticParam>(entries.size());
        InferenceVarReplacer rep = new InferenceVarReplacer(ivars);

        for( StaticParam entry : entries ) {
            new_entries.add((StaticParam)entry.accept(rep));
        }

        return new StaticParamTypeEnv(new_entries, parent.replaceAllIVars(ivars));
    }
}
