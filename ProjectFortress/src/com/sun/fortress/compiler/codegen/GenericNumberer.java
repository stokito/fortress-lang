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

package com.sun.fortress.compiler.codegen;

import java.util.List;
import java.util.Map;

import com.sun.fortress.nodes.ASTNodeInfo;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.ExprInfo;
import com.sun.fortress.nodes.FunctionalRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.Overloading;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.StaticParamKind;
import com.sun.fortress.nodes.TraitSelfType;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeInfo;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.F;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;

public class GenericNumberer extends NodeUpdateVisitor {
    
   private final Map<String, String> xlation;

    GenericNumberer(Map<String, String> xlation) {
        this.xlation = xlation;
    }

    private Id xlate(Id x) {
        if (x.getApiName().isSome())
            return x;
        String s = x.getText();
        if (!xlation.containsKey(s))
            return x;
        s = xlation.get(s);
        return NodeFactory.makeId(x,s);
    }
    
    private Op xlate(Op x) {
        if (x.getApiName().isSome())
            return x;
        String s = x.getText();
        if (!xlation.containsKey(s))
            return x;
        s = xlation.get(s);
        return NodeFactory.makeOp(x,s);
    }
    
    private IdOrOp xlate (IdOrOp x) {
        if (x instanceof Op)
            return xlate((Op) x);
        if (x instanceof Id)
            return xlate((Id) x);
        throw new Error("This cannot happen; instanceof should have covered all cases, but saw " + x == null ? "null" : x.getClass().getName());
    }
    
    F<IdOrOp,IdOrOp> xlator = new F<IdOrOp, IdOrOp>() {

        @Override
        public IdOrOp apply(IdOrOp x) {
            return xlate(x);
        }
        
    };
    
    @Override
    public Node forStaticParam(StaticParam that) {
        ASTNodeInfo info_result = (ASTNodeInfo) recur(that.getInfo());
        IdOrOp name_result = xlate(that.getName());
        List<BaseType> extendsClause_result = recurOnListOfBaseType(that.getExtendsClause());
        Option<Type> dimParam_result = recurOnOptionOfType(that.getDimParam());
        StaticParamKind kind_result = (StaticParamKind) recur(that.getKind());
        return forStaticParamOnly(that, info_result, name_result, extendsClause_result, dimParam_result, kind_result);
    }

     @Override
    public Node forOpRef(OpRef that) {
        // Clone from parent.
        ExprInfo info_result = (ExprInfo) recur(that.getInfo());
        List<StaticArg> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
        
        // need to rewrite originalName and names.
        IdOrOp originalName_result = xlate(that.getOriginalName());
        List<IdOrOp> names_result = Useful.<IdOrOp>applyToAllPossiblyReusing(that.getNames(), xlator);
        
        // what about overloadings?
        List<Overloading> overloadings_result = recurOnListOfOverloading(that.getInterpOverloadings());
        List<Overloading> newOverloadings_result = recurOnListOfOverloading(that.getNewOverloadings());
        
        Option<Type> overloadingType_result = recurOnOptionOfType(that.getOverloadingType());
        return forOpRefOnly(that, info_result, staticArgs_result, originalName_result, names_result, overloadings_result, newOverloadings_result, overloadingType_result);
    }

    @Override
    public Node forTraitSelfType(TraitSelfType that) {
        return that.getNamed().accept(this);
    }

    @Override
    public Node forTraitType(TraitType that) {
        TypeInfo info_result = (TypeInfo) recur(that.getInfo());
        
        // replace the name
        Id name_result = xlate(that.getName());
        
        List<StaticArg> args_result = recurOnListOfStaticArg(that.getArgs());
        List<StaticParam> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
        return forTraitTypeOnly(that, info_result, name_result, args_result, staticParams_result);
    }

    @Override
    public Node forVarRef(VarRef that) {
        ExprInfo info_result = (ExprInfo) recur(that.getInfo());
        
        // replace the varId
        Id varId_result = xlate(that.getVarId());
        
        List<StaticArg> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
        return forVarRefOnly(that, info_result, varId_result, staticArgs_result);
    }

    @Override
    public Node forVarType(VarType that) {
        TypeInfo info_result = (TypeInfo) recur(that.getInfo());
        
        // replace the name
        Id name_result = xlate(that.getName());
        
        return forVarTypeOnly(that, info_result, name_result);
    }
        
}
