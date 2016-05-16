/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * This is like a normal HashMap, but it only stores up to MAX_ENTRIES entries,
 * and will discard the oldest entry when a new one is added if the current size
 * equals MAX_ENTRIES
 * @author jpeebles
 */
public class BufferedHashMap<K,V> extends HashMap<K,V> {
    private LinkedList<K> keyQueue = new LinkedList<K>();
    private final int MAX_ENTRIES;

    public BufferedHashMap(int bufferSize) {
        super();
        this.MAX_ENTRIES=bufferSize;
    }

    @Override
    public V put(K key, V value) {
        super.put(key, value);
        keyQueue.offer(key);
        maintainSize();
        return value;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        super.putAll(m);
        maintainSize();
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public V remove(Object key) {
        V value = super.remove(key);
        if(value==null)
            return null;
        else {
            keyQueue.remove(key);
            return value;
        }
            
    }

    @Override
    public void clear() {
        super.clear();
        keyQueue.clear();
    }


    private void maintainSize() {
        while(size()>MAX_ENTRIES) {
            super.remove(keyQueue.poll());
        }
    }


    @Override
    public Object clone() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Set<Map.Entry<K,V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }
}
