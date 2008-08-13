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
import java.util.HashSet;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Accumulator;
import com.sun.fortress.nodes.AmbiguousMultifixOpExpr;
import com.sun.fortress.nodes.AnonymousFnName;
import com.sun.fortress.nodes.AsExpr;
import com.sun.fortress.nodes.AsIfExpr;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.BigFixity;
import com.sun.fortress.nodes.Block;
import com.sun.fortress.nodes.ChainExpr;
import com.sun.fortress.nodes.CharLiteralExpr;
import com.sun.fortress.nodes.Do;
import com.sun.fortress.nodes.DoFront;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.EnclosingFixity;
import com.sun.fortress.nodes.FnExpr;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.GeneratorClause;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.If;
import com.sun.fortress.nodes.IfClause;
import com.sun.fortress.nodes.InFixity;
import com.sun.fortress.nodes.IntLiteralExpr;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.Link;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.LooseJuxt;
import com.sun.fortress.nodes.MathPrimary;
import com.sun.fortress.nodes.MethodInvocation;
import com.sun.fortress.nodes.MultiFixity;
import com.sun.fortress.nodes.NoFixity;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NonParenthesisDelimitedMI;
import com.sun.fortress.nodes.NormalParam;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpExpr;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.PreFixity;
import com.sun.fortress.nodes.StringLiteralExpr;
import com.sun.fortress.nodes.TemplateGap;
import com.sun.fortress.nodes.TemplateNodeDepthFirstVisitor;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TupleExpr;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes.VoidLiteralExpr;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes._SyntaxTransformation;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.syntax_abstractions.environments.GrammarEnv;
import com.sun.fortress.syntax_abstractions.environments.MemberEnv;
import com.sun.fortress.syntax_abstractions.environments.SyntaxDeclEnv;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.tuple.Option;

public class JavaAstPrettyPrinter extends TemplateNodeDepthFirstVisitor<String> {

    private List<String> code;
    private SyntaxDeclEnv syntaxDeclEnv;

    public JavaAstPrettyPrinter(SyntaxDeclEnv env) {
        this.code = new LinkedList<String>();
        this.syntaxDeclEnv = env;
    }

    public Collection<? extends String> getCode() {
        return this.code;
    }

    private Collection<? extends String> mkList(List<String> exprs_result, String varName, String type) {
        List<String> cs = new LinkedList<String>();
        cs.add(String.format("List<%1$s> %2$s = new LinkedList<%1$s>();", type, varName));
        // cs.add("List<"+type+"> "+varName+" = new LinkedList<"+type+">();");
        for (String s: exprs_result) {
            cs.add(varName+".add("+s+");");
        }
        return cs;
    }

