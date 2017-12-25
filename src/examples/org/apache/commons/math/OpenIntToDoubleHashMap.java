//package org.apache.commons.math.util;
package org.apache.commons.math;

//import org.apache.commons.math.util.FastMath;

public class OpenIntToDoubleHashMap{

    /** Status indicator for free table entries. */
    protected static final byte FREE    = 0;

    /** Status indicator for full table entries. */
    protected static final byte FULL    = 1;

    /** Status indicator for removed table entries. */
    protected static final byte REMOVED = 2;

    /** Serializable version identifier */
    //private static final long serialVersionUID = -3646337053166149105L;

    /** Load factor for the map. */
    private static final float LOAD_FACTOR = 0.5f;

    /** Default starting size.
     * <p>This must be a power of two for bit mask to work properly. </p>
     */
    private static final int DEFAULT_EXPECTED_SIZE = 16;

    /** Multiplier for size growth when map fills up.
     * <p>This must be a power of two for bit mask to work properly. </p>
     */
    private static final int RESIZE_MULTIPLIER = 2;

    /** Number of bits to perturb the index when probing for collision resolution. */
    private static final int PERTURB_SHIFT = 5;

    /** Keys table. */
    private int[] keys;

    /** Values table. */
    private double[] values;

    /** States table. */
    private byte[] states;

    /** Return value for missing entries. */
    private final double missingEntries;

    /** Current size of the map. */
    private int size;

    /** Bit mask for hash values. */
    private int mask;

    /** Modifications count. */
    private transient int count;
    
	/**
     * Build an empty map with specified size.
     * @param expectedSize expected number of elements in the map
     * @param missingEntries value to return when a missing entry is fetched
     */
    public OpenIntToDoubleHashMap(final int expectedSize,
                                  final double missingEntries) {
        final int capacity = computeCapacity(expectedSize);
        keys   = new int[capacity];
        values = new double[capacity];
        states = new byte[capacity];
        this.missingEntries = missingEntries;
        mask   = capacity - 1;
    }
    
    /**
     * Build an empty map with default size
     * @param missingEntries value to return when a missing entry is fetched
     */
    public OpenIntToDoubleHashMap(final double missingEntries) {
        this(DEFAULT_EXPECTED_SIZE, missingEntries);
    }
    
    /**
     * Compute the capacity needed for a given size.
     * @param expectedSize expected size of the map
     * @return capacity to use for the specified size
     */
    private static int computeCapacity(final int expectedSize) {
        if (expectedSize == 0) {
            return 1;
        }
        //Cod below is not executed in our example
        /* 
        final int capacity   = (int) FastMath.ceil(expectedSize / LOAD_FACTOR);
        final int powerOfTwo = Integer.highestOneBit(capacity);
        if (powerOfTwo == capacity) {
            return capacity;
        }
        
        return nextPowerOfTwo(capacity);
        */
        return -1;
    }
    
    /**
     * Find the smallest power of two greater than the input value
     * @param i input value
     * @return smallest power of two greater than the input value
     */
    private static int nextPowerOfTwo(final int i) {
        return Integer.highestOneBit(i) << 1;
    }
}
