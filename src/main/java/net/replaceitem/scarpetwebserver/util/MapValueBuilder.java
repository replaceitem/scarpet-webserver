package net.replaceitem.scarpetwebserver.util;

import carpet.script.value.BooleanValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MapValueBuilder {
    private final Map<Value, Value> map = new HashMap<>();
    
    public void put(@NotNull String name, @Nullable Value value) {
        map.put(StringValue.of(name), value == null ? Value.NULL : value);
    }
    
    public void put(@NotNull String name, @Nullable String value) {
        put(name, StringValue.of(value));
    }
    
    public void put(@NotNull String name, @Nullable Number value) {
        put(name, NumericValue.of(value));
    }

    public void put(@NotNull String name, boolean value) {
        put(name, BooleanValue.of(value));
    }
    
    
    
    public MapValue build() {
        return MapValue.wrap(map);
    }


    public static <K,V> MapValue mapMap(Map<K,V> map, Function<K,Value> keyMapper, Function<V,Value> valueMapper) {
        Map<Value,Value> valueMap = new HashMap<>();
        for (Map.Entry<K, V> kkEntry : map.entrySet()) {
            valueMap.put(keyMapper.apply(kkEntry.getKey()), valueMapper.apply(kkEntry.getValue()));
        }
        return MapValue.wrap(valueMap);
    }
}
