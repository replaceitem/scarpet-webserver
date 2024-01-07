package net.replaceitem.scarpetwebserver;

import net.replaceitem.scarpetwebserver.mixin.ServiceAccessor;
import spark.RouteImpl;
import spark.Service;
import spark.route.HttpMethod;
import spark.route.Routes;

public class Webserver {
    private final Service service;
    private final String id;

    public Webserver(Config.WebserverConfig webserverConfig) {
        this.id = webserverConfig.id;
        service = Service.ignite();
        service.initExceptionHandler(this::onInitException);
        service.port(webserverConfig.port);
    }

    public Service getService() {
        return service;
    }

    public void addRoute(String method, String path, ScarpetRoute route) {
        service.addRoute(HttpMethod.get(method.toLowerCase()), RouteImpl.create(path, route));
    }
    
    public void init() {
        clearRoutes();
        service.init();
    }
    
    public void clearRoutes() {
        Routes routes = ((ServiceAccessor)(Object) service).getRoutes();
        if(routes != null) routes.clear();
    }
    
    private void onInitException(Exception e) {
        ScarpetWebserver.LOGGER.error("Error initializing webserver", e);
    }

    public String getId() {
        return id;
    }
}
