package com.sun.fortress.macro;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import xtc.parser.Module;

public class FortressGrammar {

	public void writeGrammarToDir(List<Module> modules, String tempDir) {
		File outFile = new File(tempDir+File.separatorChar+"Fortress.Rats");
        FileWriter out;
		try {
			out = new FileWriter(outFile);
	        String fortressDotRats = this.getFortressDotRats();
	        for (Module m: modules) {
	        	String originalName = m.name.toString().substring(0, m.name.toString().length()-3);
				fortressDotRats = fortressDotRats.replaceAll(originalName , m.name.toString());
	        }
			out.write(fortressDotRats);
	        out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
	
	private String getFortressDotRats() {
		String result = "";
		result = "/*******************************************************************************\n"+
		"    Copyright 2007 Sun Microsystems, Inc.,\n"+
		"    4150 Network Circle, Santa Clara, California 95054, U.S.A.\n"+
		"    All rights reserved.\n"+
		"\n"+
		"    U.S. Government Rights - Commercial software.\n"+
		"    Government users are subject to the Sun Microsystems, Inc. standard\n"+
		"    license agreement and applicable provisions of the FAR and its supplements.\n"+
		"\n"+
		"    Use is subject to license terms.\n"+
		"\n"+
		"    This distribution may include materials developed by third parties.\n"+
		"\n"+
		"    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered\n"+
		"    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.\n"+
		" ******************************************************************************/\n"+
		"\n"+
		"/*\n"+
		" * A complete Fortress grammar.\n"+
		" */\n"+
		"module      Fortress;\n"+
		"modify      Compilation  (Declaration, Identifier, Keyword, Symbol, Spacing);\n"+
		"instantiate Declaration  (TraitObject, Function, Variable, OtherDecl, Spacing,\n"+
		"                          Syntax);\n"+
		"instantiate Syntax       (Identifier, Keyword, Spacing, Symbol, Expression);\n"+
		"instantiate TraitObject  (Method, Field, AbsField, Parameter, OtherDecl, Header,\n"+
		"                          Identifier, Keyword, Spacing);\n"+
		"instantiate Function     (Parameter, Header, Type, NoNewlineExpr, Identifier,\n"+
		"                          Keyword, Symbol, Spacing);\n"+
		"instantiate Parameter    (Header, Type, Expression, Identifier, Keyword, Symbol,\n"+
		"                          Spacing);\n"+
		"instantiate Method       (MethodParam, Header, Type, NoNewlineExpr, Identifier,\n"+
		"                          Keyword, Symbol, Spacing);\n"+
		"instantiate MethodParam  (Parameter, Keyword);\n"+
		"instantiate Variable     (Header, Type, NoNewlineExpr, Identifier, Keyword,\n"+
		"                          Symbol, Spacing);\n"+
		"instantiate Field        (Variable, Header, Type, Identifier, Spacing);\n"+
		"instantiate AbsField     (Variable, Header, Type, Identifier, Spacing);\n"+
		"instantiate Header       (OtherDecl, Type, Expression, DelimitedExpr, Identifier,\n"+
		"                          Keyword, Symbol, Spacing);\n"+
		"instantiate OtherDecl    (Parameter, Header, Type, NoNewlineExpr, Identifier,\n"+
		"                          Keyword, Symbol, Spacing);\n"+
		"instantiate Type         (Header, Expression, Literal, Identifier, Keyword,\n"+
		"                          Symbol, Spacing);\n"+
		"instantiate Expression   (Parameter, Header, Type, DelimitedExpr, Identifier,\n"+
		"                          Keyword, Symbol, Spacing);\n"+
		"instantiate DelimitedExpr(TraitObject, Header, Type, Expression, LocalDecl,\n"+
		"                          Literal, Identifier, Keyword, Symbol, Spacing);\n"+
		"instantiate NoNewlineExpr(Expression, Keyword, Symbol, Spacing);\n"+
		"instantiate NoSpaceExpr  (Expression, Keyword, Symbol, Spacing);\n"+
		"instantiate Literal      (DelimitedExpr, NoSpaceExpr, Symbol, Spacing, Keyword);\n"+
		"instantiate LocalDecl    (Variable, Function, Parameter, Header, Type,\n"+
		"                          NoNewlineExpr, DelimitedExpr, Identifier, Keyword,\n"+
		"                          Symbol, Spacing);\n"+
		"instantiate Identifier   (Keyword, Symbol, Unicode, Spacing);\n"+
		"instantiate Symbol       (DelimitedExpr, NoNewlineExpr, Type, Identifier,\n"+
		"                          Spacing, Keyword);\n"+
		"instantiate Spacing      (Symbol);\n"+
		"instantiate Keyword      (Identifier);\n"+
		"instantiate Unicode;\n"+
		"\n"+
		"header {\n"+
		"import com.sun.fortress.interpreter.evaluator.ProgramError;\n"+
		"import com.sun.fortress.parser_util.precedence_opexpr.PrecedenceOpExpr;\n"+
		"import com.sun.fortress.parser_util.precedence_opexpr.Left;\n"+
		"import com.sun.fortress.parser_util.precedence_opexpr.Right;\n"+
		"import com.sun.fortress.parser_util.precedence_opexpr.RealExpr;\n"+
		"import com.sun.fortress.parser_util.precedence_opexpr.TightInfix;\n"+
		"import com.sun.fortress.parser_util.precedence_opexpr.LooseInfix;\n"+
		"import com.sun.fortress.parser_util.precedence_opexpr.Prefix;\n"+
		"import com.sun.fortress.parser_util.precedence_opexpr.Postfix;\n"+
		"import com.sun.fortress.parser_util.precedence_resolver.*;\n"+
		"import com.sun.fortress.nodes.*;\n"+
		"import com.sun.fortress.nodes_util.*;\n"+
		"import com.sun.fortress.parser_util.*;\n"+
		"import com.sun.fortress.useful.PureList;\n"+
		"import com.sun.fortress.useful.Empty;\n"+
		"import com.sun.fortress.useful.Cons;\n"+
		"import edu.rice.cs.plt.tuple.Option;\n"+
		"import edu.rice.cs.plt.iter.IterUtil;\n"+
		"import xtc.util.Action;\n"+
		"import java.util.Collections;\n"+
		"import java.util.ArrayList;\n"+
		"import java.util.List;\n"+
		"\n"+
		"import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;\n"+
		"import static com.sun.fortress.interpreter.evaluator.ProgramError.error;\n"+
		"import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;\n"+
		"}\n"+
		"\n"+
		"body {\n"+
		"\n"+
		"   Span createSpan(int start, int end) {\n"+
		"        Column s = column(start);\n"+
		"        Column e = column(end-1);\n"+
		"        SourceLocRats slStart = new SourceLocRats(s.file, s.line, s.column);\n"+
		"        SourceLocRats slEnd   = new SourceLocRats(e.file, e.line, e.column);\n"+
		"        return new Span(slStart, slEnd);\n"+
		"   }\n"+
		"}\n"+
		"\n"+
		"option parser(com.sun.fortress.parser.Fortress);";

		
		return result;
	}


}
