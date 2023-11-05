package net.replaceitem.scarpetwebserver.script;

import carpet.script.Context;
import carpet.script.ScriptHost;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;
import net.replaceitem.scarpetwebserver.ScarpetRoute;
import spark.Response;
import spark.RouteImpl;
import spark.Service;
import spark.route.HttpMethod;

import javax.servlet.http.Cookie;
import java.util.Optional;

public class Functions {
    @ScarpetFunction
    public Service ws_create() {
        return Service.ignite();
    }

    @ScarpetFunction
    public void ws_add_route(Context context, Service webserver, String method, String path, Value callback) {
        FunctionValue callbackFunction = getCallback(context.host, callback);
        webserver.addRoute(HttpMethod.get(method.toLowerCase()), RouteImpl.create(path, new ScarpetRoute(context.host.getName(), callbackFunction)));
    }

    @ScarpetFunction
    public void ws_response_set_body(Response responseValue, String body) {
        responseValue.body(body);
    }
    @ScarpetFunction
    public void ws_response_set_status(Response responseValue, int statusCode) {
        responseValue.status(statusCode);
    }
    @ScarpetFunction
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
    @ScarpetFunction
    public void ws_response_add_cookie(Response response, String name, String value, Value... optionalParams) {
        Optional<String> path = optionalArg(optionalParams, 0).map(Value::getString);
        Optional<String> domain = optionalArg(optionalParams, 1).map(Value::getString);
        Optional<Integer> maxAge = optionalArg(optionalParams, 2).map(value1 -> NumericValue.asNumber(value1, "maxAge").getInt());
        Optional<Boolean> secured = optionalArg(optionalParams, 3).map(Value::getBoolean);
        Optional<Boolean> httpOnly = optionalArg(optionalParams, 4).map(Value::getBoolean);
        Cookie cookie = new Cookie(name, value);
        path.ifPresent(cookie::setPath);
        domain.ifPresent(cookie::setDomain);
        maxAge.ifPresent(cookie::setMaxAge);
        secured.ifPresent(cookie::setSecure);
        httpOnly.ifPresent(cookie::setHttpOnly);
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
        if(array == null) return Optional.empty();
        if(array.length <= index) return Optional.empty();
        T t = array[index];
        if(t == null || (t instanceof Value value && value.isNull())) return Optional.empty();
        return Optional.of(t);
    }

    public static <T> Optional<T> optionalArg(T[] array) {
        return optionalArg(array, 0);
    }
}
