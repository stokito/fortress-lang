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
import com.sun.fortress.nodes.Do;
import com.sun.fortress.nodes.DoFront;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.EnclosingFixity;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.LooseJuxt;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpExpr;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.StringLiteralExpr;
import com.sun.fortress.nodes.TemplateGap;
import com.sun.fortress.nodes.TemplateGapExpr;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TupleExpr;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
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
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(ids_result, idName, "Id"));
        this.code.add("APIName "+rVarName+" = new APIName("+sVarName+","+idName+");");
        return rVarName;
    }

    @Override
    public String forAsExprOnly(AsExpr that, String expr_result,
            String type_result) {
        String rVarName = FreshName.getFreshName("asExpr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add("AsExpr "+rVarName+" = new AsExpr("+sVarName+","+that.isParenthesized()+","+expr_result+","+type_result+");");
        return rVarName;
    }

    @Override
    public String forAsIfExprOnly(AsIfExpr that, String expr_result,
            String type_result) {
        String varName = FreshName.getFreshName("asExpr");
        String spanName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add("AsIfExpr "+varName+" = new AsIfExpr("+spanName+","+that.isParenthesized()+","+expr_result+","+type_result+");");
        return varName;
    }

    @Override
    public String forBlockOnly(Block that, List<String> exprs_result) {
        String rVarName = FreshName.getFreshName("block");
        String exprsName = FreshName.getFreshName("exprs");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(exprs_result, exprsName, "Expr"));
        this.code.add("Block "+rVarName+" = new Block("+sVarName+", "+exprsName+");");
        return rVarName;
    }

    @Override
    public String forDoFrontOnly(DoFront that, Option<String> loc_result,
            String expr_result) {
        String rVarName = FreshName.getFreshName("doFront");
        String loc = this.handleOption(loc_result, "Expr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add("DoFront "+rVarName+" = new DoFront("+sVarName+", "+loc+", "+that.isAtomic()+", "+expr_result+");");
        return rVarName;
    }

    @Override
    public String forDoOnly(Do that, List<String> fronts_result) {
        String rVarName = FreshName.getFreshName("aDo");
        String exprsName = FreshName.getFreshName("exprs");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(fronts_result, exprsName, "DoFront"));
        this.code.add("Do "+rVarName+" = new Do("+sVarName+", "+exprsName+");");
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
        this.code.add("FnRef "+rVarName+" = new FnRef("+sVarName+", "+that.isParenthesized()+", "+fns+", "+staticArgs+");");
        return rVarName;
    }

    @Override
    public String forId(Id that) {
        String rVarName = FreshName.getFreshName("id");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add("Id "+rVarName+" = new Id("+sVarName+", \""+that.getText()+"\");");
        return rVarName;
    }

    @Override
    public String forIdOnly(Id that, Option<String> api_result) {
        String rVarName = FreshName.getFreshName("qIdName");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String api = this.handleOption(api_result, "APIName");
        this.code.add("Id "+rVarName+" = new Id("+sVarName+", "+api+");");
        return rVarName;
    }
    
    @Override
        public String forVarTypeOnly(VarType that, String name_result) {
        String rVarName = FreshName.getFreshName("idType");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add("VarType "+rVarName+" = new VarType("+sVarName+", "+name_result+");");
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
        
        this.code.add("LocalVarDecl "+rVarName+" = new LocalVarDecl("+sVarName+", "+body+", "+lhs+", "+rhs+");");
        return rVarName;
    }

    @Override
    public String forLooseJuxtOnly(LooseJuxt that, List<String> exprs_result) {
        String rVarName = FreshName.getFreshName("lj");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String args = FreshName.getFreshName("ls");
        this.code.addAll(mkList(exprs_result, args, "Expr"));
        this.code.add("LooseJuxt "+rVarName+" = new LooseJuxt("+sVarName+", "+that.isParenthesized()+", "+args+");");
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

        this.code.add("LValueBind "+rVarName+" = new LValueBind("+sVarName+", "+name_result+", "+type+", "+mods+", "+that.isMutable()+");");
        return rVarName;
    }

    @Override
    public String forStringLiteralExpr(StringLiteralExpr that) {
        String varName = FreshName.getFreshName("s");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add("StringLiteralExpr "+varName+" = new StringLiteralExpr("+sVarName+", \""+that.getText().replaceAll("\"","\\\\\"")+"\");");
        return varName;
    }

    @Override
    public String forTightJuxtOnly(TightJuxt that, List<String> exprs_result) {
        String varName = FreshName.getFreshName("ls");
        String rVarName = FreshName.getFreshName("tj");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(exprs_result, varName, "Expr"));
        this.code.add("TightJuxt "+rVarName+" = new TightJuxt("+sVarName+", "+that.isParenthesized()+", "+varName+");");
        return rVarName;
    }

    @Override
    public String forTraitTypeOnly(TraitType that, String name_result,
            List<String> args_result) {
        String varName = FreshName.getFreshName("traitType");
        String lsVarName = FreshName.getFreshName("ls");
        String spanName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(args_result, lsVarName, "StaticArg"));
        this.code.add("TraitType "+varName+" = new TraitType("+spanName+", "+that.isParenthesized()+", "+name_result+","+lsVarName+");");
        return varName;
    }

    @Override
    public String forTupleExprOnly(TupleExpr that, List<String> exprs_result) {
        String varName = FreshName.getFreshName("ls");
        String rVarName = FreshName.getFreshName("tupleExpr");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.addAll(mkList(exprs_result, varName, "Expr"));
        this.code.add("TupleExpr "+rVarName+" = new TupleExpr("+sVarName+", "+that.isParenthesized()+", "+varName+");");
        return rVarName;
    }

    @Override
    public String forTypeArgOnly(TypeArg that, String type_result) {
        String rVarName = FreshName.getFreshName("typeArg");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add("TypeArg "+rVarName+" = new TypeArg("+sVarName+", "+type_result+");");
        return rVarName;
    }

    @Override
    public String forVarRefOnly(VarRef that, String var_result) {
        String varName = FreshName.getFreshName("varRef");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add("VarRef "+varName+" = new VarRef("+sVarName+", "+var_result+");");
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
            return that.getVar().toString();
        }
        return super.forVarRef(that);
    }
    
    @Override
    public String forEnclosingOnly(Enclosing that, Option<String> api_result, String in_open_result, String in_close_result) {
        String rVarName = FreshName.getFreshName("enclosing");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String apiVarName = this.handleOption(api_result, "APIName");
        this.code.add("Enclosing "+rVarName+" = new Enclosing("+sVarName+", "+apiVarName+", "+in_open_result+", "+in_close_result+");");
        return rVarName;
    }

    @Override
    public String forEnclosingFixity(EnclosingFixity that) {
        String rVarName = FreshName.getFreshName("enclosingFixity");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        this.code.add("EnclosingFixity "+rVarName+" = new EnclosingFixity("+sVarName+");");
        return rVarName;
    }

    @Override
    public String forOpOnly(Op that, Option<String> api_result,
            Option<String> fixity_result) {
        String rVarName = FreshName.getFreshName("op");
        String sVarName = JavaAstPrettyPrinter.getSpan(that, this.code);
        String apiVarName = this.handleOption(api_result, "APIName");
        String fixityVarName = this.handleOption(fixity_result, "Fixity");
        this.code.add("Op "+rVarName+" = new Op("+sVarName+", "+apiVarName+", \""+that.getText()+"\", "+fixityVarName+");");
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
        code.add("SourceLocRats "+slStartVarName+"   = new SourceLocRats(\""+file+"\", "+startLine+", "+startColumn+");");
        int endLine = span.getBegin().getLine();
        int endColumn = span.getBegin().column();
        code.add("SourceLocRats "+slEndVarName+"   = new SourceLocRats(\""+file+"\", "+endLine+", "+endColumn+");");
        code.add("Span "+rVarName +" = new Span("+slStartVarName+","+slEndVarName+");");        
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
        this.code.add("OpExpr "+rVarName+" = new OpExpr("+sVarName+", "+that.isParenthesized()+", "+in_op_result+", "+argsVarName+");");
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
        this.code.add("OpRef "+rVarName+" = new OpRef("+sVarName+", "+that.isParenthesized()+", "+opsVarName+", "+staticArgsVarName+");");
        return rVarName;
    }

    @Override
    public String forTemplateGapExpr(TemplateGapExpr that) {
        return handleTemplateGap(that);
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
                return id.getText();
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
