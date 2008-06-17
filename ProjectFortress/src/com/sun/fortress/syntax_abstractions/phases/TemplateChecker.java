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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TemplateGap;
import com.sun.fortress.nodes._TerminalDef;
import com.sun.fortress.syntax_abstractions.environments.GrammarEnv;
import com.sun.fortress.syntax_abstractions.environments.MemberEnv;
import com.sun.fortress.syntax_abstractions.environments.SyntaxDeclEnv;
import com.sun.fortress.syntax_abstractions.intermediate.SyntaxSymbolPrinter;

import edu.rice.cs.plt.tuple.Option;

/*
 * Check that
 * 1) The number of formal and actual parameters are the same
 * 2) The asttype of the actual parameters is the same as the asttype of the formal parameters
 * 3) Rewrite pattern variables to fresh names (which are legal Java identifers)   
 */
public class TemplateChecker extends NodeUpdateVisitor {

    public static class Result extends StaticPhaseResult {
        private Api api;

        public Result(Api api, 
                Collection<StaticError> errors) {
            super(errors);
            this.api = api;
        }

        public Api api() { return api; }
    }

    private Collection<StaticError> errors;
    private SyntaxDeclEnv currentSyntaxDeclEnv;
    private MemberEnv currentMemberEnv;
    
    public TemplateChecker() {
        this.errors = new LinkedList<StaticError>();
    }

    private Collection<StaticError> getErrors() {
        return this.errors;
    }

    private boolean isSuccessfull() {
        return this.errors.isEmpty();
    }
    
    public static Result checkTemplates(Api api) {
        TemplateChecker templateChecker = new TemplateChecker();
        Api a = (Api) api.accept(templateChecker);
        if (!templateChecker.isSuccessfull()) {
            return new Result(a, templateChecker.getErrors());
        }
        return new Result(api, templateChecker.getErrors());
    }

    @Override
    public Node forNonterminalDef(NonterminalDef that) {
        this.currentMemberEnv = GrammarEnv.getMemberEnv(that.getHeader().getName());
        return super.forNonterminalDef(that);
    }

    @Override
    public Node forNonterminalExtensionDef(NonterminalExtensionDef that) {
        this.currentMemberEnv = GrammarEnv.getMemberEnv(that.getHeader().getName());
        return super.forNonterminalExtensionDef(that);
    }
    
    @Override
    public Node for_TerminalDef(_TerminalDef that) {
        this.currentMemberEnv = GrammarEnv.getMemberEnv(that.getHeader().getName());
        return super.for_TerminalDef(that);
    }

    @Override
    public Node forSyntaxDef(SyntaxDef that) {
        Option<SyntaxDeclEnv> os = this.currentMemberEnv.getSyntaxDeclEnv(that);
        if (os.isNone()) {
            String sd = "";
            for (SyntaxSymbol ss: that.getSyntaxSymbols()) {
                sd += ss.accept(new SyntaxSymbolPrinter());
            }
            throw new RuntimeException("Could not find a member environment for "+sd +" in "+this.currentMemberEnv.getName());
        }
        this.currentSyntaxDeclEnv = os.unwrap();
        return super.forSyntaxDef(that);
    }

//    Waiting for the new Astgen interface
//    @Override
//    public Node forTemplateGap(TemplateGap that) {
//        handleTemplateGap(that);
//        return super.forTemplateGap(that);
//    }

    /*
     * 1) The number of formal and actual parameters are the same
     * 2) The asttype of the actual parameters is the same as the asttype of the formal parameters
     * 3) TODO: Rewrite pattern variables to fresh names (which are legal Java identifers)
     */
    private void handleTemplateGap(TemplateGap gap) {
        Id nonterminalName = this.currentSyntaxDeclEnv.getNonterminalName(gap.getId());
        MemberEnv memberEnv = GrammarEnv.getMemberEnv(nonterminalName);
        List<Id> formalParams = memberEnv.getParameters();
        List<Id> actualParams = gap.getTemplateParams();
        
        // 1) The number of formal and actual parameters are the same
        if (formalParams.size() != actualParams.size()) {
            this.errors.add(StaticError.make("Mismatch between number of arguments: "+gap.getId(), gap));
            return;
        }
        
        // 2) The asttype of the actual parameters is the same as the asttype of the formal parameters
        boolean error = false;
        String formalArgs = "";
        String actualArgs = "";
        for (int inx=0; inx < formalParams.size(); inx++) {
            Id formalParamVar = formalParams.get(inx);
            Id formalParamNonterminal = memberEnv.getParameter(formalParamVar);
            Id actualParamVar = actualParams.get(inx);
            Id actualParamNonterminal = this.currentSyntaxDeclEnv.getNonterminalName(actualParamVar);
            if (!formalParamNonterminal.equals(actualParamNonterminal)) {
                error = true;
            }
            formalArgs += formalParamNonterminal.toString();
            actualArgs += actualParamNonterminal.toString();
            if (inx <= formalParams.size()-1) {
                formalArgs += ", ";
                actualArgs += ", ";
            }
        }
        if (error) {
            String msg = "Nonterminal %s(%s) cannot be applied to (%s)";
            msg = String.format(msg, gap.getId(), formalArgs, actualArgs);
            this.errors.add(StaticError.make(msg, gap));
        }
    }
}
