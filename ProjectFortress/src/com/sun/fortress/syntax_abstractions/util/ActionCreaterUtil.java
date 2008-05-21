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

import xtc.util.Utilities;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.EnclosingFixity;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Fixity;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpExpr;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.environments.SyntaxDeclEnv;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.tuple.Option;

public class ActionCreaterUtil {

    public static List<String> createVariableBinding(List<Integer> indents,
            SyntaxDeclEnv syntaxDeclEnv, String BOUND_VARIABLES,
            boolean isTemplate) {
        List<String> code = new LinkedList<String>();
        indents.add(3);
        code.add("Map<String, Object> "+BOUND_VARIABLES+" = new HashMap<String, Object>();");
        List<String> listCode = new LinkedList<String>();
        for(Id id: syntaxDeclEnv.getVariables()) {
            String var = id.getText();
            if (isTemplate) {
                if (syntaxDeclEnv.contains(id)) {
                    if (syntaxDeclEnv.isRepeat(id)) {
                        var = getFortressList(id, listCode, indents);  
                    }
                    if (syntaxDeclEnv.isOption(id)) {
                        var = getFortressMaybe(id, code, indents, syntaxDeclEnv);  
                    }
                    if (syntaxDeclEnv.isCharacterClass(id)) {
                        var = getFortressCharacterClass(id, code, indents);  
                    }
                    
                    if (syntaxDeclEnv.isAnyChar(id)) {
                        var = getFortressAnyChar(id, code, indents);  
                    }
                }
            }
            indents.add(3);
            code.add(BOUND_VARIABLES+".put(\""+id.getText()+"\""+", "+var+");");
        }
        listCode.addAll(code);
        return listCode;
    }

    private static String getFortressList(Id id, List<String> code, List<Integer> indents) {
        String enclosingName = FreshName.getFreshName("enclosingFixity");
        indents.add(4);
        code.add("EnclosingFixity "+enclosingName+" = new EnclosingFixity();");
        String optionName1 = FreshName.getFreshName("option");
        indents.add(4);
        code.add("Option<APIName> "+optionName1+"= Option.<APIName>none();");
        String optionName2 = FreshName.getFreshName("option");
        indents.add(4);
        code.add("Option<Fixity> "+optionName2+" = Option.<Fixity>some("+enclosingName+");");
        String opName1 = FreshName.getFreshName("op");
        indents.add(4);
        code.add("Op "+opName1+" = new Op("+optionName1+", \"<|\", "+optionName2+");");

        enclosingName = FreshName.getFreshName("enclosingFixity");
        indents.add(4);
        code.add("EnclosingFixity "+enclosingName+" = new EnclosingFixity();");
        optionName1 = FreshName.getFreshName("option");
        indents.add(4);
        code.add("Option<APIName> "+optionName1+"= Option.<APIName>none();");
        optionName2 = FreshName.getFreshName("option");
        indents.add(4);
        code.add("Option<Fixity> "+optionName2+" = Option.<Fixity>some("+enclosingName+");");
        String opName2 = FreshName.getFreshName("op");
        indents.add(4);
        code.add("Op "+opName2+" = new Op("+optionName1+", \"|>\", "+optionName2+");");

        optionName1 = FreshName.getFreshName("option");
        indents.add(4);
        code.add("Option<APIName> "+optionName1+" = Option.<APIName>none();");

        enclosingName = FreshName.getFreshName("enclosing");
        indents.add(4);
        code.add("Enclosing "+enclosingName+" = new Enclosing("+optionName1+", "+opName1+", "+opName2+");");
        String lsName = FreshName.getFreshName("ls");
        indents.add(4);
        code.add("List<OpName> "+lsName+" = new LinkedList<OpName>();");
        indents.add(4);
        code.add(lsName+".add("+enclosingName+");");
        String opRefName = FreshName.getFreshName("opRef");
        indents.add(4);
        code.add("OpRef "+opRefName+" = new OpRef(false, "+lsName+", new LinkedList<StaticArg>());");
        String tlsName = FreshName.getFreshName("ls");
        indents.add(4);
        code.add("List<Expr> "+tlsName+" = new LinkedList<Expr>();");
        indents.add(4);
        code.add(tlsName+".addAll("+id.getText()+".list());");
        String opExprName = FreshName.getFreshName("opExpr");
        indents.add(4);
        code.add("OpExpr "+opExprName+" = new OpExpr(true, "+opRefName+", "+tlsName+");");
        return opExprName;
    }

    private static String getFortressMaybe(Id id, List<String> code, List<Integer> indents, SyntaxDeclEnv syntaxDeclEnv) {
        int codeSize = code.size();
        String spanName = JavaAstPrettyPrinter.getSpan(id, code);
        for (int inx=codeSize; inx<code.size(); inx++) {
            indents.add(3);
        }        
       
        String name = FreshName.getFreshName("option");
        indents.add(3);
        code.add("Expr "+name+" = null;");
        
        String staticArgs = FreshName.getFreshName("staticArgs");
        indents.add(3);
        code.add("List<StaticArg> "+staticArgs+"= new LinkedList<StaticArg>();");
//        indents.add(3);
//        code.add(staticArgs+".add(new TypeArg(new VarType(NodeFactory.makeId("+spanName+",\""+SyntaxAbstractionUtil.FORTRESSAST+"\", "+type+"))));");
        indents.add(3);
        code.add("if (null == "+id.getText()+") {");
        String justArgs = FreshName.getFreshName("justArgs");
        indents.add(3);
        code.add("List<Expr> "+justArgs+" = new LinkedList<Expr>();");
        indents.add(3);
        code.add("    "+justArgs+".add("+id.getText()+");");
        indents.add(3);
        code.add("    "+name+" = com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil.makeObjectInstantiation("+spanName+", \""+SyntaxAbstractionUtil.FORTRESSAST+"\", \""+SyntaxAbstractionUtil.JUST+"\", "+justArgs+", "+staticArgs+");");
        indents.add(3);
        code.add("}");
        indents.add(3);
        code.add("else {");
        indents.add(3);
        code.add("    "+name+" = com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil.makeNoParamObjectInstantiation("+spanName+", \""+SyntaxAbstractionUtil.FORTRESSAST+"\", \""+SyntaxAbstractionUtil.NOTHING+"\", "+staticArgs+");");
        indents.add(3);
        code.add("}");
        return name;
    }
    
    private static String getFortressCharacterClass(Id id, List<String> code, List<Integer> indents) {
        String name = FreshName.getFreshName("characterClass");
        indents.add(3);
        code.add("StringLiteralExpr "+name+" = new StringLiteralExpr(\"\"+"+id.getText()+");");
        return name;
    }
    
    private static String getFortressAnyChar(Id id, List<String> code,
            List<Integer> indents) {
        String name = FreshName.getFreshName("anyCharacter");
        indents.add(3);
        code.add("StringLiteralExpr "+name+" = new StringLiteralExpr(\"\"+"+id.getText()+");");
        return name;
    }
    
    public static List<String> createRatsAction(String serializedComponent, List<Integer> indents) {
        List<String> code = new LinkedList<String>();
        String[] sc = Utilities.SPACE_NEWLINE_SPACE.split(serializedComponent);
        indents.add(3);
        code.add("String code = "+"\""+sc[0].replaceAll("\"", "\\\\\"") + " \"+");
        for (int inx = 1; inx < sc.length; inx++) {
            String s = "\""+sc[inx].replaceAll("\"", "\\\\\"") + " \"";
            if (inx < sc.length-1) {
                s += "+";
            }
            else {
                s += ";";
            }
            indents.add(3);
            code.add(s);
        }
        return code;
    }

}
