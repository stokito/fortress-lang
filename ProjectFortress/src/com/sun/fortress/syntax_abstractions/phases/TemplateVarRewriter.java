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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.nodes_util.NodeFactory;

import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.CaseTransformer;
import com.sun.fortress.nodes.CaseTransformerClause;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.Transformer;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.UnparsedTransformer;
import com.sun.fortress.syntax_abstractions.environments.GapEnv;
import com.sun.fortress.syntax_abstractions.environments.Depth;
import com.sun.fortress.syntax_abstractions.environments.Depth.BaseDepth;
import com.sun.fortress.syntax_abstractions.environments.Depth.ListDepth;
import com.sun.fortress.syntax_abstractions.environments.Depth.OptionDepth;
import com.sun.fortress.syntax_abstractions.util.BaseTypeCollector;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.Debug;

import static com.sun.fortress.parser_util.SyntaxUtil.notIdOrOpOrKeyword;

/*
 * Rewrite all occurrences of variables in a given template to 
 * occurrences of gaps 
 */
class TemplateVarRewriter extends NodeUpdateVisitor {

    public static final String GAPSYNTAXPREFIX = " <!@#$%^&*<";
    public static final String GAPSYNTAXSUFFIX = ">*&^%$#@!> ";

    private GapEnv gapEnv;

    // FIXME: temp hack, for vars without nonterminals
    private Map<Id, BaseType> varsToTypes;

    TemplateVarRewriter(GapEnv gapEnv) {
        this(gapEnv, new HashMap<Id, BaseType>());
    }

    TemplateVarRewriter(GapEnv gapEnv, Map<Id, BaseType> varsToTypes) {
        this.gapEnv = gapEnv;
        this.varsToTypes = varsToTypes;
    }

    @Override public Node forUnparsedTransformerOnly(UnparsedTransformer that, Id id_result) {
        return new UnparsedTransformer( rewriteVars( that.getTransformer() ), id_result);
    }

    @Override public Node forCaseTransformer(CaseTransformer thatTransformer) {
        final Id gapName = thatTransformer.getGapName();
        final BaseType caseType = lookupType(gapName);
        Debug.debug(Debug.Type.SYNTAX, 3,
                    "Case var " + gapName + " has type " + caseType);
        return thatTransformer.accept(new NodeUpdateVisitor() {

                /* extend the environment and transform the body */
                @Override public Node forCaseTransformerClause(CaseTransformerClause that) {
                    TemplateVarRewriter tvr = extendWithCaseBindings(gapName, caseType, that);
                    return new CaseTransformerClause(that.getConstructor(), 
                                                     that.getParameters(),
                                                     (Transformer) that.getBody().accept(tvr));
                }
            });
    }

    private BaseType lookupType(Id id) {
        final BaseType baseType;
        if (varsToTypes.containsKey(id)) {
            baseType = varsToTypes.get(id);
        } else {
            baseType = gapEnv.getAstType(id);
        }
        Depth depth = gapEnv.getDepth(id);
        if (depth == null) {
            // FIXME
            depth = new BaseDepth();
        }
        return depth.accept(new Depth.Visitor<BaseType>() {
                public BaseType forBaseDepth(BaseDepth depth) {
                    return baseType;
                }
                public BaseType forListDepth(ListDepth depth) {
                    return NodeFactory.makeTraitType
                        (NodeFactory.makeId(baseType.getSpan(), "List"),
                         NodeFactory.makeTypeArg(depth.getParent().accept(this)));
                }
                public BaseType forOptionDepth(OptionDepth depth) {
                    return NodeFactory.makeTraitType
                        (NodeFactory.makeId(baseType.getSpan(), "Maybe"),
                         NodeFactory.makeTypeArg(depth.getParent().accept(this)));
                }
            });
    }

