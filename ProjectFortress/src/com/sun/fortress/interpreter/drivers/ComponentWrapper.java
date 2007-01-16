/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.drivers;
import java.util.List;
import java.util.Map;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.nodes.Component;
import com.sun.fortress.interpreter.nodes.DottedId;
import com.sun.fortress.interpreter.nodes.Import;
import com.sun.fortress.interpreter.rewrite.Disambiguate;


public class ComponentWrapper {
    Component p;
    BuildEnvironments be;
    Disambiguate dis;
    Map<String, ComponentWrapper> componentDict;
    int visitState;
    private final static int UNVISITED=0, POPULATED=1, FINISHED=2;

    public ComponentWrapper(Component comp, Map<String, ComponentWrapper> dictionary) {
        BetterEnv e = BetterEnv.empty();
        e.installPrimitives();
        be = new BuildEnvironments(e);
        dis = new Disambiguate();
        componentDict = dictionary;
        p = comp;
    }

    public boolean populated() {
        return visitState > UNVISITED;
    }

    public boolean finished() {
        return visitState == FINISHED;
    }

    public boolean populatedNotFinished() {
        return visitState == POPULATED;
    }

    public void populateEnvironment() {
        if (visitState != UNVISITED)
            return;

        visitState = POPULATED;
        p = (Component) dis.visit(p); // Rewrites p!
                                      // Caches information in dis!

//        List<Import> imports = p.getImports();
//        for (Import i : imports) {
//            DottedId d = i.getSource();
//            ComponentWrapper cw = componentDict.get(d.toString());
//            cw.populateEnvironment();
//        }

        be.forComponent1(p);


    }

    public void finishEnvironment() {
        if (visitState == POPULATED) {
            visitState = FINISHED;

            // This is not good enough; we need thunk the initializations
//             List<Import> imports = p.getImports();
//             for (Import i : imports) {
//                 DottedId d = i.getSource();
//                 ComponentWrapper cw = componentDict.get(d.toString());
//                 cw.finishEnvironment();
//             }

            be.forComponentDefs(p);
            dis.registerObjectExprs(be.getEnvironment());

        } else if (visitState == UNVISITED)
            throw new IllegalStateException("Must be populated before finishing");
    }

    public Environment getEnvironment() {
        return be.getEnvironment();
    }

}
