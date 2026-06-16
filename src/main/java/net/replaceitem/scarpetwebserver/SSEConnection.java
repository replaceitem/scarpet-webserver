package net.replaceitem.scarpetwebserver;

import carpet.script.ScriptHost;
import carpet.script.value.Value;
import net.replaceitem.scarpetwebserver.script.RequestValue;
import net.replaceitem.scarpetwebserver.script.ResponseValue;
import net.replaceitem.scarpetwebserver.script.SSEConnectionValue;
import net.replaceitem.scarpetwebserver.webserver.Webserver;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class SSEConnection {
    private final Request request;
    private final Response response;
    private final Callback callback;
    private boolean sentInitialResponse;
    private boolean callbackInvoked;
    private boolean closed;

    public SSEConnection(Request request, Response response, Callback callback) {
        this.request = request;
        this.response = response;
        this.callback = callback;
        this.sentInitialResponse = false;
        this.callbackInvoked = false;
        this.closed = false;
    }

    public Request getRequest() {
        return request;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean sendInitialResponse() {
        if (sentInitialResponse || closed) return false;
        response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/event-stream");
        response.getHeaders().put(HttpHeader.CACHE_CONTROL, "no-cache");
        sentInitialResponse = true;
        response.write(false, ByteBuffer.wrap(new byte[0]), new Callback() {
            @Override
            public void failed(Throwable e) {
                if (e instanceof EofException)
                    closed = true;
                ScarpetWebserver.LOGGER.error("Got exception when sending response to SSE route", e);
                callbackFailed(e);
            }
        });
        return true;
    }

    public boolean overrideResponse(ScriptHost host, Value callbackValue) {
        if (sentInitialResponse) return false;
        try {
            String body = Webserver.callFunctionValue(host, callbackValue, List.of(RequestValue.of(request), ResponseValue.of(response))).getString();
            close(host, body.getBytes(StandardCharsets.UTF_8), Optional.empty());
        } catch (Exception e) {
            ScarpetWebserver.LOGGER.error("Got exception when calling SSE send message callback function", e);
        }
        return true;
    }

    public boolean addHeader(String key, String value) {
        if (sentInitialResponse || closed) return false;
        response.getHeaders().put(key, value);
        return true;
    }

    public boolean sendMessage(Optional<String> event, String data, ScriptHost host, Optional<Value> callbackValue) {
        if (!sentInitialResponse) {
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/event-stream");
            response.getHeaders().put(HttpHeader.CACHE_CONTROL, "no-cache");
            sentInitialResponse = true;
        }
        if (closed) return false;
        SSEConnection self = this;
        byte[] eventBytes = (String.join("",
                event.map(v -> "event: " + v.replace("\n", "") + "\n").orElse(""),
                "data: " + data.replace("\n", "\ndata: ") + "\n") + "\n").getBytes(StandardCharsets.UTF_8);

        response.write(false, ByteBuffer.wrap(eventBytes), new Callback() {
            @Override
            public void succeeded() {
                callbackValue.ifPresent(callbackValue -> {
                    try {
                        Webserver.callFunctionValue(host, callbackValue, List.of(SSEConnectionValue.of(self), Value.TRUE));
                    } catch (Exception e) {
                        ScarpetWebserver.LOGGER.error("Got exception when calling SSE send message callback function", e);
                    }
                });
            }

            @Override
            public void failed(Throwable e) {
                callbackValue.ifPresent(callbackValue -> {
                    try {
                        Webserver.callFunctionValue(host, callbackValue, List.of(SSEConnectionValue.of(self), Value.FALSE));
                    } catch (Exception e1) {
                        ScarpetWebserver.LOGGER.error("Got exception when calling SSE send message callback function", e1);
                    }
                });
                if (e instanceof EofException)
                    closed = true;
                ScarpetWebserver.LOGGER.error("Got exception when trying to send message to SSE connection", e);
                callbackFailed(e);
            }
        });

        return true;
    }

    public boolean close(ScriptHost host, Optional<Value> callbackValue) {
        return close(host, new byte[0], callbackValue);
    }

    private boolean close(ScriptHost host, byte[] data, Optional<Value> callbackValue) {
        if (closed) return false;
        closed = true;
        SSEConnection self = this;

        response.write(true, ByteBuffer.wrap(data), new Callback() {
            @Override
            public void succeeded() {
                callbackSucceeded();
                callbackValue.ifPresent(callbackValue -> {
                    try {
                        Webserver.callFunctionValue(host, callbackValue, List.of(SSEConnectionValue.of(self), Value.TRUE));
                    } catch (Exception e) {
                        ScarpetWebserver.LOGGER.error("Got exception when calling SSE close callback function", e);
                    }
                });
            }

            @Override
            public void failed(Throwable e) {
                callbackValue.ifPresent(callbackValue -> {
                    try {
                        Webserver.callFunctionValue(host, callbackValue, List.of(SSEConnectionValue.of(self), e instanceof EofException ? Value.TRUE : Value.FALSE));
                    } catch (Exception e1) {
                        ScarpetWebserver.LOGGER.error("Got exception when calling SSE close callback function", e1);
                    }
                });
                if (e instanceof EofException) {
                    callbackSucceeded();
                    return;
                }
                ScarpetWebserver.LOGGER.error("Got exception when trying to close SSE connection", e);
                callbackFailed(e);
            }
        });

        return true;
    }

    public boolean isClosed() {
        return closed;
    }

    public void callbackSucceeded() {
        if (callbackInvoked) return;
        callbackInvoked = true;
        callback.succeeded();
    }

    public void callbackFailed(Throwable e) {
        if (callbackInvoked) return;
        callbackInvoked = true;
        callback.failed(e);
    }
}
