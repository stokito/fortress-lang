/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.tools;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

import java.util.*;

/* Converts a Node to a string which is the concrete version of that node
 *
 * Caveats:
 * 1. Accumulators can be optionally prefixed by "BIG".
 *    The AST does not preserve this information.
 * 2. Operator names have equivalent canonical versions, e.g., "=/=" and "NE"
 *
 * Possible improvements:
 * 1. Add newlines and indentation
 * 2. Handle syntax abstraction nodes.
 */

public class FortressAstToConcrete extends NodeDepthFirstVisitor<String> {

    private boolean _unqualified;
    private boolean _unmangle;
    private boolean inComponent = false;
    private int width = 50;
    private Set<String> locals = new HashSet<String>();

    public FortressAstToConcrete() {
        _unqualified = false;
    }

    public FortressAstToConcrete(boolean unqualified, boolean unmangle) {
        _unqualified = unqualified;
        _unmangle = unmangle;
    }

    /* indentation utilities *************************************************/
    private int indent = 0;

    private void increaseIndent() {
        indent += 1;
    }

    private void decreaseIndent() {
        indent -= 1;
    }

    private String getIndent() {
        StringBuilder s = new StringBuilder();

        for (int i = 0; i < indent; i++) {
            s.append("    ");
        }

        return s.toString();
    }

    private String indent(String stuff) {
        String r = getIndent() + stuff.replace("\n", "\n" + getIndent());
        r = r.replaceAll(" +\n", "\n");
        r = r.replaceAll(" +$", "");
        return r;
    }

    /* utility methods ********************************************************/
    /* handles parenthesized field */
    private String handleParen(String str, boolean isParen) {
        if (isParen) return "(" + str + ")";
        else return str;
    }

    /* returns number copies of s */
    private String makeCopies(int number, String s) {
        StringBuilder buf = new StringBuilder();
        buf.append(s);
        for (int index = 1; index < number; index++) {
            buf.append(s);
        }
        return buf.toString();
    }

    /* returns a string beginning with 'kind' followed by a sequence of elements
       in 'list' separated by commas and  enclosed by '{' and '}'
     */
    private StringBuilder inCurlyBraces(StringBuilder s, String kind, List<String> list) {
        StringBuilder ss = new StringBuilder();
        ss.append("{ ");
        for (String elem : IterUtil.skipLast(list)) {
            ss.append(elem);
            if (ss.length() > width) ss.append(",\n        ");
            else ss.append(", ");
        }
        if (!list.isEmpty()) {
            ss.append(IterUtil.last(list));
        }
        ss.append(" }");
        if (ss.length() > width || s.length() > width) s.append("\n   " + kind).append(ss.toString());
        else s.append(kind).append(ss.toString());
        return s;
    }

    /* as inCurlyBraces, except skips if empty, and drops curlies if singleton. */
    private StringBuilder optCurlyBraces(StringBuilder s, String kind, List<String> list, String follow) {
        if (list.isEmpty()) return s;
        if (list.size() > 1) {
            s = inCurlyBraces(s, kind, list);
        } else {
            if (s.length() > width) s = s.append("\n   " + kind).append(list.get(0));
            else s = s.append(kind).append(list.get(0));
        }
        return s.append(follow);
    }

    private StringBuilder optCurlyClause(StringBuilder s, String name, Option<List<String>> throwsClause_result) {
        if (throwsClause_result.isSome()) {
            List<String> throws_ = throwsClause_result.unwrap();
            s = optCurlyBraces(s, name, throws_, "");
        }
        return s;
    }

    private StringBuilder throwsClause(StringBuilder s, Option<List<String>> throwsClause_result) {
        return optCurlyClause(s, " throws ", throwsClause_result);
    }

    /* returns a string of a sequence of elements
       in 'list' separated by commas and  enclosed by '[\' and '\]'
     */
    private StringBuilder inOxfordBrackets(StringBuilder s, List<String> list) {
        if (list.isEmpty()) {
            return s;
        }
        s.append("[\\");
        for (String elem : IterUtil.skipLast(list)) {
            s.append(elem);
            if (s.length() > width) s.append(",\n        ");
            else s.append(", ");
        }
        if (!list.isEmpty()) {
            s.append(IterUtil.last(list));
        }
        s.append("\\]");
        return s;
    }

    private void showMods(StringBuilder b, Modifiers mods) {
        if (mods.equals(Modifiers.None)) return;
        b.append(mods.toString());
        b.append(" ");
    }

    private String inOxfordBrackets(List<String> list) {
        return inOxfordBrackets(new StringBuilder(), list).toString();
    }

    /* returns a string of a sequence of elements
       in 'list' separated by commas and  enclosed by '(' and ')'
     */
    private String inParentheses(List<String> list) {
        if (list.size() == 1) {
            return list.get(0);
        } else {
            StringBuilder s = new StringBuilder();
            s.append("(");
            for (String elem : IterUtil.skipLast(list)) {
                s.append(elem);
                if (s.length() > width) s.append(",\n        ");
                else s.append(", ");
            }
            if (!list.isEmpty()) {
                s.append(IterUtil.last(list));
            }
            s.append(")");
            return s.toString();
        }
    }

    /*  make sure it is parenthesized */
    private String inParentheses(String str) {
        if (str.startsWith("(") && str.endsWith(")")) return str;
        else return "(" + str + ")";
    }

    private String join(List<String> all, String sep) {
        StringBuilder s = new StringBuilder();

        int index = 0;
        for (String element : all) {
            s.append(element);
            if (index < all.size() - 1) {
                if (s.length() > width && sep.equals(", ")) s.append(sep + "\n        ");
                else if (!element.equals("\n") || !sep.equals("\n")) s.append(sep);
            }
            index += 1;
        }

        return s.toString();
    }

    private String handleMapElem(Expr expr, Expr that, FortressAstToConcrete visitor) {
        if (!(expr instanceof TupleExpr)) return bug(that,
                                                     "Subexpressions of a map expression " +
                                                     "should be tuple expressions.");
        else {
            TupleExpr _expr = (TupleExpr) expr;
            List<Expr> exprs = _expr.getExprs();
            if (exprs.size() != 2) {
                return bug(that,
                           "A subexpression of a map " + "expression should be a tuple " + "expression of size 2.");
            } else {
                StringBuilder s = new StringBuilder();
                s.append(exprs.get(0).accept(visitor));
                s.append(" |-> ");
                s.append(exprs.get(1).accept(visitor));
                return s.toString();
            }
        }
    }

    private String canonicalOp(String name) {
        if (name.equals("NE")) {
            return "=/=";
        } else {
            return name;
        }
    }

    private String filterString(String original, String pattern) {
        return original.replaceAll(pattern, "");
    }

    private String handleType(String type) {
        if (type.startsWith("(*")) return "";
        else return ": " + type;
    }

    /* visit nodes ************************************************************/
    @Override
    public String forComponentOnly(Component that,
                                   String info,
                                   String name_result,
                                   List<String> imports_result,
                                   List<String> decls_result,
                                   List<String> comprises_result,
                                   List<String> exports_result) {
        inComponent = true;
        // note objectExprs parameter is a temporary hack.
        StringBuilder s = new StringBuilder();
        if (that.is_native()) s.append("native ");
        s.append("component ").append(name_result).append("\n");
        for (String import_ : imports_result) {
            s.append(import_).append("\n");
        }
        if (exports_result.size() == 0) return bug(that, "A component should have at least one export statement.");
        else s = optCurlyBraces(s, "export ", exports_result, "");
        s.append("\n");
        for (String decl_ : decls_result) {
            s.append(decl_).append("\n\n");
        }
        s.append("end");
        return s.toString();
    }

    @Override
    public String forApiOnly(Api that,
                             String info,
                             String name_result,
                             List<String> imports_result,
                             List<String> decls_result,
                             List<String> comprises_result) {
        inComponent = false;
        //increaseIndent();
        StringBuilder s = new StringBuilder();
        s.append("api ").append(name_result).append("\n");
        s.append(join(imports_result, "\n"));
        s.append("\n");
        s.append(indent(join(decls_result, "\n\n")));
        s.append("\n\n");
        //decreaseIndent();
        s.append("end");
        return s.toString();
    }

    @Override
    public String forImportStarOnly(ImportStar that, String info, String api_result, List<String> except_result) {
        StringBuilder s = new StringBuilder();
        s.append("import ").append(api_result).append(".{...}");
        s = optCurlyBraces(s, " except ", except_result, "");
        return s.toString();
    }

    @Override
    public String forImportNamesOnly(ImportNames that,
                                     String info,
                                     String api_result,
                                     List<String> aliasedNames_result) {
        StringBuilder s = new StringBuilder();
        s.append("import ").append(api_result);
        if (aliasedNames_result.isEmpty()) {
            return bug(that, "Import statement should have at least one name.");
        } else {
            s = optCurlyBraces(s, ".", aliasedNames_result, "");
        }
        return s.toString();
    }

    @Override
    public String defaultTransformationNode(_SyntaxTransformation that) {
        return "(* ..transformation.. *)";
    }

    @Override
    public String forImportApiOnly(ImportApi that, String info, List<String> apis_result) {
        StringBuilder s = new StringBuilder();
        if (apis_result.isEmpty()) {
            return bug(that, "Import statement should have at least one name.");
        } else {
            s = optCurlyBraces(s, "import api ", apis_result, "");
        }
        return s.toString();
    }

    @Override
    public String forAliasedSimpleNameOnly(AliasedSimpleName that,
                                           String info,
                                           String name_result,
                                           Option<String> alias_result) {
        StringBuilder s = new StringBuilder();
        if (that.getName() instanceof Op) {
            s.append("opr ").append(name_result);
        } else {
            s.append(name_result);
        }
        if (alias_result.isSome()) {
            s.append(" => ").append(alias_result.unwrap());
        }
        return s.toString();
    }

    @Override
    public String forAliasedAPINameOnly(AliasedAPIName that,
                                        String info,
                                        String api_result,
                                        Option<String> alias_result) {
        StringBuilder s = new StringBuilder();
        s.append(api_result);
        if (alias_result.isSome()) {
            s.append(" => ").append(alias_result.unwrap());
        }
        return s.toString();
    }

