package net.replaceitem.scarpetwebserver.script;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.DynamicRegistryManager;
import net.replaceitem.scarpetwebserver.ScarpetWebserver;
import net.replaceitem.scarpetwebserver.util.MapValueBuilder;
import net.replaceitem.scarpetwebserver.webserver.UriTemplateMappingsHandler;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.ConnectionMetaData;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.Fields;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class RequestValue extends Value {
    
    private final Request request;

    public RequestValue(Request response) {
        this.request = response;
    }

    public static Value of(Request request) {
        return request == null ? NULL : new RequestValue(request);
    }

    public Request getRequest() {
        return request;
    }

    @Override
    public @NotNull Value in(Value value1) {
        return switch(value1.getString()) {
            case "headers" -> MapValueBuilder.mapMap(getHeadersAsMap(request.getHeaders()), StringValue::of, Function.identity());
            case "method" -> StringValue.of(request.getMethod());
            case "beginNanoTime" -> NumericValue.of(request.getBeginNanoTime());
            case "connection" -> getConnection();
            case "uri" -> encodeUri();
            case "pathParams" -> getPathParams();
            case "body_string" -> getBodyString();
            
            default -> throw new InternalExpressionException("Unknown request query: " + value1.getString());
        };
    }

    private Value getBodyString() {
        try {
            return StringValue.of(Content.Source.asString(request));
        } catch (IOException e) {
            ScarpetWebserver.LOGGER.error("Could not get body as string", e);
            return NULL;
        }
    }

    private Value getPathParams() {
        return request.getAttribute(UriTemplateMappingsHandler.PATH_PARAMETER_ATTRIBUTE) instanceof Map<?, ?> pathParameterAttribute
                ? MapValueBuilder.mapMap(pathParameterAttribute, value -> StringValue.of((String) value), value -> StringValue.of((String) value))
                : NULL;
    }

    private MapValue encodeUri() {
        MapValueBuilder map = new MapValueBuilder();
        HttpURI httpURI = request.getHttpURI();

        map.put("scheme", httpURI.getScheme());
        map.put("authority", httpURI.getAuthority());
        map.put("host", httpURI.getHost());
        map.put("port", httpURI.getPort());
        map.put("path", httpURI.getPath());
        map.put("canonicalPath", httpURI.getCanonicalPath());
        map.put("decodedPath", httpURI.getDecodedPath());
        map.put("param", httpURI.getParam());
        map.put("query", httpURI.getQuery());
        map.put("fragment", httpURI.getFragment());
        map.put("user", httpURI.getUser());
        map.put("asString", httpURI.asString());
        map.put("queryParameters", encodeQueryParameters(request));

        return map.build();
    }

    private MapValue getConnection() {
        ConnectionMetaData connection = request.getConnectionMetaData();
        MapValueBuilder map = new MapValueBuilder();

        map.put("protocol", connection.getProtocol());
        map.put("httpVersion", connection.getHttpVersion().asString());
        map.put("id", connection.getId());
        map.put("persistent", connection.isPersistent());
        map.put("secure", connection.isSecure());

        return map.build();
    }

    private static MapValue encodeQueryParameters(Request request) {
        MapValueBuilder map = new MapValueBuilder();
        Fields fields = Request.extractQueryParameters(request);
        for (Fields.Field field : fields) {
            map.put(field.getName(), field.hasMultipleValues() ? ListValue.wrap(field.getValues().stream().map(StringValue::of).toList()) : StringValue.of(field.getValue()));
        }
        return map.build();
    }

    private static Map<String, Value> getHeadersAsMap(HttpFields fields) {
        Map<String, Value> map = new HashMap<>();
        for (HttpField field : fields) {
            List<String> values = field.getValueList();
            map.put(field.getName(), values.size() > 1 ? ListValue.wrap(values.stream().map(StringValue::of)) : StringValue.of(values.stream().findFirst().orElse(null)));
        }
        return map;
    }

    @Override
    public @NotNull String getTypeString() {
        return "webserver_request";
    }

    @Override
    public @NotNull String getString() {
        return getTypeString();
    }

    @Override
    public boolean getBoolean() {
        return true;
    }

    @Override
    public @NotNull NbtElement toTag(boolean force, DynamicRegistryManager regs) {
        return NbtString.of(this.getString());
    }
}
