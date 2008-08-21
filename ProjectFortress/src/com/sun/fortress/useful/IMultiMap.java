package com.sun.fortress.useful;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public interface IMultiMap<K, V> extends Map<K, Set<V>> {

    public void addInverse(Map<V, K> m);
    
    public Set<V> putItem(K k, V v);
    
    public Set<V> putItems(K k, Set<V> vs);
    
    public Set<V> removeItem(K k, V v);
    
    public final static IMultiMap EMPTY_MULTIMAP = new IMultiMap() {
        private <T> T error() { throw new IllegalStateException("Empty IMultiMap is immutable."); }
        public void addInverse(Map m) { error(); }
        public Set putItem(Object k, Object v) { return error(); }
        public Set putItems(Object k, Set vs) { return error(); }
        public Set removeItem(Object k, Object v) { return error(); }
        public void clear() { error(); }
        public boolean containsKey(Object arg0) { return false; }
        public boolean containsValue(Object arg0) { return false; }
        public Set entrySet() { return Collections.emptySet(); }
        public Object get(Object arg0) { return null; }
        public boolean isEmpty() { return false; }
        public Set keySet() { return Collections.emptySet(); }
        public Object put(Object arg0, Object arg1) { return error(); }
        public void putAll(Map arg0) { error(); }
        public Object remove(Object arg0) { return error(); }
        public int size() { return 0; }
        public Collection values() { return Collections.emptyMap().values(); }
    };
}
