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
import net.replaceitem.scarpetwebserver.script.ResponseValue;
import spark.CustomErrorPages;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static carpet.CarpetServer.scriptServer;

public class ScarpetRoute implements Route {
    private final Webserver webserver;
    private final String hostname;
    private final FunctionValue callback;

    public ScarpetRoute(Webserver webserver, String hostname, FunctionValue callback) {
        this.webserver = webserver;
        this.hostname = hostname;
        this.callback = callback;
    }

    @Override
    public Object handle(Request request, Response response) {
        try {
            return runCallback(request, response);
        } catch (Exception e) {
            ScarpetWebserver.LOGGER.error("Got exception when running webserver route callback on " + request.url(), e);
            response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return CustomErrorPages.INTERNAL_ERROR;
        }
    }

    private String runCallback(Request request, Response response) throws CommandSyntaxException, IntegrityException, InvalidCallbackException {
        CarpetScriptHost appHost = scriptServer.getAppHostByName(this.hostname);
        if(appHost == null) {
            webserver.clearRoutes();
            throw new IntegrityException("App " + this.hostname + " not loaded");
        }
        ServerCommandSource commandSource = appHost.scriptServer().server.getCommandSource();
        CarpetScriptHost executingHost = appHost.retrieveOwnForExecution(commandSource);
        List<Value> args = List.of(encodeRequestData(request), new ResponseValue(response));
        return executingHost.callUDF(commandSource, callback, args).getString();
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
