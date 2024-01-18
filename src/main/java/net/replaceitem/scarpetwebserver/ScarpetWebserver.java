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
import net.replaceitem.scarpetwebserver.script.ResponseValue;
import net.replaceitem.scarpetwebserver.script.WebserverValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Response;

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
                LOGGER.warn("Webserver with id " + webserverConfig.id + " already exists. Remove duplicate ids in the config.");
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
        SimpleTypeConverter.registerType(WebserverValue.class, Webserver.class, WebserverValue::getWebserver, "webserver");
        SimpleTypeConverter.registerType(ResponseValue.class, Response.class, ResponseValue::getResponse, "webserver_response");
        OutputConverter.register(Webserver.class, WebserverValue::new);
        OutputConverter.register(Response.class, ResponseValue::new);
        AnnotationParser.parseFunctionClass(Functions.class);
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        webservers.values().forEach(Webserver::close);
        webservers.clear();
    }
}
