/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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

import xtc.parser.Module;
import xtc.tree.Attribute;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class FortressRatsGrammar {

    private static final String TEMPLATEPARSER = "com.sun.fortress.parser.templateparser.";
    private HashMap<String, Module> map;

    public FortressRatsGrammar() {
        this.map = new HashMap<String, Module>();
    }

    private static class RatsFilenameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return name.endsWith(".rats");
        }
    }

    /**
     * Initialize the Fortress Rats! grammar by loading and parsing the modules.
     *
     * @param srcDir
     */
    public void initialize(String srcDir) {
        File f = new File(srcDir);
        String[] ls = f.list(new RatsFilenameFilter());
        for (String s : ls) {
            String name = s.substring(0, s.length() - 5);
            String filename = srcDir + File.separatorChar + s;
            if (new File(filename).isFile()) {
                this.map.put(TEMPLATEPARSER + name, RatsUtil.getRatsModule(filename));
            }
        }
    }

    public void replace(Collection<Module> modules) {
        for (Module m : modules) {
            this.map.put(m.name.name, m);
        }
    }

    /**
     * Assumes that initialize has been called.
     */
    public void setName(String name) {
        Module m = this.map.get(TEMPLATEPARSER + "TemplateParser");
        List<Attribute> attrs = new LinkedList<Attribute>();
        for (Attribute attribute : m.attributes) {
            if (attribute.getName().equals("parser")) {
                attrs.add(new Attribute("parser", TEMPLATEPARSER + name));
            } else {
                attrs.add(attribute);
            }
        }
        //attrs.add(new Attribute("verbose"));
        m.attributes = attrs;
    }

    public void clone(String targetDir) {
        for (Module m : this.map.values()) {
            RatsUtil.writeRatsModule(m, targetDir);
        }
    }
}
