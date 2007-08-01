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

package com.sun.fortress.compiler;

import java.util.Map;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;

import edu.rice.cs.plt.iter.IterUtil;

/**
 * Eliminates ambiguities in an AST that can be resolved solely by knowing what kind
 * of entity a name refers to.  This class specifically handles the following:
 * <ul>
 * <li>All names referring to APIs are made fully qualified.
 * <li>VarRefs referring to functions become FnRefs with (possibly unknown) static arguments
 *     filled in.</li>
 * <li>Assignments to VarRefs referring to setters become SetterInvocations.</li>
 * <li>VarRefs referring to getters become GetterInvocations.</li>
 * <li>VarRefs referring to methods juxtaposed with Exprs become MethodInvocations.</li>
 * <li>GetterInvocations referring to methods juxtaposed with Exprs become MethodInvocations.</li>
 * <li>FunctionRefs juxtaposed with Exprs become MethodInvocations.</li>
 * </ul>
 * 
 * Additionally, all name references that are undefined or used incorrectly are treated
 * as static errors.
 */
public class Disambiguator {
  
  public static class ApiResult extends StaticPhaseResult {
    private final Map<String, ApiIndex> _apis;
    public ApiResult(Map<String, ApiIndex> apis, Iterable<? extends StaticError> errors) {
      super(errors);
      _apis = apis;
    }
    public Map<String, ApiIndex> apis() { return _apis; }
  }
  
  /**
   * Disambiguate the given apis. To support circular references, the apis should appear in
   * the given environment.
   */
  public static ApiResult disambiguateApis(Map<String, ApiIndex> apis, GlobalEnvironment env) {
    // TODO: implement
    return new ApiResult(apis, IterUtil.<StaticError>empty());
  }

  
  public static class ComponentResult extends StaticPhaseResult {
    private final Map<String, ComponentIndex> _components;
    public ComponentResult(Map<String, ComponentIndex> components,
                           Iterable<? extends StaticError> errors) {
      super(errors);
      _components = components;
    }
    public Map<String, ComponentIndex> components() { return _components; }
  }

  /** Disambiguate the given components. */
  public static ComponentResult disambiguateComponents(Map<String, ComponentIndex> components,
                                                       GlobalEnvironment env) {
    // TODO: implement
    return new ComponentResult(components, IterUtil.<StaticError>empty());
  }
  
}
