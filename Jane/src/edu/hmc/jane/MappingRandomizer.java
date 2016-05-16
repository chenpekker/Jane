/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hmc.jane;

/**
 *
 * @author Kevin
 */
public class MappingRandomizer {

    // DATA MEMBERS
    private final Phi phi;
    public int[] hostDegrees; // The number of parasites mapped to each host.
    private Tree hostTree;
    private Tree parasiteTree;
    private int[] hostsNeedingMapping; // The hosts needing to be mapped,
                                       // weighted according to their degree.
    private int[] parasiteTips;


    public MappingRandomizer(Phi p, Tree hostTree, Tree parasiteTree) {
        this.phi = p;
        this.hostDegrees = new int[hostTree.size];
        this.hostTree = hostTree;
        this.parasiteTree = parasiteTree;
        this.hostsNeedingMapping = new int[phi.size()];
        this.parasiteTips = new int[parasiteTree.numTips];
        characterizePhi();
    }

    /* gets some important information about the mapping (such as the number
     * of hosts with each parasite count) to be used in other methods.
     */
    private void characterizePhi() {
        int parasiteTipIndex = 0;
        int hostsNeedingMappingIndex = 0;
        for (int para : parasiteTree.getTips()) {
            parasiteTips[parasiteTipIndex++] = para;
            for (int host : phi.getHosts(para)) {
                ++hostDegrees[host];
                hostsNeedingMapping[hostsNeedingMappingIndex++] = host;
            }
        }
    }

    public Phi generateRandom() {
        return generateRandomFrom(parasiteTree, false);
    }

    public Phi generateRandomFrom(Tree pTree) {
        return generateRandomFrom(pTree, true);
    }

    /* Returns a randomized mapping in which all hosts have the same degree
     * as before and all parasites have degree at least 1. The probability
     * of a host being mapped to a parasite is the degree of that host divided
     * by the total number of parasites.
     */
    private Phi generateRandomFrom(Tree pTree, boolean randomTree) {
        Phi randomPhi = new Phi(pTree.size);
        int randHost;
        int[] remainingHostDegree = new int[hostTree.size];
        System.arraycopy(hostDegrees, 0, remainingHostDegree, 0, hostTree.size);

        // Give each parasite a host.
        int tipIndex = 0;
        shuffleNHosts(pTree.numTips);
        for (int para : pTree.getTips()) {
            // parasiteTips must be recomputed every time when we build
            // from a random tree.
            if (randomTree) {
                parasiteTips[tipIndex] = para;
            }
            randHost = hostsNeedingMapping[tipIndex++];
            randomPhi.addAssociation(randHost, para);
            --remainingHostDegree[randHost];
        }

        // Some hosts still need more parasites.
        for (int host : hostTree.getTips()) {
            int currentIndex = 0;
            while (remainingHostDegree[host] > 0) {
                // Swap current array element with a random (later) one.
                int parasiteTipIndex = Jane.rand.nextInt(pTree.numTips - currentIndex) + currentIndex;
                int nextParasiteTip = parasiteTips[parasiteTipIndex];
                parasiteTips[parasiteTipIndex] = parasiteTips[currentIndex];
                parasiteTips[currentIndex] = nextParasiteTip;

                // Add a new association to the mapping. No need to update
                // mappedParasites since we will never choose nextParasiteTip
                // from parasiteTips again.
                if (!randomPhi.containsAssociation(host, nextParasiteTip)) {
                    randomPhi.addAssociation(host, nextParasiteTip);
                    --remainingHostDegree[host];
                }
                ++currentIndex;
            }
        }
        return randomPhi;
    }

    // Select n hosts at random from hostsNeedingMapping and put them
    // at the front of the array.
    private void shuffleNHosts(int n) {
        for (int i = 0; i < n; ++i) {
            int swapIndex = Jane.rand.nextInt(hostsNeedingMapping.length - i) + i;
            int swapHost = hostsNeedingMapping[swapIndex];
            hostsNeedingMapping[swapIndex] = hostsNeedingMapping[i];
            hostsNeedingMapping[i] = swapHost;
        }
    }
}
