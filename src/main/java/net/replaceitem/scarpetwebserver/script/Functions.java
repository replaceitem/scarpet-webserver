package net.replaceitem.scarpetwebserver.script;

import carpet.script.Context;
import carpet.script.ScriptHost;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;
import net.replaceitem.scarpetwebserver.ScarpetRoute;
import net.replaceitem.scarpetwebserver.ScarpetWebserver;
import net.replaceitem.scarpetwebserver.Webserver;
import spark.Response;

import javax.servlet.http.Cookie;
import java.util.Optional;

@SuppressWarnings("unused")
public class Functions {
    @ScarpetFunction
    public Webserver ws_init(Context context, String id) {
        if(context.host.isPerUser()) {
            ScarpetWebserver.LOGGER.warn("You are using scarpet-webserver in a player scoped app, this is not recommended");
        }
        Webserver webserver = ScarpetWebserver.webservers.get(id);
        webserver.init();
        return webserver;
    }

    @ScarpetFunction
    public void ws_add_route(Context context, Webserver webserver, String method, String path, Value callback) {
        FunctionValue callbackFunction = getCallback(context.host, callback);
        webserver.addRoute(method, path, new ScarpetRoute(webserver, context.host.getName(), callbackFunction));
    }

    @ScarpetFunction
    public void ws_response_set_status(Response responseValue, int statusCode) {
        responseValue.status(statusCode);
    }
    @ScarpetFunction(maxParams = 3)
    public void ws_response_redirect(Response responseValue, String location, Integer... statusCode) {
        Optional<Integer> optionalStatusCode = optionalArg(statusCode);
        if(optionalStatusCode.isPresent()) {
            responseValue.redirect(location, optionalStatusCode.get());
        } else {
            responseValue.redirect(location);
        }
    }
    @ScarpetFunction
    public void ws_response_set_content_type(Response responseValue, String contentType) {
        responseValue.type(contentType);
    }
    @ScarpetFunction
    public void ws_response_set_header(Response responseValue, String header, String value) {
        responseValue.header(header, value);
    }
    @ScarpetFunction(maxParams = 8)
    public void ws_response_add_cookie(Response response, String name, String value, Value... optionalParams) {
        Cookie cookie = new Cookie(name, value);
        optionalArg(optionalParams, 0).map(Value::getString).ifPresent(cookie::setPath);
        optionalArg(optionalParams, 1).map(Value::getString).ifPresent(cookie::setDomain);
        optionalArg(optionalParams, 2).map(value1 -> NumericValue.asNumber(value1, "maxAge").getInt()).ifPresent(cookie::setMaxAge);
        optionalArg(optionalParams, 3).map(Value::getBoolean).ifPresent(cookie::setSecure);
        optionalArg(optionalParams, 4).map(Value::getBoolean).ifPresent(cookie::setHttpOnly);
        response.raw().addCookie(cookie);
    }
    
    
    private static FunctionValue getCallback(ScriptHost host, Value value) {
        if (value instanceof FunctionValue functionValue) return functionValue;
        String name = value.getString();
        FunctionValue function = host.getFunction(name);
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
