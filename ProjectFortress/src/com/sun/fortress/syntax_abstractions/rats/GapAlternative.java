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

package com.sun.fortress.syntax_abstractions.rats;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import xtc.parser.Action;
import xtc.parser.Binding;
import xtc.parser.Element;
import xtc.parser.Module;
import xtc.parser.ModuleImport;
import xtc.parser.ModuleInstantiation;
import xtc.parser.ModuleList;
import xtc.parser.ModuleName;
import xtc.parser.NonTerminal;
import xtc.parser.OrderedChoice;
import xtc.parser.Production;
import xtc.parser.ProductionOverride;
import xtc.parser.Sequence;
import xtc.parser.SequenceName;
import xtc.parser.StringLiteral;

public class GapAlternative {

	private Set<String> modules;

	public GapAlternative() {
		this.modules = new HashSet<String>();
	}

	public OrderedChoice getAlternative(Production p) {
		if (p instanceof ProductionOverride) {
			return p.choice;
		}
		if (null != p.choice) {
			List<Element> elms = new LinkedList<Element>();
			elms.add(new StringLiteral("<ThisShouldBeUniqueEnough<"));
			elms.add(new Binding("a", new NonTerminal("Id")));
			elms.add(new StringLiteral(">ThisShouldBeUniqueEnough>"));
			elms.add(new Action(getCode(), getIndents()));
			List<Sequence> ls = new LinkedList<Sequence>();
			ls.add(new Sequence(new SequenceName("Gap"), elms));
			ls.addAll(p.choice.alternatives);
			return new OrderedChoice(ls);
		}
		return null;
	}

	private List<Integer> getIndents() {
		List<Integer> ls = new LinkedList<Integer>();
		ls.add(3);
		return ls;
	}

	private List<String> getCode() {
		List<String> ls = new LinkedList<String>();
		ls.add("yyValue = New TerminalGap(createSpan(yyStart,yyCount), a);");
		return ls;
	}

	public void apply(Collection<Module> modules, Module fortress) {
		for (Module m: modules) {
			for (Production p: m.productions) {
				this.modules.add(m.name.name);
				p.choice = this.getAlternative(p);
			}
			if (m != fortress) {
				ModuleName idName = new ModuleName("Identifier");
				if (null == m.parameters) {
					m.parameters = new ModuleList(new LinkedList<ModuleName>());
				}
				if (!m.parameters.names.contains(idName)) {
					RatsUtil.addParameterToInstantiation(fortress, m.name.name, idName);
					List<ModuleName> ls = new LinkedList<ModuleName>();
					ls.addAll(m.parameters.names);
					ls.add(idName);
					m.parameters = new ModuleList(ls);
					m.dependencies.add(new ModuleImport(idName));
				}
			}
		}

	}
}
