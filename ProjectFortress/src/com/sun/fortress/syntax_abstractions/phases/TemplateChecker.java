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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.CompilationUnitIndex;
import com.sun.fortress.compiler.typechecker.ConstraintFormula;
import com.sun.fortress.compiler.typechecker.TraitTable;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.NonterminalHeader;
import com.sun.fortress.nodes.NonterminalParameter;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TemplateGap;
import com.sun.fortress.nodes.TemplateUpdateVisitor;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes._TerminalDef;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.environments.GrammarEnv;
import com.sun.fortress.syntax_abstractions.environments.MemberEnv;
import com.sun.fortress.syntax_abstractions.environments.SyntaxDeclEnv;
import com.sun.fortress.syntax_abstractions.intermediate.SyntaxSymbolPrinter;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil;

import edu.rice.cs.plt.tuple.Option;

/*
 * Check that
 * 1) The number of formal and actual parameters are the same
 * 2) The asttype of the actual parameters is the same as the asttype of the formal parameters
 * 3) Rewrite pattern variables to fresh names (which are legal Java identifers)   
 */
public class TemplateChecker extends TemplateUpdateVisitor {

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
    private GlobalEnvironment GLOBAL_ENV;
    private CompilationUnitIndex currentApiIndex;
    private Map<Id, Id> patternVariablesAndArguments;

    public TemplateChecker(GlobalEnvironment globalEnv, CompilationUnitIndex currentApiIndex) {
        this.errors = new LinkedList<StaticError>();
        this.GLOBAL_ENV = globalEnv;
        this.currentApiIndex = currentApiIndex;
        this.patternVariablesAndArguments = new HashMap<Id, Id>();
    }

    private Collection<StaticError> getErrors() {
        return this.errors;
    }

    private boolean isSuccessfull() {
        return this.errors.isEmpty();
    }

    public static Result checkTemplates(GlobalEnvironment globalEnv, ApiIndex apiIndex, Api api) {
        TemplateChecker templateChecker = new TemplateChecker(globalEnv, apiIndex);
        Api a = (Api) api.accept(templateChecker);
        if (!templateChecker.isSuccessfull()) {
            return new Result(a, templateChecker.getErrors());
        }
        return new Result(api, templateChecker.getErrors());
    }

    @Override
    public Node forNonterminalHeader(NonterminalHeader that) {
        this.currentMemberEnv = GrammarEnv.getMemberEnv(that.getName());
        for (NonterminalParameter np: that.getParams()) {
            Id oldName = np.getName();
            Id freshName = freshId(oldName);
            this.patternVariablesAndArguments.put(oldName, freshName);
        }
        return super.forNonterminalHeader(that);
    }

    @Override
    public Node forSyntaxDef(SyntaxDef that) {
        Option<SyntaxDeclEnv> os = this.currentMemberEnv.getSyntaxDeclEnv(that);
        if (os.isNone()) {
            String sd = "";
            for (SyntaxSymbol ss: that.getSyntaxSymbols()) {
                sd += ss.accept(new SyntaxSymbolPrinter());
            }
            throw new MacroError("Could not find a member environment for "+sd +" in "+this.currentMemberEnv.getName());
        }
        this.currentSyntaxDeclEnv = os.unwrap();
        return super.forSyntaxDef(that);
    }

    @Override
    public Node forPrefixedSymbol(PrefixedSymbol that) {
        Id oldName = that.getId().unwrap();
        Id freshName = freshId(oldName);
        this.patternVariablesAndArguments.put(oldName, freshName);
        return super.forPrefixedSymbol(that);
    }

    private Id freshId(Id oldName) {
        return NodeFactory.makeId(FreshName.getFreshName("$"+oldName.getText()+"$"));
    }

    /*
     * 1) The number of formal and actual parameters are the same
     * 2) The asttype of the actual parameters is the same as the asttype of the formal parameters
     * 3) TODO: Rewrite pattern variables to fresh names (which are legal Java identifers)
     */
    @Override
    public Node forTemplateGapOnly(TemplateGap that, Id id_result, List<Id> params_result) {
//        System.err.println("handling: "+that.getId());
               
        if ((this.currentSyntaxDeclEnv.isCharacterClass(that.getGapId()) ||
             this.currentSyntaxDeclEnv.isAnyChar(that.getGapId()) ||
             this.currentSyntaxDeclEnv.getMemberEnv().isParameter(that.getGapId())) 
             &&
             !that.getTemplateParams().isEmpty()) {
            String error = String.format("%s is not applicable",that.getGapId().toString());
            this.errors.add(StaticError.make(error, that));
            return that;
        }

        if (!that.getTemplateParams().isEmpty()) {
            String errorMsg = "The nonterminal %1s is not applicable for the arguments (%2s), expected (%3s)";
            MemberEnv memberEnv = GrammarEnv.getMemberEnv(this.currentSyntaxDeclEnv.getNonterminalName(that.getGapId())); 
            List<Id> formalParams = memberEnv.getParameters();
            List<Id> actualParams = that.getTemplateParams();

            // 1) The number of formal and actual parameters are the same
            if (formalParams.size() != actualParams.size()) {
                String formals = getFormalParametersList(memberEnv, formalParams);
                String actuals = getActualParametersList(actualParams);
                String error = String.format(errorMsg, that.getGapId(), actuals, formals);
                this.errors.add(StaticError.make(error, that));
            }

            // 2) The asttype of the actual parameters is the same as the asttype of the formal parameters
            boolean isError = false;
            String formals = "";
            String actuals = "";
            for (int inx=0; inx < formalParams.size(); inx++) {
                Id formalParamVar = formalParams.get(inx);
                BaseType formalParamType = memberEnv.getParameterType(formalParamVar);
                Id actualParamVar = actualParams.get(inx);
                BaseType actualParamType = this.currentSyntaxDeclEnv.getType(actualParamVar);

//                System.err.println(formalParamType);
//                System.err.println(actualParamType);

                TypeAnalyzer ta = new TypeAnalyzer(new TraitTable(currentApiIndex, GLOBAL_ENV));
                ConstraintFormula subtype = ta.subtype(actualParamType, formalParamType);
//                System.err.println(subtype.toString());
                if (!ConstraintFormula.TRUE.equals(subtype)) {
                    isError = true;
                }
                formals += formalParamType.toString();
                actuals += actualParamType.toString();
                if (inx < formalParams.size()-1) {
                    formals += ", ";
                    actuals += ", ";
                }
            }
            if (isError) {
                String error = " "+String.format(errorMsg, that.getGapId().toString(), actuals, formals);
                this.errors.add(StaticError.make(error, that));
            }
        }
        
        if (!this.patternVariablesAndArguments.containsKey(that.getGapId())) {
            throw new MacroError("Unknown pattern variable: "+that.getGapId());
        }

//        Id freshName = this.patternVariables.get(gap.getId());
//        gap.setId(freshName);
        return that;
    }

    private String getActualParametersList(List<Id> actualParams) {
        Iterator<Id> it;
        String actuals = "";
        it = actualParams.iterator();
        while (it.hasNext()) {
            Id param = it.next();
            actuals += GrammarEnv.getType(this.currentSyntaxDeclEnv, param);
            if (it.hasNext()) {
                actuals += ", ";
            }
        }
        return actuals;
    }

    private String getFormalParametersList(MemberEnv memberEnv, List<Id> formalParams) {
        String formals = "";
        Iterator<Id> it = formalParams.iterator();
        while (it.hasNext()) {
            Id param = it.next();
            formals += memberEnv.getParameterType(param);
            if (it.hasNext()) {
                formals += ", ";
            }
        }
        return formals;
    }

}
