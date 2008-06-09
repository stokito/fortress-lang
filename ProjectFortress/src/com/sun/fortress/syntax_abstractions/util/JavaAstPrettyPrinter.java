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
import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.AsExpr;
import com.sun.fortress.nodes.AsIfExpr;
import com.sun.fortress.nodes.Block;
import com.sun.fortress.nodes.ChainExpr;
import com.sun.fortress.nodes.Do;
import com.sun.fortress.nodes.DoFront;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.EnclosingFixity;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.GeneratorClause;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.If;
import com.sun.fortress.nodes.IfClause;
import com.sun.fortress.nodes.IntLiteralExpr;
import com.sun.fortress.nodes.InFixity;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.LooseJuxt;
import com.sun.fortress.nodes.MultiFixity;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpExpr;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.PreFixity;
import com.sun.fortress.nodes.StringLiteralExpr;
import com.sun.fortress.nodes.TemplateGap;
import com.sun.fortress.nodes.TemplateGapExpr;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TupleExpr;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.VoidLiteralExpr;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes_util.SourceLoc;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.syntax_abstractions.environments.GrammarEnv;
import com.sun.fortress.syntax_abstractions.environments.MemberEnv;
import com.sun.fortress.syntax_abstractions.environments.SyntaxDeclEnv;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.tuple.Option;

