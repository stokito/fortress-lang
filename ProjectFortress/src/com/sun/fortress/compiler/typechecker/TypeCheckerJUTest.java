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

package com.sun.fortress.compiler.typechecker;

import junit.framework.TestCase;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Collections;
import java.util.Arrays;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.io.IOUtil;
import edu.rice.cs.plt.lambda.Predicate;
import edu.rice.cs.plt.lambda.Lambda;

import com.sun.fortress.compiler.StaticTest;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.Fortress;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.shell.FileBasedRepository;

import com.sun.fortress.interpreter.drivers.ProjectProperties;
public class TypeCheckerJUTest extends StaticTest {

    private final List<String> NOT_PASSING = Arrays.asList(
        staticTests + "SimpleProgram.fss",
        staticTests + "XXXMaltypedTopLevelVar.fss",                                                          
        staticTests + "XXXMultipleRefErrors.fss",
        staticTests + "XXXUndefinedArrayRef.fss",
        staticTests + "XXXUndefinedInitializer.fss",
        staticTests + "XXXUndefinedNestedRef.fss",
        staticTests + "XXXUndefinedRefInLoop.fss",
        staticTests + "XXXUndefinedVar.fss",
        staticTests + "XXXUndefinedTopLevelVar.fss",
        /* Tests for Syntax abstractions */
        staticTests + "SyntaxHelloWorldUse.fss",
        staticTests + "SyntaxHelloWorld.fss",
        staticTests + "SyntaxGrammarImportsUse.fss",
        staticTests + "XXXSyntaxGrammarImportsUse.fss",
        staticTests + "SyntaxGrammarImports.fss",
        staticTests + "SyntaxGrammarImportsA.fss",
        staticTests + "SyntaxProductionExtends.fsi",
        staticTests + "XXXSyntaxMultipleGrammarsWithSameName.fsi",
        staticTests + "XXXSyntaxMultipleNonterminalDefsWithSameName.fsi",
        staticTests + "XXXSyntaxGrammarExtendsNonExistingGrammar.fsi",
        // really not working:
        // staticTests + "XXXSyntaxNoFortressAstImport.fsi",
        staticTests + "stub to eliminate comma trouble"
    );

    public void setUp() {
        // Turn on type checking during static checking. By default,
        // type checking is turned off to allow programmers to use
        // the static checker before type checking is fully functional.
        com.sun.fortress.compiler.StaticChecker.typecheck = true;
    }
    
    public List<String> getNotPassing() {
        return NOT_PASSING;
    }
}
