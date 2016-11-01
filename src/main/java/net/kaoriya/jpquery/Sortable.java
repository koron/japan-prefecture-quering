package net.kaoriya.jpquery;

/**
 * A Sortable represents a indexed collection of comparable
 * elements.
 * It does not offer direct access to its elements, only
 * comparison and swapping by indices.
 *
 * In the method specifications we are using this[i] to
 * mean the 
 */
public interface Sortable {

    /**
     * Compares two elements by their indices.
     * @return -1 if this[first] < this[second],
     *          0 if this[first] = this[second]
     *          1 if this[first] > this[second]
     * @throws IndexOutOfBoundsException if one
     *      or both indices are outside of the
     *      limits of this sequence.
     */
    public int compare(int first, int second);

    /**
     * Swaps two elements by their indices.
     * This is roughly equivalent to this sequence:
     * <pre>
     *   temp = this[first];
     *   this[first] = this[second];
     *   this[second] = temp;
     * </pre>
     */
    public void swap(int first, int second);
}
