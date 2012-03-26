/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public interface IMultiMap<K, V> extends Map<K, Set<V>> {

    public void addInverse(Map<V, K> m);

    public Set<V> putItem(K k, V v);

    public Set<V> putItems(K k, Collection<V> vs);

    public Set<V> removeItem(K k, V v);

    public Set<V> putKey(K k);
    
    public Set<V> getEmptyIfMissing(K k);

    public Set<V> removeItemAllowEmpty(K k, V v);

    public final static IMultiMap EMPTY_MULTIMAP = new IMultiMap() {
        private <T> T error() {
            throw new IllegalStateException("Empty IMultiMap is immutable.");
        }

        public void addInverse(Map m) {
            error();
        }

        public Set putKey(Object k) {
            return error();
        }

        public Set putItem(Object k, Object v) {
            return error();
        }

        public Set putItems(Object k, Collection vs) {
            return error();
        }

        public Set removeItem(Object k, Object v) {
            return error();
        }

        public void clear() {
            error();
        }

        public boolean containsKey(Object arg0) {
            return false;
        }

        public boolean containsValue(Object arg0) {
            return false;
        }

        public Set entrySet() {
            return Collections.emptySet();
        }

        public Object get(Object arg0) {
            return null;
        }

        public boolean isEmpty() {
            return false;
        }

        public Set keySet() {
            return Collections.emptySet();
        }

        public Object put(Object arg0, Object arg1) {
            return error();
        }

        public void putAll(Map arg0) {
            error();
        }

        public Object remove(Object arg0) {
            return error();
        }

        public int size() {
            return 0;
        }

        public Collection values() {
            return Collections.emptyMap().values();
        }

        @Override
        public Set getEmptyIfMissing(Object k) {
            return Collections.emptySet();
        }

        @Override
        public Set removeItemAllowEmpty(Object k, Object v) {
            return null;
        }
    };
}
