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

package com.sun.fortress.syntax_abstractions.util;

import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;

import junit.framework.TestCase;

public class FortressTypeToJavaTypeJUTest extends TestCase {

    private Span span = NodeFactory.makeSpan("bogus");
    
    private VarType stringType = new VarType(span, NodeFactory.makeId("FortressBuiltin", "String"));
    private String stringTypeResult = "String";
    private VarType fortressASTType = new VarType(span, NodeFactory.makeId("FortressAst", "Decl"));
    private String fortressASTTypeResult = "Decl";

    private TraitType mkTraitType(String api, String id, BaseType typeArg) {
        Id name = NodeFactory.makeId(api, id);
        List<StaticArg> args = new LinkedList<StaticArg>();
        args.add(new TypeArg(span, typeArg));
        return new TraitType(span, name, args);
    }

    public void testTypeTranslatorVarTypeString() {
        assertEquals(FortressTypeToJavaType.analyze(stringType), stringTypeResult);
    }

    public void testTypeTranslatorVarTypeFortressAst() {
        assertEquals(FortressTypeToJavaType.analyze(fortressASTType), fortressASTTypeResult);
    }

    public void testTypeTranslatorTraitTypeListString() {
        TraitType type = mkTraitType("List", "List", stringType);
        assertEquals(FortressTypeToJavaType.analyze(type), "List<"+stringTypeResult+">");
    }

    public void testTypeTranslatorTraitTypeListFortressASTType() {
        TraitType type = mkTraitType("List", "List", fortressASTType);
        assertEquals(FortressTypeToJavaType.analyze(type), "List<"+fortressASTTypeResult+">");
    }

    public void testTypeTranslatorTraitTypeMaybeFortressASTType() {
        TraitType type = mkTraitType("FortressLibrary", "Maybe", fortressASTType);
        assertEquals(FortressTypeToJavaType.analyze(type), "Option<"+fortressASTTypeResult+">");
    }

    public void testTypeTranslatorTraitTypeJustFortressASTType() {
        TraitType type = mkTraitType("FortressLibrary", "Just", fortressASTType);
        assertEquals(FortressTypeToJavaType.analyze(type), "Option<"+fortressASTTypeResult+">");
    }

    public void testTypeTranslatorTraitTypeNothingFortressASTType() {
        TraitType type = mkTraitType("FortressLibrary", "Nothing", fortressASTType);
        assertEquals(FortressTypeToJavaType.analyze(type), "Option<"+fortressASTTypeResult+">");
    }

    public void testTypeTranslatorTraitTypeListListFortressASTType() {
        TraitType listType = mkTraitType("List", "List", fortressASTType);
        TraitType listListType = mkTraitType("List", "List", listType);
        assertEquals(FortressTypeToJavaType.analyze(listListType), "List<List<"+fortressASTTypeResult+">>");
    }

    public void testTypeTranslatorTraitTypeListListStringType() {
        TraitType listType = mkTraitType("List", "List", stringType);
        TraitType listListType = mkTraitType("List", "List", listType);
        assertEquals(FortressTypeToJavaType.analyze(listListType), "List<List<"+stringTypeResult+">>");
    }

    public void testTypeTranslatorTraitTypeMaybeListFortressASTType() {
        TraitType listType = mkTraitType("List", "List", fortressASTType);
        TraitType listListType = mkTraitType("FortressLibrary", "Maybe", listType);
        assertEquals(FortressTypeToJavaType.analyze(listListType), "Option<List<"+fortressASTTypeResult+">>");
    }

    public void testTypeTranslatorTraitTypeMaybeListStringType() {
        TraitType listType = mkTraitType("List", "List", stringType);
        TraitType listListType = mkTraitType("FortressLibrary", "Maybe", listType);
        assertEquals(FortressTypeToJavaType.analyze(listListType), "Option<List<"+stringTypeResult+">>");
    }

}
