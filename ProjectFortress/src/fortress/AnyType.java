/*******************************************************************************
 Copyright 2009,2011, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package fortress;

import com.sun.fortress.compiler.runtimeValues.FValue;
import com.sun.fortress.compiler.runtimeValues.RTTI;

public class AnyType {
    public static interface Any {
        public RTTI getRTTI();
        
        public static class RTTIc extends RTTI implements RTTIi {
            public RTTIc(Class javaRep) {
                super(javaRep);
            }

            public static final RTTI ONLY = new RTTIc(AnyType.Any.class);
        }
        public static interface RTTIi { }
        public abstract static class DefaultTraitMethods extends FValue{ }
    }
}
