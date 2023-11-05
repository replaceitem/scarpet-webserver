package net.replaceitem.scarpetwebserver;

import carpet.script.CarpetScriptHost;
import carpet.script.exception.IntegrityException;
import carpet.script.exception.InvalidCallbackException;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static carpet.CarpetServer.scriptServer;

public class ScarpetRoute implements Route {

    private final String hostname;
    private final FunctionValue callback;

    public ScarpetRoute(String hostname, FunctionValue callback) {
        this.hostname = hostname;
        this.callback = callback;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        runCallback(request, response);
        return null;
    }

    private void runCallback(Request request, Response response) throws CommandSyntaxException {
        CarpetScriptHost appHost = scriptServer.getAppHostByName(this.hostname);
        if(appHost == null) {
            // close server?
        }
        ServerCommandSource commandSource = appHost.scriptServer().server.getCommandSource();
        CarpetScriptHost executingHost = appHost.retrieveOwnForExecution(commandSource);
        List<Value> args = List.of(encodeRequestData(request), );
        try
        {
            executingHost.callUDF(commandSource, callback, args);
        }
        catch (NullPointerException | InvalidCallbackException | IntegrityException error)
        {
            ScarpetWebserver.LOGGER.error("Got exception when running webserver route callback on " + request.url(), error);
        }
    }

    private static MapValue encodeRequestData(Request request) {
        Map<String, Value> map = new HashMap<>();
        map.put("body", StringValue.of(request.body()));
        map.put("params", mapMap(request.params(), StringValue::of, StringValue::of));
        map.put("contentLength", NumericValue.of(request.contentLength()));
        map.put("contentType", StringValue.of(request.contentType()));
        map.put("contextPath", StringValue.of(request.contextPath()));
        map.put("url", StringValue.of(request.url()));
        map.put("uri", StringValue.of(request.uri()));
        map.put("protocol", StringValue.of(request.protocol()));
        map.put("ip", StringValue.of(request.ip()));
        map.put("host", StringValue.of(request.host()));
        map.put("scheme", StringValue.of(request.scheme()));
        map.put("userAgent", StringValue.of(request.userAgent()));
        map.put("cookies", mapMap(request.cookies(), StringValue::of, StringValue::of));
        map.put("headers", ListValue.wrap(request.headers().stream().map(StringValue::of)));
        return mapMap(map, StringValue::of, value -> value);
    }
    
    private static <K,V> MapValue mapMap(Map<K,V> map, Function<K,Value> keyMapper, Function<V,Value> valueMapper) {
        Map<Value,Value> valueMap = new HashMap<>();
        for (Map.Entry<K, V> kkEntry : map.entrySet()) {
            valueMap.put(keyMapper.apply(kkEntry.getKey()), valueMapper.apply(kkEntry.getValue()));
        }
        return MapValue.wrap(valueMap);
    }
}
