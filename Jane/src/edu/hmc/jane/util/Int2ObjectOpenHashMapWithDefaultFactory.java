/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.concurrent.Callable;

/**
 * Note: calling methods other than those overriden and put may produce
 * unexpected results.
 * @author John
 */
public class Int2ObjectOpenHashMapWithDefaultFactory<V> extends Int2ObjectOpenHashMap<V> {
private Callable<V> factory=null;

public Int2ObjectOpenHashMapWithDefaultFactory() {}

public Int2ObjectOpenHashMapWithDefaultFactory(Callable<V> factory) {
    defaultReturnFactory(factory);
}

@Override
public V defaultReturnValue() {
    try {
        return factory.call();
    } catch (NullPointerException e) {
        return super.defaultReturnValue();
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}

@Override
public void defaultReturnValue(V rv) {
    factory=null;
    super.defaultReturnValue(rv);
}

@Override
public V get(int k) {
    V v = super.get(k);
    if (v==null) {
        try {
            v = factory.call();
            super.put(k, v);
            return v;
        } catch (NullPointerException e) {
            return v;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    } else {
        return v;
    }
}

public final void defaultReturnFactory(Callable<V> factory) {
   defaultReturnValue(null);
   this.factory=factory;
}
}
