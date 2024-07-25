package net.replaceitem.scarpetwebserver;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.script.annotation.AnnotationParser;
import carpet.script.annotation.OutputConverter;
import carpet.script.annotation.SimpleTypeConverter;
import carpet.script.annotation.ValueCaster;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import net.replaceitem.scarpetwebserver.script.Functions;
import net.replaceitem.scarpetwebserver.script.RequestValue;
import net.replaceitem.scarpetwebserver.script.ResponseValue;
import net.replaceitem.scarpetwebserver.script.WebserverValue;
import net.replaceitem.scarpetwebserver.webserver.Webserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import java.util.HashMap;
import java.util.Map;

public class ScarpetWebserver implements CarpetExtension, ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("scarpet-webserver");
    
    public static Config config;
    public static Map<String, Webserver> webservers = new HashMap<>();
    
    @Override
    public void onInitialize() {
        config = Config.load();
        for (Config.WebserverConfig webserverConfig : config.webservers) {
            if(webserverConfig.port < 0) continue;
            if(webservers.containsKey(webserverConfig.id)) {
                LOGGER.warn("Webserver with id {} already exists. Remove duplicate ids in the config.", webserverConfig.id);
                continue;
            }
            webservers.put(webserverConfig.id, new Webserver(webserverConfig));
        }
        CarpetServer.manageExtension(this);
        LOGGER.info("scarpet-webserver loaded");
    }

    @Override
    public void onGameStarted() {
        ValueCaster.register(WebserverValue.class, "webserver");
        ValueCaster.register(ResponseValue.class, "webserver_response");
        ValueCaster.register(RequestValue.class, "webserver_request");
        SimpleTypeConverter.registerType(WebserverValue.class, Webserver.class, WebserverValue::getWebserver, "webserver");
        SimpleTypeConverter.registerType(ResponseValue.class, Response.class, ResponseValue::getResponse, "webserver_response");
        SimpleTypeConverter.registerType(RequestValue.class, Request.class, RequestValue::getRequest, "webserver_request");
        OutputConverter.register(Webserver.class, WebserverValue::new);
        OutputConverter.register(Response.class, ResponseValue::new);
        OutputConverter.register(Request.class, RequestValue::new);
        AnnotationParser.parseFunctionClass(Functions.class);
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        for (Webserver value : webservers.values()) {
            try {
                value.close();
            } catch (Exception e) {
                LOGGER.error("Failed to close webserver '{}'", value.getId());
            }
        }
        webservers.clear();
    }
}
