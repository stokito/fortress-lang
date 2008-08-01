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

package com.sun.fortress.compiler.desugarer;

import java.util.Collections;
import java.util.List;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.interpreter.glue.WellKnownNames;

import edu.rice.cs.plt.tuple.Option;

public class ExtendsObjectVisitor extends NodeUpdateVisitor {
    
    private final Id anyTypeId = new Id(WellKnownNames.anyTypeName);
    
    
    /** If the extends clause of a trait declaration, an object declaration, or 
     *  an object expression is empty, then replace the empty extends clause
     *  with {Object}.
     */
    private List<TraitTypeWhere> rewriteExtendsClause(Node whence, List<TraitTypeWhere> extendsClause) {
        if (extendsClause.size() > 0) return extendsClause;
        Id objectId = NodeFactory.makeId(whence.getSpan(),
                                         WellKnownNames.objectTypeName);
        TraitType typeObject = NodeFactory.makeTraitType(objectId);
        TraitTypeWhere extendsObject = NodeFactory.makeTraitTypeWhere(typeObject);
        return Collections.singletonList(extendsObject);
    }

    @Override
    public Node forObjectExprOnly(ObjectExpr that, Option<Type> exprType_result, List<TraitTypeWhere> extendsClause, List<Decl> decls) {
        extendsClause = rewriteExtendsClause(that, extendsClause);
        return super.forObjectExprOnly(that, exprType_result, extendsClause, decls);
    }

    @Override
    public Node forAbsTraitDeclOnly(AbsTraitDecl that, List<Modifier> mods, Id name,
                                    List<StaticParam> staticParams, List<TraitTypeWhere> extendsClause,
                                    WhereClause where, List<BaseType> excludes,
                                    Option<List<BaseType>> comprises, List<AbsDecl> decls) {
        if (!that.getName().equals(anyTypeId)) {
            extendsClause = rewriteExtendsClause(that, extendsClause);
        }
        return super.forAbsTraitDeclOnly(that, mods, name, staticParams, extendsClause,
                                         where, excludes, comprises, decls);
    }

    @Override
    public Node forTraitDeclOnly(TraitDecl that, List<Modifier> mods, Id name,
                                 List<StaticParam> staticParams, List<TraitTypeWhere> extendsClause,
                                 WhereClause where, List<BaseType> excludes,
                                 Option<List<BaseType>> comprises, List<Decl> decls) {
        if (!that.getName().equals(anyTypeId)) {
            extendsClause = rewriteExtendsClause(that, extendsClause);
        }        
        return super.forTraitDeclOnly(that, mods, name, staticParams, extendsClause,
                                      where, excludes, comprises, decls);
    }

    @Override
    public Node forAbsObjectDeclOnly(AbsObjectDecl that, List<Modifier> mods, Id name,
                                     List<StaticParam> staticParams, List<TraitTypeWhere> extendsClause,
                                     WhereClause where, Option<List<Param>> params,
                                     Option<List<BaseType>> throwsClause, Contract contract,
                                     List<AbsDecl> decls) {
        extendsClause = rewriteExtendsClause(that, extendsClause);
        return super.forAbsObjectDeclOnly(that, mods, name, staticParams, extendsClause,
                                          where, params, throwsClause, contract, decls);
    }
    
    @Override
    public Node forObjectDeclOnly(ObjectDecl that, List<Modifier> mods, Id name,
                                  List<StaticParam> staticParams, List<TraitTypeWhere> extendsClause,
                                  WhereClause where, Option<List<Param>> params,
                                  Option<List<BaseType>> throwsClause, Contract contract,
                                  List<Decl> decls) {
        extendsClause = rewriteExtendsClause(that, extendsClause);
        return super.forObjectDeclOnly(that, mods, name, staticParams, extendsClause,
                                       where, params, throwsClause, contract, decls);
    }

}
