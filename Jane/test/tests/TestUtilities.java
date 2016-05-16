package tests;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import edu.hmc.jane.ProblemInstance;
import edu.hmc.jane.io.NewNexusFileReader;
import edu.hmc.jane.io.NexusFileReader;
import edu.hmc.jane.io.TarzanFileReader;
import edu.hmc.jane.io.TreeFileReader;

/**
 *
 * @author Kevin
 */

public class TestUtilities {

    public static ProblemInstance readFile(String filename) {
        TreeFileReader fin = null;
        try {
            fin = new NexusFileReader(filename, false);
            if (!((NexusFileReader) fin).isNexus()) {
                fin = new TarzanFileReader(filename);
            }
            fin = new NexusFileReader(filename, false);
            if (!((NexusFileReader) fin).isNexus()) {
                fin = new NewNexusFileReader(filename);
                if (!((NewNexusFileReader) fin).isNewNexus()) {
                    fin = new TarzanFileReader(filename);
                }
            }
        } catch (java.io.FileNotFoundException e) {
            System.err.println("Could not find file: " + filename);
            System.exit(1);
        } catch (java.io.IOException e) {
            System.err.println("Error reading file.\n"+e.getLocalizedMessage());
            System.exit(1);
        }

        try {
            final ProblemInstance pi = fin.readProblem();
            return pi;
        } catch(Exception e) {
            System.err.println("Something went pretty seriously wrong");
            System.err.println(e);
        }

        return null;
    }

    public static class Pair<L,R> {
    // Implementation from
    // http://stackoverflow.com/questions/521171/a-java-collection-of-value-pairs-tuples

        public final L left;
        public final R right;

        public Pair(L left, R right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public int hashCode() { return left.hashCode() ^ right.hashCode(); }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof Pair)) return false;
            Pair pairo = (Pair) o;
            return this.left.equals(pairo.left) && this.right.equals(pairo.right);
        }
    }
}