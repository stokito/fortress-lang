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

package com.sun.fortress.syntax_abstractions.parser;

import java.io.File;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.Parser;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;

import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AliasedAPIName;
import com.sun.fortress.nodes.AliasedSimpleName;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Export;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportedNames;
import com.sun.fortress.nodes.ImportStar;

import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.TemplateNodeDepthFirstVisitor_void;

import com.sun.fortress.exceptions.StaticError;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

import com.sun.fortress.useful.Debug;

/**
 * Methods to parse a file to a collection of API ASTs which define
 * the syntax used in the file.
 */
public class PreParser {

    /** Returns the list of grammars directly-imported by the component.
     * (For an api, returns an empty collection.)
     */
    public static List<GrammarIndex> parse(APIName api_name, File f, GlobalEnvironment env) {
        CompilationUnit cu = Parser.preparseFileConvertExn(api_name, f);
        if (cu instanceof Component) {
            Component c = (Component) cu;
//             ImportedApiCollector collector = new ImportedApiCollector(env);
//             collector.collectApis(c);
//             List<GrammarIndex> result = collector.getGrammars();
            List<GrammarIndex> result = getImportedGrammars(c, env);
            if (!result.isEmpty()) {
                Debug.debug(Debug.Type.SYNTAX, "Component: ", c.getName(), " imports grammars...");
            }
            return result;
        } else {
            return new LinkedList<GrammarIndex>();
        }
    }

    private static List<GrammarIndex> getImportedGrammars(Component c, final GlobalEnvironment env) {
        final List<GrammarIndex> grammars = new ArrayList<GrammarIndex>();
        c.accept(new TemplateNodeDepthFirstVisitor_void() {
                @Override public void forImportApiOnly(ImportApi that) {
                    bug(that, errorMsg("NYI 'Import api APIName'; ",
                                       "try 'import APIName.{...}' instead."));
                }

                @Override public void forImportStarOnly(ImportStar that) {
                    if (env.definesApi(that.getApi())) {
                        APIName api = that.getApi();
                        for (GrammarIndex g: env.api(api).grammars().values()) {
                            if (!that.getExcept().contains(g.getName())) {
                                grammars.add(g);
                            }
                        }
                    } else {
                        StaticError.make("Undefined api: "+that.getApi(), that);
                    }
                }

                @Override public void forImportNamesOnly(ImportNames that) {
                    if (env.definesApi(that.getApi())) {
                        ApiIndex api = env.api(that.getApi());
                        for (AliasedSimpleName aliasedName: that.getAliasedNames()) {
                            if (aliasedName.getName() instanceof Id) {
                                Id importedName = (Id) aliasedName.getName();
                                if (api.grammars().containsKey(importedName.getText())) {
                                    grammars.add(api.grammars().get(importedName.getText()));
                                }
                            }
                        }
                    } else {
                        StaticError.make("Undefined api: "+that.getApi(), that);
                    }
                }
            });
        return grammars;
    }
}
