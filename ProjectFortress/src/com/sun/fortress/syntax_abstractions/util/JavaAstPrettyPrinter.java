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
import com.sun.fortress.nodes.Block;
import com.sun.fortress.nodes.Do;
import com.sun.fortress.nodes.DoFront;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.LooseJuxt;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.StringLiteralExpr;
import com.sun.fortress.nodes.TemplateGap;
import com.sun.fortress.nodes.TemplateGapExpr;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.syntax_abstractions.environments.GrammarEnv;
import com.sun.fortress.syntax_abstractions.environments.MemberEnv;
import com.sun.fortress.syntax_abstractions.environments.SyntaxDeclEnv;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;

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
        this.code.addAll(mkList(ids_result, idName, "Id"));
        this.code.add("APIName "+rVarName+" = new APIName("+idName+");");
        return rVarName;
    }

    @Override
    public String forBlockOnly(Block that, List<String> exprs_result) {
        String rVarName = FreshName.getFreshName("block");
        String exprsName = FreshName.getFreshName("exprs");
        this.code.addAll(mkList(exprs_result, exprsName, "Expr"));
        this.code.add("Block "+rVarName+" = new Block("+exprsName+");");
        return rVarName;
    }

    @Override
    public String forDoFrontOnly(DoFront that, Option<String> loc_result,
            String expr_result) {
        String rVarName = FreshName.getFreshName("doFront");
        String loc = "";
        if (loc_result.isNone()) {
            loc = "Option.<Expr>none()";
        }
        else {
            loc = "Option.<Expr>some("+loc_result.unwrap()+")";
        }
        this.code.add("DoFront "+rVarName+" = new DoFront("+loc+", "+that.isAtomic()+", "+expr_result+");");
        return rVarName;
    }

    @Override
    public String forDoOnly(Do that, List<String> fronts_result) {
        String rVarName = FreshName.getFreshName("aDo");
        String exprsName = FreshName.getFreshName("exprs");
        this.code.addAll(mkList(fronts_result, exprsName, "DoFront"));
        this.code.add("Do "+rVarName+" = new Do("+exprsName+");");
        return rVarName;
    }

    @Override
    public String forFnRefOnly(FnRef that, List<String> fns_result,
            List<String> staticArgs_result) {
        String rVarName = FreshName.getFreshName("fn");
        String fns = FreshName.getFreshName("ls");
        this.code.addAll(mkList(fns_result, fns, "Id"));
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
        public String forVarTypeOnly(VarType that, String name_result) {
        String rVarName = FreshName.getFreshName("idType");
        this.code.add("VarType "+rVarName+" = new VarType("+name_result+");");
        return rVarName;
    }


    @Override
    public String forLocalVarDeclOnly(LocalVarDecl that,
            List<String> body_result, List<String> lhs_result,
            Option<String> rhs_result) {
        String rVarName = FreshName.getFreshName("localVarDecl");

        String body = FreshName.getFreshName("body");
        this.code.addAll(mkList(body_result, body, "Expr"));

        String lhs = FreshName.getFreshName("lhs");
        this.code.addAll(mkList(lhs_result, lhs, "LValue"));

        String rhs = "";
        if (rhs_result.isNone()) {
            rhs = "Option.<Expr>none()";
        }
        else {
            rhs = "Option.<Expr>some("+rhs_result.unwrap()+")";
        }
        this.code.add("LocalVarDecl "+rVarName+" = new LocalVarDecl("+body+", "+lhs+", "+rhs+");");
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
    public String forLValueBindOnly(LValueBind that, String name_result,
            Option<String> type_result, List<String> mods_result) {
        String rVarName = FreshName.getFreshName("lValueBind");

        String type = "";
        if (type_result.isNone()) {
            type = "Option.<Type>none()";
        }
        else {
            type = "Option.<Type>some("+type_result.unwrap()+")";
        }

        String mods = FreshName.getFreshName("mods");
        this.code.addAll(mkList(mods_result, mods, "Modifier"));

        this.code.add("LValueBind "+rVarName+" = new LValueBind("+name_result+", "+type+", "+mods+", "+that.isMutable()+");");
        return rVarName;
    }

    @Override
    public String forIdOnly(Id that, Option<String> api_result) {
        String rVarName = FreshName.getFreshName("qIdName");
        String api = "";
        if (api_result.isNone()) {
            api = "Option.<APIName>none()";
        }
        else {
            api = "Option.<APIName>some("+api_result.unwrap()+")";
        }
        this.code.add("Id "+rVarName+" = new Id("+api+");");
        return rVarName;
    }

    @Override
    public String forStringLiteralExpr(StringLiteralExpr that) {
        String varName = FreshName.getFreshName("s");
        this.code.add("StringLiteralExpr "+varName+" = new StringLiteralExpr(\""+that.getText().replaceAll("\"","\\\\\"")+"\");");
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
        this.code.add("TypeArg "+rVarName+" = new TypeArg("+type_result+");");
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
        if (this.syntaxDeclEnv.contains(that.getVar())) { //TODO: is it ok to assume that it is not qualified?
            return that.getVar().toString();
        }
        return super.forVarRef(that);
    }

    @Override
    public String forTemplateGapExpr(TemplateGapExpr that) {
        return handleTemplateGap(that);
    }

    private String handleTemplateGap(TemplateGap t) {
        // Is the template t and instance of a template application
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

            Type type = nEnv.getType();
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
            return id.getText();
        }
        String varName = FreshName.getFreshName("template");
        String idVarName = FreshName.getFreshName("id");
        this.code.add("Id "+idVarName+" = new Id(\""+id+"\");");
        this.code.add(t.getClass().getSimpleName()+" "+varName+" = new "+t.getClass().getSimpleName()+"("+idVarName+", new LinkedList<Id>());");
        return varName;
    }

}