    private List<String> myRecurOnListOfDecl(List<Decl> that) {
        boolean sawField = false;
        boolean sawGetterSetter = false;
        List<String> accum = new java.util.ArrayList<String>(that.size());
        for (Decl elt : that) {
            if (elt instanceof VarDecl) {
                sawField = true;
            } else if (elt instanceof FnDecl) {
                if (NodeUtil.getMods((FnDecl) elt).isGetterSetter()) {
                    sawGetterSetter = true;
                    if (sawField) {
                        accum.add("\n");
                        sawField = false;
                    }
                } else {
                    if (sawGetterSetter) {
                        accum.add("\n");
                        sawGetterSetter = false;
                    }
                }
            }
            accum.add(recur(elt));
        }
        return accum;
    }

    @Override
    public String forTraitDecl(TraitDecl that) {
        if (inComponent) return super.forTraitDecl(that);
        else {
            String name_result = recur(NodeUtil.getName(that));
            List<String> staticParams_result = recurOnListOfStaticParam(NodeUtil.getStaticParams(that));
            List<String> extendsClause_result = recurOnListOfTraitTypeWhere(NodeUtil.getExtendsClause(that));
            Option<String> where_result = recurOnOptionOfWhereClause(NodeUtil.getWhereClause(that));
            Option<List<String>> params_result = recurOnOptionOfListOfParam(NodeUtil.getParams(that));
            List<String> excludes_result = recurOnListOfBaseType(NodeUtil.getExcludesClause(that));
            Option<List<String>> comprises_result = recurOnOptionOfListOfNamedType(NodeUtil.getComprisesClause(that));
            List<String> decls_result = myRecurOnListOfDecl(NodeUtil.getDecls(that));
            return forTraitDeclOnly(that,
                                    name_result,
                                    staticParams_result,
                                    extendsClause_result,
                                    where_result,
                                    decls_result,
                                    params_result,
                                    excludes_result,
                                    comprises_result);
        }
    }

    public String forTraitDeclOnly(TraitDecl that,
                                   String name_result,
                                   List<String> staticParams_result,
                                   List<String> extendsClause_result,
                                   Option<String> where_result,
                                   List<String> decls_result,
                                   Option<List<String>> params_result,
                                   List<String> excludes_result,
                                   Option<List<String>> comprises_result) {
        StringBuilder s = new StringBuilder();

        increaseIndent();
        showMods(s, NodeUtil.getMods(that));
        s.append("trait ").append(name_result);
        inOxfordBrackets(s, staticParams_result);
        if (params_result.isSome()) {
            s.append(inParentheses(inParentheses(params_result.unwrap())));
        }
        s = optCurlyBraces(s, " extends ", extendsClause_result, "");
        if (where_result.isSome()) s.append(" ").append(where_result.unwrap());
        s = optCurlyBraces(s, " excludes ", excludes_result, "");
        if (inComponent) s = optCurlyClause(s, " comprises ", comprises_result);
        else {
            if (comprises_result.isSome()) {
                List<String> throws_ = comprises_result.unwrap();
                if (!throws_.isEmpty()) {
                    if (that.isComprisesEllipses()) throws_.add("...");
                    inCurlyBraces(s, " comprises ", throws_);
                } else s.append(" comprises { ... }");
            }
        }
        if (!decls_result.isEmpty()) {
            s.append("\n");
            s.append(indent(join(decls_result, "\n")));
        }
        s.append("\nend");

        decreaseIndent();

        return s.toString();
    }

    public String forObjectDecl(ObjectDecl that) {
        if (inComponent) return super.forObjectDecl(that);
        else {
            String name_result = recur(NodeUtil.getName(that));
            List<String> staticParams_result = recurOnListOfStaticParam(NodeUtil.getStaticParams(that));
            List<String> extendsClause_result = recurOnListOfTraitTypeWhere(NodeUtil.getExtendsClause(that));
            Option<String> where_result = recurOnOptionOfWhereClause(NodeUtil.getWhereClause(that));
            Option<List<String>> params_result = recurOnOptionOfListOfParam(NodeUtil.getParams(that));
            Option<List<String>> throwsClause_result = recurOnOptionOfListOfType(NodeUtil.getThrowsClause(that));
            Option<String> contract_result = recurOnOptionOfContract(NodeUtil.getContract(that));
            List<String> decls_result = myRecurOnListOfDecl(NodeUtil.getDecls(that));
            return forObjectDeclOnly(that,
                                     name_result,
                                     staticParams_result,
                                     extendsClause_result,
                                     where_result,
                                     decls_result,
                                     params_result,
                                     throwsClause_result,
                                     contract_result);
        }
    }

    public String forObjectDeclOnly(ObjectDecl that,
                                    String name_result,
                                    List<String> staticParams_result,
                                    List<String> extendsClause_result,
                                    Option<String> where_result,
                                    List<String> decls_result,
                                    Option<List<String>> params_result,
                                    Option<List<String>> throwsClause_result,
                                    Option<String> contract_result) {
        StringBuilder s = new StringBuilder();

        increaseIndent();

        showMods(s, NodeUtil.getMods(that));
        s.append("object ").append(name_result);
        inOxfordBrackets(s, staticParams_result);
        if (params_result.isSome()) {
            s.append(inParentheses(inParentheses(params_result.unwrap())));
        }
        s = optCurlyBraces(s, " extends ", extendsClause_result, "");
        throwsClause(s, throwsClause_result);
        if (where_result.isSome()) s.append(" ").append(where_result.unwrap());
        if (contract_result.isSome()) {
            s.append(" ");
            s.append(contract_result.unwrap());
        }
        if (!decls_result.isEmpty()) {
            s.append("\n");
            s.append(indent(join(decls_result, "\n")));
        }
        s.append("\nend");

        decreaseIndent();

        return s.toString();
    }

    /* contains a true if any of the variables have a 'var' modifier */
    private boolean anyMutable(List<LValue> lhs) {
        for (LValue value : lhs) {
            if (value.isMutable()) return true;
        }
        return false;
    }

    @Override
    public String forVarDeclOnly(VarDecl that, String info, List<String> lhs_result, Option<String> init_result) {

        if (init_result.isNone()) return inParentheses(lhs_result);

        StringBuilder s = new StringBuilder();

        List<LValue> lhs = new ArrayList<LValue>();
        for (LValue lv : that.getLhs()) {
            lhs.add(lv);
        }
        boolean mutable = anyMutable(lhs);
        if (mutable && lhs_result.size() > 1) {
            s.append(filterString(inParentheses(lhs_result), "var"));
        } else s.append(inParentheses(lhs_result));
        if (mutable) {
            s.append(" := ");
        } else {
            s.append(" = ");
        }
        s.append(init_result.unwrap());

        return s.toString();
    }

    @Override
    public String forLValueOnly(LValue that, String info, String name_result, Option<String> type_result) {
        StringBuilder s = new StringBuilder();

        showMods(s, that.getMods());
        s.append(name_result);
        if (//(!locals.contains(that.getName().getText())) && 
            type_result.isSome()) {
            s.append(handleType(type_result.unwrap()));
        }

        return s.toString();
    }

    @Override
    public String for_RewriteFnOverloadDeclOnly(_RewriteFnOverloadDecl that,
                                                String info,
                                                String name_result,
                                                List<String> fns_result,
                                                Option<String> ret_type_result) {
        //
        return "(* _RewriteFnOverloadDecl(" + name_result + ") *)";
    }

    @Override
    public String for_RewriteObjectExprDeclOnly(_RewriteObjectExprDecl that,
                                                String info,
                                                List<String> objExprs_result) {
        return "(* _RewriteObjectExprDecl() *)";
    }

    @Override
    public String for_RewriteFunctionalMethodDeclOnly(_RewriteFunctionalMethodDecl that, String info) {
        return "(* _RewriteFunctionalMethodDecl() *)";
    }

    @Override
    public String forFnDecl(FnDecl that) {
        FnHeader header = that.getHeader();
        String name_result = recur(header.getName());
        List<String> staticParams_result = recurOnListOfStaticParam(header.getStaticParams());
        List<String> params_result = recurOnListOfParam(header.getParams());
        Option<String> returnType_result = recurOnOptionOfType(header.getReturnType());
        Option<String> whereClause_result = recurOnOptionOfWhereClause(header.getWhereClause());
        Option<List<String>> throwsClause_result = recurOnOptionOfListOfType(header.getThrowsClause());
        Option<String> contract_result = recurOnOptionOfContract(header.getContract());
        String unambiguousName_result = recur(that.getUnambiguousName());
        Option<String> body_result = recurOnOptionOfExpr(that.getBody());
        Option<String> implementsUnambiguousName_result = recurOnOptionOfIdOrOp(that.getImplementsUnambiguousName());
        return forFnDeclOnly(that,
                             name_result,
                             staticParams_result,
                             params_result,
                             returnType_result,
                             throwsClause_result,
                             whereClause_result,
                             contract_result,
                             unambiguousName_result,
                             body_result,
                             implementsUnambiguousName_result);
    }

