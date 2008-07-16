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

/*
 * Class for collecting names and imports for a given component.
 * Used in the Fortress com.sun.fortress.compiler.Fortress.
 */

package com.sun.fortress.syntax_abstractions.parser;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AliasedSimpleName;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.nodes.TemplateNodeDepthFirstVisitor_void;

/**
 *
 */
public class ImportedApiCollector extends TemplateNodeDepthFirstVisitor_void {

    private boolean isTopLevel;
    private GlobalEnvironment env;
    private Collection<GrammarIndex> grammars;
    private LinkedList<CompilationUnit> worklist;
    private Set<APIName> seen;
    private boolean importsTopLevelGrammars;

    public ImportedApiCollector(GlobalEnvironment env) {
        this.env = env;
        this.isTopLevel = true;
        this.worklist = new LinkedList<CompilationUnit>();
        this.seen = new HashSet<APIName>();
        this.grammars = new LinkedList<GrammarIndex>();
        this.importsTopLevelGrammars = false;
    }

    public void collectApis(CompilationUnit c) {
        this.worklist.add(c);
        while (!this.worklist.isEmpty()) {
            this.worklist.removeFirst().accept(this);
            this.isTopLevel = false;
        }
    }

    @Override
    public void forImportApiOnly(ImportApi that) {
        bug(that, errorMsg("NYI 'Import api APIName'; ",
                           "try 'import APIName.{...}' instead."));
//        for (AliasedAPIName apiAlias : that.getApis()) {
//            if (env.definesApi(apiAlias.getApi())) {
//                ApiIndex api = env.api(apiAlias.getApi());
//                for (GrammarIndex g: api.grammars().values()) {
//                    if (this.isTopLevel) importsTopLevelGrammars = true;
//                    g.isToplevel(this.isTopLevel);
//                    grammars.add(g);
//                }
//                addImportsToWorklist(apiAlias.getApi());
//            }
//            else {
//                StaticError.make("Undefined api: "+apiAlias.getApi(), that);
//            }
//        }
    }

    @Override
    public void forImportStarOnly(ImportStar that) {
        if (env.definesApi(that.getApi())) {
            APIName api = that.getApi();
            for (GrammarIndex g: env.api(api).grammars().values()) {
                if (!that.getExcept().contains(g.getName())) {
                    if (this.isTopLevel) importsTopLevelGrammars = true;
                    g.isToplevel(this.isTopLevel);
                    grammars.add(g);
                }
            }
            addImportsToWorklist(that.getApi());
        }
        else {
            StaticError.make("Undefined api: "+that.getApi(), that);
        }
    }


    @Override
    public void forImportNamesOnly(ImportNames that) {
        if (env.definesApi(that.getApi())) {
            ApiIndex api = env.api(that.getApi());
            boolean foundSome = false;
            for (AliasedSimpleName aliasedName: that.getAliasedNames()) {
                if (aliasedName.getName() instanceof Id) {
                    Id importedName = (Id) aliasedName.getName();
                    if (api.grammars().containsKey(importedName.getText())) {
                        foundSome = true;
                        GrammarIndex g = api.grammars().get(importedName.getText());
                        g.isToplevel(this.isTopLevel);
                        if (this.isTopLevel) importsTopLevelGrammars = true;
                    }
                }
            }
            grammars.addAll(api.grammars().values());
            if (foundSome) {
                addImportsToWorklist(that.getApi());
            }
        }
        else {
            StaticError.make("Undefined api: "+that.getApi(), that);
        }
    }

    /**
     * @param that
     */
    private void addImportsToWorklist(APIName api) {
        if (!seen.contains(api)) {
            seen.add(api);
            this.worklist.add(env.api(api).ast());
        }
    }

    public Collection<GrammarIndex> getGrammars() {
        return this.grammars;
    }

    public boolean importsTopLevelGrammars() {
        return this.importsTopLevelGrammars;
    }
}