    /**
     * The defaultCase should never be used, each node should have a for...
     * method defined somewhere in this file.
     */
    @Override
    public String defaultCase(Node that) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        throw new RuntimeException( "Warning: no method is defined in " + this.getClass().getName() + " for node " + that.getClass().getName() );
    }

    @Override
    public String forIntLiteralExprOnly(IntLiteralExpr that, Option<String> exprType_result){
        String rVarName = FreshName.getFreshName("intExpr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("IntLiteralExpr %s = new IntLiteralExpr(%s, new java.math.BigInteger(\"%s\"));", rVarName, sVarName, that.getVal().toString() ) );
        return rVarName;
    }

    @Override
    public String forInFixityOnly(InFixity that){
        String rVarName = FreshName.getFreshName("inFixity");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("InFixity %s = new InFixity(%s);", rVarName, sVarName) );
        return rVarName;
    }


    @Override
    public String forAccumulatorOnly(Accumulator that, Option<String> exprType_result,
            List<String> staticArgs_result, String opr_result,
            List<String> gens_result, String body_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("accumulatorName");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String lsName1 = FreshName.getFreshName("ls");
        this.code.addAll(mkList(staticArgs_result, lsName1, "StaticArg"));
        String lsName2 = FreshName.getFreshName("ls");
        this.code.addAll(mkList(gens_result, lsName2, "GeneratorClause"));
        this.code.add(String.format("Accumulator %s = new Accumulator(%s, %b, %s, %s, %s, %s);", rVarName, sVarName, that.isParenthesized(), lsName1, opr_result, lsName2, body_result));
        return rVarName;
    }

    @Override
    public String forMethodInvocationOnly(MethodInvocation that, Option<String> exprType_result, String obj_result, String method_result, List<String> staticArgs_result, String arg_result) {
        String rVarName = FreshName.getFreshName("methodInvocation");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String lsStatic = FreshName.getFreshName("ls");
        this.code.addAll(mkList(staticArgs_result, lsStatic, "StaticArg"));

        this.code.add(String.format("MethodInvocation %s = new MethodInvocation(%s, %b, %s, %s, %s, %s);", rVarName, sVarName, that.isParenthesized(), obj_result, method_result, lsStatic, arg_result ));

        return rVarName;
    }

    @Override
    public String forAnonymousFnNameOnly(AnonymousFnName that,
            Option<String> api_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("anonymousFnName");
        String apiName = this.handleOption(api_result, "APIName");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("AnonymousFnName %s = new AnonymousFnName(%s, %s);", rVarName, sVarName, apiName) );
        return rVarName;
    }

    @Override
    public String forBigFixityOnly(BigFixity that) {
        String rVarName = FreshName.getFreshName("bigFixity");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("BigFixity %s = new BigFixity(%s);", rVarName, sVarName) );
        return rVarName;
    }


    @Override
    public String forPreFixityOnly(PreFixity that){
        String rVarName = FreshName.getFreshName("preFixity");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("PreFixity %s = new PreFixity(%s);", rVarName, sVarName) );
        return rVarName;
    }

    @Override
    public String forMultiFixityOnly(MultiFixity that) {
        String rVarName = FreshName.getFreshName("multiFixity");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("MultiFixity %s = new MultiFixity(%s);", rVarName, sVarName) );
        return rVarName;
    }

    @Override
    public String forChainExprOnly(ChainExpr that, Option<String> exprType_result, String first_result, List<String> links_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("chainExpr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        // this.code.add( String.format("ChainExpr %s = new ChainExpr(%s, %s, new ArrayList<Pair<Op,Expr>>());", rVarName, sVarName, first_result) );
        this.code.add( String.format("ChainExpr %s = new ChainExpr(%s, %s, new ArrayList());", rVarName, sVarName, first_result) );
        return rVarName;
    }

    @Override
    public String forLinkOnly(Link that, String op_result, String expr_result) {
        String rVarName = FreshName.getFreshName("link");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("Link %s = new Link(%s, %s, %s);", rVarName, sVarName, op_result, expr_result));
        return rVarName;
    }

    @Override
    public String forVoidLiteralExprOnly(VoidLiteralExpr that, Option<String> exprType_result){
        String rVarName = FreshName.getFreshName("voidExpr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("VoidLiteralExpr %s = new VoidLiteralExpr(%s);",  rVarName, sVarName ) );
        return rVarName;
    }

    @Override
    public String forAPINameOnly(APIName that, List<String> ids_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("apiName");
        String idName = FreshName.getFreshName("id");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(ids_result, idName, "Id"));
        this.code.add(String.format("APIName %s = new APIName(%s, %s);", rVarName, sVarName, idName));
        return rVarName;
    }

    @Override
    public String forAsExprOnly(AsExpr that, Option<String> exprType_result, String expr_result,
            String type_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("asExpr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("AsExpr %s = new AsExpr(%s,%b,%s,%s);", rVarName, sVarName, that.isParenthesized(), expr_result, type_result) );
        return rVarName;
    }

    @Override
    public String forAsIfExprOnly(AsIfExpr that, Option<String> exprType_result, String expr_result,
            String type_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String varName = FreshName.getFreshName("asExpr");
        String spanName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("AsIfExpr %s = new AsIfExpr(%s,%b,%s,%s);", varName, spanName, that.isParenthesized(), expr_result, type_result) );
        return varName;
    }

    @Override
    public String forBlockOnly(Block that, Option<String> exprType_result, List<String> exprs_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("block");
        String exprsName = FreshName.getFreshName("exprs");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(exprs_result, exprsName, "Expr"));
        this.code.add( String.format("Block %s = new Block(%s, %s);", rVarName, sVarName, exprsName) );
        return rVarName;
    }

    @Override
    public String forDoFrontOnly(DoFront that, Option<String> loc_result,
            String expr_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("doFront");
        String loc = this.handleOption(loc_result, "Expr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("DoFront %s = new DoFront(%s, %s, %b, %s);", rVarName, sVarName, loc, that.isAtomic(), expr_result) );
        return rVarName;
    }

    @Override
    public String forDoOnly(Do that, Option<String> exprType_result, List<String> fronts_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("aDo");
        String exprsName = FreshName.getFreshName("exprs");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(fronts_result, exprsName, "DoFront"));
        this.code.add( String.format("Do %s = new Do(%s, %s);", rVarName, sVarName, exprsName) );
        return rVarName;
    }

    @Override
    public String forFnExprOnly(FnExpr that, Option<String> exprType_result, String name_result,
            List<String> staticParams_result, List<String> params_result,
            Option<String> returnType_result, Option<String> where_result,
            Option<List<String>> throwsClause_result, String body_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String varName = FreshName.getFreshName("fnExpr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String lsName1 = FreshName.getFreshName("ls");
        this.code.addAll(mkList(staticParams_result, lsName1, "StaticParam"));
        String lsName2 = FreshName.getFreshName("ls");
        this.code.addAll(mkList(params_result, lsName2, "Param"));
        String opt1 = this.handleOption(returnType_result, "Type");
        String opt2 = this.handleOption(where_result, "WhereClause");
        String opt3 = this.handleOptionList(throwsClause_result, "BaseType");
        this.code.add(String.format("FnExpr %s = new FnExpr(%s, %b, %s, %s, %s, %s, %s, %s, %s);", varName, sVarName, that.isParenthesized(), name_result, lsName1, lsName2, opt1, opt2, opt3, body_result) );
        return varName;
    }


    @Override
    public String forFnRefOnly(FnRef that, Option<String> exprType_result,
            String originalName_result,
            List<String> fns_result,
            List<String> staticArgs_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("fn");
        String fns = FreshName.getFreshName("ls");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(fns_result, fns, "Id"));
        String staticArgs = FreshName.getFreshName("ls");
        this.code.addAll(mkList(staticArgs_result, staticArgs, "StaticArg"));

        this.code.add( String.format( "FnRef %s = new FnRef(%s, %b, %s, %s, %s);", rVarName, sVarName, that.isParenthesized(), originalName_result, fns, staticArgs) );

        return rVarName;
    }

    @Override
    public String forId(Id that) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("id");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("Id %s = new Id(%s, \"%s\");", rVarName, sVarName, that.getText()) );
        return rVarName;
    }

    @Override
    public String forIdOnly(Id that, Option<String> api_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("qIdName");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String api = this.handleOption(api_result, "APIName");
        this.code.add(String.format("Id %s = new Id(%s, %s);", rVarName, sVarName, api) );
        return rVarName;
    }

    @Override
    public String forIfOnly(If that, Option<String> exprType_result, List<String> clauses_result,
            Option<String> elseClause_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("ifExpr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String clauses = FreshName.getFreshName("ls");
        this.code.addAll(mkList(clauses_result, clauses, "IfClause"));
        String elseClause = this.handleOption(elseClause_result, "Block");
        this.code.add( String.format("If %s = new If(%s,%s,%s,%s);", rVarName, sVarName, that.isParenthesized(), clauses, elseClause) );
        return rVarName;
    }

    @Override
    public String forIfClauseOnly(IfClause that, String test_result,
            String body_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("ifClause");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("IfClause %s = new IfClause(%s,%s,%s);", rVarName, sVarName, test_result, body_result) );
        return rVarName;
    }

    @Override
    public String forGeneratorClauseOnly(GeneratorClause that,
            List<String> bind_result, String init_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("generatorClause");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String clauses = FreshName.getFreshName("ls");
        this.code.addAll(mkList(bind_result, clauses, "Id"));
        this.code.add( String.format("GeneratorClause %s = new GeneratorClause(%s,%s,%s);", rVarName, sVarName, clauses, init_result) );
        return rVarName;
    }

    @Override
    public String forVarTypeOnly(VarType that, String name_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("idType");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("VarType %s = new VarType(%s, %s);", rVarName, sVarName, name_result) );
        return rVarName;
    }

    @Override
    public String forLocalVarDeclOnly(LocalVarDecl that, Option<String> exprType_result,
            List<String> body_result, List<String> lhs_result,
            Option<String> rhs_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("localVarDecl");

        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);

        String body = FreshName.getFreshName("body");
        this.code.addAll(mkList(body_result, body, "Expr"));

        String lhs = FreshName.getFreshName("lhs");
        this.code.addAll(mkList(lhs_result, lhs, "LValue"));

        String rhs = this.handleOption(rhs_result, "Expr");

        this.code.add( String.format("LocalVarDecl %s = new LocalVarDecl(%s, %s, %s, %s);", rVarName, sVarName, body, lhs, rhs) );
        return rVarName;
    }

    @Override
    public String forLooseJuxtOnly(LooseJuxt that, Option<String> exprType_result, String multiJuxt_result,
            String infixJuxt_result,
            List<String> exprs_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("lj");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String args = FreshName.getFreshName("ls");
        this.code.addAll(mkList(exprs_result, args, "Expr"));
        this.code.add( String.format("LooseJuxt %s = new LooseJuxt(%s, %b, %s, %s, %s);", rVarName, sVarName, that.isParenthesized(), multiJuxt_result, infixJuxt_result, args) );
        return rVarName;
    }

    @Override
    public String forLValueBindOnly(LValueBind that, String name_result,
            Option<String> type_result, List<String> mods_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("lValueBind");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String type = this.handleOption(type_result, "Type");
        String mods = FreshName.getFreshName("mods");
        this.code.addAll(mkList(mods_result, mods, "Modifier"));

        this.code.add(String.format("LValueBind %s = new LValueBind(%s, %s, %s, %s, %b);", rVarName, sVarName, name_result, type, mods, that.isMutable()) );
        return rVarName;
    }


    @Override
	public String forMathPrimaryOnly(MathPrimary that, Option<String> exprType_result, String multiJuxt_result,
			String infixJuxt_result, String front_result,
			List<String> rest_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("mathPrimary");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String rest = FreshName.getFreshName("rest");
        this.code.addAll(mkList(rest_result, rest, "MathItem"));
        this.code.add(String.format("MathPrimary %s = new MathPrimary(%s, %s, %s);", rVarName, sVarName, front_result, rest) );
        return rVarName;
	}

	@Override
    public String forNoFixityOnly(NoFixity that) {
        String varName = FreshName.getFreshName("noFixity");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add(String.format("NoFixity %s = new NoFixity(%s);", varName, sVarName));
        return varName;
    }

    @Override
    public String forNormalParamOnly(NormalParam that,
            List<String> mods_result, String name_result,
            Option<String> type_result, Option<String> defaultExpr_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String varName = FreshName.getFreshName("normalParam");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String lsName = FreshName.getFreshName("ls");
        this.code.addAll(mkList(mods_result, lsName, "Modifier"));
        String typeName = this.handleOption(type_result, "Type");
        String defaultExprName = this.handleOption(defaultExpr_result, "Expr");
        this.code.add(String.format("NormalParam %s = new NormalParam(%s, %s, %s, %s, %s);", varName, sVarName, lsName, name_result, typeName, defaultExprName));
        return varName;
    }

    @Override
    public String forNonParenthesisDelimitedMIOnly(
            NonParenthesisDelimitedMI that, String expr_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String varName = FreshName.getFreshName("nonParenDelimitedMI");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add(String.format("NonParenthesisDelimitedMI %s = new NonParenthesisDelimitedMI(%s, %s);", varName, sVarName, expr_result));
        return varName;
    }

    @Override
    public String forStringLiteralExpr(StringLiteralExpr that) {
        String varName = FreshName.getFreshName("s");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add(String.format("StringLiteralExpr %s = new StringLiteralExpr(%s, %b, \"%s\");", varName, sVarName, that.isParenthesized(), escapeString(that.getText())));
        return varName;
    }

    /* Escape \'s and "'s
     */
    private String escapeString(String s){
        return s.replaceAll("\\\\","\\\\\\\\").replaceAll("\"","\\\\\"");
    }

    @Override
    public String forCharLiteralExpr(CharLiteralExpr that){
        String varName = FreshName.getFreshName("charLiteral");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add(String.format("CharLiteralExpr %s = new CharLiteralExpr(%s, %b, \"%s\", %d);", varName, sVarName, that.isParenthesized(), that.getText().replaceAll("\"","\\\\\""), that.getVal() ));
        return varName;
    }

    @Override
    public String forTightJuxtOnly(TightJuxt that, Option<String> exprType_result, String multiJuxt_result,
            String infixJuxt_result,
            List<String> exprs_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String varName = FreshName.getFreshName("ls");
        String rVarName = FreshName.getFreshName("tj");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(exprs_result, varName, "Expr"));
        this.code.add( String.format("TightJuxt %s = new TightJuxt(%s, %b, %s, %s, %s);", rVarName, sVarName, that.isParenthesized(), multiJuxt_result, infixJuxt_result, varName) );
        return rVarName;
    }

    @Override
    public String forTraitTypeOnly(TraitType that, String name_result,
            List<String> args_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String varName = FreshName.getFreshName("traitType");
        String lsVarName = FreshName.getFreshName("ls");
        String spanName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(args_result, lsVarName, "StaticArg"));
        this.code.add( String.format("TraitType %s = new TraitType(%s, %b, %s, %s);", varName, spanName, that.isParenthesized(), name_result, lsVarName) );
        return varName;
    }

    @Override
    public String forTupleExprOnly(TupleExpr that, Option<String> exprType_result, List<String> exprs_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String varName = FreshName.getFreshName("ls");
        String rVarName = FreshName.getFreshName("tupleExpr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(exprs_result, varName, "Expr"));
        this.code.add( String.format("TupleExpr %s = new TupleExpr(%s, %s, %s);", rVarName, sVarName, that.isParenthesized(), varName) );
        return rVarName;
    }

    @Override
    public String forTypeArgOnly(TypeArg that, String type_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("typeArg");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("TypeArg %s = new TypeArg(%s, %s);", rVarName, sVarName, type_result ) );
        return rVarName;
    }

    @Override
    public String forVarRefOnly(VarRef that, Option<String> exprType_result, String var_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String varName = FreshName.getFreshName("varRef");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("VarRef %s = new VarRef(%s, %s);", varName, sVarName, var_result) );
        return varName;
    }

    @Override
    public String forVarRef(VarRef that) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        Id id = that.getVar();
        //TODO: is it ok to assume that it is not qualified?
        if (this.syntaxDeclEnv.contains(that.getVar())) {
            /* TODO: this same sort of code is used in handleTemplateGap.
             * It should probably be abstracted and turned into a function.
             */
            /*
            if (this.syntaxDeclEnv.isRepeat(id)) {
                return "(OpExpr)"+ActionCreater.BOUND_VARIABLES+".get(\""+id.getText()+"\")";
            }
            // return that.getVar().toString();
            // return String.format("(%s) %s.get(\"%s\")", GrammarEnv.getType(), ActionCreater.BOUND_VARIABLES, that.getVar().toString() );
            Id nonterminal = syntaxDeclEnv.getNonterminalName(id);
            Debug.debug( 4, String.format("%s is a non-terminal of type %s", id.getText(), lookupAstNode(nonterminal) ) );
            return String.format("(%s) %s.get(\"%s\")", lookupAstNode(nonterminal), ActionCreater.BOUND_VARIABLES, id.getText() );
             */
            return extractVar(id);

        }
        return super.forVarRef(that);
    }

    @Override
    public String forWhereClauseOnly(WhereClause that,
            List<String> bindings_result, List<String> constraints_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String varName = FreshName.getFreshName("whereClause");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String lsName1 = FreshName.getFreshName("ls");
        this.code.addAll(mkList(bindings_result, lsName1, "WhereBinding"));
        String lsName2 = FreshName.getFreshName("ls");
        this.code.addAll(mkList(constraints_result, lsName2, "WhereConstraint"));
        this.code.add(String.format("WhereClause %s = new WhereClause(%s, %s, %s);", varName, sVarName, lsName1, lsName2));
        return varName;
    }

    @Override
    public String forEnclosingOnly(Enclosing that, Option<String> api_result, String in_open_result, String in_close_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("enclosing");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String apiVarName = this.handleOption(api_result, "APIName");
        this.code.add( String.format("Enclosing %s = new Enclosing(%s, %s, %s, %s);", rVarName, sVarName, apiVarName, in_open_result, in_close_result) );
        return rVarName;
    }

    @Override
    public String forEnclosingFixity(EnclosingFixity that) {
        String rVarName = FreshName.getFreshName("enclosingFixity");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("EnclosingFixity %s = new EnclosingFixity(%s);", rVarName, sVarName) );
        return rVarName;
    }

    @Override
    public String forOpOnly(Op that, Option<String> api_result,
            Option<String> fixity_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("op");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String apiVarName = this.handleOption(api_result, "APIName");
        String fixityVarName = this.handleOption(fixity_result, "Fixity");
        this.code.add( String.format("Op %s = new Op(%s, %s, \"%s\", %s);", rVarName, sVarName, apiVarName, that.getText(), fixityVarName) );
        return rVarName;
    }

    public static String getSpan(AbstractNode that, List<String> code) {
        Span span = that.getSpan();
        String rVarName = FreshName.getFreshName("span");
        String slStartVarName = FreshName.getFreshName("sl");
        String slEndVarName = FreshName.getFreshName("sl");
        String file = span.getBegin().getFileName();
        int startLine = span.getBegin().getLine();
        int startColumn = span.getBegin().column();
        int startOffset = span.getBegin().getOffset();
        code.add( String.format("SourceLocRats %s = new SourceLocRats(\"%s\", %s, %s, %s);", slStartVarName, file, startLine, startColumn, startOffset) );
        int endLine = span.getBegin().getLine();
        int endColumn = span.getBegin().column();
        int endOffset = span.getBegin().getOffset();
        code.add( String.format("SourceLocRats %s = new SourceLocRats(\"%s\", %s, %s, %s);", slEndVarName, file, endLine, endColumn, endOffset) );
        code.add( String.format( "Span %s = new Span(%s,%s);", rVarName, slStartVarName, slEndVarName) );
        return rVarName;
    }

    private String handleOption(Option<String> o, String type) {
        String rVarName = FreshName.getFreshName("option");
        String rhs = "Option.<"+type+">none()";
        if (o.isSome()) {
            rhs = "Option.<"+type+">some("+o.unwrap()+")";
        }
        this.code.add("Option<"+type+"> "+rVarName +" = " +rhs+";");
        return rVarName;
    }

    private String handleOptionList(Option<List<String>> o, String type) {
        String rVarName = FreshName.getFreshName("option");
        String rhs = "Option.<List<"+type+">>none()";
        if (o.isSome()) {
            String lsName = FreshName.getFreshName("ls");
            this.code.addAll(mkList(o.unwrap(), lsName , type));
            rhs = "Option.<List<"+type+">>some("+lsName+")";
        }
        this.code.add("Option<List<"+type+">> "+rVarName +" = " +rhs+";");
        return rVarName;
    }

    private String handleVariables( Map<String,Object> variables ){
        String rVarName = FreshName.getFreshName("variables");
        String type = "java.util.HashMap<String,Object>";

        this.code.add(String.format("%s %s = new %s();", type, rVarName, type));
        for ( Map.Entry<String,Object> entry : variables.entrySet() ){
            AbstractNode node = (AbstractNode) entry.getValue();
            this.code.add(String.format("%s.put(\"%s\",%s);", rVarName, entry.getKey(), node.accept(this) ));
        }

        return rVarName;
    }

    @Override public String defaultTransformationNodeCase(_SyntaxTransformation that){
        String rVarName = FreshName.getFreshName("transform");
        String sVarName = JavaAstPrettyPrinter.getSpan((AbstractNode) that, this.code);
        String type = that.getClass().getName();

        String variables = handleVariables(that.getVariables());

        this.code.add(String.format("%s %s = new %s(%s,\"%s\",%s);", type, rVarName, type, sVarName, that.getSyntaxTransformer(), variables));
        return rVarName;
    }

    @Override
	public String forAmbiguousMultifixOpExprOnly(AmbiguousMultifixOpExpr that, Option<String> exprType_result,
			String infix_op_result, String multifix_op_result,
			List<String> args_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("opExpr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String argsVarName = FreshName.getFreshName("ls");
        this.code.addAll(mkList(args_result, argsVarName, "Expr"));
        this.code.add( String.format("AmbiguousMultifixOpExpr %s = new AmbiguousMultifixOpExpr(%s, %b, %s, %s, %s);", rVarName, sVarName, that.isParenthesized(), infix_op_result, multifix_op_result, argsVarName) );
        return rVarName;
	}

	@Override
    public String forOpExprOnly(OpExpr that, Option<String> exprType_result, String in_op_result, List<String> args_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("opExpr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String argsVarName = FreshName.getFreshName("ls");
        this.code.addAll(mkList(args_result, argsVarName, "Expr"));
        this.code.add( String.format("OpExpr %s = new OpExpr(%s, %b, %s, %s);", rVarName, sVarName, that.isParenthesized(), in_op_result, argsVarName) );
        return rVarName;
    }

    @Override
    public String forOpRefOnly(OpRef that, Option<String> exprType_result,
            String originalName_result,
            List<String> ops_result,
            List<String> staticArgs_result) {
        if (that instanceof TemplateGap) {
            return handleTemplateGap( (TemplateGap) that);
        }
        String rVarName = FreshName.getFreshName("opRef");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String opsVarName = FreshName.getFreshName("ls");
        this.code.addAll(mkList(ops_result, opsVarName, "OpName"));
        String staticArgsVarName = FreshName.getFreshName("ls");
        this.code.addAll(mkList(staticArgs_result, staticArgsVarName, "StaticArg"));
        this.code.add( String.format("OpRef %s = new OpRef(%s, %b, %s, %s);",
                rVarName, sVarName, that.isParenthesized(),
                originalName_result,
                opsVarName, staticArgsVarName) );
        return rVarName;
    }

    private String extractVar(Id id){
        if (this.syntaxDeclEnv.isRepeat(id)) {
            Debug.debug( Debug.Type.SYNTAX, 4, id.getText(), " is a repeat symbol" );
            return "(OpExpr)"+ActionCreater.BOUND_VARIABLES+".get(\""+id.getText()+"\")";
        }
        if (this.syntaxDeclEnv.isOption(id)) {
            Debug.debug( Debug.Type.SYNTAX, 4, id.getText(), " is an optional symbol" );
            return "(Expr)"+ActionCreater.BOUND_VARIABLES+".get(\""+id.getText()+"\")";
        }
        if (this.syntaxDeclEnv.isCharacterClass(id)) {
            Debug.debug( Debug.Type.SYNTAX, 4, id.getText(), " is a character class" );
            return "(StringLiteralExpr)"+ActionCreater.BOUND_VARIABLES+".get(\""+id.getText()+"\")";
        }
        if (this.syntaxDeclEnv.isAnyChar(id)) {
            Debug.debug( Debug.Type.SYNTAX, 4, id.getText(), " is an any char" );
            return "(StringLiteralExpr)"+ActionCreater.BOUND_VARIABLES+".get(\""+id.getText()+"\")";
        }
        if (this.syntaxDeclEnv.isNonterminal(id)) {
            Type type = syntaxDeclEnv.getType(id);
            String stype = new FortressTypeToJavaType().analyze(type);
            Debug.debug( Debug.Type.SYNTAX, 4, id.getText(), " is a non-terminal of type ", stype );
            return String.format("(%s) %s.get(\"%s\")", stype, ActionCreater.BOUND_VARIABLES, id.getText() );
        }
        NI.nyi();
        /* will never get here */
        return null;
    }

    private String normalGap(TemplateGap t){
        return extractVar(t.getGapId());
    }

    @Override
    public String defaultTemplateGapCase(TemplateGap t) {
        return handleTemplateGap(t);
    }

    private String handleTemplateGap(TemplateGap t) {
        // Is the template t an instance of a template application
        // or a template variable?
        Id id = t.getGapId();
        if (!t.getTemplateParams().isEmpty()) {
            Debug.debug(Debug.Type.SYNTAX, "The gap has parameters: ", t.getTemplateParams() );
            return parameterizedGap(t);
        }
        if (this.syntaxDeclEnv.contains(id)) {
            return normalGap(t);
        }
        String idVarName = FreshName.getFreshName("id");
        this.code.add("Id "+idVarName+" = new Id(\""+id+"\");");
        String params = FreshName.getFreshName("params");
        this.code.add("List<Id> "+params+" = new LinkedList<Id>();");
        String sVarName = JavaAstPrettyPrinter.getSpan((AbstractNode) t, this.code);
        String typeName = syntaxDeclEnv.getMemberEnv().getParameterType(id).accept(new BaseTypeCollector());
        return SyntaxAbstractionUtil.makeTemplateGap(code, new LinkedList<Integer>(), typeName, idVarName, params, sVarName);

    }


    private String parameterizedGap(TemplateGap t){
        Id id = t.getGapId();

        if (this.syntaxDeclEnv.getMemberEnv().isParameter(id) ||
            this.syntaxDeclEnv.isAnyChar(id) ||
            this.syntaxDeclEnv.isCharacterClass(id)) {
            throw new RuntimeException("Should not occur, a static check should have caught this: "+id);
        }
            MemberEnv mEnv = GrammarEnv.getMemberEnv(this.syntaxDeclEnv.getNonterminalName(id));

            String paramEnv = FreshName.getFreshName("paramEnv");
            List<String> ls = new LinkedList<String>();
            ls.add("final Map<String, AbstractNode> "+paramEnv +" = new HashMap<String, AbstractNode>();");
            /* dead code probably
            for (int inx=0;inx<t.getTemplateParams().size();inx++) {
                Id formalParam = mEnv.getParameter(inx);
                Id actualParam = t.getTemplateParams().get(inx);
                String var = actualParam.toString();
                Option<String> op = ActionCreaterUtil.FortressWrapper(actualParam, this.syntaxDeclEnv, this.code, ls, new LinkedList<Integer>());
                if (op.isSome()) {
                    var = op.unwrap();
                }
                ls.add(paramEnv+".put(\""+formalParam +"\", "+var+");");
            }
            */
            this.code.addAll(ls);
            return addParamHandlers(id, mEnv, t.getTemplateParams(), paramEnv, this.code);
    }

    private String addParamHandlers(Id id, MemberEnv env, List<Id> params, String paramEnv,
            List<String> code) {
        Type type = this.syntaxDeclEnv.getType(id);
        String typeName = new FortressTypeToJavaType().analyze(type);

        String rVarName = FreshName.getFreshName("visitor");
        this.code.add(typeName+" "+rVarName+" = ("+typeName+") "+id+".accept(new NodeUpdateVisitor() {");

        Set<String> templateTypes = new HashSet<String>();
        for (int inx=0;inx<params.size();inx++) {
            Id formalParam = env.getParameter(inx);
            BaseType nonterminalType = env.getParameterType(formalParam);
            String templateTypeName = nonterminalType.accept(new BaseTypeCollector());
            String className = String.format("TemplateGap%s", templateTypeName);
            templateTypes.add(className);
        }

        for (String className: templateTypes) {
            addMethod(className, code, paramEnv);
        }
        this.code.add("});");
        return rVarName;
    }

    private void addMethod(String className, List<String> code, String paramEnv) {
        code.add("  @Override");
        code.add("  public Node for"+className +"("+className+" that) {");
        code.add("    Node n = null;");
        code.add("    String id = that.getGapId().getText();");
        //        this.code.add("    System.err.println(\"Looking up: \"+id);");
        code.add("    if (null != (n = "+paramEnv+".get(id))) {");
        //        this.code.add("      System.err.println(\"Subs\");");
        code.add("      return n;");
        code.add("    }");
        //        this.code.add("    System.err.println(\"No subs\");");
        code.add("    throw new RuntimeException(\"Undefined variable: \"+id+\"in template: "+className+"\");");
        code.add("  }");
    }

}