    public String forFnDeclOnly(FnDecl thatFnDecl,
                                final String name_result,
                                List<String> staticParams_result,
                                List<String> params_result,
                                Option<String> returnType_result,
                                Option<List<String>> throwsClause_result,
                                Option<String> where_result,
                                Option<String> contract_result,
                                String unambigousName_result,
                                Option<String> body_result,
                                Option<String> implementsUnambiguousName_result) {
        StringBuilder s = new StringBuilder();
        showMods(s, NodeUtil.getMods(thatFnDecl));
        final String sparams = inOxfordBrackets(staticParams_result);
        final String vparams = inParentheses(params_result);
        s.append(NodeUtil.getName(thatFnDecl).accept(new NodeDepthFirstVisitor<String>() {

            @Override
            public String forId(final Id idThat) {
                return name_result + sparams + inParentheses(inParentheses(vparams));
            }

            @Override
            public String forNamedOp(final NamedOp opThat) {
                final String oper = opThat.getText();
                if (opThat.isEnclosing()) {
                    String left = oper.split("_")[0];
                    String right = oper.substring(left.length());
//                     if (left.equals("BIG")) {
//                         left = "BIG " + right.split(" ")[0];
//                         right = right.substring(left.length());
//                     }
                    String params = vparams.equals("()") ? "" : vparams;
                    params = params.startsWith("(") ? params.substring(1, params.length() - 1) : params;
                    return "opr " + left + sparams + " " + params + " " + right;
                }
                return opThat.getFixity().accept(new NodeDepthFirstVisitor<String>() {
                    @Override
                    public String forPreFixityOnly(PreFixity that) {
                        return "opr " + oper + sparams + inParentheses(vparams);
                    }

                    @Override
                    public String forPostFixityOnly(PostFixity that) {
                        return "opr " + inParentheses(vparams) + oper + sparams;
                    }

                    @Override
                    public String forNoFixityOnly(NoFixity that) {
                        return "opr " + oper + "()";
                    }

                    @Override
                    public String forInFixityOnly(InFixity that) {
                        return "opr " + oper + sparams + inParentheses(vparams);
                    }

                    @Override
                    public String forMultiFixityOnly(MultiFixity that) {
                        return "opr " + oper + sparams + inParentheses(vparams);
                    }

                    @Override
                    public String forBigFixityOnly(BigFixity that) {
                        return "opr " + oper + sparams + inParentheses(vparams);
                    }

                    @Override
                    public String forUnknownFixityOnly(UnknownFixity that) {
                        return "opr " + oper + sparams + inParentheses(vparams) + " (* unknown fixity *)";
                    }
                });
            }
        }));

        if (returnType_result.isSome()) {
            s.append(handleType(returnType_result.unwrap()));
        }
        throwsClause(s, throwsClause_result);
        if (where_result.isSome()) s.append(" ").append(where_result.unwrap());
        if (contract_result.isSome()) {
            s.append(" ");
            s.append(contract_result.unwrap());
        }
        if (body_result.isSome()) {
            s.append(" =\n");
            increaseIndent();
            s.append(body_result.unwrap());
            decreaseIndent();
        }
        return s.toString();
    }

    @Override
    public String forParamOnly(Param that,
                               String info,
                               String name_result,
                               Option<String> type_result,
                               Option<String> defaultExpr_result,
                               Option<String> varargsType_result) {
        StringBuilder s = new StringBuilder();

        showMods(s, that.getMods());
        s.append(name_result);

        if (!NodeUtil.isVarargsParam(that)) {
            if (type_result.isSome() && !name_result.equals("self")) s.append(handleType(type_result.unwrap()));
            if (defaultExpr_result.isSome()) s.append("=").append(defaultExpr_result);
        } else {
            s.append(handleType(varargsType_result.unwrap()));
            s.append("...");
        }

        return s.toString();
    }

    @Override
    public String forDimDeclOnly(DimDecl that,
                                 String info,
                                 String dim_result,
                                 Option<String> derived_result,
                                 Option<String> default_result) {
        StringBuilder s = new StringBuilder();

        s.append("dim ").append(dim_result).append(" ");
        if (derived_result.isSome()) {
            s.append("= ").append(derived_result.unwrap());
        }
        if (default_result.isSome()) {
            s.append("default ").append(default_result.unwrap());
        }

        return s.toString();
    }

    @Override
    public String forUnitDeclOnly(UnitDecl that,
                                  String info,
                                  List<String> units_result,
                                  Option<String> dim_result,
                                  Option<String> def_result) {
        StringBuilder s = new StringBuilder();

        if (that.isSi_unit()) {
            s.append("SI_");
        }
        s.append("unit ");
        s.append(join(units_result, " "));
        if (dim_result.isSome()) {
            s.append(handleType(dim_result.unwrap()));
        }
        if (def_result.isSome()) {
            s.append("= ").append(def_result.unwrap());
        }

        return s.toString();
    }

    @Override
    public String forTestDeclOnly(TestDecl that,
                                  String info,
                                  String name_result,
                                  List<String> gens_result,
                                  String expr_result) {
        StringBuilder s = new StringBuilder();

        s.append("test ").append(name_result).append(" [ ");
        s.append(join(gens_result, ", "));
        s.append(" ] = ");
        s.append(expr_result);

        return s.toString();
    }

    @Override
    public String forPropertyDeclOnly(PropertyDecl that,
                                      String info,
                                      Option<String> name_result,
                                      List<String> params_result,
                                      String expr_result) {
        StringBuilder s = new StringBuilder();

        s.append("property ");
        if (name_result.isSome()) {
            s.append(name_result.unwrap()).append(" = ");
        }
        if (!params_result.isEmpty()) {
            s.append("FORALL ");
            s.append(inParentheses(params_result));
            s.append(" ");
        }
        s.append(expr_result);

        return s.toString();
    }

    @Override
    public String forGrammarDeclOnly(GrammarDecl that,
                                     String info,
                                     String name_result,
                                     List<String> extends_result,
                                     List<String> members_result,
                                     List<String> transformers_result) {
        StringBuilder s = new StringBuilder();

        if (that.isNativeDef()) {
            s.append("native ");
        }
        increaseIndent();
        s.append("grammar " + name_result);
        if (!extends_result.isEmpty()) {
            s.append(" extends { ");
            s.append(join(extends_result, ", "));
            s.append(" }");
        }
        s.append("\n");

        s.append(indent(join(members_result, "\n")));

        /* transformers_result ?? */

        s.append("\n");
        s.append("end");
        decreaseIndent();

        return s.toString();
    }

    @Override
    public String forNonterminalDefOnly(NonterminalDef that,
                                        String info,
                                        String name_result,
                                        List<String> syntaxDefs_result,
                                        String header_result,
                                        Option<String> astType_result) {
        StringBuilder s = new StringBuilder();

        s.append(header_result);
        if (astType_result.isSome()) {
            s.append(handleType(astType_result.unwrap()));
        }
        s.append(":=\n");
        s.append("  " + join(syntaxDefs_result, "\n| "));

        return s.toString();
    }

    @Override
    public String forNonterminalExtensionDefOnly(NonterminalExtensionDef that,
                                                 String info,
                                                 String name_result,
                                                 List<String> syntaxDefs_result) {
        StringBuilder s = new StringBuilder();

        s.append(name_result);
        s.append("|:=\n");
        s.append("  " + join(syntaxDefs_result, "\n| "));

        return s.toString();
    }

    @Override
    public String forNonterminalHeaderOnly(NonterminalHeader that,
                                           String info,
                                           String name_result,
                                           List<String> params_result,
                                           List<String> staticParams_result,
                                           Option<String> type_result,
                                           Option<String> whereClause_result) {
        StringBuilder s = new StringBuilder();
        // modifier???
        s.append(name_result);
        inOxfordBrackets(s, staticParams_result);
        if (type_result.isSome()) {
            s.append(handleType(type_result.unwrap()));
        }
        if (!params_result.isEmpty()) {
            s.append("(");
            s.append(join(params_result, ", "));
            s.append(")");
        }

        return s.toString();
    }

    @Override
    public String forNonterminalParameterOnly(NonterminalParameter that,
                                              String info,
                                              String name_result,
                                              String type_result) {
        StringBuilder s = new StringBuilder();

        s.append(name_result).append(handleType(type_result));

        return s.toString();
    }

    @Override
    public String forSyntaxDefOnly(SyntaxDef that,
                                   String info,
                                   List<String> syntaxSymbols_result,
                                   String transformer_result) {
        StringBuilder s = new StringBuilder();

        s.append(join(syntaxSymbols_result, ""));
        s.append(" => ");
        s.append(transformer_result);

        return s.toString();
    }

    @Override
    public String forPreTransformerDefOnly(PreTransformerDef that, String info, String transformer_result) {
        return transformer_result;
    }

    /* TODO: handle nonterminal_result */
    @Override
    public String forUnparsedTransformerOnly(UnparsedTransformer that, String info, String nonterminal_result) {
        StringBuilder s = new StringBuilder();

        /* should be nonterminal_result + " <[" ... */
        s.append("<[ " + that.getTransformer() + "]>");

        return s.toString();
    }

    @Override
    public String forCaseTransformerOnly(CaseTransformer that,
                                         String info,
                                         String gapName_result,
                                         List<String> clauses_result) {
        StringBuilder s = new StringBuilder();

        increaseIndent();
        s.append("case ").append(gapName_result).append(" of\n");
        s.append(indent(join(clauses_result, "\n")));
        s.append("\n");
        s.append("end");
        decreaseIndent();

        return s.toString();
    }

    @Override
    public String forNodeTransformer(NodeTransformer that) {
        return "(* ..macro.. *)";
    }

    @Override
    public String forCaseTransformerClauseOnly(CaseTransformerClause that,
                                               String info,
                                               String constructor_result,
                                               List<String> parameters_result,
                                               String body_result) {
        StringBuilder s = new StringBuilder();

        s.append(constructor_result);
        if (!parameters_result.isEmpty()) {
            s.append("(");
            s.append(join(parameters_result, ", "));
            s.append(")");
        }
        s.append(" => ");
        s.append(body_result);

        return s.toString();
    }

    @Override
    public String forSuperSyntaxDefOnly(SuperSyntaxDef that,
                                        String info,
                                        String nonterminal_result,
                                        String grammar_result) {
        StringBuilder s = new StringBuilder();

        s.append(nonterminal_result).append(" from ").append(grammar_result);

        return s.toString();
    }

    @Override
    public String forPrefixedSymbolOnly(PrefixedSymbol that, String info, String id_result, String symbol_result) {
        StringBuilder s = new StringBuilder();
        s.append(id_result);
        s.append(":");
        s.append(symbol_result);
        return s.toString();
    }

    @Override
    public String forOptionalSymbolOnly(OptionalSymbol that, String info, String symbol_result) {
        return "{" + symbol_result + "}?";
    }

    @Override
    public String forRepeatSymbolOnly(RepeatSymbol that, String info, String symbol_result) {
        return "{" + symbol_result + "}*";
    }

    @Override
    public String forRepeatOneOrMoreSymbolOnly(RepeatOneOrMoreSymbol that, String info, String symbol_result) {
        return "{" + symbol_result + "}+";
    }

    @Override
    public String forNoWhitespaceSymbolOnly(NoWhitespaceSymbol that, String info, String symbol_result) {
        return symbol_result + "#";
    }

