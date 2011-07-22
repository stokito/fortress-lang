/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.phases;

import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.rats.RatsUtil;
import com.sun.fortress.useful.Debug;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionUnwrapException;

import java.util.LinkedList;
import java.util.List;

/*
 * Collect transformer names
 */
public class RewriteTransformerNames extends NodeUpdateVisitor {

    private Option<String> api;
    private Option<String> grammar;
    private Option<String> productionName;
    // private Option<List<NonterminalParameter>> parameters;

    public RewriteTransformerNames() {
        this.api = Option.none();
        this.grammar = Option.none();
    }

    @Override
    public Node forApi(Api that) {
        api = Option.some(that.getName().toString());
        return super.forApi(that);
    }

    @Override
    public Node forGrammarDecl(GrammarDecl that) {
        grammar = Option.some(that.getName().toString().replace('.', '_'));
        return super.forGrammarDecl(that);
    }

    @Override
    public Node forNonterminalDef(NonterminalDef that) {
        productionName = Option.some(that.getName().getText());
        Node result = super.forNonterminalDef(that);
        productionName = Option.none();
        return result;
    }

    @Override
    public Node forNonterminalExtensionDef(NonterminalExtensionDef that) {
        productionName = Option.some(that.getName().getText());
        Node result = super.forNonterminalExtensionDef(that);
        productionName = Option.none();
        return result;
    }

    /* These names might need to be consistently created,
     * rather than use RatsUtil.getFreshName.
     * Also, at this point, the grammar is fully qualifid
     * so the output is something like api_api_grammar_name.
     * If this ever matters then take off the prefix:
     * api.unwrap() + "_"
     */
    private String transformationName(String name) {
        return api.unwrap() + "_" + grammar.unwrap() + "_" + RatsUtil.getFreshName(name + "Transformer");
    }

    @Override
    public Node forPreTransformerDefOnly(PreTransformerDef that, ASTNodeInfo info, Transformer transformer) {
        try {
            Debug.debug(Debug.Type.SYNTAX, 1, "Found a pre-transformer " + productionName.unwrap());
            String name = transformationName(productionName.unwrap());
            List<NonterminalParameter> params = new LinkedList<NonterminalParameter>();
            return new NamedTransformerDef(NodeFactory.makeSpanInfo(NodeFactory.makeSpan(
                    "RewriteTransformerNames.forPreTransformerDefOnly")), name, params, transformer);
        }
        catch (OptionUnwrapException e) {
            throw new MacroError("Somehow got to a pretransformer node but api/grammar/parameters wasn't set", e);
        }
    }
}
