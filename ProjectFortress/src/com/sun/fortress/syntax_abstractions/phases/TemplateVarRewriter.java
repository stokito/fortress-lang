/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.phases;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.syntax_abstractions.environments.Depth;
import com.sun.fortress.syntax_abstractions.environments.Depth.BaseDepth;
import com.sun.fortress.syntax_abstractions.environments.Depth.ListDepth;
import com.sun.fortress.syntax_abstractions.environments.Depth.OptionDepth;
import com.sun.fortress.syntax_abstractions.environments.GapEnv;
import com.sun.fortress.syntax_abstractions.util.BaseTypeCollector;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.tuple.Option;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public Node forUnparsedTransformerOnly(UnparsedTransformer that, ASTNodeInfo info, Id id_result) {
        return new UnparsedTransformer(NodeFactory.makeSpanInfo(NodeFactory.makeSpan(that)),
                                       rewriteVars(that.getTransformer(), that),
                                       id_result);
    }

    @Override
    public Node forCaseTransformer(CaseTransformer thatTransformer) {
        final Id gapName = thatTransformer.getGapName();
        final BaseType caseType = lookupType(gapName);
        Debug.debug(Debug.Type.SYNTAX, 3, "Case var " + gapName + " has type " + caseType);
        return thatTransformer.accept(new NodeUpdateVisitor() {

            /* extend the environment and transform the body */
            @Override
            public Node forCaseTransformerClause(CaseTransformerClause that) {
                TemplateVarRewriter tvr = extendWithCaseBindings(gapName, caseType, that);
                return new CaseTransformerClause(NodeFactory.makeSpanInfo(NodeFactory.makeSpan(that)),
                                                 that.getConstructor(),
                                                 that.getParameters(),
                                                 (Transformer) that.getBody().accept(tvr));
            }
        });
    }

    private BaseType lookupType(Id id) {
        Option<BaseType> otype = tryLookupType(id);
        if (otype.isSome()) {
            return otype.unwrap();
        } else {
            throw new MacroError(id, "No type found for gap: " + id);
        }
    }

    private Option<BaseType> tryLookupType(Id id) {
        final BaseType baseType;
        if (varsToTypes.containsKey(id)) {
            baseType = varsToTypes.get(id);
        } else if (gapEnv.isGap(id)) {
            baseType = gapEnv.getAstType(id);
        } else {
            return Option.<BaseType>none();
        }
        Depth outer_depth = gapEnv.getDepth(id);
        if (outer_depth == null) {
            // FIXME
            outer_depth = new BaseDepth();
        }
        BaseType type = outer_depth.accept(new Depth.Visitor<BaseType>() {
            public BaseType forBaseDepth(BaseDepth depth) {
                return baseType;
            }

            public BaseType forListDepth(ListDepth depth) {
                return NodeFactory.makeTraitType(NodeFactory.makeId(NodeUtil.getSpan(baseType), "List"),
                                                 NodeFactory.makeTypeArg(depth.getParent().accept(this)));
            }

            public BaseType forOptionDepth(OptionDepth depth) {
                return NodeFactory.makeTraitType(NodeFactory.makeId(NodeUtil.getSpan(baseType), "Maybe"),
                                                 NodeFactory.makeTypeArg(depth.getParent().accept(this)));
            }
        });
        return Option.some(type);
    }

    private TemplateVarRewriter extendWithCaseBindings(Id gapName, BaseType caseType, CaseTransformerClause that) {
        List<Id> parameters = that.getParameters();

        Debug.debug(Debug.Type.SYNTAX, 2, "Case type for ", gapName, " is ", caseType);

        // FIXME!!!
        if (that.getConstructor().getText().equals("Cons")) {
            if (parameters.size() == 2) {
                Map<Id, BaseType> extendedVars = new HashMap<Id, BaseType>(varsToTypes);
                extendedVars.put(parameters.get(0), unwrapListType(caseType));
                extendedVars.put(parameters.get(1), caseType);
                return new TemplateVarRewriter(gapEnv, extendedVars);
            } else {
                throw new MacroError(that, "Cons case expects 2 parameters");
            }
        } else if (that.getConstructor().getText().equals("Empty")) {
            if (parameters.isEmpty()) {
                // nothing to add
                return this;
            } else {
                throw new MacroError(that, "Empty case expects 0 parameters");
            }
        } else {
            throw new MacroError(that.getConstructor(), "Unrecognized constructor name: " + that.getConstructor());
        }
    }

    private BaseType unwrapListType(BaseType type) {
        if (type instanceof TraitType && ((TraitType) type).getName().getText().equals("List")) {
            List<StaticArg> params = ((TraitType) type).getArgs();
            if (params.size() == 1 && params.get(0) instanceof TypeArg) {
                // FIXME: Check cast to BaseType
                return (BaseType) ((TypeArg) params.get(0)).getTypeArg();
            }
        }
        throw new MacroError(type, "expected a List type, got: " + type);
    }


    /* Rewrite template to surround occurrences of pattern variables
     * with the gap brackets (ie, PREFIX and SUFFIX), also including
     * associated AST type (FIXME: should be nonterminal).
     *
     * Main: Copy characters from input to output until encountering either
     * a string opener (") or an identifier-character.
     *
     * String: Copy characters from input to output until encountering
     * a string closer ("), then go back to Main.
     *
     * Identifier: Accumulate identifier characters until encountering
     * a non-identifier character.
     *   - If the string is a gap name in scope, then try to parse
     *     gap arguments.
     *   - Otherwise, copy the identifier to output and go back to Main.
     *
     * Gap Arguments: Expect a left parenthesis, then a comma-separated
     * list of identifiers.
     */

    String rewriteVars(String t, Node src) {
        int index = 0;
        int size = t.length();
        StringBuilder output = new StringBuilder();

        while (index < size) {
            // Main:
            char next = t.charAt(index);
            if (identifierStartChar(next)) {
                index = identifierLoop(t, index, size, output, src);
            } else if (stringStartChar(next)) {
                index = stringLoop(t, index, size, output, src);
            } else {
                output.append(next);
                index++;
            }
        }
        return output.toString();
    }

    // for testing
    String rewriteVars(String t) {
        Node src = new UnparsedTransformer(NodeFactory.makeSpanInfo(NodeFactory.macroSpan), t, NodeFactory.makeId(
                NodeFactory.macroSpan,
                "Expr"));
        return rewriteVars(t, src);
    }

    boolean identifierStartChar(char c) {
        // FIXME: Accept Fortress identifiers, not Java identifiers
        return Character.isJavaIdentifierStart(c);
    }

    boolean identifierChar(char c) {
        // FIXME: Accept Fortress identifiers, not Java identifiers
        return Character.isJavaIdentifierPart(c);
    }

    boolean stringStartChar(char c) {
        return c == '"';
    }

    boolean stringEndChar(char c) {
        return c == '"';
    }

    int stringLoop(String t, int index, int size, StringBuilder output, Node src) {
        // index is on the quotation mark character
        output.append(t.charAt(index));
        index++;

        while (index < size) {
            char next = t.charAt(index);
            output.append(next);
            index++;
            if (stringEndChar(next)) {
                return index;
            }
        }
        if (!(src instanceof ASTNode)) bug(src, "Only ASTNodes are supported.");
        throw new MacroError((ASTNode) src, "Unclosed string literal in template");
    }

    int identifierLoop(String t, int index, int size, StringBuilder output, Node src) {
        // index is on the first char of the identifier
        StringBuilder varbuffer = new StringBuilder();
        varbuffer.append(t.charAt(index));
        index++;

        while (index < size) {
            char next = t.charAt(index);
            if (identifierChar(next)) {
                varbuffer.append(next);
                index++;
            } else {
                break;
            }
        }
        String var = varbuffer.toString();
        if (!(src instanceof ASTNode)) bug(src, "Only AST nodes are supported.");
        Id varId = NodeFactory.makeId(NodeUtil.getSpan((ASTNode) src), var);
        Option<BaseType> otype = tryLookupType(varId);

        // FIXME! Add back support for parameters.

        if (otype.isSome()) {
            BaseType btype = otype.unwrap();
            String type = btype.accept(new BaseTypeCollector());
            Debug.debug(Debug.Type.SYNTAX, 3, "Found gap: name='" + var + "', type='" + type + "'");
            writeVar(var, type, output);
            return index;
        } else {
            output.append(var);
            return index;
        }
    }

    void writeVar(String var, String type, StringBuilder output) {
        output.append(GAPSYNTAXPREFIX);
        output.append(type.trim());
        output.append(" ");
        output.append(var);
        output.append(" ");
        output.append(GAPSYNTAXSUFFIX);
    }


    /*
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
    */

    static String getGapString(String var, String type) {
        return GAPSYNTAXPREFIX + type.trim() + " " + var + " " + GAPSYNTAXSUFFIX;
    }
}
