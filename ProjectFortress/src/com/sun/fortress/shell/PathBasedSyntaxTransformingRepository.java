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

package com.sun.fortress.shell;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.Parser.Result;
import com.sun.fortress.exceptions.ProgramError;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.syntax_abstractions.parser.FortressParser;
import com.sun.fortress.useful.Path;

import edu.rice.cs.plt.tuple.Option;

public class PathBasedSyntaxTransformingRepository extends PathBasedRepository {

	final GlobalEnvironment env;

	public PathBasedSyntaxTransformingRepository(Path p, GlobalEnvironment specificEnv) {
		super(p);
		this.env = specificEnv;
	}

	public PathBasedSyntaxTransformingRepository(Path p) {
		super(p);
		this.env = new GlobalEnvironment.FromRepository(this);
	}

	@Override
	protected CompilationUnit getCompilationUnit(APIName api_name, File f) throws IOException {
		Result result = FortressParser.parse(api_name, f, env, verbose());
		if (result.isSuccessful()) {
			Iterator<Api> apis = result.apis().iterator();
			Iterator<Component> components = result.components().iterator();
			if (apis.hasNext()) return apis.next();
			if (components.hasNext()) return components.next();
			throw new ProgramError("Successful parse result was nonetheless empty, file " + f.getCanonicalPath());
		}
		throw new ProgramError(result.errors());
	}

}
