package net.replaceitem.scarpetwebserver.webserver;

import carpet.CarpetServer;
import carpet.script.CarpetScriptHost;
import carpet.script.ScriptHost;
import carpet.script.exception.IntegrityException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.InvalidCallbackException;
import carpet.script.value.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.replaceitem.scarpetwebserver.Config;

import java.util.*;

import net.replaceitem.scarpetwebserver.SSEConnection;
import net.replaceitem.scarpetwebserver.ScarpetWebserver;
import net.replaceitem.scarpetwebserver.script.RequestValue;
import net.replaceitem.scarpetwebserver.script.ResponseValue;
import net.replaceitem.scarpetwebserver.script.SSEConnectionValue;
import org.eclipse.jetty.http.*;
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
    
    public void addRoute(String method, String path, ScriptHost host, Value callback) {
        HttpMethod httpMethod = HttpMethod.INSENSITIVE_CACHE.get(method);
        UriTemplateMappingsHandler uriTemplateMappingsHandler = methodMappingsHandler
                .computeIfAbsent(httpMethod, (_) -> new UriTemplateMappingsHandler());
        uriTemplateMappingsHandler.addMapping(new UriTemplatePathSpec(path), createHandler(host, callback));
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
        server.setDefaultHandler(createStatusHandler(HttpStatus.Code.NOT_FOUND));
    }

    public String getId() {
        return id;
    }

    public void setNotFound(ScriptHost host, Value callback) {
        server.setDefaultHandler(createHandler(host, callback));
    }

    public void close() throws Exception {
        server.stop();
    }

    public static Value callFunctionValue(ScriptHost host, Value value, List<Value> args)
            throws InternalExpressionException, CommandSyntaxException, InvalidCallbackException, IntegrityException {
        FunctionValue callbackFunction = value instanceof FunctionValue functionValue ? functionValue : host.getFunction(value.getString());
        if (callbackFunction == null)
            throw new InternalExpressionException("Function " + value.getString() + " is not defined yet");
        CarpetScriptHost appHost = CarpetServer.scriptServer.modules.get(host.getName());
        if (appHost == null)
            throw new IntegrityException("App " + host.getName() + " not loaded");
        CommandSourceStack commandSource = appHost.scriptServer().server.createCommandSourceStack();
        return appHost.retrieveOwnForExecution(commandSource).callUDF(commandSource, callbackFunction, args);
    }

    private Handler createHandler(ScriptHost host, Value value) {
        return new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                try {
                    String body = callFunctionValue(host, value, List.of(RequestValue.of(request), ResponseValue.of(response))).getString();
                    Content.Sink.write(response, true, body, callback);
                } catch (Exception e) {
                    ScarpetWebserver.LOGGER.error("Got exception when running webserver route callback on {}", request.getHttpURI(), e);
                    if (e instanceof IntegrityException) clearRoutes();
                    callback.failed(e);
                    return true;
                }
                return true;
            }
        };
    }

    @SuppressWarnings("SameParameterValue")
    private Handler createStatusHandler(HttpStatus.Code statusCode) {
        return new Handler.Abstract() {
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
        };
    }

    public void addSSERoute(String path, ScriptHost host, Value callback) {
        UriTemplateMappingsHandler uriTemplateMappingsHandler = methodMappingsHandler
                .computeIfAbsent(HttpMethod.GET, (_) -> new UriTemplateMappingsHandler());
        uriTemplateMappingsHandler.addMapping(new UriTemplatePathSpec(path), createSSEHandler(host, callback));
    }

    private Handler createSSEHandler(ScriptHost host, Value value) {
        return new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                SSEConnection connection = new SSEConnection(request, response, callback);
                try {
                    callFunctionValue(host, value, List.of(SSEConnectionValue.of(connection)));
                } catch (Exception e) {
                    ScarpetWebserver.LOGGER.error("Got exception when running webserver route callback on {}", request.getHttpURI(), e);
                    if (e instanceof IntegrityException) clearRoutes();
                    connection.callbackFailed(e);
                    return true;
                }
                connection.sendInitialResponse();
                return true;
            }
        };
    }
}
