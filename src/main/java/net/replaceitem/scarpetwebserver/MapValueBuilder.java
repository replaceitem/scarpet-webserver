package net.replaceitem.scarpetwebserver;

import carpet.script.value.BooleanValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

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
}
