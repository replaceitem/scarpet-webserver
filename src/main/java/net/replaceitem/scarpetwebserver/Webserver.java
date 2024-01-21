package net.replaceitem.scarpetwebserver;

import carpet.script.exception.InternalExpressionException;
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
    private UriTemplateMappingsHandler uriTemplateMappingsHandler;

    public Webserver(Config.WebserverConfig webserverConfig) {
        this.id = webserverConfig.id;
        server = new Server(webserverConfig.port);
        server.setDynamic(true);
    }

    public Server getServer() {
        return server;
    }

    public void addRoute(String method, String path, ScarpetHandler handler) {
        if(uriTemplateMappingsHandler.isStarted()) throw new InternalExpressionException("Webserver is already started");
        uriTemplateMappingsHandler.addMapping(new UriTemplatePathSpec(path), handler);
    }

    public void init() throws Exception {
        if(!server.isStarting() && !server.isStarted()) server.start();
        clearRoutes();
    }
    
    public void start() {
        try {
            uriTemplateMappingsHandler.start();
        } catch (Exception ignored) {}
    }

    public void clearRoutes() {
        uriTemplateMappingsHandler = new UriTemplateMappingsHandler();
        server.setHandler(uriTemplateMappingsHandler);
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
