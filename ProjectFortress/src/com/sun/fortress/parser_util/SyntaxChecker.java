/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.parser_util;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * A visitor that checks syntactic restrictions:
 * 1) A declaration in an API should not have any missing types.
 * 2) An operator method that is not a subscripting operator method
 * nor a subscripted assignment operator method
 * should have the self parameter.
 * 3) Valid modifiers for traits, objects, functionals, variables
 * in components and APIs.
 */
public final class SyntaxChecker extends NodeDepthFirstVisitor_void {
    private boolean inComponent = false;
    private boolean inApi = false;
    private boolean inTrait = false;
    private boolean inObject = false;
    private boolean inBlock = false;
    private BufferedWriter writer;
    private Set<APIName> onDemands = new HashSet<APIName>();
    private Map<APIName, Set<AliasedSimpleName>> imports = new HashMap<APIName, Set<AliasedSimpleName>>();

    public SyntaxChecker(BufferedWriter in_writer) {
        writer = in_writer;
    }

    private void log(Node that, String message) {
        try {
            if (!(that instanceof ASTNode)) bug(that, "Only ASTNodes are supported.");
            writer.write(NodeUtil.getSpan((ASTNode) that) + ":\n    " + message + "\n");
        }
        catch (IOException error) {
            error("Writing to a log file for the syntax checker failed!");
        }
    }

    public void forImportStar(ImportStar that) {
        APIName name = that.getApiName();
        if (onDemands.contains(name)) log(that,
                                          "There should be at most one on-demand import statement " +
                                          "from a single API.");
        else onDemands.add(name);
        if (that.getForeignLanguage().isSome()) log(that, "Foreign language imports are not allowed to have {...}.");
    }

    public void forImportNames(ImportNames that) {
        APIName name = that.getApiName();
        List<AliasedSimpleName> names = that.getAliasedNames();
        Set<AliasedSimpleName> thisNames = new HashSet<AliasedSimpleName>(names.size());
        thisNames.addAll(names);
        if (imports.keySet().contains(name) && imports.get(name).equals(thisNames)) log(that,
                                                                                        "No two import statements should be identical.");
        else imports.put(name, thisNames);
    }

    public void forComponent(Component that) {
        inComponent = true;
        super.forComponent(that);
        inComponent = false;
    }

    public void forComponentOnly(Component that) {
        try {
            writer.close();
        }
        catch (IOException error) {
            error("Closing a log file for the syntax checker failed!");
        }
    }

    public void forApi(Api that) {
        inApi = true;
        super.forApi(that);
        inApi = false;
    }

    public void forApiOnly(Api that) {
        try {
            writer.close();
        }
        catch (IOException error) {
            error("Closing a log file for the syntax checker failed!");
        }
    }

    private void ruleOutDominates(TraitObjectDecl that) {
    	
    	List<StaticParam> ls = that.getHeader().getStaticParams();
    	
    	for (StaticParam sp: ls) {
    		
    		if (!sp.getDominatesClause().isEmpty()) {
    			log(that, "Declaration " + NodeUtil.getName(that) + " must not declare a dominating static parameter.");
    		}
    		
    	}
    	
    }
    
    public void forTraitDecl(TraitDecl that) {
        inTrait = true;
        Modifiers mods = NodeUtil.getMods(that);
        ruleOutDominates(that);
        if (!Modifiers.TraitMod.containsAll(mods)) {
            log(that, mods.remove(Modifiers.TraitMod) + " cannot modify a trait, " + NodeUtil.getName(that));
        }
        if (inApi && mods.isPrivate()) {
            log(that, "Private trait " + NodeUtil.getName(that) + " must not appear in an API.");
        }
        super.forTraitDecl(that);
        inTrait = false;
    }