    private TemplateVarRewriter extendWithCaseBindings(Id gapName, BaseType caseType, 
                                                       CaseTransformerClause that) {
        List<Id> parameters = that.getParameters();

        Debug.debug(Debug.Type.SYNTAX, 2, "Case type for " + gapName + " is " + caseType);

        // FIXME!!!
        if (that.getConstructor().getText().equals("Cons")) {
            if (parameters.size() == 2) {
                Map<Id, BaseType> extendedVars = new HashMap<Id, BaseType>(varsToTypes);
                extendedVars.put(parameters.get(0), unwrapListType(caseType));
                extendedVars.put(parameters.get(1), caseType);
                return new TemplateVarRewriter(gapEnv, extendedVars);
            } else {
                throw new RuntimeException("Cons case expects 2 parameters");
            }
        } else if (that.getConstructor().getText().equals("Empty")) {
            if (parameters.isEmpty()) {
                // nothing to add
                return this;
            } else {
                throw new RuntimeException("Empty case expects 0 parameters");
            }
        } else {
            throw new RuntimeException("Unrecognized constructor name: " + that.getConstructor());
        }
    }

    private BaseType unwrapListType(BaseType type) {
        if (type instanceof TraitType && ((TraitType)type).getName().getText().equals("List")) {
            List<StaticArg> params = ((TraitType)type).getArgs();
            if (params.size() == 1 && params.get(0) instanceof TypeArg) {
                // FIXME: Check cast to BaseType
                return (BaseType)((TypeArg)params.get(0)).getType();
            }
        }
        throw new RuntimeException("expected a List type, got: " + type);
    }

    String rewriteVars(String t) {
        String result = "";
        boolean inSideString = false;

        Set<Id> allGaps = new HashSet<Id>();
        allGaps.addAll(varsToTypes.keySet());
        allGaps.addAll(gapEnv.gaps());

        L:for (int inx=0; inx<t.length(); inx++) {
            if (inSideString && (t.charAt(inx) == '\\') && (inx+1<t.length()) &&
                    (t.charAt(inx+1) == '\"')) {
                result+= "\\\"";
                inx+= 2;
            }
            if (isString(inx, t)) {
                inSideString = !inSideString;
            } else if (!inSideString) {
                for (Id id: allGaps) {
                    String var = id.getText();
                    int end = inx+var.length();

                    if (match(inx, end, var, t)) {
                        if (isVar(inx, end, t)) {
                            inx = end-1;
                            String tmp = "";
                            if (isTemplateApplication(end, t)) {
                                Pair<Integer,String> p = 
                                    parseTemplateApplication(end, t, var);
                                inx = p.getA();
                                tmp = p.getB();
                            }
                            // FIXME: What is BaseTypeCollector doing that
                            // getJavaType wouldn't?
                            String type = lookupType(id).accept(new BaseTypeCollector());
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
            return isTemplateApplication(end, t) || toEndOfString(end, t)
                || notIdOrOpOrKeyword(t.charAt(end));
        } else if (endOfString(end, t)) {
            return notIdOrOpOrKeyword(t.charAt(inx-1));
        } else {
            char c = t.charAt(inx-1);
            return (notIdOrOpOrKeyword(c ) && isTemplateApplication(end, t))
                || (notIdOrOpOrKeyword(c) && notIdOrOpOrKeyword(t.charAt(end)));
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

    private Pair<Integer,String> parseTemplateApplication(int end, String t, String v) {
        String result = "";
        if (isTemplateApplication(end, t)) {
            int jnx = getEndOfTemplateApplication(end,t);
            List<String> params = parseArgs(t.substring(end+1, jnx));
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
        return getGapString(v, nonterminal);
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

    private List<String> parseArgs(String s) {
        String[] tokens = s.split(",");
        List<String> ls = new LinkedList<String>();
        for (String token: tokens) {
            ls.add(token.trim());
        }
        return ls;
    }

    static String getGapString(String var, String type) {
        return GAPSYNTAXPREFIX+type.trim()+" "+var+" "+GAPSYNTAXSUFFIX;
    }
}
