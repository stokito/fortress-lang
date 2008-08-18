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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.lang.reflect.Method;

import xtc.parser.ParseError;
import xtc.parser.SemanticValue;

import com.sun.fortress.compiler.Parser;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.exceptions.ParserError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.Export;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.NonterminalHeader;
import com.sun.fortress.nodes.NonterminalParameter;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.TemplateGap;
import com.sun.fortress.nodes.TemplateGapExpr;
import com.sun.fortress.nodes.Transformer;
import com.sun.fortress.nodes.NamedTransformerDef;
import com.sun.fortress.nodes.PreTransformerDef;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.syntax_abstractions.environments.GapEnv;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionUnwrapException;

/*
 * Collect transformer names
 */
public class RewriteTransformerNames extends NodeUpdateVisitor {

    // private List<String> names;
    private Option<String> api;
    private Option<String> grammar;
    private Option<String> productionName;
    private Option<List<NonterminalParameter>> parameters;

    public RewriteTransformerNames(){
        // this.names = new ArrayList<String>();
        this.api = Option.none();
        this.grammar = Option.none();
    }

    @Override public Node forApi(Api that) {
        api = Option.some(that.getName().toString());
        return super.forApi(that);
    }
    
    @Override public Node forGrammarDef(GrammarDef that) {
        grammar = Option.some(that.getName().toString().replace( '.', '_' ));
        return super.forGrammarDef(that);
    }

    /* These names might need to be consistently created, rather than use FreshName
     * Also, at this point the grammar is fully qualifid so the output is something
     * like api_api_grammar_name. If this ever matters then take off the first
     * api.unwrap() + "_"
     */
    private String transformationName( String name ){
        return api.unwrap() + "_" + grammar.unwrap() + "_" + FreshName.getFreshName( name + "Transformer" );
    }

    @Override public Node forNonterminalHeader(NonterminalHeader that) {
        parameters = Option.some(that.getParams());
        productionName = Option.some(that.getName().getText());
        Node result = super.forNonterminalHeader(that);
        /*
        parameters = Option.none();
        productionName = Option.none();
        */
        return result;
    }

    @Override public Node forPreTransformerDefOnly(PreTransformerDef that, Transformer transformer) {
        try{
            Debug.debug( Debug.Type.SYNTAX, 1, "Found a pre-transformer " + productionName.unwrap());
            String name = transformationName(productionName.unwrap());
            return new NamedTransformerDef( name, parameters.unwrap(), transformer );
        } catch ( OptionUnwrapException e ){
            throw new MacroError( "Somehow got to a pretransformer node but api/grammar/parameters wasn't set", e );
        }
    }
}
