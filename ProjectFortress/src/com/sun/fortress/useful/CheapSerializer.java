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

package com.sun.fortress.useful;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

abstract public class CheapSerializer<T> {

    public abstract void write(OutputStream o, T data) throws IOException;
    public abstract T read(InputStream i) throws IOException;
    public abstract void version(OutputStream o) throws IOException;
    public abstract void version(InputStream o) throws IOException, VersionMismatch;
    
    protected static void check(InputStream o, byte[] v) throws IOException, VersionMismatch {
        byte[] b = new byte[v.length];
        int n = o.read(b);
        if (n < v.length)
            throw new VersionMismatch("Expected " + new String(v) + ", got (short) " + new String(b,0,n));
        for (int i = 0; i < n; i++)
            if (b[i] != v[i])
                throw new VersionMismatch("Expected " + new String(v) + ", got " + new String(b,0,n));

    }
    
    public void writeInt(OutputStream o, int data) throws IOException {
        java.lang.String s = java.lang.String.valueOf(data);
        byte[] b1 = s.getBytes();
        o.write(b1);
        o.write(' ');
    }
    
    public int readInt(InputStream i) throws IOException {
        int x = i.read();
        int sum = 0;
        while (x != -1 && x != ' ') {
            if ('0' > x || x > '9') {
                throw new NumberFormatException();
            }
            sum = sum * 10 + (x - '0');
            x = i.read();
        }
        return sum;
    }
    
    public void writeLong(OutputStream o, long data) throws IOException {
        java.lang.String s = java.lang.String.valueOf(data);
        byte[] b1 = s.getBytes();
        o.write(b1);
        o.write(' ');
    }
    
    public long readLong(InputStream i) throws IOException {
        int x = i.read();
        long sum = 0;
        while (x != -1 && x != ' ') {
            if ('0' > x || x > '9') {
                throw new NumberFormatException();
            }
            sum = sum * 10 + (x - '0');
            x = i.read();
        }
        return sum;
    }
    
    static public CheapSerializer<java.lang.String> STRING =
        new CheapSerializer<java.lang.String>() {
        
        @Override
        public java.lang.String read(InputStream i) throws IOException {
            int n = readInt(i);
            byte[] b = new byte[n];
            int m = i.read(b);
            if (m != n)
                throw new EOFException("input ended early");
            m = i.read();
            if (m == -1)
                throw new EOFException("input ended early");
            if (m != ' ')
                throw new RuntimeException("Expected ' ', got '" + (char) m + "'");
            return new String(b);
        }

        @Override
        public void write(OutputStream o, java.lang.String data) throws IOException {
            byte[] b2 = data.getBytes();
            writeInt(o, b2.length);
            o.write(b2);
            o.write(' ');
        }

         byte[] V = {'S', 'T', 'R', 'I', 'N', 'G', '_', '1', '.', '0', ' '};
        
        @Override
        public void version(OutputStream o) throws IOException {
            o.write(V);
        }

        @Override
        public void version(InputStream o) throws IOException, VersionMismatch {
            // TODO Auto-generated method stub
            check(o,V);
        }
        
    };
    
    static public CheapSerializer<Integer> INTEGER =
        new CheapSerializer<Integer>() {

        byte[] V = {'I', 'N', 'T', 'E', 'G', 'E', 'R', '_', '1', '.', '0', ' '};
        
        @Override
        public void version(OutputStream o) throws IOException {
            o.write(V);
        }

        @Override
        public void version(InputStream o) throws  IOException, VersionMismatch {
            // TODO Auto-generated method stub
            check(o,V);
        }
           @Override
            public Integer read(InputStream i) throws IOException {
                return new Integer(readInt(i));
            }

            @Override
            public void write(OutputStream o, Integer data) throws IOException {
                writeInt(o, data.intValue());
            }
        
    };

    
    static public CheapSerializer<Long> LONG =
        new CheapSerializer<Long>() {

        byte[] V = {'L', 'O', 'N', 'G', '_', '1', '.', '0', ' '};
        
        @Override
        public void version(OutputStream o) throws IOException {
            o.write(V);
        }

        @Override
        public void version(InputStream o) throws IOException, VersionMismatch {
            // TODO Auto-generated method stub
            check(o,V);
        }
           @Override
            public Long read(InputStream i) throws IOException {
                return new Long(readLong(i));
            }

            @Override
            public void write(OutputStream o, Long data) throws IOException {
                writeLong(o, data.longValue());
            }
        
    };

    
    static public class MAP<K,V> extends CheapSerializer<Map<K,V>> {
        
        private CheapSerializer<K> keys;
        private CheapSerializer<V> values;

        public MAP(CheapSerializer<K> keys, CheapSerializer<V> values) {
            this.keys = keys;
            this.values = values;
        }
        
        
        @Override
        public Map<K, V> read(InputStream i) throws IOException {
            int n = readInt(i);
            HashMap<K,V> m = new HashMap<K,V>();
            for (int j = 0; j < n; j++) {
                K k = keys.read(i);
                V v = values.read(i);
                m.put(k,v);
            }
            return m;
        }

        @Override
        public void write(OutputStream o, Map<K, V> data) throws IOException {
            Set<Entry<K, V>> s = data.entrySet();
            int n = s.size();
            writeInt(o, n);
            for (Entry<K,V> e : s) {
                keys.write(o, e.getKey());
                values.write(o, e.getValue());
            }
        }

        byte[] V = {'M', 'A', 'P', '_', '1', '.', '0', ' '};

        @Override
        public void version(OutputStream o) throws IOException {
            o.write(V);
            keys.version(o);
            values.version(o);
            
        }


        @Override
        public void version(InputStream o) throws IOException, VersionMismatch {
            check(o,V);
            keys.version(o);
            values.version(o);
            
        }
        
    }


}