    public void forObjectDecl(ObjectDecl that) {
        inObject = true;
        Modifiers mods = NodeUtil.getMods(that);
        ruleOutDominates(that);
        if (!Modifiers.ObjectMod.containsAll(mods)) {
            log(that, mods.remove(Modifiers.ObjectMod) + " cannot modify an object, " + NodeUtil.getName(that));
        }
        if (inApi && mods.isPrivate()) {
            log(that, "Private object " + NodeUtil.getName(that) + " must not appear in an API.");
        }
        if (NodeUtil.getParams(that).isSome()) {
            for (Param p : NodeUtil.getParams(that).unwrap()) {
                Modifiers m = p.getMods();
                if (NodeUtil.isMutable(p) && p.getIdType().isNone()) log(p,
                                                                         "The type of " + p.getName() +
                                                                         " is required.");
                if (inComponent) {
                    if (!Modifiers.ParamFldMod.containsAll(m)) log(p,
                                                                   m.remove(Modifiers.ParamFldMod) +
                                                                   " cannot modify an object parameter, " +
                                                                   p.getName());
                } else {
                    if (!Modifiers.ApiFldMod.containsAll(m)) log(p,
                                                                 m.remove(Modifiers.ApiFldMod) +
                                                                 " cannot modify an object parameter, " + p.getName());
                }
            }
        } else { // singleton object
            if (NodeUtil.getContract(that).isSome()) log(that,
                                                         "Singleton object " + NodeUtil.getName(that) +
                                                         " must not have a contract.");
        }

        super.forObjectDecl(that);
        inObject = false;
    }

    public void forObjectExpr(ObjectExpr that) {
        inObject = true;
        super.forObjectExpr(that);
        inObject = false;
    }

    public void forLocalVarDeclOnly(LocalVarDecl that) {
        for (LValue lvb : that.getLhs()) {
            // variable declaration without a body expression or
            if (that.getRhs().isNone() || lvb.isMutable()) { // a mutable variable
                if (lvb.getIdType().isNone()) // type is required
                    log(lvb, "The type of " + lvb.getName() + " is required.");
            }
        }

        Modifiers mods = NodeUtil.getMods(writer, that);
        if (!Modifiers.LocalVarMod.containsAll(mods)) log(that,
                                                          mods.remove(Modifiers.LocalVarMod) +
                                                          " cannot modify local variables.");
    }

    public void forVarDeclOnly(VarDecl that) {
        Modifiers mods = NodeUtil.getMods(writer, that);

        for (LValue lvb : that.getLhs()) {
            // variable declaration without a body expression or
            if (that.getInit().isNone() || lvb.isMutable()) { // a mutable variable
                if (lvb.getIdType().isNone()) // type is required
                    log(lvb, "The type of " + lvb.getName() + " is required.");
            }
            // Top-level _ declarations are allowed only in components
            // not in objects/traits/APIs.
            if (lvb.getName().getText().equals("_") && (inApi || inTrait || inObject)) log(lvb,
                                                                                           "Fields or top-level declarations in APIs " +
                                                                                           "cannot be named '_'.");
            // All hidden field declarations in traits are settable.
            if (inTrait && mods.isHidden() && (!mods.isSettable())) log(lvb,
                                                                        "Hidden field declarations in traits should be settable.");
        }

        if (inComponent) {
            if (inTrait) {
                if (!Modifiers.AbsFldMod.containsAll(mods)) log(that,
                                                                mods.remove(Modifiers.AbsFldMod) +
                                                                " cannot modify fields in a trait in a component.");
            } else if (inObject) {
                if (!Modifiers.FldMod.containsAll(mods)) log(that,
                                                             mods.remove(Modifiers.FldMod) +
                                                             " cannot modify fields in an object in a component.");
            } else { // top-level variable declaration
                if (!Modifiers.VarMod.containsAll(mods)) log(that,
                                                             mods.remove(Modifiers.VarMod) +
                                                             " cannot modify top-level variables in a component.");
            }
        } else { // in API
            if (inTrait || inObject) {
                if (!Modifiers.ApiFldMod.containsAll(mods)) log(that,
                                                                mods.remove(Modifiers.ApiFldMod) +
                                                                " cannot modify fields in an API.");
            } else { // top-level variable declaration
                if (!Modifiers.AbsVarMod.containsAll(mods)) log(that,
                                                                mods.remove(Modifiers.AbsVarMod) +
                                                                " cannot modify top-level variables in an API.");
            }
        }
    }

    private String fixityToString(Fixity fixity) {
        if (fixity instanceof InFixity) return "infix";
        else if (fixity instanceof PreFixity) return "prefix";
        else if (fixity instanceof PostFixity) return "postfix";
        else if (fixity instanceof NoFixity) return "nofix";
        else if (fixity instanceof MultiFixity) return "multifix";
        else return "other fixity";
    }

