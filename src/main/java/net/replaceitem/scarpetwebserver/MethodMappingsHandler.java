package net.replaceitem.scarpetwebserver;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public class MethodMappingsHandler<H extends Handler> extends Handler.Abstract {
    
    private final Map<HttpMethod,H> methodMappings = new EnumMap<>(HttpMethod.class);
    
    public H computeIfAbsent(HttpMethod method, Function<HttpMethod, H> handlerSupplier) {
        return methodMappings.computeIfAbsent(method, handlerSupplier);
    }
    
    public H get(HttpMethod method) {
        return methodMappings.get(method);
    }
    
    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        String method = request.getMethod();
        HttpMethod httpMethod = HttpMethod.fromString(method);
        if(httpMethod == null) return false;
        H handler = get(httpMethod);
        if(handler == null) return false;
        return handler.handle(request, response, callback);
    }
}
