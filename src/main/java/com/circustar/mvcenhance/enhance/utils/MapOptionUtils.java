package com.circustar.mvcenhance.enhance.utils;

import java.util.HashMap;
import java.util.Map;

public class MapOptionUtils {
    public static <T> T getValue(Map map, Object key, T defaultValue) {
        if(map == null) {
            return defaultValue;
        }
        if(map.containsKey(key)) {
            T res = (T) map.get(key);
            return res == null? defaultValue : res;
        }
        return null;
    }
    public static Map copy(Map map) {
        if(map == null) {
            return null;
        }
        Map newMap = new HashMap();
        newMap.putAll(map);
        return newMap;
    }
}
