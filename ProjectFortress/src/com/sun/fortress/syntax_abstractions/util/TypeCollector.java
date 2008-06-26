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

package com.sun.fortress.syntax_abstractions.util;

import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.nodes.AnyCharacterSymbol;
import com.sun.fortress.nodes.BackspaceSymbol;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.BreaklineSymbol;
import com.sun.fortress.nodes.CarriageReturnSymbol;
import com.sun.fortress.nodes.CharSymbol;
import com.sun.fortress.nodes.CharacterClassSymbol;
import com.sun.fortress.nodes.FormfeedSymbol;
import com.sun.fortress.nodes.GroupSymbol;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NewlineSymbol;
import com.sun.fortress.nodes.NoWhitespaceSymbol;
import com.sun.fortress.nodes.TabSymbol;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.KeywordSymbol;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.OptionalSymbol;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.RepeatOneOrMoreSymbol;
import com.sun.fortress.nodes.RepeatSymbol;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.environments.GrammarEnv;
import com.sun.fortress.syntax_abstractions.environments.MemberEnv;

public class TypeCollector extends NodeDepthFirstVisitor<BaseType> {

    private TypeCollector() {}

    public static BaseType getType(PrefixedSymbol ps) {
        return ps.getSymbol().accept(new TypeCollector());
    }

    @Override
    public BaseType defaultCase(Node that) {
        throw new RuntimeException("Unexpected case: "+that.getClass());
    }

    @Override
    public BaseType forOptionalSymbol(OptionalSymbol that) {
        return handle(that.getSymbol(), SyntaxAbstractionUtil.FORTRESSLIBRARY, SyntaxAbstractionUtil.MAYBE);
    }

    @Override
    public BaseType forRepeatOneOrMoreSymbol(RepeatOneOrMoreSymbol that) {
        return handle(that.getSymbol(), SyntaxAbstractionUtil.LIST, SyntaxAbstractionUtil.LIST);
    }

    @Override
    public BaseType forRepeatSymbol(RepeatSymbol that) {
        return handle(that.getSymbol(), SyntaxAbstractionUtil.LIST, SyntaxAbstractionUtil.LIST);
    }

    @Override
    public BaseType forGroupSymbol(GroupSymbol that) {
        // TODO Auto-generated method stub
        return super.forGroupSymbol(that);
    }

    @Override
    public BaseType forGroupSymbolOnly(GroupSymbol that, List<BaseType> symbols_result) {
        // TODO Auto-generated method stub
        return super.forGroupSymbolOnly(that, symbols_result);
    }

    @Override
    public BaseType forNonterminalSymbol(NonterminalSymbol that) {
        if (!GrammarEnv.contains(that.getNonterminal())) {
            throw new RuntimeException("Grammar environment does not contain identifier: "+that.getNonterminal());
        }
        MemberEnv memberEnv = GrammarEnv.getMemberEnv(that.getNonterminal());
        return memberEnv.getAstType();
    }

    @Override
    public BaseType forKeywordSymbol(KeywordSymbol that) {
        Id string = NodeFactory.makeId(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.STRING);
        return new TraitType(string);
    }

    @Override
    public BaseType forTokenSymbol(TokenSymbol that) {
        Id string = NodeFactory.makeId(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.STRING);
        return new TraitType(string);
    }

    @Override
    public BaseType forCharacterClassSymbol(CharacterClassSymbol that) {
        Id string = NodeFactory.makeId(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.CHAR);
        return new TraitType(string);
    }

    @Override
    public BaseType forAnyCharacterSymbol(AnyCharacterSymbol that) {
        Id string = NodeFactory.makeId(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.CHAR);
        return new TraitType(string);
    }

    @Override
    public BaseType forBackspaceSymbol(BackspaceSymbol that) {
        Id string = NodeFactory.makeId(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.STRING);
        return new TraitType(string);
    }

    @Override
    public BaseType forBreaklineSymbol(BreaklineSymbol that) {
        Id string = NodeFactory.makeId(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.STRING);
        return new TraitType(string);
    }

    @Override
    public BaseType forCarriageReturnSymbol(CarriageReturnSymbol that) {
        Id string = NodeFactory.makeId(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.STRING);
        return new TraitType(string);
    }

    @Override
    public BaseType forCharSymbol(CharSymbol that) {
        Id string = NodeFactory.makeId(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.STRING);
        return new TraitType(string);
    }

    @Override
    public BaseType forFormfeedSymbol(FormfeedSymbol that) {
        Id string = NodeFactory.makeId(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.STRING);
        return new TraitType(string);
    }

    @Override
    public BaseType forNewlineSymbol(NewlineSymbol that) {
        Id string = NodeFactory.makeId(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.STRING);
        return new TraitType(string);
    }

    @Override
    public BaseType forNoWhitespaceSymbol(NoWhitespaceSymbol that) {
        Id string = NodeFactory.makeId(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.STRING);
        return new TraitType(string);
    }

    @Override
    public BaseType forTabSymbol(TabSymbol that) {
        Id string = NodeFactory.makeId(SyntaxAbstractionUtil.FORTRESSBUILTIN, SyntaxAbstractionUtil.STRING);
        return new TraitType(string);
    }

    private BaseType handle(SyntaxSymbol symbol, String api, String id) {
        BaseType type = symbol.accept(this);
        Id list = NodeFactory.makeId(api, id);
        List<StaticArg> args = new LinkedList<StaticArg>();
        args.add(new TypeArg(type));
        return new TraitType(list, args);
    }


}
