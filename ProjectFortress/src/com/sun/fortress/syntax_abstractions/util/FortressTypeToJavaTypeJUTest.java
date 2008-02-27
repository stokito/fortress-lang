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

import com.sun.fortress.nodes.IdType;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes_util.NodeFactory;

import junit.framework.TestCase;

public class FortressTypeToJavaTypeJUTest extends TestCase {

	private FortressTypeToJavaType tt = new FortressTypeToJavaType();

	private IdType stringType = new IdType(NodeFactory.makeQualifiedIdName("FortressBuiltin", "String"));
	private String stringTypeResult = "String";
	private IdType fortressASTType = new IdType(NodeFactory.makeQualifiedIdName("FortressAst", "Decl"));
	private String fortressASTTypeResult = "Decl";
	
    private InstantiatedType mkInstantiatedType(String api, String id, TraitType typeArg) {
    	QualifiedIdName name = NodeFactory.makeQualifiedIdName(api, id);
		List<StaticArg> args = new LinkedList<StaticArg>();
		args.add(new TypeArg(typeArg));
		return new InstantiatedType(name, args);
    }
	
    public void testTypeTranslatorIdTypeString() {
    	assertEquals(tt.analyze(stringType), stringTypeResult);
    }
    
    public void testTypeTranslatorIdTypeFortressAst() {
    	assertEquals(tt.analyze(fortressASTType), fortressASTTypeResult);
    }
    
    public void testTypeTranslatorInstantiatedTypeListString() {
    	InstantiatedType type = mkInstantiatedType("ArrayList", "List", stringType);
    	assertEquals(tt.analyze(type), "List<"+stringTypeResult+">");
    }
    
    public void testTypeTranslatorInstantiatedTypeListFortressASTType() {
    	InstantiatedType type = mkInstantiatedType("ArrayList", "List", fortressASTType);
    	assertEquals(tt.analyze(type), "List<"+fortressASTTypeResult+">");
    }
    
    public void testTypeTranslatorInstantiatedTypeMaybeFortressASTType() {
    	InstantiatedType type = mkInstantiatedType("FortressLibrary", "Maybe", fortressASTType);
    	assertEquals(tt.analyze(type), "Option<"+fortressASTTypeResult+">");
    }
    
    public void testTypeTranslatorInstantiatedTypeJustFortressASTType() {
    	InstantiatedType type = mkInstantiatedType("FortressLibrary", "Just", fortressASTType);
    	assertEquals(tt.analyze(type), "Option<"+fortressASTTypeResult+">");
    }
    
    public void testTypeTranslatorInstantiatedTypeNothingFortressASTType() {
    	InstantiatedType type = mkInstantiatedType("FortressLibrary", "Nothing", fortressASTType);
    	assertEquals(tt.analyze(type), "Option<"+fortressASTTypeResult+">");
    }
    
    public void testTypeTranslatorInstantiatedTypeListListFortressASTType() {
    	InstantiatedType listType = mkInstantiatedType("ArrayList", "List", fortressASTType);
    	InstantiatedType listListType = mkInstantiatedType("ArrayList", "List", listType);
    	assertEquals(tt.analyze(listListType), "List<List<"+fortressASTTypeResult+">>");
    }

    public void testTypeTranslatorInstantiatedTypeListListStringType() {
    	InstantiatedType listType = mkInstantiatedType("ArrayList", "List", stringType);
    	InstantiatedType listListType = mkInstantiatedType("ArrayList", "List", listType);
    	assertEquals(tt.analyze(listListType), "List<List<"+stringTypeResult+">>");
    }
    
    public void testTypeTranslatorInstantiatedTypeMaybeListFortressASTType() {
    	InstantiatedType listType = mkInstantiatedType("ArrayList", "List", fortressASTType);
    	InstantiatedType listListType = mkInstantiatedType("FortressLibrary", "Maybe", listType);
    	assertEquals(tt.analyze(listListType), "Option<List<"+fortressASTTypeResult+">>");
    }
    
    public void testTypeTranslatorInstantiatedTypeMaybeListStringType() {
    	InstantiatedType listType = mkInstantiatedType("ArrayList", "List", stringType);
    	InstantiatedType listListType = mkInstantiatedType("FortressLibrary", "Maybe", listType);
    	assertEquals(tt.analyze(listListType), "Option<List<"+stringTypeResult+">>");
    }
   
}
