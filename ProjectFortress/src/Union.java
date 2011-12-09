/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

import com.sun.fortress.compiler.runtimeValues.RTTI;
import com.sun.fortress.runtimeSystem.Naming;

/*
 * Created on Oct 25, 2011
 *
 */

public class Union {
    public static class RTTIc extends RTTI {
        private final RTTI a;
        private final RTTI b;
        private volatile String lazyClassName;
        
        public static RTTI factory(RTTI a, RTTI b) {
            // Is this where we canonicalize?
            return new RTTIc(a, b);
        }
        
        public RTTIc(RTTI a, RTTI b) {
            super(RTTIc.class); // urk!!
            this.a = a;
            this.b = b;
        }
        
        public boolean runtimeSupertypeOf(RTTI other) {
            return a.runtimeSupertypeOf(other) ||
            b.runtimeSupertypeOf(other);
        }
        
        public String toString() {
            return getSN() + ": Union[\\" + a.toString() + ", " + b.toString() + "\\]";
        }
        
        public String className() {
            if (lazyClassName == null)
                synchronized (this) {
                    if (lazyClassName == null) {
                String className = Naming.UNION + Naming.LEFT_OXFORD + 
                                    a.className() + Naming.GENERIC_SEPARATOR +
                                    b.className() + Naming.RIGHT_OXFORD;
                
                // Pretty sure this is demangled already.
                // String deMangle = Naming.demangleFortressIdentifier(className);
                // String noDots = Naming.dotToSep(deMangle);
                lazyClassName = className;
               }
            }
            return lazyClassName;
        }


    }
}
