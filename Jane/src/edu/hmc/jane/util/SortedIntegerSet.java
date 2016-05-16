package edu.hmc.jane.util;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.SortedSet;

/**
 * A sorted LinkedList of Integers for use in storing the possible e_H locations
 * in the DP. 
 * @author Kevin
 */
public class SortedIntegerSet extends LinkedList<Integer> {

    // initializes the SortedIntegerSet to contain all values in the range [left, right)
    // if the range is empty then so will be the returned SortedIntegerSet.
    public SortedIntegerSet(int left, int right) {
        super();
        for (int i = left; i < right; ++i) {
            super.addLast(new Integer(i));
        }
    }

    //initializes the SortedIntegerSet to contain only the item item
    public SortedIntegerSet(int value) {
        super();
        super.add(new Integer(value));
    }

    public SortedIntegerSet() {
        super();
    }

    public SortedIntegerSet(SortedSet<? extends Integer> values) {
        super(values);
    }

    public boolean add(int value) {
        return add(new Integer(value));
    }

    @Override
    public boolean add(Integer item) {
        ListIterator<Integer> it = super.listIterator();
        while (it.hasNext()) {
            Integer next = (Integer) it.next();
            int comparisonValue = next.compareTo(item);

            if (comparisonValue == 0) {
                return false;
            } else if (comparisonValue > 0) {
                it.previous();
                break;
            }
        }
        it.add(item);
        return true;
    }

    //returns the union of this and another SortedIntegerSet
    public SortedIntegerSet union(SortedIntegerSet other) {
        ListIterator it1 = super.listIterator();
        ListIterator it2 = other.listIterator();
        
        Integer elem1 = null;
        Integer elem2 = null;

        // If either are empty, clone the other.
        if (it1.hasNext()) {
            elem1 = (Integer) it1.next();
        } else {
            return (SortedIntegerSet) other.clone();
        }
        if (it2.hasNext()) {
            elem2 = (Integer) it2.next();
        } else {
            return (SortedIntegerSet) this.clone();
        }

        SortedIntegerSet newList = new SortedIntegerSet();

        // Copy elements to the new List.
        while (elem1 != null || elem2 != null) {
            if (elem1 != null && elem1.equals(elem2)) {
                newList.addLast(elem1);
                elem1 = (it1.hasNext()) ? (Integer) it1.next() : null;
                elem2 = (it2.hasNext()) ? (Integer) it2.next() : null;
            } else if (elem1 != null  && (elem2 == null || elem1.compareTo(elem2) < 0)) {
                newList.addLast(elem1);
                elem1 = (it1.hasNext()) ? (Integer) it1.next() : null;
            } else {
                newList.addLast(elem2);
                elem2 = (it2.hasNext()) ? (Integer) it2.next() : null;
            }
        }
        
        return newList;
    }

    public int getMin() {
        return super.getFirst();
    }
}
