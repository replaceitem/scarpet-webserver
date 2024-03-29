package net.replaceitem.scarpetwebserver.webserver;

import net.replaceitem.scarpetwebserver.Config;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.pathmap.UriTemplatePathSpec;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;

public class Webserver {
    private final Server server;
    private final String id;
    private MethodMappingsHandler<UriTemplateMappingsHandler> methodMappingsHandler;

    public Webserver(Config.WebserverConfig webserverConfig) {
        this.id = webserverConfig.id;
        server = new Server(webserverConfig.port);
        server.setDynamic(true);
    }
    
    public void addRoute(String method, String path, ScarpetHandler handler) {
        HttpMethod httpMethod = HttpMethod.INSENSITIVE_CACHE.get(method);
        UriTemplateMappingsHandler uriTemplateMappingsHandler = methodMappingsHandler.computeIfAbsent(httpMethod, (m) -> new UriTemplateMappingsHandler());
        uriTemplateMappingsHandler.addMapping(new UriTemplatePathSpec(path), handler);
    }

    public void init() throws Exception {
        if(!server.isStarting() && !server.isStarted()) server.start();
        clearRoutes();
    }
    
    public void start() {
        try {
            methodMappingsHandler.start();
        } catch (Exception ignored) {}
    }

    public void clearRoutes() {
        methodMappingsHandler = new MethodMappingsHandler<>();
        server.setHandler(methodMappingsHandler);
        server.setDefaultHandler(new StatusCodeHandler(HttpStatus.Code.NOT_FOUND));
    }

    public String getId() {
        return id;
    }

    public void setNotFound(ScarpetHandler route) {
        server.setDefaultHandler(route);
    }

    public void close() throws Exception {
        server.stop();
    }

    private static class StatusCodeHandler extends Handler.Abstract.NonBlocking {

        public StatusCodeHandler(HttpStatus.Code statusCode) {
            this.statusCode = statusCode;
        }

        private final HttpStatus.Code statusCode;

        @Override
        public boolean handle(Request request, Response response, Callback callback) {
            response.setStatus(statusCode.getCode());
            response.getHeaders().put(MimeTypes.Type.TEXT_HTML.getContentTypeField());
            
            Content.Sink.write(response, true, """
            <!DOCTYPE html>
            <html>
            <head>
              <title>%d</title>
            </head>
            <body>
              <h1>%s</>
            </body>
            </html>
            """.formatted(statusCode.getCode(), statusCode.toString()), callback);
            return true;
        }
    }
}
