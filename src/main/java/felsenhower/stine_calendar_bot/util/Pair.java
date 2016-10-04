package felsenhower.stine_calendar_bot.util;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a generic pair of two (different) types.
 * 
 * @author felsenhower
 *
 * @param <K>
 *            the type of the key
 * @param <V>
 *            the type of the value
 */
public class Pair<K, V> {
    private K key;
    private V value;

    /**
     * Creates a new Pair with the given key and value
     * 
     * @param key
     *            the key
     * @param value
     *            the value
     */
    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Creates a new Pair with empty key and value (each null)
     */
    public Pair() {
        this(null, null);
    }
    
    /**
     * Creates a new Pair from a Map.Entry
     */
    public Pair(Map.Entry<K, V> mapEntry) {
        this(mapEntry.getKey(), mapEntry.getValue());
    }

    /**
     * @return the key
     */
    public K getKey() {
        return key;
    }

    /**
     * @param key
     *            the key to set
     */
    public void setKey(K key) {
        this.key = key;
    }

    /**
     * @return the value
     */
    public V getValue() {
        return value;
    }

    /**
     * @param value
     *            the value to set
     */
    public void setValue(V value) {
        this.value = value;
    }

    /**
     * Sets both key and value
     * 
     * @param key
     *            the key to set
     * @param value
     *            the value to set
     */
    public void set(K key, V value) {
        this.setKey(key);
        this.setValue(value);
    }

    /**
     * Converts the Pair to a Map.Entry 
     */
    public Map.Entry<K, V> toMapEntry() {
        return new HashMap<K, V>() {
            private static final long serialVersionUID = 1L;
            {
                put(key, value);
            }
        }.entrySet().iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Pair))
            return false;
        Pair other = (Pair) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "[" + key.toString() + "," + value.toString() + "]";
    }

}
