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
package com.sun.fortress.interpreter.env;

import java.util.HashMap;
import java.util.List;

import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;

public class APIWrapper extends CUWrapper {

    public APIWrapper(Component comp, HashMap<String, ComponentWrapper> linker,
            String[] implicitLibs) {
        super(comp, linker, implicitLibs);
        // TODO Auto-generated constructor stub
    }

    public APIWrapper(Api api, HashMap<String, ComponentWrapper> linker,
            String[] implicitLibs) {
        super(api, linker, implicitLibs);
        // TODO Auto-generated constructor stub
    }

    public APIWrapper(Component comp, CUWrapper api,
            HashMap<String, ComponentWrapper> linker, String[] implicitLibs) {
        super(comp, api, linker, implicitLibs);
        // TODO Auto-generated constructor stub
    }

    public APIWrapper(Component comp, List<CUWrapper> api_list,
            HashMap<String, ComponentWrapper> linker, String[] implicitLibs) {
        super(comp, api_list, linker, implicitLibs);
        // TODO Auto-generated constructor stub
    }

}
