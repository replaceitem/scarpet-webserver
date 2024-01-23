package net.replaceitem.scarpetwebserver.script;

import carpet.script.Context;
import carpet.script.ScriptHost;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;
import net.replaceitem.scarpetwebserver.webserver.ScarpetHandler;
import net.replaceitem.scarpetwebserver.ScarpetWebserver;
import net.replaceitem.scarpetwebserver.webserver.Webserver;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Response;

import java.util.Optional;

@SuppressWarnings("unused")
public class Functions {
    @ScarpetFunction
    public Webserver ws_init(Context context, String id) {
        if(context.host.isPerUser()) {
            ScarpetWebserver.LOGGER.warn("You are using scarpet-webserver in a player scoped app, this is not recommended");
        }
        Webserver webserver = ScarpetWebserver.webservers.get(id);
        if(webserver == null) return null;
        try {
            webserver.init();
        } catch (Exception e) {
            ScarpetWebserver.LOGGER.error("Could not start webserver '" + webserver.getId() + "'", e);
            return null;
        }
        return webserver;
    }

    @ScarpetFunction
    public void ws_start(Context context, Webserver webserver) {
        webserver.start();
    }

    @ScarpetFunction
    public void ws_add_route(Context context, Webserver webserver, String method, String path, Value callback) {
        FunctionValue callbackFunction = getCallback(context.host, callback);
        webserver.addRoute(method, path, new ScarpetHandler(webserver, context.host.getName(), callbackFunction));
    }
    
    @ScarpetFunction
    public void ws_not_found(Context context, Webserver webserver, Value callback) {
        FunctionValue callbackFunction = getCallback(context.host, callback);
        webserver.setNotFound(new ScarpetHandler(webserver, context.host.getName(), callbackFunction));
    }
    
    
    

    @ScarpetFunction
    public void ws_response_set_status(Response response, int statusCode) {
        response.setStatus(statusCode);
    }
    @ScarpetFunction
    public void ws_response_set_content_type(Response response, String contentType) {
        response.getHeaders().put(HttpHeader.CONTENT_TYPE, contentType);
    }
    @ScarpetFunction
    public void ws_response_add_header(Response responseValue, String header, String value) {
        responseValue.getHeaders().put(header, value);
    }
    
    
    private static FunctionValue getCallback(ScriptHost host, Value value) {
        if (value instanceof FunctionValue functionValue) return functionValue;
        String name = value.getString();
        FunctionValue function = host.getFunction(name);
        //noinspection ConstantValue
        if (function == null) {
            throw new InternalExpressionException("Function " + name + " is not defined yet");
        }
        return function;
    }

    public static <T> Optional<T> optionalArg(T[] array, int index) {
        if (array == null || array.length <= index) return Optional.empty();
        T t = array[index];
        if(t == null || (t instanceof Value value && value.isNull())) return Optional.empty();
        return Optional.of(t);
    }

    public static <T> Optional<T> optionalArg(T[] array) {
        return optionalArg(array, 0);
    }
}
