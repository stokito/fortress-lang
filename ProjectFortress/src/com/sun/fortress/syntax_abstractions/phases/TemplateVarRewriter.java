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

package com.sun.fortress.syntax_abstractions.phases;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.CaseTransformer;
import com.sun.fortress.nodes.CaseTransformerClause;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Transformer;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes.UnparsedTransformer;
import com.sun.fortress.parser_util.IdentifierUtil;
import com.sun.fortress.syntax_abstractions.environments.GrammarEnv;
import com.sun.fortress.syntax_abstractions.environments.MemberEnv;
import com.sun.fortress.syntax_abstractions.util.BaseTypeCollector;
import com.sun.fortress.syntax_abstractions.util.JavaAstPrettyPrinter;
import com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.Debug;

import static com.sun.fortress.parser_util.SyntaxUtil.notIdOrOpOrKeyword;

import edu.rice.cs.plt.tuple.Option;

/*
 * Rewrite all occurrences of variables in a given template to 
 * occurrences of gaps 
 */
public class TemplateVarRewriter extends NodeUpdateVisitor {

    public static final String GAPSYNTAXPREFIX = " <!@#$%^&*<";
    public static final String GAPSYNTAXSUFFIX = ">*&^%$#@!> ";

    private Map<Id, BaseType> vars;
    private Option<BaseType> caseBaseType;

    public TemplateVarRewriter( Map<Id, BaseType> vs ){
        vars = vs;
        caseBaseType = Option.none();
    }
    
    @Override public Node forUnparsedTransformerOnly(UnparsedTransformer that, Id id_result) {
        return new UnparsedTransformer( rewriteVars( that.getTransformer() ), id_result);
    }

    @Override public Node forCaseTransformer(CaseTransformer that) {
        Debug.debug( Debug.Type.SYNTAX, 2, "Case type for " + that.getGapName() + " is " + vars.get(that.getGapName()) );
        caseBaseType = Option.wrap(vars.get(that.getGapName()));
        return super.forCaseTransformer(that);
    }
    
    /* extend the environment and transform the body */
    @Override public Node forCaseTransformerClause(CaseTransformerClause that) {
        Map<Id, BaseType> myvars = new HashMap<Id, BaseType>( vars );
        for ( Id param : that.getParameters() ){
            myvars.put( param, caseBaseType.unwrap() );
        }
        return new CaseTransformerClause( that.getConstructor(), that.getParameters(), (Transformer) that.getBody().accept( new TemplateVarRewriter( myvars ) ) );
    }

    public String rewriteVars(String t) {
        Map<String, String> varToGapName = new HashMap<String, String>();
        String result = "";
        boolean inSideString = false;
        
        L:for (int inx=0; inx<t.length(); inx++) {
            
            if (inSideString && (t.charAt(inx) == '\\') && (inx+1<t.length()) && (t.charAt(inx+1) == '\"')) {
                result+= "\\\"";
                inx+= 2;
            }
            if (isString(inx, t)) {
                inSideString = !inSideString;
            }
            else if (!inSideString) {
                for (Id id: vars.keySet()) {
                    String var = id.getText();
                    //	            System.err.println("Testing: "+var);
                    int end = inx+var.length();

                    if (match(inx, end, var, t)) {
                        //					System.err.println("Match... "+inx);
                        if (isVar(inx, end, t)) {
                            //						System.err.println("isVar...");
                            inx = end-1;
                            String tmp = "";
                            if (isTemplateApplication(end, t)) {
                                Pair<Integer,String> p = parseTemplateApplication(varToGapName, end, t, var);
                                inx = p.getA();
                                tmp = p.getB();
                            }
                            String type = vars.get(id).accept(new BaseTypeCollector());
                            result += getVar(var+tmp, type);
                            continue L;
                        }
                    }
                }
            }
            result += t.charAt(inx);
        }	
        return result;
    }

    private boolean isString(int inx, String t) {
        return t.charAt(inx) == '"';
    }

    private boolean isVar(int inx, int end, String t) {
        if (startOfString(inx)) {
            return isTemplateApplication(end, t) || toEndOfString(end, t) || notIdOrOpOrKeyword(t.charAt(end));
        } else if (endOfString(end, t)) {
            return notIdOrOpOrKeyword(t.charAt(inx-1));
        } else {
            char c = t.charAt(inx-1);
            return (notIdOrOpOrKeyword(c ) && isTemplateApplication(end, t)) || (notIdOrOpOrKeyword(c) && notIdOrOpOrKeyword(t.charAt(end)));
        }
    }

    private boolean endOfString(int end, String t) {
        return end == t.length();
    }

    private boolean toEndOfString(int end, String t) {
        return endOfString(end, t) || isTemplateApplication(end, t);
    }

    private boolean match(int s, int e, String var, String t) {
        if (e-1<t.length()) {
            return t.substring(s, e).equals(var);
        }
        return false;
    }

    private Pair<Integer,String> parseTemplateApplication(Map<String, String> varToGapName, int end, String t, String v) {
        String result = "";
        if (isTemplateApplication(end, t)) {
            int jnx = getEndOfTemplateApplication(end,t);
            List<String> params = parseArgs(varToGapName, t.substring(end+1, jnx));
            if (!params.isEmpty()) {
                result = "(";
                Iterator<String> it = params.iterator();
                while (it.hasNext()) {
                    result += it.next();
                    if (it.hasNext()) {
                        result += ", ";
                    }
                }
                result += ")";
            }			
            return new Pair<Integer,String>(jnx, result);
        }
        // return an error code
        return new Pair<Integer,String>(-1, result);
    }

    private String getVar(String v, String nonterminal) {
        return TemplateVarRewriter.getGapString(v, nonterminal);
    }

    private int getEndOfTemplateApplication(int end, String t) {
        return t.indexOf(')', end);
    }

    private boolean isTemplateApplication(int end, String t) {
        int jnx = getEndOfTemplateApplication(end, t);		
        if (jnx > -1) {
            return t.length() > end && t.charAt(end) == '(';	
        }
        return false;
    }

    private boolean startOfString(int inx) {
        return inx == 0;
    }

    private List<String> parseArgs(Map<String, String> varToGapName , String s) {
        String[] tokens = s.split(",");
        List<String> ls = new LinkedList<String>();
        for (String token: tokens) {
            ls.add(token.trim());
        }
        return ls;
    }

    public static String getGapString(String var, String type) {
        return getGapSyntaxPrefix(type)+" "+var+" "+TemplateVarRewriter.GAPSYNTAXSUFFIX;
    }

    public static String getGapSyntaxPrefix(String type) {
        /* get rid of this check? */
        if (type.startsWith("THELLO")) {
            return TemplateVarRewriter.GAPSYNTAXPREFIX+"StringLiteralExpr";
        }
        return TemplateVarRewriter.GAPSYNTAXPREFIX+type.trim();
    }
}
