/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.env;

import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;

import java.util.HashMap;
import java.util.List;

public class NonApiWrapper extends CUWrapper {

    public NonApiWrapper(Component comp, HashMap<String, NonApiWrapper> linker, String[] implicitLibs) {
        super(comp, linker, implicitLibs);
        // TODO Auto-generated constructor stub
    }

    public NonApiWrapper(Api api, HashMap<String, NonApiWrapper> linker, String[] implicitLibs) {
        super(api, linker, implicitLibs);
        // TODO Auto-generated constructor stub
    }

    public NonApiWrapper(Component comp,
                         List<APIWrapper> api_list,
                         HashMap<String, NonApiWrapper> linker,
                         String[] implicitLibs) {
        super(comp, api_list, linker, implicitLibs);
    }

    // called from ForeignComponentWrapper
    protected NonApiWrapper(APIWrapper apicw, HashMap<String, NonApiWrapper> linker, String[] implicitLibs) {
        super(apicw, linker, implicitLibs);
    }

}
