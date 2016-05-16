/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

/**
 * This class replaces RandomBufferedHashSet, which had incorrect behavior
 * in the presence of duplicate entries.
 *
 * This class provides a data structure for reservoir sampling. The class itself
 * is an ArrayList that holds only MAX_ENTRIES entries. If you call
 * add when size()==MAX_ENTRIES, it will decide whether to accept or reject
 * the new element at random, and, if it accepts, decide to remove one current
 * element at random to maintain the size. The randomization is such that it
 * guarantees that if you provide it less than or equal to MAX_ENTRIES after
 * initializing it, it will contain everything you gave it, but if you give it
 * more than MAX_ENTRIES, the probability of it containing any item you gave it
 * is (MAX_ENTRIES/(total number items given)), ie. it will give you back a random
 * subset of whatever you give it that has size MAX_ENTRIES, without having to
 * keep everything you give it in memory and only needing to make one pass
 * through the data.
 *
 * Note that removing items explicitly is not allowed.
 *
 * @author Kevin
 */
public class RandomBufferedArrayList<T> extends ArrayList<T> {
    private final double MAX_ENTRIES;
    private double numAddAttempts=0d;
    Random random = new Random();

    public RandomBufferedArrayList(int bufferSize) {
        super();
        MAX_ENTRIES=bufferSize;
        super.ensureCapacity((int) MAX_ENTRIES);
    }

    /*
     * Returns true if the element was added.
     */
    @Override
    public boolean add(T o) {
        numAddAttempts++;
        if(size()<MAX_ENTRIES) {
            return super.add(o);
        } else if(random.nextDouble() < MAX_ENTRIES/(numAddAttempts)) {
            super.set(random.nextInt(size()), o);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Iterator<T> iterator() {
        final Iterator<T> superIter = super.iterator();

        return new Iterator<T>() {
            private Iterator<T> iter = superIter;

            public boolean hasNext() {
                return iter.hasNext();
            }

            public T next() {
                return iter.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    /*
     * There is no inherent reason why you couldn't implement this if you feel
     * like it. It's just that I don't need it right now and I don't have the
     * time.
     */
    public HashSet<T> clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        numAddAttempts=0;
        super.clear();
    }
    
}
