package net.replaceitem.scarpetwebserver;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.script.annotation.AnnotationParser;
import carpet.script.annotation.OutputConverter;
import carpet.script.annotation.SimpleTypeConverter;
import carpet.script.annotation.ValueCaster;
import net.fabricmc.api.ModInitializer;
import net.replaceitem.scarpetwebserver.script.Functions;
import net.replaceitem.scarpetwebserver.script.ResponseValue;
import net.replaceitem.scarpetwebserver.script.WebserverValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Response;
import spark.Service;

public class ScarpetWebserver implements CarpetExtension, ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("scarpet-webserver");
    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(this);
        LOGGER.info("scarpet-webserver loaded");
    }

    @Override
    public void onGameStarted() {
        AnnotationParser.parseFunctionClass(Functions.class);
        ValueCaster.register(WebserverValue.class, "webserver");
        ValueCaster.register(ResponseValue.class, "webserver_response");
        SimpleTypeConverter.registerType(WebserverValue.class, Service.class, WebserverValue::getService, "webserver");
        SimpleTypeConverter.registerType(ResponseValue.class, Response.class, ResponseValue::getResponse, "webserver_response");
        OutputConverter.register(Service.class, WebserverValue::new);
        OutputConverter.register(Response.class, ResponseValue::new);
    }
}
