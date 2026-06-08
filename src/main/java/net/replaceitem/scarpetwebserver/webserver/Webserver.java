package net.replaceitem.scarpetwebserver.webserver;

import carpet.CarpetServer;
import carpet.script.CarpetScriptHost;
import carpet.script.ScriptHost;
import carpet.script.exception.IntegrityException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import net.minecraft.commands.CommandSourceStack;
import net.replaceitem.scarpetwebserver.Config;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.replaceitem.scarpetwebserver.ScarpetWebserver;
import net.replaceitem.scarpetwebserver.script.RequestValue;
import net.replaceitem.scarpetwebserver.script.ResponseValue;
import org.eclipse.jetty.http.*;
import org.eclipse.jetty.http.pathmap.UriTemplatePathSpec;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Webserver {
    private final Server server;
    private final String id;
    private MethodMappingsHandler<UriTemplateMappingsHandler> methodMappingsHandler;

    private record SSEConnection(Request request, Response response, Callback callback) {}
    private Map<String, Collection<SSEConnection>> sseRoomConnectionMap;

    public Webserver(Config.WebserverConfig webserverConfig) {
        this.id = webserverConfig.id;
        server = new Server(webserverConfig.port);
        server.setDynamic(true);
    }
    
    public void addRoute(String method, String path, ScriptHost host, Value callback) {
        HttpMethod httpMethod = HttpMethod.INSENSITIVE_CACHE.get(method);
        UriTemplateMappingsHandler uriTemplateMappingsHandler = methodMappingsHandler.computeIfAbsent(httpMethod, (_) -> new UriTemplateMappingsHandler());
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
        if (sseRoomConnectionMap != null) {
            for (Collection<SSEConnection> connections : sseRoomConnectionMap.values()) {
                for (SSEConnection c : connections) {
                    try {
                        c.response.write(true, ByteBuffer.wrap(new byte[0]), c.callback);
                    } catch (Exception ignored) {}
                }
            }
        }
        sseRoomConnectionMap = new ConcurrentHashMap<>();
        server.setHandler(methodMappingsHandler);
        server.setDefaultHandler(new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                response.setStatus(HttpStatus.Code.NOT_FOUND.getCode());
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
                """.formatted(HttpStatus.Code.NOT_FOUND.getCode(), HttpStatus.Code.NOT_FOUND.toString()), callback);
                return true;
            }
        });
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

    private CarpetScriptHost getAppHost(ScriptHost host) {
        CarpetScriptHost appHost = CarpetServer.scriptServer.modules.get(host.getName());
        if (appHost == null) {
            clearRoutes();
            throw new IntegrityException("App " + host.getName() + " not loaded");
        }
        return appHost;
    }

    private Handler createHandler(ScriptHost host, Value value) {
        FunctionValue callbackFunction = value instanceof FunctionValue functionValue ? functionValue : host.getFunction(value.getString());
        if (callbackFunction == null)
            throw new InternalExpressionException("Function " + value.getString() + " is not defined yet");

        return new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                try {
                    CarpetScriptHost appHost = getAppHost(host);
                    CommandSourceStack commandSource = appHost.scriptServer().server.createCommandSourceStack();
                    CarpetScriptHost executingHost = appHost.retrieveOwnForExecution(commandSource);
                    List<Value> args = List.of(RequestValue.of(request), ResponseValue.of(response));
                    String body = executingHost.callUDF(commandSource, callbackFunction, args).getString();
                    Content.Sink.write(response, true, body, callback);
                } catch (Exception e) {
                    ScarpetWebserver.LOGGER.error("Got exception when running webserver route callback on {}", request.getHttpURI(), e);
                    callback.failed(e);
                    return true;
                }
                return true;
            }
        };
    }

    public void addSSERoute(String path, String... rooms) {
        UriTemplateMappingsHandler uriTemplateMappingsHandler = methodMappingsHandler.computeIfAbsent(HttpMethod.GET, (_) -> new UriTemplateMappingsHandler());
        uriTemplateMappingsHandler.addMapping(new UriTemplatePathSpec(path), createSSEHandler(rooms));
    }

    private Handler createSSEHandler(String... rooms) {
        for (String room : rooms)
            sseRoomConnectionMap.computeIfAbsent(room, (_) -> new ConcurrentLinkedQueue<>());
        return new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                try {
                    HttpFields.Mutable headers = response.getHeaders();
                    headers.put(HttpHeader.CONTENT_TYPE, "text/event-stream");
                    headers.put(HttpHeader.CACHE_CONTROL, "no-cache");
                    headers.put(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                    response.write(false, ByteBuffer.wrap(new byte[0]), new Callback() {
                        @Override
                        public void succeeded() {
                            SSEConnection c = new SSEConnection(request, response, callback);
                            for (String room : rooms)
                                sseRoomConnectionMap.computeIfAbsent(room, (_) -> new ConcurrentLinkedQueue<>()).add(c);
                        }

                        @Override
                        public void failed(Throwable e) {
                            ScarpetWebserver.LOGGER.error("Got exception when running webserver SSE route callback on {}", request.getHttpURI(), e);
                            callback.failed(e);
                        }
                    });
                } catch (Exception e) {
                    ScarpetWebserver.LOGGER.error("Got exception when running webserver SSE route callback on {}", request.getHttpURI(), e);
                    callback.failed(e);
                    return true;
                }
                return true;
            }
        };
    }

    private String messageField(@NotNull String fieldName, @Nullable String fieldValue) {
        return fieldValue == null ? "" : fieldName + ": " + fieldValue.replace("\n", "\n" + fieldName + ": ") + "\n";
    }

    public int sendSSEMessage(@Nullable String event, String data, String... rooms) {
        if (rooms.length == 0) return 0;
        HashSet<SSEConnection> connections = new HashSet<>();
        for (String room : rooms) {
            Collection<SSEConnection> roomConnections = sseRoomConnectionMap.get(room);
            if (roomConnections != null)
                connections.addAll(roomConnections);
        }
        if (connections.isEmpty()) return 0;
        byte[] eventBytes = (String.join("", messageField("event", event),
                messageField("data", data)) + "\n").getBytes(StandardCharsets.UTF_8);
        for (SSEConnection c : connections)
            c.response.write(false, ByteBuffer.wrap(eventBytes), new Callback() {
                @Override
                public void failed(Throwable x) {
                    for (String room : rooms) {
                        Collection<SSEConnection> roomConnections = sseRoomConnectionMap.get(room);
                        if (roomConnections != null)
                            roomConnections.remove(c);
                    }
                }
            });
        return connections.size();
    }
}
