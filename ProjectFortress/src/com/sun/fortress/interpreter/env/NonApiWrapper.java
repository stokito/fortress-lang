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
package com.sun.fortress.interpreter.env;

import java.util.HashMap;
import java.util.List;

import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;

public class NonApiWrapper extends CUWrapper {

    public NonApiWrapper(Component comp,
            HashMap<String, NonApiWrapper> linker, String[] implicitLibs) {
        super(comp, linker, implicitLibs);
        // TODO Auto-generated constructor stub
    }

    public NonApiWrapper(Api api, HashMap<String, NonApiWrapper> linker,
            String[] implicitLibs) {
        super(api, linker, implicitLibs);
        // TODO Auto-generated constructor stub
    }

    public NonApiWrapper(Component comp, APIWrapper api,
            HashMap<String, NonApiWrapper> linker, String[] implicitLibs) {
        super(comp, api, linker, implicitLibs);
    }

    public NonApiWrapper(Component comp, List<APIWrapper> api_list,
            HashMap<String, NonApiWrapper> linker, String[] implicitLibs) {
        super(comp, api_list, linker, implicitLibs);
    }

    public NonApiWrapper(APIWrapper apicw,
            HashMap<String, NonApiWrapper> linker, String[] implicitLibs) {
        super(apicw, linker, implicitLibs);
    }

}
