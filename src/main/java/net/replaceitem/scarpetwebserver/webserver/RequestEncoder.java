package net.replaceitem.scarpetwebserver.webserver;

import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.replaceitem.scarpetwebserver.util.MapValueBuilder;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.ConnectionMetaData;
import org.eclipse.jetty.server.Request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class RequestEncoder {
    public static MapValue encodeRequest(Request request) {
        MapValueBuilder map = new MapValueBuilder();
        
        map.put("beginNanoTime", request.getBeginNanoTime());
        map.put("headers", mapMap(getHeadersAsMap(request.getHeaders()), StringValue::of, Function.identity()));
        map.put("method", request.getMethod());
        map.put("connection", encodeConnection(request.getConnectionMetaData()));
        map.put("uri", encodeUri(request.getHttpURI()));
        if(request.getAttribute(UriTemplateMappingsHandler.PATH_PARAMETER_ATTRIBUTE) instanceof Map<?,?> pathParameterAttribute) {
            map.put("pathParams", mapMap(pathParameterAttribute, value -> StringValue.of(((String) value)), value -> StringValue.of(((String) value))));
        }
                
        return map.build();
    }

    private static MapValue encodeUri(HttpURI httpURI) {
        MapValueBuilder map = new MapValueBuilder();

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

        return map.build();
    }

    private static MapValue encodeConnection(ConnectionMetaData connection) {
        MapValueBuilder map = new MapValueBuilder();
        
        map.put("protocol", connection.getProtocol());
        map.put("httpVersion", connection.getHttpVersion().asString());
        map.put("id", connection.getId());
        map.put("persistent", connection.isPersistent());
        map.put("secure", connection.isSecure());

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

    private static <K,V> MapValue mapMap(Map<K,V> map, Function<K,Value> keyMapper, Function<V,Value> valueMapper) {
        Map<Value,Value> valueMap = new HashMap<>();
        for (Map.Entry<K, V> kkEntry : map.entrySet()) {
            valueMap.put(keyMapper.apply(kkEntry.getKey()), valueMapper.apply(kkEntry.getValue()));
        }
        return MapValue.wrap(valueMap);
    }
}
