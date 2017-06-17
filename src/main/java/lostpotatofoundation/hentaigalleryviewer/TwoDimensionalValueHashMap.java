package lostpotatofoundation.hentaigalleryviewer;

import java.util.HashMap;

public class TwoDimensionalValueHashMap<K, V> extends HashMap<K, HashMap<K, V>> {
    public V put(K columnKey, K rowKey, V value) {
        HashMap<K, V> innerMap = getOrDefault(rowKey, new HashMap<>());
        innerMap.put(columnKey, value);
        put(rowKey, innerMap);
        return value;
    }

    public V get(K columnKey, K rowKey) {
        return getOrDefault(rowKey, new HashMap<>()).get(columnKey);
    }

    public V remove2D(K columnKey, K rowKey) {
        HashMap<K, V> innerMap = getOrDefault(rowKey, new HashMap<>());
        V v = innerMap.remove(columnKey);
        put(rowKey, innerMap);
        return v;
    }

    public boolean containsKey(K columnKey, K rowKey) {
        return containsKey(rowKey) && get(rowKey).containsKey(columnKey);
    }

    @Override
    public boolean containsValue(Object value) {
        return keySet().parallelStream().anyMatch(k -> getOrDefault(k, new HashMap<>()).containsValue(value));
    }
}
