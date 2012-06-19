/*******************************************************************************
 Copyright 2009,2011, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package fortress;

import com.sun.fortress.compiler.runtimeValues.FValue;
import com.sun.fortress.compiler.runtimeValues.RTTI;
import com.sun.fortress.runtimeSystem.Naming;

public class AnyType {
    public static interface Any {
        public RTTI getRTTI();
        
        public static class RTTIc extends RTTI implements RTTIi {
            public RTTIc(Class javaRep) {
                super(javaRep);
            }
            
            public String className() {
                String className = "fortress.AnyType$Any";
                String deMangle = Naming.demangleFortressIdentifier(className);
                String noDots = Naming.dotToSep(deMangle);
                return noDots;  
            } 
            
            public static final RTTI ONLY = new RTTIc(AnyType.Any.class);
        }
        public static interface RTTIi { }
        public abstract static class DefaultTraitMethods extends FValue{ }
    }
}