public class JavaAstPrettyPrinter extends NodeDepthFirstVisitor<String> {

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
        throw new RuntimeException( "Warning: no method is defined in " + this.getClass().getName() + " for node " + that.getClass().getName() );
        /*
        System.out.println( "Warning! No case implemented in " + this.getClass().getName() + " for node " + that.getClass().getName() );
        String rVarName = FreshName.getFreshName("def");
        this.code.add("StringLiteralExpr "+rVarName+" = new StringLiteralExpr(\""+that.getClass()+"\");");
        return rVarName;
        */
    }

    @Override
    public String forIntLiteralExprOnly(IntLiteralExpr that){
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
    public String forChainExprOnly(ChainExpr that, String first_result) {
	    String rVarName = FreshName.getFreshName("chainExpr");
	    String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
	    // this.code.add( String.format("ChainExpr %s = new ChainExpr(%s, %s, new ArrayList<Pair<Op,Expr>>());", rVarName, sVarName, first_result) );
	    this.code.add( String.format("ChainExpr %s = new ChainExpr(%s, %s, new ArrayList());", rVarName, sVarName, first_result) );
	    return rVarName;
    }

    @Override 
    public String forVoidLiteralExprOnly(VoidLiteralExpr that){
        String rVarName = FreshName.getFreshName("voidExpr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("VoidLiteralExpr %s = new VoidLiteralExpr(%s);",  rVarName, sVarName ) );
        return rVarName;
    }

    @Override
    public String forAPINameOnly(APIName that, List<String> ids_result) {
        String rVarName = FreshName.getFreshName("apiName");
        String idName = FreshName.getFreshName("id");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(ids_result, idName, "Id"));
        this.code.add(String.format("APIName %s = new APIName(%s, %s);", rVarName, sVarName, idName));
        return rVarName;
    }

    @Override
    public String forAsExprOnly(AsExpr that, String expr_result,
            String type_result) {
        String rVarName = FreshName.getFreshName("asExpr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("AsExpr %s = new AsExpr(%s,%b,%s,%s);", rVarName, sVarName, that.isParenthesized(), expr_result, type_result) );
        return rVarName;
    }

    @Override
    public String forAsIfExprOnly(AsIfExpr that, String expr_result,
            String type_result) {
        String varName = FreshName.getFreshName("asExpr");
        String spanName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("AsIfExpr %s = new AsIfExpr(%s,%b,%s,%s);", varName, spanName, that.isParenthesized(), expr_result, type_result) );
        return varName;
    }

    @Override
    public String forBlockOnly(Block that, List<String> exprs_result) {
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
        String rVarName = FreshName.getFreshName("doFront");
        String loc = this.handleOption(loc_result, "Expr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("DoFront %s = new DoFront(%s, %s, %b, %s);", rVarName, sVarName, loc, that.isAtomic(), expr_result) );
        return rVarName;
    }

    @Override
    public String forDoOnly(Do that, List<String> fronts_result) {
        String rVarName = FreshName.getFreshName("aDo");
        String exprsName = FreshName.getFreshName("exprs");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(fronts_result, exprsName, "DoFront"));
        this.code.add( String.format("Do %s = new Do(%s, %s);", rVarName, sVarName, exprsName) );
        return rVarName;
    }

    @Override
    public String forFnRefOnly(FnRef that, List<String> fns_result,
            List<String> staticArgs_result) {
        String rVarName = FreshName.getFreshName("fn");
        String fns = FreshName.getFreshName("ls");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(fns_result, fns, "Id"));
        String staticArgs = FreshName.getFreshName("ls");
        this.code.addAll(mkList(staticArgs_result, staticArgs, "StaticArg"));

        this.code.add( String.format( "FnRef %s = new FnRef(%s, %s, %s, %s);", rVarName, sVarName, that.isParenthesized(), fns, staticArgs) );

        return rVarName;
    }

    @Override
    public String forId(Id that) {
        String rVarName = FreshName.getFreshName("id");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("Id %s = new Id(%s, \"%s\");", rVarName, sVarName, that.getText()) );
        return rVarName;
    }

    @Override
    public String forIdOnly(Id that, Option<String> api_result) {
        String rVarName = FreshName.getFreshName("qIdName");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String api = this.handleOption(api_result, "APIName");
        this.code.add(String.format("Id %s = new Id(%s, %s);", rVarName, sVarName, api) );
        return rVarName;
    }
    
    @Override
    public String forIfOnly(If that, List<String> clauses_result,
            Option<String> elseClause_result) {
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
        String rVarName = FreshName.getFreshName("ifClause");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("IfClause %s = new IfClause(%s,%s,%s);", rVarName, sVarName, test_result, body_result) );
        return rVarName;
    }

    @Override
    public String forGeneratorClauseOnly(GeneratorClause that,
            List<String> bind_result, String init_result) {
        String rVarName = FreshName.getFreshName("generatorClause");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String clauses = FreshName.getFreshName("ls");
        this.code.addAll(mkList(bind_result, clauses, "Id"));   
        this.code.add( String.format("GeneratorClause %s = new GeneratorClause(%s,%s,%s);", rVarName, sVarName, clauses, init_result) );
        return rVarName;
    }

    @Override
        public String forVarTypeOnly(VarType that, String name_result) {
        String rVarName = FreshName.getFreshName("idType");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("VarType %s = new VarType(%s, %s);", rVarName, sVarName, name_result) );
        return rVarName;
    }

    @Override
    public String forLocalVarDeclOnly(LocalVarDecl that,
            List<String> body_result, List<String> lhs_result,
            Option<String> rhs_result) {
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
    public String forLooseJuxtOnly(LooseJuxt that, List<String> exprs_result) {
        String rVarName = FreshName.getFreshName("lj");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String args = FreshName.getFreshName("ls");
        this.code.addAll(mkList(exprs_result, args, "Expr"));
        this.code.add( String.format("LooseJuxt %s = new LooseJuxt(%s, %b, %s);", rVarName, sVarName, that.isParenthesized(), args) );
        return rVarName;
    }

    @Override
    public String forLValueBindOnly(LValueBind that, String name_result,
            Option<String> type_result, List<String> mods_result) {
        String rVarName = FreshName.getFreshName("lValueBind");

        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        
        String type = this.handleOption(type_result, "Type");

        String mods = FreshName.getFreshName("mods");
        this.code.addAll(mkList(mods_result, mods, "Modifier"));

        this.code.add(String.format("LValueBind %s = new LValueBind(%s, %s, %s, %s, %b);", rVarName, sVarName, name_result, mods, that.isMutable()) );
        return rVarName;
    }

    @Override
    public String forStringLiteralExpr(StringLiteralExpr that) {
        String varName = FreshName.getFreshName("s");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("StringLiteralExpr %s = new StringLiteralExpr(%s, \"%s\");", varName, sVarName, that.getText().replaceAll("\"","\\\\\"")) );
        return varName;
    }

    @Override
    public String forTightJuxtOnly(TightJuxt that, List<String> exprs_result) {
        String varName = FreshName.getFreshName("ls");
        String rVarName = FreshName.getFreshName("tj");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(exprs_result, varName, "Expr"));
        this.code.add( String.format("TightJuxt %s = new TightJuxt(%s, %b, %s);", rVarName, sVarName, that.isParenthesized(), varName) );
        return rVarName;
    }

    @Override
    public String forTraitTypeOnly(TraitType that, String name_result,
            List<String> args_result) {
        String varName = FreshName.getFreshName("traitType");
        String lsVarName = FreshName.getFreshName("ls");
        String spanName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(args_result, lsVarName, "StaticArg"));
        this.code.add( String.format("TraitType %s = new TraitType(%s, %b, %s, %s);", varName, spanName, that.isParenthesized(), name_result, lsVarName) );
        return varName;
    }

    @Override
    public String forTupleExprOnly(TupleExpr that, List<String> exprs_result) {
        String varName = FreshName.getFreshName("ls");
        String rVarName = FreshName.getFreshName("tupleExpr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(exprs_result, varName, "Expr"));
        this.code.add( String.format("TupleExpr %s = new TupleExpr(%s, %s, %s);", rVarName, sVarName, that.isParenthesized(), varName) );
        return rVarName;
    }

    @Override
    public String forTypeArgOnly(TypeArg that, String type_result) {
        String rVarName = FreshName.getFreshName("typeArg");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("TypeArg %s = new TypeArg(%s, %s);", rVarName, sVarName, type_result ) );
        return rVarName;
    }

    @Override
    public String forVarRefOnly(VarRef that, String var_result) {
        String varName = FreshName.getFreshName("varRef");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add( String.format("VarRef %s = new VarRef(%s, %s);", varName, sVarName, var_result) );
        return varName;
    }

    @Override
    public String forVarRef(VarRef that) {
        Id id = that.getVar();
        //TODO: is it ok to assume that it is not qualified?
        if (this.syntaxDeclEnv.contains(that.getVar())) {
            /* TODO: this same sort of code is used in handleTemplateGap.
             * It should probably be abstracted and turned into a function.
             */
            if (this.syntaxDeclEnv.isRepeat(id)) {
                return "(OpExpr)"+ActionCreater.BOUND_VARIABLES+".get(\""+id.getText()+"\")";  
            }
            // return that.getVar().toString();
            return String.format("(Expr) %s.get(\"%s\")", ActionCreater.BOUND_VARIABLES, that.getVar().toString() );
        }
        return super.forVarRef(that);
    }
    
    @Override
    public String forEnclosingOnly(Enclosing that, Option<String> api_result, String in_open_result, String in_close_result) {
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

    @Override
    public String forOpExprOnly(OpExpr that, String in_op_result, List<String> args_result) {
        String rVarName = FreshName.getFreshName("opExpr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String argsVarName = FreshName.getFreshName("ls");
        this.code.addAll(mkList(args_result, argsVarName, "Expr"));
        this.code.add( String.format("OpExpr %s = new OpExpr(%s, %b, %s, %s);", rVarName, sVarName, that.isParenthesized(), in_op_result, argsVarName) );
        return rVarName;
    }

    @Override
    public String forOpRefOnly(OpRef that, List<String> ops_result, List<String> staticArgs_result) {
        String rVarName = FreshName.getFreshName("opRef");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String opsVarName = FreshName.getFreshName("ls");
        this.code.addAll(mkList(ops_result, opsVarName, "OpName"));
        String staticArgsVarName = FreshName.getFreshName("ls");
        this.code.addAll(mkList(staticArgs_result, staticArgsVarName, "StaticArg"));
        this.code.add( String.format("OpRef %s = new OpRef(%s, %b, %s, %s);", rVarName, sVarName, that.isParenthesized(), opsVarName, staticArgsVarName) );
        return rVarName;
    }

    @Override
    public String forTemplateGapExpr(TemplateGapExpr that) {
        return handleTemplateGap(that);
    }

    private String lookupAstNode(Id id){
        if ( syntaxDeclEnv.getNonterminalName(id) == null ){
            /* FIXME ?? */
            return "StringLiteralExpr";
        } else {
            return GrammarEnv.getMemberEnv(syntaxDeclEnv.getNonterminalName(id)).getType().toString();
        }
    }

    private String handleTemplateGap(TemplateGap t) {
        // Is the template t an instance of a template application
        // or a template variable?
        Id id = t.getId();
        if (!t.getParams().isEmpty()) {
            String className = t.getClass().getSimpleName();

            String paramEnv = FreshName.getFreshName("paramEnv");
            this.code.add("final Map<String, AbstractNode> "+paramEnv +" = new HashMap<String, AbstractNode>();");

            Id memberName = this.syntaxDeclEnv.getNonterminalName(id);
            
            if (!GrammarEnv.contains(memberName)) {
                throw new RuntimeException("Grammar environment does not contain identifier: "+id);
            }
            MemberEnv nEnv = GrammarEnv.getMemberEnv(memberName);
                        
            for (int inx=0;inx<t.getParams().size();inx++) {
                this.code.add(paramEnv+".put(\""+nEnv.getParameter(inx)+"\", "+t.getParams().get(inx)+");");
            }

            Type type = this.syntaxDeclEnv.getType(id);
            String typeName = type.toString();
            
            String rVarName = FreshName.getFreshName("rs");            
            this.code.add(typeName+" "+rVarName+" = ("+typeName+") "+id+".accept(new NodeUpdateVisitor() {");
            this.code.add("  @Override");
            this.code.add("  public Node for"+className +"("+className+" that) {");
            this.code.add("    Node n = null;");
            this.code.add("    String id = that.getId().getText();");
//            this.code.add("    System.err.println(\"Looking up: \"+id);");
            this.code.add("    if (null != (n = "+paramEnv+".get(id))) {");
//            this.code.add("      System.err.println(\"Subs\");");
            this.code.add("      return n;");
            this.code.add("    }");
//            this.code.add("    System.err.println(\"No subs\");");
            this.code.add("    return super.for"+className+"(that);");
            this.code.add("  }");
            this.code.add("});");
            return rVarName;
        }
        if (this.syntaxDeclEnv.contains(id)) {
            if (this.syntaxDeclEnv.isRepeat(id)) {
                return "(OpExpr)"+ActionCreater.BOUND_VARIABLES+".get(\""+id.getText()+"\")";  
            }
            if (this.syntaxDeclEnv.isOption(id)) {
                return "(Expr)"+ActionCreater.BOUND_VARIABLES+".get(\""+id.getText()+"\")";  
            }
            if (this.syntaxDeclEnv.isCharacterClass(id)) {
                return "(StringLiteralExpr)"+ActionCreater.BOUND_VARIABLES+".get(\""+id.getText()+"\")";  
            }
            if (this.syntaxDeclEnv.isAnyChar(id)) {
                return "(StringLiteralExpr)"+ActionCreater.BOUND_VARIABLES+".get(\""+id.getText()+"\")";
            }
            if (this.syntaxDeclEnv.isNonterminal(id)) {
                // return id.getText();
                return String.format("(%s) %s.get(\"%s\")", lookupAstNode(id), ActionCreater.BOUND_VARIABLES, id.getText() );
            }
            NI.nyi();
        }
        String varName = FreshName.getFreshName("template");
        String idVarName = FreshName.getFreshName("id");
        this.code.add("Id "+idVarName+" = new Id(\""+id+"\");");
        this.code.add(t.getClass().getSimpleName()+" "+varName+" = new "+t.getClass().getSimpleName()+"("+idVarName+", new LinkedList<Id>());");
        return varName;
    }

}
