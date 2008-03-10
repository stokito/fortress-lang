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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AsIfExpr;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdType;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes.LooseJuxt;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.OprExpr;
import com.sun.fortress.nodes.OptionalSymbol;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.QualifiedOpName;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StringLiteralExpr;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.syntax_abstractions.phases.NonterminalTypeDictionary;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

public class JavaAstPrettyPrinter extends NodeDepthFirstVisitor<String> {

	private List<String> code;
	private Map<String, PrefixedSymbol> boundVariables;

	public JavaAstPrettyPrinter(Collection<PrefixedSymbol> boundVariables) {
		this.code = new LinkedList<String>();
		this.boundVariables = new HashMap<String, PrefixedSymbol>();
		for (PrefixedSymbol ps: boundVariables) {
			if (ps.getId().isSome()) {
				this.boundVariables.put(Option.unwrap(ps.getId()).getText(), ps);
			}
		}
	}

	public Collection<? extends String> getCode() {
		return this.code;
	}

	private Collection<? extends String> mkList(List<String> exprs_result, String varName, String type) {
		List<String> cs = new LinkedList<String>();
		cs.add("List<"+type+"> "+varName+" = new LinkedList<"+type+">();");
		for (String s: exprs_result) {
			cs.add(varName+".add("+s+");");
		}
		return cs;
	}

	@Override
	public String defaultCase(Node that) {
		String rVarName = FreshName.getFreshName("def");
		this.code.add("StringLiteralExpr "+rVarName+" = new StringLiteralExpr(\""+that.getClass()+"\");");
		return rVarName;
	}

	@Override
	public String forAPINameOnly(APIName that, List<String> ids_result) {
		String rVarName = FreshName.getFreshName("apiName");
		String idName = FreshName.getFreshName("id");
		this.code.addAll(mkList(ids_result, idName, "Id"));
		this.code.add("APIName "+rVarName+" = new APIName("+idName+");");
		return rVarName;
	}

	@Override
	public String forFnRefOnly(FnRef that, List<String> fns_result,
			List<String> staticArgs_result) {
		String rVarName = FreshName.getFreshName("fn");
		String fns = FreshName.getFreshName("ls");
		this.code.addAll(mkList(fns_result, fns, "QualifiedIdName"));
		String staticArgs = FreshName.getFreshName("ls");
		this.code.addAll(mkList(staticArgs_result, staticArgs, "StaticArg"));
		this.code.add("FnRef "+rVarName+" = new FnRef("+that.isParenthesized()+", "+fns+", "+staticArgs+");");
		return rVarName;
	}

	@Override
	public String forId(Id that) {
		String rVarName = FreshName.getFreshName("id");
		this.code.add("Id "+rVarName+" = new Id(\""+that.getText()+"\");");
		return rVarName;
	}

	@Override
	public String forIdTypeOnly(IdType that, String name_result) {
		String rVarName = FreshName.getFreshName("idType");
		this.code.add("IdType "+rVarName+" = new IdType("+name_result+");");
		return rVarName;		
	}

	@Override
	public String forLooseJuxtOnly(LooseJuxt that, List<String> exprs_result) {
		String rVarName = FreshName.getFreshName("lj");
		String args = FreshName.getFreshName("ls");
		this.code.addAll(mkList(exprs_result, args, "Expr"));
		this.code.add("LooseJuxt "+rVarName+" = new LooseJuxt("+that.isParenthesized()+", "+args+");");
		return rVarName;
	}

	@Override
	public String forQualifiedIdNameOnly(QualifiedIdName that,
			Option<String> api_result, String name_result) {
		String rVarName = FreshName.getFreshName("qIdName");
		String api = "";
		if (api_result.isNone()) {
			api = "Option.<APIName>none()";
		}
		else {
			api = "Option.<APIName>some("+Option.unwrap(api_result)+")";
		}
		this.code.add("QualifiedIdName "+rVarName+" = new QualifiedIdName("+api+", "+name_result+");");
		return rVarName; 
	}

	@Override
	public String forStringLiteralExpr(StringLiteralExpr that) {
		String varName = FreshName.getFreshName("s");
		this.code.add("StringLiteralExpr "+varName+" = new StringLiteralExpr(\""+that.getText()+"\");");
		return varName;
	}

	@Override
	public String forTightJuxtOnly(TightJuxt that, List<String> exprs_result) {
		String varName = FreshName.getFreshName("ls");
		String rVarName = FreshName.getFreshName("tj");
		this.code.addAll(mkList(exprs_result, varName, "Expr"));
		this.code.add("TightJuxt "+rVarName+" = new TightJuxt("+that.isParenthesized()+", "+varName+");");
		return rVarName; 
	}

	@Override
	public String forTypeArgOnly(TypeArg that, String type_result) {
		String rVarName = FreshName.getFreshName("typeArg");
		this.code.add("TypeArg "+rVarName+" = new TypeArg("+that.isParenthesized()+", "+type_result+");");
		return rVarName; 
	}

	@Override
	public String forVarRefOnly(VarRef that, String var_result) {
		String varName = FreshName.getFreshName("varRef");
		this.code.add("VarRef "+varName+" = new VarRef("+var_result+");");
		return varName;
	}

