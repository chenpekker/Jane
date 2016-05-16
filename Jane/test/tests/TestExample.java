package tests;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 *
 * @author Kevin
 */

public class TestExample {

    @Test (timeout = 100)
    public void sampleTest() {
        SortedSet<Integer> test = new TreeSet<Integer>();
        assertTrue(test.isEmpty());
        assertEquals(test.size(), 0);
        assertArrayEquals(new Integer[0], test.toArray());

        test.add(42);
        test.add(73);

        assertTrue(test.contains(42));
        assertTrue(!test.contains(17));

        test.clear();
        assertTrue(test.isEmpty());
        assertEquals(test.size(), 0);
    }
}