    @Override
    public String forGroupSymbolOnly(GroupSymbol that, String info, List<String> symbols_result) {
        StringBuilder s = new StringBuilder();
        s.append(join(symbols_result, ""));
        return s.toString();
    }

    @Override
    public String forAnyCharacterSymbolOnly(AnyCharacterSymbol that, String info) {
        return "_";
    }

    @Override
    public String forWhitespaceSymbolOnly(WhitespaceSymbol that, String info) {
        return " ";
    }

    @Override
    public String forTabSymbolOnly(TabSymbol that, String info) {
        return "TAB";
    }

    @Override
    public String forFormfeedSymbolOnly(FormfeedSymbol that, String info) {
        return "FORMFEED";
    }

    @Override
    public String forCarriageReturnSymbolOnly(CarriageReturnSymbol that, String info) {
        return "RETURN";
    }

    @Override
    public String forBackspaceSymbolOnly(BackspaceSymbol that, String info) {
        return "BACKSPACE";
    }

    @Override
    public String forNewlineSymbolOnly(NewlineSymbol that, String info) {
        return "NEWLINE";
    }

    @Override
    public String forBreaklineSymbolOnly(BreaklineSymbol that, String info) {
        return "\n";
    }

    @Override
    public String forItemSymbolOnly(ItemSymbol that, String info) {
        return that.getItem();
    }

    @Override
    public String forNonterminalSymbolOnly(NonterminalSymbol that, String info, String nonterminal_result) {
        return nonterminal_result;
    }

    @Override
    public String forKeywordSymbolOnly(KeywordSymbol that, String info) {
        return that.getToken();
    }

    @Override
    public String forTokenSymbolOnly(TokenSymbol that, String info) {
        return that.getToken();
    }

    @Override
    public String forNotPredicateSymbolOnly(NotPredicateSymbol that, String info, String symbol_result) {
        return "NOT " + symbol_result;
    }

    @Override
    public String forAndPredicateSymbolOnly(AndPredicateSymbol that, String info, String symbol_result) {
        return "AND " + symbol_result;
    }

    @Override
    public String forCharacterClassSymbolOnly(CharacterClassSymbol that, String info, List<String> characters_result) {
        StringBuilder s = new StringBuilder();

        s.append("[");
        s.append(join(characters_result, ""));
        s.append("]");

        return s.toString();
    }

    @Override
    public String forCharSymbolOnly(CharSymbol that, String info) {
        return that.getString();
    }

    @Override
    public String forCharacterIntervalOnly(CharacterInterval that, String info) {
        return that.getBeginSymbol() + ":" + that.getEndSymbol();
    }

    @Override
    public String defaultTemplateGap(TemplateGap t) {
        StringBuilder s = new StringBuilder();

        s.append(t.getGapId().accept(this));
        if (!t.getTemplateParams().isEmpty()) {
            s.append("(");
            for (Id id : t.getTemplateParams()) {
                s.append(id.accept(this));
            }
            s.append(")");
        }

        return s.toString();
    }

    @Override
    public String forAsExprOnly(AsExpr that, String info, String expr_result, String type_result) {
        return handleParen(expr_result + " typed " + type_result, NodeUtil.isParenthesized(that));
    }

    @Override
    public String forAsIfExprOnly(AsIfExpr that, String info, String expr_result, String type_result) {
        return handleParen(expr_result + " asif " + type_result, NodeUtil.isParenthesized(that));
    }