	@Override
	public String forVarRef(VarRef that) {
		if (this.boundVariables.containsKey(that.getVar().toString())) {
			PrefixedSymbol ps = this.boundVariables.get(that.getVar().toString());
			Option<Type> t = getType(ps);
			if (t.isSome()) {
				Type type = Option.unwrap(t);
				if (type instanceof InstantiatedType) {
					InstantiatedType instType = (InstantiatedType) type;
					return handleList(that.getVar().toString(), IterUtil.first(instType.getArgs()));
				}
			}
			return that.getVar().toString();
		}
		return super.forVarRef(that);
	}

	private String handleList(String varName, StaticArg saType) {
		String rVarName = FreshName.getFreshName("list");
		if(saType instanceof TypeArg) {
			TypeArg type = (TypeArg) saType;
//			String typeVarName = saType.accept(this);
			String t = type.getType().toString();
			String staticArgVarName = FreshName.getFreshName("ls");
			this.code.add("List<StaticArg> "+staticArgVarName+" = new LinkedList<StaticArg>();");
			this.code.add(staticArgVarName+".add("+type.accept(this)+");");
			this.code.add("Expr "+rVarName+" = null;");
			this.code.add("if ("+varName+".list().isEmpty()) {");
			this.code.add(rVarName + " = SyntaxAbstractionUtil.makeObjectInstantiation(createSpan(yyStart, yyCount), \"ArrayList\", \"emptyList\", Option.<Expr>none(), "+staticArgVarName+");");
			this.code.add("}");

			this.code.add("else {");
			this.code.add("List<QualifiedOpName> ops = new LinkedList<QualifiedOpName>();");
			this.code.add("ops.add(NodeFactory.makeQualifiedEncloserOpName(createSpan(yyStart,yyCount)));");
			this.code.add("List<Expr> args = "+varName+".list();");
//			this.code.add("Expr e = args.remove(0);");
//			this.code.add("e = new AsIfExpr(e, new InstantiatedType("+((IdType)type.getType()).getName().accept(this)+", new LinkedList<StaticArg>()));");
//			this.code.add("args.add(0, e);");
			this.code.add(rVarName + " = new OprExpr(new OpRef(ops), args);");		
			this.code.add("}");
			return rVarName;
		}
		else throw new RuntimeException("Unforseen type: "+saType.getClass());
	}

	/**
	 * We siliently assume, that Terminal definitions does not 
	 * @param ps
	 * @param nonterminalNameToDeclaredReturnType
	 * @return
	 */
	private Option<Type> getType(PrefixedSymbol ps) {

//		Pair<String, Option<Kinds>> result = ps.getSymbol().accept(new NodeDepthFirstVisitor<Pair<String, Option<Kinds>>>() {
//
//			@Override
//			public Pair<String, Option<Kinds>> defaultCase(Node that) {
//				return new Pair<String, Option<Kinds>>("", Option.<Kinds>none());
//			}		
//
//			@Override
//			public Pair<String, Option<Kinds>> forOptionalSymbolOnly(
//					OptionalSymbol that,
//					Pair<String, Option<Kinds>> symbol_result) {
//				return handle(symbol_result, Kinds.OPTIONAL);
//			}
//
//			@Override
//			public Pair<String, Option<Kinds>> forRepeatOneOrMoreSymbolOnly(
//					RepeatOneOrMoreSymbol that,
//					Pair<String, Option<Kinds>> symbol_result) {
//				return handle(symbol_result, Kinds.REPETITION);
//			}
//
//			@Override
//			public Pair<String, Option<Kinds>> forRepeatSymbolOnly(
//					RepeatSymbol that, Pair<String, Option<Kinds>> symbol_result) {
//				return handle(symbol_result, Kinds.REPETITION);
//			}
//
//			@Override 
//			public Pair<String, Option<Kinds>> forNonterminalSymbol(NonterminalSymbol that) {
//				return new Pair<String, Option<Kinds>>(that.getNonterminal().getName().toString(), Option.<Kinds>none());
//			}
//
//			private Pair<String, Option<Kinds>> handle(
//					Pair<String, Option<Kinds>> p, Kinds kind) {
//				if (p.second().isNone()) {
//					return new Pair<String, Option<Kinds>>(p.first(), Option.some(kind));
//				}
//				return new Pair<String, Option<Kinds>>(p.first(), Option.some(kind));
//			}
//		});
//
//		Option<Type> type = NonterminalTypeDictionary.getType(result.first());
//
//		if (type.isNone()) {
//			return Option.none();
//		}
//
//		List<StaticArg> staticArgs = new LinkedList<StaticArg>();
//		staticArgs.add(new TypeArg(Option.unwrap(type)));
//
//		if (result.second().isNone()) {
//			return type;
//		}
//
////		if (Option.unwrap(result.second()).equals(Kinds.OPTIONAL)) {
////			QualifiedIdName name = NodeFactory.makeQualifiedIdName("FortressLibrary", "Maybe");
////			return Option.<Type>some(NodeFactory.makeInstantiatedType(name, staticArgs));
////		}
////		else if (Option.unwrap(result.second()).equals(Kinds.REPETITION)) {
////			QualifiedIdName name = NodeFactory.makeQualifiedIdName("ArrayList", "List");
////			return Option.<Type>some(NodeFactory.makeInstantiatedType(name, staticArgs));
////		}
		return Option.none();
	}
}
