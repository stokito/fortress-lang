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

package com.sun.fortress.compiler.optimization;

import java.util.HashMap;
import java.util.List;

import com.sun.fortress.useful.Bits;

abstract public class Unbox {
    /*
     * Cases for unboxing:
     * 
     * 1) primitive.  ZZ{8,16,32,64}, RR{32,64}, Boolean
     * 2) singleton (non-generic) object.
     * 3) unboxed fields
     * 4) comprises unboxed
     * 
     */
    
    abstract public int bitWidth();
    public String javaRep() {
        int b = bitWidth();
        if (b == 0)
            return "V";
        if (b <= 8)
            return "B";
        if (b <= 16)
            return "S";
        if (b <= 32)
            return "I";
        
        String s = "J";
        while (b > 64) {
            b = b - 64;
            s = s + "J";
        }
        return s;
    }
    
    public static class TaggedUnbox extends Unbox {
        final Unbox item;
        final Object tag;
        public TaggedUnbox(Object tag, Unbox item) {
            this.item = item;
            this.tag = tag;
        }
        @Override
        public int bitWidth() {
            return item.bitWidth();
        }
        @Override
        public String javaRep() {
            return item.javaRep();
        }
        public Object tag() {
            return tag;
        }
    }
    
    public static class ZZ extends Unbox {
        final int width;
        
        public ZZ(int n) {
            width = n;
            if (n == 8) {
            } else if (n == 16) {
            } else if (n == 32) {
            } else if (n == 64) {
            } else throw new IllegalArgumentException("Expected n in {8,16,32,64}, not " + n);
        }

        @Override
        public int bitWidth() {
            return width;
        }

    }

    public static class RR extends Unbox {
        final int width;
        
        public RR(int n) {
            width = n;
            if (n == 32) {
            } else if (n == 64) {
            } else throw new IllegalArgumentException("Expected n in {8,16,32,64}, not " + n);
        }

        @Override
        public int bitWidth() {
            return width;
        }
        public String javaRep() {
            return width == 32 ? "F" : "D";
        }

    }

    
    public static class Singleton extends Unbox {
        
        public Singleton() {
        }
        
        @Override
        public int bitWidth() {
            return 0;
        }

        @Override
        public String javaRep() {
            return "V";
        }
    }
    
    public static class Comprises extends Unbox {
        final int tagWidth;
        final int maxChoicesWidth;
        final TaggedUnbox[] tagToItem;
        final HashMap<Object, Integer> itemToTag =
            new HashMap<Object, Integer>();
       
        public Comprises(List<TaggedUnbox> choices) {
            int l = choices.size();
            tagToItem = new TaggedUnbox[l];
            
            tagWidth = Bits.ceilLogTwo(l);
            int max = 0;
            int i = 0;
            for (TaggedUnbox u : choices) {
                tagToItem[i] = u;
                itemToTag.put(u.tag(), i);
                i++;
                
                int s = u.bitWidth();
                if (s > max)
                    max = s;
            }
            maxChoicesWidth = max;
        }

        @Override
        public int bitWidth() {
            // TODO Auto-generated method stub
            return tagWidth + maxChoicesWidth;
        }
        
        /* Need container type, container index, shift, mask */
        /* Need tag type, tag index, shift, mask */
        
    }
    
    public static class Contains extends Unbox {
        final int bitWidth;
        final HashMap<Object, Integer> itemToOffset =
            new HashMap<Object, Integer>();
        
        public Contains(List<TaggedUnbox> members) {
            int sum = 0;
            for (TaggedUnbox u : members) {
                itemToOffset.put(u.tag(), sum);
                sum += u.bitWidth();
            }
            bitWidth = sum;
        }

        @Override
        public int bitWidth() {
            return bitWidth;
        }
    }
    
}