    @Override
    public String forAssignmentOnly(Assignment that,
                                    String info,
                                    List<String> lhs_result,
                                    Option<String> opr_result,
                                    String rhs_result,
                                    List<String> assignmentInfos) {
        StringBuilder s = new StringBuilder();

        s.append(inParentheses(lhs_result)).append(" ");
        if (opr_result.isSome()) {
            s.append(opr_result.unwrap()).append("= ");
        } else {
            s.append(":= ");
        }
        s.append(rhs_result);

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forBlock(Block that) {
        Set<String> original = new HashSet<String>();
        original.addAll(locals);
        String result = super.forBlock(that);
        locals.clear();
        locals.addAll(original);
        return result;
    }

    @Override
    public String forBlockOnly(Block that, String info, Option<String> loc_result, List<String> exprs_result) {
        StringBuilder s = new StringBuilder();

        increaseIndent();
        if (loc_result.isSome()) {
            s.append("at ").append(loc_result.unwrap()).append(" ");
        }
        if (that.isAtomicBlock()) {
            s.append("atomic ");
        }
        if (that.isWithinDo()) {
            s.append("do\n");
        }
        s.append(join(exprs_result, "\n"));
        decreaseIndent();

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forCaseExprOnly(CaseExpr that,
                                  String info,
                                  Option<String> param_result,
                                  Option<String> compare_result,
                                  String equalsOp_result,
                                  String inOp_result,
                                  List<String> clauses_result,
                                  Option<String> elseClause_result) {
        StringBuilder s = new StringBuilder();

        s.append("case ");
        if (param_result.isSome()) s.append(param_result.unwrap()).append(" ");
        else s.append("most ");
        if (compare_result.isSome()) s.append(compare_result.unwrap()).append(" ");
        s.append("of\n");
        s.append(join(clauses_result, "\n"));
        if (elseClause_result.isSome()) {
            s.append("\nelse => ");
            s.append(elseClause_result.unwrap());
        }
        s.append("\nend");

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forDoOnly(Do that, String info, List<String> fronts_result) {
        StringBuilder s = new StringBuilder();

        s.append(indent(join(fronts_result, " also\n")));
        s.append("\nend");

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forForOnly(For that, String info, List<String> gens_result, String body_result) {
        StringBuilder s = new StringBuilder();

        s.append("for ");
        s.append(join(gens_result, ", \n"));
        s.append("\n").append(body_result);
        s.append("\nend");

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forIfOnly(If that, String info, List<String> clauses_result, Option<String> elseClause_result) {
        StringBuilder s = new StringBuilder();

        s.append("if ");
        if (clauses_result.isEmpty()) {
            return bug(that, "An if expression should have at least " + "one then branch.");
        } else {
            Iterator<String> iter = clauses_result.iterator();
            s.append(iter.next());
            while (iter.hasNext()) {
                s.append("elif ");
                s.append(iter.next());
            }
        }
        if (elseClause_result.isSome()) {
            s.append("\nelse ");
            s.append(elseClause_result.unwrap());
        }
        s.append("\nend");

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forLabelOnly(Label that, String info, String name_result, String body_result) {
        StringBuilder s = new StringBuilder();

        s.append("label ").append(name_result).append("\n");
        s.append(body_result);
        s.append("\nend ").append(name_result);

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }


    @Override
    public String forObjectExprOnly(ObjectExpr that, String info, String header_result, Option<String> selfType) {
        StringBuilder s = new StringBuilder();
        List<String> extendsClause_result = recurOnListOfTraitTypeWhere(NodeUtil.getExtendsClause(that));
        List<String> decls_result = recurOnListOfDecl(NodeUtil.getDecls(that));

        increaseIndent();
        s.append("object ");
        s = optCurlyBraces(s, "extends ", extendsClause_result, "");
        if (!decls_result.isEmpty()) {
            s.append("\n");
            s.append(indent(join(decls_result, "\n")));
        }
        s.append("\nend");
        decreaseIndent();

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String for_RewriteObjectExprOnly(_RewriteObjectExpr that,
                                            String info,
                                            String header_result,
                                            List<String> staticArgs_result) {
        StringBuilder s = new StringBuilder();

        s.append("object ");
        List<String> extendsClause_result = recurOnListOfTraitTypeWhere(NodeUtil.getExtendsClause(that));
        List<String> decls_result = recurOnListOfDecl(NodeUtil.getDecls(that));

        s = optCurlyBraces(s, "extends ", extendsClause_result, "");
        if (!decls_result.isEmpty()) {
            s.append("\n");
            s.append(join(decls_result, "\n"));
        }
        s.append("\nend");

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forTryOnly(Try that,
                             String info,
                             String body_result,
                             Option<String> catchClause_result,
                             List<String> forbid_result,
                             Option<String> finallyClause_result) {
        StringBuilder s = new StringBuilder();

        increaseIndent();
        s.append("try\n").append(indent(body_result)).append("\n");
        decreaseIndent();
        if (catchClause_result.isSome()) {
            s.append(catchClause_result.unwrap()).append("\n");
        }
        s = optCurlyBraces(s, "forbid ", forbid_result, "\n");
        if (finallyClause_result.isSome()) {
            s.append("finally ");
            s.append(finallyClause_result.unwrap()).append("\n");
        }
        s.append("end");

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forTupleExprOnly(TupleExpr that,
                                   String info,
                                   List<String> exprs_result,
                                   Option<String> varargs_result,
                                   List<String> keywords_result) {
        if (varargs_result.isSome() || keywords_result.size() > 0) { // ArgExpr
            StringBuilder s = new StringBuilder();

            s.append("(");
            int exprs_size = exprs_result.size();
            int varargs_size = (varargs_result.isSome()) ? 1 : 0;
            int keywords_size = keywords_result.size();
            s.append(join(exprs_result, ", "));
            if (varargs_size == 1) {
                if (exprs_size > 0) s.append(", ");
                s.append(varargs_result.unwrap());
                s.append("...");
            }
            if (keywords_size > 0) {
                if (exprs_size + varargs_size > 0) s.append(", ");
                s.append(join(keywords_result, ", "));
            }
            s.append(")");

            return s.toString();
        } else {
            if (exprs_result.size() == 1)
                return handleParen(exprs_result.get(0), NodeUtil.isParenthesized(that));
            else
                return IterUtil.toString(exprs_result, "(", ", ", ")");
        }
    }

    @Override
    public String forTypecaseOnly(Typecase that,
                                  String info,
                                  String bindExpr_result,
                                  List<String> clauses_result,
                                  Option<String> elseClause_result) {
        StringBuilder s = new StringBuilder();

        s.append("typecase ");
        s.append(bindExpr_result);
        s.append(" of\n");
        increaseIndent();
        s.append(indent(join(clauses_result, "\n")));
        if (elseClause_result.isSome()) {
            s.append(indent("\nelse => " + elseClause_result.unwrap()));
        }
        decreaseIndent();
        s.append("\n");
        s.append("end");

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forWhileOnly(While that, String info, String test_result, String body_result) {
        StringBuilder s = new StringBuilder();

        s.append("while ").append(test_result);
        s.append(" ").append(body_result);

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forAccumulatorOnly(Accumulator that,
                                     String info,
                                     List<String> staticArgs_result,
                                     String opr_result,
                                     List<String> gens_result,
                                     String body_result) {
        StringBuilder s = new StringBuilder();

        if (that.getAccOp().isEnclosing()) { // comprehensions
            Op _op = that.getAccOp();
            String op = _op.getText();
            String left = op.split("_")[0];
            String right = op.substring(left.length());
            left = left.startsWith("BIG") ? left.substring(4, left.length()) : left;
            String closing = right.startsWith("BIG") ? right.substring(4, right.length()) : right;
            String sargs = inOxfordBrackets(staticArgs_result);
            if (left.endsWith("|->")) {
                s.append(left.substring(0, left.length() - 3));
                s.append(sargs);
                s.append(" ");
                s.append(handleMapElem(that.getBody(), that, this));
            } else {
                s.append(left);
                s.append(sargs);
                s.append(" ");
                s.append(body_result);
            }
            s.append(" | ");
            s.append(join(gens_result, ", "));
            s.append(closing);
        } else { // reductions
            if (opr_result.equals("BIG +")) {
                s.append("SUM");
            } else if (opr_result.equals("BIG DOT")) {
                s.append("PROD");
            } else {
                s.append(opr_result);
            }
            inOxfordBrackets(s, staticArgs_result);
            s.append(" ");
            if (!gens_result.isEmpty()) {
                s.append("[");
                s.append(join(gens_result, ", "));
                s.append("] ");
            }
            s.append(body_result);
        }

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    /* Possible differences in the original Fortress program and
       the unparsed program.
       In the Fortress source program,
       "BIG" may appear before an array comprehension.
       In AST, that information is gone because it has no meaning.
     */
    @Override
    public String forArrayComprehensionOnly(ArrayComprehension that,
                                            String info,
                                            List<String> staticArgs_result,
                                            List<String> clauses_result) {
        StringBuilder s = new StringBuilder();

        s.append("[");
        inOxfordBrackets(s, staticArgs_result);
        s.append(" ");
        s.append(indent(join(clauses_result, "\n")));
        s.append("]");

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forAtomicExprOnly(AtomicExpr that, String info, String expr_result) {
        StringBuilder s = new StringBuilder();
        s.append("atomic ").append(expr_result);
        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forExitOnly(Exit that, String info, Option<String> target_result, Option<String> returnExpr_result) {
        StringBuilder s = new StringBuilder();

        s.append("exit ");
        if (target_result.isSome()) {
            s.append(target_result.unwrap()).append(" ");
        }
        if (returnExpr_result.isSome()) {
            s.append("with ").append(returnExpr_result.unwrap());
        }

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forSpawnOnly(Spawn that, String info, String body_result) {
        return handleParen("spawn " + body_result, NodeUtil.isParenthesized(that));
    }

    @Override
    public String forThrowOnly(Throw that, String info, String expr_result) {
        return handleParen("throw " + expr_result, NodeUtil.isParenthesized(that));
    }

    @Override
    public String forTryAtomicExprOnly(TryAtomicExpr that, String info, String expr_result) {
        return handleParen("tryatomic " + expr_result, NodeUtil.isParenthesized(that));
    }

    @Override
    public String forFnExpr(FnExpr that) {
        FnHeader header = that.getHeader();
        String name_result = recur(header.getName());
        List<String> staticParams_result = recurOnListOfStaticParam(header.getStaticParams());
        List<String> params_result = recurOnListOfParam(header.getParams());
        Option<String> returnType_result = recurOnOptionOfType(header.getReturnType());
        Option<String> whereClause_result = recurOnOptionOfWhereClause(header.getWhereClause());
        Option<List<String>> throwsClause_result = recurOnOptionOfListOfType(header.getThrowsClause());
        String body_result = recur(that.getBody());
        return forFnExprOnly(that,
                             "",
                             name_result,
                             staticParams_result,
                             params_result,
                             returnType_result,
                             whereClause_result,
                             throwsClause_result,
                             body_result);
    }

    public String forFnExprOnly(FnExpr that,
                                String info,
                                String name_result,
                                List<String> staticParams_result,
                                List<String> params_result,
                                Option<String> returnType_result,
                                Option<String> where_result,
                                Option<List<String>> throwsClause_result,
                                String body_result) {
        StringBuilder s = new StringBuilder();

        s.append("fn ");
        s.append(inParentheses(inParentheses(params_result)));
        if (returnType_result.isSome()) {
            s.append(handleType(returnType_result.unwrap()));
        }
        throwsClause(s, throwsClause_result);
        s.append(" => ").append(body_result);

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forLetFnOnly(LetFn that, String info, String body_result, List<String> fns_result) {
        StringBuilder s = new StringBuilder();

        s.append(join(fns_result, "\n"));
        s.append("\n");
        s.append(body_result);

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forLocalVarDecl(LocalVarDecl that) {
        List<String> lhs_result = recurOnListOfLValue(that.getLhs());
        Option<String> rhs_result = recurOnOptionOfExpr(that.getRhs());
        for (LValue lv : that.getLhs()) {
            locals.add(lv.getName().getText());
        }
        String body_result = recur(that.getBody());
        return forLocalVarDeclOnly(that, "", body_result, lhs_result, rhs_result);
    }

    @Override
    public String forLocalVarDeclOnly(LocalVarDecl that,
                                      String info,
                                      String body_result,
                                      List<String> lhs_result,
                                      Option<String> rhs_result) {
        StringBuilder s = new StringBuilder();

        boolean mutable = anyMutable(that.getLhs());
        if (mutable && lhs_result.size() > 1) {
            if (rhs_result.isNone()) s.append("var ");
            s.append(filterString(inParentheses(lhs_result), "var"));
        } else s.append(inParentheses(lhs_result));
        if (rhs_result.isSome()) {
            if (mutable) {
                s.append(" := ");
            } else {
                s.append(" = ");
            }
            s.append(rhs_result.unwrap());
        }
        s.append("\n");
        s.append(body_result);

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forSubscriptExprOnly(SubscriptExpr that,
                                       String info,
                                       String obj_result,
                                       List<String> subs_result,
                                       Option<String> op_result,
                                       List<String> staticArgs_result) {
        StringBuilder s = new StringBuilder();
        String left;
        String right;

        s.append(obj_result);
        if (op_result.isSome()) {
            String enclosing = op_result.unwrap();
            int size = enclosing.length();
            left = enclosing.substring(0, size / 2);
            right = enclosing.substring(size / 2 + 1, size);
        } else {
            left = "[";
            right = "]";
        }
        s.append(left);
        inOxfordBrackets(s, staticArgs_result);
        s.append(" ");
        s.append(join(subs_result, ", "));
        s.append(" ").append(right);

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forFloatLiteralExprOnly(FloatLiteralExpr that, String info) {
        return handleParen(that.getText(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forIntLiteralExprOnly(IntLiteralExpr that, String info) {
        return handleParen(that.getText(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forCharLiteralExprOnly(CharLiteralExpr that, String info) {
        return handleParen("'" + that.getText().replaceAll("\\\\", "\\\\\\\\").replaceAll("\\\"", "\\\\\"").replaceAll(
                "\\t",
                "\\\\t").replaceAll("\\n", "\\\\n") + "'", NodeUtil.isParenthesized(that));
    }

    @Override
    public String forStringLiteralExprOnly(StringLiteralExpr that, String info) {
        return handleParen("\"" + that.getText().replaceAll("\\\\", "\\\\\\\\").replaceAll("\\\"", "\\\\\"").replaceAll(
                "\\t",
                "\\\\t").replaceAll("\\n", "\\\\n") + "\"", NodeUtil.isParenthesized(that));
    }

    @Override
    public String forVoidLiteralExprOnly(VoidLiteralExpr that, String info) {
        return "()";
    }

    @Override
    public String forVarRefOnly(VarRef that, String info, String var_result, List<String> staticArgs_result) {
        if (NodeUtil.isSingletonObject(that)) {
            StringBuilder s = new StringBuilder();

            s.append(var_result);
            inOxfordBrackets(s, staticArgs_result);

            return handleParen(s.toString(), NodeUtil.isParenthesized(that));
        } else return handleParen(var_result, NodeUtil.isParenthesized(that));
    }

    @Override
    public String forFieldRefOnly(FieldRef that, String info, String obj_result, String field_result) {
        StringBuilder s = new StringBuilder();

        s.append(obj_result).append(".").append(field_result);

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forFnRefOnly(FnRef that,
                               String info,
                               List<String> staticArgs_result,
                               String originalName_result,
                               List<String> fns_result,
                               List<String> interpOverloadings_result,
                               List<String> newOverloadings_result,
                               Option<String> type_result,
                               Option<String> schema_result) {
        StringBuilder s = new StringBuilder();

        s.append(originalName_result);
        inOxfordBrackets(s, staticArgs_result);

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String for_RewriteFnRefOnly(_RewriteFnRef that,
                                       String info,
                                       String fn_result,
                                       List<String> staticArgs_result) {
        StringBuilder s = new StringBuilder();

        s.append(fn_result);
        inOxfordBrackets(s, staticArgs_result);

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forOpRefOnly(OpRef that,
                               String info,
                               List<String> staticArgs_result,
                               String originalName_result,
                               List<String> ops_result,
                               List<String> interpOverloadings_result,
                               List<String> newOverloadings_result,
                               Option<String> type_result,
                               Option<String> schema_result) {
        return handleParen(canonicalOp(originalName_result), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forOverloadingOnly(Overloading that,
                                     String info,
                                     String unambiguousName_result,
                                     String originalName_result,
                                     Option<String> type_result,
                                     Option<String> schema_result) {
        StringBuilder s = new StringBuilder();

        s.append(unambiguousName_result);
        if (type_result.isSome()) s.append(": " + type_result.unwrap());

        return "(* " + s.toString() + "*)";
    }

    @Override
    public String forJuxtOnly(Juxt that,
                              String info,
                              String multiJuxt_result,
                              String infixJuxt_result,
                              List<String> exprs_result) {
        StringBuilder s = new StringBuilder();

        if (that.isTight()) {
            if (exprs_result.isEmpty()) return bug(that,
                                                   "A tight juxtaposition expression should have " +
                                                   "at least two subexpressions.");
            Iterator<String> iter = exprs_result.iterator();
            s.append(iter.next());
            while (iter.hasNext()) {
                s.append(inParentheses(iter.next()));
            }
            if (NodeUtil.isParenthesized(that)) return "(" + s.toString() + ")";
            else return s.toString();
        } else {
            for (String expr : exprs_result) {
                s.append(expr);
                s.append(" ");
            }
            return handleParen(s.toString(), NodeUtil.isParenthesized(that));
        }
    }

    @Override
    public String for_RewriteFnAppOnly(_RewriteFnApp that,
                                       String info,
                                       String function_result,
                                       String argument_result) {
        StringBuilder s = new StringBuilder();

        s.append(function_result);
        s.append(inParentheses(argument_result));

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    private boolean isDiv(String oper) {
        String[] all = new String[]{"/", "DIV", "per"};
        List<String> div = new LinkedList<String>(java.util.Arrays.asList(all));
        return div.contains(oper);
    }

    private String operatorSpace(String oper) {
        if (oper.equals("^")) {
            return oper;
        }
        return " " + oper + " ";
    }

    private String unmangle(String name) {
        // There can be multiple $ signs in a generated name.
        // Multiple desugarings prepend $ signs to names.
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char next = name.charAt(i);
            if (next == '$') {
                result.append("fortress_");
            } else {
                result.append(next);
            }
        }
        return result.toString();
    }

    @Override
    public String forOpExprOnly(final OpExpr thatOpExpr,
                                String info,
                                final String op_result,
                                final List<String> args_result) {
        StringBuilder s = new StringBuilder();
        final FortressAstToConcrete visitor = this;

        s.append(thatOpExpr.getOp().getOriginalName().accept(new NodeDepthFirstVisitor<String>() {
            @Override
            public String forNamedOp(final NamedOp opThat) {
                final String oper = canonicalOp(opThat.getText());
                if (opThat.isEnclosing()) {
                    String op = opThat.getText();
                    String left = op.split("_")[0];
                    String right = op.substring(left.length());
                    String staticArgs = "";
                    List<StaticArg> sargs = thatOpExpr.getOp().getStaticArgs();
                    if (!sargs.isEmpty()) {
                        List<String> _sargs = new ArrayList<String>();
                        for (StaticArg sarg : sargs) {
                            _sargs.add(sarg.accept(visitor));
                        }
                        staticArgs = inOxfordBrackets(_sargs);
                    }
                    if (left.endsWith("|->")) {
                        StringBuilder s = new StringBuilder();

                        s.append(left.substring(0, left.length() - 3));
                        s.append(staticArgs);
                        s.append(" ");
                        List<Expr> exprs = thatOpExpr.getArgs();
                        if (!exprs.isEmpty()) {
                            for (Expr expr : IterUtil.skipLast(exprs)) {
                                s.append(handleMapElem(expr, thatOpExpr, visitor));
                                s.append(", ");
                            }
                            s.append(handleMapElem(IterUtil.last(exprs), thatOpExpr, visitor));
                        }
                        s.append(right);
                        return s.toString();
                    } else {
                        return (left + staticArgs + join(args_result, ", ").trim() + right);
                    }
                }

                return opThat.getFixity().accept(new NodeDepthFirstVisitor<String>() {
                    @Override
                    public String forPreFixityOnly(PreFixity that) {
                        assert (args_result.size() == 1);
                        return oper + inParentheses(args_result.get(0));
                    }

                    @Override
                    public String forPostFixityOnly(PostFixity that) {
                        assert (args_result.size() == 1);
                        return args_result.get(0) + oper;
                    }

                    @Override
                    public String forNoFixityOnly(NoFixity that) {
                        assert (args_result.isEmpty());
                        return oper;
                    }

                    @Override
                    public String forInFixityOnly(InFixity that) {
                        assert (args_result.size() == 2);
                        String result = args_result.get(0) + operatorSpace(oper) + args_result.get(1);
                        if (isDiv(oper)) {
                            return inParentheses(result);
                        } else {
                            return result;
                        }
                    }

                    @Override
                    public String forMultiFixityOnly(MultiFixity that) {
                        return join(args_result, " " + oper + " ");
                    }

                    @Override
                    public String forBigFixityOnly(BigFixity that) {
                        return oper + inParentheses(args_result);
                    }

                    @Override
                    public String forUnknownFixityOnly(UnknownFixity that) {
                        switch (args_result.size()) {
                            case 0:
                                return oper + "(* unknown fixity *)";
                            case 1:
                                return oper + inParentheses("(* unknown fixity *)" + args_result.get(0));
                            default:
                                return join(args_result, " " + oper + "(* unknown fixity *) ");
                        }
                    }
                });
            }
        }));

        return handleParen(s.toString(), NodeUtil.isParenthesized(thatOpExpr));
    }

    @Override
    public String forAmbiguousMultifixOpExprOnly(AmbiguousMultifixOpExpr that,
                                                 String info,
                                                 String infix_op_result,
                                                 String multifix_op_result,
                                                 List<String> args_result) {
        StringBuilder s = new StringBuilder();

        s.append(join(args_result, " " + infix_op_result + " "));

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forChainExprOnly(ChainExpr that,
                                   String info,
                                   String first_result,
                                   List<String> links_result,
                                   String and_result) {
        StringBuilder s = new StringBuilder();

        s.append(first_result);
        s.append(join(links_result, " "));

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forCoercionInvocationOnly(CoercionInvocation that,
                                            String info,
                                            String type_result,
                                            String arg_result) {
        StringBuilder s = new StringBuilder();

        s.append(type_result);
        s.append(".coercion");
        s.append(inParentheses(arg_result));

        return "(* " + handleParen(s.toString(), NodeUtil.isParenthesized(that)) + " *)";
    }

    @Override
    public String forMethodInvocationOnly(MethodInvocation that,
                                          String info,
                                          String obj_result,
                                          String method_result,
                                          List<String> staticArgs_result,
                                          String arg_result,
                                          Option<String> overloading_result,
                                          Option<String> schema_result) {
        StringBuilder s = new StringBuilder();

        s.append(obj_result).append(".").append(method_result);
        inOxfordBrackets(s, staticArgs_result);
        s.append(inParentheses(arg_result));

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forMathPrimaryOnly(MathPrimary that,
                                     String info,
                                     String multiJuxt_result,
                                     String infixJuxt_result,
                                     String front_result,
                                     List<String> rest_result) {
        StringBuilder s = new StringBuilder();

        s.append(front_result);
        s.append(join(rest_result, ""));

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forArrayElementOnly(ArrayElement that,
                                      String info,
                                      List<String> staticArgs_result,
                                      String element_result) {
        return handleParen(element_result, NodeUtil.isParenthesized(that));
    }

    @Override
    public String forArrayElementsOnly(ArrayElements that,
                                       String info,
                                       List<String> staticArgs_result,
                                       List<String> elements_result) {
        StringBuilder s = new StringBuilder();

        if (that.isOutermost()) {
            s.append("[");
            inOxfordBrackets(s, staticArgs_result);
            s.append(" ");
        }
        String separator;
        if (that.getDimension() == 1) separator = " ";
        else separator = makeCopies(that.getDimension() - 1, ";");
        s.append(join(elements_result, separator));
        if (that.isOutermost()) s.append(" ]");

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forSpanInfoOnly(SpanInfo info) {
        return "";
    }

    @Override
    public String forExprInfoOnly(ExprInfo info, Option<String> type) {
        return "";
    }

    @Override
    public String forTypeInfoOnly(TypeInfo that, List<String> staticParams, Option<String> whereClause) {
        return "";
    }

    @Override
    public String forDimBaseOnly(DimBase that, String info) {
        return handleParen("Unity", NodeUtil.isParenthesized(that));
    }

    @Override
    public String forDimRefOnly(DimRef that, String info, String name_result) {
        return handleParen(name_result, NodeUtil.isParenthesized(that));
    }

    @Override
    public String forDimBinaryOpOnly(DimBinaryOp that, String info, String left, String right, String op) {
        StringBuilder s = new StringBuilder();

        s.append(left);
        s.append(op);
        s.append(right);

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forDimExponentOnly(DimExponent that, String info, String base_result, String power_result) {
        StringBuilder s = new StringBuilder();

        s.append(base_result).append("^");
        s.append(power_result);

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forDimUnaryOpOnly(DimUnaryOp that, String info, String val_result, String op_result) {
        StringBuilder s = new StringBuilder();

        if (op_result.equals("square") || op_result.equals("cubic") || op_result.equals("inverse")) { // DimPrefixOp
            s.append(op_result).append(" ");
            s.append(val_result);
        } else { // DimPostfixOp
            s.append(val_result);
            s.append(op_result);
        }

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forAnyTypeOnly(AnyType that, String info) {
        return handleParen("Any", NodeUtil.isParenthesized(that));
    }

    @Override
    public String forBottomTypeOnly(BottomType that, String info) {
        return "(* BottomType *)";
    }

    @Override
    public String forVarTypeOnly(VarType that, String info, String name_result) {
        return handleParen(name_result, NodeUtil.isParenthesized(that));
    }

    @Override
    public String forTraitTypeOnly(TraitType that,
                                   String info,
                                   String name_result,
                                   List<String> args_result,
                                   List<String> params_result) {
        StringBuilder s = new StringBuilder();

        s.append(name_result);
        inOxfordBrackets(s, args_result);

        return s.toString();
    }

    @Override
    public String forTraitSelfTypeOnly(TraitSelfType that,
                                       String info,
                                       String named_result,
                                       List<String> comprised_result) {
        return named_result + " & " + inParentheses(comprised_result);
    }

    @Override
    public String forObjectExprTypeOnly(ObjectExprType that,
                                        String info,
                                        List<String> extended_result) {
        return "ObjectExprType";
    }

    @Override
    public String forArrayTypeOnly(ArrayType that, String info, String type_result, String indices_result) {
        StringBuilder s = new StringBuilder();

        s.append(type_result);
        s.append("[");
        s.append(indices_result);
        s.append("]");

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forMatrixTypeOnly(MatrixType that, String info, String type_result, List<String> dimensions) {
        StringBuilder s = new StringBuilder();

        s.append(type_result);
        s.append("^");
        if (dimensions.size() == 1) {
            s.append(dimensions.get(0));
        } else {
            s.append("(");
            s.append(join(dimensions, " BY "));
            s.append(")");
        }

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forTaggedDimTypeOnly(TaggedDimType that,
                                       String info,
                                       String type_result,
                                       String dim_result,
                                       Option<String> unit_result) {
        StringBuilder s = new StringBuilder();

        s.append(type_result);
        if (unit_result.isNone()) s.append("(");
        else s.append(" ");
        s.append(dim_result);
        if (unit_result.isSome()) {
            s.append(" in ");
            s.append(unit_result.unwrap());
        }
        if (unit_result.isNone()) s.append(")");

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forTaggedUnitTypeOnly(TaggedUnitType that, String info, String type_result, String unit_result) {
        StringBuilder s = new StringBuilder();

        s.append(type_result).append(" ");
        s.append(unit_result);

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String forArrowTypeOnly(ArrowType that,
                                   String info,
                                   String domain_result,
                                   String range_result,
                                   String effect_result,
                                   Option<String> methodInfo_result) {
        StringBuilder s = new StringBuilder();

        s.append(domain_result);
        s.append("->");
        s.append(range_result);
        s.append(effect_result);

        return handleParen(s.toString(), NodeUtil.isParenthesized(that));
    }

    @Override
    public String for_InferenceVarTypeOnly(_InferenceVarType that, String bogus, String info) {
        return "(* _InferenceVarType *)";
    }

    @Override
    public String forIntersectionTypeOnly(IntersectionType that, String info, List<String> elements_result) {
        StringBuilder s = new StringBuilder();

        s.append("(* INTERSECT(");
        s.append(join(elements_result, ", "));
        s.append(") *)");

        return s.toString();
    }

    @Override
    public String forUnionTypeOnly(UnionType that, String info, List<String> elements_result) {
        StringBuilder s = new StringBuilder();

        s.append("(* UNION(");
        s.append(join(elements_result, ", "));
        s.append(") *)");

        return s.toString();
    }

    @Override
    public String forFixedPointTypeOnly(FixedPointType that, String info, String name_result, String body_result) {
        StringBuilder s = new StringBuilder();

        s.append("(* FIX ");
        s.append(name_result);
        s.append(".");
        s.append(body_result);
        s.append(" *)");

        return s.toString();
    }

    @Override
    public String forLabelTypeOnly(LabelType that, String info) {
        return "(* LabelType *)";
    }

    @Override
    public String forTupleTypeOnly(TupleType that,
                                   String info,
                                   List<String> args_result,
                                   Option<String> varargs_result,
                                   List<String> keywords_result) {
        StringBuilder s = new StringBuilder();

        int args_size = args_result.size();
        int varargs_size = (varargs_result.isSome()) ? 1 : 0;
        int keywords_size = keywords_result.size();
        boolean inParen = (args_size + varargs_size + keywords_size > 1 || varargs_size > 0 || keywords_size > 0);
        s.append(join(args_result, ", "));
        if (varargs_size == 1) {
            if (args_size > 0) s.append(", ");
            s.append(varargs_result.unwrap());
            s.append("...");
        }
        if (keywords_size > 0) {
            if (args_size + varargs_size > 0) s.append(", ");
            s.append(join(keywords_result, ", "));
        }

        String result = s.toString();
        if (inParen) return inParentheses(result);

        else return result.equals("") ? "()" : result;
    }

    @Override
    public String forEffectOnly(Effect that, String info, Option<List<String>> throwsClause_result) {
        StringBuilder s = new StringBuilder();
        throwsClause(s, throwsClause_result);
        return s.toString();
    }

    @Override
    public String forTypeArgOnly(TypeArg that, String info, String type_result) {
        return type_result;
    }

    @Override
    public String forIntArgOnly(IntArg that, String info, String val_result) {
        return val_result;
    }

    @Override
    public String forBoolArgOnly(BoolArg that, String info, String bool_result) {
        return bool_result;
    }

    @Override
    public String forOpArgOnly(OpArg that, String info, String id_result) {
        return id_result;
    }

    @Override
    public String forDimArgOnly(DimArg that, String info, String dim_result) {
        return dim_result;
    }

    @Override
    public String forUnitArgOnly(UnitArg that, String info, String unit_result) {
        return unit_result;
    }

    @Override
    public String forIntBaseOnly(IntBase that, String info, String val_result) {
        return handleParen(val_result, that.isParenthesized());
    }

    @Override
    public String forIntRefOnly(IntRef that, String info, String name_result) {
        return handleParen(name_result, that.isParenthesized());
    }

    @Override
    public String forIntBinaryOpOnly(IntBinaryOp that,
                                     String info,
                                     String left_result,
                                     String right_result,
                                     String op_result) {
        return handleParen(left_result + op_result + right_result, that.isParenthesized());
    }

    @Override
    public String forBoolBaseOnly(BoolBase that, String info) {
        if (that.isBoolVal()) return handleParen("true", that.isParenthesized());
        else return handleParen("false", that.isParenthesized());
    }

    @Override
    public String forBoolRefOnly(BoolRef that, String info, String name_result) {
        return handleParen(name_result, that.isParenthesized());
    }

    @Override
    public String forBoolUnaryOpOnly(BoolUnaryOp that, String info, String bool_result, String op_result) {
        return handleParen(op_result + bool_result, that.isParenthesized());
    }

    @Override
    public String forBoolBinaryOpOnly(BoolBinaryOp that,
                                      String info,
                                      String left_result,
                                      String right_result,
                                      String op) {
        return handleParen(left_result + " " + op + " " + right_result, that.isParenthesized());
    }

    @Override
    public String forUnitRefOnly(UnitRef that, String info, String name_result) {
        return handleParen(name_result, that.isParenthesized());
    }

    @Override
    public String forUnitBinaryOpOnly(UnitBinaryOp that,
                                      String info,
                                      String left_result,
                                      String right_result,
                                      String op_result) {
        return handleParen(left_result + op_result + right_result, that.isParenthesized());
    }

    @Override
    public String forWhereClauseOnly(WhereClause that,
                                     String info,
                                     List<String> bindings_result,
                                     List<String> constraints_result) {
        if (bindings_result.isEmpty() && constraints_result.isEmpty()) return "";
        StringBuilder s = new StringBuilder();
        s.append("where ");
        inOxfordBrackets(s, bindings_result);
        if (!constraints_result.isEmpty()) inCurlyBraces(s, "", constraints_result);
        return s.toString();
    }

    @Override
    public String forWhereBindingOnly(WhereBinding that,
                                      String info,
                                      final String name_result,
                                      final List<String> supers_result,
                                      String kind_result) {
        return that.getKind().accept(new NodeDepthFirstVisitor<String>() {
            @Override
            public String forKindType(final KindType kind) {
                StringBuilder s = new StringBuilder();
                s.append(name_result);
                s = optCurlyBraces(s, " extends ", supers_result, "");
                return s.toString();
            }

            @Override
            public String forKindNat(final KindNat kind) {
                return "nat " + name_result;
            }

            @Override
            public String forKindInt(final KindInt kind) {
                return "int " + name_result;
            }

            @Override
            public String forKindBool(final KindBool kind) {
                return "bool " + name_result;
            }

            @Override
            public String forKindUnit(final KindUnit kind) {
                return "unit " + name_result;
            }
        });
    }

    @Override
    public String forWhereExtendsOnly(WhereExtends that, String info, String name_result, List<String> supers_result) {
        StringBuilder s = new StringBuilder();
        s.append(name_result);
        if (supers_result.isEmpty()) {
            return bug(that, "A type variable constraint declared in " + "a where clause should have its bound.");
        } else {
            s = optCurlyBraces(s, " extends ", supers_result, "");
        }
        return s.toString();
    }

    @Override
    public String forTypeAliasOnly(TypeAlias that,
                                   String info,
                                   String name_result,
                                   List<String> staticParams_result,
                                   String type_result) {
        StringBuilder s = new StringBuilder();
        s.append("type ").append(name_result);
        inOxfordBrackets(s, staticParams_result);
        s.append(" = ").append(type_result);
        return s.toString();
    }

    @Override
    public String forWhereCoercesOnly(WhereCoerces that, String info, String left_result, String right_result) {
        StringBuilder s = new StringBuilder();
        s.append(left_result);
        if (that.isWidens()) s.append(" widens ");
        if (that.isCoerces() && that.isWidens()) s.append("or");
        if (that.isCoerces()) s.append(" coerces ");
        s.append(right_result);
        return s.toString();
    }

    @Override
    public String forWhereEqualsOnly(WhereEquals that, String info, String left_result, String right_result) {
        StringBuilder s = new StringBuilder();
        s.append(left_result).append(" = ").append(right_result);
        return s.toString();
    }

    @Override
    public String forUnitConstraintOnly(UnitConstraint that, String info, String name_result) {
        StringBuilder s = new StringBuilder();
        s.append(name_result).append(" = dimensionless");
        return s.toString();
    }

    @Override
    public String forIntConstraintOnly(IntConstraint that,
                                       String info,
                                       String left_result,
                                       String right_result,
                                       String op_result) {
        StringBuilder s = new StringBuilder();
        s.append(left_result).append(op_result).append(right_result);
        return s.toString();
    }

    @Override
    public String forBoolConstraintExprOnly(BoolConstraintExpr that, String info, String constraint_result) {
        return constraint_result;
    }

    @Override
    public String forContractOnly(Contract that,
                                  String info,
                                  Option<List<String>> requires_result,
                                  Option<List<String>> ensures_result,
                                  Option<List<String>> invariants_result) {
        StringBuilder s = new StringBuilder();

        if (requires_result.isSome()) {
            List<String> requires = requires_result.unwrap();
            if (!requires.isEmpty()) inCurlyBraces(s, "requires ", requires).append("\n");
        }

        if (ensures_result.isSome()) {
            List<String> ensures = ensures_result.unwrap();
            if (!ensures.isEmpty()) inCurlyBraces(s, "ensures ", ensures).append("\n");
        }

        if (invariants_result.isSome()) {
            List<String> invariants = invariants_result.unwrap();
            if (!invariants.isEmpty()) inCurlyBraces(s, "invariant ", invariants).append("\n");
        }

        return s.toString();
    }

    @Override
    public String forEnsuresClauseOnly(EnsuresClause that, String info, String post_result, Option<String> pre_result) {
        StringBuilder s = new StringBuilder();
        s.append(post_result);
        if (pre_result.isSome()) {
            s.append(" provided ").append(pre_result);
        }
        return s.toString();
    }

    @Override
    public String forStaticParamOnly(final StaticParam that,
                                     String info,
                                     final String name_result,
                                     final List<String> extendsClause_result,
                                     final List<String> dominatesClause_result,
                                     final Option<String> dim_result,
                                     String kind_result) {
        return that.getKind().accept(new NodeDepthFirstVisitor<String>() {
            @Override
            public String forKindType(final KindType kind) {
                StringBuilder s = new StringBuilder();
                s.append(name_result);
                s = optCurlyBraces(s, " extends ", extendsClause_result, "");
                s = optCurlyBraces(s, " dominates ", dominatesClause_result, "");
                if (that.isAbsorbsParam()) {
                    s.append(" absorbs unit");
                }
                return s.toString();
            }

            @Override
            public String forKindNat(final KindNat kind) {
                return "nat " + name_result;
            }

            @Override
            public String forKindInt(final KindInt kind) {
                return "int " + name_result;
            }

            @Override
            public String forKindBool(final KindBool kind) {
                return "bool " + name_result;
            }

            @Override
            public String forKindDim(final KindDim kind) {
                return "dim " + name_result;
            }

            @Override
            public String forKindUnit(final KindUnit kind) {
                StringBuilder s = new StringBuilder();
                s.append("unit ").append(name_result);
                if (dim_result.isSome()) {
                    s.append(handleType(dim_result.unwrap()));
                }
                if (that.isAbsorbsParam()) {
                    s.append(" absorbs unit");
                }
                return s.toString();
            }

            @Override
            public String forKindOp(final KindOp kind) {
                return "opr " + name_result;
            }
        });
    }

    @Override
    public String forAPINameOnly(APIName that, String info, List<String> ids_result) {
        StringBuilder s = new StringBuilder();
        if (IterUtil.isEmpty(ids_result)) return s.toString();
        else {
            for (String id : IterUtil.skipLast(ids_result)) {
                s.append(id).append(".");
            }
            s.append(IterUtil.last(ids_result));
            return s.toString();
        }
    }

    @Override
    public String forIdOnly(Id that, String info, Option<String> api_result) {
        StringBuilder s = new StringBuilder();
        if (api_result.isSome() && !_unqualified) s.append(api_result.unwrap()).append(".");
        if (_unmangle) s.append(unmangle(that.getText()));
        else s.append(that.getText());
        return s.toString();
    }

    @Override
    public String forOpOnly(Op that, String info, Option<String> api_result, String fixity_result) {
        if (that.isEnclosing()) {
            StringBuilder s = new StringBuilder();

            String op = that.getText();
            String left = op.split("_")[0];
            String right = op.substring(left.length());

            s.append(left);
            s.append(" ");
            s.append(right.startsWith("BIG") ? right.substring(4, right.length()) : right);

            return s.toString();
        }
        return canonicalOp(that.getText());
    }

    @Override
    public String forAnonymousFnNameOnly(AnonymousFnName that, String info, Option<String> api_result) {
        return "";
    }

    @Override
    public String forConstructorFnNameOnly(ConstructorFnName that,
                                           String info,
                                           Option<String> api_result,
                                           String def_result) {
        return "(* ConstructorFnName *)";
    }

    @Override
    public String forArrayComprehensionClauseOnly(ArrayComprehensionClause that,
                                                  String info,
                                                  List<String> bind_result,
                                                  String init_result,
                                                  List<String> gens_result) {
        StringBuilder s = new StringBuilder();
        s.append(inParentheses(bind_result));
        s.append(" |-> ");
        s.append(join(gens_result, "\n"));
        return s.toString();
    }

    @Override
    public String forKeywordExprOnly(KeywordExpr that, String info, String name_result, String init_result) {
        return name_result + " = " + init_result;
    }

    @Override
    public String forCaseClauseOnly(CaseClause that,
                                    String info,
                                    String match_result,
                                    String body_result,
                                    Option<String> op_result) {
        return match_result + " => " + body_result;
    }

    @Override
    public String forCatchOnly(Catch that, String info, String name_result, List<String> clauses_result) {
        StringBuilder s = new StringBuilder();

        s.append("catch ");
        s.append(name_result).append("\n");
        s.append(join(clauses_result, "\n"));

        return s.toString();
    }

    @Override
    public String forCatchClauseOnly(CatchClause that, String info, String match_result, String body_result) {
        StringBuilder s = new StringBuilder();
        s.append(match_result);
        s.append(" => ");
        s.append(body_result);
        return s.toString();
    }

    @Override
    public String forIfClauseOnly(IfClause that, String info, String test_result, String body_result) {
        StringBuilder s = new StringBuilder();

        s.append(test_result);
        s.append(" then\n");
        increaseIndent();
        s.append(indent(body_result)).append("\n");
        decreaseIndent();

        return s.toString();
    }

    @Override
    public String forTypecaseClauseOnly(TypecaseClause that,
                                        String info,
                                        Option<String> name,
                                        String match_result,
                                        String body_result) {
        StringBuilder s = new StringBuilder();
        if (name.isSome()) s.append(name.unwrap()).append(":");
        s.append(match_result);
        s.append(" => ");
        s.append(body_result);
        return s.toString();
    }

    @Override
    public String forExtentRangeOnly(ExtentRange that,
                                     String info,
                                     Option<String> base_result,
                                     Option<String> size_result,
                                     Option<String> op_result) {
        StringBuilder s = new StringBuilder();

        if (base_result.isSome()) {
            s.append(base_result.unwrap());
        }
        if (op_result.isSome()) {
            s.append(op_result.unwrap());
        }
        if (size_result.isSome()) {
            s.append(size_result.unwrap());
        }

        return s.toString();
    }

    @Override
    public String forGeneratorClauseOnly(GeneratorClause that,
                                         String info,
                                         List<String> bind_result,
                                         String init_result) {
        StringBuilder s = new StringBuilder();

        if (!bind_result.isEmpty()) {
            if (bind_result.size() == 1) {
                s.append(bind_result.get(0));
            } else {
                s.append(inParentheses(bind_result));
            }
            s.append(" <- ");
        }
        s.append(init_result);

        return s.toString();
    }

    @Override
    public String forKeywordTypeOnly(KeywordType that, String info, String name_result, String type_result) {
        return name_result + " = " + type_result;
    }

    @Override
    public String forTraitTypeWhereOnly(TraitTypeWhere that,
                                        String info,
                                        String type_result,
                                        Option<String> where_result) {
        StringBuilder s = new StringBuilder();

        s.append(type_result);
        if (where_result.isSome()) {
            if (!where_result.unwrap().equals("")) {
                s.append(" ");
                s.append(where_result.unwrap());
            }
        }
        return s.toString();
    }

    @Override
    public String forIndicesOnly(Indices that, String info, List<String> extents_result) {
        StringBuilder s = new StringBuilder();

        s.append(join(extents_result, ", "));

        return s.toString();
    }

    @Override
    public String forParenthesisDelimitedMIOnly(ParenthesisDelimitedMI that, String info, String expr_result) {
        return inParentheses(expr_result);
    }

    @Override
    public String forNonParenthesisDelimitedMIOnly(NonParenthesisDelimitedMI that, String info, String expr_result) {
        return expr_result;
    }

    @Override
    public String forExponentiationMIOnly(ExponentiationMI that,
                                          String info,
                                          String op_result,
                                          Option<String> expr_result) {
        StringBuilder s = new StringBuilder();

        s.append(op_result);
        if (expr_result.isSome()) {
            s.append(expr_result.unwrap());
        }

        return s.toString();
    }

    @Override
    public String forSubscriptingMIOnly(SubscriptingMI that,
                                        String info,
                                        String op_result,
                                        List<String> exprs_result,
                                        List<String> staticArgs_result) {
        StringBuilder s = new StringBuilder();

        String op = that.getOp().getText();
        String left = op.substring(1).split("_")[0];
        String right = op.substring(left.length());
        s.append(left);
        inOxfordBrackets(s, staticArgs_result);
        s.append(" ");
        s.append(join(exprs_result, ", "));
        s.append(" ");
        s.append(right);

        return s.toString();
    }

    @Override
    public String forFixityOnly(Fixity that) {
        return "";
    }

    @Override
    public String forLinkOnly(Link that, String info, String op_result, String expr_result) {
        StringBuilder s = new StringBuilder();
        s.append(" ").append(op_result).append(" ").append(expr_result);
        return s.toString();
    }

    @Override
    public String forStaticParamKindOnly(StaticParamKind that) {
        return "";
    }

    @Override
    public String forDummyExprOnly(DummyExpr that, String info_result) {
        return String.format("$dummy (* %s *)", NodeUtil.getExprType(that));
    }

    @Override
    public String forMethodInfoOnly(MethodInfo that, String selfType_result) {
        return "";
    }

    @Override
    public String forPatternOnly(Pattern that, String info, Option<String> name, String patterns) {
        return "Pattern @ " + NodeUtil.getSpan((TypeOrPattern)that) +
               "\n    name=" + name + " patterns=" + patterns;
    }

    @Override
    public String forPatternArgsOnly(PatternArgs that, String info, List<String> patterns) {
        return "PatternArgs @ " + NodeUtil.getSpan(that) +
               "\n    patterns=" + patterns;
    }

    @Override
    public String forPlainPatternOnly(PlainPattern that, String info, Option<String> field,
                                      String name, Option<String> type) {
        return "PlainPattern name=" + name + " idType=" + type;
    }

    @Override
    public String forTypePatternOnly(TypePattern that, String info, Option<String> field, String type) {
        return "TypePattern " + type;
    }

    @Override
    public String forNestedPatternOnly(NestedPattern that, String info, Option<String> field, String pat) {
        return "NestedPattern " + pat;
    }
}
