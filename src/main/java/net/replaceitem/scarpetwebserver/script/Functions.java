package net.replaceitem.scarpetwebserver.script;

import carpet.script.Context;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.value.Value;
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
            ScarpetWebserver.LOGGER.error("Could not start webserver '{}'", webserver.getId(), e);
            return null;
        }
        return webserver;
    }

    @ScarpetFunction
    public Webserver ws_get(Context context, String id) {
        return ScarpetWebserver.webservers.get(id);
    }

    @ScarpetFunction
    public void ws_start(Context context, Webserver webserver) {
        webserver.start();
    }

    @ScarpetFunction
    public void ws_add_route(Context context, Webserver webserver, String method, String path, Value callback) {
        webserver.addRoute(method, path, context.host, callback);
    }
    
    @ScarpetFunction
    public void ws_not_found(Context context, Webserver webserver, Value callback) {
        webserver.setNotFound(context.host, callback);
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

    @ScarpetFunction(maxParams = ScarpetFunction.UNLIMITED_PARAMS)
    public void ws_add_sse_route(Webserver webserver, String path, String... rooms) {
        webserver.addSSERoute(path, rooms);
    }

    @ScarpetFunction(maxParams = ScarpetFunction.UNLIMITED_PARAMS)
    public int ws_send_sse_message(Webserver webserver, Optional<String> event, String data, String... rooms) {
        return webserver.sendSSEMessage(event.orElse(null), data, rooms);
    }
}
