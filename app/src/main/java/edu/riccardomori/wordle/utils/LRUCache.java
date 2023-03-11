package edu.riccardomori.wordle.utils;

import java.util.LinkedHashMap;
import java.util.Map;

// Simple LRU cache implementation based on LinkedHashMap
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private int maxSize;

    public LRUCache(int size) {
        // Initialize a LinkedHashMap with a loadFactor of 1 and with access-ordering
        super(size, 1, true);
        this.maxSize = size;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return super.size() > this.maxSize;
    }
}
