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
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import com.sun.fortress.compiler.typechecker.TypeEnv;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.interpreter.glue.WellKnownNames;

import edu.rice.cs.plt.tuple.Option;

public class ExtendsObjectVisitor extends NodeUpdateVisitor {

    private List<TraitTypeWhere> mungeExtendsClause(Node whence, List<TraitTypeWhere> extendsClause) {
        if (extendsClause.size() > 0) return extendsClause;
        Id objectId = NodeFactory.makeId(whence.getSpan(),
                                         WellKnownNames.fortressBuiltin,
                                         WellKnownNames.objectTypeName);
        TraitType typeObject = NodeFactory.makeTraitType(objectId);
        TraitTypeWhere extendsObject = NodeFactory.makeTraitTypeWhere(typeObject);
        return Collections.singletonList(extendsObject);
    }

    @Override
    public Node forObjectExprOnly(ObjectExpr that, List<TraitTypeWhere> extendsClause, List<Decl> decls) {
        extendsClause = mungeExtendsClause(that, extendsClause);
        return super.forObjectExprOnly(that, extendsClause, decls);
    }

    public Node forAbsTraitDeclOnly(AbsTraitDecl that, List<Modifier> mods, Id name,
                                    List<StaticParam> staticParams, List<TraitTypeWhere> extendsClause,
                                    WhereClause where, List<BaseType> excludes,
                                    Option<List<BaseType>> comprises, List<AbsDecl> decls) {
        extendsClause = mungeExtendsClause(that, extendsClause);
        return super.forAbsTraitDeclOnly(that, mods, name, staticParams, extendsClause,
                                         where, excludes, comprises, decls);
    }

    public Node forTraitDeclOnly(TraitDecl that, List<Modifier> mods, Id name,
                                 List<StaticParam> staticParams, List<TraitTypeWhere> extendsClause,
                                 WhereClause where, List<BaseType> excludes,
                                 Option<List<BaseType>> comprises, List<Decl> decls) {
        extendsClause = mungeExtendsClause(that, extendsClause);
        return super.forTraitDeclOnly(that, mods, name, staticParams, extendsClause,
                                      where, excludes, comprises, decls);
    }

    public Node forAbsObjectDeclOnly(AbsObjectDecl that, List<Modifier> mods, Id name,
                                     List<StaticParam> staticParams, List<TraitTypeWhere> extendsClause,
                                     WhereClause where, Option<List<Param>> params,
                                     Option<List<BaseType>> throwsClause, Contract contract,
                                     List<AbsDecl> decls) {
        extendsClause = mungeExtendsClause(that, extendsClause);
        return super.forAbsObjectDeclOnly(that, mods, name, staticParams, extendsClause,
                                          where, params, throwsClause, contract, decls);
    }

    public Node forObjectDeclOnly(ObjectDecl that, List<Modifier> mods, Id name,
                                  List<StaticParam> staticParams, List<TraitTypeWhere> extendsClause,
                                  WhereClause where, Option<List<Param>> params,
                                  Option<List<BaseType>> throwsClause, Contract contract,
                                  List<Decl> decls) {
        extendsClause = mungeExtendsClause(that, extendsClause);
        return super.forObjectDeclOnly(that, mods, name, staticParams, extendsClause,
                                       where, params, throwsClause, contract, decls);
    }

}