    public void forFnDeclOnly(FnDecl that) {
        boolean hasBody = that.getBody().isSome();
        Modifiers mods = NodeUtil.getMods(that);
        IdOrOpOrAnonymousName name = NodeUtil.getName(that);
        List<Param> params = NodeUtil.getParams(that);

        if (name instanceof IdOrOp) {
            // TODO: check there is no top-level operator definition for SEQV
            //       when we have a better magic
            if ((inTrait || inObject) && ((IdOrOp) name).getText().equals("===")) log(that,
                                                                                      "The object equivalence operator, SEQV, should not be overridden.");
            if (name instanceof Op) { // operator declaration
                Op op = (Op) name;
                Fixity fix = op.getFixity();
                if ((fix instanceof InFixity || fix instanceof MultiFixity) && NodeUtil.hasKeywordParam(params)) log(
                        that,
                        "An infix/multifix operator declaration must not have any keyword parameters.");
                if (op.getText().equals("^") && !(fix instanceof InFixity)) log(that,
                                                                                "The operator, ^, is always an infix operator but it is declared to be " +
                                                                                fixityToString(fix) + ".");
                if (fix instanceof PostFixity) {
                    if (params.size() != 1) log(that, "A postfix operator declaration must have one value parameter.");
                    else {
                        Param p = params.get(0);
                        if (NodeUtil.isKeywordParam(p) || NodeUtil.isVarargsParam(p)) log(that,
                                                                                          "A postfix operator declaration should not " +
                                                                                          "have a keyword parameter or a varargs parameter.");
                    }
                }
            }
        }

        if (mods.isGetter()) {
            if (!params.isEmpty()) log(that, "Getter declaration should not have a parameter.");
        } else if (mods.isSetter()) {
            // Is this really true?  What if we have a tuple-typed setter?
            if (!(params.size() == 1)) log(that, "Setter declaration should have a single parameter.");
        }

        if (inBlock) { // local function declaration
            if (!Modifiers.LocalFnMod.containsAll(mods)) {
                log(that, mods.remove(Modifiers.LocalFnMod) + " cannot modify a local function, " + name);
            }
            if (inObject && !hasBody) {
                log(that, "Object method " + name + " lacks a body.");
            }
        } else if (inTrait || inObject) {
            if (!Modifiers.MethodMod.containsAll(mods)) {
                log(that, mods.remove(Modifiers.MethodMod) + " cannot modify a method, " + name);
            }
            if (inComponent) {
                if (inObject && !hasBody) {
                    log(that, "Object method " + name + " lacks a body.");
                }
                if (mods.isAbstract() && hasBody) {
                    log(that, "Method " + name + " is concrete, but declared abstract.");
                }
            } else {
                if (mods.isPrivate()) {
                    log(that, "private cannot modify a method " + name + " in an API.");
                }
            }
        } else { // top-level function declaration
            if (!Modifiers.FnMod.containsAll(mods)) {
                log(that, mods.remove(Modifiers.FnMod) + " cannot modify a function, " + name);
            }
            if (!inComponent) {
                if (mods.isPrivate()) {
                    log(that, "private cannot modify a function " + name + " in an API.");
                }
            }
        }

        boolean isOprMethod = false;
        if ((inTrait || inObject) && (name instanceof Op)) {
            isOprMethod = (!(((Op) name).isEnclosing())) || ((Op) name).getText().equals("| |");
        }

        boolean isCoercion = name instanceof Id && ((Id) name).getText().equals("coerce");
        if (isCoercion && inComponent && !hasBody) log(that, "The coercion body is required.");

        boolean hasSelf = false;
        if ((!hasBody) && NodeUtil.getReturnType(that).isNone() && !isCoercion) log(that,
                                                                                    "The return type of " + name +
                                                                                    " is required.");

        for (Param p : params) {
            if (p.getName().getText().equals("self")) hasSelf = true;
            if ((!hasBody) && (!NodeUtil.isVarargsParam(p)) && p.getIdType().isNone() && !p.getName().getText().equals(
                    "self")) log(p, "The type of " + p.getName() + " is required.");
        }
        if (isOprMethod && !hasSelf) log(that, "An operator method " + name + " should have the self parameter.");
    }

    public void forBlock(Block that) {
        inBlock = true;
        super.forBlock(that);
        inBlock = false;
    }

}
