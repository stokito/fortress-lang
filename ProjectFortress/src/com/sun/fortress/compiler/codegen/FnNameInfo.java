/*******************************************************************************
    Copyright 2009,2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.index.Functional;
import com.sun.fortress.compiler.index.HasTraitStaticParameters;
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.FnHeader;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.useful.ConcatenatedList;
import com.sun.fortress.useful.DeletedList;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.InsertedList;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;

public class FnNameInfo {
    final List<StaticParam> static_params;
    private final List<StaticParam> trait_static_params;
    private final Type returnType;
    private final Type paramType;
    final APIName ifNone;
    private final IdOrOp name;
    final Span span;
    
    public FnNameInfo(List<StaticParam> static_params,
            List<StaticParam> trait_static_params, Type returnType,
            Type paramType, APIName ifNone, IdOrOp name, Span span) {
        
        super();
        this.static_params = static_params;
        this.trait_static_params = trait_static_params;
        this.returnType = returnType;
        this.paramType = paramType;
        this.ifNone = ifNone;
        this.name = name;
        this.span = span;
    }
    
    public FnNameInfo convertGenericMethodToClosureDecl(int self_index,
            List<StaticParam> to_static_params) {
        Id sp_name = NodeFactory.makeId(span, Naming.UP_INDEX);
        Id p_name = NamingCzar.selfName(span);
        StaticParam new_sp = NodeFactory.makeTypeParam(span, Naming.UP_INDEX);
        Type inserted_param_type = NodeFactory.makeVarType(span, sp_name);
        // convert paramType to list of types, remove self, add inserted_param_type at zero.
        List<Type> param_types = paramType instanceof TupleType ?
                ((TupleType) paramType).getElements() : Useful.list(paramType);
        if (self_index != Naming.NO_SELF)
            param_types = new DeletedList<Type>(param_types, self_index);
        param_types = new InsertedList<Type>(param_types, 0, inserted_param_type);
        Type new_param_type = param_types.size() == 1 ?
                param_types.get(0) :
                    NodeFactory.makeTupleType(span, param_types);
        // edit static params -- new_sp, method_sp, trait_sp
        List<StaticParam> new_sparams =
            new ConcatenatedList<StaticParam>(
                    new InsertedList<StaticParam>(static_params, 0, new_sp),
                to_static_params);
        // This use of trait_static_params is perhaps a little dubious; we'll see.
        return new FnNameInfo(new_sparams, trait_static_params, returnType, new_param_type,ifNone, getName(), span);
    }

    public FnNameInfo(FnDecl x, List<StaticParam> trait_static_params, APIName ifNone) {
        FnHeader header = x.getHeader();

        static_params = x.getHeader().getStaticParams();
        this.trait_static_params = trait_static_params;
        returnType = header.getReturnType().unwrap();
        paramType = NodeUtil.getParamType(x);
        this.ifNone = ifNone;
        span = NodeUtil.getSpan(x);
        name = (IdOrOp) (x.getHeader().getName());
    }
    
    public FnNameInfo(Functional x, APIName ifNone) {
        HasTraitStaticParameters htsp = (HasTraitStaticParameters) x;
        trait_static_params =  x instanceof HasTraitStaticParameters ?
            ((HasTraitStaticParameters)x).traitStaticParameters():
                Collections.<StaticParam>emptyList();
        static_params = x.staticParameters();
        ArrowType at = fndeclToType(x, Naming.NO_SELF);
        returnType = at.getRange();
        paramType = at.getDomain();
        this.ifNone = ifNone;
        span =x.getSpan();
        name = x.name();
    }
    
    /**
     * Returns the ArrowType of a method where the self parameter (if any)
     * has been removed from the domain.  This is used to create signatures
     * for the method part of a functional method.
     * 
     * @param x
     * @param selfIndex
     * @return
     */
    static ArrowType fndeclToType(Functional x, int selfIndex) {
        Type rt = x.getReturnType().unwrap();
        List<Param> lp = x.parameters();
        if (selfIndex != Naming.NO_SELF)
            lp = new DeletedList<Param>(lp, selfIndex);
        return typeAndParamsToArrow(x.getSpan(), rt, lp);
    }

    /**
     * @param x
     * @param rt
     * @param lp
     * @return
     */
    static ArrowType typeAndParamsToArrow(Span span, Type rt, List<Param> lp) {
        Type dt = null;
        switch (lp.size()) {
        case 0:
            dt = NodeFactory.makeVoidType(span);
            break;
        case 1:
            dt = (Type)lp.get(0).getIdType().unwrap(); // TODO varargs
            break;
        default:
            dt = NodeFactory.makeTupleType(Useful.applyToAll(lp, new Fn<Param,Type>() {
                @Override
                public Type apply(Param x) {
                    return (Type)x.getIdType().unwrap(); // TODO varargs
                }}));
            break;
        }
        return NodeFactory.makeArrowType(NodeFactory.makeSpan(dt,rt), dt, rt);
    }
    
    public TypeArg boundsFor(StaticParam sp) {
        List<BaseType> tl = sp.getExtendsClause();
        if (tl.size() == 0) {
            return NodeFactory.makeTypeArg(NodeFactory.makeTraitType(
                    NodeFactory.makeId(sp.getInfo().getSpan(),
                            Option.<APIName>some(NamingCzar.fortressLibrary()), "Object")));
        } else if (tl.size() == 1) {
            return NodeFactory.makeTypeArg(tl.get(0));
        } else {
            return NodeFactory.makeTypeArg(NodeFactory.makeIntersectionType(tl));
        }
    }
    
    public ArrowType normalizedSchema(Type at) {
        // make list of params by concatenating trait and method params
        // make list of synthesized args in form "1t", "2t", ..., "1m", "2m", ..
        // use STR to normalize the return type.
        List<StaticParam> lsp = new ArrayList<StaticParam>();
        List<StaticArg> lsa = new ArrayList<StaticArg>();
        int i = 1;
        for (StaticParam sp : trait_static_params) {
            lsp.add(sp);
            lsa.add(boundsFor(sp));
            // lsa.add(NodeFactory.makeTypeArg(NodeFactory.makeVarType(sp.getInfo().getSpan(), i + "t")));
            i++;
        }
        i = 1;
        for (StaticParam sp : static_params) {
            lsp.add(sp);
            lsa.add(boundsFor(sp));
            // lsa.add(NodeFactory.makeTypeArg(NodeFactory.makeVarType(sp.getInfo().getSpan(), i + "m")));
            i++;
        }
        StaticTypeReplacer str = new StaticTypeReplacer(lsp, lsa);
        ArrowType new_at =  (ArrowType) (str.replaceIn(at));
        return  new_at;
    }

    /**
     * Returns the ArrowType of a method where the self parameter (if any)
     * has been removed from the domain.  This is used to create signatures
     * for the method part of a functional method.
     * 
     * @param x
     * @param selfIndex
     * @return
     */
    static ArrowType methodArrowType(FnNameInfo x, int selfIndex) {
        Type rt = x.returnType;
        Type dt = x.paramType;
        if (selfIndex != Naming.NO_SELF) {
            if (dt instanceof TupleType) {
                List<Type> types = ((TupleType) dt).getElements();
                types = new DeletedList<Type>(types, selfIndex);
                dt = types.size() == 1 ?
                        types.get(0) :
                        NodeFactory.makeTupleType(NodeUtil.getSpan(dt), types);
            } else {
               dt = NodeFactory.makeVoidType(x.span);
            }
        }
        return NodeFactory.makeArrowType(NodeFactory.makeSpan(dt,rt), dt, rt);
    }
    
    public ArrowType methodArrowType(int selfIndex) {
        return normalizedSchema(methodArrowType(this, selfIndex));
    }

    public ArrowType functionArrowType() {
        return methodArrowType(this, Naming.NO_SELF);
    }

    public IdOrOp getName() {
        return name;
    }
    
    public List<StaticParam> getTrait_static_params() {
        return trait_static_params;
    }
    public List<StaticParam> getStatic_params() {
        return static_params;
    }
}
